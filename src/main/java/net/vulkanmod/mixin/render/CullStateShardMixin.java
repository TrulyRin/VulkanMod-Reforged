package net.vulkanmod.mixin.render;

import net.vulkanmod.compat.render.MaterialCullState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.renderer.RenderStateShard$CullStateShard")
public abstract class CullStateShardMixin {
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderStateShard$BooleanStateShard;<init>(Ljava/lang/String;Ljava/lang/Runnable;Ljava/lang/Runnable;Z)V"),
            index = 1, require = 1)
    private static Runnable vulkanMod$explicitMaterialCull(String name, Runnable setup, Runnable clear, boolean enabled) {

        return enabled ? () -> MaterialCullState.setupCull(setup) : setup;
    }
}
