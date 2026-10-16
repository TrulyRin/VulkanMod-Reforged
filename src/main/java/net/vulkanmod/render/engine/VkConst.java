package net.vulkanmod.render.engine;

import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import org.lwjgl.vulkan.VK10;

public class VkConst {

    public static int of(AddressMode addressMode) {
        return switch (addressMode) {
            case REPEAT -> VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
            case CLAMP_TO_EDGE -> VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
        };
    }

    public static int of(FilterMode filterMode) {
        return switch (filterMode) {
            case NEAREST -> VK10.VK_FILTER_NEAREST;
            case LINEAR -> VK10.VK_FILTER_LINEAR;
        };
    }
}
