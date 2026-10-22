package net.vulkanmod.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.vulkanmod.compat.render.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererM<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(method = "render", at = @At("HEAD"))
    private void vulkanMod$deferGuiLivingEntityRenderStateBoundary(T entity, float entityYaw, float partialTicks,
                                                                   PoseStack poseStack, MultiBufferSource buffer,
                                                                   int packedLight, CallbackInfo ci) {
        if (!GuiEntityRenderState.isGuiEntityPreview(packedLight)) {
            return;
        }

        GuiEntityRenderState.prepareDeferredDraw();
    }
}
