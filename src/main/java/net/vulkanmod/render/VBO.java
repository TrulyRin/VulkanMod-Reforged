package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.compat.observer.GuiRenderTrace;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.vulkan.shader.PipelineState;
import org.lwjgl.opengl.GL11;
import net.vulkanmod.compat.gl.GlPatchState;
import net.vulkanmod.compat.sable.render.SublevelRenderScope;
import net.vulkanmod.compat.sable.veil.VeilProgramDraws;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.PreparedDraw;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.CustomUniformMatrices;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

@OnlyIn(Dist.CLIENT)
public class VBO {
    private VertexBuffer vertexBuffer;
    private static final IndexUploadState.Backend<IndexBuffer> INDEX_BACKEND = new IndexUploadState.Backend<>() {
        public IndexBuffer create(int bytes, QuadPatchIndices.IndexType type) {
            return new IndexBuffer(bytes, MemoryTypes.GPU_MEM, type == QuadPatchIndices.IndexType.U16
                    ? IndexBuffer.IndexType.SHORT : IndexBuffer.IndexType.INT);
        }
        public void upload(IndexBuffer buffer, ByteBuffer data) { buffer.copyBuffer(data); }
        public void retire(IndexBuffer buffer) { buffer.freeBuffer(); }
        public ByteBuffer allocateStaging(int bytes) { return MemoryUtil.memAlloc(bytes); }
        public void freeStaging(ByteBuffer data) { MemoryUtil.memFree(data); }
    };
    private IndexUploadState<IndexBuffer> indices = new IndexUploadState<>(INDEX_BACKEND);
    private final DirectVertexBufferState directState = new DirectVertexBufferState();
    private AutoIndexBuffer automaticIndices;

    private int indexCount;
    private int uploadedIndexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
    private VertexFormat format;
    private VertexFormat.IndexType indexType;
    private boolean uploaded;
    private boolean closed;
    private boolean immediateUpload;
    private boolean transientGeometry;
    private boolean uploadedCustomIndices;
    private boolean nativeCountSelected;
    private int nativeIndexCount;
    private final PreparedDraw prepared = new PreparedDraw();
    private final float[] savedMatrices = new float[48];
    private final Matrix4f savedModelViewUniform = new Matrix4f();
    private final Matrix4f savedProjectionUniform = new Matrix4f();
    private boolean savedModelView, savedProjection, drawing;
    private int savedMatrixProgram;
    private long savedMatrixGeneration;

    public boolean setImmediateUpload(boolean enabled) {
        boolean previous = this.immediateUpload;
        this.immediateUpload = enabled;
        return previous;
    }

    public VBO() {}

    public void upload(MeshData meshData) {
        uploadAndGetState(meshData);
    }


    public MeshData.DrawState uploadAndGetState(MeshData meshData) {
        try (meshData) {
            if (closed) throw new IllegalStateException("VBO is closed");
            this.uploaded = false;
            this.indexCount = 0;
            this.nativeCountSelected = false;
            this.directState.clear();
            this.automaticIndices = null;
            this.prepared.invalidate();
            this.transientGeometry = this.immediateUpload;
            this.indices.clear();
            MeshData.DrawState parameters = meshData.drawState();
            this.uploadedIndexCount = parameters.indexCount();
            this.vertexCount = parameters.vertexCount();
            this.mode = parameters.mode();
            this.format = parameters.format();
            this.indexType = parameters.indexType();
            ByteBuffer customIndices = meshData.indexBuffer();
            this.uploadedCustomIndices = customIndices != null;
            if (this.uploadedIndexCount < 0 || this.vertexCount < 0) {
                throw new IllegalArgumentException("Negative VBO geometry counts");
            }
            if (this.transientGeometry) {
                VertexBuffer previous = this.vertexBuffer;
                this.vertexBuffer = null;
                if (previous != null) previous.freeBuffer();
                Renderer.getDrawer().prepare(this.prepared, meshData.vertexBuffer(), parameters, customIndices);
                this.indexCount = this.uploadedIndexCount;
            } else {
                this.uploadVertexBuffer(parameters, meshData.vertexBuffer());
                this.uploadIndices(customIndices);
            }
            this.uploaded = true;
            return parameters;
        } catch (RuntimeException | Error failure) {
            this.uploaded = false;
            this.prepared.invalidate();
            this.indexCount = 0;
            throw failure;
        }
    }

    private void uploadVertexBuffer(MeshData.DrawState parameters, ByteBuffer data) {
        if (data != null) {
            VertexBuffer previous = this.vertexBuffer;
            this.vertexBuffer = null;
            if (previous != null) previous.freeBuffer();
            if (parameters.vertexCount() > 0) {
                this.vertexBuffer = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
                this.vertexBuffer.copyToVertexBuffer(parameters.format().getVertexSize(), parameters.vertexCount(), data);
            }
        }
    }

    public void uploadIndexBuffer(ByteBuffer data) {
        if (!uploaded || closed) throw new IllegalStateException("Index re-sort requires a completed VBO upload");
        try {
            if (this.transientGeometry) Renderer.getDrawer().updatePreparedIndices(this.prepared, data);
            else this.uploadIndices(data);
            this.uploadedCustomIndices = true;
            this.nativeCountSelected = false;
            this.directState.clear();
        } catch (RuntimeException | Error failure) {
            this.uploaded = false;
            this.prepared.invalidate();
            this.indexCount = 0;
            throw failure;
        }
    }

    private void uploadIndices(ByteBuffer data) {
        this.automaticIndices = null;
        this.indexCount = this.uploadedIndexCount;
        if (this.indexCount == 0) {
            this.indices.clear();
            return;
        }
        if (data == null) {

            this.indices.clear();

            AutoIndexBuffer autoIndexBuffer;
            switch (this.mode) {
                case TRIANGLE_FAN -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleFanIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case TRIANGLE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleStripIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getTriangleStripIndexBuffer();
                    this.indexCount = AutoIndexBuffer.DrawType.getTriangleStripIndexCount(this.vertexCount);
                }
                case QUADS -> {
                    autoIndexBuffer = Renderer.getDrawer().getQuadsIndexBuffer();
                }
                case LINES -> {
                    autoIndexBuffer = Renderer.getDrawer().getLinesIndexBuffer();
                }
                case DEBUG_LINE_STRIP -> {
                    autoIndexBuffer = Renderer.getDrawer().getDebugLineStripIndexBuffer();
                }
                case TRIANGLES, DEBUG_LINES -> {
                    autoIndexBuffer = null;
                }
                default -> throw new IllegalStateException("Unexpected draw mode: %s".formatted(this.mode));
            }

            if (autoIndexBuffer != null) {
                autoIndexBuffer.checkCapacity(this.vertexCount);
                this.indices.useBorrowed(autoIndexBuffer.getIndexBuffer());
                this.automaticIndices = autoIndexBuffer;
            }
        } else {
            this.indices.uploadCustom(data, PatchGeometry.indexType(this.indexType), this.uploadedIndexCount,
                    this.vertexCount, this.mode == VertexFormat.Mode.QUADS);
        }

    }

    public boolean usesCustomIndices() { return this.uploadedCustomIndices; }

    public void markManaged(int vertexId, int indexId) {
        if (this.closed) throw new IllegalStateException("VBO is closed");
        this.directState.markManaged(vertexId, indexId);
        this.nativeCountSelected = false;
    }


    public void syncDirect(int vertexId, int indexId, VertexFormat format, VertexFormat.Mode mode,
                           VertexFormat.IndexType type, boolean sequential, int nativeIndexCount) {
        if (this.closed) throw new IllegalStateException("VBO is closed");
        try {
            if (this.directState.usesManaged(vertexId, indexId)) {
                if (!this.uploaded) throw new IllegalStateException("Managed vertex geometry was invalidated");
                if (this.format != format || this.mode != mode || (this.uploadedCustomIndices && this.indexType != type))
                    throw new IllegalArgumentException("Native metadata differs from the successful MeshData upload");
                DirectVertexBufferState.requireCount(nativeIndexCount, this.uploadedIndexCount);
                refreshAutomaticIndices();
                this.nativeIndexCount = nativeIndexCount;
                this.nativeCountSelected = true;
                this.indexCount = !this.uploadedCustomIndices ? DirectVertexBufferState.convertedCount(mode, nativeIndexCount) : nativeIndexCount;
                return;
            }
            if (format == null && nativeIndexCount == 0 && !this.uploaded) {
                this.indexCount = 0;
                return;
            }
            this.indexCount = 0;
            boolean changed = this.directState.prepare(vertexId, indexId, format, mode, type, sequential, nativeIndexCount);
            if (changed) replaceDirect(format, mode, type, sequential);
            refreshAutomaticIndices();
            this.nativeIndexCount = nativeIndexCount;
            this.nativeCountSelected = true;
            this.indexCount = this.directState.submittedCount();
            this.uploaded = true;
        } catch (RuntimeException | Error failure) {
            this.uploaded = false;
            this.indexCount = 0;
            this.prepared.invalidate();
            throw failure;
        }
    }

    private void replaceDirect(VertexFormat format, VertexFormat.Mode mode, VertexFormat.IndexType type, boolean sequential) {
        VertexBuffer replacement = null;
        IndexUploadState<IndexBuffer> replacementIndices = new IndexUploadState<>(INDEX_BACKEND);
        AutoIndexBuffer automatic = null;
        ByteBuffer converted = null;
        try {
            int vertices = this.directState.vertexCount();
            if (vertices > 0) {
                ByteBuffer data = this.directState.vertices();
                replacement = new VertexBuffer(data.remaining(), MemoryTypes.GPU_MEM);
                replacement.copyToVertexBuffer(format.getVertexSize(), vertices, data);
            }
            if (sequential) {
                automatic = automaticIndices(mode);
                if (automatic != null) {
                    automatic.checkCapacity(vertices);
                    replacementIndices.useBorrowed(automatic.getIndexBuffer());
                }
            } else {
                ByteBuffer data;
                if (this.directState.convertsIndices()) {
                    converted = MemoryUtil.memAlloc(DirectVertexBufferState.checkedBytes(this.directState.convertedCapacity(), type.bytes));
                    this.directState.writeConvertedIndices(converted);
                    data = converted;
                } else data = this.directState.indices();
                replacementIndices.uploadCustom(data, PatchGeometry.indexType(type), this.directState.convertedCapacity(),
                        vertices, mode == VertexFormat.Mode.QUADS);
            }
        } catch (RuntimeException | Error failure) {
            try { replacementIndices.close(); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            if (replacement != null) {
                try { replacement.freeBuffer(); }
                catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            }
            throw failure;
        } finally {
            if (converted != null) MemoryUtil.memFree(converted);
        }
        VertexBuffer previous = this.vertexBuffer;
        IndexUploadState<IndexBuffer> previousIndices = this.indices;
        this.vertexBuffer = replacement;
        this.indices = replacementIndices;
        this.automaticIndices = automatic;
        this.vertexCount = this.directState.vertexCount();
        this.uploadedIndexCount = this.directState.capacity();
        this.format = format;
        this.mode = mode;
        this.indexType = type;
        this.uploadedCustomIndices = !sequential;
        this.transientGeometry = false;
        this.prepared.invalidate();
        this.directState.commit();
        Throwable failure = null;
        try { previousIndices.close(); }
        catch (RuntimeException | Error cleanup) { failure = cleanup; }
        if (previous != null) {
            try { previous.freeBuffer(); }
            catch (RuntimeException | Error cleanup) {
                if (failure == null) failure = cleanup;
                else if (failure != cleanup) failure.addSuppressed(cleanup);
            }
        }
        rethrow(failure);
    }

    private static AutoIndexBuffer automaticIndices(VertexFormat.Mode mode) {
        return switch (mode) {
            case QUADS -> Renderer.getDrawer().getQuadsIndexBuffer();
            case LINES -> Renderer.getDrawer().getLinesIndexBuffer();
            case TRIANGLE_FAN -> Renderer.getDrawer().getTriangleFanIndexBuffer();
            case TRIANGLE_STRIP, LINE_STRIP -> Renderer.getDrawer().getTriangleStripIndexBuffer();
            case DEBUG_LINE_STRIP -> Renderer.getDrawer().getDebugLineStripIndexBuffer();
            case TRIANGLES, DEBUG_LINES -> null;
        };
    }

    private void refreshAutomaticIndices() {
        if (!this.transientGeometry && this.automaticIndices != null) {
            IndexBuffer current = this.automaticIndices.getIndexBuffer();
            if (this.indices.indices() != current) this.indices.useBorrowed(current);
        }
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (!hasGeometry() || shader == null) return;
        RenderSystem.assertOnRenderThread();
        if (!shader.getVertexFormat().equals(this.format) && net.vulkanmod.compat.RuntimeOptions.diagnosticsEnabled()) {
            net.vulkanmod.Initializer.LOGGER.warn("Vertex format mismatch for shader {}", shader.getName());
        }
        saveMatrices(shader);
        Throwable failure = null;
        boolean retainNamedMatrices = false;
        try {
            boolean programOwned = VeilShaderProgramAccess.isWrapper(shader);
            GraphicsPipeline pipeline;
            if (programOwned) {
                pipeline = VeilProgramDraws.prepare(shader, this.format, true, MV, P);
            } else {
                shader.setDefaultUniforms(this.mode, MV, P, net.minecraft.client.Minecraft.getInstance().getWindow());
                shader.apply();
                pipeline = ((ShaderMixed) shader).getPipeline(this.format);
            }
            if (pipeline == null) return;
            validateGeometry(pipeline);
            VRenderSystem.applyMVP(MV, P);
            VRenderSystem.setPrimitiveTopology(this.mode);
            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline(pipeline);
            if (!programOwned) VTextureSelector.bindShaderTextures(pipeline, (ShaderMixed) shader);
            renderer.uploadAndBindUBOs(pipeline);
            traceHudDraw(shader, pipeline, P);
            drawGeometry(pipeline);

            retainNamedMatrices = !programOwned;
        } catch (RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            restoreMatrices(retainNamedMatrices ? null : shader, failure);
        }
    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, GraphicsPipeline pipeline) {
        if (!hasGeometry() || pipeline == null) return;
        RenderSystem.assertOnRenderThread();
        saveMatrices(null);
        Throwable failure = null;
        try {
            validateGeometry(pipeline);
            VRenderSystem.applyMVP(MV, P);
            VRenderSystem.setPrimitiveTopology(this.mode);
            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            drawGeometry(pipeline);
        } catch (RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            restoreMatrices(null, failure);
        }
    }

    private void traceHudDraw(ShaderInstance shader, GraphicsPipeline pipeline, Matrix4f projection) {
        if (!GuiRenderTrace.isActive()) return;
        GuiRenderTrace.logDraw("shaderName=" + shader.getName()
                + " pipeline=" + pipeline.name + " vertexCount=" + this.vertexCount + " mode=" + this.mode
                + " alpha=" + VRenderSystem.getShaderColor().getFloat(12)
                + " blend=" + PipelineState.blendInfo.enabled
                + " srcRgb=" + PipelineState.blendInfo.srcRgbFactor + " dstRgb=" + PipelineState.blendInfo.dstRgbFactor
                + " srcAlpha=" + PipelineState.blendInfo.srcAlphaFactor + " dstAlpha=" + PipelineState.blendInfo.dstAlphaFactor
                + " depthTest=" + VRenderSystem.depthTest + " depthMask=" + VRenderSystem.depthMask + " cull=" + VRenderSystem.cull
                + " texture0=" + RenderSystem.getShaderTexture(0) + " boundTexture=" + GlTexture.getBoundTextureId(GL11.GL_TEXTURE_2D)
                + " projectionM33=" + projection.m33() + " projectionM23=" + projection.m23());
    }

    private boolean hasGeometry() {
        return this.uploaded && (!this.nativeCountSelected || this.indexCount > 0)
                && (this.transientGeometry ? this.vertexCount > 0 : this.indexCount > 0);
    }

    private void saveMatrices(ShaderInstance shader) {
        if (this.drawing) throw new IllegalStateException("Vertex buffer draw is already in progress");
        for (int i = 0; i < 16; i++) {
            this.savedMatrices[i] = VRenderSystem.getModelViewMatrix().getFloat(i * 4);
            this.savedMatrices[16 + i] = VRenderSystem.getProjectionMatrix().getFloat(i * 4);
            this.savedMatrices[32 + i] = VRenderSystem.MVP.getFloat(i * 4);
        }
        this.savedModelView = shader != null && shader.MODEL_VIEW_MATRIX != null;
        this.savedProjection = shader != null && shader.PROJECTION_MATRIX != null;
        this.savedMatrixProgram = 0;
        this.savedMatrixGeneration = 0;
        if ((this.savedModelView || this.savedProjection) && VeilShaderProgramAccess.isWrapper(shader)) {
            int owner = VeilShaderProgramAccess.programId(shader);
            var linked = GlProgram.linkedProgram(owner);
            if (owner <= 0 || linked == null) throw new IllegalStateException("Veil matrix snapshot requires a linked program owner");
            this.savedMatrixProgram = owner;
            this.savedMatrixGeneration = linked.generation();
            if (this.savedModelView) CustomUniformMatrices.copyProgram(owner, linked.generation(), this.savedModelViewUniform, "ModelViewMat");
            if (this.savedProjection) CustomUniformMatrices.copyProgram(owner, linked.generation(), this.savedProjectionUniform, "ProjMat");
        } else {
            if (this.savedModelView) CustomUniformMatrices.copy(shader.MODEL_VIEW_MATRIX, this.savedModelViewUniform, "ModelViewMat");
            if (this.savedProjection) CustomUniformMatrices.copy(shader.PROJECTION_MATRIX, this.savedProjectionUniform, "ProjMat");
        }
        this.drawing = true;
    }

    private void restoreMatrices(ShaderInstance shader, Throwable primary) {
        try {

            for (int i = 0; i < 16; i++) {
                VRenderSystem.getModelViewMatrix().putFloat(i * 4, this.savedMatrices[i]);
                VRenderSystem.getProjectionMatrix().putFloat(i * 4, this.savedMatrices[16 + i]);
                VRenderSystem.MVP.putFloat(i * 4, this.savedMatrices[32 + i]);
            }
            boolean programOwned = shader != null && this.savedMatrixProgram > 0;
            int previousProgram = GlProgram.currentProgramId();
            if (programOwned) {
                if (VeilShaderProgramAccess.programId(shader) != this.savedMatrixProgram)
                    throw new IllegalStateException("Veil matrix snapshot program owner changed during the draw");
                CustomUniformMatrices.requireProgramGeneration(this.savedMatrixProgram, this.savedMatrixGeneration);
                GlProgram.useProgram(this.savedMatrixProgram);
            }
            try {
                Throwable failure = null;
                try { if (shader != null && this.savedModelView) shader.MODEL_VIEW_MATRIX.set(this.savedModelViewUniform); }
                catch (RuntimeException | Error error) { failure = error; }
                try { if (shader != null && this.savedProjection) shader.PROJECTION_MATRIX.set(this.savedProjectionUniform); }
                catch (RuntimeException | Error error) {
                    if (failure == null) failure = error;
                    else if (failure != error) failure.addSuppressed(error);
                }
                if (failure instanceof RuntimeException error) throw error;
                if (failure instanceof Error error) throw error;
            } finally {
                if (programOwned) GlProgram.useProgram(previousProgram);
            }
        } catch (RuntimeException | Error restore) {
            if (primary == null) throw restore;
            if (restore != primary) primary.addSuppressed(restore);
        } finally {
            this.drawing = false;
        }
    }

    public void draw() {
        if (hasGeometry()) {
            RenderSystem.assertOnRenderThread();

            Renderer renderer = Renderer.getInstance();
            Pipeline previousPipeline = renderer.getBoundPipeline();
            var scopedState = SublevelRenderScope.current().orElse(null);
            Object activeShader = scopedState != null && scopedState.shader() != null
                    ? scopedState.shader() : RenderSystem.getShader();

            if (activeShader instanceof ShaderInstance shader && VeilShaderProgramAccess.isWrapper(shader)) {
                GraphicsPipeline pipeline = VeilProgramDraws.prepare(shader, this.format, true);
                if (pipeline == null) return;
                validateGeometry(pipeline);
                try {
                    VRenderSystem.setPrimitiveTopology(this.mode);
                    renderer.bindGraphicsPipeline(pipeline);
                    renderer.uploadAndBindUBOs(pipeline);
                    drawGeometry(pipeline);
                } finally {
                    renderer.restoreGraphicsPipeline(previousPipeline);
                }
                return;
            }
            if (scopedState != null && activeShader instanceof ShaderMixed scopedShader) {
                GraphicsPipeline scopedPipeline = scopedShader.getSablePipeline(this.format);
                if (scopedPipeline != null) {
                    SublevelRenderScope.Token shaderScope = scopedState.shader() == null
                            ? SublevelRenderScope.enter(scopedState.pass(), activeShader)
                            : null;
                    try {
                        validateGeometry(scopedPipeline);
                        renderer.bindGraphicsPipeline(scopedPipeline);
                        VTextureSelector.bindShaderTextures(scopedPipeline);
                        renderer.uploadAndBindUBOs(scopedPipeline);
                        drawGeometry(scopedPipeline);
                    } finally {
                        renderer.restoreGraphicsPipeline(previousPipeline);
                        VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
                        if (shaderScope != null) {
                            shaderScope.close();
                        }
                    }
                    return;
                }
            }
            Pipeline pipeline = renderer.getBoundPipeline();
            validateGeometry(pipeline);
            VRenderSystem.setPrimitiveTopology(this.mode);
            if (pipeline instanceof GraphicsPipeline graphics) renderer.bindGraphicsPipeline(graphics);
            renderer.uploadAndBindUBOs(pipeline);

            drawGeometry(pipeline);
        }
    }


    private void validateGeometry(Pipeline pipeline) {
        if (this.transientGeometry) {
            boolean patches = pipeline instanceof GraphicsPipeline graphics && graphics.hasTessellation();
            if (this.nativeCountSelected) Renderer.getDrawer().validatePrepared(this.prepared, patches, this.nativeIndexCount);
            else Renderer.getDrawer().validatePrepared(this.prepared, patches);
            return;
        }
        if (pipeline instanceof GraphicsPipeline graphics && graphics.hasTessellation()) {
            PatchGeometry.requireQuads(this.mode, this.vertexCount, this.uploadedIndexCount, GlPatchState.getPatchVertices());
            int selected = this.nativeCountSelected ? DirectVertexBufferState.patchCount(this.nativeIndexCount) : this.vertexCount;
            if (this.indices.isCustom()) {
                this.indices.requirePatchIndices();
                if (selected > this.indices.patchIndexCount()) throw new IllegalArgumentException("Native patch prefix exceeds its companion index stream");
            }
        }
    }

    private void drawGeometry(Pipeline pipeline) {
        if (this.transientGeometry) {
            boolean patches = pipeline instanceof GraphicsPipeline graphics && graphics.hasTessellation();
            if (this.nativeCountSelected) Renderer.getDrawer().drawPrepared(this.prepared, patches, this.nativeIndexCount);
            else Renderer.getDrawer().drawPrepared(this.prepared, patches);
            return;
        }
        if (pipeline instanceof GraphicsPipeline graphics && graphics.hasTessellation()) {
            int selected = this.nativeCountSelected ? DirectVertexBufferState.patchCount(this.nativeIndexCount) : this.vertexCount;
            if (this.indices.isCustom()) {
                Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indices.requirePatchIndices(),
                        this.nativeCountSelected ? selected : this.indices.patchIndexCount());
            } else {
                Renderer.getDrawer().draw(this.vertexBuffer, selected);
            }
        } else if (this.indices.indices() != null) {
            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indices.indices(), this.indexCount);
        } else {
            Renderer.getDrawer().draw(this.vertexBuffer, this.nativeCountSelected ? this.nativeIndexCount : this.vertexCount);
        }
    }

    public void close() {
        if (closed) return;
        closed = true;
        this.prepared.invalidate();
        VertexBuffer previous = this.vertexBuffer;
        this.vertexBuffer = null;
        this.uploaded = false;
        this.vertexCount = 0;
        this.indexCount = 0;
        this.uploadedIndexCount = 0;
        this.format = null;
        this.mode = null;
        this.indexType = null;
        this.automaticIndices = null;
        this.directState.clear();
        this.nativeCountSelected = false;
        Throwable failure = null;
        try { this.indices.close(); }
        catch (RuntimeException | Error cleanup) { failure = cleanup; }
        if (previous != null) {
            try { previous.freeBuffer(); }
            catch (RuntimeException | Error cleanup) {
                if (failure == null) failure = cleanup;
                else if (failure != cleanup) failure.addSuppressed(cleanup);
            }
        }
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

}
