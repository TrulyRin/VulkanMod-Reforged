package net.vulkanmod.mixin.matrix;

import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

                                                                                                  
@Mixin(CachedOrthoProjectionMatrixBuffer.class)
public abstract class CachedOrthoProjectionMatrixBufferM {
    @Redirect(method = "createProjectionMatrix", at = @At(value = "INVOKE",
            target = "Lorg/joml/Matrix4f;setOrtho(FFFFFF)Lorg/joml/Matrix4f;", remap = false))
    private Matrix4f vulkanmod$useVulkanDepth(Matrix4f matrix, float left, float right, float bottom,
                                            float top, float near, float far) {
        return matrix.setOrtho(left, right, bottom, top, near, far, true);
    }
}
