package net.vulkanmod.mixin.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
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

@Mixin(GuiGraphics.class)
public class GuiGraphicsRenderStateMixin {
    @WrapMethod(method = "renderTooltipInternal(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;)V", require = 1)
    private void vulkanMod$renderTooltipStateBoundary(Font font, List<ClientTooltipComponent> components, int mouseX, int mouseY,
                                                      ClientTooltipPositioner tooltipPositioner, Operation<Void> original) {
        RenderStateSnapshot snapshot = new RenderStateSnapshot();
        GuiItemRenderState.beginTooltipOverlay();
        try {
            vulkanMod$prepareFlatGuiOverlayState();
            original.call(font, components, mouseX, mouseY, tooltipPositioner);
        } finally {
            try {
                ((GuiGraphics) (Object) this).flush();
            } finally {
                try {
                    GuiItemRenderState.endTooltipOverlay();
                } finally {
                    snapshot.restore();
                }
            }
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

    @WrapMethod(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V", require = 1)
    private void vulkanMod$renderItemStateBoundary(@Nullable LivingEntity entity, @Nullable Level level, ItemStack stack, int x, int y,
                                                   int seed, int guiOffset, Operation<Void> original) {
        RenderStateSnapshot snapshot = new RenderStateSnapshot();
        try {
            vulkanMod$prepareGuiItemState();
            original.call(entity, level, stack, x, y, seed, guiOffset);
        } finally {
            snapshot.restore();
        }
    }

    @Unique
    private static void vulkanMod$prepareFlatGuiOverlayState() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
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
