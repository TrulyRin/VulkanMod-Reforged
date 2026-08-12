package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.vulkanmod.render.culling.EntityCulling;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class EntityRendererM<T extends Entity> {

    @Redirect(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean isVisible(Frustum frustum, AABB aABB, T entity, Frustum originalFrustum, double cameraX, double cameraY, double cameraZ) {
        return EntityCulling.isVisible(entity, frustum, aABB, cameraX, cameraY, cameraZ);
    }
}
