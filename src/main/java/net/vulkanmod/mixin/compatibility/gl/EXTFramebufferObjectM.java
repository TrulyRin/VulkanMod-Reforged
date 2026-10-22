package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlTexture;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.IntBuffer;

@Mixin(EXTFramebufferObject.class)
public class EXTFramebufferObjectM {
    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsRenderbufferEXT(@NativeType("GLuint") int renderbuffer) {
        return renderbuffer == 0 || GlRenderbuffer.getRenderbuffer(renderbuffer) != null;
    }

    @Overwrite(remap = false)
    public static void glBindRenderbufferEXT(@NativeType("GLenum") int target, @NativeType("GLuint") int renderbuffer) {
        GlRenderbuffer.bindRenderbuffer(target, renderbuffer);
    }

    @Overwrite(remap = false)
    public static void glDeleteRenderbuffersEXT(@NativeType("GLuint const *") IntBuffer renderbuffers) {
        if (renderbuffers == null) {
            return;
        }
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            GlRenderbuffer.deleteRenderbuffer(renderbuffers.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteRenderbuffersEXT(@NativeType("GLuint const *") int renderbuffer) {
        GlRenderbuffer.deleteRenderbuffer(renderbuffer);
    }

    @Overwrite(remap = false)
    public static void glDeleteRenderbuffersEXT(@NativeType("GLuint const *") int[] renderbuffers) {
        if (renderbuffers == null) {
            return;
        }
        for (int renderbuffer : renderbuffers) {
            GlRenderbuffer.deleteRenderbuffer(renderbuffer);
        }
    }

    @Overwrite(remap = false)
    public static void glGenRenderbuffersEXT(@NativeType("GLuint *") IntBuffer renderbuffers) {
        if (renderbuffers == null) {
            return;
        }
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            renderbuffers.put(i, GlRenderbuffer.genId());
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenRenderbuffersEXT() {
        return GlRenderbuffer.genId();
    }

    @Overwrite(remap = false)
    public static void glGenRenderbuffersEXT(@NativeType("GLuint *") int[] renderbuffers) {
        if (renderbuffers == null) {
            return;
        }
        for (int i = 0; i < renderbuffers.length; i++) {
            renderbuffers[i] = GlRenderbuffer.genId();
        }
    }

    @Overwrite(remap = false)
    public static void glRenderbufferStorageEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        GlRenderbuffer.renderbufferStorage(target, internalformat, width, height);
    }

    @Overwrite(remap = false)
    public static void glGetRenderbufferParameterivEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlRenderbuffer.getRenderbufferParameter(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetRenderbufferParameterivEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlRenderbuffer.getRenderbufferParameter(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static int glGetRenderbufferParameteriEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlRenderbuffer.getRenderbufferParameter(target, pname);
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsFramebufferEXT(@NativeType("GLuint") int framebuffer) {
        return framebuffer == 0 || GlFramebuffer.getFramebuffer(framebuffer) != null;
    }

    @Overwrite(remap = false)
    public static void glBindFramebufferEXT(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer) {
        GlFramebuffer.bindFramebuffer(target, framebuffer);
    }

    @Overwrite(remap = false)
    public static void glDeleteFramebuffersEXT(@NativeType("GLuint const *") IntBuffer framebuffers) {
        if (framebuffers == null) {
            return;
        }
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            GlFramebuffer.deleteFramebuffer(framebuffers.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteFramebuffersEXT(@NativeType("GLuint const *") int framebuffer) {
        GlFramebuffer.deleteFramebuffer(framebuffer);
    }

    @Overwrite(remap = false)
    public static void glDeleteFramebuffersEXT(@NativeType("GLuint const *") int[] framebuffers) {
        if (framebuffers == null) {
            return;
        }
        for (int framebuffer : framebuffers) {
            GlFramebuffer.deleteFramebuffer(framebuffer);
        }
    }

    @Overwrite(remap = false)
    public static void glGenFramebuffersEXT(@NativeType("GLuint *") IntBuffer framebuffers) {
        if (framebuffers == null) {
            return;
        }
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            framebuffers.put(i, GlFramebuffer.genFramebufferId());
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenFramebuffersEXT() {
        return GlFramebuffer.genFramebufferId();
    }

    @Overwrite(remap = false)
    public static void glGenFramebuffersEXT(@NativeType("GLuint *") int[] framebuffers) {
        if (framebuffers == null) {
            return;
        }
        for (int i = 0; i < framebuffers.length; i++) {
            framebuffers[i] = GlFramebuffer.genFramebufferId();
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLenum")
    public static int glCheckFramebufferStatusEXT(@NativeType("GLenum") int target) {
        return GlFramebuffer.glCheckFramebufferStatus(target);
    }

    @Overwrite(remap = false)
    public static void glFramebufferTexture1DEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture, @NativeType("GLint") int level) {
        GlFramebuffer.framebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Overwrite(remap = false)
    public static void glFramebufferTexture2DEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture, @NativeType("GLint") int level) {
        GlFramebuffer.framebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Overwrite(remap = false)
    public static void glFramebufferTexture3DEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int zoffset) {
        GlFramebuffer.framebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Overwrite(remap = false)
    public static void glFramebufferRenderbufferEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int renderbuffertarget, @NativeType("GLuint") int renderbuffer) {
        GlFramebuffer.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Overwrite(remap = false)
    public static void glGetFramebufferAttachmentParameterivEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlFramebuffer.getFramebufferAttachmentParameteri(target, attachment, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetFramebufferAttachmentParameterivEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlFramebuffer.getFramebufferAttachmentParameteri(target, attachment, pname);
        }
    }

    @Overwrite(remap = false)
    public static int glGetFramebufferAttachmentParameteriEXT(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int pname) {
        return GlFramebuffer.getFramebufferAttachmentParameteri(target, attachment, pname);
    }

    @Overwrite(remap = false)
    public static void glBlitFramebufferEXT(@NativeType("GLint") int srcX0, @NativeType("GLint") int srcY0, @NativeType("GLint") int srcX1, @NativeType("GLint") int srcY1, @NativeType("GLint") int dstX0, @NativeType("GLint") int dstY0, @NativeType("GLint") int dstX1, @NativeType("GLint") int dstY1, @NativeType("GLbitfield") int mask, @NativeType("GLenum") int filter) {
        GlFramebuffer.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Overwrite(remap = false)
    public static void glGenerateMipmapEXT(@NativeType("GLenum") int target) {
        GlTexture.generateMipmap(target);
    }
}
