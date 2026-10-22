package net.vulkanmod.mixin.compatibility.tensura;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.github.manasmods.tensura.client.screen.widgets.SimpleButton", remap = false)
public abstract class TensuraSimpleButtonMixin extends Button {
    protected TensuraSimpleButtonMixin(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration createNarration) {
        super(x, y, width, height, message, onPress, createNarration);
    }

    @Inject(
            method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = {
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V",
                            shift = At.Shift.AFTER
                    ),
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V",
                            shift = At.Shift.AFTER
                    )
            },
            require = 0
    )
    private void vulkanMod$redrawLabelAfterButtonTexture(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick,
                                                         CallbackInfo ci) {
        Component message = this.getMessage();
        if (message == null || message.getString().isEmpty()) {
            return;
        }

        int textColor = this.isActive() ? 0xFFFFFF : 0xA0A0A0;
        int alpha = Mth.ceil(this.alpha * 255.0f) << 24;
        this.renderString(guiGraphics, Minecraft.getInstance().font, textColor | alpha);
    }
}
