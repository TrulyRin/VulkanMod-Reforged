package net.vulkanmod.compat.sable.aeronautics;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.gl.GlPatchState;
import net.vulkanmod.compat.gl.TextureUnitLimits;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlSampler;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.lwjgl.opengl.GL13;


public final class AeronauticsMaterialState implements AutoCloseable {
    private final Thread owner = Thread.currentThread();
    private boolean restored;
    private final ShaderInstance shader = RenderSystem.getShader();
    private final int program = GlProgram.currentProgramId();
    private final int patchVertices = GlPatchState.getPatchVertices();
    private final int textureActive = GlTexture.getActiveTexture();
    private final int cachedTextureActive = GlStateManager._getActiveTexture();
    private final int shaderTexture0 = RenderSystem.getShaderTexture(0);
    private final int shaderTexture2 = RenderSystem.getShaderTexture(2);
    private final Renderer renderer = Renderer.getInstance();
    private final Pipeline pipeline = renderer.getBoundPipeline();
    private final int topology = VRenderSystem.topology;
    private final boolean depthTest = VRenderSystem.depthTest;
    private final int depthFunction = VRenderSystem.depthFun;
    private final boolean depthMask = VRenderSystem.depthMask;
    private final boolean cull = VRenderSystem.cull;
    private final int colorMask = VRenderSystem.colorMask;
    private final PipelineState.BlendInfo blend = PipelineState.blendInfo;
    private final boolean blending = blend.enabled;
    private final int srcRgb = blend.srcRgbFactor, dstRgb = blend.dstRgbFactor;
    private final int srcAlpha = blend.srcAlphaFactor, dstAlpha = blend.dstAlphaFactor;
    private final int blendOp = blend.blendOp, blendOpRgb = blend.blendOpRgb, blendOpAlpha = blend.blendOpAlpha;
    private final GlTexture.BindingSnapshot textures;
    private final VTextureSelector.BindingSnapshot selectors;
    private final GlSampler.BindingSnapshot samplers;

    private AeronauticsMaterialState(int samplerCount) {
        int activeUnit = textureActive - GL13.GL_TEXTURE0;
        int cachedUnit = cachedTextureActive - GL13.GL_TEXTURE0;
        if (!TextureUnitLimits.isValid(activeUnit) || !TextureUnitLimits.isValid(cachedUnit)) {
            throw new IllegalStateException("Invalid active texture unit before Aeronautics material pass");
        }
        long units = ((1L << samplerCount) - 1) | 1L | (1L << 2)
                | (1L << activeUnit) | (1L << cachedUnit);
        textures = GlTexture.captureBindings(units);
        selectors = VTextureSelector.captureBindings(units);
        samplers = GlSampler.captureBindings();
    }


    public static AeronauticsMaterialState capture(int samplerCount) {
        if (samplerCount < 0 || samplerCount > TextureUnitLimits.COMBINED) {
            throw new IllegalArgumentException("Invalid Aeronautics sampler count");
        }
        RenderSystem.assertOnRenderThread();
        return new AeronauticsMaterialState(samplerCount);
    }


    public Throwable restore(Throwable failure) {
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("Aeronautics material state belongs to a different thread");
        }
        if (restored) return failure;
        restored = true;
        try { RenderSystem.setShader(() -> shader); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { GlProgram.useProgram(program); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { GlPatchState.patchParameteri(0x8E72, patchVertices); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { RenderSystem.setShaderTexture(0, shaderTexture0); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { RenderSystem.setShaderTexture(2, shaderTexture2); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { textures.restore(); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { samplers.restore(); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { GlStateManager._activeTexture(cachedTextureActive); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        try { GlTexture.activeTexture(textureActive); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }

        try { selectors.restore(); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        VRenderSystem.topology = topology;
        VRenderSystem.depthTest = depthTest;
        VRenderSystem.depthFun = depthFunction;
        VRenderSystem.depthMask = depthMask;
        VRenderSystem.cull = cull;
        VRenderSystem.colorMask = colorMask;
        PipelineState.blendInfo = blend;
        blend.enabled = blending;
        blend.srcRgbFactor = srcRgb;
        blend.dstRgbFactor = dstRgb;
        blend.srcAlphaFactor = srcAlpha;
        blend.dstAlphaFactor = dstAlpha;
        blend.blendOp = blendOp;
        blend.blendOpRgb = blendOpRgb;
        blend.blendOpAlpha = blendOpAlpha;
        VRenderSystem.syncGlStateCache();
        try { renderer.restoreGraphicsPipeline(pipeline); }
        catch (RuntimeException | Error error) { failure = append(failure, error); }
        return failure;
    }

    @Override
    public void close() {
        Throwable failure = restore(null);
        if (failure instanceof Error error) throw error;
        if (failure != null) throw (RuntimeException) failure;
    }

    private static Throwable append(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }
}
