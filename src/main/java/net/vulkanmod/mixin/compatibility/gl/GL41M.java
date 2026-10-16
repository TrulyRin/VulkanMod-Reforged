package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL41C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GL41C.class)
public class GL41M {
    private static final java.util.Set<Integer> PROGRAM_PIPELINES = new java.util.HashSet<>();
    private static int vulkanmod$nextProgramPipeline = 1;

    private static int vulkanmod$genProgramPipeline() {
        int id = vulkanmod$nextProgramPipeline++;
        PROGRAM_PIPELINES.add(id);
        return id;
    }

    private static void vulkanmod$genProgramPipelines(int[] ids) {
        if (ids == null) {
            return;
        }

        for (int i = 0; i < ids.length; i++) {
            ids[i] = vulkanmod$genProgramPipeline();
        }
    }

    private static void vulkanmod$genProgramPipelines(java.nio.IntBuffer ids) {
        if (ids == null) {
            return;
        }

        for (int i = ids.position(); i < ids.limit(); i++) {
            ids.put(i, vulkanmod$genProgramPipeline());
        }
    }

    private static void vulkanmod$deleteProgramPipelines(int id) {
        PROGRAM_PIPELINES.remove(id);
    }

    private static void vulkanmod$deleteProgramPipelines(int[] ids) {
        if (ids == null) {
            return;
        }

        for (int id : ids) {
            PROGRAM_PIPELINES.remove(id);
        }
    }

    private static void vulkanmod$deleteProgramPipelines(java.nio.IntBuffer ids) {
        if (ids == null) {
            return;
        }

        for (int i = ids.position(); i < ids.limit(); i++) {
            PROGRAM_PIPELINES.remove(ids.get(i));
        }
    }

    private static void vulkanmod$putZero(int[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0;
        }
    }

    private static void vulkanmod$putZero(java.nio.IntBuffer values) {
        if (values != null && values.remaining() > 0) {
            values.put(values.position(), 0);
        }
    }

    private static void vulkanmod$putZero(float[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0.0f;
        }
    }

    private static void vulkanmod$putZero(java.nio.FloatBuffer values) {
        if (values != null && values.remaining() > 0) {
            values.put(values.position(), 0.0f);
        }
    }

    private static void vulkanmod$putZero(double[] values) {
        if (values != null && values.length > 0) {
            values[0] = 0.0;
        }
    }

    private static void vulkanmod$putZero(java.nio.DoubleBuffer values) {
        if (values != null && values.remaining() > 0) {
            values.put(values.position(), 0.0);
        }
    }

    @Overwrite(remap = false)
    public static void glClearDepthf(@NativeType("GLfloat") float depth) {
        VRenderSystem.clearDepth(depth);
    }

    @Overwrite(remap = false)
    public static void glDepthRangef(@NativeType("GLfloat") float zNear, @NativeType("GLfloat") float zFar) {
    }

    @Overwrite(remap = false)
    public static void glProgramParameteri(@NativeType("GLuint") int program, @NativeType("GLenum") int pname, @NativeType("GLint") int value) {
    }

    @Overwrite(remap = false)
    public static void glReleaseShaderCompiler() {
    }

    @Overwrite(remap = false)
    public static void glActiveShaderProgram(int p0, int p1) {
    }

    @Overwrite(remap = false)
    public static void glBindProgramPipeline(int p0) {
    }

    @Overwrite(remap = false)
    public static int glCreateShaderProgramv(int p0, java.lang.CharSequence[] p1) {
        return net.vulkanmod.gl.GlProgram.createProgram();
    }

    @Overwrite(remap = false)
    public static int glCreateShaderProgramv(int p0, java.lang.CharSequence p1) {
        return net.vulkanmod.gl.GlProgram.createProgram();
    }

    @Overwrite(remap = false)
    public static int glCreateShaderProgramv(int p0, org.lwjgl.PointerBuffer p1) {
        return net.vulkanmod.gl.GlProgram.createProgram();
    }

    @Overwrite(remap = false)
    public static void glDeleteProgramPipelines(int[] p0) {
        vulkanmod$deleteProgramPipelines(p0);
    }

    @Overwrite(remap = false)
    public static void glDeleteProgramPipelines(int p0) {
        vulkanmod$deleteProgramPipelines(p0);
    }

    @Overwrite(remap = false)
    public static void glDeleteProgramPipelines(java.nio.IntBuffer p0) {
        vulkanmod$deleteProgramPipelines(p0);
    }

    @Overwrite(remap = false)
    public static void glDepthRangeArrayv(int p0, double[] p1) {
    }

    @Overwrite(remap = false)
    public static void glDepthRangeArrayv(int p0, java.nio.DoubleBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glDepthRangeIndexed(int p0, double p1, double p2) {
    }

    @Overwrite(remap = false)
    public static int glGenProgramPipelines() {
        return vulkanmod$genProgramPipeline();
    }

    @Overwrite(remap = false)
    public static void glGenProgramPipelines(int[] p0) {
        vulkanmod$genProgramPipelines(p0);
    }

    @Overwrite(remap = false)
    public static void glGenProgramPipelines(java.nio.IntBuffer p0) {
        vulkanmod$genProgramPipelines(p0);
    }

    @Overwrite(remap = false)
    public static double glGetDoublei(int p0, int p1) {
        return 0.0;
    }

    @Overwrite(remap = false)
    public static void glGetDoublei_v(int p0, int p1, double[] p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetDoublei_v(int p0, int p1, java.nio.DoubleBuffer p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static float glGetFloati(int p0, int p1) {
        return 0.0f;
    }

    @Overwrite(remap = false)
    public static void glGetFloati_v(int p0, int p1, float[] p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetFloati_v(int p0, int p1, java.nio.FloatBuffer p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetProgramBinary(int p0, int[] p1, int[] p2, java.nio.ByteBuffer p3) {
        vulkanmod$putZero(p1);
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetProgramBinary(int p0, java.nio.IntBuffer p1, java.nio.IntBuffer p2, java.nio.ByteBuffer p3) {
        vulkanmod$putZero(p1);
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static java.lang.String glGetProgramPipelineInfoLog(int p0) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetProgramPipelineInfoLog(int p0, int[] p1, java.nio.ByteBuffer p2) {
        vulkanmod$putZero(p1);
    }

    @Overwrite(remap = false)
    public static java.lang.String glGetProgramPipelineInfoLog(int p0, int p1) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glGetProgramPipelineInfoLog(int p0, java.nio.IntBuffer p1, java.nio.ByteBuffer p2) {
        vulkanmod$putZero(p1);
    }

    @Overwrite(remap = false)
    public static int glGetProgramPipelinei(int p0, int p1) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetProgramPipelineiv(int p0, int p1, int[] p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetProgramPipelineiv(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetShaderPrecisionFormat(int p0, int p1, int[] p2, int[] p3) {
        vulkanmod$putZero(p2);
        vulkanmod$putZero(p3);
    }

    @Overwrite(remap = false)
    public static int glGetShaderPrecisionFormat(int p0, int p1, java.nio.IntBuffer p2) {
        vulkanmod$putZero(p2);
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetShaderPrecisionFormat(int p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3) {
        vulkanmod$putZero(p2);
        vulkanmod$putZero(p3);
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribLdv(int p0, int p1, double[] p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static void glGetVertexAttribLdv(int p0, int p1, java.nio.DoubleBuffer p2) {
        vulkanmod$putZero(p2);
    }

    @Overwrite(remap = false)
    public static boolean glIsProgramPipeline(int p0) {
        return PROGRAM_PIPELINES.contains(p0);
    }

    @Overwrite(remap = false)
    public static void glProgramBinary(int p0, int p1, java.nio.ByteBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1d(int p0, int p1, double p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1dv(int p0, int p1, double[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1dv(int p0, int p1, java.nio.DoubleBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1f(int p0, int p1, float p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1fv(int p0, int p1, float[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1fv(int p0, int p1, java.nio.FloatBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1i(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1iv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1iv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1ui(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1uiv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform1uiv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2d(int p0, int p1, double p2, double p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2dv(int p0, int p1, double[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2dv(int p0, int p1, java.nio.DoubleBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2f(int p0, int p1, float p2, float p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2fv(int p0, int p1, float[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2fv(int p0, int p1, java.nio.FloatBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2i(int p0, int p1, int p2, int p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2iv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2iv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2ui(int p0, int p1, int p2, int p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2uiv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform2uiv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3d(int p0, int p1, double p2, double p3, double p4) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3dv(int p0, int p1, double[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3dv(int p0, int p1, java.nio.DoubleBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3f(int p0, int p1, float p2, float p3, float p4) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3fv(int p0, int p1, float[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3fv(int p0, int p1, java.nio.FloatBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3i(int p0, int p1, int p2, int p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3iv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3iv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3ui(int p0, int p1, int p2, int p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3uiv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform3uiv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4d(int p0, int p1, double p2, double p3, double p4, double p5) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4dv(int p0, int p1, double[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4dv(int p0, int p1, java.nio.DoubleBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4f(int p0, int p1, float p2, float p3, float p4, float p5) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4fv(int p0, int p1, float[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4fv(int p0, int p1, java.nio.FloatBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4i(int p0, int p1, int p2, int p3, int p4, int p5) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4iv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4iv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4ui(int p0, int p1, int p2, int p3, int p4, int p5) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4uiv(int p0, int p1, int[] p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniform4uiv(int p0, int p1, java.nio.IntBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x3dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x3dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x3fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x3fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x4dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x4dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x4fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix2x4fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x2dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x2dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x2fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x2fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x4dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x4dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x4fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix3x4fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x2dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x2dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x2fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x2fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x3dv(int p0, int p1, boolean p2, double[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x3dv(int p0, int p1, boolean p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x3fv(int p0, int p1, boolean p2, float[] p3) {
    }

    @Overwrite(remap = false)
    public static void glProgramUniformMatrix4x3fv(int p0, int p1, boolean p2, java.nio.FloatBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glScissorArrayv(int p0, int[] p1) {
    }

    @Overwrite(remap = false)
    public static void glScissorArrayv(int p0, java.nio.IntBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glScissorIndexed(int p0, int p1, int p2, int p3, int p4) {
    }

    @Overwrite(remap = false)
    public static void glScissorIndexedv(int p0, int[] p1) {
    }

    @Overwrite(remap = false)
    public static void glScissorIndexedv(int p0, java.nio.IntBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glShaderBinary(int[] p0, int p1, java.nio.ByteBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glShaderBinary(java.nio.IntBuffer p0, int p1, java.nio.ByteBuffer p2) {
    }

    @Overwrite(remap = false)
    public static void glUseProgramStages(int p0, int p1, int p2) {
    }

    @Overwrite(remap = false)
    public static void glValidateProgramPipeline(int p0) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL1d(int p0, double p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL1dv(int p0, double[] p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL1dv(int p0, java.nio.DoubleBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL2d(int p0, double p1, double p2) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL2dv(int p0, double[] p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL2dv(int p0, java.nio.DoubleBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL3d(int p0, double p1, double p2, double p3) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL3dv(int p0, double[] p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL3dv(int p0, java.nio.DoubleBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL4d(int p0, double p1, double p2, double p3, double p4) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL4dv(int p0, double[] p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribL4dv(int p0, java.nio.DoubleBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribLPointer(int p0, int p1, int p2, int p3, java.nio.ByteBuffer p4) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribLPointer(int p0, int p1, int p2, int p3, long p4) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribLPointer(int p0, int p1, int p2, java.nio.DoubleBuffer p3) {
    }

    @Overwrite(remap = false)
    public static void glViewportArrayv(int p0, float[] p1) {
    }

    @Overwrite(remap = false)
    public static void glViewportArrayv(int p0, java.nio.FloatBuffer p1) {
    }

    @Overwrite(remap = false)
    public static void glViewportIndexedf(int p0, float p1, float p2, float p3, float p4) {
    }

    @Overwrite(remap = false)
    public static void glViewportIndexedfv(int p0, float[] p1) {
    }

    @Overwrite(remap = false)
    public static void glViewportIndexedfv(int p0, java.nio.FloatBuffer p1) {
    }
}

