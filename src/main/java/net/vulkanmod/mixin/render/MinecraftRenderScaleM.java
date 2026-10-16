package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.vulkanmod.render.MinecraftRenderScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftRenderScaleM {
    @Inject(method = "getMainRenderTarget", at = @At("RETURN"), cancellable = true)
    private void vulkanmod$worldRenderTarget(CallbackInfoReturnable<RenderTarget> cir) {
        cir.setReturnValue(MinecraftRenderScale.targetOrOriginal(cir.getReturnValue()));
    }

    @Inject(method = {"resizeDisplay", "destroy", "close"}, at = @At("HEAD"))
    private void vulkanmod$releaseScaledTarget(CallbackInfo ci) {
        MinecraftRenderScale.close();
    }
}
