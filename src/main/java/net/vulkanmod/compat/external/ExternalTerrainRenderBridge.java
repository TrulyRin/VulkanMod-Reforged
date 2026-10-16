package net.vulkanmod.compat.external;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.IndexBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.VertexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class ExternalTerrainRenderBridge {
    private static final int EXTERNAL_LOD_VERTEX_SIZE_BYTES = 16;
    private static final String DEBUG_DRAW_PROPERTY = "vulkanmod.compat.externalLod.debugDraw";
    private static final boolean DEBUG_DRAW_ENABLED = Boolean.getBoolean(DEBUG_DRAW_PROPERTY);
    private static final long DIAGNOSTIC_INTERVAL_MS = 2_000L;

    private static final Int2ReferenceOpenHashMap<VertexUpload> VERTEX_UPLOADS = new Int2ReferenceOpenHashMap<>();
    private static final Int2ReferenceOpenHashMap<IndexUpload> INDEX_UPLOADS = new Int2ReferenceOpenHashMap<>();
    private static final DrawDiagnostics DIAGNOSTICS = new DrawDiagnostics();

    private static final MappedBuffer COMBINED_MATRIX = new MappedBuffer(16 * Float.BYTES);
    private static final MappedBuffer MODEL_OFFSET_AND_Y_OFFSET = new MappedBuffer(4 * Float.BYTES);
    private static final MappedBuffer RENDER_PARAMS = new MappedBuffer(4 * Float.BYTES);

    private ExternalTerrainRenderBridge() {
    }

    public static MappedBuffer getCombinedMatrix() {
        return COMBINED_MATRIX;
    }

    public static MappedBuffer getModelOffsetAndYOffset() {
        return MODEL_OFFSET_AND_Y_OFFSET;
    }

    public static MappedBuffer getRenderParams() {
        return RENDER_PARAMS;
    }

    public static void drawElements(int mode, int count, int type, long indicesOffset) {
        if (!ExternalRenderPathSupport.shouldDrawExternalLodBridge()) {
            record(DrawOutcome.BRIDGE_DISABLED);
            return;
        }

        if (mode != GL11.GL_TRIANGLES || count <= 0) {
            if (DEBUG_DRAW_ENABLED) {
                record(DrawOutcome.UNSUPPORTED_MODE_OR_COUNT, "mode=%d count=%d".formatted(mode, count));
            }
            return;
        }

        try {
            if (Renderer.getInstance() == null || !Renderer.isRecording()) {
                record(DrawOutcome.NOT_RECORDING);
                return;
            }
        } catch (NullPointerException ignored) {
            if (DEBUG_DRAW_ENABLED) {
                record(DrawOutcome.NOT_RECORDING, "renderer missing");
            }
            return;
        }

        GlBuffer vertexGlBuffer = GlBuffer.getArrayBufferBound();
        GlBuffer indexGlBuffer = GlBuffer.getElementArrayBufferBound();
        if (vertexGlBuffer == null || indexGlBuffer == null || vertexGlBuffer.getSize() == 0 || indexGlBuffer.getSize() == 0) {
            if (DEBUG_DRAW_ENABLED) {
                record(DrawOutcome.MISSING_BUFFERS, "vertex=%s index=%s".formatted(bufferInfo(vertexGlBuffer), bufferInfo(indexGlBuffer)));
            }
            return;
        }

        int vertexCount = vertexGlBuffer.getSize() / EXTERNAL_LOD_VERTEX_SIZE_BYTES;
        if (vertexCount <= 0) {
            if (DEBUG_DRAW_ENABLED) {
                record(DrawOutcome.EMPTY_VERTEX_BUFFER, "vertexBytes=%d".formatted(vertexGlBuffer.getSize()));
            }
            return;
        }

        IndexBuffer.IndexType indexType = switch (type) {
            case GL11.GL_UNSIGNED_INT -> IndexBuffer.IndexType.INT;
            case GL11.GL_UNSIGNED_SHORT -> IndexBuffer.IndexType.SHORT;
            default -> null;
        };
        if (indexType == null) {
            if (DEBUG_DRAW_ENABLED) {
                record(DrawOutcome.UNSUPPORTED_INDEX_TYPE, "type=%d".formatted(type));
            }
            return;
        }

        int indexBytes = Math.min(count * indexType.size, Math.max(0, indexGlBuffer.getSize() - (int) indicesOffset));
        if (indexBytes <= 0) {
            if (DEBUG_DRAW_ENABLED) {
                record(DrawOutcome.EMPTY_INDEX_RANGE, "count=%d offset=%d indexBytes=%d".formatted(count, indicesOffset, indexGlBuffer.getSize()));
            }
            return;
        }
        if (indexBytes % indexType.size != 0) {
            if (DEBUG_DRAW_ENABLED) {
                record(DrawOutcome.MISALIGNED_INDEX_RANGE, "bytes=%d indexSize=%d".formatted(indexBytes, indexType.size));
            }
            return;
        }

        syncUniforms();

        if (ExternalRenderPathSupport.shouldDrawExternalLodBridgeDirectlyToMainFramebuffer()) {
            GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }

        GraphicsPipeline pipeline = PipelineManager.getExternalLodPipeline();
        if (pipeline == null) {
            record(DrawOutcome.MISSING_PIPELINE);
            return;
        }

        IndexUpload reusableIndexUpload = getReusableIndexUpload(indexGlBuffer, indexType, (int) indicesOffset, indexBytes, vertexCount);
        ByteBuffer indexSlice = null;
        int maxIndex = -1;
        if (reusableIndexUpload == null) {
            ByteBuffer indexData = indexGlBuffer.copyData();
            indexData.position((int) indicesOffset);
            indexData.limit((int) indicesOffset + indexBytes);
            indexSlice = indexData.slice();
            indexSlice.order(indexData.order());
            maxIndex = maxIndexWithinVertexBounds(indexSlice, indexType, vertexCount);
            if (maxIndex < 0) {
                if (DEBUG_DRAW_ENABLED) {
                    record(DrawOutcome.OUT_OF_BOUNDS_INDICES, "vertices=%d indexBytes=%d".formatted(vertexCount, indexBytes));
                }
                return;
            }
        }

        VertexBuffer vertexBuffer = getOrUploadVertexBuffer(vertexGlBuffer, vertexCount);
        IndexBuffer indexBuffer = reusableIndexUpload != null
                ? reusableIndexUpload.buffer()
                : uploadIndexBuffer(indexGlBuffer, indexType, (int) indicesOffset, indexBytes, indexSlice, maxIndex);

        VRenderSystem.setPrimitiveTopologyGL(mode);
        Renderer renderer = Renderer.getInstance();
        renderer.bindGraphicsPipeline(pipeline);
        renderer.uploadAndBindUBOs(pipeline);
        Renderer.getDrawer().drawIndexed(vertexBuffer, indexBuffer, indexBytes / indexType.size);
        if (DEBUG_DRAW_ENABLED) {
            record(DrawOutcome.SUBMITTED, "vertices=%d indices=%d type=%s offset=%d matrix=%s model=%s clip=%.2f dither=%s".formatted(
                    vertexCount,
                    indexBytes / indexType.size,
                    indexType,
                    indicesOffset,
                    GlProgram.getFloatUniform("uCombinedMatrix") != null,
                    GlProgram.getFloatUniform("uModelOffset") != null,
                    RENDER_PARAMS.buffer.getFloat(4),
                    getBooleanUniform("uDitherExternalLodRendering")
            ));
        }
    }

    public static void drawArrays(int mode, int first, int count) {

    }

    public static void onBufferDeleted(int id) {
        VertexUpload vertexUpload = VERTEX_UPLOADS.remove(id);
        if (vertexUpload != null) {
            vertexUpload.buffer.freeBuffer();
        }

        IndexUpload indexUpload = INDEX_UPLOADS.remove(id);
        if (indexUpload != null) {
            indexUpload.buffer.freeBuffer();
        }
    }

    private static VertexBuffer getOrUploadVertexBuffer(GlBuffer glBuffer, int vertexCount) {
        VertexUpload upload = VERTEX_UPLOADS.get(glBuffer.getId());
        if (upload != null && upload.version == glBuffer.getVersion()) {
            return upload.buffer;
        }

        if (upload != null) {
            upload.buffer.freeBuffer();
        }

        VertexBuffer vertexBuffer = new VertexBuffer(glBuffer.getSize(), MemoryTypes.HOST_MEM);
        vertexBuffer.copyToVertexBuffer(CustomVertexFormat.EXTERNAL_LOD.getVertexSize(), vertexCount, glBuffer.copyData());
        VERTEX_UPLOADS.put(glBuffer.getId(), new VertexUpload(vertexBuffer, glBuffer.getVersion()));
        return vertexBuffer;
    }

    private static IndexUpload getReusableIndexUpload(GlBuffer glBuffer, IndexBuffer.IndexType indexType, int offset, int size, int vertexCount) {
        IndexUpload upload = INDEX_UPLOADS.get(glBuffer.getId());
        return isReusableIndexUpload(upload, glBuffer.getVersion(), offset, size, indexType, vertexCount) ? upload : null;
    }

    static boolean isReusableIndexUpload(IndexUpload upload, int version, int offset, int size, IndexBuffer.IndexType indexType, int vertexCount) {
        return upload != null
                && upload.version == version
                && upload.offset == offset
                && upload.size == size
                && upload.type == indexType
                && upload.maxIndex < vertexCount;
    }

    private static IndexBuffer uploadIndexBuffer(GlBuffer glBuffer, IndexBuffer.IndexType indexType, int offset, int size, ByteBuffer indexData, int maxIndex) {
        IndexUpload upload = INDEX_UPLOADS.get(glBuffer.getId());
        if (upload != null) {
            upload.buffer.freeBuffer();
        }

        IndexBuffer indexBuffer = new IndexBuffer(size, MemoryTypes.HOST_MEM, indexType);
        ByteBuffer indexCopy = indexData.duplicate();
        indexCopy.position(0);
        indexBuffer.copyBuffer(indexCopy);
        INDEX_UPLOADS.put(glBuffer.getId(), new IndexUpload(indexBuffer, glBuffer.getVersion(), offset, size, indexType, maxIndex));
        return indexBuffer;
    }

    private static void syncUniforms() {
        float[] matrix = GlProgram.getFloatUniform("uCombinedMatrix");
        if (matrix != null && matrix.length >= 16) {
            FloatBuffer dst = COMBINED_MATRIX.buffer.asFloatBuffer();
            dst.position(0);
            dst.put(matrix, 0, 16);
        } else {
            ByteBuffer src = VRenderSystem.getMVP().buffer.duplicate();
            src.position(0);
            src.limit(16 * Float.BYTES);
            COMBINED_MATRIX.buffer.position(0);
            COMBINED_MATRIX.buffer.put(src);
            COMBINED_MATRIX.buffer.position(0);
        }

        writeModelOffsetAndYOffset(
                MODEL_OFFSET_AND_Y_OFFSET,
                GlProgram.getFloatUniform("uModelOffset"),
                GlProgram.getFloatUniform("uCameraPos"),
                GlProgram.getFloatUniform("uWorldYOffset"));

        RENDER_PARAMS.putFloat(0, getFloat(GlProgram.getFloatUniform("uMircoOffset"), 0, 0.01f));
        RENDER_PARAMS.putFloat(4, getFloat(GlProgram.getFloatUniform("uClipDistance"), 0, 0.0f));
        RENDER_PARAMS.putFloat(8, getBooleanUniform("uIsWhiteWorld") ? 1.0f : 0.0f);
        RENDER_PARAMS.putFloat(12, getBooleanUniform("uDitherExternalLodRendering") ? 1.0f : 0.0f);
    }

    static void writeModelOffsetAndYOffset(MappedBuffer dst, float[] modelOffset, float[] cameraPos, float[] worldYOffset) {
        dst.putFloat(0, getFloat(modelOffset, 0, 0.0f) - getFloat(cameraPos, 0, 0.0f));
        dst.putFloat(4, getFloat(modelOffset, 1, 0.0f) - getFloat(cameraPos, 1, 0.0f));
        dst.putFloat(8, getFloat(modelOffset, 2, 0.0f) - getFloat(cameraPos, 2, 0.0f));
        dst.putFloat(12, getFloat(worldYOffset, 0, 0.0f));
    }

    static boolean indicesWithinVertexBounds(ByteBuffer indexData, IndexBuffer.IndexType indexType, int vertexCount) {
        return maxIndexWithinVertexBounds(indexData, indexType, vertexCount) >= 0;
    }

    static int maxIndexWithinVertexBounds(ByteBuffer indexData, IndexBuffer.IndexType indexType, int vertexCount) {
        if (vertexCount <= 0) {
            return -1;
        }

        ByteBuffer indices = indexData.duplicate();
        indices.order(indexData.order());
        int maxIndex = -1;
        while (indices.remaining() >= indexType.size) {
            long index = switch (indexType) {
                case SHORT -> Short.toUnsignedInt(indices.getShort());
                case INT -> Integer.toUnsignedLong(indices.getInt());
            };

            if (index >= vertexCount) {
                return -1;
            }

            if (index > maxIndex) {
                maxIndex = (int) index;
            }
        }

        return indices.hasRemaining() ? -1 : maxIndex;
    }

    private static float getFloat(float[] values, int index, float fallback) {
        return values != null && values.length > index ? values[index] : fallback;
    }

    private static boolean getBooleanUniform(String name) {
        int[] ints = GlProgram.getIntUniform(name);
        if (ints != null && ints.length > 0) {
            return ints[0] != 0;
        }

        float[] floats = GlProgram.getFloatUniform(name);
        return floats != null && floats.length > 0 && floats[0] != 0.0f;
    }

    private static String bufferInfo(GlBuffer buffer) {
        return buffer == null ? "null" : "id=%d size=%d".formatted(buffer.getId(), buffer.getSize());
    }

    private static void record(DrawOutcome outcome) {
        record(outcome, "");
    }

    private static void record(DrawOutcome outcome, String detail) {
        if (!DEBUG_DRAW_ENABLED) {
            return;
        }
        long now = System.currentTimeMillis();
        if (record(DIAGNOSTICS, outcome, detail, true, now)) {
            System.out.println("[VulkanMod][external_lod] bridge " + DIAGNOSTICS.summary());
        }
    }

    static boolean record(DrawDiagnostics diagnostics, DrawOutcome outcome, String detail, boolean debugEnabled, long nowMs) {
        if (!debugEnabled) {
            return false;
        }
        boolean shouldLog = diagnostics.shouldLog(nowMs, DIAGNOSTIC_INTERVAL_MS);
        diagnostics.record(outcome, shouldLog && detail != null ? detail : "");
        return shouldLog;
    }

    enum DrawOutcome {
        SUBMITTED,
        BRIDGE_DISABLED,
        UNSUPPORTED_MODE_OR_COUNT,
        NOT_RECORDING,
        MISSING_BUFFERS,
        EMPTY_VERTEX_BUFFER,
        UNSUPPORTED_INDEX_TYPE,
        EMPTY_INDEX_RANGE,
        MISALIGNED_INDEX_RANGE,
        OUT_OF_BOUNDS_INDICES,
        MISSING_PIPELINE
    }

    static final class DrawDiagnostics {
        private final long[] counts = new long[DrawOutcome.values().length];
        private DrawOutcome lastOutcome;
        private String lastDetail = "";
        private long lastLogMs;

        void record(DrawOutcome outcome, String detail) {
            counts[outcome.ordinal()]++;
            lastOutcome = outcome;
            lastDetail = detail == null ? "" : detail;
        }

        long count(DrawOutcome outcome) {
            return counts[outcome.ordinal()];
        }

        boolean shouldLog(long nowMs, long intervalMs) {
            if (nowMs - lastLogMs < intervalMs) {
                return false;
            }

            lastLogMs = nowMs;
            return true;
        }

        String summary() {
            long submitted = count(DrawOutcome.SUBMITTED);
            long skipped = 0;
            for (DrawOutcome outcome : DrawOutcome.values()) {
                if (outcome != DrawOutcome.SUBMITTED) {
                    skipped += count(outcome);
                }
            }

            String last = lastOutcome == null ? "none" : lastOutcome.name();
            String detail = lastDetail.isBlank() ? "" : " " + lastDetail;
            return "submitted=%d skipped=%d last=%s%s".formatted(submitted, skipped, last, detail);
        }
    }

    private record VertexUpload(VertexBuffer buffer, int version) {
    }

    record IndexUpload(IndexBuffer buffer, int version, int offset, int size, IndexBuffer.IndexType type, int maxIndex) {
    }
}
