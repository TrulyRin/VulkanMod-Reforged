package net.vulkanmod.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.vulkanmod.compat.render.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class ItemRendererM {
    @Inject(method = "render", at = @At("HEAD"))
    private void vulkanMod$deferGuiItemModelStateBoundary(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
                                                          PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                                          int packedOverlay, BakedModel model, CallbackInfo ci) {
        if (displayContext == ItemDisplayContext.GUI) {
            GuiItemRenderState.prepareDeferredDraw();
        }
    }
}
