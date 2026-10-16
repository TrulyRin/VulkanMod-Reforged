package net.vulkanmod.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.compat.path.RenderPathOwnership;
import net.vulkanmod.compat.observer.GuiRenderTrace;
import net.vulkanmod.compat.render.RenderStateSnapshot;
import net.vulkanmod.compat.scguns.ScgunsCooldownHud;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiRenderStateBoundaryMixin {
    @Unique
    private RenderStateSnapshot vulkanMod$guiStateSnapshot;

    @Inject(method = "render", at = @At("HEAD"))
    private void vulkanMod$beginGuiRenderStateBoundary(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Renderer.getInstance().getMainPass().resolveRenderScaleForGui();

        boolean active = RenderPathOwnership.isPathActive(RenderPath.GUI);
        GuiRenderTrace.beginGuiRender();
        if (GuiRenderTrace.isActive()) {
            GuiRenderTrace.logBoundary("begin", "active=" + active + " snapshotAlpha=" + VRenderSystem.getShaderColor().getFloat(12));
        }

        if (!active) {
            return;
        }

        this.vulkanMod$guiStateSnapshot = new RenderStateSnapshot();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void vulkanMod$endGuiRenderStateBoundary(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        try {
            vulkanMod$renderScgunsCooldownHud();
        } finally {
            boolean active = this.vulkanMod$guiStateSnapshot != null;
            if (GuiRenderTrace.isActive()) {
                GuiRenderTrace.logBoundary("end", "active=" + active + " snapshotAlpha=" + VRenderSystem.getShaderColor().getFloat(12));
            }

            RenderStateSnapshot snapshot = this.vulkanMod$guiStateSnapshot;
            this.vulkanMod$guiStateSnapshot = null;
            try {
                if (snapshot != null) {
                    snapshot.restore();
                }
            } finally {
                GuiRenderTrace.endGuiRender();
            }
        }
    }

    @Unique
    private static void vulkanMod$renderScgunsCooldownHud() {
        if (!ScgunsCooldownHud.isRegistered() || !RenderSystem.isOnRenderThread()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Renderer renderer = Renderer.getInstance();
        boolean safeFrame = renderer != null && Renderer.isRecording()
                && Renderer.getCommandBuffer() != null && renderer.getBoundRenderPass() != null;
        if (!safeFrame || minecraft.options.hideGui) {
            return;
        }

                                                                                     
        RenderStateSnapshot snapshot = new RenderStateSnapshot();
        try {
            ScgunsCooldownHud.render(safeFrame, minecraft.options.hideGui);
        } finally {
            snapshot.restore();
        }
    }
}
