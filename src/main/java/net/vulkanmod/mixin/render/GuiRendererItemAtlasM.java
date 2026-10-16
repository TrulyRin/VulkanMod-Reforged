package net.vulkanmod.mixin.render;

import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.BlitRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import com.mojang.blaze3d.textures.GpuTexture;
import net.vulkanmod.render.gui.InventoryAtlasDiagnostics;
import net.vulkanmod.render.gui.OffscreenGuiBlitCoordinates;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererItemAtlasM {
    @Shadow @Final private GuiRenderState renderState;
    @Shadow private GpuTexture itemsAtlas;

    @Inject(method = "prepareItemElements", at = @At("HEAD"))
    private void vulkanmod$beginItemAtlasCapture(CallbackInfo ci) {
        if (InventoryAtlasDiagnostics.ENABLED) {
            InventoryAtlasDiagnostics.beginPass(this.renderState.getItemModelIdentities().size());
        }
    }

    @Inject(method = "prepareItemElements", at = @At("RETURN"))
    private void vulkanmod$captureRenderedItemAtlas(CallbackInfo ci) {
        if (InventoryAtlasDiagnostics.ENABLED) InventoryAtlasDiagnostics.completePass(this.itemsAtlas);
    }

    @ModifyArg(method = "submitBlitFromItemAtlas", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/state/GuiRenderState;submitBlitToCurrentLayer(Lnet/minecraft/client/gui/render/state/BlitRenderState;)V"), index = 0)
    private BlitRenderState vulkanmod$adaptItemAtlasBlit(BlitRenderState state) {
        return OffscreenGuiBlitCoordinates.toTopLeft(state);
    }
}
