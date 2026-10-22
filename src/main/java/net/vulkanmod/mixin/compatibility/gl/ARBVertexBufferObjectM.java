package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(ARBVertexBufferObject.class)
public class ARBVertexBufferObjectM {
    @Overwrite(remap = false)
    public static void glBindBufferARB(@NativeType("GLenum") int target, @NativeType("GLuint") int buffer) {
        GlBuffer.glBindBuffer(target, buffer);
    }

    @Overwrite(remap = false)
    public static void glDeleteBuffersARB(@NativeType("GLuint const *") IntBuffer buffers) {
        GlBuffer.glDeleteBuffers(buffers);
    }

    @Overwrite(remap = false)
    public static void glDeleteBuffersARB(@NativeType("GLuint const *") int buffer) {
        GlBuffer.glDeleteBuffers(buffer);
    }

    @Overwrite(remap = false)
    public static void glDeleteBuffersARB(@NativeType("GLuint const *") int[] buffers) {
        if (buffers == null) {
            return;
        }
        for (int buffer : buffers) {
            GlBuffer.glDeleteBuffers(buffer);
        }
    }

    @Overwrite(remap = false)
    public static void glGenBuffersARB(@NativeType("GLuint *") IntBuffer buffers) {
        if (buffers == null) {
            return;
        }
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            buffers.put(i, GlBuffer.glGenBuffers());
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenBuffersARB() {
        return GlBuffer.glGenBuffers();
    }

    @Overwrite(remap = false)
    public static void glGenBuffersARB(@NativeType("GLuint *") int[] buffers) {
        if (buffers == null) {
            return;
        }
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = GlBuffer.glGenBuffers();
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsBufferARB(@NativeType("GLuint") int buffer) {
        return GlBuffer.glIsBuffer(buffer);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, size, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") ByteBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") ShortBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") IntBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, byteBuffer(data, Integer.BYTES), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") FloatBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, byteBuffer(data, Float.BYTES), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") DoubleBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, byteBuffer(data, Double.BYTES), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") short[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data == null ? 0 : (long) data.length * Short.BYTES, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") int[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") float[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferDataARB(@NativeType("GLenum") int target, @NativeType("void const *") double[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data == null ? 0 : (long) data.length * Double.BYTES, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferSubDataARB(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") ByteBuffer data) {
        GlBuffer.glBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glBufferSubDataARB(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") ShortBuffer data) {
        GlBuffer.glBufferSubData(target, offset, byteBuffer(data, Short.BYTES));
    }

    @Overwrite(remap = false)
    public static void glBufferSubDataARB(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") IntBuffer data) {
        GlBuffer.glBufferSubData(target, offset, byteBuffer(data, Integer.BYTES));
    }

    @Overwrite(remap = false)
    public static void glBufferSubDataARB(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") FloatBuffer data) {
        GlBuffer.glBufferSubData(target, offset, byteBuffer(data, Float.BYTES));
    }

    @Overwrite(remap = false)
    public static void glBufferSubDataARB(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") DoubleBuffer data) {
        GlBuffer.glBufferSubData(target, offset, byteBuffer(data, Double.BYTES));
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubDataARB(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") ByteBuffer data) {
        GlBuffer.glGetBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapBufferARB(@NativeType("GLenum") int target, @NativeType("GLenum") int access) {
        return GlBuffer.glMapBuffer(target, access);
    }

    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapBufferARB(@NativeType("GLenum") int target, @NativeType("GLenum") int access, @Nullable ByteBuffer oldBuffer) {
        return GlBuffer.glMapBuffer(target, access);
    }

    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapBufferARB(@NativeType("GLenum") int target, @NativeType("GLenum") int access, @NativeType("GLsizeiptr") long length, @Nullable ByteBuffer oldBuffer) {
        return GlBuffer.glMapBufferRange(target, 0, length, access);
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glUnmapBufferARB(@NativeType("GLenum") int target) {
        return GlBuffer.glUnmapBuffer(target);
    }

    @Overwrite(remap = false)
    public static void glGetBufferParameterivARB(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlBuffer.glGetBufferParameteri(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetBufferParameterivARB(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlBuffer.glGetBufferParameteri(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static int glGetBufferParameteriARB(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlBuffer.glGetBufferParameteri(target, pname);
    }

    @Overwrite(remap = false)
    public static void glGetBufferPointervARB(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("void **") PointerBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), 0L);
        }
    }

    @Overwrite(remap = false)
    @NativeType("void *")
    public static long glGetBufferPointerARB(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return 0L;
    }

    private static ByteBuffer byteBuffer(IntBuffer data, int bytesPerElement) {
        if (data == null) {
            return null;
        }
        return MemoryUtil.memByteBuffer(MemoryUtil.memAddress(data), data.remaining() * bytesPerElement);
    }

    private static ByteBuffer byteBuffer(FloatBuffer data, int bytesPerElement) {
        if (data == null) {
            return null;
        }
        return MemoryUtil.memByteBuffer(MemoryUtil.memAddress(data), data.remaining() * bytesPerElement);
    }

    private static ByteBuffer byteBuffer(ShortBuffer data, int bytesPerElement) {
        if (data == null) {
            return null;
        }
        return MemoryUtil.memByteBuffer(MemoryUtil.memAddress(data), data.remaining() * bytesPerElement);
    }

    private static ByteBuffer byteBuffer(DoubleBuffer data, int bytesPerElement) {
        if (data == null) {
            return null;
        }
        return MemoryUtil.memByteBuffer(MemoryUtil.memAddress(data), data.remaining() * bytesPerElement);
    }
}
