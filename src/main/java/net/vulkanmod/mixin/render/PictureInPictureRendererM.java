package net.vulkanmod.mixin.render;

import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.vulkanmod.render.gui.OffscreenGuiBlitCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureRendererM {
    @ModifyArg(method = "blitTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitBlitToCurrentLayer(Lnet/minecraft/client/gui/render/state/BlitRenderState;)V"), index = 0)
    private BlitRenderState vulkanmod$adaptPictureInPictureBlit(BlitRenderState state) {
        return OffscreenGuiBlitCoordinates.toTopLeft(state);
    }
}
