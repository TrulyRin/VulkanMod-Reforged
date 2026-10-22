package net.vulkanmod.compat.opengl;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlVertexArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL11.*;


final class GlRawDrawPlan {
    final ByteBuffer vertices, indices;
    final MeshData.DrawState state;
    private final BufferVersion vertexSource, indexSource;

    private GlRawDrawPlan(ByteBuffer vertices, ByteBuffer indices, MeshData.DrawState state,
                          BufferVersion vertexSource, BufferVersion indexSource) {
        this.vertices = vertices;
        this.indices = indices;
        this.state = state;
        this.vertexSource = vertexSource;
        this.indexSource = indexSource;
    }

    static GlRawDrawPlan arrays(VertexFormat format, int first, int count) {
        if (first < 0 || count < 0) throw new IllegalArgumentException("Negative raw vertex range");
        VertexSource source = vertexSource(format);
        if (source == null) return null;
        long start = source.base + (long) first * format.getVertexSize();
        long bytes = (long) count * format.getVertexSize();
        requireRange(start, bytes, source.version.size, "vertex");
        return new GlRawDrawPlan(slice(source.version.bytes(), start, bytes), null,
                drawState(format, count, count, VertexFormat.IndexType.SHORT), source.version, null);
    }

    static GlRawDrawPlan elements(VertexFormat format, int count, int type, long offset) {
        if (count < 0) throw new IllegalArgumentException("Negative raw index count");
        int width = type == GL_UNSIGNED_SHORT ? Short.BYTES : type == GL_UNSIGNED_INT ? Integer.BYTES : 0;
        if (width == 0) return null;
        if (offset < 0 || offset % width != 0) throw new IllegalArgumentException("Invalid raw index byte offset");
        VertexSource source = vertexSource(format);
        if (source == null) return null;
        BufferVersion index = new BufferVersion(GlVertexArray.elementBuffer(), "index");
        long bytes = (long) count * width;
        requireRange(offset, bytes, index.size, "index");
        ByteBuffer indices = slice(index.bytes(), offset, bytes);
        long available = (source.version.size - source.base) / format.getVertexSize();
        long maximum = -1;
        for (int i = 0; i < count; i++) {
            long value = width == Short.BYTES ? Short.toUnsignedInt(indices.getShort(i * width))
                    : Integer.toUnsignedLong(indices.getInt(i * width));
            if (value >= available) throw new IllegalArgumentException("Raw index exceeds its vertex source");
            maximum = Math.max(maximum, value);
        }
        int vertexCount = Math.toIntExact(maximum + 1);
        long vertexBytes = (long) vertexCount * format.getVertexSize();
        return new GlRawDrawPlan(slice(source.version.bytes(), source.base, vertexBytes), indices,
                drawState(format, vertexCount, count, width == Short.BYTES ? VertexFormat.IndexType.SHORT : VertexFormat.IndexType.INT),
                source.version, index);
    }

    void requireCurrent() {
        this.vertexSource.requireCurrent();
        if (this.indexSource != null) this.indexSource.requireCurrent();
    }

    private static MeshData.DrawState drawState(VertexFormat format, int vertices, int count, VertexFormat.IndexType type) {

        return new MeshData.DrawState(format, vertices, count, VertexFormat.Mode.TRIANGLES, type);
    }

    private record VertexSource(BufferVersion version, long base) {}

    private static VertexSource vertexSource(VertexFormat format) {
        if (format == null || format.getVertexSize() <= 0) return null;
        var elements = format.getElements();
        var attributes = GlVertexArray.attributes();
        if (elements.isEmpty() || attributes.size() != elements.size()) return null;
        GlBuffer buffer = null;
        long base = -1;
        for (int i = 0; i < elements.size(); i++) {
            var element = elements.get(i);
            var attribute = attributes.get(i);
            if (attribute.buffer() == null || GlBuffer.getById(attribute.buffer().getId()) != attribute.buffer())
                throw new IllegalArgumentException("Raw vertex attribute has no live buffer");
            if (!supported(element) || attribute.location() != i || attribute.size() != element.count()
                    || attribute.type() != glType(element.type()) || attribute.divisor() != 0
                    || attribute.stride() != format.getVertexSize()) return null;
            boolean normalized = element.usage() == VertexFormatElement.Usage.COLOR || element.usage() == VertexFormatElement.Usage.NORMAL;
            boolean integer = element.usage() == VertexFormatElement.Usage.UV && element.type() != VertexFormatElement.Type.FLOAT;
            if (attribute.normalized() != normalized || attribute.integer() != integer) return null;
            long candidate = attribute.offset() - format.getOffset(element);
            if (candidate < 0 || buffer != null && (buffer != attribute.buffer() || base != candidate)) return null;
            buffer = attribute.buffer();
            base = candidate;
        }
        BufferVersion version = new BufferVersion(buffer, "vertex");
        requireRange(base, 0, version.size, "vertex");
        return new VertexSource(version, base);
    }

    private static boolean supported(VertexFormatElement element) {
        return switch (element.usage()) {
            case POSITION -> element.type() == VertexFormatElement.Type.FLOAT && element.count() == 3;
            case COLOR -> element.type() == VertexFormatElement.Type.UBYTE && element.count() == 4;
            case UV -> (element.type() == VertexFormatElement.Type.FLOAT || element.type() == VertexFormatElement.Type.SHORT
                    || element.type() == VertexFormatElement.Type.USHORT) && element.count() == 2;
            case NORMAL -> element.type() == VertexFormatElement.Type.BYTE && element.count() == 3;
            default -> false;
        };
    }

    private static int glType(VertexFormatElement.Type type) {
        return switch (type) {
            case FLOAT -> GL_FLOAT;
            case BYTE -> GL_BYTE;
            case UBYTE -> GL_UNSIGNED_BYTE;
            case SHORT -> GL_SHORT;
            case USHORT -> GL_UNSIGNED_SHORT;
            case INT -> GL_INT;
            case UINT -> GL_UNSIGNED_INT;
        };
    }

    private static void requireRange(long offset, long bytes, int capacity, String kind) {
        if (offset < 0 || bytes < 0 || offset > capacity || bytes > capacity - offset)
            throw new IllegalArgumentException("Raw " + kind + " range exceeds its buffer");
    }

    private static ByteBuffer slice(ByteBuffer source, long offset, long bytes) {
        return source.position(Math.toIntExact(offset)).limit(Math.toIntExact(offset + bytes)).slice().order(ByteOrder.nativeOrder());
    }

    private static final class BufferVersion {
        final GlBuffer buffer;
        final int revision, size;
        BufferVersion(GlBuffer buffer, String kind) {
            if (buffer == null || GlBuffer.getById(buffer.getId()) != buffer)
                throw new IllegalArgumentException("Raw " + kind + " source has no live buffer");
            this.buffer = buffer;
            this.revision = buffer.getVersion();
            this.size = buffer.getSize();
        }
        ByteBuffer bytes() { requireCurrent(); return this.buffer.copyData().order(ByteOrder.nativeOrder()); }
        void requireCurrent() {
            if (GlBuffer.getById(this.buffer.getId()) != this.buffer || this.buffer.getVersion() != this.revision
                    || this.buffer.getSize() != this.size)
                throw new IllegalArgumentException("Raw source changed while preparing its draw");
        }
    }
}
