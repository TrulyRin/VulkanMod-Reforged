package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(GL31C.class)
public class GL31M {
    private static final int GL_INVALID_INDEX = -1;

    private static void vulkanmod$putZero(IntBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), 0);
        }
    }

    private static void vulkanmod$putZero(int[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0;
        }
    }

    private static void vulkanmod$putInvalid(IntBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), GL_INVALID_INDEX);
        }
    }

    private static void vulkanmod$putInvalid(int[] values) {
        if (values != null && values.length > 0) {
            values[0] = GL_INVALID_INDEX;
        }
    }

    private static void vulkanmod$writeEmptyString(IntBuffer length, ByteBuffer name) {
        vulkanmod$putZero(length);
        if (name != null && name.remaining() > 0) {
            name.put(name.position(), (byte) 0);
        }
    }

    private static void vulkanmod$writeEmptyString(int[] length, ByteBuffer name) {
        vulkanmod$putZero(length);
        if (name != null && name.remaining() > 0) {
            name.put(name.position(), (byte) 0);
        }
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstanced(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("GLenum") int type, @NativeType("void const *") long indices, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstanced(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("void const *") ByteBuffer indices, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstanced(@NativeType("GLenum") int mode, @NativeType("void const *") ByteBuffer indices, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstanced(@NativeType("GLenum") int mode, @NativeType("void const *") ShortBuffer indices, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsInstanced(@NativeType("GLenum") int mode, @NativeType("void const *") IntBuffer indices, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static void glDrawArraysInstanced(@NativeType("GLenum") int mode, @NativeType("GLint") int first, @NativeType("GLsizei") int count, @NativeType("GLsizei") int primcount) {
    }

    @Overwrite(remap = false)
    public static void glCopyBufferSubData(@NativeType("GLenum") int readTarget, @NativeType("GLenum") int writeTarget, @NativeType("GLintptr") long readOffset, @NativeType("GLintptr") long writeOffset, @NativeType("GLsizeiptr") long size) {
        GlBuffer.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }

    @Overwrite(remap = false)
    public static int glGetUniformBlockIndex(@NativeType("GLuint") int program, @NativeType("GLchar const *") CharSequence uniformBlockName) {
        return GL_INVALID_INDEX;
    }

    @Overwrite(remap = false)
    public static int glGetUniformBlockIndex(@NativeType("GLuint") int program, @NativeType("GLchar const *") ByteBuffer uniformBlockName) {
        return GL_INVALID_INDEX;
    }

    @Overwrite(remap = false)
    public static void glUniformBlockBinding(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex, @NativeType("GLuint") int uniformBlockBinding) {
    }

    @Overwrite(remap = false)
    public static int glGetActiveUniformBlocki(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformBlockiv(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformBlockiv(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static String glGetActiveUniformBlockName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetActiveUniformBlockName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformBlockName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer uniformBlockName) {
        vulkanmod$writeEmptyString(length, uniformBlockName);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformBlockName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformBlockIndex, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer uniformBlockName) {
        vulkanmod$writeEmptyString(length, uniformBlockName);
    }

    @Overwrite(remap = false)
    public static int glGetUniformIndices(@NativeType("GLuint") int program, @NativeType("GLchar const **") CharSequence uniformName) {
        return GL_INVALID_INDEX;
    }

    @Overwrite(remap = false)
    public static void glGetUniformIndices(@NativeType("GLuint") int program, @NativeType("GLchar const **") CharSequence[] uniformNames, @NativeType("GLuint *") IntBuffer uniformIndices) {
        vulkanmod$putInvalid(uniformIndices);
    }

    @Overwrite(remap = false)
    public static void glGetUniformIndices(@NativeType("GLuint") int program, @NativeType("GLchar const **") PointerBuffer uniformNames, @NativeType("GLuint *") IntBuffer uniformIndices) {
        vulkanmod$putInvalid(uniformIndices);
    }

    @Overwrite(remap = false)
    public static void glGetUniformIndices(@NativeType("GLuint") int program, @NativeType("GLchar const **") PointerBuffer uniformNames, @NativeType("GLuint *") int[] uniformIndices) {
        vulkanmod$putInvalid(uniformIndices);
    }

    @Overwrite(remap = false)
    public static int glGetActiveUniformsi(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformIndex, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformsiv(@NativeType("GLuint") int program, @NativeType("GLuint const *") IntBuffer uniformIndices, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformsiv(@NativeType("GLuint") int program, @NativeType("GLuint const *") int[] uniformIndices, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static String glGetActiveUniformName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformIndex, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetActiveUniformName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformIndex) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformIndex, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer uniformName) {
        vulkanmod$writeEmptyString(length, uniformName);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformName(@NativeType("GLuint") int program, @NativeType("GLuint") int uniformIndex, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer uniformName) {
        vulkanmod$writeEmptyString(length, uniformName);
    }

    @Overwrite(remap = false)
    public static void glPrimitiveRestartIndex(@NativeType("GLuint") int index) {
    }

    @Overwrite(remap = false)
    public static void glTexBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLuint") int buffer) {
    }
}
