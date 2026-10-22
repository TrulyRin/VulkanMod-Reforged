package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.gl.TextureUnitLimits;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SamplerTextureSlot;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class VTextureSelector {
    public static final int SIZE = TextureUnitLimits.COMBINED;

    private static final VulkanImage[] boundTextures = new VulkanImage[SIZE];
    private static final int[] textureOwners = new int[SIZE];

    private static final int[] levels = new int[SIZE];

    private static VulkanImage whiteTexture;

    private static int activeTexture = 0;
    private static final Set<Integer> missingTextureWarnings = ConcurrentHashMap.newKeySet();


    public static BindingSnapshot captureBindings(long unitMask) {
        long validMask=(1L<<SIZE)-1L;
        if((unitMask&~validMask)!=0L) throw new IllegalArgumentException("Texture binding mask exceeds available units");
        int count=Long.bitCount(unitMask);
        VulkanImage[] images=new VulkanImage[count];
        GlTexture[] glOwners=new GlTexture[count];
        int[] owners=new int[count],imageLevels=new int[count];
        long remaining=unitMask;
        for(int index=0;remaining!=0L;index++) {
            int unit=Long.numberOfTrailingZeros(remaining);
            images[index]=boundTextures[unit]; owners[index]=textureOwners[unit]; imageLevels[index]=levels[unit];
            if(owners[index]!=0) glOwners[index]=GlTexture.getTexture(owners[index]);
            remaining&=remaining-1L;
        }
        return new BindingSnapshot(unitMask,images,owners,imageLevels,glOwners);
    }

    public static final class BindingSnapshot {
        private final long unitMask;
        private final VulkanImage[] images;
        private final GlTexture[] glOwners;
        private final int[] owners,imageLevels;
        private boolean restored;

        private BindingSnapshot(long unitMask,VulkanImage[] images,int[] owners,int[] imageLevels,GlTexture[] glOwners) {
            this.unitMask=unitMask; this.images=images; this.owners=owners; this.imageLevels=imageLevels; this.glOwners=glOwners;
        }


        public void restore() {
            if(restored) return;
            restored=true;
            long remaining=unitMask;
            for(int index=0;remaining!=0L;index++) {
                int unit=Long.numberOfTrailingZeros(remaining);
                if(owners[index]==0) {
                    boundTextures[unit]=images[index]; textureOwners[unit]=0; levels[unit]=imageLevels[index];
                } else {
                    GlTexture owner=glOwners[index];
                    boolean live=owner!=null && GlTexture.getTexture(owners[index])==owner;
                    boundTextures[unit]=live?owner.getVulkanImage():null;
                    textureOwners[unit]=live?owners[index]:0;
                    levels[unit]=-1;
                }
                remaining&=remaining-1L;
            }
        }
    }

    public static void bindTexture(VulkanImage texture) {
        bindTexture(0, texture);
    }

    public static void bindTexture(int i, VulkanImage texture) {
        if (!TextureUnitLimits.isValid(i)) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        textureOwners[i] = 0;
        levels[i] = -1;
    }


    public static void bindGlTexture(int unit, int textureId, VulkanImage texture) {
        if (!TextureUnitLimits.isValid(unit)) return;
        boundTextures[unit] = texture;
        textureOwners[unit] = textureId;
        levels[unit] = -1;
    }

    public static void updateGlTexture(int textureId, VulkanImage texture) {
        if (textureId <= 0) return;
        for (int unit = 0; unit < SIZE; unit++) {
            if (textureOwners[unit] == textureId) {
                boundTextures[unit] = texture;
                levels[unit] = -1;
            }
        }
    }

    public static void deleteGlTexture(int textureId) {
        if (textureId <= 0) return;
        for (int unit = 0; unit < SIZE; unit++) {
            if (textureOwners[unit] == textureId) bindTexture(unit, null);
        }
    }

    public static void bindImage(int i, VulkanImage texture, int level) {
        if(i < 0 || i > 7) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", i, SIZE - 1));
            return;
        }

        boundTextures[i] = texture;
        textureOwners[i] = 0;
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
        var descriptors = pipeline.getImageDescriptors();
        for (int i = 0; i < descriptors.size(); i++) {
            ImageDescriptor descriptor = descriptors.get(i);
            if (descriptor.usesVanillaTextureBinding()) {
                Object shader = RenderSystem.getShader();
                bindShaderTexture(pipeline, descriptor, shader instanceof ShaderMixed owner ? owner : null);
            }
        }
    }

    public static void bindShaderTextures(Pipeline pipeline, ShaderMixed owner) {
        var descriptors = pipeline.getImageDescriptors();
        for (int i = 0; i < descriptors.size(); i++) {
            ImageDescriptor descriptor = descriptors.get(i);
            if (descriptor.usesVanillaTextureBinding()) bindShaderTexture(pipeline, descriptor, owner);
        }
    }


    public static void bindShaderTextures(Iterable<ImageDescriptor> descriptors, Consumer<ImageDescriptor> bind) {
        for (ImageDescriptor descriptor : descriptors) {
            if (descriptor.usesVanillaTextureBinding()) bind.accept(descriptor);
        }
    }

    private static void bindShaderTexture(Pipeline pipeline, ImageDescriptor state, ShaderMixed owner) {
        int unit = state.getTextureUnit();
        int namedTexture = owner == null ? -1 : owner.samplerTextureId(pipeline, state.name);
        final int shaderTexture = namedTexture != -1 ? namedTexture : RenderSystem.getShaderTexture(unit);
        GlTexture texture = GlTexture.getTexture(shaderTexture);

        if (texture != null && texture.getVulkanImage() != null) {
            VTextureSelector.bindTexture(unit, texture.getVulkanImage());
        }
        else {
            VulkanImage fallback = getMissingTextureFallback();
            VTextureSelector.bindTexture(unit, fallback);
            if (missingTextureWarnings.add(shaderTexture)) {
                Initializer.LOGGER.warn("Missing Vulkan texture for shader texture {} on unit {} "
                                + "(descriptor {}); using the missing-texture fallback",
                        shaderTexture, unit, state.name);
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
        return getWhiteTexture();
    }

    public static VulkanImage getImage(int i) {
        return TextureUnitLimits.isValid(i) ? boundTextures[i] : null;
    }

    public static void setLightTexture(VulkanImage texture) {
        bindTexture(2, texture);
    }

    public static void setOverlayTexture(VulkanImage texture) {
        bindTexture(1, texture);
    }

    public static void setActiveTexture(int activeTexture) {
        if (!TextureUnitLimits.isValid(activeTexture)) {
            Initializer.LOGGER.error(String.format("On Texture binding: index %d out of range [0, %d]", activeTexture, SIZE - 1));
            return;
        }

        VTextureSelector.activeTexture = activeTexture;
    }

    public static VulkanImage getBoundTexture(int i) { return getImage(i); }

    public static VulkanImage getWhiteTexture() {
        if (whiteTexture == null) whiteTexture = VulkanImage.createWhiteTexture();
        return whiteTexture;
    }

    public static void freeWhiteTexture() {
        VulkanImage image = whiteTexture;
        whiteTexture = null;
        if (image != null) image.free();
    }
}
