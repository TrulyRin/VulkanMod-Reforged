package net.vulkanmod.render.chunk.build;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;


public final class BlockLayerMesh implements AutoCloseable {
    private final ByteBufferBuilder storage;
    private final CopiedMesh mesh;
    private boolean closed;

    private BlockLayerMesh(ByteBufferBuilder storage, CopiedMesh mesh) {
        this.storage = storage;
        this.mesh = mesh;
    }


    public static BlockLayerMesh copyOf(TerrainBufferBuilder.RenderedBuffer source) {
        Objects.requireNonNull(source, "source");
        TerrainBufferBuilder.DrawState state = Objects.requireNonNull(source.drawState(), "drawState");
        if (state.vertexSize() != DefaultVertexFormat.BLOCK.getVertexSize()) {
            throw new IllegalArgumentException("Standalone block geometry requires the BLOCK vertex stride");
        }
        if (state.indexOnly() || state.vertexCount() <= 0 || state.vertexCount() % 4 != 0) {
            throw new IllegalArgumentException("Standalone block geometry requires complete vertex quads");
        }
        if (state.indexType() == null) {
            throw new IllegalArgumentException("Missing block geometry index type");
        }

        final int vertexBytes;
        final int indexBytes;
        final int totalBytes;
        try {
            int expectedIndices = Math.multiplyExact(state.vertexCount() / 4, 6);
            if (state.indexCount() != expectedIndices) {
                throw new IllegalArgumentException("Index count does not describe the full quad extent");
            }
            vertexBytes = Math.multiplyExact(state.vertexCount(), state.vertexSize());
            indexBytes = state.sequentialIndex() ? 0 : Math.multiplyExact(state.indexCount(), state.indexType().bytes);
            totalBytes = Math.addExact(vertexBytes, indexBytes);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Block geometry byte extent overflows", overflow);
        }


        ByteBuffer sourceVertices = source.vertexBuffer();
        requireExtent(sourceVertices, vertexBytes, "vertex");
        ByteBuffer sourceIndices = state.sequentialIndex() ? null : source.indexBuffer();
        if (sourceIndices != null) {
            requireExtent(sourceIndices, indexBytes, "index");
        } else if (indexBytes != 0) {
            throw new IllegalArgumentException("Missing custom block index bytes");
        }

        ByteBufferBuilder storage = new ByteBufferBuilder(totalBytes);
        ByteBufferBuilder.Result vertices = null;
        ByteBufferBuilder.Result indices = null;
        try {
            copyBytes(storage, sourceVertices, vertexBytes);
            vertices = Objects.requireNonNull(storage.build(), "Copied vertex result");
            if (sourceIndices != null) {
                copyBytes(storage, sourceIndices, indexBytes);
                indices = Objects.requireNonNull(storage.build(), "Copied index result");
            }
            MeshData.DrawState drawState = new MeshData.DrawState(DefaultVertexFormat.BLOCK,
                    state.vertexCount(), state.indexCount(), VertexFormat.Mode.QUADS, state.indexType());
            return new BlockLayerMesh(storage, new CopiedMesh(vertices, indices, drawState));
        } catch (RuntimeException | Error failure) {
            closeAfterFailure(indices, failure);
            closeAfterFailure(vertices, failure);
            closeAfterFailure(storage, failure);
            throw failure;
        }
    }

    public MeshData mesh() {
        if (closed) throw new IllegalStateException("Block layer mesh has been closed");
        return mesh;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            mesh.close();
        } finally {
            storage.close();
        }
    }

    private static void requireExtent(ByteBuffer bytes, int expectedBytes, String kind) {
        if (bytes == null || bytes.remaining() != expectedBytes) {
            throw new IllegalArgumentException("Incorrect block " + kind + " byte extent");
        }
    }

    private static void copyBytes(ByteBufferBuilder storage, ByteBuffer source, int size) {
        MemoryUtil.memByteBuffer(storage.reserve(size), size).put(source.duplicate());
    }

    private static void closeAfterFailure(AutoCloseable resource, Throwable failure) {
        if (resource == null) return;
        try {
            resource.close();
        } catch (Throwable cleanupFailure) {
            if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
        }
    }


    private static final class CopiedMesh extends MeshData {
        private final ByteBufferBuilder.Result copiedIndices;
        private boolean closed;

        private CopiedMesh(ByteBufferBuilder.Result vertices, ByteBufferBuilder.Result indices, DrawState state) {
            super(vertices, state);
            this.copiedIndices = indices;
        }

        @Override
        public ByteBuffer vertexBuffer() {
            checkOpen();
            return super.vertexBuffer();
        }

        @Override
        public ByteBuffer indexBuffer() {
            checkOpen();
            ByteBuffer sorted = super.indexBuffer();
            return sorted != null ? sorted : copiedIndices == null ? null : copiedIndices.byteBuffer();
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try {
                super.close();
            } finally {
                if (copiedIndices != null) copiedIndices.close();
            }
        }

        private void checkOpen() {
            if (closed) throw new IllegalStateException("Copied block mesh has been closed");
        }
    }
}
