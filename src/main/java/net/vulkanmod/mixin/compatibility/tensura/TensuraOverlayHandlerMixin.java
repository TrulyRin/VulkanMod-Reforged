package net.vulkanmod.mixin.compatibility.tensura;

import net.vulkanmod.compat.render.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.github.manasmods.tensura.handler.client.OverlayHandler", remap = false)
public class TensuraOverlayHandlerMixin {
    @Inject(method = "renderPlayer(ZZ)V", at = @At("HEAD"), require = 0)
    private static void vulkanMod$beginHudPlayerPreview(boolean flipped, boolean statusBars, CallbackInfo ci) {
        GuiEntityRenderState.beginHudEntityPreview();
    }

    @Inject(method = "renderPlayer(ZZ)V", at = @At("RETURN"), require = 0)
    private static void vulkanMod$endHudPlayerPreview(boolean flipped, boolean statusBars, CallbackInfo ci) {
        GuiEntityRenderState.endHudEntityPreview();
    }
}
