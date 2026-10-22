package net.vulkanmod.mixin.render.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AgeableListModel;
import net.vulkanmod.compat.render.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AgeableListModel.class)
public class AgeableListModelM {
    @Inject(method = "renderToBuffer", at = @At("HEAD"))
    private void vulkanMod$deferDirectGuiPlayerModelStateBoundary(PoseStack poseStack, VertexConsumer buffer,
                                                                  int packedLight, int packedOverlay, int color,
                                                                  CallbackInfo ci) {
        if (!GuiEntityRenderState.isGuiEntityPreview(packedLight)) {
            return;
        }

        GuiEntityRenderState.prepareDeferredDraw();
    }
}
