package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL40C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

@Mixin(GL40C.class)
public class GL40M {
    private static final Set<Integer> TRANSFORM_FEEDBACKS = new HashSet<>();
    private static int vulkanmod$nextTransformFeedback = 1;

    private static int vulkanmod$genTransformFeedback() {
        int id = vulkanmod$nextTransformFeedback++;
        TRANSFORM_FEEDBACKS.add(id);
        return id;
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

    private static void vulkanmod$putZero(DoubleBuffer buffer) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), 0.0);
        }
    }

    private static void vulkanmod$putZero(double[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0.0;
        }
    }

    @Overwrite(remap = false)
    public static void glBlendFunci(@NativeType("GLuint") int buf, @NativeType("GLenum") int sfactor, @NativeType("GLenum") int dfactor) {
        if (buf == 0) {
            VRenderSystem.blendFunc(sfactor, dfactor);
        }
    }

    @Overwrite(remap = false)
    public static void glBlendFuncSeparatei(@NativeType("GLuint") int buf, @NativeType("GLenum") int srcRGB, @NativeType("GLenum") int dstRGB, @NativeType("GLenum") int srcAlpha, @NativeType("GLenum") int dstAlpha) {
        if (buf == 0) {
            VRenderSystem.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
        }
    }

    @Overwrite(remap = false)
    public static void glBlendEquationi(@NativeType("GLuint") int buf, @NativeType("GLenum") int mode) {
    }

    @Overwrite(remap = false)
    public static void glBlendEquationSeparatei(@NativeType("GLuint") int buf, @NativeType("GLenum") int modeRGB, @NativeType("GLenum") int modeAlpha) {
    }

    @Overwrite(remap = false)
    public static void glMinSampleShading(@NativeType("GLfloat") float value) {
    }

    @Overwrite(remap = false)
    public static void glPatchParameteri(@NativeType("GLenum") int pname, @NativeType("GLint") int value) {
    }

    @Overwrite(remap = false)
    public static void glPatchParameterfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer values) {
    }

    @Overwrite(remap = false)
    public static void glPatchParameterfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] values) {
    }

    @Overwrite(remap = false)
    public static void glDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") long indirect) {
    }

    @Overwrite(remap = false)
    public static void glDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") ByteBuffer indirect) {
    }

    @Overwrite(remap = false)
    public static void glDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") IntBuffer indirect) {
    }

    @Overwrite(remap = false)
    public static void glDrawArraysIndirect(@NativeType("GLenum") int mode, @NativeType("void const *") int[] indirect) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") long indirect) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer indirect) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") IntBuffer indirect) {
    }

    @Overwrite(remap = false)
    public static void glDrawElementsIndirect(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") int[] indirect) {
    }

    @Overwrite(remap = false)
    public static void glBindTransformFeedback(@NativeType("GLenum") int target, @NativeType("GLuint") int id) {
        if (id != 0) {
            TRANSFORM_FEEDBACKS.add(id);
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteTransformFeedbacks(@NativeType("GLuint") int id) {
        TRANSFORM_FEEDBACKS.remove(id);
    }

    @Overwrite(remap = false)
    public static void glDeleteTransformFeedbacks(@NativeType("GLuint const *") IntBuffer ids) {
        if (ids == null) {
            return;
        }

        for (int i = ids.position(); i < ids.limit(); i++) {
            TRANSFORM_FEEDBACKS.remove(ids.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteTransformFeedbacks(@NativeType("GLuint const *") int[] ids) {
        if (ids == null) {
            return;
        }

        for (int id : ids) {
            TRANSFORM_FEEDBACKS.remove(id);
        }
    }

    @Overwrite(remap = false)
    public static int glGenTransformFeedbacks() {
        return vulkanmod$genTransformFeedback();
    }

    @Overwrite(remap = false)
    public static void glGenTransformFeedbacks(@NativeType("GLuint *") IntBuffer ids) {
        if (ids == null) {
            return;
        }

        for (int i = ids.position(); i < ids.limit(); i++) {
            ids.put(i, vulkanmod$genTransformFeedback());
        }
    }

    @Overwrite(remap = false)
    public static void glGenTransformFeedbacks(@NativeType("GLuint *") int[] ids) {
        if (ids == null) {
            return;
        }

        for (int i = 0; i < ids.length; i++) {
            ids[i] = vulkanmod$genTransformFeedback();
        }
    }

    @Overwrite(remap = false)
    public static boolean glIsTransformFeedback(@NativeType("GLuint") int id) {
        return TRANSFORM_FEEDBACKS.contains(id);
    }

    @Overwrite(remap = false)
    public static void glPauseTransformFeedback() {
    }

    @Overwrite(remap = false)
    public static void glResumeTransformFeedback() {
    }

    @Overwrite(remap = false)
    public static void glDrawTransformFeedback(@NativeType("GLenum") int mode, @NativeType("GLuint") int id) {
    }

    @Overwrite(remap = false)
    public static void glDrawTransformFeedbackStream(@NativeType("GLenum") int mode, @NativeType("GLuint") int id, @NativeType("GLuint") int stream) {
    }

    @Overwrite(remap = false)
    public static void glBeginQueryIndexed(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLuint") int id) {
    }

    @Overwrite(remap = false)
    public static void glEndQueryIndexed(@NativeType("GLenum") int target, @NativeType("GLuint") int index) {
    }

    @Overwrite(remap = false)
    public static int glGetQueryIndexedi(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetQueryIndexediv(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetQueryIndexediv(@NativeType("GLenum") int target, @NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glUniform1d(@NativeType("GLint") int location, @NativeType("GLdouble") double x) {
    }

    @Overwrite(remap = false)
    public static void glUniform2d(@NativeType("GLint") int location, @NativeType("GLdouble") double x, @NativeType("GLdouble") double y) {
    }

    @Overwrite(remap = false)
    public static void glUniform3d(@NativeType("GLint") int location, @NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glUniform4d(@NativeType("GLint") int location, @NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z, @NativeType("GLdouble") double w) {
    }

    @Overwrite(remap = false)
    public static void glUniform1dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform1dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform2dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform2dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform3dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform3dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniform4dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniform4dv(@NativeType("GLint") int location, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2x3dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2x3dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3x2dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3x2dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2x4dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix2x4dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4x2dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4x2dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3x4dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix3x4dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4x3dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") DoubleBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glUniformMatrix4x3dv(@NativeType("GLint") int location, @NativeType("GLboolean") boolean transpose, @NativeType("GLdouble const *") double[] value) {
    }

    @Overwrite(remap = false)
    public static double glGetUniformd(@NativeType("GLuint") int program, @NativeType("GLint") int location) {
        return 0.0;
    }

    @Overwrite(remap = false)
    public static void glGetUniformdv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLdouble *") DoubleBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetUniformdv(@NativeType("GLuint") int program, @NativeType("GLint") int location, @NativeType("GLdouble *") double[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static int glGetSubroutineUniformLocation(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLchar const *") ByteBuffer name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetSubroutineUniformLocation(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLchar const *") CharSequence name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetSubroutineIndex(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLchar const *") ByteBuffer name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetSubroutineIndex(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLchar const *") CharSequence name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetActiveSubroutineUniformi(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetActiveSubroutineUniformiv(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer values) {
        vulkanmod$putZero(values);
    }

    @Overwrite(remap = false)
    public static void glGetActiveSubroutineUniformiv(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] values) {
        vulkanmod$putZero(values);
    }

    @Overwrite(remap = false)
    public static String glGetActiveSubroutineUniformName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetActiveSubroutineUniformName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetActiveSubroutineUniformName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glGetActiveSubroutineUniformName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static String glGetActiveSubroutineName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetActiveSubroutineName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetActiveSubroutineName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glGetActiveSubroutineName(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLuint") int index, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer name) {
        vulkanmod$putZero(length);
    }

    @Overwrite(remap = false)
    public static void glUniformSubroutinesui(@NativeType("GLenum") int shadertype, @NativeType("GLuint") int index) {
    }

    @Overwrite(remap = false)
    public static void glUniformSubroutinesuiv(@NativeType("GLenum") int shadertype, @NativeType("GLuint const *") IntBuffer indices) {
    }

    @Overwrite(remap = false)
    public static void glUniformSubroutinesuiv(@NativeType("GLenum") int shadertype, @NativeType("GLuint const *") int[] indices) {
    }

    @Overwrite(remap = false)
    public static int glGetUniformSubroutineui(@NativeType("GLenum") int shadertype, @NativeType("GLint") int location) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetUniformSubroutineuiv(@NativeType("GLenum") int shadertype, @NativeType("GLint") int location, @NativeType("GLuint *") IntBuffer params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static void glGetUniformSubroutineuiv(@NativeType("GLenum") int shadertype, @NativeType("GLint") int location, @NativeType("GLuint *") int[] params) {
        vulkanmod$putZero(params);
    }

    @Overwrite(remap = false)
    public static int glGetProgramStagei(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetProgramStageiv(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer values) {
        vulkanmod$putZero(values);
    }

    @Overwrite(remap = false)
    public static void glGetProgramStageiv(@NativeType("GLuint") int program, @NativeType("GLenum") int shadertype, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] values) {
        vulkanmod$putZero(values);
    }
}

