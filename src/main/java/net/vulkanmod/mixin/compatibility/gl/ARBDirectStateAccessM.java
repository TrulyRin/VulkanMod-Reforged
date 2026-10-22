package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.gl.GlVertexArray;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.IntBuffer;

@Mixin(ARBDirectStateAccess.class)
public class ARBDirectStateAccessM {
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CE5;

    @Overwrite(remap = false)
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
    public static void glNamedFramebufferTexture(@NativeType("GLuint") int framebuffer, @NativeType("GLenum") int attachment, @NativeType("GLuint") int texture, @NativeType("GLint") int level) {
        GlFramebuffer.namedFramebufferTexture(framebuffer, attachment, texture, level);
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferRenderbuffer(@NativeType("GLuint") int framebuffer, @NativeType("GLenum") int attachment, @NativeType("GLenum") int renderbuffertarget, @NativeType("GLuint") int renderbuffer) {
        GlFramebuffer.namedFramebufferRenderbuffer(framebuffer, attachment, renderbuffertarget, renderbuffer);
    }

    @Overwrite(remap = false)
    public static int glCheckNamedFramebufferStatus(@NativeType("GLuint") int framebuffer, @NativeType("GLenum") int target) {
        return GL_FRAMEBUFFER_COMPLETE;
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
    public static void glTextureParameteri(@NativeType("GLuint") int texture, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
        if (GlTexture.getTexture(texture) == null) {
            return;
        }

        int previous = GlTexture.getBoundTextureId(GL11.GL_TEXTURE_2D);
        GlTexture.bindTexture(texture);
        try {
            GlTexture.texParameteri(GL11.GL_TEXTURE_2D, pname, param);
        } finally {
            GlTexture.bindTexture(previous);
        }
    }

    @Overwrite(remap = false)
    public static void glTextureParameterf(@NativeType("GLuint") int texture, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glNamedFramebufferTextureLayer(@NativeType("GLuint") int framebuffer, @NativeType("GLenum") int attachment, @NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int layer) {
        GlFramebuffer.namedFramebufferTexture(framebuffer, attachment, texture, level);
    }
}
