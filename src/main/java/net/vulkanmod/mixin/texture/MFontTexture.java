package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.font.FontTexture;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FontTexture.class)
public class MFontTexture implements VAbstractTextureI {
    private int vulkanMod$id;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(Lcom/mojang/blaze3d/platform/NativeImage$InternalGlFormat;III)V"))
    private void redirect(NativeImage.InternalGlFormat internalFormat, int id, int width, int height) {
                                                                           
        VulkanImage image = new VulkanImage.Builder(width, height).setFormat(internalFormat).createVulkanImage();
        this.setId(id);
        this.setVulkanImage(image);
                                             
    }

    @Override
    public void bindTexture() {
        GlTexture.bindTexture(this.vulkanMod$id);
    }

    @Override
    public void setId(int id) {
        this.vulkanMod$id = id;
    }

    @Override
    public VulkanImage getVulkanImage() {
        GlTexture glTexture = GlTexture.getTexture(this.vulkanMod$id);
        return glTexture == null ? null : glTexture.getVulkanImage();
    }

    @Override
    public void setVulkanImage(VulkanImage image) {
        GlTexture.setVulkanImage(this.vulkanMod$id, image);
    }
}
