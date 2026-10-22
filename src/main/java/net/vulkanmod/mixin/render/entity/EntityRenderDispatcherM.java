package net.vulkanmod.mixin.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.vulkanmod.compat.render.GuiEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherM {
    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void vulkanMod$deferGuiEntityRenderStateBoundary(E entity, double x, double y, double z,
                                                                                float rotationYaw, float partialTicks,
                                                                                PoseStack poseStack, MultiBufferSource buffer,
                                                                                int packedLight, CallbackInfo ci) {
        if (entity instanceof LivingEntity && GuiEntityRenderState.isGuiEntityPreview(packedLight)) {
            GuiEntityRenderState.prepareDeferredDraw();
        }
    }

}
