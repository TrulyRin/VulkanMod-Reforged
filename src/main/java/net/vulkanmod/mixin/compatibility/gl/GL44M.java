package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL44C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(GL44C.class)
public class GL44M {
    private static final int GL_STATIC_DRAW = 0x88E4;

    private static ByteBuffer vulkanmod$copyBytes(ShortBuffer data) {
        if (data == null) {
            return null;
        }
        ShortBuffer src = data.duplicate();
        int byteCount = src.remaining() * Short.BYTES;
        ByteBuffer bytes = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        bytes.asShortBuffer().put(src);
        bytes.limit(byteCount);
        return bytes;
    }

    private static ByteBuffer vulkanmod$copyBytes(IntBuffer data) {
        if (data == null) {
            return null;
        }
        IntBuffer src = data.duplicate();
        int byteCount = src.remaining() * Integer.BYTES;
        ByteBuffer bytes = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        bytes.asIntBuffer().put(src);
        bytes.limit(byteCount);
        return bytes;
    }

    private static ByteBuffer vulkanmod$copyBytes(FloatBuffer data) {
        if (data == null) {
            return null;
        }
        FloatBuffer src = data.duplicate();
        int byteCount = src.remaining() * Float.BYTES;
        ByteBuffer bytes = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        bytes.asFloatBuffer().put(src);
        bytes.limit(byteCount);
        return bytes;
    }

    private static ByteBuffer vulkanmod$copyBytes(DoubleBuffer data) {
        if (data == null) {
            return null;
        }
        DoubleBuffer src = data.duplicate();
        int byteCount = src.remaining() * Double.BYTES;
        ByteBuffer bytes = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        bytes.asDoubleBuffer().put(src);
        bytes.limit(byteCount);
        return bytes;
    }

    private static ByteBuffer vulkanmod$copyBytes(short[] data) {
        if (data == null) {
            return null;
        }
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Short.BYTES).order(ByteOrder.nativeOrder());
        bytes.asShortBuffer().put(data);
        bytes.limit(data.length * Short.BYTES);
        return bytes;
    }

    private static ByteBuffer vulkanmod$copyBytes(int[] data) {
        if (data == null) {
            return null;
        }
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Integer.BYTES).order(ByteOrder.nativeOrder());
        bytes.asIntBuffer().put(data);
        bytes.limit(data.length * Integer.BYTES);
        return bytes;
    }

    private static ByteBuffer vulkanmod$copyBytes(float[] data) {
        if (data == null) {
            return null;
        }
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Float.BYTES).order(ByteOrder.nativeOrder());
        bytes.asFloatBuffer().put(data);
        bytes.limit(data.length * Float.BYTES);
        return bytes;
    }

    private static ByteBuffer vulkanmod$copyBytes(double[] data) {
        if (data == null) {
            return null;
        }
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Double.BYTES).order(ByteOrder.nativeOrder());
        bytes.asDoubleBuffer().put(data);
        bytes.limit(data.length * Double.BYTES);
        return bytes;
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") ByteBuffer data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, data, GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") ShortBuffer data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") IntBuffer data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") FloatBuffer data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") DoubleBuffer data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") short[] data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") int[] data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") float[] data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("void const *") double[] data, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBufferStorage(@NativeType("GLenum") int target, @NativeType("GLsizeiptr") long size, @NativeType("GLbitfield") int flags) {
        GlBuffer.glBufferData(target, size, GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glBindBuffersBase(@NativeType("GLenum") int target, @NativeType("GLuint") int first, @NativeType("GLuint const *") IntBuffer buffers) {
    }

    @Overwrite(remap = false)
    public static void glBindBuffersBase(@NativeType("GLenum") int target, @NativeType("GLuint") int first, @NativeType("GLuint const *") int[] buffers) {
        if (buffers != null && buffers.length > 0) {
            GlBuffer.glBindBuffer(target, buffers[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glBindBuffersRange(@NativeType("GLenum") int target, @NativeType("GLuint") int first, @NativeType("GLuint const *") IntBuffer buffers, @NativeType("GLintptr const *") PointerBuffer offsets, @NativeType("GLsizeiptr const *") PointerBuffer sizes) {
        if (buffers != null && buffers.remaining() > 0) {
            GlBuffer.glBindBuffer(target, buffers.get(buffers.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glBindBuffersRange(@NativeType("GLenum") int target, @NativeType("GLuint") int first, @NativeType("GLuint const *") int[] buffers, @NativeType("GLintptr const *") PointerBuffer offsets, @NativeType("GLsizeiptr const *") PointerBuffer sizes) {
        if (buffers != null && buffers.length > 0) {
            GlBuffer.glBindBuffer(target, buffers[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glBindTextures(@NativeType("GLuint") int first, @NativeType("GLuint const *") IntBuffer textures) {
    }

    @Overwrite(remap = false)
    public static void glBindTextures(@NativeType("GLuint") int first, @NativeType("GLuint const *") int[] textures) {
    }

    @Overwrite(remap = false)
    public static void glBindSamplers(@NativeType("GLuint") int first, @NativeType("GLuint const *") IntBuffer samplers) {
        if (samplers == null) {
            return;
        }
        for (int i = samplers.position(); i < samplers.limit(); i++) {
            net.vulkanmod.gl.GlSampler.bindSampler(first + i - samplers.position(), samplers.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glBindSamplers(@NativeType("GLuint") int first, @NativeType("GLuint const *") int[] samplers) {
        if (samplers == null) {
            return;
        }
        for (int i = 0; i < samplers.length; i++) {
            net.vulkanmod.gl.GlSampler.bindSampler(first + i, samplers[i]);
        }
    }

    @Overwrite(remap = false)
    public static void glBindImageTextures(@NativeType("GLuint") int first, @NativeType("GLuint const *") IntBuffer textures) {
    }

    @Overwrite(remap = false)
    public static void glBindImageTextures(@NativeType("GLuint") int first, @NativeType("GLuint const *") int[] textures) {
    }

    @Overwrite(remap = false)
    public static void glBindVertexBuffers(@NativeType("GLuint") int first, @NativeType("GLuint const *") IntBuffer buffers, @NativeType("GLintptr const *") PointerBuffer offsets, @NativeType("GLsizei const *") IntBuffer strides) {
    }

    @Overwrite(remap = false)
    public static void glBindVertexBuffers(@NativeType("GLuint") int first, @NativeType("GLuint const *") int[] buffers, @NativeType("GLintptr const *") PointerBuffer offsets, @NativeType("GLsizei const *") int[] strides) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ShortBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") IntBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") FloatBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") DoubleBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") short[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") int[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") float[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") double[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ShortBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") IntBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") FloatBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") DoubleBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") short[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") int[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") float[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") double[] data) {
    }
}
