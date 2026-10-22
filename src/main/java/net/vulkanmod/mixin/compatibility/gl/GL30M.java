package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlEmulationLog;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.gl.GlVertexArray;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(GL30C.class)
public class GL30M {
    private static void vulkanmod$putZero(ByteBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), (byte) 0);
        }
    }

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

    private static void vulkanmod$writeEmptyString(IntBuffer length, ByteBuffer text) {
        vulkanmod$putZero(length);
        vulkanmod$putZero(text);
    }

    private static void vulkanmod$writeEmptyString(int[] length, ByteBuffer text) {
        vulkanmod$putZero(length);
        vulkanmod$putZero(text);
    }

    @Overwrite(remap = false)
    public static void glGenerateMipmap(@NativeType("GLenum") int target) {
        GlTexture.generateMipmap(target);
    }

    @NativeType("void")
    @Overwrite(remap = false)
    public static int glGenFramebuffers() {
        return GlFramebuffer.genFramebufferId();
    }

    @Overwrite(remap = false)
    public static void glBindFramebuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer) {
        GlFramebuffer.bindFramebuffer(target, framebuffer);
    }

    @Overwrite(remap = false)
    public static void glFramebufferTexture2D(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture, @NativeType("GLint") int level) {
        GlFramebuffer.framebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Overwrite(remap = false)
    public static void glFramebufferRenderbuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int renderbuffertarget, @NativeType("GLuint") int renderbuffer) {
        GlFramebuffer.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }

    @Overwrite(remap = false)
    public static void glDeleteFramebuffers(@NativeType("GLuint const *") int framebuffer) {
        GlFramebuffer.deleteFramebuffer(framebuffer);
    }

    @Overwrite(remap = false)
    @NativeType("GLenum")
    public static int glCheckFramebufferStatus(@NativeType("GLenum") int target) {
        return GlFramebuffer.glCheckFramebufferStatus(target);
    }

    @Overwrite(remap = false)
    public static boolean glIsFramebuffer(@NativeType("GLuint") int framebuffer) {
        return framebuffer == 0 || GlFramebuffer.getFramebuffer(framebuffer) != null;
    }

    @Overwrite(remap = false)
    public static int glGetFramebufferAttachmentParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int pname) {
        return GlFramebuffer.getFramebufferAttachmentParameteri(target, attachment, pname);
    }

    @NativeType("void")
    @Overwrite(remap = false)
    public static int glGenRenderbuffers() {
        return GlRenderbuffer.genId();
    }

    @Overwrite(remap = false)
    public static void glBindRenderbuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int framebuffer) {
        GlRenderbuffer.bindRenderbuffer(target, framebuffer);
    }

    @Overwrite(remap = false)
    public static void glRenderbufferStorage(@NativeType("GLenum") int target, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        GlRenderbuffer.renderbufferStorage(target, internalformat, width, height);
    }

    @Overwrite(remap = false)
    public static void glDeleteRenderbuffers(@NativeType("GLuint const *") int renderbuffer) {
        GlRenderbuffer.deleteRenderbuffer(renderbuffer);
    }

    @Overwrite(remap = false)
    public static int glGenVertexArrays() {
        return GlVertexArray.genVertexArray();
    }

    @Overwrite(remap = false)
    public static void glGenVertexArrays(@NativeType("GLuint *") IntBuffer arrays) {
        GlVertexArray.genVertexArrays(arrays);
    }

    @Overwrite(remap = false)
    public static void glBindVertexArray(@NativeType("GLuint") int array) {
        GlVertexArray.bindVertexArray(array);
    }

    @Overwrite(remap = false)
    public static void glDeleteVertexArrays(@NativeType("GLuint const *") int array) {
        GlVertexArray.deleteVertexArray(array);
    }

    @Overwrite(remap = false)
    public static void glDeleteVertexArrays(@NativeType("GLuint const *") IntBuffer arrays) {
        GlVertexArray.deleteVertexArrays(arrays);
    }

    @Overwrite(remap = false)
    public static boolean glIsVertexArray(@NativeType("GLuint") int array) {
        return GlVertexArray.isVertexArray(array);
    }

    @Overwrite(remap = false)
    public static void glVertexAttribIPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
        GlVertexArray.vertexAttribIPointer(index, size, type, stride, pointer);
    }

    @Overwrite(remap = false)
    public static void glGenFramebuffers(@NativeType("GLuint *") IntBuffer framebuffers) {
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            framebuffers.put(i, GlFramebuffer.genFramebufferId());
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteFramebuffers(@NativeType("GLuint const *") IntBuffer framebuffers) {
        for (int i = framebuffers.position(); i < framebuffers.limit(); i++) {
            GlFramebuffer.deleteFramebuffer(framebuffers.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glGenRenderbuffers(@NativeType("GLuint *") IntBuffer renderbuffers) {
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            renderbuffers.put(i, GlRenderbuffer.genId());
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteRenderbuffers(@NativeType("GLuint const *") IntBuffer renderbuffers) {
        for (int i = renderbuffers.position(); i < renderbuffers.limit(); i++) {
            GlRenderbuffer.deleteRenderbuffer(renderbuffers.get(i));
        }
    }

    @Overwrite(remap = false)
    public static boolean glIsRenderbuffer(@NativeType("GLuint") int renderbuffer) {
        return renderbuffer == 0 || GlRenderbuffer.getRenderbuffer(renderbuffer) != null;
    }

    @Overwrite(remap = false)
    public static void glRenderbufferStorageMultisample(@NativeType("GLenum") int target, @NativeType("GLsizei") int samples, @NativeType("GLenum") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        GlRenderbuffer.renderbufferStorageMultisample(target, samples, internalformat, width, height);
    }

    @Overwrite(remap = false)
    public static int glGetRenderbufferParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlRenderbuffer.getRenderbufferParameter(target, pname);
    }

    @Overwrite(remap = false)
    @NativeType("void *")
    public static ByteBuffer glMapBufferRange(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long length, @NativeType("GLbitfield") int access) {
        return GlBuffer.glMapBufferRange(target, offset, length, access);
    }

    @Overwrite(remap = false)
    public static void glFlushMappedBufferRange(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long length) {
    }

    @Overwrite(remap = false)
    public static void glBlitFramebuffer(@NativeType("GLint") int srcX0, @NativeType("GLint") int srcY0, @NativeType("GLint") int srcX1, @NativeType("GLint") int srcY1, @NativeType("GLint") int dstX0, @NativeType("GLint") int dstY0, @NativeType("GLint") int dstX1, @NativeType("GLint") int dstY1, @NativeType("GLbitfield") int mask, @NativeType("GLenum") int filter) {
        GlFramebuffer.blitFramebuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }

    @Overwrite(remap = false)
    public static void glFramebufferTexture1D(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture, @NativeType("GLint") int level) {
    }

    @Overwrite(remap = false)
    public static void glFramebufferTexture3D(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int textarget, @NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int zoffset) {
    }

    @Overwrite(remap = false)
    public static void glFramebufferTextureLayer(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLuint") int texture, @NativeType("GLint") int level, @NativeType("GLint") int layer) {
    }

    @Overwrite(remap = false)
    public static void glGetFramebufferAttachmentParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlFramebuffer.getFramebufferAttachmentParameteri(target, attachment, pname));
        }
    }

    @Overwrite(remap = false)
    public static String glGetStringi(@NativeType("GLenum") int name, @NativeType("GLuint") int index) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glClearBufferiv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer, @NativeType("GLint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferuiv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferfv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer, @NativeType("GLfloat const *") FloatBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferfi(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer, @NativeType("GLfloat") float depth, @NativeType("GLint") int stencil) {
    }

    @Overwrite(remap = false)
    public static void glBindBufferBase(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLuint") int buffer) {
        GlBuffer.glBindBuffer(target, buffer);
    }

    @Overwrite(remap = false)
    public static void glBindBufferRange(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLuint") int buffer, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long size) {
        GlBuffer.glBindBuffer(target, buffer);
    }

    @Overwrite(remap = false)
    public static void glUniform1ui(@NativeType("GLint") int location, @NativeType("GLuint") int v0) {
    }

    @Overwrite(remap = false)
    public static void glUniform2ui(@NativeType("GLint") int location, @NativeType("GLuint") int v0, @NativeType("GLuint") int v1) {
    }

    @Overwrite(remap = false)
    public static void glUniform3ui(@NativeType("GLint") int location, @NativeType("GLuint") int v0, @NativeType("GLuint") int v1, @NativeType("GLuint") int v2) {
    }

    @Overwrite(remap = false)
    public static void glUniform4ui(@NativeType("GLint") int location, @NativeType("GLuint") int v0, @NativeType("GLuint") int v1, @NativeType("GLuint") int v2, @NativeType("GLuint") int v3) {
    }

    @Overwrite(remap = false)
    public static void glColorMaski(@NativeType("GLuint") int buf, @NativeType("GLboolean") boolean r, @NativeType("GLboolean") boolean g, @NativeType("GLboolean") boolean b, @NativeType("GLboolean") boolean a) {
    }

    @Overwrite(remap = false)
    public static void glEnablei(@NativeType("GLenum") int cap, @NativeType("GLuint") int index) {
    }

    @Overwrite(remap = false)
    public static void glDisablei(@NativeType("GLenum") int cap, @NativeType("GLuint") int index) {
    }

    @Overwrite(remap = false)
    public static boolean glIsEnabledi(@NativeType("GLenum") int cap, @NativeType("GLuint") int index) {
        return false;
    }

    @Overwrite(remap = false)
    public static void glBindFragDataLocation(@NativeType("GLuint") int program, @NativeType("GLuint") int colorNumber, @NativeType("GLchar const *") CharSequence name) {
    }

    @Overwrite(remap = false)
    public static int glGetFragDataLocation(@NativeType("GLuint") int program, @NativeType("GLchar const *") CharSequence name) {
        return 0;
    }

    @Overwrite(remap = false)
    public static int glGetIntegeri(@NativeType("GLenum") int target, @NativeType("GLuint") int index) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetIntegeri_v(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLint *") IntBuffer data) {
        if (data != null && data.remaining() > 0) {
            data.put(data.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glBeginConditionalRender(@NativeType("GLuint") int id, @NativeType("GLenum") int mode) {
    }

    @Overwrite(remap = false)
    public static void glEndConditionalRender() {
    }

    @Overwrite(remap = false)
    public static void glBeginTransformFeedback(@NativeType("GLenum") int primitiveMode) {
    }

    @Overwrite(remap = false)
    public static void glEndTransformFeedback() {
    }

    @Overwrite(remap = false)
    public static void glClampColor(@NativeType("GLenum") int target, @NativeType("GLenum") int clamp) {
    }

    @Overwrite(remap = false)
    public static void glGenFramebuffers(@NativeType("GLuint *") int[] framebuffers) {
        if (framebuffers == null) {
            return;
        }
        for (int i = 0; i < framebuffers.length; i++) {
            framebuffers[i] = GlFramebuffer.genFramebufferId();
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteFramebuffers(@NativeType("GLuint const *") int[] framebuffers) {
        if (framebuffers == null) {
            return;
        }
        for (int framebuffer : framebuffers) {
            GlFramebuffer.deleteFramebuffer(framebuffer);
        }
    }

    @Overwrite(remap = false)
    public static void glGenRenderbuffers(@NativeType("GLuint *") int[] renderbuffers) {
        if (renderbuffers == null) {
            return;
        }
        for (int i = 0; i < renderbuffers.length; i++) {
            renderbuffers[i] = GlRenderbuffer.genId();
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteRenderbuffers(@NativeType("GLuint const *") int[] renderbuffers) {
        if (renderbuffers == null) {
            return;
        }
        for (int renderbuffer : renderbuffers) {
            GlRenderbuffer.deleteRenderbuffer(renderbuffer);
        }
    }

    @Overwrite(remap = false)
    public static void glGenVertexArrays(@NativeType("GLuint *") int[] arrays) {
        if (arrays == null) {
            return;
        }
        for (int i = 0; i < arrays.length; i++) {
            arrays[i] = GlVertexArray.genVertexArray();
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteVertexArrays(@NativeType("GLuint const *") int[] arrays) {
        if (arrays == null) {
            return;
        }
        for (int array : arrays) {
            GlVertexArray.deleteVertexArray(array);
        }
    }

    @Overwrite(remap = false)
    public static void glGetFramebufferAttachmentParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int attachment, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlFramebuffer.getFramebufferAttachmentParameteri(target, attachment, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetRenderbufferParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlRenderbuffer.getRenderbufferParameter(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetRenderbufferParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlRenderbuffer.getRenderbufferParameter(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static boolean glGetBooleani(@NativeType("GLenum") int target, @NativeType("GLuint") int index) {
        return false;
    }

    @Overwrite(remap = false)
    public static void glGetBooleani_v(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLboolean *") ByteBuffer data) {
        vulkanmod$putZero(data);
    }

    @Overwrite(remap = false)
    public static void glGetIntegeri_v(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLint *") int[] data) {
        vulkanmod$putZero(data);
    }

    @Overwrite(remap = false)
    public static void glClearBufferiv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer, @NativeType("GLint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferuiv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glClearBufferfv(@NativeType("GLenum") int buffer, @NativeType("GLint") int drawbuffer, @NativeType("GLfloat const *") float[] value) {
    }

    @Overwrite(remap = false)
    @NativeType("void *")
    public static ByteBuffer glMapBufferRange(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("GLsizeiptr") long length, @NativeType("GLbitfield") int access, ByteBuffer oldBuffer) {
        ByteBuffer mapped = GlBuffer.glMapBufferRange(target, offset, length, access);
        return mapped != null ? mapped : oldBuffer;
    }

    @Overwrite(remap = false)
    public static void glBindFragDataLocation(@NativeType("GLuint") int program, @NativeType("GLuint") int colorNumber, @NativeType("GLchar const *") ByteBuffer name) {
    }

    @Overwrite(remap = false)
    public static int glGetFragDataLocation(@NativeType("GLuint") int program, @NativeType("GLchar const *") ByteBuffer name) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glTransformFeedbackVaryings(@NativeType("GLuint") int program, @NativeType("GLchar const **") CharSequence varying, @NativeType("GLenum") int bufferMode) {
    }

    @Overwrite(remap = false)
    public static void glTransformFeedbackVaryings(@NativeType("GLuint") int program, @NativeType("GLchar const **") CharSequence[] varyings, @NativeType("GLenum") int bufferMode) {
    }

    @Overwrite(remap = false)
    public static void glTransformFeedbackVaryings(@NativeType("GLuint") int program, @NativeType("GLchar const **") PointerBuffer varyings, @NativeType("GLenum") int bufferMode) {
    }

    @Overwrite(remap = false)
    public static String glGetTransformFeedbackVarying(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei") int maxLength, @NativeType("GLsizei *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        vulkanmod$putZero(size);
        vulkanmod$putZero(type);
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetTransformFeedbackVarying(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        vulkanmod$putZero(size);
        vulkanmod$putZero(type);
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbackVarying(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLsizei *") IntBuffer size, @NativeType("GLenum *") IntBuffer type, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$writeEmptyString(length, name);
        vulkanmod$putZero(size);
        vulkanmod$putZero(type);
    }

    @Overwrite(remap = false)
    public static void glGetTransformFeedbackVarying(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei *") int[] length, @NativeType("GLsizei *") int[] size, @NativeType("GLenum *") int[] type, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$writeEmptyString(length, name);
        vulkanmod$putZero(size);
        vulkanmod$putZero(type);
    }

    @Overwrite(remap = false)
    public static void glTexParameterIi(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @Overwrite(remap = false)
    public static void glTexParameterIui(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLuint") int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @Overwrite(remap = false)
    public static void glTexParameterIiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            GlTexture.texParameteri(target, pname, params.get(params.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glTexParameterIiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
        if (params != null && params.length > 0) {
            GlTexture.texParameteri(target, pname, params[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glTexParameterIuiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLuint const *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            GlTexture.texParameteri(target, pname, params.get(params.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glTexParameterIuiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLuint const *") int[] params) {
        if (params != null && params.length > 0) {
            GlTexture.texParameteri(target, pname, params[0]);
        }
    }

    @Overwrite(remap = false)
    public static int glGetTexParameterIi(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlTexture.getTexParameteri(target, pname);
    }

    @Overwrite(remap = false)
    @NativeType("GLuint")
    public static int glGetTexParameterIui(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlTexture.getTexParameteri(target, pname);
    }

    @Overwrite(remap = false)
    public static void glGetTexParameterIiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlTexture.getTexParameteri(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetTexParameterIiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlTexture.getTexParameteri(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetTexParameterIuiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLuint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlTexture.getTexParameteri(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetTexParameterIuiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLuint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlTexture.getTexParameteri(target, pname);
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLuint")
    public static int glGetUniformui(@NativeType("GLuint") int program, @NativeType("GLint") int location) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetUniformuiv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLuint *") IntBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetUniformuiv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLuint *") int[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glUniform1uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform1uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform2uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform2uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform3uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform3uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform4uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform4uiv(@NativeType("GLint") int location, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static int glGetVertexAttribIi(@NativeType("GLuint") int index, @NativeType("GLenum") int pname) {
        return GlVertexArray.getVertexAttribi(index, pname);
    }

    @Overwrite(remap = false)
    @NativeType("GLuint")
    public static int glGetVertexAttribIui(@NativeType("GLuint") int index, @NativeType("GLenum") int pname) {
        return GlVertexArray.getVertexAttribi(index, pname);
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribIiv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlVertexArray.getVertexAttribi(index, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribIiv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlVertexArray.getVertexAttribi(index, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribIuiv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLuint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlVertexArray.getVertexAttribi(index, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribIuiv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLuint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlVertexArray.getVertexAttribi(index, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI1i(@NativeType("GLuint") int index, @NativeType("GLint") int x) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI2i(@NativeType("GLuint") int index, @NativeType("GLint") int x, @NativeType("GLint") int y) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI3i(@NativeType("GLuint") int index, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLint") int z) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4i(@NativeType("GLuint") int index, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLint") int z, @NativeType("GLint") int w) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI1ui(@NativeType("GLuint") int index, @NativeType("GLuint") int x) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI2ui(@NativeType("GLuint") int index, @NativeType("GLuint") int x, @NativeType("GLuint") int y) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI3ui(@NativeType("GLuint") int index, @NativeType("GLuint") int x, @NativeType("GLuint") int y, @NativeType("GLuint") int z) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4ui(@NativeType("GLuint") int index, @NativeType("GLuint") int x, @NativeType("GLuint") int y, @NativeType("GLuint") int z, @NativeType("GLuint") int w) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI1iv(@NativeType("GLuint") int index, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI1iv(@NativeType("GLuint") int index, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI1uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI1uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI2iv(@NativeType("GLuint") int index, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI2iv(@NativeType("GLuint") int index, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI2uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI2uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI3iv(@NativeType("GLuint") int index, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI3iv(@NativeType("GLuint") int index, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI3uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI3uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4iv(@NativeType("GLuint") int index, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4iv(@NativeType("GLuint") int index, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4bv(@NativeType("GLuint") int index, @NativeType("GLbyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4ubv(@NativeType("GLuint") int index, @NativeType("GLubyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4usv(@NativeType("GLuint") int index, @NativeType("GLushort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribI4usv(@NativeType("GLuint") int index, @NativeType("GLushort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribIPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
        GlVertexArray.unsupportedClientPointer();
    }

    @Overwrite(remap = false)
    public static void glVertexAttribIPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
        GlVertexArray.unsupportedClientPointer();
    }

    @Overwrite(remap = false)
    public static void glVertexAttribIPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
        GlVertexArray.unsupportedClientPointer();
    }
}
