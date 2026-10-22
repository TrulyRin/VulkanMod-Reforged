package net.vulkanmod.mixin.render;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.compat.render.GuiEntityRenderState;
import net.vulkanmod.compat.render.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiBufferSource.BufferSource.class)
public class BufferSourceM {
    @Unique
    private boolean vulkanMod$endingFullBatch;

    @Inject(method = "endBatch()V", at = @At("HEAD"))
    private void vulkanMod$beginGuiEntityFullBatch(CallbackInfo ci) {
        this.vulkanMod$endingFullBatch = vulkanMod$hasDeferredGuiDrawState();
        if (this.vulkanMod$endingFullBatch) {
            vulkanMod$prepareDeferredGuiDrawState();
        }
    }

    @Inject(method = "endBatch()V", at = @At("RETURN"))
    private void vulkanMod$endGuiEntityFullBatch(CallbackInfo ci) {
        if (this.vulkanMod$endingFullBatch) {
            this.vulkanMod$endingFullBatch = false;
            vulkanMod$restoreDeferredGuiDrawState();
        }
    }

    @Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("HEAD"))
    private void vulkanMod$beginGuiEntityRenderTypeBatch(RenderType renderType, CallbackInfo ci) {
        if (!this.vulkanMod$endingFullBatch && vulkanMod$hasDeferredGuiDrawState()) {
            vulkanMod$prepareDeferredGuiDrawState();
        }
    }

    @Inject(method = "endBatch(Lnet/minecraft/client/renderer/RenderType;)V", at = @At("RETURN"))
    private void vulkanMod$endGuiEntityRenderTypeBatch(RenderType renderType, CallbackInfo ci) {
        if (!this.vulkanMod$endingFullBatch) {
            vulkanMod$restoreDeferredGuiDrawState();
        }
    }

    @Unique
    private static boolean vulkanMod$hasDeferredGuiDrawState() {
        return GuiEntityRenderState.hasDeferredDrawState() || GuiItemRenderState.hasDeferredDrawState();
    }

    @Unique
    private static void vulkanMod$prepareDeferredGuiDrawState() {
        if (GuiEntityRenderState.hasDeferredDrawState()) {
            GuiEntityRenderState.prepareDeferredDraw();
        }

        if (GuiItemRenderState.hasDeferredDrawState()) {
            GuiItemRenderState.prepareDeferredDraw();
        }
    }

    @Unique
    private static void vulkanMod$restoreDeferredGuiDrawState() {
        GuiItemRenderState.restoreDeferredDrawState();
        GuiEntityRenderState.restoreDeferredDrawState();
    }
}
