package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;

@Mixin(GL13C.class)
public class GL13M {
    private static void vulkanmod$zero(ByteBuffer data) {
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, (byte) 0);
        }
    }

    @Overwrite(remap = false)
    public static void glActiveTexture(@NativeType("GLenum") int texture) {
        GlTexture.activeTexture(texture);
    }

    @Overwrite(remap = false)
    public static void glCompressedTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("void const *") ByteBuffer data) {
        GlTexture.compressedTexImage2D(target, level, internalformat, width, height, border, data);
    }

    @Overwrite(remap = false)
    public static void glCompressedTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLsizei") int imageSize, @NativeType("void const *") long data) {
        ByteBuffer buffer = (data != 0L && imageSize > 0) ? MemoryUtil.memByteBuffer(data, imageSize) : null;
        GlTexture.compressedTexImage2D(target, level, internalformat, width, height, border, buffer);
    }

    @Overwrite(remap = false)
    public static void glCompressedTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("void const *") ByteBuffer data) {
        GlTexture.texImage1D(target, level, internalformat, width, border, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
    }

    @Overwrite(remap = false)
    public static void glCompressedTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLsizei") int imageSize, @NativeType("void const *") long data) {
        GlTexture.texImage1D(target, level, internalformat, width, border, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
    }

    @Overwrite(remap = false)
    public static void glCompressedTexImage3D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLint") int border, @NativeType("void const *") ByteBuffer data) {
        int format = GL11.GL_RGBA;
        int type = GL11.GL_UNSIGNED_BYTE;
        GlTexture.texImage3D(target, level, internalformat, width, height, depth, border, format, type, data);
    }

    @Overwrite(remap = false)
    public static void glCompressedTexImage3D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLint") int border, @NativeType("GLsizei") int imageSize, @NativeType("void const *") long data) {
        ByteBuffer buffer = (data != 0L && imageSize > 0) ? MemoryUtil.memByteBuffer(data, imageSize) : null;
        int format = GL11.GL_RGBA;
        int type = GL11.GL_UNSIGNED_BYTE;
        GlTexture.texImage3D(target, level, internalformat, width, height, depth, border, format, type, buffer);
    }

    @Overwrite(remap = false)
    public static void glCompressedTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("void const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLsizei") int imageSize, @NativeType("void const *") long data) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("void const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLsizei") int imageSize, @NativeType("void const *") long data) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTexSubImage3D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("void const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glCompressedTexSubImage3D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLenum") int format, @NativeType("GLsizei") int imageSize, @NativeType("void const *") long data) {
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTexImage(@NativeType("GLenum") int target, @NativeType("GLint") int lod, @NativeType("void *") ByteBuffer img) {
        vulkanmod$zero(img);
    }

    @Overwrite(remap = false)
    public static void glGetCompressedTexImage(@NativeType("GLenum") int target, @NativeType("GLint") int lod, @NativeType("void *") long img) {
    }

    @Overwrite(remap = false)
    public static void glSampleCoverage(@NativeType("GLfloat") float value, @NativeType("GLboolean") boolean invert) {
    }
}
