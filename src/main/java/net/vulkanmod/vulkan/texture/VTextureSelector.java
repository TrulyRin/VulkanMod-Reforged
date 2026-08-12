package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SamplerTextureSlot;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class VTextureSelector {
    public static final int SIZE = 12;

    private static final VulkanImage[] boundTextures = new VulkanImage[SIZE];

    private static final int[] levels = new int[SIZE];

    private static final VulkanImage whiteTexture = VulkanImage.createWhiteTexture();

    private static int activeTexture = 0;
    private static final Set<Integer> missingTextureWarnings = ConcurrentHashMap.newKeySet();

    public static void bindTexture(VulkanImage texture) {
        boundTextures[0] = texture;
    }

    public static void bindTexture(int i, VulkanImage texture) {
        if(i < 0 || i >= SIZE) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        levels[i] = -1;
    }

    public static void bindImage(int i, VulkanImage texture, int level) {
        if(i < 0 || i > 7) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        levels[i] = level;
    }

    public static void uploadSubTexture(int mipLevel, int width, int height, int xOffset, int yOffset, int unpackSkipRows, int unpackSkipPixels, int unpackRowLength, ByteBuffer buffer) {
        VulkanImage texture = boundTextures[activeTexture];

        if(texture == null)
            throw new NullPointerException("Texture is null at index: " + activeTexture);

        texture.uploadSubTextureAsync(mipLevel, width, height, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, unpackRowLength, buffer);
    }

    public static int getTextureIdx(String name) {
        return SamplerTextureSlot.getTextureIdx(name);
    }

    public static void bindShaderTextures(Pipeline pipeline) {
        var imageDescriptors = pipeline.getImageDescriptors();

        for (ImageDescriptor state : imageDescriptors) {
            final int shaderTexture = RenderSystem.getShaderTexture(state.imageIdx);

            GlTexture texture = GlTexture.getTexture(shaderTexture);

            if (texture != null && texture.getVulkanImage() != null) {
                VTextureSelector.bindTexture(state.imageIdx, texture.getVulkanImage());
            }
            else {
                VulkanImage fallback = getMissingTextureFallback();
                VTextureSelector.bindTexture(state.imageIdx, fallback);
                if (missingTextureWarnings.add(shaderTexture)) {
                    Initializer.LOGGER.warn("Missing Vulkan texture for shader texture {} on unit {} "
                                    + "(descriptor {}); using the missing-texture fallback",
                            shaderTexture, state.imageIdx, state.name);
                }
            }
        }
    }

    private static VulkanImage getMissingTextureFallback() {
        try {
            GlTexture missingTexture = GlTexture.getTexture(MissingTextureAtlasSprite.getTexture().getId());
            if (missingTexture != null && missingTexture.getVulkanImage() != null) {
                return missingTexture.getVulkanImage();
            }
        } catch (RuntimeException exception) {
        }
        return whiteTexture;
    }

    public static VulkanImage getImage(int i) {
        return boundTextures[i];
    }

    public static void setLightTexture(VulkanImage texture) {
        boundTextures[2] = texture;
    }

    public static void setOverlayTexture(VulkanImage texture) {
        boundTextures[1] = texture;
    }

    public static void setActiveTexture(int activeTexture) {
        if(activeTexture < 0 || activeTexture >= SIZE) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", activeTexture, SIZE - 1));
        }

        VTextureSelector.activeTexture = activeTexture;
    }

    public static VulkanImage getBoundTexture(int i) { return boundTextures[i]; }

    public static VulkanImage getWhiteTexture() { return whiteTexture; }
}
