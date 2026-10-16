package net.vulkanmod.vulkan.texture;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR;

import static net.vulkanmod.vulkan.texture.SamplerManager.*;

public class SamplerInfo {
    final int encodedState;
    final float maxLod;
    final int maxAnisotropy;

    public SamplerInfo() {
        this(VK_SAMPLER_ADDRESS_MODE_REPEAT, VK_SAMPLER_ADDRESS_MODE_REPEAT,
             VK_FILTER_NEAREST, VK_FILTER_NEAREST, VK_SAMPLER_MIPMAP_MODE_NEAREST,
             0, false, 0, -1);
    }

    public SamplerInfo(int addressModeU, int addressModeV,
                       int minFilter, int magFilter, int mipmapMode,
                       float maxLod, boolean anisotropy, float maxAnisotropy,
                       int reductionMode)
    {
        this(addressModeU, addressModeV, minFilter, magFilter, mipmapMode, maxLod,
             anisotropy, maxAnisotropy, false, 0, reductionMode);
    }

    public SamplerInfo(int addressModeU, int addressModeV,
                       int minFilter, int magFilter, int mipmapMode,
                       float maxLod, boolean anisotropy, float maxAnisotropy,
                       boolean compare, int compareOp,
                       int reductionMode)
    {
        this.maxLod = maxLod;
        this.maxAnisotropy = (int) maxAnisotropy;

        this.encodedState = getEncodedState(addressModeU, addressModeV, minFilter, magFilter, mipmapMode,
                                            anisotropy, compare, compareOp, reductionMode);
    }

    public int getAddressModeU() {
        return (this.encodedState >> ADDRESS_MODE_U_OFFSET) & ADDRESS_MODE_MASK;
    }

    public int getAddressModeV() {
        return (this.encodedState >> ADDRESS_MODE_V_OFFSET) & ADDRESS_MODE_MASK;
    }

    public int getMinFilter() {
        return (this.encodedState >> MIN_FILTER_OFFSET) & 1;
    }

    public int getMagFilter() {
        return (this.encodedState >> MAG_FILTER_OFFSET) & 1;
    }

    public int getMipmapMode() {
        return (this.encodedState >> MIPMAP_MODE_OFFSET) & 1;
    }

    public boolean getAnisotropy() {
        return ((this.encodedState >> ANISOTROPY_OFFSET) & 1) != 0;
    }

    public boolean compareEnabled() {
        return ((this.encodedState >> COMPARE_ENABLED_OFFSET) & 1) != 0;
    }

    public int getCompareOp() {
        return (this.encodedState >> COMPARE_OP_OFFSET) & COMPARE_OP_MASK;
    }

    public boolean hasReductionMode() {
        return ((this.encodedState >> REDUCTION_MODE_ENABLE_OFFSET) & 1) != 0;
    }

    public int getReductionMode() {
        return (this.encodedState >> REDUCTION_MODE_OFFSET) & REDUCTION_MODE_MASK;
    }

    public int getMaxAnisotropy() {
        return maxAnisotropy;
    }

    public float getMaxLod() {
        return maxLod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SamplerInfo samplerInfo = (SamplerInfo) o;
        return Float.compare(maxLod, samplerInfo.maxLod) == 0 && maxAnisotropy == samplerInfo.maxAnisotropy && encodedState == samplerInfo.encodedState;
    }

    @Override
    public int hashCode() {
        int result = encodedState;
        result = 31 * result + Float.hashCode(maxLod);
        result = 31 * result + maxAnisotropy;
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        int addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
        int addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;

        int minFilter = VK_FILTER_LINEAR;
        int magFilter = VK_FILTER_LINEAR;
        int mipmapMode = VK_SAMPLER_MIPMAP_MODE_LINEAR;

        float maxLod = 0.0f;
        boolean anisotropy = false;
        float maxAnisotropy = 0.0f;

        boolean compareEnabled = false;
        int compareOp = VK_COMPARE_OP_ALWAYS;

        int reductionMode = 0;

        Builder() {}

        public Builder setAddressMode(int addressMode) {
            this.addressModeU = addressMode;
            this.addressModeV = addressMode;

            return this;
        }

        public Builder setFiltering(int minFilter, int magFilter, int mipmapMode) {
            this.minFilter = minFilter;
            this.magFilter = magFilter;
            this.mipmapMode = mipmapMode;

            return this;
        }

        public Builder setMaxLod(float maxLod) {
            this.maxLod = maxLod;

            return this;
        }

        public Builder setAnisotropy(float maxAnisotropy) {
            this.anisotropy = true;
            this.maxAnisotropy = maxAnisotropy;

            return this;
        }

        public Builder setCompare(boolean enable, int compareOp) {
            this.compareEnabled = enable;
            this.compareOp = compareOp;

            return this;
        }

        public Builder setReductionMode(int reductionMode) {
            this.reductionMode = reductionMode;

            return this;
        }

        public SamplerInfo createSamplerInfo() {
            return new SamplerInfo(addressModeU, addressModeV,
                                   minFilter, magFilter,
                                   mipmapMode, maxLod,
                                   anisotropy, maxAnisotropy,
                                   compareEnabled, compareOp,
                                   reductionMode);
        }
    }
}
