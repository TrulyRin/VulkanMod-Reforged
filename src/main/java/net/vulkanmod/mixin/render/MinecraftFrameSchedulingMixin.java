package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.optimization.FrameYieldPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public abstract class MinecraftFrameSchedulingMixin {
    @Redirect(method = "runTick", at = @At(value = "INVOKE",
            target = "Ljava/lang/Thread;yield()V"))
    private void vulkanmod$paceFrameThroughVulkan() {
        FrameYieldPolicy.atFrameEnd(RenderSystem.getDevice(), Thread::yield);
    }
}
