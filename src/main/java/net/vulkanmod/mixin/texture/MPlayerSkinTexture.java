package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HttpTexture.class, priority = 900)
public class MPlayerSkinTexture {

                                                                                        
                                                     
    @Inject(method = "m_118020_", at = @At("HEAD"), cancellable = true, remap = false)
    private void upload(NativeImage image, CallbackInfo ci) {
        VulkanImage vulkanImage = new VulkanImage.Builder(image.getWidth(), image.getHeight()).createVulkanImage();
        ((VAbstractTextureI)this).setVulkanImage(vulkanImage);
        ((VAbstractTextureI)this).bindTexture();
        image.upload(0, 0, 0, true);
        ci.cancel();
    }
}
