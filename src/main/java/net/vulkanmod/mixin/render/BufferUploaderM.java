package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.observer.GuiRenderTrace;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ShaderMixed;
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

import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ZERO;

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

        MeshData.DrawState parameters = meshData.drawState();

        Renderer renderer = Renderer.getInstance();

        if (parameters.vertexCount() > 0) {
            ShaderInstance shaderInstance = RenderSystem.getShader();
            String shaderName = shaderInstance.getName();

            if (!shaderInstance.getVertexFormat().equals(parameters.format())) {
                if (net.vulkanmod.compat.RuntimeOptions.diagnosticsEnabled()) {
                    net.vulkanmod.Initializer.LOGGER.warn("Vertex format mismatch for shader {}", shaderName);
                }

            }

            shaderInstance.apply();

            GraphicsPipeline pipeline = ((ShaderMixed)(shaderInstance)).getPipeline(parameters.format());

            if (pipeline == null) {

                meshData.close();
                return;
            }

            if (isFlatGuiDraw(shaderName, parameters)) {
                configureFlatGuiDrawState();
            }

            traceHudDraw(shaderName, pipeline, parameters);

            VRenderSystem.setPrimitiveTopology(parameters.mode());
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private static void draw(MeshData meshData, CallbackInfo ci) {
        ci.cancel();

        MeshData.DrawState parameters = meshData.drawState();

        if (parameters.vertexCount() > 0) {
            Renderer renderer = Renderer.getInstance();
            Pipeline pipeline = renderer.getBoundPipeline();
            renderer.uploadAndBindUBOs(pipeline);

            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

    private static boolean isFlatGuiDraw(String shaderName, MeshData.DrawState parameters) {
        return (isFlatGuiShader(shaderName) || isUniversalCraftGuiFormat(parameters.format()))
                && isOrthographicProjection();
    }

    private static boolean isFlatGuiShader(String shaderName) {
        return "position_tex".equals(shaderName)
                || "position_tex_color".equals(shaderName)
                || shaderName.startsWith("rendertype_gui")
                || shaderName.startsWith("rendertype_text");
    }

    private static boolean isUniversalCraftGuiFormat(VertexFormat format) {
        if (format == null) {
            return false;
        }

        var attributes = format.getElementAttributeNames();
        int size = attributes.size();
        if (size == 0 || !"uc_Position".equals(attributes.get(0))) {
            return false;
        }

        return size == 1
                || size == 2 && ("uc_UV0".equals(attributes.get(1))
                || "uc_Color".equals(attributes.get(1)));
    }

    private static boolean isOrthographicProjection() {
        return RenderSystem.getProjectionMatrix().m33() == 1.0f;
    }

    private static void configureFlatGuiDrawState() {
        VRenderSystem.disableDepthTest();
        VRenderSystem.depthMask(false);
        VRenderSystem.disableCull();
        VRenderSystem.enableBlend();
        if (!isVignetteBlendState()) {
            VRenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        }
    }

    private static boolean isVignetteBlendState() {
        PipelineState.BlendInfo blendInfo = PipelineState.blendInfo;
        return blendInfo.enabled
                && blendInfo.srcRgbFactor == VK_BLEND_FACTOR_ZERO
                && blendInfo.dstRgbFactor == VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR
                && blendInfo.srcAlphaFactor == VK_BLEND_FACTOR_ONE
                && blendInfo.dstAlphaFactor == VK_BLEND_FACTOR_ZERO;
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
