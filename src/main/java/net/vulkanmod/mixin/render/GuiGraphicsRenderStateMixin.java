package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.vulkanmod.compat.render.GuiItemRenderState;
import net.vulkanmod.compat.render.RenderStateSnapshot;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(GuiGraphics.class)
public class GuiGraphicsRenderStateMixin {
    @Unique
    private final Deque<RenderStateSnapshot> vulkanMod$tooltipStateSnapshots = new ArrayDeque<>();
    @Unique
    private final Deque<RenderStateSnapshot> vulkanMod$itemStateSnapshots = new ArrayDeque<>();

    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void vulkanMod$beginTooltipRenderStateBoundary(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY,
                                                           ClientTooltipPositioner tooltipPositioner, CallbackInfo ci) {
        this.vulkanMod$tooltipStateSnapshots.push(new RenderStateSnapshot());
        GuiItemRenderState.beginTooltipOverlay();
        vulkanMod$prepareFlatGuiOverlayState();
    }

    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void vulkanMod$endTooltipRenderStateBoundary(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY,
                                                         ClientTooltipPositioner tooltipPositioner, CallbackInfo ci) {
        RenderStateSnapshot snapshot = this.vulkanMod$tooltipStateSnapshots.poll();

        if (snapshot != null) {
            ((GuiGraphics) (Object) this).flush();
            GuiItemRenderState.endTooltipOverlay();
            snapshot.restore();
        }
    }

    @Inject(method = "renderTooltipInternal",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    shift = At.Shift.BEFORE))
    private void vulkanMod$flushTooltipBeforePosePop(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY,
                                                     ClientTooltipPositioner tooltipPositioner, CallbackInfo ci) {
        ((GuiGraphics) (Object) this).flush();
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("HEAD"))
    private void vulkanMod$beginItemRenderStateBoundary(@Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y,
                                                        int seed, int guiOffset, CallbackInfo ci) {
        this.vulkanMod$itemStateSnapshots.push(new RenderStateSnapshot());
        vulkanMod$prepareGuiItemState();
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("RETURN"))
    private void vulkanMod$endItemRenderStateBoundary(@Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y,
                                                      int seed, int guiOffset, CallbackInfo ci) {
        RenderStateSnapshot snapshot = this.vulkanMod$itemStateSnapshots.poll();

        if (snapshot != null) {
            snapshot.restore();
        }
    }

    @Unique
    private static void vulkanMod$prepareFlatGuiOverlayState() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    @Unique
    private static void vulkanMod$prepareGuiItemState() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }
}
