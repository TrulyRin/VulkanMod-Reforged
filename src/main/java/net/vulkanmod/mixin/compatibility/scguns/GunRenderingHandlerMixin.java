package net.vulkanmod.mixin.compatibility.scguns;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.vulkanmod.compat.scguns.ScgunsCooldownHud;
import net.vulkanmod.compat.scguns.ScgunsCooldownRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(targets = "top.ribs.scguns.client.handler.GunRenderingHandler", remap = false)
public class GunRenderingHandlerMixin implements ScgunsCooldownRenderer {
    @Shadow(remap = false)
    private void handleRenderTick() {
        throw new AssertionError("Mixin shadow");
    }

    @Inject(method = "<init>()V", at = @At("RETURN"), remap = false, require = 1)
    private void vulkanMod$captureCooldownRenderer(CallbackInfo ci) {
        ScgunsCooldownHud.register(this);
    }

    @Inject(method = "renderCooldownIndicator(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void vulkanMod$gateCooldownIndicator(Minecraft minecraft, Player player, ItemStack stack, CallbackInfo ci) {
        if (!ScgunsCooldownHud.isHudScope()) {
            ci.cancel();
        }
    }

    @Override
    public void vulkanMod$renderCooldownHud() {

        this.handleRenderTick();
    }
}
