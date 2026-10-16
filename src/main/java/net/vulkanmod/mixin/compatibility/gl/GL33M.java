package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlQuery;
import net.vulkanmod.gl.GlSampler;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

@Mixin(GL33C.class)
public class GL33M {
    private static void vulkanmod$put(IntBuffer buffer, int value) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void vulkanmod$put(int[] values, int value) {
        if (values != null && values.length > 0) {
            values[0] = value;
        }
    }

    private static void vulkanmod$put(FloatBuffer buffer, float value) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void vulkanmod$put(float[] values, float value) {
        if (values != null && values.length > 0) {
            values[0] = value;
        }
    }

    private static void vulkanmod$put(LongBuffer buffer, long value) {
        if (buffer != null && buffer.remaining() > 0) {
            buffer.put(buffer.position(), value);
        }
    }

    private static void vulkanmod$put(long[] values, long value) {
        if (values != null && values.length > 0) {
            values[0] = value;
        }
    }

    @Overwrite(remap = false)
    public static void glVertexAttribDivisor(@NativeType("GLuint") int index, @NativeType("GLuint") int divisor) {
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenSamplers() {
        return GlSampler.genSamplers();
    }

    @Overwrite(remap = false)
    public static void glGenSamplers(@NativeType("GLuint *") IntBuffer samplers) {
        GlSampler.genSamplers(samplers);
    }

    @Overwrite(remap = false)
    public static void glGenSamplers(@NativeType("GLuint *") int[] samplers) {
        if (samplers == null) {
            return;
        }
        for (int i = 0; i < samplers.length; i++) {
            samplers[i] = GlSampler.genSamplers();
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteSamplers(@NativeType("GLuint const *") int sampler) {
        GlSampler.deleteSamplers(sampler);
    }

    @Overwrite(remap = false)
    public static void glDeleteSamplers(@NativeType("GLuint const *") IntBuffer samplers) {
        GlSampler.deleteSamplers(samplers);
    }

    @Overwrite(remap = false)
    public static void glDeleteSamplers(@NativeType("GLuint const *") int[] samplers) {
        if (samplers == null) {
            return;
        }
        for (int sampler : samplers) {
            GlSampler.deleteSamplers(sampler);
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsSampler(@NativeType("GLuint") int sampler) {
        return GlSampler.isSampler(sampler);
    }

    @Overwrite(remap = false)
    public static void glBindSampler(@NativeType("GLuint") int unit, @NativeType("GLuint") int sampler) {
        GlSampler.bindSampler(unit, sampler);
    }

    @Overwrite(remap = false)
    public static void glSamplerParameteri(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
        GlSampler.samplerParameteri(sampler, pname, param);
    }

    @Overwrite(remap = false)
    public static void glSamplerParameterf(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
        GlSampler.samplerParameterf(sampler, pname, param);
    }

    @Overwrite(remap = false)
    public static void glSamplerParameteriv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            GlSampler.samplerParameteri(sampler, pname, params.get(params.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glSamplerParameteriv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
        if (params != null && params.length > 0) {
            GlSampler.samplerParameteri(sampler, pname, params[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glSamplerParameterfv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
        if (params != null && params.remaining() > 0) {
            GlSampler.samplerParameterf(sampler, pname, params.get(params.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glSamplerParameterfv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
        if (params != null && params.length > 0) {
            GlSampler.samplerParameterf(sampler, pname, params[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glSamplerParameterIiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
        glSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static void glSamplerParameterIiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
        glSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static void glSamplerParameterIuiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLuint const *") IntBuffer params) {
        glSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static void glSamplerParameterIuiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLuint const *") int[] params) {
        glSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static int glGetSamplerParameteri(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname) {
        return GlSampler.getSamplerParameteri(sampler, pname);
    }

    @Overwrite(remap = false)
    public static float glGetSamplerParameterf(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname) {
        return GlSampler.getSamplerParameterf(sampler, pname);
    }

    @Overwrite(remap = false)
    public static int glGetSamplerParameterIi(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname) {
        return GlSampler.getSamplerParameteri(sampler, pname);
    }

    @Overwrite(remap = false)
    @NativeType("GLuint")
    public static int glGetSamplerParameterIui(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname) {
        return GlSampler.getSamplerParameteri(sampler, pname);
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameteriv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$put(params, GlSampler.getSamplerParameteri(sampler, pname));
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameteriv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        vulkanmod$put(params, GlSampler.getSamplerParameteri(sampler, pname));
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameterfv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer params) {
        vulkanmod$put(params, GlSampler.getSamplerParameterf(sampler, pname));
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameterfv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] params) {
        vulkanmod$put(params, GlSampler.getSamplerParameterf(sampler, pname));
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameterIiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        glGetSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameterIiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        glGetSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameterIuiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLuint *") IntBuffer params) {
        glGetSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static void glGetSamplerParameterIuiv(@NativeType("GLuint") int sampler, @NativeType("GLenum") int pname, @NativeType("GLuint *") int[] params) {
        glGetSamplerParameteriv(sampler, pname, params);
    }

    @Overwrite(remap = false)
    public static void glQueryCounter(@NativeType("GLuint") int id, @NativeType("GLenum") int target) {
        GlQuery.queryCounter(id, target);
    }

    @Overwrite(remap = false)
    public static long glGetQueryObjecti64(@NativeType("GLuint") int id, @NativeType("GLenum") int pname) {
        return GlQuery.getQueryObject(id, pname);
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjecti64v(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLint64 *") LongBuffer params) {
        vulkanmod$put(params, GlQuery.getQueryObject(id, pname));
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjecti64v(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLint64 *") long[] params) {
        vulkanmod$put(params, GlQuery.getQueryObject(id, pname));
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjecti64v(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLint64 *") long params) {
    }

    @Overwrite(remap = false)
    public static long glGetQueryObjectui64(@NativeType("GLuint") int id, @NativeType("GLenum") int pname) {
        return GlQuery.getQueryObject(id, pname);
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectui64v(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLuint64 *") LongBuffer params) {
        vulkanmod$put(params, GlQuery.getQueryObject(id, pname));
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectui64v(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLuint64 *") long[] params) {
        vulkanmod$put(params, GlQuery.getQueryObject(id, pname));
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectui64v(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLuint64 *") long params) {
    }

    @Overwrite(remap = false)
    public static int glGetFragDataIndex(@NativeType("GLuint") int program, @NativeType("GLchar const *") CharSequence name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static int glGetFragDataIndex(@NativeType("GLuint") int program, @NativeType("GLchar const *") ByteBuffer name) {
        return -1;
    }

    @Overwrite(remap = false)
    public static void glBindFragDataLocationIndexed(@NativeType("GLuint") int program, @NativeType("GLuint") int colorNumber, @NativeType("GLuint") int index, @NativeType("GLchar const *") CharSequence name) {
    }

    @Overwrite(remap = false)
    public static void glBindFragDataLocationIndexed(@NativeType("GLuint") int program, @NativeType("GLuint") int colorNumber, @NativeType("GLuint") int index, @NativeType("GLchar const *") ByteBuffer name) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP1ui(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint") int value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP2ui(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint") int value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP3ui(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint") int value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP4ui(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint") int value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP1uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP1uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP2uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP2uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP3uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP3uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP4uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glVertexAttribP4uiv(@NativeType("GLuint") int index, @NativeType("GLenum") int type, @NativeType("GLboolean") boolean normalized, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP2ui(@NativeType("GLenum") int type, @NativeType("GLuint") int value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP3ui(@NativeType("GLenum") int type, @NativeType("GLuint") int value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP4ui(@NativeType("GLenum") int type, @NativeType("GLuint") int value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP2uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP2uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP4uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer value) {
    }

    @Overwrite(remap = false)
    public static void glVertexP4uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] value) {
    }

    @Overwrite(remap = false)
    public static void glColorP3ui(@NativeType("GLenum") int type, @NativeType("GLuint") int color) {
    }

    @Overwrite(remap = false)
    public static void glColorP4ui(@NativeType("GLenum") int type, @NativeType("GLuint") int color) {
    }

    @Overwrite(remap = false)
    public static void glColorP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer color) {
    }

    @Overwrite(remap = false)
    public static void glColorP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] color) {
    }

    @Overwrite(remap = false)
    public static void glColorP4uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer color) {
    }

    @Overwrite(remap = false)
    public static void glColorP4uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] color) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorP3ui(@NativeType("GLenum") int type, @NativeType("GLuint") int color) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer color) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] color) {
    }

    @Overwrite(remap = false)
    public static void glNormalP3ui(@NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glNormalP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glNormalP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP1ui(@NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP2ui(@NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP3ui(@NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP4ui(@NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP1uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP1uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP2uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP2uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP3uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP4uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordP4uiv(@NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP1ui(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP2ui(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP3ui(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP4ui(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint") int coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP1uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP1uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP2uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP2uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP3uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP3uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP4uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoordP4uiv(@NativeType("GLenum") int texture, @NativeType("GLenum") int type, @NativeType("GLuint const *") int[] coords) {
    }
}

