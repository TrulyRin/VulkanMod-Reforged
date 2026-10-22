package net.vulkanmod.mixin.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Screenshot.class)
public class ScreenshotRecorderM {




    @Overwrite
    public static NativeImage takeScreenshot(RenderTarget target) {
        int textureId = target.getColorTextureId();
        GlTexture texture = GlTexture.getTexture(textureId);
        VulkanImage source = texture != null ? texture.getVulkanImage() : null;
        if (source == null) {
            throw new IllegalStateException("Screenshot target has no color image");
        }



        NativeImage nativeimage = new NativeImage(source.width, source.height, false);

        try {
            GlTexture.bindTextureUnit(0, textureId);
            return Renderer.getInstance().flushCmds(() -> {
                nativeimage.downloadTexture(0, true);


                if (source.requiresGlDownloadFlip(0)) nativeimage.flipY();
                return nativeimage;
            });
        } catch (RuntimeException | Error failure) {
            try { nativeimage.close(); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
}
