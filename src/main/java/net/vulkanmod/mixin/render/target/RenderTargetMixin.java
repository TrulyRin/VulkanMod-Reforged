package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.MainTarget;
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

    @Shadow public int viewWidth;
    @Shadow public int viewHeight;
    @Shadow public int width;
    @Shadow public int height;

    @Shadow protected int depthBufferId;
    @Shadow protected int colorTextureId;
    @Shadow public int frameBufferId;

    @Shadow @Final private float[] clearChannels;
    @Shadow @Final public boolean useDepth;

    boolean needClear = false;
    boolean bound = false;

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    public void clear(boolean getError, CallbackInfo ci) {
        ci.cancel();
        RenderSystem.assertOnRenderThreadOrInit();

        if(!Renderer.isRecording())
            return;

        GlFramebuffer glFramebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId);
        if(!bound || GlFramebuffer.getBoundFramebuffer() != glFramebuffer) {
            needClear = true;
            return;
        }

        GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
        int i = 16384;
        if (this.useDepth) {
            GlStateManager._clearDepth(1.0);
            i |= 256;
        }

        GlStateManager._clear(i, getError);
        needClear = false;
    }

    @Inject(method = "bindRead", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V", shift = At.Shift.AFTER), cancellable = true)
    public void bindRead(CallbackInfo ci) {
        RenderSystem.assertOnRenderThread();

        if ((Object) this instanceof MainTarget) {
            Renderer.getInstance().getMainPass().bindAsTexture();
            ci.cancel();
            return;
        }

        applyClear();

        GlTexture.bindTexture(this.colorTextureId);

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

        if ((Object) this instanceof MainTarget) {
            Renderer.getInstance().getMainPass().rebindMainTarget();
            ci.cancel();
            return;
        }

        GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        if (bl) {
            GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
        }

        this.bound = true;
        if (needClear)
            this.clear(false);
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

    @Shadow public abstract void clear(boolean getError);

    @Shadow protected abstract void _bindWrite(boolean bl);

    @Inject(method = "_blitToScreen", at = @At("HEAD"), cancellable = true)
    private void _blitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {

        if (!this.needClear) {
            Framebuffer framebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId).getFramebuffer();
            VTextureSelector.bindTexture(0, framebuffer.getColorAttachment());

            DrawUtil.blitToScreen();
        }

        ci.cancel();
    }

    @Inject(method = "getColorTextureId", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;colorTextureId:I", opcode = org.objectweb.asm.Opcodes.GETFIELD), cancellable = true)
    private void injClear(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof MainTarget) {
            cir.setReturnValue(Renderer.getInstance().getMainPass().getColorAttachmentGlId());
            return;
        }

        prepareColorTextureForSampling();
    }

    @Inject(method = "getDepthTextureId", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;depthBufferId:I", opcode = org.objectweb.asm.Opcodes.GETFIELD), cancellable = true)
    private void getDepthTextureId(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof MainTarget) {
            cir.setReturnValue(Renderer.getInstance().getMainPass().getDepthAttachmentGlId());
        }
    }

    @Override
    public boolean isBound() {
        return bound;
    }

    @Override
    public RenderPass getRenderPass() {
        return GlFramebuffer.getFramebuffer(this.frameBufferId).getRenderPass();
    }

    @Unique
    private void applyClear() {
        if (this.needClear) {
            GlFramebuffer currentFramebuffer = GlFramebuffer.getBoundFramebuffer();

            this._bindWrite(false);

            if (currentFramebuffer != null) {
                GlFramebuffer.beginRendering(currentFramebuffer);
            }
        }
    }

    @Unique
    private void prepareColorTextureForSampling() {
        applyClear();

        GlFramebuffer glFramebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId);
        if (this.bound && glFramebuffer != null && GlFramebuffer.getBoundFramebuffer() == glFramebuffer) {
            return;
        }

        GlTexture.transitionReadOnly(this.colorTextureId);
    }
}
