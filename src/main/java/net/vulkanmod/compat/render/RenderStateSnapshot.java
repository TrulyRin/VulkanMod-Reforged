package net.vulkanmod.compat.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.mixin.render.RenderSystemAccessor;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlSampler;

public final class RenderStateSnapshot {
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;
    private final boolean blendEnabled;
    private final int srcRgbFactor;
    private final int dstRgbFactor;
    private final int srcAlphaFactor;
    private final int dstAlphaFactor;
    private final int blendOp;
    private final int blendOpAlpha;
    private final boolean depthTest;
    private final boolean depthMask;
    private final int depthFunc;
    private final int colorMask;
    private final boolean cull;
    private final int cullFace;
    private final int frontFace;
    private final int polygonMode;
    private final int topology;
    private final boolean scissorEnabled;
    private final int scissorX;
    private final int scissorY;
    private final int scissorWidth;
    private final int scissorHeight;
    private final int viewportX;
    private final int viewportY;
    private final int viewportWidth;
    private final int viewportHeight;
    private final long viewportRevision;
    private final boolean glViewportSelection;
    private final int[] shaderTextures;
    private final int[] samplerBindings;
    private final GlFramebuffer.BindingSnapshot framebuffers;

    public RenderStateSnapshot() {
        this.red = VRenderSystem.getShaderColor().getFloat(0);
        this.green = VRenderSystem.getShaderColor().getFloat(4);
        this.blue = VRenderSystem.getShaderColor().getFloat(8);
        this.alpha = VRenderSystem.getShaderColor().getFloat(12);
        this.blendEnabled = PipelineState.blendInfo.enabled;
        this.srcRgbFactor = PipelineState.blendInfo.srcRgbFactor;
        this.dstRgbFactor = PipelineState.blendInfo.dstRgbFactor;
        this.srcAlphaFactor = PipelineState.blendInfo.srcAlphaFactor;
        this.dstAlphaFactor = PipelineState.blendInfo.dstAlphaFactor;
        this.blendOp = PipelineState.blendInfo.blendOpRgb;
        this.blendOpAlpha = PipelineState.blendInfo.blendOpAlpha;
        this.depthTest = VRenderSystem.depthTest;
        this.depthMask = VRenderSystem.depthMask;
        this.depthFunc = VRenderSystem.depthFun;
        this.colorMask = VRenderSystem.getColorMask();
        this.cull = VRenderSystem.cull;
        this.cullFace = VRenderSystem.cullFace;
        this.frontFace = VRenderSystem.frontFace;
        this.polygonMode = VRenderSystem.polygonMode;
        this.topology = VRenderSystem.topology;
        this.scissorEnabled = Renderer.isScissorEnabled();
        this.scissorX = Renderer.getScissorBox(0);
        this.scissorY = Renderer.getScissorBox(1);
        this.scissorWidth = Renderer.getScissorBox(2);
        this.scissorHeight = Renderer.getScissorBox(3);
        this.viewportX = Renderer.getViewportBox(0);
        this.viewportY = Renderer.getViewportBox(1);
        this.viewportWidth = Renderer.getViewportBox(2);
        this.viewportHeight = Renderer.getViewportBox(3);
        this.viewportRevision = Renderer.getViewportRevision();
        this.glViewportSelection = Renderer.isGlViewportSelection();
        this.shaderTextures = RenderSystemAccessor.getShaderTextures().clone();
        this.samplerBindings = new int[this.shaderTextures.length];
        for (int unit = 0; unit < this.samplerBindings.length; unit++) {
            this.samplerBindings[unit] = GlSampler.getBoundSampler(unit);
        }
        this.framebuffers = GlFramebuffer.captureBindings();
    }

    public void restore() {
        RenderSystem.setShaderColor(red, green, blue, alpha);
        PipelineState.blendInfo = new PipelineState.BlendInfo(blendEnabled, srcRgbFactor, dstRgbFactor,
                srcAlphaFactor, dstAlphaFactor, blendOp);
        PipelineState.blendInfo.blendOpAlpha = blendOpAlpha;
        VRenderSystem.depthTest = depthTest;
        VRenderSystem.depthMask = depthMask;
        VRenderSystem.depthFun = depthFunc;
        VRenderSystem.syncGlStateCache();
        VRenderSystem.colorMask = colorMask;
        VRenderSystem.cull = cull;
        VRenderSystem.cullFace = cullFace;
        VRenderSystem.frontFace = frontFace;
        VRenderSystem.polygonMode = polygonMode;
        VRenderSystem.topology = topology;
        for (int unit = 0; unit < shaderTextures.length; unit++) {
            RenderSystem.setShaderTexture(unit, shaderTextures[unit]);
            GlSampler.bindSampler(unit, samplerBindings[unit]);
        }
        if (framebuffers.restoreLiveTargets()) {
            if (viewportWidth > 0 && viewportHeight != 0) {
                Renderer.setViewport(viewportX, viewportY, viewportWidth, viewportHeight);
            }
            Renderer.setScissor(scissorX, scissorY, scissorWidth, scissorHeight);
            Renderer.setScissorEnabled(scissorEnabled);
            if (viewportWidth > 0 && viewportHeight != 0)
                Renderer.restoreViewportRevision(viewportRevision, glViewportSelection);
        }
    }
}
