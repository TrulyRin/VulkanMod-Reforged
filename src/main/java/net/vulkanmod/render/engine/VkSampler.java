package net.vulkanmod.render.engine;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.vulkan.VK10;

import java.util.OptionalDouble;

public class VkSampler extends GpuSampler {
    private final AddressMode addressModeU;
    private final AddressMode addressModeV;
    private final FilterMode minFilter;
    private final FilterMode magFilter;
    private final int maxAnisotropy;
    private final OptionalDouble maxLod;
    private boolean closed;

    private final long id;

    public VkSampler(AddressMode addressModeU, AddressMode addressModeV, FilterMode minFilter, FilterMode magFilter,
                     int maxAnisotropy, OptionalDouble maxLod) {
        this.addressModeU = addressModeU;
        this.addressModeV = addressModeV;
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        this.maxAnisotropy = Math.max(1, Math.min(maxAnisotropy, DeviceManager.getMaxSupportedAnisotropy()));
        this.maxLod = maxLod;
        float nativeMaxLod = maxLod.isPresent() ? (float) maxLod.getAsDouble() : 1000.0f;

        this.id = SamplerManager.getSampler(VkConst.of(addressModeU), VkConst.of(addressModeV),
                                            VkConst.of(minFilter), VkConst.of(magFilter),
                                            VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR, nativeMaxLod,
                                            this.maxAnisotropy > 1, this.maxAnisotropy, -1);
    }



    public long getId() {
        return this.id;
    }

    @Override
    public AddressMode getAddressModeU() {
        return this.addressModeU;
    }

    @Override
    public AddressMode getAddressModeV() {
        return this.addressModeV;
    }

    @Override
    public FilterMode getMinFilter() {
        return this.minFilter;
    }

    @Override
    public FilterMode getMagFilter() {
        return this.magFilter;
    }

    @Override
    public int getMaxAnisotropy() {
        return this.maxAnisotropy;
    }

    @Override
    public OptionalDouble getMaxLod() {
        return this.maxLod;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
        }
    }

    public boolean isClosed() {
        return this.closed;
    }
}
