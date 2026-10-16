package net.vulkanmod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.compat.gl.GlPatchState;
import net.vulkanmod.compat.sable.render.SableDrawState;
import net.vulkanmod.compat.sable.render.SublevelRenderScope;
import net.vulkanmod.compat.sable.veil.VeilProgramDraws;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.AutoIndexBuffer;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

@OnlyIn(Dist.CLIENT)
public class VBO {
    private VertexBuffer vertexBuffer;
    private final IndexUploadState<IndexBuffer> indices = new IndexUploadState<>(new IndexUploadState.Backend<>() {
        public IndexBuffer create(int bytes, QuadPatchIndices.IndexType type) {
            return new IndexBuffer(bytes, MemoryTypes.GPU_MEM, type == QuadPatchIndices.IndexType.U16
                    ? IndexBuffer.IndexType.SHORT : IndexBuffer.IndexType.INT);
        }
        public void upload(IndexBuffer buffer, ByteBuffer data) { buffer.copyBuffer(data); }
        public void retire(IndexBuffer buffer) { buffer.freeBuffer(); }
        public ByteBuffer allocateStaging(int bytes) { return MemoryUtil.memAlloc(bytes); }
        public void freeStaging(ByteBuffer data) { MemoryUtil.memFree(data); }
    });

    private int indexCount;
    private int uploadedIndexCount;
    private int vertexCount;
    private VertexFormat.Mode mode;
    private VertexFormat format;
    private VertexFormat.IndexType indexType;
    private boolean uploaded;
    private boolean closed;

    public VBO() {}

    public void upload(MeshData meshData) {
        try (meshData) {
            if (closed) throw new IllegalStateException("VBO is closed");
            this.uploaded = false;
            this.indexCount = 0;
            this.indices.clear();
            MeshData.DrawState parameters = meshData.drawState();
            this.uploadedIndexCount = parameters.indexCount();
            this.vertexCount = parameters.vertexCount();
            this.mode = parameters.mode();
            this.format = parameters.format();
            this.indexType = parameters.indexType();
            if (this.uploadedIndexCount < 0 || this.vertexCount < 0) {
                throw new IllegalArgumentException("Negative VBO geometry counts");
            }
            this.uploadVertexBuffer(parameters, meshData.vertexBuffer());
            this.uploadIndices(meshData.indexBuffer());
            this.uploaded = true;
        } catch (RuntimeException | Error failure) {
            this.uploaded = false;
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
            this.uploadIndices(data);
        } catch (RuntimeException | Error failure) {
            this.uploaded = false;
            this.indexCount = 0;
            throw failure;
        }
    }

    private void uploadIndices(ByteBuffer data) {
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
            }
        } else {
            this.indices.uploadCustom(data, PatchGeometry.indexType(this.indexType), this.uploadedIndexCount,
                    this.vertexCount, this.mode == VertexFormat.Mode.QUADS);
        }

    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, ShaderInstance shader) {
        if (this.indexCount != 0) {
            RenderSystem.assertOnRenderThread();

            RenderSystem.setShader(() -> shader);

            if (VeilShaderProgramAccess.isWrapper(shader)) {
                GraphicsPipeline pipeline = VeilProgramDraws.prepare(shader, this.format, true, MV, P);
                if (pipeline == null) return;
                validateGeometry(pipeline);
                Renderer renderer = Renderer.getInstance();
                VRenderSystem.setPrimitiveTopology(this.mode);
                renderer.bindGraphicsPipeline(pipeline);
                renderer.uploadAndBindUBOs(pipeline);
                drawGeometry(pipeline);
                return;
            }

            drawWithShader(MV, P, ((ShaderMixed) shader).getPipeline(this.format));

        }
    }

    public void drawWithShader(Matrix4f MV, Matrix4f P, GraphicsPipeline pipeline) {
        if (this.indexCount != 0) {
            if (pipeline == null) {
                return;
            }
            RenderSystem.assertOnRenderThread();

            validateGeometry(pipeline);
            VRenderSystem.applyMVP(MV, P);

            VRenderSystem.setPrimitiveTopology(this.mode);

            Renderer renderer = Renderer.getInstance();
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);

            drawGeometry(pipeline);

            VRenderSystem.applyMVP(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());

        }
    }

    public void draw() {
        if (this.indexCount != 0) {
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
                        SableDrawState.applyMatrices(scopedShader);
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
            renderer.uploadAndBindUBOs(pipeline);

            drawGeometry(pipeline);
        }
    }

                                                                                  
    private void validateGeometry(Pipeline pipeline) {
        if (pipeline instanceof GraphicsPipeline graphics && graphics.hasTessellation()) {
            PatchGeometry.requireQuads(this.mode, this.vertexCount, this.uploadedIndexCount, GlPatchState.getPatchVertices());
            if (this.indices.isCustom()) this.indices.requirePatchIndices();
        }
    }

    private void drawGeometry(Pipeline pipeline) {
        if (pipeline instanceof GraphicsPipeline graphics && graphics.hasTessellation()) {
            if (this.indices.isCustom()) {
                Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indices.requirePatchIndices(), this.indices.patchIndexCount());
            } else {
                Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
            }
        } else if (this.indices.indices() != null) {
            Renderer.getDrawer().drawIndexed(this.vertexBuffer, this.indices.indices(), this.indexCount);
        } else {
            Renderer.getDrawer().draw(this.vertexBuffer, this.vertexCount);
        }
    }

    public void close() {
        if (closed) return;
        closed = true;
        VertexBuffer previous = this.vertexBuffer;
        this.vertexBuffer = null;
        this.uploaded = false;
        this.vertexCount = 0;
        this.indexCount = 0;
        this.uploadedIndexCount = 0;
        this.format = null;
        this.mode = null;
        this.indexType = null;
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
