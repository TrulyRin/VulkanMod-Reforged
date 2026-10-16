package net.vulkanmod.mixin.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.vulkanmod.render.MinecraftRenderScale;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererRenderScaleM {
    @Shadow @Final private Minecraft minecraft;

    @WrapMethod(method = "renderLevel")
    private void vulkanmod$renderScaledWorld(DeltaTracker deltaTracker, Operation<Void> original) {
        MinecraftRenderScale.renderLevel(this.minecraft, () -> original.call(deltaTracker));
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void vulkanmod$releaseWorldScale(ClientLevel level, CallbackInfo ci) {
        MinecraftRenderScale.close();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void vulkanmod$closeWorldScale(CallbackInfo ci) {
        MinecraftRenderScale.close();
    }
}
