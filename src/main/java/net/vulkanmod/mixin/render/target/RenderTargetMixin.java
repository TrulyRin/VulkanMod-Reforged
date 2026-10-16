package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ExtendedRenderTarget;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderTarget.class, priority = 900)
public abstract class RenderTargetMixin implements ExtendedRenderTarget {

    @Shadow(remap = false) public int f_83917_;
    @Shadow(remap = false) public int f_83918_;
    @Shadow(remap = false) public int f_83915_;
    @Shadow(remap = false) public int f_83916_;

    @Shadow(remap = false) protected int f_83924_;
    @Shadow(remap = false) protected int f_83923_;
    @Shadow(remap = false) public int f_83920_;

    @Shadow(remap = false) @Final private float[] f_83921_;
    @Shadow(remap = false) @Final public boolean f_83919_;

    boolean needClear = false;
    boolean bound = false;

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    public void clear(boolean getError, CallbackInfo ci) {
        ci.cancel();
        RenderSystem.assertOnRenderThreadOrInit();

        if(!Renderer.isRecording())
            return;

        GlFramebuffer glFramebuffer = GlFramebuffer.getFramebuffer(this.f_83920_);
        if(!bound || GlFramebuffer.getBoundFramebuffer() != glFramebuffer) {
            needClear = true;
            return;
        }

        GlStateManager._clearColor(this.f_83921_[0], this.f_83921_[1], this.f_83921_[2], this.f_83921_[3]);
        int i = 16384;
        if (this.f_83919_) {
            GlStateManager._clearDepth(1.0);
            i |= 256;
        }

        GlStateManager._clear(i, getError);
        needClear = false;
    }

    @Inject(method = "bindRead", at = @At("HEAD"), cancellable = true)
    public void bindRead(CallbackInfo ci) {
        RenderSystem.assertOnRenderThread();

        applyClear();

        GlTexture.bindTexture(this.f_83923_);

        GlTexture.transitionReadOnly();
        ci.cancel();
    }

    @Inject(method = "unbindRead", at = @At("HEAD"), cancellable = true)
    public void unbindRead(CallbackInfo ci) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.bindTexture(0);
        ci.cancel();
    }

    @Inject(method = "_bindWrite", at = @At("HEAD"), cancellable = true)
    private void _bindWrite(boolean bl, CallbackInfo ci) {
        RenderSystem.assertOnRenderThreadOrInit();

        GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, this.f_83920_);
        if (bl) {
            GlStateManager._viewport(0, 0, this.f_83917_, this.f_83918_);
        }

        this.bound = true;
        if (needClear)
            this.m_83954_(false);
        ci.cancel();
    }

    @Inject(method = "unbindWrite", at = @At("HEAD"), cancellable = true)
    public void unbindWrite(CallbackInfo ci) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                GlStateManager._glBindFramebuffer(36160, 0);
                this.bound = false;
            });
        } else {
            GlStateManager._glBindFramebuffer(36160, 0);
            this.bound = false;
        }
        ci.cancel();
    }

    @Shadow(remap = false) public abstract void m_83954_(boolean getError);

    @Shadow(remap = false) protected abstract void m_83961_(boolean bl);

    @Inject(method = "_blitToScreen", at = @At("HEAD"), cancellable = true)
    private void _blitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {

        if (!this.needClear) {
            Framebuffer framebuffer = GlFramebuffer.getFramebuffer(this.f_83920_).getFramebuffer();
            VTextureSelector.bindTexture(0, framebuffer.getColorAttachment());

            DrawUtil.blitToScreen();
        }

        ci.cancel();
    }

    @Inject(method = "getColorTextureId", at = @At("HEAD"))
    private void injClear(CallbackInfoReturnable<Integer> cir) {
        prepareColorTextureForSampling();
    }

    @Override
    public boolean isBound() {
        return bound;
    }

    @Override
    public RenderPass getRenderPass() {
        return GlFramebuffer.getFramebuffer(this.f_83920_).getRenderPass();
    }

    @Unique
    private void applyClear() {
        if (this.needClear) {
            GlFramebuffer currentFramebuffer = GlFramebuffer.getBoundFramebuffer();

            this.m_83961_(false);

            if (currentFramebuffer != null) {
                GlFramebuffer.beginRendering(currentFramebuffer);
            }
        }
    }

    @Unique
    private void prepareColorTextureForSampling() {
        applyClear();

        GlFramebuffer glFramebuffer = GlFramebuffer.getFramebuffer(this.f_83920_);
        if (this.bound && glFramebuffer != null && GlFramebuffer.getBoundFramebuffer() == glFramebuffer) {
            return;
        }

        GlTexture.transitionReadOnly(this.f_83923_);
    }
}

