package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.ShaderInstance;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.vulkanmod.render.RenderTargetBlit;
import net.vulkanmod.render.RenderTargetBindContext;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ExtendedRenderTarget;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
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

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true, order = 2000)
    public void clear(boolean getError, CallbackInfo ci) {
        ci.cancel();
        RenderSystem.assertOnRenderThreadOrInit();

        if(!Renderer.isRecording() || Renderer.skipRendering) {
            needClear = true;
            return;
        }

        if ((Object) this instanceof MainTarget) {
            try (Renderer.RenderTargetScope ignored = Renderer.getInstance().temporaryRenderTarget()) {
                RenderTargetBindContext.runInternal(() -> this._bindWrite(false));
                clearCurrentAttachments(getError);
            }
            return;
        }

        GlFramebuffer glFramebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId);
        if(!bound || GlFramebuffer.getBoundFramebuffer() != glFramebuffer) {
            needClear = true;
            return;
        }

        clearCurrentAttachments(getError);
    }

    @Unique
    private void clearCurrentAttachments(boolean getError) {
        GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
        int i = 16384;
        if (this.useDepth) {
            GlStateManager._clearDepth(1.0);
            i |= 256;
        }

        GlStateManager._clear(i, getError);
        needClear = false;
    }

    @Inject(method = "bindRead", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V", shift = At.Shift.AFTER), cancellable = true, order = 2000)
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

    @Inject(method = "unbindRead", at = @At("HEAD"), cancellable = true, order = 2000)
    public void unbindRead(CallbackInfo ci) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.bindTexture(0);
        ci.cancel();
    }

    @Inject(method = "_bindWrite", at = @At("HEAD"), cancellable = true, order = 2000)
    private void _bindWrite(boolean bl, CallbackInfo ci) {
        RenderSystem.assertOnRenderThreadOrInit();

        if ((Object) this instanceof MainTarget) {
            if (RenderTargetBindContext.isInternal()) Renderer.getInstance().getMainPass().rebindMainTarget();
            else GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
            ci.cancel();
            return;
        }

        if (RenderTargetBindContext.isInternal()) GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        else GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        if (bl) {
            GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
        }

        this.bound = true;
        if (needClear)
            this.clear(false);
        ci.cancel();
    }

    @Inject(method = "unbindWrite", at = @At("HEAD"), cancellable = true, order = 2000)
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

    @Inject(method = "copyDepthFrom", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThreadOrInit()V", shift = At.Shift.AFTER), order = 2000)
    private void vulkanmod$prepareDepthCopy(RenderTarget source, CallbackInfo ci) {
        ((ExtendedRenderTarget) source).vulkanmod$prepareForDepthCopy();
        this.vulkanmod$prepareForDepthCopy();
    }

    @Override
    public void vulkanmod$prepareForDepthCopy() {

        if (!((Object) this instanceof MainTarget)) applyClear();
    }

    @ModifyExpressionValue(method = "copyDepthFrom", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;frameBufferId:I", opcode = org.objectweb.asm.Opcodes.GETFIELD, ordinal = 0))
    private int vulkanmod$depthCopySourceFramebuffer(int framebuffer, RenderTarget source) {

        return source instanceof MainTarget ? 0 : framebuffer;
    }

    @ModifyExpressionValue(method = "copyDepthFrom", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;frameBufferId:I", opcode = org.objectweb.asm.Opcodes.GETFIELD, ordinal = 1))
    private int vulkanmod$depthCopyDestinationFramebuffer(int framebuffer) {
        return (Object) this instanceof MainTarget ? 0 : framebuffer;
    }

    @Inject(method = "_blitToScreen", at = @At("HEAD"), cancellable = true, order = 2000)
    private void _blitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {

        if (this.needClear) ci.cancel();
    }

    @Unique private RenderTargetBlit.Call vulkanmod$blitCall;

    @WrapMethod(method = "_blitToScreen")
    private void vulkanmod$scopedBlit(int width, int height, boolean disableBlend, Operation<Void> original) {
        RenderTargetBlit.Call previous = this.vulkanmod$blitCall;
        try (RenderTargetBlit.Call call = new RenderTargetBlit.Call()) {
            this.vulkanmod$blitCall = call;
            original.call(width, height, disableBlend);
        } finally {
            this.vulkanmod$blitCall = previous;
        }
    }

    @ModifyExpressionValue(method = "_blitToScreen", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;colorTextureId:I"))
    private int vulkanmod$liveBlitSource(int ignoredRawId) {
        return this.vulkanmod$blitCall.source((RenderTarget) (Object) this);
    }

    @WrapOperation(method = "_blitToScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V"))
    private void vulkanmod$applyBlitOwner(ShaderInstance shader, Operation<Void> original) {
        this.vulkanmod$blitCall.apply(shader, () -> original.call(shader));
    }

    @WrapOperation(method = "_blitToScreen", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferUploader;draw(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private void vulkanmod$drawBlitMesh(MeshData mesh, Operation<Void> original) {
        this.vulkanmod$blitCall.draw(mesh);
    }

    @Inject(method = "getColorTextureId", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;colorTextureId:I", opcode = org.objectweb.asm.Opcodes.GETFIELD), cancellable = true, order = 2000)
    private void injClear(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof MainTarget) {
            cir.setReturnValue(Renderer.getInstance().getMainPass().getColorAttachmentGlId());
            return;
        }

        prepareColorTextureForSampling();
    }

    @Inject(method = "getDepthTextureId", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;depthBufferId:I", opcode = org.objectweb.asm.Opcodes.GETFIELD), cancellable = true, order = 2000)
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
            boolean wasBound = this.bound;
            try (Renderer.RenderTargetScope ignored = Renderer.getInstance().temporaryRenderTarget()) {
                RenderTargetBindContext.runInternal(() -> this._bindWrite(false));
            } finally {
                this.bound = wasBound;
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
