package net.vulkanmod.mixin.matrix;

import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

                                                                                            
@Mixin(GameRenderer.class)
public abstract class GameRendererProjectionM {
    @Redirect(method = "getProjectionMatrix", at = @At(value = "INVOKE",
            target = "Lorg/joml/Matrix4f;perspective(FFFF)Lorg/joml/Matrix4f;", remap = false))
    private Matrix4f vulkanmod$useVulkanDepth(Matrix4f matrix, float fov, float aspect, float near, float far) {
        return matrix.perspective(fov, aspect, near, far, true);
    }
}
