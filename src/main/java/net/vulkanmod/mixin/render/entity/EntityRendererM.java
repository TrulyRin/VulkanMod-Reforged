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
public abstract class EntityRendererM<T extends Entity> {
    @Redirect(method = "shouldRender", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", ordinal = 0))
    private boolean vulkanmod$isEntityVisible(Frustum frustum, AABB bounds, T entity,
                                              Frustum originalFrustum, double cameraX, double cameraY, double cameraZ) {
        return EntityCulling.isVisible(entity, frustum, bounds, cameraX, cameraY, cameraZ);
    }
}
