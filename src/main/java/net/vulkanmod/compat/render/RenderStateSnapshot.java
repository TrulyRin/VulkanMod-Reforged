package net.vulkanmod.compat.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.PipelineState;

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
    }

    public void restore() {
        RenderSystem.setShaderColor(red, green, blue, alpha);
        PipelineState.blendInfo = new PipelineState.BlendInfo(blendEnabled, srcRgbFactor, dstRgbFactor,
                srcAlphaFactor, dstAlphaFactor, blendOp);
        PipelineState.blendInfo.blendOpAlpha = blendOpAlpha;
        VRenderSystem.depthTest = depthTest;
        VRenderSystem.depthMask = depthMask;
        VRenderSystem.depthFun = depthFunc;
        VRenderSystem.colorMask = colorMask;
        VRenderSystem.cull = cull;
        VRenderSystem.cullFace = cullFace;
        VRenderSystem.frontFace = frontFace;
        VRenderSystem.polygonMode = polygonMode;
        VRenderSystem.topology = topology;
    }
}

