package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.VAbstractTextureI;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTexture.class)
public abstract class MAbstractTexture implements VAbstractTextureI {
    @Shadow(remap = false) private int f_117950_;

    @Inject(method = "setFilter", at = @At("TAIL"))
    private void updateVulkanSampler(boolean blur, boolean mipmap, CallbackInfo ci) {
        GlTexture glTexture = GlTexture.getTexture(((AbstractTexture)(Object)this).getId());
        if (glTexture == null) {
            return;
        }

        VulkanImage vulkanImage = glTexture.getVulkanImage();
        if (vulkanImage != null) {
            vulkanImage.updateTextureSampler(blur, false, mipmap);
        }
    }

    @Override
    public void bindTexture() {
        GlTexture.bindTexture(((AbstractTexture)(Object)this).getId());
    }

    @Override
    public void setId(int id) {
        this.f_117950_ = id;
    }

    @Override
    public VulkanImage getVulkanImage() {
        GlTexture glTexture = GlTexture.getTexture(((AbstractTexture)(Object)this).getId());
        return glTexture == null ? null : glTexture.getVulkanImage();
    }

    @Override
    public void setVulkanImage(VulkanImage image) {
        GlTexture.setVulkanImage(((AbstractTexture)(Object)this).getId(), image);
    }
}
