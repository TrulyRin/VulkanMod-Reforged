package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(GL42C.class)
public class GL42M {
    private static final int GL_RGBA = 0x1908;
    private static final int GL_UNSIGNED_BYTE = 0x1401;

    @Overwrite(remap = false)
    public static void glTexStorage2D(@NativeType("GLenum") int target, @NativeType("GLsizei") int levels, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        if (target == GL11.GL_TEXTURE_2D) {
            GlTexture.texImage2D(target, 0, internalformat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        }
    }

    @Overwrite(remap = false)
    public static void glTexStorage1D(@NativeType("GLenum") int target, @NativeType("GLsizei") int levels, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width) {
    }

    @Overwrite(remap = false)
    public static void glTexStorage3D(@NativeType("GLenum") int target, @NativeType("GLsizei") int levels, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth) {
    }

    @Overwrite(remap = false)
    public static void glBindImageTexture(@NativeType("GLuint") int unit, @NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLboolean") boolean layered, @NativeType("GLint") int layer, @NativeType("GLenum") int access, @NativeType("GLenum") int format) {
    }

    @Overwrite(remap = false)
    public static void glMemoryBarrier(@NativeType("GLbitfield") int barriers) {
    }

    @Overwrite(remap = false)
    public static void glDrawArraysInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLint") int first, @NativeType("GLsizei") int count, @NativeType("GLsizei") int primcount, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("GLenum") int type, @NativeType("void const *") long indices, @NativeType("GLsizei") int primcount, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("void const *") ByteBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("void const *") ByteBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("void const *") ShortBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseInstance(@NativeType("GLenum") int mode, @NativeType("void const *") IntBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseVertexBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("GLenum") int type, @NativeType("void const *") long indices, @NativeType("GLsizei") int primcount, @NativeType("GLint") int basevertex, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseVertexBaseInstance(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("void const *") ByteBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLint") int basevertex, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseVertexBaseInstance(@NativeType("GLenum") int mode, @NativeType("void const *") ByteBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLint") int basevertex, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseVertexBaseInstance(@NativeType("GLenum") int mode, @NativeType("void const *") ShortBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLint") int basevertex, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstancedBaseVertexBaseInstance(@NativeType("GLenum") int mode, @NativeType("void const *") IntBuffer indices, @NativeType("GLsizei") int primcount, @NativeType("GLint") int basevertex, @NativeType("GLuint") int baseinstance) {
    }

    @Overwrite(remap = false)
    public static void glDrawTransformFeedbackInstanced(@NativeType("GLenum") int mode, @NativeType("GLuint") int id, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static void glDrawTransformFeedbackStreamInstanced(@NativeType("GLenum") int mode, @NativeType("GLuint") int id, @NativeType("GLuint") int stream, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static int glGetActiveAtomicCounterBufferi(@NativeType("GLuint") int program, @NativeType("GLuint") int bufferIndex, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetActiveAtomicCounterBufferiv(@NativeType("GLuint") int program, @NativeType("GLuint") int bufferIndex, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glGetActiveAtomicCounterBufferiv(@NativeType("GLuint") int program, @NativeType("GLuint") int bufferIndex, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = 0;
        }
    }

    @Overwrite(remap = false)
    public static int glGetInternalformati(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetInternalformativ(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glGetInternalformativ(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = 0;
        }
    }
}
