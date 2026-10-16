package net.vulkanmod.compat.external;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

                                                                                                        
public final class ExternalTerrainSubmission<B> implements AutoCloseable {
    public interface Backend<B> {
        B uploadVertices(ByteBuffer data);
        B uploadIndices(ByteBuffer data, int indexBytes);
        void submit(B vertices, B indices, int indexBytes, int count);
        void release(B buffer);
    }

    public enum Outcome {
        DISABLED, NOT_RECORDING, UNSUPPORTED_MODE, UNSUPPORTED_INDEX_TYPE,
        INVALID_VERTEX_DATA, INVALID_INDEX_RANGE, OUT_OF_BOUNDS_INDICES, SUBMITTED
    }

    private static final int PACKED_VERTEX_BYTES = 16;
    private final Backend<B> backend;
    private final Map<Integer, VertexUpload<B>> vertices = new HashMap<>();
    private final Map<Integer, IndexUpload<B>> indices = new HashMap<>();

    public ExternalTerrainSubmission(Backend<B> backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public Outcome draw(boolean enabled, boolean recording, int mode, int count, int indexType, long offset,
                        int vertexId, int vertexVersion, ByteBuffer vertexData,
                        int indexId, int indexVersion, ByteBuffer indexData) {
        if (!enabled) return Outcome.DISABLED;
        if (!recording) return Outcome.NOT_RECORDING;
        if (mode != 4) return Outcome.UNSUPPORTED_MODE;
        int indexBytes = switch (indexType) {
            case 0x1403 -> 2;
            case 0x1405 -> 4;
            default -> 0;
        };
        if (indexBytes == 0) return Outcome.UNSUPPORTED_INDEX_TYPE;
        if (vertexId <= 0 || vertexData == null || vertexData.remaining() == 0
                || vertexData.remaining() % PACKED_VERTEX_BYTES != 0) return Outcome.INVALID_VERTEX_DATA;
        if (indexId <= 0 || indexData == null || count <= 0 || offset < 0
                || offset % indexBytes != 0 || offset >= indexData.remaining()) return Outcome.INVALID_INDEX_RANGE;

        int size = (int) Math.min((long) count * indexBytes, indexData.remaining() - offset);
        if (size <= 0 || size % indexBytes != 0) return Outcome.INVALID_INDEX_RANGE;
        int vertexCount = vertexData.remaining() / PACKED_VERTEX_BYTES;
        IndexUpload<B> indexUpload = this.indices.get(indexId);
        boolean reuseIndices = indexUpload != null && indexUpload.version == indexVersion
                && indexUpload.offset == offset && indexUpload.size == size && indexUpload.indexBytes == indexBytes;
        ByteBuffer indexSlice = null;
        int maxIndex;
        if (reuseIndices) {
            maxIndex = indexUpload.maxIndex;
        } else {
            ByteBuffer slice = indexData.duplicate().order(indexData.order());
            slice.position(slice.position() + (int) offset);
            slice.limit(slice.position() + size);
            indexSlice = slice.slice().order(indexData.order());
            maxIndex = maxIndex(indexSlice, indexBytes, vertexCount);
        }
        if (maxIndex < 0 || maxIndex >= vertexCount) return Outcome.OUT_OF_BOUNDS_INDICES;

        VertexUpload<B> vertexUpload = this.vertices.get(vertexId);
        if (vertexUpload == null || vertexUpload.version != vertexVersion
                || vertexUpload.size != vertexData.remaining()) {
            B uploaded = this.backend.uploadVertices(vertexData.duplicate());
            VertexUpload<B> replacement = new VertexUpload<>(uploaded, vertexVersion, vertexData.remaining());
            this.vertices.put(vertexId, replacement);
            if (vertexUpload != null) this.backend.release(vertexUpload.buffer);
            vertexUpload = replacement;
        }
        if (!reuseIndices) {
            B uploaded = this.backend.uploadIndices(indexSlice, indexBytes);
            IndexUpload<B> replacement = new IndexUpload<>(uploaded, indexVersion, offset, size, indexBytes, maxIndex);
            this.indices.put(indexId, replacement);
            if (indexUpload != null) this.backend.release(indexUpload.buffer);
            indexUpload = replacement;
        }

        this.backend.submit(vertexUpload.buffer, indexUpload.buffer, indexBytes, size / indexBytes);
        return Outcome.SUBMITTED;
    }

    private static int maxIndex(ByteBuffer data, int indexBytes, int vertexCount) {
        ByteBuffer read = data.duplicate().order(data.order());
        int maximum = -1;
        while (read.remaining() >= indexBytes) {
            long index = indexBytes == 2 ? Short.toUnsignedInt(read.getShort()) : Integer.toUnsignedLong(read.getInt());
            if (index >= vertexCount) return -1;
            maximum = Math.max(maximum, (int) index);
        }
        return maximum;
    }

    public void onBufferDeleted(int id) {
        VertexUpload<B> vertex = this.vertices.remove(id);
        IndexUpload<B> index = this.indices.remove(id);
        try {
            if (vertex != null) this.backend.release(vertex.buffer);
        } finally {
            if (index != null) this.backend.release(index.buffer);
        }
    }

    @Override
    public void close() {
        var ownedVertices = this.vertices.values().stream().map(VertexUpload::buffer).toList();
        var ownedIndices = this.indices.values().stream().map(IndexUpload::buffer).toList();
        this.vertices.clear();
        this.indices.clear();
        ownedVertices.forEach(this.backend::release);
        ownedIndices.forEach(this.backend::release);
    }

    private record VertexUpload<B>(B buffer, int version, int size) {}
    private record IndexUpload<B>(B buffer, int version, long offset, int size, int indexBytes, int maxIndex) {}
}
