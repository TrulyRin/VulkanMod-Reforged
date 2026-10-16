package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.vulkanmod.compat.gl.GlPatchState;
import net.vulkanmod.render.ImmediateIndexStream;
import net.vulkanmod.render.PatchGeometry;
import net.vulkanmod.render.PatchIndexStream;
import net.vulkanmod.render.QuadPatchIndices;
import net.vulkanmod.vulkan.memory.*;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;

public class Drawer {
    private static final int INITIAL_VB_SIZE = 2000000;
    private static final int INITIAL_UB_SIZE = 200000;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);

    private int framesNum;
    private VertexBuffer[] vertexBuffers;
    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer quadsIntIndexBuffer;
    private final AutoIndexBuffer linesIndexBuffer;
    private final AutoIndexBuffer debugLineStripIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer triangleStripIndexBuffer;
    private UniformBuffer[] uniformBuffers;
    private PatchIndexStream<IndexBuffer>[] patchIndices;
    private ImmediateIndexStream<IndexBuffer>[] immediateIndices;

    private static final ImmediateIndexStream.Backend<IndexBuffer> IMMEDIATE_BACKEND = new ImmediateIndexStream.Backend<>() {
        public IndexBuffer create(int capacity, ImmediateIndexStream.Type type) {
            return new IndexBuffer(capacity, MemoryTypes.HOST_MEM, type == ImmediateIndexStream.Type.U16
                    ? IndexBuffer.IndexType.SHORT : IndexBuffer.IndexType.INT);
        }
        public void append(IndexBuffer buffer, ByteBuffer bytes) { buffer.copyBuffer(bytes); }
        public void reset(IndexBuffer buffer) { buffer.reset(); }
        public void retire(IndexBuffer buffer) { buffer.freeBuffer(); }
    };

    private static final PatchIndexStream.Backend<IndexBuffer> PATCH_BACKEND = new PatchIndexStream.Backend<>() {
        public IndexBuffer create(int capacity, QuadPatchIndices.IndexType type) {
            return new IndexBuffer(capacity, MemoryTypes.HOST_MEM, type == QuadPatchIndices.IndexType.U16
                    ? IndexBuffer.IndexType.SHORT : IndexBuffer.IndexType.INT);
        }
        public void append(IndexBuffer buffer, ByteBuffer bytes) { buffer.copyBuffer(bytes); }
        public void reset(IndexBuffer buffer) { buffer.reset(); }
        public void retire(IndexBuffer buffer) { buffer.freeBuffer(); }
        public ByteBuffer allocateStaging(int capacity) { return MemoryUtil.memAlloc(capacity); }
        public void freeStaging(ByteBuffer bytes) { MemoryUtil.memFree(bytes); }
    };

    private int currentFrame;

    public Drawer() {

        this.quadsIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.QUAD_U16_MAX_VERTEX_COUNT, AutoIndexBuffer.DrawType.QUADS);
        this.quadsIntIndexBuffer = new AutoIndexBuffer(100000, AutoIndexBuffer.DrawType.QUADS);
        this.linesIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.LINES);
        this.debugLineStripIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.DEBUG_LINE_STRIP);
        this.triangleFanIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        this.triangleStripIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    @SuppressWarnings("unchecked")
    public void createResources(int framesNum) {
        rethrowImmediateCleanup(closeImmediateIndices());
        this.framesNum = framesNum;

        if (this.vertexBuffers != null) {
            Arrays.stream(this.vertexBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.vertexBuffers = new VertexBuffer[framesNum];
        Arrays.setAll(this.vertexBuffers, i -> new VertexBuffer(INITIAL_VB_SIZE, MemoryTypes.HOST_MEM));

        if (this.uniformBuffers != null) {
            Arrays.stream(this.uniformBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.uniformBuffers = new UniformBuffer[framesNum];
        Arrays.setAll(this.uniformBuffers, i -> new UniformBuffer(INITIAL_UB_SIZE, MemoryTypes.HOST_MEM));

        if (this.patchIndices != null) {
            for (var stream : this.patchIndices) stream.close();
        }
        this.patchIndices = (PatchIndexStream<IndexBuffer>[]) new PatchIndexStream<?>[framesNum];
        Arrays.setAll(this.patchIndices, i -> new PatchIndexStream<>(PATCH_BACKEND, 16384));
        this.immediateIndices = (ImmediateIndexStream<IndexBuffer>[]) new ImmediateIndexStream<?>[framesNum];
        Arrays.setAll(this.immediateIndices, i -> new ImmediateIndexStream<>(IMMEDIATE_BACKEND, 16384));
    }

    public void resetBuffers(int currentFrame) {
        this.immediateIndices[currentFrame].reset();
        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers[currentFrame].reset();
        this.patchIndices[currentFrame].reset();
    }

                                                                                                  
    public void preparePatches(MeshData.DrawState state, ByteBuffer customIndices) {
        PatchGeometry.requireQuads(state.mode(), state.vertexCount(), state.indexCount(), GlPatchState.getPatchVertices());
        if (customIndices != null) {
            this.patchIndices[this.currentFrame].upload(customIndices, PatchGeometry.indexType(state.indexType()),
                    state.indexCount(), state.vertexCount());
        }
    }

    public void drawPatches(ByteBuffer vertices, MeshData.DrawState state, boolean customIndices) {
        VertexBuffer vertexBuffer = this.vertexBuffers[this.currentFrame];
        vertexBuffer.copyToVertexBuffer(state.format().getVertexSize(), state.vertexCount(), vertices);
        if (customIndices) {
            var stream = this.patchIndices[this.currentFrame];
            IndexBuffer indices = stream.buffer();
            if (indices == null || stream.indexCount() == 0) throw new IllegalStateException("Patch indices were not prepared");
            drawIndexed(vertexBuffer, indices, stream.indexCount());
        } else {
            draw(vertexBuffer, state.vertexCount());
        }
    }

                                                                                                        
    public void draw(ByteBuffer vertices, MeshData.DrawState state, ByteBuffer customIndices) {
        if (customIndices == null) {
            draw(vertices, state.mode(), state.format(), state.vertexCount());
            return;
        }
        var stream = this.immediateIndices[this.currentFrame];
        ImmediateIndexStream.Type type = switch (state.indexType()) {
            case SHORT -> ImmediateIndexStream.Type.U16;
            case INT -> ImmediateIndexStream.Type.U32;
        };
                                                                                                          
                                                                                                              
        stream.upload(customIndices, type, state.indexCount());
        if (stream.indexCount() == 0) return;
        VertexBuffer vertexBuffer = this.vertexBuffers[this.currentFrame];
        vertexBuffer.copyToVertexBuffer(state.format().getVertexSize(), state.vertexCount(), vertices);
        drawIndexed(vertexBuffer, stream.buffer(), stream.indexCount());
    }

    public void draw(ByteBuffer buffer, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount) {
        AutoIndexBuffer autoIndexBuffer;
        int indexCount;

        VertexBuffer vertexBuffer = this.vertexBuffers[this.currentFrame];
        vertexBuffer.copyToVertexBuffer(vertexFormat.getVertexSize(), vertexCount, buffer);

        switch (mode) {
            case QUADS -> {
                indexCount = vertexCount * 3 / 2;

                autoIndexBuffer = indexCount > AutoIndexBuffer.U16_MAX_INDEX_COUNT
                        ? this.quadsIntIndexBuffer : this.quadsIndexBuffer;
            }
            case LINES -> {
                autoIndexBuffer = this.linesIndexBuffer;
                indexCount = vertexCount * 3 / 2;
            }
            case TRIANGLE_FAN -> {
                autoIndexBuffer = this.triangleFanIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case TRIANGLE_STRIP -> {
                autoIndexBuffer = this.triangleStripIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case LINE_STRIP -> {
                autoIndexBuffer = this.triangleStripIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case DEBUG_LINE_STRIP -> {
                autoIndexBuffer = this.debugLineStripIndexBuffer;
                indexCount = (vertexCount - 1) * 2;
            }
            case TRIANGLES, DEBUG_LINES -> {
                indexCount = 0;
                autoIndexBuffer = null;
            }
            default -> throw new RuntimeException(String.format("unknown drawMode: %s", mode));
        }

        if (indexCount > 0) {
            autoIndexBuffer.checkCapacity(vertexCount);

            drawIndexed(vertexBuffer, autoIndexBuffer.getIndexBuffer(), indexCount);
        } else {
            draw(vertexBuffer, vertexCount);
        }

    }

    public void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        bindIndexBuffer(commandBuffer, indexBuffer);
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
    }

    public void draw(VertexBuffer vertexBuffer, int vertexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
    }

    public void bindIndexBuffer(VkCommandBuffer commandBuffer, IndexBuffer indexBuffer) {
        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), indexBuffer.indexType.type);
    }

    public void cleanUpResources() {
        Throwable failure = closeImmediateIndices();
        try { cleanUpExistingResources(); }
        catch (RuntimeException | Error cleanup) { failure = combineImmediateCleanup(failure, cleanup); }
        rethrowImmediateCleanup(failure);
    }

    private void cleanUpExistingResources() {
        if (this.patchIndices != null) {
            var previous = this.patchIndices;
            this.patchIndices = null;
            for (var stream : previous) stream.close();
        }
        Buffer buffer;
        for (int i = 0; i < this.framesNum; ++i) {
            buffer = this.vertexBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            buffer = this.uniformBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

        }

        this.quadsIndexBuffer.freeBuffer();
        this.quadsIntIndexBuffer.freeBuffer();
        this.linesIndexBuffer.freeBuffer();
        this.triangleFanIndexBuffer.freeBuffer();
        this.debugLineStripIndexBuffer.freeBuffer();
        this.triangleStripIndexBuffer.freeBuffer();
    }

    private Throwable closeImmediateIndices() {
        var previous = this.immediateIndices;
        this.immediateIndices = null;
        Throwable failure = null;
        if (previous != null) {
            for (var stream : previous) {
                if (stream == null) continue;
                try { stream.close(); }
                catch (RuntimeException | Error cleanup) { failure = combineImmediateCleanup(failure, cleanup); }
            }
        }
        return failure;
    }

    private static Throwable combineImmediateCleanup(Throwable primary, Throwable cleanup) {
        if (primary == null) return cleanup;
        if (primary != cleanup) primary.addSuppressed(cleanup);
        return primary;
    }

    private static void rethrowImmediateCleanup(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

    public AutoIndexBuffer getQuadsIndexBuffer() {
        return this.quadsIndexBuffer;
    }

    public AutoIndexBuffer getLinesIndexBuffer() {
        return this.linesIndexBuffer;
    }

    public AutoIndexBuffer getTriangleFanIndexBuffer() {
        return this.triangleFanIndexBuffer;
    }

    public AutoIndexBuffer getTriangleStripIndexBuffer() {
        return this.triangleStripIndexBuffer;
    }

    public AutoIndexBuffer getDebugLineStripIndexBuffer() {
        return this.debugLineStripIndexBuffer;
    }

    public UniformBuffer getUniformBuffer() {
        return this.uniformBuffers[this.currentFrame];
    }

}
