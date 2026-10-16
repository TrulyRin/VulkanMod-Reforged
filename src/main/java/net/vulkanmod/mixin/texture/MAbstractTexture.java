package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractTexture.class, priority = 900)
public abstract class MAbstractTexture {
    @Shadow protected boolean blur;
    @Shadow protected boolean mipmap;

    @Shadow protected int id;

    @Inject(method = "setFilter", at = @At("TAIL"))
    private void updateVulkanSampler(boolean blur, boolean mipmap, CallbackInfo ci) {
        GlTexture glTexture = GlTexture.getTexture(this.id);
        if (glTexture == null) {
            return;
        }

        VulkanImage vulkanImage = glTexture.getVulkanImage();
        if (vulkanImage != null) {
            vulkanImage.updateTextureSampler(this.blur, false, this.mipmap);
        }
    }
}
