package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlProgram;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Mixin(ARBShaderObjects.class)
public class ARBShaderObjectsM {
    @Unique
    private static int vulkanmod$nextObjectHandle = 1;
    @Unique
    private static int vulkanmod$currentProgramObject;
    @Unique
    private static final Map<Integer, Integer> vulkanmod$programObjects = new HashMap<>();
    @Unique
    private static final Map<Integer, Integer> vulkanmod$shaderObjects = new HashMap<>();
    @Unique
    private static final Map<Integer, Integer> vulkanmod$shaderTypes = new HashMap<>();

    @Overwrite(remap = false)
    public static void glDeleteObjectARB(@NativeType("GLhandleARB") int obj) {
        Integer program = vulkanmod$programObjects.remove(obj);
        if (program != null) {
            GlProgram.deleteProgram(program);
            if (vulkanmod$currentProgramObject == obj) {
                vulkanmod$currentProgramObject = 0;
            }
        } else {
            Integer shader = vulkanmod$shaderObjects.remove(obj);
            vulkanmod$shaderTypes.remove(obj);
            if (shader != null) {
                GlProgram.deleteShader(shader);
            }
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLhandleARB")
    public static int glGetHandleARB(@NativeType("GLenum") int pname) {
        return pname == ARBShaderObjects.GL_PROGRAM_OBJECT_ARB ? vulkanmod$currentProgramObject : 0;
    }

    @Overwrite(remap = false)
    public static void glDetachObjectARB(@NativeType("GLhandleARB") int containerObj, @NativeType("GLhandleARB") int attachedObj) {
    }

    @Overwrite(remap = false)
    @NativeType("GLhandleARB")
    public static int glCreateShaderObjectARB(@NativeType("GLenum") int shaderType) {
        int handle = vulkanmod$genObjectHandle();
        vulkanmod$shaderObjects.put(handle, GlProgram.createShader(shaderType));
        vulkanmod$shaderTypes.put(handle, shaderType);
        return handle;
    }

    @Overwrite(remap = false)
    public static void glShaderSourceARB(@NativeType("GLhandleARB") int shaderObj, @NativeType("GLcharARB const **") PointerBuffer strings, @NativeType("GLint const *") IntBuffer length) {
        GlProgram.shaderSource(vulkanmod$coreShader(shaderObj), "");
    }

    @Overwrite(remap = false)
    public static void glShaderSourceARB(@NativeType("GLhandleARB") int shaderObj, @NativeType("GLcharARB const **") PointerBuffer strings, @NativeType("GLint const *") int[] length) {
        GlProgram.shaderSource(vulkanmod$coreShader(shaderObj), "");
    }

    @Overwrite(remap = false)
    public static void glShaderSourceARB(@NativeType("GLhandleARB") int shaderObj, @NativeType("GLcharARB const **") CharSequence... strings) {
        GlProgram.shaderSource(vulkanmod$coreShader(shaderObj), strings != null && strings.length > 0 ? strings[0] : "");
    }

    @Overwrite(remap = false)
    public static void glShaderSourceARB(@NativeType("GLhandleARB") int shaderObj, @NativeType("GLcharARB const *") CharSequence string) {
        GlProgram.shaderSource(vulkanmod$coreShader(shaderObj), string);
    }

    @Overwrite(remap = false)
    public static void glCompileShaderARB(@NativeType("GLhandleARB") int shaderObj) {
        GlProgram.compileShader(vulkanmod$coreShader(shaderObj));
    }

    @Overwrite(remap = false)
    @NativeType("GLhandleARB")
    public static int glCreateProgramObjectARB() {
        int handle = vulkanmod$genObjectHandle();
        vulkanmod$programObjects.put(handle, GlProgram.createProgram());
        return handle;
    }

    @Overwrite(remap = false)
    public static void glAttachObjectARB(@NativeType("GLhandleARB") int containerObj, @NativeType("GLhandleARB") int obj) {
        GlProgram.attachShader(vulkanmod$coreProgram(containerObj), vulkanmod$coreShader(obj));
    }

    @Overwrite(remap = false)
    public static void glLinkProgramARB(@NativeType("GLhandleARB") int programObj) {
        GlProgram.linkProgram(vulkanmod$coreProgram(programObj));
    }

    @Overwrite(remap = false)
    public static void glUseProgramObjectARB(@NativeType("GLhandleARB") int programObj) {
        vulkanmod$currentProgramObject = vulkanmod$programObjects.containsKey(programObj) ? programObj : 0;
        GlProgram.useProgram(vulkanmod$coreProgram(programObj));
    }

    @Overwrite(remap = false)
    public static void glValidateProgramARB(@NativeType("GLhandleARB") int programObj) {
    }

    @Overwrite(remap = false)
    public static void glUniform1fARB(@NativeType("GLint") int location, @NativeType("GLfloat") float v0) {
        GlProgram.uniform1f(location, v0);
    }

    @Overwrite(remap = false)
    public static void glUniform2fARB(@NativeType("GLint") int location, @NativeType("GLfloat") float v0, @NativeType("GLfloat") float v1) {
        GlProgram.uniform2f(location, v0, v1);
    }

    @Overwrite(remap = false)
    public static void glUniform3fARB(@NativeType("GLint") int location, @NativeType("GLfloat") float v0, @NativeType("GLfloat") float v1, @NativeType("GLfloat") float v2) {
        GlProgram.uniform3f(location, v0, v1, v2);
    }

    @Overwrite(remap = false)
    public static void glUniform4fARB(@NativeType("GLint") int location, @NativeType("GLfloat") float v0, @NativeType("GLfloat") float v1, @NativeType("GLfloat") float v2, @NativeType("GLfloat") float v3) {
        GlProgram.uniform4f(location, v0, v1, v2, v3);
    }

    @Overwrite(remap = false)
    public static void glUniform1iARB(@NativeType("GLint") int location, @NativeType("GLint") int v0) {
        GlProgram.uniform1i(location, v0);
    }

    @Overwrite(remap = false)
    public static void glUniform2iARB(@NativeType("GLint") int location, @NativeType("GLint") int v0, @NativeType("GLint") int v1) {
    }

    @Overwrite(remap = false)
    public static void glUniform3iARB(@NativeType("GLint") int location, @NativeType("GLint") int v0, @NativeType("GLint") int v1, @NativeType("GLint") int v2) {
        GlProgram.uniform3i(location, v0, v1, v2);
    }

    @Overwrite(remap = false)
    public static void glUniform4iARB(@NativeType("GLint") int location, @NativeType("GLint") int v0, @NativeType("GLint") int v1, @NativeType("GLint") int v2, @NativeType("GLint") int v3) {
    }

    @Overwrite(remap = false)
    public static void glUniform1fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        if (value != null && value.remaining() >= 1) {
            GlProgram.uniform1f(location, value.get(value.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform2fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        if (value != null && value.remaining() >= 2) {
            GlProgram.uniform2f(location, value.get(value.position()), value.get(value.position() + 1));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform3fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        if (value != null && value.remaining() >= 3) {
            GlProgram.uniform3f(location, value.get(value.position()), value.get(value.position() + 1), value.get(value.position() + 2));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform4fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") FloatBuffer value) {
        if (value != null && value.remaining() >= 4) {
            GlProgram.uniform4f(location, value.get(value.position()), value.get(value.position() + 1), value.get(value.position() + 2), value.get(value.position() + 3));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform1ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
        if (value != null && value.remaining() >= 1) {
            GlProgram.uniform1i(location, value.get(value.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform2ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform3ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
        if (value != null && value.remaining() >= 3) {
            GlProgram.uniform3i(location, value.get(value.position()), value.get(value.position() + 1), value.get(value.position() + 2));
        }
    }

    @Overwrite(remap = false)
    public static void glUniform4ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2fvARB(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") FloatBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3fvARB(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") FloatBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4fvARB(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") FloatBuffer value) {
        GlProgram.uniformMatrix4fv(location, transpose, value);
    }

    @Overwrite(remap = false)
    public static void glGetObjectParameterfvARB(@NativeType("GLhandleARB") int obj, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), glGetObjectParameteriARB(obj, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetObjectParameterfvARB(@NativeType("GLhandleARB") int obj, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] params) {
        if (params != null && params.length > 0) {
            params[0] = glGetObjectParameteriARB(obj, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetObjectParameterivARB(@NativeType("GLhandleARB") int obj, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), glGetObjectParameteriARB(obj, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetObjectParameterivARB(@NativeType("GLhandleARB") int obj, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = glGetObjectParameteriARB(obj, pname);
        }
    }

    @Overwrite(remap = false)
    public static int glGetObjectParameteriARB(@NativeType("GLhandleARB") int obj, @NativeType("GLenum") int pname) {
        if (pname == ARBShaderObjects.GL_OBJECT_TYPE_ARB) {
            if (vulkanmod$programObjects.containsKey(obj)) {
                return ARBShaderObjects.GL_PROGRAM_OBJECT_ARB;
            }
            return vulkanmod$shaderObjects.containsKey(obj) ? ARBShaderObjects.GL_SHADER_OBJECT_ARB : 0;
        }

        if (pname == ARBShaderObjects.GL_OBJECT_SUBTYPE_ARB) {
            return vulkanmod$shaderTypes.getOrDefault(obj, 0);
        }

        if (vulkanmod$programObjects.containsKey(obj)) {
            return GlProgram.getProgrami(vulkanmod$coreProgram(obj), mapObjectParameter(pname));
        }
        return GlProgram.getShaderi(vulkanmod$coreShader(obj), mapObjectParameter(pname));
    }

    @Overwrite(remap = false)
    public static void glGetInfoLogARB(@NativeType("GLhandleARB") int obj, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLcharARB *") ByteBuffer infoLog) {
        if (length != null && length.remaining() > 0) {
            length.put(length.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glGetInfoLogARB(@NativeType("GLhandleARB") int obj, @NativeType("GLsizei *") int[] length, @NativeType("GLcharARB *") ByteBuffer infoLog) {
        if (length != null && length.length > 0) {
            length[0] = 0;
        }
    }

    @Overwrite(remap = false)
    public static String glGetInfoLogARB(@NativeType("GLhandleARB") int obj, @NativeType("GLsizei") int maxLength) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetInfoLogARB(@NativeType("GLhandleARB") int obj) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetAttachedObjectsARB(@NativeType("GLhandleARB") int containerObj, @NativeType("GLsizei *") IntBuffer count, @NativeType("GLhandleARB *") IntBuffer obj) {
        if (count != null && count.remaining() > 0) {
            count.put(count.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glGetAttachedObjectsARB(@NativeType("GLhandleARB") int containerObj, @NativeType("GLsizei *") int[] count, @NativeType("GLhandleARB *") int[] obj) {
        if (count != null && count.length > 0) {
            count[0] = 0;
        }
    }

    @Overwrite(remap = false)
    public static int glGetUniformLocationARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLcharARB const *") ByteBuffer name) {
        return GlProgram.getUniformLocation(vulkanmod$coreProgram(programObj), "");
    }

    @Overwrite(remap = false)
    public static int glGetUniformLocationARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLcharARB const *") CharSequence name) {
        return GlProgram.getUniformLocation(vulkanmod$coreProgram(programObj), name);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type, @NativeType("GLcharARB *") ByteBuffer name) {
        putZero(length);
        putZero(size);
        putZero(type);
    }

    @Overwrite(remap = false)
    public static void glGetActiveUniformARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLuint") int index, @NativeType("GLsizei *") int[] length, @NativeType("GLint *") int[] size, @NativeType("GLenum *") int[] type, @NativeType("GLcharARB *") ByteBuffer name) {
        putZero(length);
        putZero(size);
        putZero(type);
    }

    @Overwrite(remap = false)
    public static String glGetActiveUniformARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLuint") int index, @NativeType("GLsizei") int maxLength, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        putZero(size);
        putZero(type);
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetActiveUniformARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLuint") int index, @NativeType("GLint *") IntBuffer size, @NativeType("GLenum *") IntBuffer type) {
        putZero(size);
        putZero(type);
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetUniformfvARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLint") int location, @NativeType("GLfloat *") FloatBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), 0.0f);
        }
    }

    @Overwrite(remap = false)
    public static void glGetUniformfvARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLint") int location, @NativeType("GLfloat *") float[] params) {
        if (params != null && params.length > 0) {
            params[0] = 0.0f;
        }
    }

    @Overwrite(remap = false)
    public static float glGetUniformfARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLint") int location) {
        return 0.0f;
    }

    @Overwrite(remap = false)
    public static void glGetUniformivARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLint") int location, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glGetUniformivARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLint") int location, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = 0;
        }
    }

    @Overwrite(remap = false)
    public static int glGetUniformiARB(@NativeType("GLhandleARB") int programObj, @NativeType("GLint") int location) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetShaderSourceARB(@NativeType("GLhandleARB") int obj, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLcharARB *") ByteBuffer source) {
        putZero(length);
    }

    @Overwrite(remap = false)
    public static void glGetShaderSourceARB(@NativeType("GLhandleARB") int obj, @NativeType("GLsizei *") int[] length, @NativeType("GLcharARB *") ByteBuffer source) {
        putZero(length);
    }

    @Overwrite(remap = false)
    public static String glGetShaderSourceARB(@NativeType("GLhandleARB") int obj, @NativeType("GLsizei") int maxLength) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetShaderSourceARB(@NativeType("GLhandleARB") int obj) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glUniform1fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        if (value != null && value.length >= 1) {
            GlProgram.uniform1f(location, value[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform2fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        if (value != null && value.length >= 2) {
            GlProgram.uniform2f(location, value[0], value[1]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform3fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        if (value != null && value.length >= 3) {
            GlProgram.uniform3f(location, value[0], value[1], value[2]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform4fvARB(@NativeType("GLint") int location, @NativeType("GLfloat const *") float[] value) {
        if (value != null && value.length >= 4) {
            GlProgram.uniform4f(location, value[0], value[1], value[2], value[3]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform1ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
        if (value != null && value.length >= 1) {
            GlProgram.uniform1i(location, value[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform2ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform3ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
        if (value != null && value.length >= 3) {
            GlProgram.uniform3i(location, value[0], value[1], value[2]);
        }
    }

    @Overwrite(remap = false)
    public static void glUniform4ivARB(@NativeType("GLint") int location, @NativeType("GLint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2fvARB(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") float[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3fvARB(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") float[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4fvARB(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLfloat const *") float[] value) {
        if (value != null) {
            GlProgram.uniformMatrix4fv(location, transpose, FloatBuffer.wrap(value));
        }
    }

    @Unique
    private static int mapObjectParameter(int pname) {
        return switch (pname) {
            case ARBShaderObjects.GL_OBJECT_DELETE_STATUS_ARB -> GL20.GL_DELETE_STATUS;
            case ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB -> GL20.GL_COMPILE_STATUS;
            case ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB -> GL20.GL_LINK_STATUS;
            case ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB -> GL20.GL_VALIDATE_STATUS;
            case ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB -> GL20.GL_INFO_LOG_LENGTH;
            case ARBShaderObjects.GL_OBJECT_ATTACHED_OBJECTS_ARB -> GL20.GL_ATTACHED_SHADERS;
            case ARBShaderObjects.GL_OBJECT_ACTIVE_UNIFORMS_ARB -> GL20.GL_ACTIVE_UNIFORMS;
            case ARBShaderObjects.GL_OBJECT_ACTIVE_UNIFORM_MAX_LENGTH_ARB -> GL20.GL_ACTIVE_UNIFORM_MAX_LENGTH;
            case ARBShaderObjects.GL_OBJECT_SHADER_SOURCE_LENGTH_ARB -> GL20.GL_SHADER_SOURCE_LENGTH;
            default -> pname;
        };
    }

    @Unique
    private static int vulkanmod$genObjectHandle() {
        while (vulkanmod$programObjects.containsKey(vulkanmod$nextObjectHandle)
                || vulkanmod$shaderObjects.containsKey(vulkanmod$nextObjectHandle)) {
            vulkanmod$nextObjectHandle++;
        }
        return vulkanmod$nextObjectHandle++;
    }

    @Unique
    private static int vulkanmod$coreProgram(int handle) {
        return vulkanmod$programObjects.getOrDefault(handle, 0);
    }

    @Unique
    private static int vulkanmod$coreShader(int handle) {
        return vulkanmod$shaderObjects.getOrDefault(handle, 0);
    }

    @Unique
    private static void putZero(IntBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), 0);
        }
    }

    @Unique
    private static void putZero(int[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0;
        }
    }
}

