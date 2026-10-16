package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlQuery;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlSampler;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.gl.GlVertexArray;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(GL45C.class)
public class GL45M {
    private static final int GL_FRAMEBUFFER = 0x8D40;
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_STATIC_DRAW = 0x88E4;
    private static final int GL_BUFFER_SIZE = 0x8764;

    private static ByteBuffer vulkanmod$bytes(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof ByteBuffer buffer) {
            return buffer.slice();
        }

        ByteBuffer bytes;
        if (data instanceof java.nio.ShortBuffer buffer) {
            java.nio.ShortBuffer src = buffer.duplicate();
            bytes = ByteBuffer.allocate(src.remaining() * Short.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asShortBuffer().put(src);
        } else if (data instanceof java.nio.IntBuffer buffer) {
            java.nio.IntBuffer src = buffer.duplicate();
            bytes = ByteBuffer.allocate(src.remaining() * Integer.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asIntBuffer().put(src);
        } else if (data instanceof java.nio.LongBuffer buffer) {
            java.nio.LongBuffer src = buffer.duplicate();
            bytes = ByteBuffer.allocate(src.remaining() * Long.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asLongBuffer().put(src);
        } else if (data instanceof java.nio.FloatBuffer buffer) {
            java.nio.FloatBuffer src = buffer.duplicate();
            bytes = ByteBuffer.allocate(src.remaining() * Float.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asFloatBuffer().put(src);
        } else if (data instanceof java.nio.DoubleBuffer buffer) {
            java.nio.DoubleBuffer src = buffer.duplicate();
            bytes = ByteBuffer.allocate(src.remaining() * Double.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asDoubleBuffer().put(src);
        } else if (data instanceof short[] values) {
            bytes = ByteBuffer.allocate(values.length * Short.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asShortBuffer().put(values);
        } else if (data instanceof int[] values) {
            bytes = ByteBuffer.allocate(values.length * Integer.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asIntBuffer().put(values);
        } else if (data instanceof long[] values) {
            bytes = ByteBuffer.allocate(values.length * Long.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asLongBuffer().put(values);
        } else if (data instanceof float[] values) {
            bytes = ByteBuffer.allocate(values.length * Float.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asFloatBuffer().put(values);
        } else if (data instanceof double[] values) {
            bytes = ByteBuffer.allocate(values.length * Double.BYTES).order(java.nio.ByteOrder.nativeOrder());
            bytes.asDoubleBuffer().put(values);
        } else {
            return null;
        }

        bytes.limit(bytes.capacity());
        bytes.position(0);
        return bytes;
    }

    private static void vulkanmod$zero(Object output) {
        if (output instanceof ByteBuffer buffer) {
            ByteBuffer dst = buffer.duplicate();
            while (dst.hasRemaining()) {
                dst.put((byte) 0);
            }
        } else if (output instanceof java.nio.ShortBuffer buffer) {
            for (int i = buffer.position(); i < buffer.limit(); i++) {
                buffer.put(i, (short) 0);
            }
        } else if (output instanceof java.nio.IntBuffer buffer) {
            for (int i = buffer.position(); i < buffer.limit(); i++) {
                buffer.put(i, 0);
            }
        } else if (output instanceof java.nio.LongBuffer buffer) {
            for (int i = buffer.position(); i < buffer.limit(); i++) {
                buffer.put(i, 0L);
            }
        } else if (output instanceof java.nio.FloatBuffer buffer) {
            for (int i = buffer.position(); i < buffer.limit(); i++) {
                buffer.put(i, 0.0f);
            }
        } else if (output instanceof java.nio.DoubleBuffer buffer) {
            for (int i = buffer.position(); i < buffer.limit(); i++) {
                buffer.put(i, 0.0);
            }
        } else if (output instanceof short[] values) {
            java.util.Arrays.fill(values, (short) 0);
        } else if (output instanceof int[] values) {
            java.util.Arrays.fill(values, 0);
        } else if (output instanceof long[] values) {
            java.util.Arrays.fill(values, 0L);
        } else if (output instanceof float[] values) {
            java.util.Arrays.fill(values, 0.0f);
        } else if (output instanceof double[] values) {
            java.util.Arrays.fill(values, 0.0);
        } else if (output instanceof org.lwjgl.PointerBuffer buffer) {
            for (int i = buffer.position(); i < buffer.limit(); i++) {
                buffer.put(i, 0L);
            }
        }
    }

    private static void vulkanmod$getNamedBufferSubData(int buffer, long offset, Object output) {
        ByteBuffer mapped = GlBuffer.mapNamedBuffer(buffer);
        if (mapped == null || output == null) {
            vulkanmod$zero(output);
            return;
        }

        ByteBuffer src = mapped.duplicate().order(java.nio.ByteOrder.nativeOrder());
        if (offset >= src.capacity()) {
            vulkanmod$zero(output);
            return;
        }
        src.position((int) Math.max(offset, 0L));
        vulkanmod$putBytes(output, src.slice().order(java.nio.ByteOrder.nativeOrder()));
    }

    private static void vulkanmod$putBytes(Object output, ByteBuffer src) {
        if (output instanceof ByteBuffer buffer) {
            ByteBuffer dst = buffer.duplicate();
            int count = Math.min(dst.remaining(), src.remaining());
            ByteBuffer copy = src.duplicate();
            copy.limit(copy.position() + count);
            dst.put(copy);
        } else if (output instanceof java.nio.ShortBuffer buffer) {
            java.nio.ShortBuffer values = src.asShortBuffer();
            int count = Math.min(buffer.remaining(), values.remaining());
            java.nio.ShortBuffer dst = buffer.duplicate();
            for (int i = 0; i < count; i++) {
                dst.put(dst.position() + i, values.get(values.position() + i));
            }
        } else if (output instanceof java.nio.IntBuffer buffer) {
            java.nio.IntBuffer values = src.asIntBuffer();
            int count = Math.min(buffer.remaining(), values.remaining());
            java.nio.IntBuffer dst = buffer.duplicate();
            for (int i = 0; i < count; i++) {
                dst.put(dst.position() + i, values.get(values.position() + i));
            }
        } else if (output instanceof java.nio.LongBuffer buffer) {
            java.nio.LongBuffer values = src.asLongBuffer();
            int count = Math.min(buffer.remaining(), values.remaining());
            java.nio.LongBuffer dst = buffer.duplicate();
            for (int i = 0; i < count; i++) {
                dst.put(dst.position() + i, values.get(values.position() + i));
            }
        } else if (output instanceof java.nio.FloatBuffer buffer) {
            java.nio.FloatBuffer values = src.asFloatBuffer();
            int count = Math.min(buffer.remaining(), values.remaining());
            java.nio.FloatBuffer dst = buffer.duplicate();
            for (int i = 0; i < count; i++) {
                dst.put(dst.position() + i, values.get(values.position() + i));
            }
        } else if (output instanceof java.nio.DoubleBuffer buffer) {
            java.nio.DoubleBuffer values = src.asDoubleBuffer();
            int count = Math.min(buffer.remaining(), values.remaining());
            java.nio.DoubleBuffer dst = buffer.duplicate();
            for (int i = 0; i < count; i++) {
                dst.put(dst.position() + i, values.get(values.position() + i));
            }
        } else if (output instanceof short[] values) {
            java.nio.ShortBuffer typed = src.asShortBuffer();
            typed.get(values, 0, Math.min(values.length, typed.remaining()));
        } else if (output instanceof int[] values) {
            java.nio.IntBuffer typed = src.asIntBuffer();
            typed.get(values, 0, Math.min(values.length, typed.remaining()));
        } else if (output instanceof long[] values) {
            java.nio.LongBuffer typed = src.asLongBuffer();
            typed.get(values, 0, Math.min(values.length, typed.remaining()));
        } else if (output instanceof float[] values) {
            java.nio.FloatBuffer typed = src.asFloatBuffer();
            typed.get(values, 0, Math.min(values.length, typed.remaining()));
        } else if (output instanceof double[] values) {
            java.nio.DoubleBuffer typed = src.asDoubleBuffer();
            typed.get(values, 0, Math.min(values.length, typed.remaining()));
        }
    }

    private static int vulkanmod$getNamedBufferParameteri(int buffer, int pname) {
        if (pname != GL_BUFFER_SIZE) {
            return 0;
        }

        ByteBuffer mapped = GlBuffer.mapNamedBuffer(buffer);
        return mapped != null ? mapped.capacity() : 0;
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glCreateBuffers() {
        return GlBuffer.glGenBuffers();
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glCreateTextures(@NativeType("GLenum") int target) {
        return GlTexture.genTextureId();
    }

    @Overwrite(remap = false)
    public static void glCreateTextures(@NativeType("GLenum") int target, @NativeType("GLuint *") IntBuffer textures) {
        for (int i = textures.position(); i < textures.limit(); i++) {
            textures.put(i, GlTexture.genTextureId());
        }
    }

    @Overwrite(remap = false)
    public static void glCreateTextures(@NativeType("GLenum") int target, @NativeType("GLuint *") int[] textures) {
        for (int i = 0; i < textures.length; i++) {
            textures[i] = GlTexture.genTextureId();
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glCreateFramebuffers() {
        return GlFramebuffer.genFramebufferId();
    }

    @Overwrite(remap = false)
    public static void glCreateFramebuffers(@NativeType("GLuint *") IntBuffer framebuffers) {
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            framebuffers.put(i, GlFramebuffer.genFramebufferId());
        }
    }

    @Overwrite(remap = false)
    public static void glCreateFramebuffers(@NativeType("GLuint *") int[] framebuffers) {
        for (int i = 0; i < framebuffers.length; i++) {
            framebuffers[i] = GlFramebuffer.genFramebufferId();
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glCreateRenderbuffers() {
        return GlRenderbuffer.genId();
    }

    @Overwrite(remap = false)
    public static void glCreateRenderbuffers(@NativeType("GLuint *") IntBuffer renderbuffers) {
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            renderbuffers.put(i, GlRenderbuffer.genId());
        }
    }

    @Overwrite(remap = false)
    public static void glCreateRenderbuffers(@NativeType("GLuint *") int[] renderbuffers) {
        for (int i = 0; i < renderbuffers.length; i++) {
            renderbuffers[i] = GlRenderbuffer.genId();
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glCreateVertexArrays() {
        return GlVertexArray.genVertexArray();
    }

    @Overwrite(remap = false)
    public static void glCreateVertexArrays(@NativeType("GLuint *") IntBuffer arrays) {
        GlVertexArray.genVertexArrays(arrays);
    }

    @Overwrite(remap = false)
    public static void glCreateVertexArrays(@NativeType("GLuint *") int[] arrays) {
        for (int i = 0; i < arrays.length; i++) {
            arrays[i] = GlVertexArray.genVertexArray();
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glCreateSamplers() {
        return GlSampler.genSamplers();
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glCreateQueries(@NativeType("GLenum") int target) {
        return GlQuery.genQueries();
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(@NativeType("GLuint") int buffer, @NativeType("void const *") ByteBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.namedBufferData(buffer, data, usage);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(@NativeType("GLuint") int buffer, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int usage) {
        GlBuffer.namedBufferData(buffer, size, usage);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(@NativeType("GLuint") int buffer, @NativeType("void const *") ByteBuffer data, @NativeType("GLbitfield") int flags) {
        GlBuffer.namedBufferData(buffer, data, GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(@NativeType("GLuint") int buffer, @NativeType("GLsizeiptr") long size, @NativeType("GLbitfield") int flags) {
        GlBuffer.namedBufferData(buffer, size, GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(@NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset, @NativeType("void const *") ByteBuffer data) {
        GlBuffer.namedBufferSubData(buffer, offset, data);
    }

    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapNamedBuffer(@NativeType("GLuint") int buffer, @NativeType("GLenum") int access) {
        return GlBuffer.mapNamedBuffer(buffer);
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glUnmapNamedBuffer(@NativeType("GLuint") int buffer) {
        return GlBuffer.unmapNamedBuffer(buffer);
    }

    @Overwrite(remap = false)
    public static void glBindTextureUnit(@NativeType("GLuint") int unit, @NativeType("GLuint") int texture) {
        int previousUnit = GlTexture.getActiveTexture();
        GlTexture.activeTexture(GL30.GL_TEXTURE0 + unit);
        GlTexture.bindTexture(texture);
        GlTexture.activeTexture(previousUnit);
    }

    @Overwrite(remap = false)
    public static void glTextureParameteri(@NativeType("GLuint") int texture, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
        withTextureBound(texture, () -> GlTexture.texParameteri(GL11.GL_TEXTURE_2D, pname, param));
    }

    @Overwrite(remap = false)
    public static void glTextureParameterf(@NativeType("GLuint") int texture, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glGenerateTextureMipmap(@NativeType("GLuint") int texture) {
        withTextureBound(texture, () -> GlTexture.generateMipmap(GL11.GL_TEXTURE_2D));
    }

    @Overwrite(remap = false)
    public static void glTextureStorage2D(@NativeType("GLuint") int texture, @NativeType("GLsizei") int levels, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        withTextureBound(texture, () -> GlTexture.texImage2D(GL11.GL_TEXTURE_2D, 0, internalformat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer pixels) {
        withTextureBound(texture, () -> GlTexture.texSubImage2D(GL11.GL_TEXTURE_2D, level, xoffset, yoffset, width, height, format, type, pixels));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") long pixels) {
        withTextureBound(texture, () -> GlTexture.texSubImage2D(GL11.GL_TEXTURE_2D, level, xoffset, yoffset, width, height, format, type, pixels));
    }

    @Overwrite(remap = false)
    @NativeType("GLenum")
    public static int glCheckNamedFramebufferStatus(@NativeType("GLuint") int framebuffer, @NativeType("GLenum") int target) {
        return GL_FRAMEBUFFER_COMPLETE;
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferTexture(@NativeType("GLuint") int framebuffer, @NativeType("GLenum") int attachment, @NativeType("GLuint") int texture, @NativeType("GLint") int level) {
        GlFramebuffer.namedFramebufferTexture(framebuffer, attachment, texture, level);
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferRenderbuffer(@NativeType("GLuint") int framebuffer, @NativeType("GLenum") int attachment, @NativeType("GLenum") int renderbuffertarget, @NativeType("GLuint") int renderbuffer) {
        GlFramebuffer.namedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer);
    }

    @Overwrite(remap = false)
    public static void glNamedRenderbufferStorage(@NativeType("GLuint") int renderbuffer, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        GlRenderbuffer.namedRenderbufferStorage(renderbuffer, internalformat, width, height);
    }

    @Overwrite(remap = false)
    public static void glNamedRenderbufferStorageMultisample(@NativeType("GLuint") int renderbuffer, @NativeType("GLsizei") int samples, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        GlRenderbuffer.namedRenderbufferStorageMultisample(renderbuffer, samples, internalformat, width, height);
    }

    @Overwrite(remap = false)
    public static void glClipControl(@NativeType("GLenum") int origin, @NativeType("GLenum") int depth) {
    }

    @Overwrite(remap = false)
    public static void glBlitNamedFramebuffer(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11) {
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferData(int p0, int p1, int p2, int p3, float[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferData(int p0, int p1, int p2, int p3, int[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferData(int p0, int p1, int p2, int p3, short[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferData(int p0, int p1, int p2, int p3, java.nio.ByteBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferData(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferData(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferData(int p0, int p1, int p2, int p3, java.nio.ShortBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, float[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, int[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, short[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, java.nio.ByteBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, java.nio.FloatBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, java.nio.IntBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glClearNamedBufferSubData(int p0, int p1, long p2, long p3, int p4, int p5, java.nio.ShortBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glClearNamedFramebufferfi(int p0, int p1, int p2, float p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glClearNamedFramebufferfv(int p0, int p1, int p2, float[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glClearNamedFramebufferfv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glClearNamedFramebufferiv(int p0, int p1, int p2, int[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glClearNamedFramebufferiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glClearNamedFramebufferuiv(int p0, int p1, int p2, int[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glClearNamedFramebufferuiv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glCompressedTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, java.nio.ByteBuffer p5) {
        vulkanmod$zero(p5);
    }

    @Overwrite(remap = false)
    public static void glCompressedTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, long p8) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, java.nio.ByteBuffer p7) {
        vulkanmod$zero(p7);
    }

    @Overwrite(remap = false)
    public static void glCompressedTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, java.nio.ByteBuffer p9) {
        vulkanmod$zero(p9);
    }

    @Overwrite(remap = false)
    public static void glCopyNamedBufferSubData(int p0, int p1, long p2, long p3, long p4) {
        GlBuffer.copyNamedBufferSubData(p0, p1, p2, p3, p4);
    }

    @Overwrite(remap = false)
    public static void glCopyTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5) {
    }

    @Overwrite(remap = false)
    public static void glCopyTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7) {
    }

    @Overwrite(remap = false)
    public static void glCopyTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8) {
    }

    @Overwrite(remap = false)
    public static void glCreateBuffers(int[] p0) {
        if (p0 != null) {
            for (int i = 0; i < p0.length; i++) {
                p0[i] = GlBuffer.glGenBuffers();
            }
        }
    }

    @Overwrite(remap = false)
    public static void glCreateBuffers(java.nio.IntBuffer p0) {
        if (p0 != null) {
            for (int i = p0.position(); i < p0.limit(); i++) {
                p0.put(i, GlBuffer.glGenBuffers());
            }
        }
    }

    @Overwrite(remap = false)
    public static int glCreateProgramPipelines() {
        return GL41M.glGenProgramPipelines();
    }

    @Overwrite(remap = false)
    public static void glCreateProgramPipelines(int[] p0) {
        if (p0 != null) {
            for (int i = 0; i < p0.length; i++) {
                p0[i] = GL41M.glGenProgramPipelines();
            }
        }
    }

    @Overwrite(remap = false)
    public static void glCreateProgramPipelines(java.nio.IntBuffer p0) {
        if (p0 != null) {
            for (int i = p0.position(); i < p0.limit(); i++) {
                p0.put(i, GL41M.glGenProgramPipelines());
            }
        }
    }

    @Overwrite(remap = false)
    public static void glCreateQueries(int p0, int[] p1) {
        if (p1 != null) {
            for (int i = 0; i < p1.length; i++) {
                p1[i] = GlQuery.genQueries();
            }
        }
    }

    @Overwrite(remap = false)
    public static void glCreateQueries(int p0, java.nio.IntBuffer p1) {
        if (p1 != null) {
            for (int i = p1.position(); i < p1.limit(); i++) {
                p1.put(i, GlQuery.genQueries());
            }
        }
    }

    @Overwrite(remap = false)
    public static void glCreateSamplers(int[] p0) {
        if (p0 != null) {
            for (int i = 0; i < p0.length; i++) {
                p0[i] = GlSampler.genSamplers();
            }
        }
    }

    @Overwrite(remap = false)
    public static void glCreateSamplers(java.nio.IntBuffer p0) {
        if (p0 != null) {
            for (int i = p0.position(); i < p0.limit(); i++) {
                p0.put(i, GlSampler.genSamplers());
            }
        }
    }

    @Overwrite(remap = false)
    public static int glCreateTransformFeedbacks() {
        return GL40M.glGenTransformFeedbacks();
    }

    @Overwrite(remap = false)
    public static void glCreateTransformFeedbacks(int[] p0) {
        if (p0 != null) {
            for (int i = 0; i < p0.length; i++) {
                p0[i] = GL40M.glGenTransformFeedbacks();
            }
        }
    }

    @Overwrite(remap = false)
    public static void glCreateTransformFeedbacks(java.nio.IntBuffer p0) {
        if (p0 != null) {
            for (int i = p0.position(); i < p0.limit(); i++) {
                p0.put(i, GL40M.glGenTransformFeedbacks());
            }
        }
    }

    @Overwrite(remap = false)
    public static void glDisableVertexArrayAttrib(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glEnableVertexArrayAttrib(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glFlushMappedNamedBufferRange(int p0, long p1, long p2) {
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureImage(int p0, int p1, int p2, long p3) {
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureImage(int p0, int p1, java.nio.ByteBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, double[] p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, float[] p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int[] p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, short[] p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, long p9) {
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.ByteBuffer p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.DoubleBuffer p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.FloatBuffer p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.IntBuffer p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.ShortBuffer p8) {
        vulkanmod$zero(p8);
    }

    @Overwrite(remap = false)
    public static int glGetGraphicsResetStatus() {
        return 0;
    }

    @Overwrite(remap = false)
    public static int glGetNamedBufferParameteri(int p0, int p1) {
        return vulkanmod$getNamedBufferParameteri(p0, p1);
    }

    @Overwrite(remap = false)
    public static long glGetNamedBufferParameteri64(int p0, int p1) {
        return vulkanmod$getNamedBufferParameteri(p0, p1);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferParameteri64v(int p0, int p1, long[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferParameteri64v(int p0, int p1, java.nio.LongBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferParameteriv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static long glGetNamedBufferPointer(int p0, int p1) {
        return 0L;
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferPointerv(int p0, int p1, org.lwjgl.PointerBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, double[] p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, float[] p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, int[] p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, long[] p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, short[] p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, java.nio.ByteBuffer p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, java.nio.DoubleBuffer p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, java.nio.FloatBuffer p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, java.nio.IntBuffer p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, java.nio.LongBuffer p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedBufferSubData(int p0, long p1, java.nio.ShortBuffer p2) {
        vulkanmod$getNamedBufferSubData(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static int glGetNamedFramebufferAttachmentParameteri(int p0, int p1, int p2) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetNamedFramebufferAttachmentParameteriv(int p0, int p1, int p2, int[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetNamedFramebufferAttachmentParameteriv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static int glGetNamedFramebufferParameteri(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetNamedFramebufferParameteriv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedFramebufferParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static int glGetNamedRenderbufferParameteri(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetNamedRenderbufferParameteriv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetNamedRenderbufferParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetQueryBufferObjecti64v(int p0, int p1, int p2, long p3) {
    }

    @Overwrite(remap = false)
    public static void glGetQueryBufferObjectiv(int p0, int p1, int p2, long p3) {
    }

    @Overwrite(remap = false)
    public static void glGetQueryBufferObjectui64v(int p0, int p1, int p2, long p3) {
    }

    @Overwrite(remap = false)
    public static void glGetQueryBufferObjectuiv(int p0, int p1, int p2, long p3) {
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, double[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, float[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, int[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, short[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, int p4, long p5) {
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, java.nio.ByteBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, java.nio.DoubleBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetTextureImage(int p0, int p1, int p2, int p3, java.nio.ShortBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static float glGetTextureLevelParameterf(int p0, int p1, int p2) {
        return 0.0f;
    }

    @Overwrite(remap = false)
    public static void glGetTextureLevelParameterfv(int p0, int p1, int p2, float[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetTextureLevelParameterfv(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static int glGetTextureLevelParameteri(int p0, int p1, int p2) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetTextureLevelParameteriv(int p0, int p1, int p2, int[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetTextureLevelParameteriv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static int glGetTextureParameterIi(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameterIiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameterIiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static int glGetTextureParameterIui(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameterIuiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameterIuiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static float glGetTextureParameterf(int p0, int p1) {
        return 0.0f;
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameterfv(int p0, int p1, float[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static int glGetTextureParameteri(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameteriv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetTextureParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, double[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, float[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, short[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, long p11) {
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.ByteBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.DoubleBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.FloatBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.IntBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glGetTextureSubImage(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.ShortBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static int glGetTransformFeedbacki(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static int glGetTransformFeedbacki(int p0, int p1, int p2) {
        return 0;
    }

    @Overwrite(remap = false)
    public static long glGetTransformFeedbacki64(int p0, int p1, int p2) {
        return 0L;
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbacki64_v(int p0, int p1, int p2, long[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbacki64_v(int p0, int p1, int p2, java.nio.LongBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbacki_v(int p0, int p1, int p2, int[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbacki_v(int p0, int p1, int p2, java.nio.IntBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbackiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbackiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static long glGetVertexArrayIndexed64i(int p0, int p1, int p2) {
        return 0L;
    }

    @Overwrite(remap = false)
    public static void glGetVertexArrayIndexed64iv(int p0, int p1, int p2, long[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetVertexArrayIndexed64iv(int p0, int p1, int p2, java.nio.LongBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static int glGetVertexArrayIndexedi(int p0, int p1, int p2) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetVertexArrayIndexediv(int p0, int p1, int p2, int[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetVertexArrayIndexediv(int p0, int p1, int p2, java.nio.IntBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static int glGetVertexArrayi(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetVertexArrayiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetVertexArrayiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, float[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, int[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, short[] p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, int p3, long p4) {
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, java.nio.ByteBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, java.nio.FloatBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, java.nio.IntBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnColorTable(int p0, int p1, int p2, java.nio.ShortBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnCompressedTexImage(int p0, int p1, int p2, long p3) {
    }

    @Overwrite(remap = false)
    public static void glGetnCompressedTexImage(int p0, int p1, java.nio.ByteBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnConvolutionFilter(int p0, int p1, int p2, int p3, long p4) {
    }

    @Overwrite(remap = false)
    public static void glGetnConvolutionFilter(int p0, int p1, int p2, java.nio.ByteBuffer p3) {
        vulkanmod$zero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetnHistogram(int p0, boolean p1, int p2, int p3, int p4, long p5) {
    }

    @Overwrite(remap = false)
    public static void glGetnHistogram(int p0, boolean p1, int p2, int p3, java.nio.ByteBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static double glGetnMapd(int p0, int p1) {
        return 0.0;
    }

    @Overwrite(remap = false)
    public static void glGetnMapdv(int p0, int p1, double[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnMapdv(int p0, int p1, java.nio.DoubleBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static float glGetnMapf(int p0, int p1) {
        return 0.0f;
    }

    @Overwrite(remap = false)
    public static void glGetnMapfv(int p0, int p1, float[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnMapfv(int p0, int p1, java.nio.FloatBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static int glGetnMapi(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetnMapiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnMapiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnMinmax(int p0, boolean p1, int p2, int p3, int p4, long p5) {
    }

    @Overwrite(remap = false)
    public static void glGetnMinmax(int p0, boolean p1, int p2, int p3, java.nio.ByteBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnPixelMapfv(int p0, float[] p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glGetnPixelMapfv(int p0, java.nio.FloatBuffer p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glGetnPixelMapuiv(int p0, int[] p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glGetnPixelMapuiv(int p0, java.nio.IntBuffer p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glGetnPixelMapusv(int p0, short[] p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glGetnPixelMapusv(int p0, java.nio.ShortBuffer p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glGetnPolygonStipple(int p0, long p1) {
    }

    @Overwrite(remap = false)
    public static void glGetnPolygonStipple(java.nio.ByteBuffer p0) {
        vulkanmod$zero(p0);
    }

    @Overwrite(remap = false)
    public static void glGetnSeparableFilter(int p0, int p1, int p2, int p3, long p4, int p5, long p6, java.nio.ByteBuffer p7) {
        vulkanmod$zero(p7);
    }

    @Overwrite(remap = false)
    public static void glGetnSeparableFilter(int p0, int p1, int p2, java.nio.ByteBuffer p3, java.nio.ByteBuffer p4, java.nio.ByteBuffer p5) {
        vulkanmod$zero(p3);
        vulkanmod$zero(p4);
        vulkanmod$zero(p5);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, double[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, float[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, int[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, short[] p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, int p4, long p5) {
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, java.nio.ByteBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, java.nio.DoubleBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, java.nio.FloatBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, java.nio.IntBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glGetnTexImage(int p0, int p1, int p2, int p3, java.nio.ShortBuffer p4) {
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static double glGetnUniformd(int p0, int p1) {
        return 0.0;
    }

    @Overwrite(remap = false)
    public static void glGetnUniformdv(int p0, int p1, double[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnUniformdv(int p0, int p1, java.nio.DoubleBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static float glGetnUniformf(int p0, int p1) {
        return 0.0f;
    }

    @Overwrite(remap = false)
    public static void glGetnUniformfv(int p0, int p1, float[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnUniformfv(int p0, int p1, java.nio.FloatBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static int glGetnUniformi(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetnUniformiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnUniformiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static int glGetnUniformui(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetnUniformuiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetnUniformuiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glInvalidateNamedFramebufferData(int p0, int[] p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glInvalidateNamedFramebufferData(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateNamedFramebufferData(int p0, java.nio.IntBuffer p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glInvalidateNamedFramebufferSubData(int p0, int[] p1, int p2, int p3, int p4, int p5) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glInvalidateNamedFramebufferSubData(int p0, int p1, int p2, int p3, int p4, int p5) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateNamedFramebufferSubData(int p0, java.nio.IntBuffer p1, int p2, int p3, int p4, int p5) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static java.nio.ByteBuffer glMapNamedBuffer(int p0, int p1, java.nio.ByteBuffer p2) {
        return null;
    }

    @Overwrite(remap = false)
    public static java.nio.ByteBuffer glMapNamedBuffer(int p0, int p1, long p2, java.nio.ByteBuffer p3) {
        return null;
    }

    @Overwrite(remap = false)
    public static java.nio.ByteBuffer glMapNamedBufferRange(int p0, long p1, long p2, int p3) {
        return GlBuffer.mapNamedBufferRange(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static java.nio.ByteBuffer glMapNamedBufferRange(int p0, long p1, long p2, int p3, java.nio.ByteBuffer p4) {
        return GlBuffer.mapNamedBufferRange(p0, p1, p2);
    }

    @Overwrite(remap = false)
    public static void glMemoryBarrierByRegion(int p0) {
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, double[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, float[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, int[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, long[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, short[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, java.nio.DoubleBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, java.nio.FloatBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, java.nio.IntBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, java.nio.LongBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferData(int p0, java.nio.ShortBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), p2);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, double[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, float[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, int[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, short[] p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, java.nio.DoubleBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, java.nio.FloatBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, java.nio.IntBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferStorage(int p0, java.nio.ShortBuffer p1, int p2) {
        GlBuffer.namedBufferData(p0, vulkanmod$bytes(p1), GL_STATIC_DRAW);
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, double[] p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, float[] p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, int[] p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, long[] p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, short[] p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, java.nio.DoubleBuffer p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, java.nio.FloatBuffer p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, java.nio.IntBuffer p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, java.nio.LongBuffer p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedBufferSubData(int p0, long p1, java.nio.ShortBuffer p2) {
        GlBuffer.namedBufferSubData(p0, p1, vulkanmod$bytes(p2));
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferDrawBuffer(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferDrawBuffers(int p0, int[] p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferDrawBuffers(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferDrawBuffers(int p0, java.nio.IntBuffer p1) {
        vulkanmod$zero(p1);
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferParameteri(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferReadBuffer(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferTextureLayer(int p0, int p1, int p2, int p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, float[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, int[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, short[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, int p6, long p7) {
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.ByteBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.FloatBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.IntBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glReadnPixels(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.ShortBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureBarrier() {
    }

    @Overwrite(remap = false)
    public static void glTextureBuffer(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glTextureBufferRange(int p0, int p1, int p2, long p3, long p4) {
    }

    @Overwrite(remap = false)
    public static void glTextureParameterIi(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glTextureParameterIiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureParameterIiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureParameterIui(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glTextureParameterIuiv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureParameterIuiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureParameterfv(int p0, int p1, float[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureParameterfv(int p0, int p1, java.nio.FloatBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureParameteriv(int p0, int p1, int[] p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureParameteriv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$zero(p2);
    }

    @Overwrite(remap = false)
    public static void glTextureStorage1D(int p0, int p1, int p2, int p3) {
    }

    @Overwrite(remap = false)
    public static void glTextureStorage2DMultisample(int p0, int p1, int p2, int p3, int p4, boolean p5) {
    }

    @Overwrite(remap = false)
    public static void glTextureStorage3D(int p0, int p1, int p2, int p3, int p4, int p5) {
    }

    @Overwrite(remap = false)
    public static void glTextureStorage3DMultisample(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6) {
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, double[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, float[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, int[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, short[] p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.ByteBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.DoubleBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.FloatBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.IntBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, java.nio.ShortBuffer p6) {
        vulkanmod$zero(p6);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage1D(int p0, int p1, int p2, int p3, int p4, int p5, long p6) {
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, double[] p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, float[] p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int[] p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, short[] p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.DoubleBuffer p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.FloatBuffer p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.IntBuffer p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage2D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, java.nio.ShortBuffer p8) {
        glTextureSubImage2D(p0, p1, p2, p3, p4, p5, p6, p7, vulkanmod$bytes(p8));
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, double[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, float[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, short[] p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.ByteBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.DoubleBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.FloatBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.IntBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, java.nio.ShortBuffer p10) {
        vulkanmod$zero(p10);
    }

    @Overwrite(remap = false)
    public static void glTextureSubImage3D(int p0, int p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, long p10) {
    }

    @Overwrite(remap = false)
    public static void glTransformFeedbackBufferBase(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glTransformFeedbackBufferRange(int p0, int p1, int p2, long p3, long p4) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayAttribBinding(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayAttribFormat(int p0, int p1, int p2, int p3, boolean p4, int p5) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayAttribIFormat(int p0, int p1, int p2, int p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayAttribLFormat(int p0, int p1, int p2, int p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayBindingDivisor(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayElementBuffer(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayVertexBuffer(int p0, int p1, int p2, long p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glVertexArrayVertexBuffers(int p0, int p1, int[] p2, org.lwjgl.PointerBuffer p3, int[] p4) {
        vulkanmod$zero(p2);
        vulkanmod$zero(p3);
        vulkanmod$zero(p4);
    }

    @Overwrite(remap = false)
    public static void glVertexArrayVertexBuffers(int p0, int p1, java.nio.IntBuffer p2, org.lwjgl.PointerBuffer p3, java.nio.IntBuffer p4) {
        vulkanmod$zero(p2);
        vulkanmod$zero(p3);
        vulkanmod$zero(p4);
    }

    private static void withTextureBound(int texture, Runnable action) {
        if (GlTexture.getTexture(texture) == null) {
            return;
        }

        int previous = GlTexture.getBoundTextureId(GL11.GL_TEXTURE_2D);
        GlTexture.bindTexture(texture);
        try {
            action.run();
        } catch (RuntimeException | LinkageError ignored) {

        } finally {
            GlTexture.bindTexture(previous);
        }
    }
}

