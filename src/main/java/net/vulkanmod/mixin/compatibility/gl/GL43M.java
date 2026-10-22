package net.vulkanmod.mixin.compatibility.gl;

import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLDebugMessageCallbackI;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

@Mixin(GL43C.class)
public class GL43M {
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

    private static void vulkanmod$putZero(LongBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), 0L);
        }
    }

    private static void vulkanmod$putZero(long[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0L;
        }
    }

    @Overwrite(remap = false)
    public static void glDebugMessageCallback(@NativeType("GLDEBUGPROC") GLDebugMessageCallbackI callback, @NativeType("void const *") long userParam) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageControl(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLenum") int severity, @NativeType("GLuint const *") IntBuffer ids, @NativeType("GLboolean") boolean enabled) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageControl(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLenum") int severity, @NativeType("GLuint const *") int ids, @NativeType("GLboolean") boolean enabled) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageControl(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLenum") int severity, @NativeType("GLuint const *") int[] ids, @NativeType("GLboolean") boolean enabled) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageInsert(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLuint") int id, @NativeType("GLenum") int severity, @NativeType("GLchar const *") CharSequence message) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageInsert(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLuint") int id, @NativeType("GLenum") int severity, @NativeType("GLchar const *") ByteBuffer message) {
    }

    @Overwrite(remap = false)
    public static int glGetDebugMessageLog(@NativeType("GLuint") int count, @NativeType("GLenum *") IntBuffer sources, @NativeType("GLenum *") IntBuffer types, @NativeType("GLuint *") IntBuffer ids, @NativeType("GLenum *") IntBuffer severities, @NativeType("GLsizei *") IntBuffer lengths, @NativeType("GLchar *") ByteBuffer messageLog) {
        vulkanmod$putZero(sources);
        vulkanmod$putZero(types);
        vulkanmod$putZero(ids);
        vulkanmod$putZero(severities);
        vulkanmod$putZero(lengths);
        return 0;
    }

    @Overwrite(remap = false)
    public static int glGetDebugMessageLog(@NativeType("GLuint") int count, @NativeType("GLenum *") int[] sources, @NativeType("GLenum *") int[] types, @NativeType("GLuint *") int[] ids, @NativeType("GLenum *") int[] severities, @NativeType("GLsizei *") int[] lengths, @NativeType("GLchar *") ByteBuffer messageLog) {
        vulkanmod$putZero(sources);
        vulkanmod$putZero(types);
        vulkanmod$putZero(ids);
        vulkanmod$putZero(severities);
        vulkanmod$putZero(lengths);
        return 0;
    }

    @Overwrite(remap = false)
    public static void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLchar const *") CharSequence label) {
    }

    @Overwrite(remap = false)
    public static void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLchar const *") ByteBuffer label) {
    }

    @Overwrite(remap = false)
    public static String glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer label) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer label) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLchar const *") CharSequence label) {
    }

    @Overwrite(remap = false)
    public static void glObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLchar const *") ByteBuffer label) {
    }

    @Overwrite(remap = false)
    public static String glGetObjectPtrLabel(@NativeType("void *") long ptr) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer label) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glGetObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer label) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glPushDebugGroup(@NativeType("GLenum") int source, @NativeType("GLuint") int id, @NativeType("GLchar const *") CharSequence message) {
    }

    @Overwrite(remap = false)
    public static void glPushDebugGroup(@NativeType("GLenum") int source, @NativeType("GLuint") int id, @NativeType("GLchar const *") ByteBuffer message) {
    }

    @Overwrite(remap = false)
    public static void glPopDebugGroup() {
    }

    @Overwrite(remap = false)
    public static void glInvalidateFramebuffer(@NativeType("GLenum") int target, @NativeType("GLenum const *") IntBuffer attachments) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateFramebuffer(@NativeType("GLenum") int target, @NativeType("GLenum const *") int attachment) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateFramebuffer(@NativeType("GLenum") int target, @NativeType("GLenum const *") int[] attachments) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateSubFramebuffer(@NativeType("GLenum") int target, @NativeType("GLenum const *") IntBuffer attachments, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateSubFramebuffer(@NativeType("GLenum") int target, @NativeType("GLenum const *") int attachment, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateSubFramebuffer(@NativeType("GLenum") int target, @NativeType("GLenum const *") int[] attachments, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateTexImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateTexSubImage(@NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int zoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateBufferData(@NativeType("GLuint") int buffer) {
    }

    @Overwrite(remap = false)
    public static void glInvalidateBufferSubData(@NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long length) {
    }

    @Overwrite(remap = false)
    public static void glCopyImageSubData(@NativeType("GLuint") int srcName, @NativeType("GLenum") int srcTarget, @NativeType("GLint") int srcLevel, @NativeType("GLint") int srcX, @NativeType("GLint") int srcY, @NativeType("GLint") int srcZ, @NativeType("GLuint") int dstName, @NativeType("GLenum") int dstTarget, @NativeType("GLint") int dstLevel, @NativeType("GLint") int dstX, @NativeType("GLint") int dstY, @NativeType("GLint") int dstZ, @NativeType("GLsizei") int srcWidth, @NativeType("GLsizei") int srcHeight, @NativeType("GLsizei") int srcDepth) {
    }

    @Overwrite(remap = false)
    public static void glDispatchCompute(@NativeType("GLuint") int num_groups_x, @NativeType("GLuint") int num_groups_y, @NativeType("GLuint") int num_groups_z) {
    }

    @Overwrite(remap = false)
    public static void glDispatchComputeIndirect(@NativeType("GLintptr") long indirect) {
    }

    @Overwrite(remap = false)
    public static void glFramebufferParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static int glGetFramebufferParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetFramebufferParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetFramebufferParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glTexStorage2DMultisample(@NativeType("GLenum") int target, @NativeType("GLsizei") int samples, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLboolean") boolean fixedsamplelocations) {
    }

    @Overwrite(remap = false)
    public static void glTexStorage3DMultisample(@NativeType("GLenum") int target, @NativeType("GLsizei") int samples, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLsizei") int depth, @NativeType("GLboolean") boolean fixedsamplelocations) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ShortBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") IntBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") FloatBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") short[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") int[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") float[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferSubData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferSubData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ShortBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferSubData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") IntBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferSubData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") FloatBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferSubData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") short[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferSubData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") int[] data) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferSubData(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") float[] data) {
    }

    @Overwrite(remap = false)
    public static long glGetInternalformati64(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int pname) {
        return 0L;
    }

    @Overwrite(remap = false)
    public static void glGetInternalformati64v(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int pname, @NativeType("GLint64 *") LongBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetInternalformati64v(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLenum") int pname, @NativeType("GLint64 *") long[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glMultiDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") ByteBuffer indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glMultiDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") IntBuffer indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glMultiDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") int[] indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glMultiDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") long indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glMultiDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glMultiDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") IntBuffer indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glMultiDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") int[] indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glMultiDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") long indirect, @NativeType("GLsizei") int primcount, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glShaderStorageBlockBinding(@NativeType("GLuint") int program, @NativeType("GLuint") int storageBlockIndex, @NativeType("GLuint") int storageBlockBinding) {
    }

    @Overwrite(remap = false)
    public static int glGetProgramInterfacei(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetProgramInterfaceiv(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetProgramInterfaceiv(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static int glGetProgramResourceIndex(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLchar const *") ByteBuffer name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetProgramResourceIndex(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLchar const *") CharSequence name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static String glGetProgramResourceName(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLuint") int index) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetProgramResourceName(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLuint") int index, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetProgramResourceName(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glGetProgramResourceName(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLuint") int index, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glGetProgramResourceiv(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLuint") int index, @NativeType("GLenum const *") IntBuffer props, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$putZero(length);
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetProgramResourceiv(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLuint") int index, @NativeType("GLenum const *") int[] props, @NativeType("GLsizei *") int[] length, @NativeType("GLint *") int[] params) {
        vulkanmod$putZero(length);
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static int glGetProgramResourceLocation(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLchar const *") ByteBuffer name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetProgramResourceLocation(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLchar const *") CharSequence name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetProgramResourceLocationIndex(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLchar const *") ByteBuffer name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetProgramResourceLocationIndex(@NativeType("GLuint") int program, @NativeType("GLenum") int programInterface, @NativeType("GLchar const *") CharSequence name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static void glTexBufferRange(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size) {
    }

    @Overwrite(remap = false)
    public static void glTextureView(@NativeType("GLuint") int texture, @NativeType("GLenum") int target, @NativeType("GLuint") int origtexture, @NativeType("GLenum") int internalformat, @NativeType("GLuint") int minlevel, @NativeType("GLuint") int numlevels, @NativeType("GLuint") int minlayer, @NativeType("GLuint") int numlayers) {
    }

    @Overwrite(remap = false)
    public static void glBindVertexBuffer(@NativeType("GLuint") int bindingindex, @NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset, @NativeType("GLsizei") int stride) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribFormat(@NativeType("GLuint") int attribindex, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint") int relativeoffset) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribIFormat(@NativeType("GLuint") int attribindex, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLuint") int relativeoffset) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribLFormat(@NativeType("GLuint") int attribindex, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLuint") int relativeoffset) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribBinding(@NativeType("GLuint") int attribindex, @NativeType("GLuint") int bindingindex) {
    }

    @Overwrite(remap = false)
    public static void glVertexBindingDivisor(@NativeType("GLuint") int bindingindex, @NativeType("GLuint") int divisor) {
    }
}
