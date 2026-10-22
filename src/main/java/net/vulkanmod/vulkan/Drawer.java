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
import java.nio.ByteOrder;
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
    private long geometryGeneration;

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
        this.geometryGeneration++;
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
        this.geometryGeneration++;
        this.immediateIndices[currentFrame].reset();
        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers[currentFrame].reset();
        this.patchIndices[currentFrame].reset();
    }


    public void prepare(PreparedDraw range, ByteBuffer vertices, MeshData.DrawState state, ByteBuffer indices) {
        range.invalidate();
        if (state.vertexCount() < 0 || state.indexCount() < 0) {
            throw new IllegalArgumentException("Negative immediate geometry counts");
        }
        long bytes = (long) state.vertexCount() * state.format().getVertexSize();
        if (bytes != 0 && (vertices == null || bytes > vertices.remaining())) {
            throw new IllegalArgumentException("Immediate vertex extent exceeds its source");
        }
        range.mode = state.mode();
        range.indexType = state.indexType();
        range.vertexCount = state.vertexCount();
        range.sourceIndexCount = state.indexCount();
        if (state.vertexCount() != 0) {
            selectIndices(range, indices);
            VertexBuffer buffer = this.vertexBuffers[this.currentFrame];
            buffer.copyToVertexBuffer(state.format().getVertexSize(), state.vertexCount(), vertices);
            range.vertexBuffer = buffer.getId();
            range.vertexOffset = buffer.getOffset();
        }
        range.owner = this;
        range.generation = this.geometryGeneration;
        range.valid = true;
    }


    public void updatePreparedIndices(PreparedDraw range, ByteBuffer indices) {
        requirePrepared(range);
        range.valid = false;
        range.clearPatches();
        selectIndices(range, indices);
        range.valid = true;
    }

    private void selectIndices(PreparedDraw range, ByteBuffer indices) {
        range.customIndices = indices != null;
        range.indexAddress = 0;
        if (indices != null) {
            var stream = this.immediateIndices[this.currentFrame];
            stream.upload(indices, range.indexType == VertexFormat.IndexType.SHORT
                    ? ImmediateIndexStream.Type.U16 : ImmediateIndexStream.Type.U32, range.sourceIndexCount);
            IndexBuffer buffer = stream.buffer();
            range.indexCount = stream.indexCount();
            range.indexBuffer = buffer == null ? 0 : buffer.getId();
            range.indexOffset = stream.offset();
            range.nativeIndexType = range.indexType == VertexFormat.IndexType.SHORT ? VK_INDEX_TYPE_UINT16 : VK_INDEX_TYPE_UINT32;
            if (buffer != null) range.indexAddress = buffer.getMappedAddress() + stream.offset();
            return;
        }
        AutoIndexBuffer automatic;
        long count;
        switch (range.mode) {
            case QUADS -> {
                count = (long) range.vertexCount * 3 / 2;
                automatic = count > AutoIndexBuffer.U16_MAX_INDEX_COUNT ? this.quadsIntIndexBuffer : this.quadsIndexBuffer;
            }
            case LINES -> { count = (long) range.vertexCount * 3 / 2; automatic = this.linesIndexBuffer; }
            case TRIANGLE_FAN, TRIANGLE_STRIP, LINE_STRIP -> {
                count = Math.max(0L, (long) range.vertexCount - 2) * 3;
                automatic = range.mode == VertexFormat.Mode.TRIANGLE_FAN ? this.triangleFanIndexBuffer : this.triangleStripIndexBuffer;
            }
            case DEBUG_LINE_STRIP -> { count = Math.max(0L, (long) range.vertexCount - 1) * 2; automatic = this.debugLineStripIndexBuffer; }
            case TRIANGLES, DEBUG_LINES -> { count = 0; automatic = null; }
            default -> throw new IllegalArgumentException("Unsupported immediate draw mode: " + range.mode);
        }
        range.indexCount = Math.toIntExact(count);
        range.indexBuffer = range.indexOffset = 0;
        if (automatic != null && count != 0) {
            automatic.checkCapacity(range.vertexCount);
            IndexBuffer buffer = automatic.getIndexBuffer();
            range.indexBuffer = buffer.getId();
            range.indexOffset = buffer.getOffset();
            range.nativeIndexType = buffer.indexType.type;
        }
    }


    public void validatePrepared(PreparedDraw range, boolean patches) {
        validatePrepared(range, patches, range.sourceIndexCount);
    }

    public void validatePrepared(PreparedDraw range, boolean patches, int nativeIndexCount) {
        requirePrepared(range);
        if (nativeIndexCount < 0 || nativeIndexCount > range.sourceIndexCount) {
            throw new IllegalArgumentException("Immediate draw prefix exceeds its uploaded index extent");
        }
        if (patches && nativeIndexCount % 6 != 0) {
            throw new IllegalArgumentException("Four-vertex patches require complete quad index groups");
        }
        if (!patches) {
            int selected = preparedIndexCount(range, nativeIndexCount);
            if (range.indexBuffer != 0 ? selected > range.indexCount : nativeIndexCount > range.vertexCount) {
                throw new IllegalArgumentException("Immediate draw prefix exceeds its prepared geometry");
            }
            return;
        }
        PatchGeometry.requireQuads(range.mode, range.vertexCount, range.sourceIndexCount, GlPatchState.getPatchVertices());
        if (nativeIndexCount == 0 || !range.customIndices
                || range.patchPrepared && range.patchSourceCount >= nativeIndexCount) return;
        var stream = this.patchIndices[this.currentFrame];
        int bytes = Math.multiplyExact(nativeIndexCount, range.indexType.bytes);
        ByteBuffer source = MemoryUtil.memByteBuffer(range.indexAddress, bytes).order(ByteOrder.nativeOrder());
        stream.upload(source, PatchGeometry.indexType(range.indexType), nativeIndexCount, range.vertexCount);
        IndexBuffer buffer = stream.buffer();
        range.patchBuffer = buffer == null ? 0 : buffer.getId();
        range.patchOffset = stream.offset();
        range.patchCount = stream.indexCount();
        range.patchType = range.indexType == VertexFormat.IndexType.SHORT ? VK_INDEX_TYPE_UINT16 : VK_INDEX_TYPE_UINT32;
        range.patchSourceCount = nativeIndexCount;
        range.patchPrepared = true;
    }

    private void requirePrepared(PreparedDraw range) {
        if (!range.valid || range.owner != this || range.generation != this.geometryGeneration) {
            throw new IllegalStateException("Immediate geometry is not live in this frame");
        }
    }

    public void drawPrepared(PreparedDraw range, boolean patches) {
        drawPrepared(range, patches, range.sourceIndexCount);
    }

    public void drawPrepared(PreparedDraw range, boolean patches, int nativeIndexCount) {
        validatePrepared(range, patches, nativeIndexCount);
        if (range.vertexCount == 0 || nativeIndexCount == 0) return;
        if (patches) {
            int selectedControls = Math.toIntExact((long) nativeIndexCount / 6 * 4);
            if (range.customIndices) drawIndexed(range.vertexBuffer, range.vertexOffset, range.patchBuffer,
                    range.patchOffset, range.patchType, selectedControls);
            else draw(range.vertexBuffer, range.vertexOffset, selectedControls);
        } else if (range.indexBuffer != 0) {
            drawIndexed(range.vertexBuffer, range.vertexOffset, range.indexBuffer, range.indexOffset,
                    range.nativeIndexType, preparedIndexCount(range, nativeIndexCount));
        } else {
            draw(range.vertexBuffer, range.vertexOffset, nativeIndexCount);
        }
    }

    private static int preparedIndexCount(PreparedDraw range, int nativeIndexCount) {
        if (range.customIndices) return nativeIndexCount;
        return switch (range.mode) {
            case TRIANGLE_FAN, TRIANGLE_STRIP, LINE_STRIP ->
                    Math.toIntExact(Math.max(0L, (long) nativeIndexCount - 2) * 3);
            case DEBUG_LINE_STRIP -> Math.toIntExact(Math.max(0L, (long) nativeIndexCount - 1) * 2);
            default -> nativeIndexCount;
        };
    }

    public void drawIndexed(long vertexBuffer, long vertexOffset, long indexBuffer, long indexOffset, int indexType, int count) {
        VkCommandBuffer commands = Renderer.getCommandBuffer();
        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer);
        VUtil.UNSAFE.putLong(pOffsets, vertexOffset);
        nvkCmdBindVertexBuffers(commands, 0, 1, pBuffers, pOffsets);
        vkCmdBindIndexBuffer(commands, indexBuffer, indexOffset, indexType);
        if (count > 0) Renderer.recordRasterWrite();
        vkCmdDrawIndexed(commands, count, 1, 0, 0, 0);
    }

    public void draw(long vertexBuffer, long vertexOffset, int count) {
        VkCommandBuffer commands = Renderer.getCommandBuffer();
        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer);
        VUtil.UNSAFE.putLong(pOffsets, vertexOffset);
        nvkCmdBindVertexBuffers(commands, 0, 1, pBuffers, pOffsets);
        if (count > 0) Renderer.recordRasterWrite();
        vkCmdDraw(commands, count, 1, 0, 0);
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
        if (indexCount > 0) Renderer.recordRasterWrite();
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
    }

    public void draw(VertexBuffer vertexBuffer, int vertexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        VUtil.UNSAFE.putLong(pBuffers, vertexBuffer.getId());
        VUtil.UNSAFE.putLong(pOffsets, vertexBuffer.getOffset());
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);

        if (vertexCount > 0) Renderer.recordRasterWrite();
        vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
    }

    public void bindIndexBuffer(VkCommandBuffer commandBuffer, IndexBuffer indexBuffer) {
        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getId(), indexBuffer.getOffset(), indexBuffer.indexType.type);
    }

    public void cleanUpResources() {
        this.geometryGeneration++;
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
