package net.vulkanmod.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VulkanImage;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;

                                                                                             
public final class MinecraftRenderScale {
    private static final WorldRenderScale<RenderTarget> WORLD = new WorldRenderScale<>(
            new WorldRenderScale.Backend<>() {
                @Override
                public RenderTarget create(int width, int height, RenderTarget original) {
                    return new TextureTarget("VulkanMod scaled world", width, height, true, original.useStencil);
                }

                @Override
                public void clear(RenderTarget target) {
                    RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(
                            target.getColorTexture(), 0, target.getDepthTexture(), 1.0);
                }

                @Override
                public void endRendering() {
                    Renderer.getInstance().endRenderPass();
                }

                @Override
                public void resolve(RenderTarget source, RenderTarget destination) {
                    VulkanImage sourceImage = ((VkGpuTexture) Objects.requireNonNull(
                            source.getColorTexture(), "scaled world color attachment")).getVulkanImage();
                    VkGpuTexture destinationTexture = (VkGpuTexture) Objects.requireNonNull(
                            destination.getColorTexture(), "native color attachment");
                    VulkanImage destinationImage = destinationTexture.getVulkanImage();
                                                                                                                 
                    ImageUtil.blitFramebuffer(sourceImage, destinationImage, VK_FILTER_LINEAR);
                                                                                              
                                                                                                  
                    destinationTexture.completeColorOverwrite();
                }

                @Override
                public void release(RenderTarget target) {
                    target.destroyBuffers();
                }
            });

    private MinecraftRenderScale() {
    }

    public static RenderTarget targetOrOriginal(RenderTarget original) {
        return WORLD.targetOrOriginal(original);
    }

    public static void renderLevel(Minecraft minecraft, Runnable worldRenderer) {
        RenderTarget original = minecraft.getMainRenderTarget();
        int scale = Initializer.CONFIG == null ? RenderScale.DEFAULT : Initializer.CONFIG.renderScale;
        WORLD.render(original, original.width, original.height, scale,
                minecraft.level != null, worldRenderer);
    }

    public static void close() {
        WORLD.close();
    }
}
