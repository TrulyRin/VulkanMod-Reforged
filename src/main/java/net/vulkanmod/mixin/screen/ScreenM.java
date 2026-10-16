package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.vulkanmod.compat.render.RenderStateSnapshot;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenM {
    @Unique
    private RenderStateSnapshot vulkanMod$screenStateSnapshot;
    @Unique
    private RenderStateSnapshot vulkanMod$blurStateSnapshot;

    @Inject(method = "renderWithTooltip", at = @At("HEAD"))
    private void vulkanMod$beginScreenRenderStateBoundary(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.vulkanMod$screenStateSnapshot = new RenderStateSnapshot();
        Renderer.clearAttachments(GL11.GL_DEPTH_BUFFER_BIT);
    }

    @Inject(method = "renderWithTooltip", at = @At("RETURN"))
    private void vulkanMod$endScreenRenderStateBoundary(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        RenderStateSnapshot snapshot = this.vulkanMod$screenStateSnapshot;
        this.vulkanMod$screenStateSnapshot = null;

        if (snapshot != null) {
            snapshot.restore();
        }
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void vulkanMod$beginBlurBackgroundState(float f, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) {
            Renderer.resetScissor();
            ci.cancel();
            return;
        }

        this.vulkanMod$blurStateSnapshot = new RenderStateSnapshot();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        Renderer.resetScissor();
    }

    @Inject(method = "renderBlurredBackground", at = @At("RETURN"))
    private void vulkanMod$endBlurBackgroundState(float f, CallbackInfo ci) {
        Renderer.resetScissor();

        Renderer.clearAttachments(256);

        RenderStateSnapshot snapshot = this.vulkanMod$blurStateSnapshot;
        this.vulkanMod$blurStateSnapshot = null;

        if (snapshot != null) {
            snapshot.restore();
        }
    }
}
