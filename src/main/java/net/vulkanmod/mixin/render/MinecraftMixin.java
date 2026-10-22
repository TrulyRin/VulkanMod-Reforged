package net.vulkanmod.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.TimerQuery;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.renderer.GameRenderer;
import net.vulkanmod.compat.ExternalClientFaultBoundary;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.opengl.GlCapabilitiesFallback;
import net.vulkanmod.config.GraphicsModeCompatibility;
import net.vulkanmod.diagnostics.benchmark.MinecraftBenchmarkController;
import net.vulkanmod.render.texture.SpriteUtil;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow public boolean noRender;
    @Shadow @Final public Options options;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V", shift = At.Shift.AFTER))
    private void installGlCapabilitiesFallback(GameConfig gameConfig, CallbackInfo ci) {
        GlCapabilitiesFallback.install();
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void forceGraphicsMode(GameConfig gameConfig, CallbackInfo ci) {
        clampUnsupportedGraphicsMode();
    }

    @Unique
    private void clampUnsupportedGraphicsMode() {
        var graphicsModeOption = this.options.graphicsMode();
        GraphicsStatus requested = graphicsModeOption.get();
        GraphicsStatus supported = GraphicsModeCompatibility.coerce(requested);

        if(requested != supported) {
            Initializer.LOGGER.error("{} graphics mode not supported, forcing {}", requested, supported);
            graphicsModeOption.set(supported);
        }
    }

    @Inject(method = "runTick", at = @At(value = "HEAD"))
    private void resetBuffers(boolean bl, CallbackInfo ci) {
        MinecraftBenchmarkController.onClientTick((Minecraft) (Object) this);
        clampUnsupportedGraphicsMode();
        Renderer.getInstance().preInitFrame();
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))
    private void beginRender(int i, boolean bl) {
        RenderSystem.clear(i, bl);
        MinecraftBenchmarkController.onFrameStart();
        Renderer.getInstance().beginFrame();
    }

    @WrapOperation(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"), require = 1)
    private void vulkanMod$renderAcquiredFrame(GameRenderer gameRenderer, DeltaTracker deltaTracker, boolean renderLevel,
                                              Operation<Void> original) {
        if (Renderer.isRecording() && !Renderer.skipRendering) {
            original.call(gameRenderer, deltaTracker, renderLevel);
        }
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay()V", shift = At.Shift.BEFORE))
    private void submitRender(boolean tick, CallbackInfo ci) {
        if (Renderer.getInstance().endFrame()) {
            MinecraftBenchmarkController.onFrameEnd((Minecraft) (Object) this);
        }
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V"))
    private void redirectMainTarget1(RenderTarget instance, boolean bl) {
        Renderer.getInstance().getMainPass().mainTargetBindWrite();
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;unbindWrite()V"))
    private void redirectMainTarget2(RenderTarget instance) {
        Renderer.getInstance().getMainPass().mainTargetUnbindWrite();
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V"))
    private void removeBlit(RenderTarget instance, int i, int j) {
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V"))
    private void removeThreadYield() {
    }

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void limitWhenMinimized(CallbackInfoReturnable<Integer> cir) {
        if(this.noRender) cir.setReturnValue(10);
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;getInstance()Ljava/util/Optional;"))
    private Optional<TimerQuery> removeTimer() {
        return Optional.empty();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"),
    locals = LocalCapture.CAPTURE_FAILHARD)
    private void redirectResourceTick(boolean bl, CallbackInfo ci, Runnable runnable, int i, int j) {
        int n = Math.min(10, i) - 1;
        SpriteUtil.setDoUpload(j == n);
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;tick()V"))
    private void guardClientTick(Minecraft instance) {
        ExternalClientFaultBoundary.runClientTick(instance);
    }

    @Inject(method = "close", at = @At(value = "HEAD"))
    public void close(CallbackInfo ci) {
        Vulkan.waitIdle();
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/VirtualScreen;close()V"))
    public void close2(CallbackInfo ci) {
        Vulkan.cleanUp();
        Util.shutdownExecutors();
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;emergencySave()V"))
    private void skipEmergencySave(Minecraft instance) {

    }

    @Inject(method = "resizeDisplay", at = @At("HEAD"))
    public void onResolutionChanged(CallbackInfo ci) {
        Renderer renderer = Renderer.getInstance();
        if (renderer == null || !renderer.consumeInternalResizeDisplayNotification()) {
            Renderer.scheduleSwapChainUpdate();
        }
    }

    @Redirect(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;noRender:Z", opcode = Opcodes.PUTFIELD))
    private void keepVar(Minecraft instance, boolean value) { }

    @Redirect(method = "setOverlay", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false), require = 0)
    private java.util.function.Supplier redirectLoadingOverlay(java.util.function.Supplier mc, java.util.function.Supplier rm, java.util.function.Consumer listener, boolean bl) {
        if (net.vulkanmod.compat.EarlyWindowCompat.isHandoffComplete()) {
            net.vulkanmod.Initializer.LOGGER.info("VulkanMod: Handoff is active, forcing vanilla LoadingOverlay instead of FML overlay.");
            return mc;
        }
        return net.neoforged.fml.loading.ImmediateWindowHandler.loadingOverlay(mc, rm, listener, bl);
    }

}
