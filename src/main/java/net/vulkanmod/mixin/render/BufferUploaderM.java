package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.observer.GuiRenderTrace;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.compat.sable.render.SublevelRenderScope;
import net.vulkanmod.compat.sable.veil.VeilProgramDraws;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BufferUploader.class, priority = 900)
public class BufferUploaderM {
    @Inject(method = "reset", at = @At("HEAD"), cancellable = true)
    private static void reset(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "drawWithShader", at = @At("HEAD"), cancellable = true)
    private static void drawWithShader(MeshData meshData, CallbackInfo ci) {
        ci.cancel();

        RenderSystem.assertOnRenderThread();

        try (meshData) {
            MeshData.DrawState parameters = meshData.drawState();
            if (parameters.vertexCount() <= 0) return;
            Renderer renderer = Renderer.getInstance();
            ShaderInstance shaderInstance = RenderSystem.getShader();
            if (shaderInstance == null) return;
            String shaderName = shaderInstance.getName();

            if (!shaderInstance.getVertexFormat().equals(parameters.format())) {
                if (net.vulkanmod.compat.RuntimeOptions.diagnosticsEnabled()) {
                    net.vulkanmod.Initializer.LOGGER.warn("Vertex format mismatch for shader {}", shaderName);
                }

            }

            boolean programOwned = VeilShaderProgramAccess.isWrapper(shaderInstance);
            GraphicsPipeline pipeline;
            if (programOwned) {
                pipeline = VeilProgramDraws.prepare(shaderInstance, parameters.format(), true,
                        RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
            } else {
                shaderInstance.apply();
                pipeline = ((ShaderMixed) shaderInstance).getPipeline(parameters.format());
            }
            if (pipeline == null) return;

            traceHudDraw(shaderName, pipeline, parameters);
            drawPrepared(renderer, pipeline, meshData, programOwned);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void draw(MeshData meshData, CallbackInfo ci) {
        ci.cancel();

        RenderSystem.assertOnRenderThread();
        try (meshData) {
            MeshData.DrawState parameters = meshData.drawState();
            if (parameters.vertexCount() <= 0) return;
            Renderer renderer = Renderer.getInstance();
            Pipeline pipeline = renderer.getBoundPipeline();
            var scope = SublevelRenderScope.current().orElse(null);
            Object activeShader = scope != null && scope.shader() != null ? scope.shader() : RenderSystem.getShader();
            if (activeShader instanceof ShaderInstance shader && VeilShaderProgramAccess.isWrapper(shader)) {
                GraphicsPipeline owned = VeilProgramDraws.prepare(shader, parameters.format(), true);
                if (owned == null) return;
                try {
                    drawPrepared(renderer, owned, meshData, true);
                } finally {
                    renderer.restoreGraphicsPipeline(pipeline);
                }
                return;
            }
            if (pipeline == null) return;
            if (pipeline instanceof GraphicsPipeline graphics && graphics.hasTessellation()) {
                throw new IllegalStateException("Raw patch draw requires its Veil shader owner");
            }
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters, meshData.indexBuffer());
        }
    }

    private static void drawPrepared(Renderer renderer, GraphicsPipeline pipeline, MeshData meshData, boolean programOwned) {
        MeshData.DrawState state = meshData.drawState();
        boolean patches = pipeline.hasTessellation();
        if (patches) Renderer.getDrawer().preparePatches(state, meshData.indexBuffer());
        VRenderSystem.setPrimitiveTopology(state.mode());
        renderer.bindGraphicsPipeline(pipeline);
        if (!programOwned) VTextureSelector.bindShaderTextures(pipeline);
        renderer.uploadAndBindUBOs(pipeline);
        if (patches) {
            Renderer.getDrawer().drawPatches(meshData.vertexBuffer(), state, meshData.indexBuffer() != null);
        } else {
            Renderer.getDrawer().draw(meshData.vertexBuffer(), state, meshData.indexBuffer());
        }
    }

    private static void traceHudDraw(String shaderName, GraphicsPipeline pipeline, MeshData.DrawState parameters) {
        if (!GuiRenderTrace.isActive()) {
            return;
        }

        Matrix4f projection = RenderSystem.getProjectionMatrix();
        GuiRenderTrace.logDraw("shaderName=" + shaderName
                + " pipeline=" + pipeline.name
                + " vertexCount=" + parameters.vertexCount()
                + " mode=" + parameters.mode()
                + " alpha=" + VRenderSystem.getShaderColor().getFloat(12)
                + " blend=" + PipelineState.blendInfo.enabled
                + " srcRgb=" + PipelineState.blendInfo.srcRgbFactor
                + " dstRgb=" + PipelineState.blendInfo.dstRgbFactor
                + " srcAlpha=" + PipelineState.blendInfo.srcAlphaFactor
                + " dstAlpha=" + PipelineState.blendInfo.dstAlphaFactor
                + " depthTest=" + VRenderSystem.depthTest
                + " depthMask=" + VRenderSystem.depthMask
                + " cull=" + VRenderSystem.cull
                + " texture0=" + RenderSystem.getShaderTexture(0)
                + " boundTexture=" + GlTexture.getBoundTextureId(GL11.GL_TEXTURE_2D)
                + " projectionM33=" + projection.m33()
                + " projectionM23=" + projection.m23());
    }

}
