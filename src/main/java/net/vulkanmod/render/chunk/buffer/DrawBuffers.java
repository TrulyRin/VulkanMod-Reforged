package net.vulkanmod.render.chunk.buffer;

import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.memory.IndirectBuffer;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.shader.Pipeline;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public class DrawBuffers {
    private static final int VERTEX_SIZE = PipelineManager.TERRAIN_VERTEX_FORMAT.getVertexSize();
    private static final int INDEX_SIZE = Short.BYTES;
    private static final int SOLID_VERTEX_BUFFER_BYTES = 2_097_152;
    private static final int CUTOUT_VERTEX_BUFFER_BYTES = 2_097_152;
    private static final int CUTOUT_MIPPED_VERTEX_BUFFER_BYTES = 4_194_304;
    private static final int TRANSLUCENT_VERTEX_BUFFER_BYTES = 1_048_576;
    private final int index;
    private final Vector3i origin;
    private final int minHeight;

    private boolean allocated = false;
    AreaBuffer indexBuffer;
    private final EnumMap<TerrainRenderType, AreaBuffer> vertexBuffers = new EnumMap<>(TerrainRenderType.class);

    public DrawBuffers(int index, Vector3i origin, int minHeight) {
        this.index = index;
        this.origin = origin;
        this.minHeight = minHeight;
    }

    public PendingUpload stageUpload(RenderSection section, UploadBuffer buffer,
                                     TerrainRenderType renderType, boolean fullReplacement) {
        DrawParameters current = section.getDrawParameters(renderType);
        DrawParameters staged = new DrawParameters();
        if (!fullReplacement) {
            staged.copyFrom(current);
        }
        PendingUpload pending = new PendingUpload(this, renderType, staged, fullReplacement);

        try {
            int vertexOffset = staged.vertexOffset;
            int firstIndex = -1;

            if (!buffer.indexOnly) {
                AreaBuffer.Segment segment = this.getAreaBufferOrAlloc(renderType)
                        .upload(buffer.getVertexBuffer(), -1, staged);
                vertexOffset = segment.offset / VERTEX_SIZE;
                staged.vertexOffset = vertexOffset;
                pending.uploadedVertex = true;

                staged.baseInstance = encodeSectionOffset(
                        section.xOffset(), section.yOffset(), section.zOffset());
            }

            if (!buffer.autoIndices) {
                if (this.indexBuffer == null)
                    this.indexBuffer = new AreaBuffer(AreaBuffer.Usage.INDEX, 131072, INDEX_SIZE);

                AreaBuffer.Segment segment = this.indexBuffer.upload(buffer.getIndexBuffer(), -1, staged);
                firstIndex = segment.offset / INDEX_SIZE;
                staged.firstIndex = firstIndex;
                pending.uploadedIndex = true;
            }

            staged.indexCount = buffer.indexCount;
            staged.firstIndex = firstIndex;
            staged.vertexOffset = vertexOffset;
            return pending;
        } catch (Throwable throwable) {
            pending.discard();
            throw throwable;
        } finally {
            buffer.release();
        }
    }

    public void commitFullUpdate(RenderSection section,
                                 Map<TerrainRenderType, PendingUpload> stagedLayers) {
        for (TerrainRenderType renderType : TerrainRenderType.VALUES) {
            PendingUpload pending = stagedLayers.get(renderType);
            if (pending == null) {
                this.clearCurrentParameters(section.getDrawParameters(renderType), renderType);
            } else {
                this.commitPending(section.getDrawParameters(renderType), pending);
                stagedLayers.remove(renderType);
            }
        }
    }

    public void commitPartialUpdate(RenderSection section, PendingUpload pending) {
        this.commitPending(section.getDrawParameters(pending.renderType), pending);
    }

    private void commitPending(DrawParameters current, PendingUpload pending) {
        if (pending.owner != this || pending.resolved) {
            throw new IllegalStateException("Invalid staged chunk upload");
        }

        if (pending.uploadedVertex) {
            this.getAreaBuffer(pending.renderType)
                    .rebindSegment(pending.parameters.vertexOffset, current);
        }
        if (pending.uploadedIndex) {
            this.indexBuffer.rebindSegment(pending.parameters.firstIndex, current);
        }

        if (pending.fullReplacement) {
            this.deferCurrentSegments(current, pending.renderType, true, true);
            current.copyFrom(pending.parameters);
        } else {
            if (pending.uploadedVertex) {
                this.deferCurrentSegments(current, pending.renderType, true, false);
                current.vertexOffset = pending.parameters.vertexOffset;
                current.baseInstance = pending.parameters.baseInstance;
            }
            if (pending.uploadedIndex) {
                this.deferCurrentSegments(current, pending.renderType, false, true);
                current.firstIndex = pending.parameters.firstIndex;
            }
            current.indexCount = pending.parameters.indexCount;
        }

        pending.resolved = true;
    }

    private void clearCurrentParameters(DrawParameters current, TerrainRenderType renderType) {
        this.deferCurrentSegments(current, renderType, true, true);
        current.clearValues();
    }

    private void deferCurrentSegments(DrawParameters current, TerrainRenderType renderType,
                                      boolean vertex, boolean index) {
        MemoryManager memoryManager = MemoryManager.getInstance();
        if (vertex && current.vertexOffset != -1) {
            AreaBuffer vertexBuffer = this.getAreaBuffer(renderType);
            if (vertexBuffer != null) {
                memoryManager.addToFreeSegment(vertexBuffer, current.vertexOffset);
            }
        }
        if (index && current.firstIndex != -1 && this.indexBuffer != null) {
            memoryManager.addToFreeSegment(this.indexBuffer, current.firstIndex);
        }
    }

    private AreaBuffer getAreaBufferOrAlloc(TerrainRenderType renderType) {
        this.allocated = true;

        return this.vertexBuffers.computeIfAbsent(
                renderType, renderType1 -> {
                    net.vulkanmod.render.optimization.ChunkFrameDiagnostics.recordAreaBufferCreated();
                    return new AreaBuffer(AreaBuffer.Usage.VERTEX, initialVertexBufferElementCount(renderType), VERTEX_SIZE);
                });
    }

    static int initialVertexBufferElementCount(TerrainRenderType renderType) {
        return Math.max(1, (initialVertexBufferBytes(renderType) + VERTEX_SIZE - 1) / VERTEX_SIZE);
    }

    private static int initialVertexBufferBytes(TerrainRenderType renderType) {
        return switch (renderType) {
            case SOLID -> SOLID_VERTEX_BUFFER_BYTES;
            case CUTOUT -> CUTOUT_VERTEX_BUFFER_BYTES;
            case CUTOUT_MIPPED -> CUTOUT_MIPPED_VERTEX_BUFFER_BYTES;
            case TRANSLUCENT, TRIPWIRE -> TRANSLUCENT_VERTEX_BUFFER_BYTES;
        };
    }

    public AreaBuffer getAreaBuffer(TerrainRenderType r) {
        return this.vertexBuffers.get(r);
    }

    private boolean hasRenderType(TerrainRenderType r) {
        return this.vertexBuffers.containsKey(r);
    }

    private int encodeSectionOffset(int xOffset, int yOffset, int zOffset) {
        final int xOffset1 = (xOffset & 127);
        final int zOffset1 = (zOffset & 127);
        final int yOffset1 = (yOffset - this.minHeight & 127);
        return yOffset1 << 16 | zOffset1 << 8 | xOffset1;
    }

    public static final float POS_OFFSET = PipelineManager.TERRAIN_VERTEX_FORMAT == CustomVertexFormat.COMPRESSED_TERRAIN ? 4.0f : 0.0f;

    private void updateChunkAreaOrigin(VkCommandBuffer commandBuffer, Pipeline pipeline, double camX, double camY, double camZ, MemoryStack stack) {
        float xOffset = (float) ((this.origin.x) + POS_OFFSET - camX);
        float yOffset = (float) ((this.origin.y) + POS_OFFSET - camY);
        float zOffset = (float) ((this.origin.z) + POS_OFFSET - camZ);

        ByteBuffer byteBuffer = stack.malloc(12);

        byteBuffer.putFloat(0, xOffset);
        byteBuffer.putFloat(4, yOffset);
        byteBuffer.putFloat(8, zOffset);

        vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, byteBuffer);
    }

    public void buildDrawBatchesIndirect(IndirectBuffer indirectBuffer, StaticQueue<RenderSection> queue, TerrainRenderType terrainRenderType) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            ByteBuffer byteBuffer = stack.malloc(20 * queue.size());
            long bufferPtr = MemoryUtil.memAddress0(byteBuffer);

            boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;

            int drawCount = 0;

            final int queueSize = queue.size();
            for (int idx = 0; idx < queueSize; idx++) {

                final RenderSection section = queue.get(isTranslucent ? queueSize - 1 - idx : idx);
                final DrawParameters drawParameters = section.getDrawParameters(terrainRenderType);

                if (drawParameters.indexCount <= 0)
                    continue;

                long ptr = bufferPtr + (drawCount * 20L);
                MemoryUtil.memPutInt(ptr, drawParameters.indexCount);
                MemoryUtil.memPutInt(ptr + 4, 1);
                MemoryUtil.memPutInt(ptr + 8, drawParameters.firstIndex == -1 ? 0 : drawParameters.firstIndex);
                MemoryUtil.memPutInt(ptr + 12, drawParameters.vertexOffset);
                MemoryUtil.memPutInt(ptr + 16, drawParameters.baseInstance);

                drawCount++;
            }

            if (drawCount == 0) return;

            indirectBuffer.recordCopyCmd(byteBuffer.position(0));

            vkCmdDrawIndexedIndirect(Renderer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, 20);
        }

    }

    public void buildDrawBatchesDirect(StaticQueue<RenderSection> queue, TerrainRenderType renderType) {
        boolean isTranslucent = renderType == TerrainRenderType.TRANSLUCENT;
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        final int queueSize = queue.size();
        for (int idx = 0; idx < queueSize; idx++) {
            final RenderSection section = queue.get(isTranslucent ? queueSize - 1 - idx : idx);
            final DrawParameters drawParameters = section.getDrawParameters(renderType);

            if (drawParameters.indexCount <= 0)
                continue;

            final int firstIndex = drawParameters.firstIndex == -1 ? 0 : drawParameters.firstIndex;
            vkCmdDrawIndexed(commandBuffer, drawParameters.indexCount, 1, firstIndex, drawParameters.vertexOffset, drawParameters.baseInstance);
        }
    }

    public void bindBuffers(VkCommandBuffer commandBuffer, Pipeline pipeline, TerrainRenderType terrainRenderType, double camX, double camY, double camZ) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var vertexBuffer = getAreaBuffer(terrainRenderType);
            nvkCmdBindVertexBuffers(commandBuffer, 0, 1, stack.npointer(vertexBuffer.getId()), stack.npointer(0));
            updateChunkAreaOrigin(commandBuffer, pipeline, camX, camY, camZ, stack);
        }

        if (terrainRenderType == TerrainRenderType.TRANSLUCENT) {
            vkCmdBindIndexBuffer(commandBuffer, this.indexBuffer.getId(), 0, VK_INDEX_TYPE_UINT16);
        }

    }

    public void releaseBuffers() {
        if (!this.allocated)
            return;

        net.vulkanmod.render.optimization.ChunkFrameDiagnostics.recordAreaReleased();
        this.vertexBuffers.values().forEach(AreaBuffer::freeBuffer);
        this.vertexBuffers.clear();

        if (this.indexBuffer != null)
            this.indexBuffer.freeBuffer();
        this.indexBuffer = null;

        this.allocated = false;
    }

    public boolean isAllocated() {
        return !this.vertexBuffers.isEmpty();
    }

    public EnumMap<TerrainRenderType, AreaBuffer> getVertexBuffers() {
        return vertexBuffers;
    }

    public AreaBuffer getIndexBuffer() {
        return indexBuffer;
    }

    public static final class PendingUpload {
        private final DrawBuffers owner;
        private final TerrainRenderType renderType;
        private final DrawParameters parameters;
        private final boolean fullReplacement;
        private boolean uploadedVertex;
        private boolean uploadedIndex;
        private boolean resolved;

        private PendingUpload(DrawBuffers owner, TerrainRenderType renderType,
                              DrawParameters parameters, boolean fullReplacement) {
            this.owner = owner;
            this.renderType = renderType;
            this.parameters = parameters;
            this.fullReplacement = fullReplacement;
        }

        public void discard() {
            if (this.resolved) {
                return;
            }
            MemoryManager memoryManager = MemoryManager.getInstance();
            if (this.uploadedVertex) {
                AreaBuffer vertexBuffer = this.owner.getAreaBuffer(this.renderType);
                if (vertexBuffer != null) {
                    memoryManager.addToFreeSegment(vertexBuffer, this.parameters.vertexOffset);
                }
            }
            if (this.uploadedIndex && this.owner.indexBuffer != null) {
                memoryManager.addToFreeSegment(this.owner.indexBuffer, this.parameters.firstIndex);
            }
            this.resolved = true;
        }
    }

    public static class DrawParameters {
        int indexCount = 0;
        int firstIndex = -1;
        int vertexOffset = -1;
        int baseInstance;

        public DrawParameters() {}

        private void copyFrom(DrawParameters source) {
            this.indexCount = source.indexCount;
            this.firstIndex = source.firstIndex;
            this.vertexOffset = source.vertexOffset;
            this.baseInstance = source.baseInstance;
        }

        private void clearValues() {
            this.indexCount = 0;
            this.firstIndex = -1;
            this.vertexOffset = -1;
            this.baseInstance = 0;
        }

        public void reset(ChunkArea chunkArea, TerrainRenderType r) {
            AreaBuffer areaBuffer = chunkArea.getDrawBuffers().getAreaBuffer(r);
            if (areaBuffer != null && this.vertexOffset != -1) {
                areaBuffer.setSegmentFree(this.vertexOffset);
            }

            AreaBuffer indexBuffer = chunkArea.getDrawBuffers().getIndexBuffer();
            if (indexBuffer != null && this.firstIndex != -1) {
                indexBuffer.setSegmentFree(this.firstIndex);
            }

            this.clearValues();
        }
    }

}
