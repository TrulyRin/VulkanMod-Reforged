package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class GlBuffer {
    private static final int GL_COPY_READ_BUFFER_BINDING = 0x8F36;
    private static final int GL_COPY_WRITE_BUFFER_BINDING = 0x8F37;

    private static final int GL_BUFFER_SIZE = 0x8764;
    private static final int GL_BUFFER_USAGE = 0x8765;
    private static final int GL_BUFFER_MAPPED = 0x88BC;
    private static final int GL_STATIC_DRAW = 0x88E4;

    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlBuffer> map = new Int2ReferenceOpenHashMap<>();

    private static GlBuffer arrayBufferBound;
    private static GlBuffer elementArrayBufferBound;
    private static GlBuffer pixelPackBufferBound;
    private static GlBuffer pixelUnpackBufferBound;
    private static GlBuffer copyReadBufferBound;
    private static GlBuffer copyWriteBufferBound;

    private static final Int2ReferenceOpenHashMap<GlBuffer> otherTargetBindings = new Int2ReferenceOpenHashMap<>();

    public static int glGenBuffers() {
        int id = ID_COUNTER;
        map.put(id, new GlBuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void glBindBuffer(int target, int buffer) {
        GlBuffer glBuffer = map.get(buffer);

        if (buffer > 0 && glBuffer == null) {
            glBuffer = new GlBuffer(buffer);
            map.put(buffer, glBuffer);
            if (buffer >= ID_COUNTER)
                ID_COUNTER = buffer + 1;
        }

        if (glBuffer != null) {
            glBuffer.target = target;
        }

        switch (target) {
            case GL32.GL_ARRAY_BUFFER -> arrayBufferBound = glBuffer;
            case GL32.GL_ELEMENT_ARRAY_BUFFER -> elementArrayBufferBound = glBuffer;
            case GL32.GL_PIXEL_PACK_BUFFER -> pixelPackBufferBound = glBuffer;
            case GL32.GL_PIXEL_UNPACK_BUFFER -> pixelUnpackBufferBound = glBuffer;
            case GL32.GL_COPY_READ_BUFFER -> copyReadBufferBound = glBuffer;
            case GL32.GL_COPY_WRITE_BUFFER -> copyWriteBufferBound = glBuffer;
            default -> {
                if (glBuffer == null) {
                    otherTargetBindings.remove(target);
                } else {
                    otherTargetBindings.put(target, glBuffer);
                }
            }
        }
    }

    public static void glBufferData(int target, ByteBuffer byteBuffer, int usage) {
        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null) {
            return;
        }

        if (byteBuffer == null) {
            buffer.allocate(0);
        } else {
            ByteBuffer src = byteBuffer.slice();
            buffer.allocate(src.remaining());
            buffer.data.position(0);
            buffer.data.put(src);
            buffer.data.position(0);
        }

        buffer.version++;
    }

    public static void glBufferData(int target, long size, int usage) {
        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null) {
            return;
        }

        buffer.allocate((int) size);
        buffer.version++;
    }

    public static void glBufferData(int target, ShortBuffer shortBuffer, int usage) {
        ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(MemoryUtil.memAddress(shortBuffer), shortBuffer.remaining() * Short.BYTES);
        glBufferData(target, byteBuffer, usage);
    }

    public static void glBufferData(int target, int[] ints, int usage) {
        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null) {
            return;
        }

        int size = ints == null ? 0 : ints.length * Integer.BYTES;
        buffer.allocate(size);
        if (ints != null) {
            buffer.data.asIntBuffer().put(ints);
        }
        buffer.version++;
    }

    public static void glBufferData(int target, float[] floats, int usage) {
        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null) {
            return;
        }

        int size = floats == null ? 0 : floats.length * Float.BYTES;
        buffer.allocate(size);
        if (floats != null) {
            buffer.data.asFloatBuffer().put(floats);
        }
        buffer.version++;
    }

    public static void glBufferSubData(int target, long offset, ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return;
        }

        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null) {
            return;
        }

        ByteBuffer src = byteBuffer.slice();
        int end = (int) offset + src.remaining();
        if (buffer.data == null || end > buffer.data.capacity()) {
            ByteBuffer oldData = buffer.data;
            int oldSize = buffer.size;
            buffer.data = MemoryUtil.memAlloc(end);
            buffer.size = end;
            if (oldData != null) {
                ByteBuffer oldCopy = oldData.duplicate();
                oldCopy.position(0);
                oldCopy.limit(oldSize);
                buffer.data.put(oldCopy);
                MemoryUtil.memFree(oldData);
            }
            buffer.data.position(0);
        }

        buffer.data.position((int) offset);
        buffer.data.put(src);
        buffer.data.position(0);
        buffer.size = Math.max(buffer.size, end);
        buffer.version++;
    }

    public static void glBufferStorage(int target, ByteBuffer byteBuffer, int flags) {
        glBufferData(target, byteBuffer, GL32.GL_STATIC_DRAW);
    }

    public static boolean glIsBuffer(int id) {
        return id == 0 || map.containsKey(id);
    }

    public static ByteBuffer glMapBuffer(int target, int access) {
        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null) {

            return null;
        }

        ByteBuffer mappedBuffer = buffer.data;
        if (mappedBuffer == null) {
            buffer.allocate(0);
            mappedBuffer = buffer.data;
        }
        mappedBuffer.position(0);
        return mappedBuffer;
    }

    public static boolean glUnmapBuffer(int i) {
        GlBuffer buffer = getBoundBuffer(i);
        if (buffer != null) {
            buffer.version++;
        }
        return true;
    }

    public static void glDeleteBuffers(IntBuffer intBuffer) {
        for (int i = intBuffer.position(); i < intBuffer.limit(); i++) {
            glDeleteBuffers(intBuffer.get(i));
        }
    }

    public static void glDeleteBuffers(int id) {
        var buffer = map.remove(id);

        if (buffer != null) {
            buffer.freeData();
            ExternalTerrainRenderBridge.onBufferDeleted(id);
        }

        if (arrayBufferBound == buffer) arrayBufferBound = null;
        if (elementArrayBufferBound == buffer) elementArrayBufferBound = null;
        if (pixelPackBufferBound == buffer) pixelPackBufferBound = null;
        if (pixelUnpackBufferBound == buffer) pixelUnpackBufferBound = null;
        if (copyReadBufferBound == buffer) copyReadBufferBound = null;
        if (copyWriteBufferBound == buffer) copyWriteBufferBound = null;
        if (buffer != null) {
            otherTargetBindings.values().removeIf(bound -> bound == buffer);
        }
    }

    public static GlBuffer getPixelUnpackBufferBound() {
        return pixelUnpackBufferBound;
    }

    public static GlBuffer getPixelPackBufferBound() {
        return pixelPackBufferBound;
    }

    public static GlBuffer getArrayBufferBound() {
        return arrayBufferBound;
    }

    public static GlBuffer getElementArrayBufferBound() {
        return elementArrayBufferBound;
    }

    public static int getBoundId(int pname) {
        GlBuffer buffer = switch (pname) {
            case GL32.GL_ARRAY_BUFFER_BINDING -> arrayBufferBound;
            case GL32.GL_ELEMENT_ARRAY_BUFFER_BINDING -> elementArrayBufferBound;
            case GL32.GL_PIXEL_PACK_BUFFER_BINDING -> pixelPackBufferBound;
            case GL32.GL_PIXEL_UNPACK_BUFFER_BINDING -> pixelUnpackBufferBound;
            case GL_COPY_READ_BUFFER_BINDING -> copyReadBufferBound;
            case GL_COPY_WRITE_BUFFER_BINDING -> copyWriteBufferBound;
            default -> null;
        };

        return buffer != null ? buffer.id : 0;
    }

    private static GlBuffer getRequiredBoundBuffer(int target) {
        GlBuffer buffer = getBoundBuffer(target);
        if (buffer == null) {
            GlEmulationLog.warnOnce("buffer.unbound." + target,
                    "Buffer operation on target 0x{} with no bound buffer; operation skipped", Integer.toHexString(target));
        }
        return buffer;
    }

    private static GlBuffer getBoundBuffer(int target) {
        return switch (target) {
            case GL32.GL_ARRAY_BUFFER -> arrayBufferBound;
            case GL32.GL_ELEMENT_ARRAY_BUFFER -> elementArrayBufferBound;
            case GL32.GL_PIXEL_PACK_BUFFER -> pixelPackBufferBound;
            case GL32.GL_PIXEL_UNPACK_BUFFER -> pixelUnpackBufferBound;
            case GL32.GL_COPY_READ_BUFFER -> copyReadBufferBound;
            case GL32.GL_COPY_WRITE_BUFFER -> copyWriteBufferBound;
            default -> otherTargetBindings.get(target);
        };
    }

    public static int glGetBufferParameteri(int target, int pname) {
        GlBuffer buffer = getBoundBuffer(target);
        if (buffer == null) {
            return 0;
        }

        return switch (pname) {
            case GL_BUFFER_SIZE -> buffer.size;
            case GL_BUFFER_USAGE -> GL_STATIC_DRAW;
            case GL_BUFFER_MAPPED -> 0;
            default -> 0;
        };
    }

    public static void glGetBufferSubData(int target, long offset, ByteBuffer dest) {
        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null || buffer.data == null || dest == null) {
            return;
        }

        int length = Math.min(dest.remaining(), Math.max(buffer.size - (int) offset, 0));
        if (length <= 0) {
            return;
        }

        ByteBuffer src = buffer.data.duplicate();
        src.position((int) offset);
        src.limit((int) offset + length);
        dest.duplicate().put(src);
    }

    public static void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        GlBuffer read = getRequiredBoundBuffer(readTarget);
        GlBuffer write = getRequiredBoundBuffer(writeTarget);
        if (read == null || write == null || read.data == null || size <= 0) {
            return;
        }

        int length = (int) Math.min(size, Math.max(read.size - (int) readOffset, 0));
        if (length <= 0) {
            return;
        }

        ByteBuffer src = read.data.duplicate();
        src.position((int) readOffset);
        src.limit((int) readOffset + length);
        glBufferSubDataInto(write, writeOffset, src);
    }

    public static ByteBuffer glMapBufferRange(int target, long offset, long length, int access) {
        GlBuffer buffer = getRequiredBoundBuffer(target);
        if (buffer == null) {

            return null;
        }

        int end = (int) (offset + length);
        if (buffer.data == null || end > buffer.data.capacity()) {
            buffer.growPreservingData(end);
        }

        return MemoryUtil.memByteBuffer(MemoryUtil.memAddress0(buffer.data) + offset, (int) length);
    }

    public static void namedBufferData(int id, ByteBuffer byteBuffer, int usage) {
        GlBuffer buffer = map.get(id);
        if (buffer == null) {
            return;
        }

        if (byteBuffer == null) {
            buffer.allocate(0);
        } else {
            ByteBuffer src = byteBuffer.slice();
            buffer.allocate(src.remaining());
            buffer.data.position(0);
            buffer.data.put(src);
            buffer.data.position(0);
        }
        buffer.version++;
    }

    public static void namedBufferData(int id, long size, int usage) {
        GlBuffer buffer = map.get(id);
        if (buffer == null) {
            return;
        }

        buffer.allocate((int) size);
        buffer.version++;
    }

    public static void namedBufferSubData(int id, long offset, ByteBuffer byteBuffer) {
        GlBuffer buffer = map.get(id);
        if (buffer == null || byteBuffer == null) {
            return;
        }

        glBufferSubDataInto(buffer, offset, byteBuffer.slice());
    }

    public static ByteBuffer mapNamedBufferRange(int id, long offset, long length) {
        GlBuffer buffer = map.get(id);
        if (buffer == null || length <= 0) {
            return null;
        }

        int end = (int) (offset + length);
        if (buffer.data == null || end > buffer.data.capacity()) {
            buffer.growPreservingData(end);
        }

        return MemoryUtil.memByteBuffer(MemoryUtil.memAddress0(buffer.data) + offset, (int) length);
    }

    public static void copyNamedBufferSubData(int readId, int writeId, long readOffset, long writeOffset, long size) {
        GlBuffer read = map.get(readId);
        GlBuffer write = map.get(writeId);
        if (read == null || write == null || read.data == null || size <= 0) {
            return;
        }

        int length = (int) Math.min(size, Math.max(read.size - (int) readOffset, 0));
        if (length <= 0) {
            return;
        }

        ByteBuffer src = read.data.duplicate();
        src.position((int) readOffset);
        src.limit((int) readOffset + length);
        glBufferSubDataInto(write, writeOffset, src);
    }

    public static ByteBuffer mapNamedBuffer(int id) {
        GlBuffer buffer = map.get(id);
        if (buffer == null) {
            return null;
        }

        if (buffer.data == null) {
            buffer.allocate(0);
        }
        buffer.data.position(0);
        return buffer.data;
    }

    public static boolean unmapNamedBuffer(int id) {
        GlBuffer buffer = map.get(id);
        if (buffer != null) {
            buffer.version++;
        }
        return true;
    }

    private static void glBufferSubDataInto(GlBuffer buffer, long offset, ByteBuffer src) {
        int end = (int) offset + src.remaining();
        if (buffer.data == null || end > buffer.data.capacity()) {
            buffer.growPreservingData(end);
        }

        ByteBuffer dst = buffer.data.duplicate();
        dst.position((int) offset);
        dst.put(src);
        buffer.size = Math.max(buffer.size, end);
        buffer.version++;
    }

    int id;
    int target;
    int size;
    int version;

    ByteBuffer data;

    public GlBuffer(int id) {
        this.id = id;
    }

    private void allocate(int size) {
        if (this.data != null)
            this.freeData();

        this.size = Math.max(size, 0);
        this.data = MemoryUtil.memAlloc(this.size);
    }

    private void growPreservingData(int capacity) {
        ByteBuffer oldData = this.data;
        int oldSize = this.size;
        this.data = MemoryUtil.memAlloc(capacity);
        this.size = capacity;
        if (oldData != null) {
            ByteBuffer oldCopy = oldData.duplicate();
            oldCopy.position(0);
            oldCopy.limit(oldSize);
            this.data.put(oldCopy);
            MemoryUtil.memFree(oldData);
        }
        this.data.position(0);
    }

    public int getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getVersion() {
        return version;
    }

    public ByteBuffer copyData() {
        if (this.data == null) {
            return ByteBuffer.allocateDirect(0);
        }

        ByteBuffer copy = this.data.duplicate();
        copy.order(this.data.order());
        copy.position(0);
        copy.limit(this.size);
        return copy;
    }

    private void freeData() {
        if (data != null) {
            MemoryUtil.memFree(data);
            data = null;
        }
        size = 0;
    }

}
