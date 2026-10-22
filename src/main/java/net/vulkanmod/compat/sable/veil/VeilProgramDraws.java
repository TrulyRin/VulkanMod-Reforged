package net.vulkanmod.compat.sable.veil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import org.joml.Matrix4f;


public final class VeilProgramDraws {
    private static final ProgramDrawOrder<ShaderInstance, VertexFormat, GraphicsPipeline, Matrix4f> ORDER =
            new ProgramDrawOrder<>(new ProgramDrawOrder.Backend<>() {
                @Override public int owner(ShaderInstance shader) { return VeilShaderProgramAccess.programId(shader); }
                @Override public long generation(int owner) {
                    var linked = GlProgram.linkedProgram(owner);
                    return linked == null ? 0 : linked.generation();
                }
                @Override public void apply(ShaderInstance shader) { shader.apply(); }
                @Override public void uniforms(ShaderInstance shader, Matrix4f modelView, Matrix4f projection) {
                    if (GlProgram.currentProgramId() != VeilShaderProgramAccess.programId(shader)) {
                        throw new IllegalStateException("Veil draw owner was not bound by its wrapper");
                    }


                    if (modelView != null) {
                        var uniform = shader.getUniform("ModelViewMat");
                        if (uniform == null) throw new IllegalStateException("Veil draw has no ModelViewMat uniform");
                        uniform.set(modelView);
                    }
                    if (projection != null) {
                        var uniform = shader.getUniform("ProjMat");
                        if (uniform == null) throw new IllegalStateException("Veil draw has no ProjMat uniform");
                        uniform.set(projection);
                    }
                }
                @Override public GraphicsPipeline pipeline(ShaderInstance shader, VertexFormat format) {
                    return ((ShaderMixed) shader).getPipeline(format);
                }
                @Override public boolean resources(ShaderInstance shader, GraphicsPipeline pipeline) {
                    return VeilProgramPipelines.prepareForDraw(shader, pipeline);
                }
            });

    private VeilProgramDraws() {}

    public static GraphicsPipeline prepare(ShaderInstance shader, VertexFormat format, boolean apply,
                                           Matrix4f modelView, Matrix4f projection) {
        RenderSystem.assertOnRenderThread();
        if (!VeilShaderProgramAccess.isWrapper(shader)) return null;
        return ORDER.prepare(shader, format, apply, modelView, projection);
    }


    public static GraphicsPipeline prepare(ShaderInstance shader, VertexFormat format, boolean apply) {
        return prepare(shader, format, apply, null, null);
    }
}
