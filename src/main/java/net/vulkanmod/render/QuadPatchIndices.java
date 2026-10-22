package net.vulkanmod.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;


public final class QuadPatchIndices {
    private QuadPatchIndices() {}

    public enum IndexType {
        U16(Short.BYTES), U32(Integer.BYTES);

        private final int bytes;
        IndexType(int bytes) { this.bytes = bytes; }
        public int bytes() { return bytes; }
    }

    public static int compactedIndexCount(int triangleIndexCount) {
        if (triangleIndexCount < 0 || triangleIndexCount % 6 != 0) {
            throw invalid("Triangle index count must be a nonnegative multiple of six");
        }
        return triangleIndexCount / 6 * 4;
    }

    public static int requiredBytes(int triangleIndexCount, IndexType type) {
        requireType(type);
        return byteCount(compactedIndexCount(triangleIndexCount), type);
    }












    public static int compact(ByteBuffer source, IndexType type, int triangleIndexCount,
                              int vertexCount, ByteBuffer destination) {
        if (source == null || destination == null) throw invalid("Source and destination are required");
        if (source == destination) throw invalid("Source and destination must not be the same buffer");
        requireType(type);
        if (vertexCount < 0) throw invalid("Vertex count must be nonnegative");
        if (destination.isReadOnly()) throw invalid("Destination must be writable");
        int outputCount = compactedIndexCount(triangleIndexCount);
        int sourceBytes = byteCount(triangleIndexCount, type);
        int outputBytes = byteCount(outputCount, type);
        if (source.remaining() < sourceBytes) throw invalid("Source has too few index bytes");
        if (destination.remaining() < outputBytes) throw invalid("Destination has too few index bytes");
        rejectKnownOverlap(source, sourceBytes, destination, outputBytes);

        int width = type.bytes;
        int sourcePosition = source.position();


        for (int quad = 0; quad < triangleIndexCount / 6; quad++) {
            int offset = sourcePosition + quad * 6 * width;
            long a = read(source, type, offset);
            long b = read(source, type, offset + width);
            long c = read(source, type, offset + 2 * width);
            long repeatedC = read(source, type, offset + 3 * width);
            long d = read(source, type, offset + 4 * width);
            long repeatedA = read(source, type, offset + 5 * width);
            if (a >= vertexCount || b >= vertexCount || c >= vertexCount || repeatedC >= vertexCount
                    || d >= vertexCount || repeatedA >= vertexCount) {
                throw invalid("Quad index lies outside the uploaded vertex range");
            }
            if (c != repeatedC || a != repeatedA) {
                throw invalid("Expected each quad's triangle pattern (a,b,c,c,d,a)");
            }

        }

        int destinationPosition = destination.position();
        for (int quad = 0; quad < triangleIndexCount / 6; quad++) {
            int from = sourcePosition + quad * 6 * width;
            int to = destinationPosition + quad * 4 * width;
            write(destination, type, to, read(source, type, from));
            write(destination, type, to + width, read(source, type, from + width));
            write(destination, type, to + 2 * width, read(source, type, from + 2 * width));
            write(destination, type, to + 3 * width, read(source, type, from + 4 * width));
        }
        return outputCount;
    }

    private static void requireType(IndexType type) {
        if (type == null) throw invalid("Index type is required");
    }

    private static int byteCount(int indices, IndexType type) {
        long bytes = (long) indices * type.bytes;
        if (bytes > Integer.MAX_VALUE) throw invalid("Index byte extent exceeds an int-addressable buffer");
        return (int) bytes;
    }

    private static long read(ByteBuffer source, IndexType type, int offset) {
        return type == IndexType.U16 ? Short.toUnsignedInt(source.getShort(offset))
                : Integer.toUnsignedLong(source.getInt(offset));
    }

    private static void write(ByteBuffer destination, IndexType type, int offset, long value) {
        if (type == IndexType.U16) destination.putShort(offset, (short) value);
        else destination.putInt(offset, (int) value);
    }

    private static void rejectKnownOverlap(ByteBuffer source, int sourceBytes,
                                           ByteBuffer destination, int destinationBytes) {
        if (sourceBytes == 0 || destinationBytes == 0) return;
        if (source.isDirect() && destination.isDirect()) {
            if (overlap(MemoryUtil.memAddress(source), sourceBytes,
                    MemoryUtil.memAddress(destination), destinationBytes)) {
                throw invalid("Source and destination direct-memory regions overlap");
            }
        } else if (source.hasArray() && destination.hasArray() && source.array() == destination.array()) {
            if (overlap((long) source.arrayOffset() + source.position(), sourceBytes,
                    (long) destination.arrayOffset() + destination.position(), destinationBytes)) {
                throw invalid("Source and destination heap-array regions overlap");
            }
        }
    }

    private static boolean overlap(long first, int firstBytes, long second, int secondBytes) {
        return first < second + secondBytes && second < first + firstBytes;
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Quad patch indices: " + message);
    }
}
