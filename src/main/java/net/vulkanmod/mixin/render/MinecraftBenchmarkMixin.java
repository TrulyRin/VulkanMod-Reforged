package net.vulkanmod.mixin.render;

import net.minecraft.client.Minecraft;
import net.vulkanmod.diagnostics.benchmark.MinecraftBenchmarkController;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.optimization.FramePerformanceMetrics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

                                                                                 
@Mixin(Minecraft.class)
public abstract class MinecraftBenchmarkMixin {
    @Unique private boolean vulkanmod$benchmarkFrameStarted;

    @Inject(method = "runTick", at = @At("HEAD"))
    private void vulkanmod$beginFrameMetrics(boolean tick, CallbackInfo ci) {
        FramePerformanceMetrics.INSTANCE.beginFrame();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vulkanmod$benchmarkClientTick(CallbackInfo ci) {
        MinecraftBenchmarkController.onClientTick((Minecraft) (Object) this);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V"))
    private void vulkanmod$benchmarkFrameStart(boolean tick, CallbackInfo ci) {
        this.vulkanmod$benchmarkFrameStarted = !((Minecraft) (Object) this).getWindow().isMinimized();
        if (this.vulkanmod$benchmarkFrameStarted) {
            MinecraftBenchmarkController.onFrameStart();
        }
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/Window;updateDisplay(Lcom/mojang/blaze3d/TracyFrameCapture;)V",
            shift = At.Shift.AFTER))
    private void vulkanmod$benchmarkFrameEnd(boolean tick, CallbackInfo ci) {
                                                                                  
        FramePerformanceMetrics.INSTANCE.finishFrame(Initializer.CONFIG != null && Initializer.CONFIG.adaptiveChunkUploads);
        if (this.vulkanmod$benchmarkFrameStarted) {
            this.vulkanmod$benchmarkFrameStarted = false;
            MinecraftBenchmarkController.onFrameEnd((Minecraft) (Object) this);
        }
    }
}
