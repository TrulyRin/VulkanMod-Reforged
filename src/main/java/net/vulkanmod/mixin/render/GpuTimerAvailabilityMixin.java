package net.vulkanmod.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.engine.VkGpuTimestampQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

                                                                                       
@Mixin(Minecraft.class)
public abstract class GpuTimerAvailabilityMixin {
    @ModifyExpressionValue(method = "runTick", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/TimerQuery;isRecording()Z"))
    private boolean vulkanmod$skipUnavailableGpuProfiling(boolean alreadyRecording) {
        return alreadyRecording || !VkGpuTimestampQuery.isSupported();
    }
}
