package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.gl.GlBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


final class DirectVertexBufferState {
    private boolean cached, managed;
    private int managedVertexId, managedIndexId, managedVertexVersion, managedIndexVersion;
    private GlBuffer managedVertices, managedIndices;
    private GlBuffer vertices, indices;
    private int vertexVersion, indexVersion, vertexBytes, indexBytes;
    private VertexFormat format;
    private VertexFormat.Mode mode;
    private VertexFormat.IndexType indexType;
    private boolean sequential;
    private int vertexCount, capacity, selectedCount, convertedCapacity, validatedPrefix;

    void markManaged(int vertexId, int indexId) {
        clear();
        this.managed = true;
        this.managedVertexId = vertexId;
        this.managedIndexId = indexId;
        this.managedVertices = GlBuffer.getById(vertexId);
        this.managedIndices = GlBuffer.getById(indexId);
        this.managedVertexVersion = revision(this.managedVertices);
        this.managedIndexVersion = revision(this.managedIndices);
    }

    boolean usesManaged(int vertexId, int indexId) {
        return this.managed && vertexId == this.managedVertexId && indexId == this.managedIndexId
                && same(GlBuffer.getById(vertexId), this.managedVertices, this.managedVertexVersion)
                && same(GlBuffer.getById(indexId), this.managedIndices, this.managedIndexVersion);
    }


    boolean prepare(int vertexId, int indexId, VertexFormat format, VertexFormat.Mode mode,
                    VertexFormat.IndexType type, boolean sequential, int count) {
        GlBuffer vertices = GlBuffer.getById(vertexId);
        GlBuffer indices = sequential ? null : GlBuffer.getById(indexId);
        if (vertices == null || format == null || mode == null || (!sequential && (indices == null || type == null)))
            throw new IllegalArgumentException("Direct vertex draws require live named buffers and native format/mode metadata");
        if (this.cached && this.vertices == vertices && this.indices == indices
                && this.vertexVersion == vertices.getVersion() && this.indexVersion == revision(indices)
                && this.vertexBytes == vertices.getSize() && this.indexBytes == size(indices)
                && this.format == format && this.mode == mode && this.indexType == type && this.sequential == sequential) {
            requireCount(count, this.capacity);
            if (!sequential && count > this.validatedPrefix) {
                validatePrefix(indices, type, this.vertexCount, this.validatedPrefix, count);
                this.validatedPrefix = count;
            }
            this.selectedCount = count;
            return false;
        }

        this.cached = false;
        int stride = format.getVertexSize();
        int bytes = vertices.getSize();
        if (stride <= 0 || bytes < 0 || bytes % stride != 0)
            throw new IllegalArgumentException("Direct vertex buffer extent is not a whole number of native vertices");
        int vertexCount = bytes / stride;
        int capacity;
        if (sequential) {
            capacity = nativeCapacity(mode, vertexCount);
        } else {
            if (indices.getSize() < 0 || indices.getSize() % type.bytes != 0)
                throw new IllegalArgumentException("Direct index buffer extent does not match its declared index type");
            capacity = indices.getSize() / type.bytes;
        }
        requireCount(count, capacity);
        int convertedCapacity = convertedCount(mode, capacity);
        if (!sequential) {
            validatePrefix(indices, type, vertexCount, 0, count);
            checkedBytes(convertedCapacity, type.bytes);
        }
        this.vertices = vertices;
        this.indices = indices;
        this.vertexVersion = vertices.getVersion();
        this.indexVersion = revision(indices);
        this.vertexBytes = bytes;
        this.indexBytes = size(indices);
        this.format = format;
        this.mode = mode;
        this.indexType = type;
        this.sequential = sequential;
        this.vertexCount = vertexCount;
        this.capacity = capacity;
        this.selectedCount = count;
        this.convertedCapacity = convertedCapacity;
        this.validatedPrefix = sequential ? 0 : count;
        return true;
    }

    void commit() { this.cached = true; this.managed = false; }
    int vertexCount() { return this.vertexCount; }
    int capacity() { return this.capacity; }
    int selectedCount() { return this.selectedCount; }
    int convertedCapacity() { return this.convertedCapacity; }
    int submittedCount() { return convertedCount(this.mode, this.selectedCount); }
    ByteBuffer vertices() { return this.vertices.copyData(); }
    ByteBuffer indices() { return this.indices.copyData().order(ByteOrder.nativeOrder()); }
    boolean convertsIndices() { return convertsIndices(this.mode); }


    void writeConvertedIndices(ByteBuffer target) {
        ByteBuffer source = indices();
        int count = this.capacity;
        if (this.mode == VertexFormat.Mode.TRIANGLE_FAN) {
            for (int i = 1; i + 1 < count; i++) { put(target, source, 0); put(target, source, i); put(target, source, i + 1); }
        } else if (this.mode == VertexFormat.Mode.DEBUG_LINE_STRIP) {
            for (int i = 0; i + 1 < count; i++) { put(target, source, i); put(target, source, i + 1); }
        } else {
            for (int i = 0; i + 2 < count; i++) {
                put(target, source, (i & 1) == 0 ? i : i + 1);
                put(target, source, (i & 1) == 0 ? i + 1 : i);
                put(target, source, i + 2);
            }
        }
        target.flip();
    }

    private void put(ByteBuffer target, ByteBuffer source, int index) {
        if (this.indexType == VertexFormat.IndexType.SHORT) target.putShort(source.getShort(index * Short.BYTES));
        else target.putInt(source.getInt(index * Integer.BYTES));
    }

    private static void validatePrefix(GlBuffer indices, VertexFormat.IndexType type, int vertices, int from, int count) {
        ByteBuffer source = indices.copyData().order(ByteOrder.nativeOrder());
        for (int i = from; i < count; i++) {
            long index = type == VertexFormat.IndexType.SHORT
                    ? Short.toUnsignedInt(source.getShort(i * Short.BYTES))
                    : Integer.toUnsignedLong(source.getInt(i * Integer.BYTES));
            if (index >= vertices) throw new IllegalArgumentException("Direct index references a vertex outside the uploaded buffer");
        }
    }

    void clear() {
        this.cached = this.managed = false;
        this.vertices = this.indices = this.managedVertices = this.managedIndices = null;
        this.format = null;
        this.mode = null;
        this.indexType = null;
    }

    static void requireCount(int count, int capacity) {
        if (count < 0 || count > capacity)
            throw new IllegalArgumentException("Native draw index count exceeds the uploaded geometry");
    }

    static int nativeCapacity(VertexFormat.Mode mode, int vertices) {
        return switch (mode) {
            case QUADS, LINES -> checked((long) (vertices / 4) * 6);
            default -> vertices;
        };
    }

    static int convertedCount(VertexFormat.Mode mode, int nativeCount) {
        return switch (mode) {
            case TRIANGLE_FAN, TRIANGLE_STRIP, LINE_STRIP -> checked(Math.max(0L, (long) nativeCount - 2) * 3);
            case DEBUG_LINE_STRIP -> checked(Math.max(0L, (long) nativeCount - 1) * 2);
            default -> nativeCount;
        };
    }

    static int patchCount(int nativeCount) {
        if (nativeCount < 0 || nativeCount % 6 != 0)
            throw new IllegalArgumentException("Tessellation requires complete native quad index prefixes");
        return checked((long) nativeCount / 6 * 4);
    }

    static int checkedBytes(int count, int width) { return checked((long) count * width); }
    private static int checked(long value) {
        if (value < 0 || value > Integer.MAX_VALUE) throw new IllegalArgumentException("Direct geometry extent exceeds supported integer range");
        return (int) value;
    }
    private static boolean convertsIndices(VertexFormat.Mode mode) {
        return mode == VertexFormat.Mode.TRIANGLE_FAN || mode == VertexFormat.Mode.TRIANGLE_STRIP
                || mode == VertexFormat.Mode.LINE_STRIP || mode == VertexFormat.Mode.DEBUG_LINE_STRIP;
    }
    private static int revision(GlBuffer buffer) { return buffer == null ? -1 : buffer.getVersion(); }
    private static int size(GlBuffer buffer) { return buffer == null ? 0 : buffer.getSize(); }
    private static boolean same(GlBuffer actual, GlBuffer expected, int version) { return actual == expected && revision(actual) == version; }
}
