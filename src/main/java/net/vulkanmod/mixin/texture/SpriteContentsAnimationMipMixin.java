package net.vulkanmod.mixin.texture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

                                                                                             
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$AnimatedTexture")
public class SpriteContentsAnimationMipMixin {
    @ModifyArg(method = "createAnimationState", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;"),
            index = 6, require = 1)
    private int vulkanmod$allocateOnlyUploadedMipLevels(int requestedLevels) {
                                                                                 
                                                                                   
        return requestedLevels - 1;
    }
}
