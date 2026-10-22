package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlProgramQueries;
import net.vulkanmod.gl.GlVertexArray;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(GL20C.class)
public class GL20M {
    private static void vulkanmod$putZero(IntBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), 0);
        }
    }

    private static void vulkanmod$putZero(FloatBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), 0.0F);
        }
    }

    private static void vulkanmod$putZero(DoubleBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), 0.0);
        }
    }

    private static void vulkanmod$putZero(int[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0;
        }
    }

    private static void vulkanmod$putZero(float[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0.0F;
        }
    }

    private static void vulkanmod$putZero(double[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0.0;
        }
    }

    private static void vulkanmod$writeEmptyString(IntBuffer length, ByteBuffer text) {
        vulkanmod$putZero(length);
        if (text != null && text.remaining() > 0) {
            text.put(text.position(), (byte) 0);
        }
    }

    private static void vulkanmod$writeEmptyString(int[] length, ByteBuffer text) {
        vulkanmod$putZero(length);
        if (text != null && text.remaining() > 0) {
            text.put(text.position(), (byte) 0);
        }
    }

    @Overwrite(remap = false)
    public static int glCreateProgram() {
        return GlProgram.createProgram();
    }

    @Overwrite(remap = false)
    public static void glDeleteProgram(@NativeType("GLuint") int program) {
        GlProgram.deleteProgram(program);
    }

    @Overwrite(remap = false)
    public static int glCreateShader(@NativeType("GLenum") int type) {
        return GlProgram.createShader(type);
    }

    @Overwrite(remap = false)
    public static void glDeleteShader(@NativeType("GLuint") int shader) {
        GlProgram.deleteShader(shader);
    }

    @Overwrite(remap = false)
    public static void glAttachShader(@NativeType("GLuint") int program, @NativeType("GLuint") int shader) {
        GlProgram.attachShader(program, shader);
    }

    @Overwrite(remap = false)
    public static void glShaderSource(@NativeType("GLuint") int shader, @NativeType("GLchar const *") CharSequence string) {
        GlProgram.shaderSource(shader, string);
    }

    @Overwrite(remap = false)
    public static void glShaderSource(@NativeType("GLuint") int shader, @NativeType("GLchar const **") CharSequence... strings) {
        GlProgram.shaderSource(shader, strings != null && strings.length > 0 ? strings[0] : "");
    }

    @Overwrite(remap = false)
    public static void glShaderSource(@NativeType("GLuint") int shader, @NativeType("GLchar const **") PointerBuffer strings, @NativeType("GLint const *") IntBuffer length) {
        GlProgram.shaderSource(shader, "");
    }

    @Overwrite(remap = false)
    public static void glShaderSource(@NativeType("GLuint") int shader, @NativeType("GLchar const **") PointerBuffer strings, @NativeType("GLint const *") int[] length) {
        GlProgram.shaderSource(shader, "");
    }

    @Overwrite(remap = false)
    public static void glCompileShader(@NativeType("GLuint") int shader) {
        GlProgram.compileShader(shader);
    }

    @Overwrite(remap = false)
    public static void glLinkProgram(@NativeType("GLuint") int program) {
        GlProgram.linkProgram(program);
    }

    @Overwrite(remap = false)
    public static void glUseProgram(@NativeType("GLuint") int program) {
        GlProgram.useProgram(program);
    }

    @Overwrite(remap = false)
    public static int glGetShaderi(@NativeType("GLuint") int shader, @NativeType("GLenum") int pname) {
        return GlProgram.getShaderi(shader, pname);
    }

    @Overwrite(remap = false)
    public static void glGetShaderiv(@NativeType("GLuint") int shader, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        params.put(params.position(), GL20.GL_TRUE);
    }

    @Overwrite(remap = false)
    public static void glGetShaderiv(@NativeType("GLuint") int shader, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlProgram.getShaderi(shader, pname);
        }
    }

    @Overwrite(remap = false)
    public static int glGetProgrami(@NativeType("GLuint") int program, @NativeType("GLenum") int pname) {
        return GlProgram.getProgrami(program, pname);
    }

    @Overwrite(remap = false)
    public static void glGetProgramiv(@NativeType("GLuint") int program, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.hasRemaining()) {
            params.put(params.position(), GlProgram.getProgrami(program, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetProgramiv(@NativeType("GLuint") int program, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlProgram.getProgrami(program, pname);
        }
    }

    @Overwrite(remap = false)
    public static String glGetShaderInfoLog(@NativeType("GLuint") int shader, @NativeType("GLsizei") int maxLength) {
        return GlProgram.getShaderInfoLog(shader);
    }

    @Overwrite(remap = false)
    public static String glGetShaderInfoLog(@NativeType("GLuint") int shader) {
        return GlProgram.getShaderInfoLog(shader);
    }

    @Overwrite(remap = false)
    public static void glGetShaderInfoLog(@NativeType("GLuint") int shader, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer infoLog) {
        vulkanmod$writeEmptyString(length, infoLog);
    }

    @Overwrite(remap = false)
    public static void glGetShaderInfoLog(@NativeType("GLuint") int shader, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer infoLog) {
        vulkanmod$writeEmptyString(length, infoLog);
    }

    @Overwrite(remap = false)
    public static String glGetShaderSource(@NativeType("GLuint") int shader, @NativeType("GLsizei") int maxLength) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetShaderSource(@NativeType("GLuint") int shader) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetShaderSource(@NativeType("GLuint") int shader, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer source) {
        vulkanmod$writeEmptyString(length, source);
    }

    @Overwrite(remap = false)
    public static void glGetShaderSource(@NativeType("GLuint") int shader, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer source) {
        vulkanmod$writeEmptyString(length, source);
    }

    @Overwrite(remap = false)
    public static String glGetProgramInfoLog(@NativeType("GLuint") int program, @NativeType("GLsizei") int maxLength) {
        return GlProgram.getProgramInfoLog(program);
    }

    @Overwrite(remap = false)
    public static String glGetProgramInfoLog(@NativeType("GLuint") int program) {
        return GlProgram.getProgramInfoLog(program);
    }

    @Overwrite(remap = false)
    public static void glGetProgramInfoLog(@NativeType("GLuint") int program, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer infoLog) {
        vulkanmod$writeEmptyString(length, infoLog);
    }

    @Overwrite(remap = false)
    public static void glGetProgramInfoLog(@NativeType("GLuint") int program, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer infoLog) {
        vulkanmod$writeEmptyString(length, infoLog);
    }

    @Overwrite(remap = false)
    public static void glBindAttribLocation(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLchar const *") CharSequence name) {
        GlProgram.bindAttribLocation(program, index, name);
    }

    @Overwrite(remap = false)
    public static void glBindAttribLocation(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLchar const *") ByteBuffer name) {
        GlProgram.bindAttribLocation(program, index, "");
    }

    @Overwrite(remap = false)
    public static int glGetAttribLocation(@NativeType("GLuint") int program, @NativeType("GLchar const *") CharSequence name) {
        return GlProgram.getAttribLocation(program, name);
    }

    @Overwrite(remap = false)
    public static int glGetAttribLocation(@NativeType("GLuint") int program, @NativeType("GLchar const *") ByteBuffer name) {
        return GlProgram.getAttribLocation(program, GlProgramQueries.decodeName(name));
    }

    @Overwrite(remap = false)
    public static int glGetUniformLocation(@NativeType("GLuint") int program, @NativeType("GLchar const *") CharSequence name) {
        return GlProgram.getUniformLocation(program, name);
    }

    @Overwrite(remap = false)
    public static int glGetUniformLocation(@NativeType("GLuint") int program, @NativeType("GLchar const *") ByteBuffer name) {
        return GlProgram.getUniformLocation(program, GlProgramQueries.decodeName(name));
    }

    @Overwrite(remap = false)
    public static void glUniform1i(@NativeType("GLint") int location, @NativeType("GLint") int v0) {
        GlProgram.uniform1i(location, v0);
    }

    @Overwrite(remap = false)
    public static void glUniform1f(@NativeType("GLint") int location, @NativeType("GLfloat") float v0) {
        GlProgram.uniform1f(location, v0);
    }

    @Overwrite(remap = false)
    public static void glUniform2f(@NativeType("GLint") int location, @NativeType("GLfloat") float v0, @NativeType("GLfloat") float v1) {
        GlProgram.uniform2f(location, v0, v1);
    }

    @Overwrite(remap = false)
    public static void glUniform3f(@NativeType("GLint") int location, @NativeType("GLfloat") float v0, @NativeType("GLfloat") float v1, @NativeType("GLfloat") float v2) {
        GlProgram.uniform3f(location, v0, v1, v2);
    }

    @Overwrite(remap = false)
    public static void glUniform3i(@NativeType("GLint") int location, @NativeType("GLint") int v0, @NativeType("GLint") int v1, @NativeType("GLint") int v2) {
        GlProgram.uniform3i(location, v0, v1, v2);
    }

    @Overwrite(remap = false)
    public static void glUniform4f(@NativeType("GLint") int location, @NativeType("GLfloat") float v0, @NativeType("GLfloat") float v1, @NativeType("GLfloat") float v2, @NativeType("GLfloat") float v3) {
        GlProgram.uniform4f(location, v0, v1, v2, v3);
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4fv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") FloatBuffer value) {
        GlProgram.uniformMatrix4fv(location, transpose, value);
    }

    @Overwrite(remap = false)
    public static void glEnableVertexAttribArray(@NativeType("GLuint") int index) {
        GlVertexArray.enableVertexAttribArray(index);
    }

    @Overwrite(remap = false)
    public static void glDisableVertexAttribArray(@NativeType("GLuint") int index) {
        GlVertexArray.disableVertexAttribArray(index);
    }

    @Overwrite(remap = false)
    public static void glVertexAttribPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
        GlVertexArray.vertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Overwrite(remap = false)
    public static void glBlendEquationSeparate(@NativeType("GLenum") int modeRGB, @NativeType("GLenum") int modeAlpha) {
        net.vulkanmod.vulkan.VRenderSystem.blendEquationSeparate(modeRGB, modeAlpha);
    }

    @Overwrite(remap = false)
    public static void glDetachShader(@NativeType("GLuint") int program, @NativeType("GLuint") int shader) {
        GlProgram.detachShader(program, shader);
    }

    @Overwrite(remap = false)
    public static void glValidateProgram(@NativeType("GLuint") int program) {
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsProgram(@NativeType("GLuint") int program) {
        return GlProgram.isProgram(program);
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsShader(@NativeType("GLuint") int shader) {
        return shader > 0;
    }

    @Overwrite(remap = false)
    public static void glUniform2i(@NativeType("GLint") int location, @NativeType("GLint") int v0, @NativeType("GLint") int v1) {
    }

    @Overwrite(remap = false)
    public static void glUniform4i(@NativeType("GLint") int location, @NativeType("GLint") int v0, @NativeType("GLint") int v1, @NativeType("GLint") int v2, @NativeType("GLint") int v3) {
    }

    @Overwrite(remap = false)
    public static void glUniform1fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        GlProgram.uniform1fv(location, value);
    }

    @Overwrite(remap = false)
    public static void glUniform1fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        GlProgram.uniform1fv(location, value);
    }

    @Overwrite(remap = false)
    public static void glUniform1iv(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
        if (value != null && value.remaining() >= 1) {
            GlProgram.uniform1i(location, value.get(value.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform1iv(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
        if (value != null && value.length >= 1) {
            GlProgram.uniform1i(location, value[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform2fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        if (value != null && value.remaining() >= 2) {
            GlProgram.uniform2f(location, value.get(value.position()), value.get(value.position() + 1));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform2fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        if (value != null && value.length >= 2) {
            GlProgram.uniform2f(location, value[0], value[1]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform2iv(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform2iv(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform3fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        if (value != null && value.remaining() >= 3) {
            GlProgram.uniform3f(location, value.get(value.position()), value.get(value.position() + 1), value.get(value.position() + 2));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform3fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        if (value != null && value.length >= 3) {
            GlProgram.uniform3f(location, value[0], value[1], value[2]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform3iv(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
        if (value != null && value.remaining() >= 3) {
            GlProgram.uniform3i(location, value.get(value.position()), value.get(value.position() + 1), value.get(value.position() + 2));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform3iv(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
        if (value != null && value.length >= 3) {
            GlProgram.uniform3i(location, value[0], value[1], value[2]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform4fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        if (value != null && value.remaining() >= 4) {
            GlProgram.uniform4f(location, value.get(value.position()), value.get(value.position() + 1), value.get(value.position() + 2), value.get(value.position() + 3));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform4fv(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        if (value != null && value.length >= 4) {
            GlProgram.uniform4f(location, value[0], value[1], value[2], value[3]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform4iv(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform4iv(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2fv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") FloatBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2fv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") float[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3fv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") FloatBuffer value) {
        GlProgram.uniformMatrix3fv(location, transpose, value);
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3fv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") float[] value) {
        GlProgram.uniformMatrix3fv(location, transpose, value);
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4fv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") float[] value) {
        GlProgram.uniformMatrix4fv(location, transpose, value);
    }

    @Overwrite(remap = false)
    public static void glDrawBuffers(@NativeType("GLenum const *") int buf) {
    }

    @Overwrite(remap = false)
    public static void glDrawBuffers(@NativeType("GLenum const *") IntBuffer bufs) {
    }

    @Overwrite(remap = false)
    public static void glDrawBuffers(@NativeType("GLenum const *") int[] bufs) {
    }

    @Overwrite(remap = false)
    public static void glStencilOpSeparate(@NativeType("GLenum") int face, @NativeType("GLenum") int sfail, @NativeType("GLenum") int dpfail, @NativeType("GLenum") int dppass) {
        net.vulkanmod.vulkan.VRenderSystem.stencilOp(sfail, dpfail, dppass);
    }

    @Overwrite(remap = false)
    public static void glStencilFuncSeparate(@NativeType("GLenum") int face, @NativeType("GLenum") int func, @NativeType("GLint") int ref, @NativeType("GLuint") int mask) {
        net.vulkanmod.vulkan.VRenderSystem.stencilFunc(func, ref, mask);
    }

    @Overwrite(remap = false)
    public static void glStencilMaskSeparate(@NativeType("GLenum") int face, @NativeType("GLuint") int mask) {
        net.vulkanmod.vulkan.VRenderSystem.stencilMask(mask);
    }

    @Overwrite(remap = false)
    public static float glGetUniformf(@NativeType("GLuint") int program, @NativeType("GLint") int location) {
        return GlProgram.getUniformf(program, location);
    }

    @Overwrite(remap = false)
    public static int glGetUniformi(@NativeType("GLuint") int program, @NativeType("GLint") int location) {
        return GlProgram.getUniformi(program, location);
    }

    @Overwrite(remap = false)
    public static int glGetVertexAttribi(@NativeType("GLuint") int index, @NativeType("GLenum") int pname) {
        return GlVertexArray.getVertexAttribi(index, pname);
    }

    @Overwrite(remap = false)
    public static void glGetUniformfv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLfloat *") FloatBuffer params) {
        GlProgram.getUniformfv(program, location, params);
    }

    @Overwrite(remap = false)
    public static void glGetUniformfv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLfloat *") float[] params) {
        GlProgram.getUniformfv(program, location, params);
    }

    @Overwrite(remap = false)
    public static void glGetUniformiv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLint *") IntBuffer params) {
        GlProgram.getUniformiv(program, location, params);
    }

    @Overwrite(remap = false)
    public static void glGetUniformiv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLint *") int[] params) {
        GlProgram.getUniformiv(program, location, params);
    }

    @Overwrite(remap = false)
    @NativeType("void *")
    public static long glGetVertexAttribPointer(@NativeType("GLuint") int index, @NativeType("GLenum") int pname) {
        return GlVertexArray.getVertexAttribPointer(index, pname);
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribPointerv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("void **") PointerBuffer pointer) {
        if (pointer != null && pointer.remaining() > 0) {
            pointer.put(pointer.position(), GlVertexArray.getVertexAttribPointer(index, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribdv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLdouble *") DoubleBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlVertexArray.getVertexAttribi(index, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribdv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLdouble *") double[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlVertexArray.getVertexAttribi(index, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribfv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlVertexArray.getVertexAttribi(index, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribfv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlVertexArray.getVertexAttribi(index, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribiv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlVertexArray.getVertexAttribi(index, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribiv(@NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlVertexArray.getVertexAttribi(index, pname);
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLuint")
    public static String glGetActiveUniform(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei") int maxLength, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        return GlProgramQueries.activeName(GlProgram.activeUniform(program, index), maxLength, size, type);
    }

    @Overwrite(remap = false)
    public static String glGetActiveUniform(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        return GlProgramQueries.activeName(GlProgram.activeUniform(program, index), Integer.MAX_VALUE, size, type);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniform(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type, @NativeType("GLchar *") ByteBuffer name) {
        GlProgramQueries.activeVariable(GlProgram.activeUniform(program, index), length, size, type, name);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniform(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei *") int[] length, @NativeType("GLint *") int[] size, @NativeType("GLenum *") int[] type, @NativeType("GLchar *") ByteBuffer name) {
        GlProgramQueries.activeVariable(GlProgram.activeUniform(program, index), length, size, type, name);
    }

    @Overwrite(remap = false)
    @NativeType("GLuint")
    public static String glGetActiveAttrib(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei") int maxLength, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        return GlProgramQueries.activeName(GlProgram.activeAttribute(program, index), maxLength, size, type);
    }

    @Overwrite(remap = false)
    public static String glGetActiveAttrib(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        return GlProgramQueries.activeName(GlProgram.activeAttribute(program, index), Integer.MAX_VALUE, size, type);
    }

    @Overwrite(remap = false)
    public static void glGetActiveAttrib(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type, @NativeType("GLchar *") ByteBuffer name) {
        GlProgramQueries.activeVariable(GlProgram.activeAttribute(program, index), length, size, type, name);
    }

    @Overwrite(remap = false)
    public static void glGetActiveAttrib(@NativeType("GLuint") int program, @NativeType("GLuint") int index, @NativeType("GLsizei *") int[] length, @NativeType("GLint *") int[] size, @NativeType("GLenum *") int[] type, @NativeType("GLchar *") ByteBuffer name) {
        GlProgramQueries.activeVariable(GlProgram.activeAttribute(program, index), length, size, type, name);
    }

    @Overwrite(remap = false)
    public static void glGetAttachedShaders(@NativeType("GLuint") int program, @NativeType("GLsizei *") IntBuffer count, @NativeType("GLuint *") IntBuffer shaders) {
        vulkanmod$putZero(count);
    }

    @Overwrite(remap = false)
    public static void glGetAttachedShaders(@NativeType("GLuint") int program, @NativeType("GLsizei *") int[] count, @NativeType("GLuint *") int[] shaders) {
        vulkanmod$putZero(count);
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1s(@NativeType("GLuint") int index, @NativeType("GLshort") short x) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1f(@NativeType("GLuint") int index, @NativeType("GLfloat") float x) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1d(@NativeType("GLuint") int index, @NativeType("GLdouble") double x) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2s(@NativeType("GLuint") int index, @NativeType("GLshort") short x, @NativeType("GLshort") short y) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2f(@NativeType("GLuint") int index, @NativeType("GLfloat") float x, @NativeType("GLfloat") float y) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2d(@NativeType("GLuint") int index, @NativeType("GLdouble") double x, @NativeType("GLdouble") double y) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3s(@NativeType("GLuint") int index, @NativeType("GLshort") short x, @NativeType("GLshort") short y, @NativeType("GLshort") short z) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3f(@NativeType("GLuint") int index, @NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3d(@NativeType("GLuint") int index, @NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4s(@NativeType("GLuint") int index, @NativeType("GLshort") short x, @NativeType("GLshort") short y, @NativeType("GLshort") short z, @NativeType("GLshort") short w) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4f(@NativeType("GLuint") int index, @NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z, @NativeType("GLfloat") float w) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4d(@NativeType("GLuint") int index, @NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z, @NativeType("GLdouble") double w) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nub(@NativeType("GLuint") int index, @NativeType("GLubyte") byte x, @NativeType("GLubyte") byte y, @NativeType("GLubyte") byte z, @NativeType("GLubyte") byte w) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib1dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib2dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib3dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4bv(@NativeType("GLuint") int index, @NativeType("GLbyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4sv(@NativeType("GLuint") int index, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4iv(@NativeType("GLuint") int index, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4iv(@NativeType("GLuint") int index, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4fv(@NativeType("GLuint") int index, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4dv(@NativeType("GLuint") int index, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4ubv(@NativeType("GLuint") int index, @NativeType("GLubyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4usv(@NativeType("GLuint") int index, @NativeType("GLushort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4usv(@NativeType("GLuint") int index, @NativeType("GLushort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4uiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nbv(@NativeType("GLuint") int index, @NativeType("GLbyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nsv(@NativeType("GLuint") int index, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nsv(@NativeType("GLuint") int index, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Niv(@NativeType("GLuint") int index, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Niv(@NativeType("GLuint") int index, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nubv(@NativeType("GLuint") int index, @NativeType("GLubyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nusv(@NativeType("GLuint") int index, @NativeType("GLushort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nusv(@NativeType("GLuint") int index, @NativeType("GLushort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nuiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttrib4Nuiv(@NativeType("GLuint") int index, @NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
        GlVertexArray.unsupportedClientPointer();
    }

    @Overwrite(remap = false)
    public static void glVertexAttribPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
        GlVertexArray.unsupportedClientPointer();
    }

    @Overwrite(remap = false)
    public static void glVertexAttribPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
        GlVertexArray.unsupportedClientPointer();
    }

    @Overwrite(remap = false)
    public static void glVertexAttribPointer(@NativeType("GLuint") int index, @NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
        GlVertexArray.unsupportedClientPointer();
    }
}
