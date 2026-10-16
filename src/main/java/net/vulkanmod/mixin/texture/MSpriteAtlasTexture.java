package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextureAtlas.class, priority = 900)
public class MSpriteAtlasTexture {

    @Redirect(method = "m_247065_", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(IIII)V"), remap = false)
    private void redirect(int id, int maxLevel, int width, int height) {
        VulkanImage image = new VulkanImage.Builder(width, height).setMipLevels(maxLevel + 1).createVulkanImage();
        ((VAbstractTextureI)(this)).setVulkanImage(image);
        ((VAbstractTextureI)(this)).bindTexture();
    }

                                                                                              
                                                                                            
    @Inject(method = "m_247255_", at = @At("HEAD"), cancellable = true, remap = false)
    private void skipSetFilter(SpriteLoader.Preparations data, CallbackInfo ci) {
        ci.cancel();
    }
}
