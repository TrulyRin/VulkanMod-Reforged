package net.vulkanmod.vulkan.texture;

import it.unimi.dsi.fastutil.shorts.Short2LongMap;
import it.unimi.dsi.fastutil.shorts.Short2LongOpenHashMap;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkSamplerReductionModeCreateInfo;

import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

import static net.vulkanmod.vulkan.Vulkan.getVkDevice;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_SAMPLER_REDUCTION_MODE_MAX;
import static org.lwjgl.vulkan.VK12.VK_SAMPLER_REDUCTION_MODE_MIN;

public abstract class SamplerManager {
    static final float MIP_BIAS = -0.5f;

    static final Short2LongMap SAMPLERS = new Short2LongOpenHashMap();
    private static final Map<SamplerKey, Long> GL_SAMPLERS = new HashMap<>();

    public record SamplerKey(int minFilter, int magFilter,
                             int wrapS, int wrapT, int wrapR,
                             float minLod, float maxLod, float lodBias,
                             int compareMode, int compareFunc) {
    }

    public static long getTextureSampler(byte maxLod, byte flags) {
        short key = (short) (flags | (maxLod << 8));
        long sampler = SAMPLERS.getOrDefault(key, 0L);

        if (sampler == 0L) {
            sampler = createTextureSampler(maxLod, flags);
            SAMPLERS.put(key, sampler);
        }

        return sampler;
    }

    public static long getTextureSampler(SamplerKey key) {
        return GL_SAMPLERS.computeIfAbsent(key, SamplerManager::createTextureSampler);
    }

    private static long createTextureSampler(SamplerKey key) {
        try (MemoryStack stack = stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .minFilter(toVkFilter(key.minFilter()))
                    .magFilter(toVkFilter(key.magFilter()))
                    .mipmapMode(toVkMipmapMode(key.minFilter()))
                    .addressModeU(toVkAddressMode(key.wrapS()))
                    .addressModeV(toVkAddressMode(key.wrapT()))
                    .addressModeW(toVkAddressMode(key.wrapR()))
                    .mipLodBias(key.lodBias())
                    .minLod(key.minLod())
                    .maxLod(usesMipmaps(key.minFilter()) ? key.maxLod() : 0.0f)
                    .anisotropyEnable(false)
                    .compareEnable(key.compareMode() == 0x884E)
                    .compareOp(toVkCompareOp(key.compareFunc()))
                    .borderColor(VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK)
                    .unnormalizedCoordinates(false);

            LongBuffer sampler = stack.mallocLong(1);
            if (vkCreateSampler(getVkDevice(), samplerInfo, null, sampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create OpenGL-compatible Vulkan sampler");
            }
            return sampler.get(0);
        }
    }

    static boolean usesMipmaps(int minFilter) {
        return minFilter != GL_NEAREST && minFilter != GL_LINEAR;
    }

    static int toVkFilter(int filter) {
        return switch (filter) {
            case GL_LINEAR, GL_LINEAR_MIPMAP_NEAREST, GL_LINEAR_MIPMAP_LINEAR -> VK_FILTER_LINEAR;
            default -> VK_FILTER_NEAREST;
        };
    }

    static int toVkMipmapMode(int minFilter) {
        return switch (minFilter) {
            case GL_NEAREST_MIPMAP_LINEAR, GL_LINEAR_MIPMAP_LINEAR -> VK_SAMPLER_MIPMAP_MODE_LINEAR;
            default -> VK_SAMPLER_MIPMAP_MODE_NEAREST;
        };
    }

    static int toVkAddressMode(int wrap) {
        return switch (wrap) {
            case GL_CLAMP, 0x812F -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
            case 0x812D -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
            case 0x8370 -> VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT;
            case 0x8743 -> VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
            default -> VK_SAMPLER_ADDRESS_MODE_REPEAT;
        };
    }

    static int toVkCompareOp(int compareFunc) {
        return switch (compareFunc) {
            case GL_NEVER -> VK_COMPARE_OP_NEVER;
            case GL_LESS -> VK_COMPARE_OP_LESS;
            case GL_EQUAL -> VK_COMPARE_OP_EQUAL;
            case GL_LEQUAL -> VK_COMPARE_OP_LESS_OR_EQUAL;
            case GL_GREATER -> VK_COMPARE_OP_GREATER;
            case GL_NOTEQUAL -> VK_COMPARE_OP_NOT_EQUAL;
            case GL_GEQUAL -> VK_COMPARE_OP_GREATER_OR_EQUAL;
            default -> VK_COMPARE_OP_ALWAYS;
        };
    }

    private static long createTextureSampler(byte maxLod, byte flags) {
        Validate.isTrue(
                (flags & (REDUCTION_MIN_BIT | REDUCTION_MAX_BIT)) != (REDUCTION_MIN_BIT | REDUCTION_MAX_BIT)
        );

        try (MemoryStack stack = stackPush()) {

            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);

            if ((flags & LINEAR_FILTERING_BIT) != 0) {
                samplerInfo.magFilter(VK_FILTER_LINEAR);
                samplerInfo.minFilter(VK_FILTER_LINEAR);
            } else {
                samplerInfo.magFilter(VK_FILTER_NEAREST);
                samplerInfo.minFilter(VK_FILTER_NEAREST);
            }

            if ((flags & CLAMP_BIT) != 0) {
                samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
                samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
                samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            } else {
                samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT);
                samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT);
            }

            samplerInfo.anisotropyEnable(false);
                                               
            samplerInfo.borderColor(VK_BORDER_COLOR_INT_OPAQUE_WHITE);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK_COMPARE_OP_ALWAYS);

            if ((flags & USE_MIPMAPS_BIT) == 0) {
                samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);
                samplerInfo.maxLod(0.0F);
                samplerInfo.minLod(0.0F);
            } else {
                if ((flags & MIPMAP_LINEAR_FILTERING_BIT) != 0) {
                    samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR);
                } else {
                    samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);
                }
                samplerInfo.maxLod(maxLod);
                samplerInfo.minLod(0.0F);
                samplerInfo.mipLodBias(MIP_BIAS);
            }

                            
            if ((flags & (REDUCTION_MAX_BIT | REDUCTION_MIN_BIT)) != 0) {
                VkSamplerReductionModeCreateInfo reductionModeInfo = VkSamplerReductionModeCreateInfo.calloc(stack);
                reductionModeInfo.sType$Default();
                reductionModeInfo.reductionMode((flags & REDUCTION_MAX_BIT) != 0 ? VK_SAMPLER_REDUCTION_MODE_MAX : VK_SAMPLER_REDUCTION_MODE_MIN);
                samplerInfo.pNext(reductionModeInfo.address());
            }

            LongBuffer pTextureSampler = stack.mallocLong(1);

            if (vkCreateSampler(getVkDevice(), samplerInfo, null, pTextureSampler) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }

            return pTextureSampler.get(0);
        }
    }

    public static void cleanUp() {
        for (long id : SAMPLERS.values()) {
            vkDestroySampler(DeviceManager.vkDevice, id, null);
        }
        for (long id : GL_SAMPLERS.values()) {
            vkDestroySampler(DeviceManager.vkDevice, id, null);
        }
        SAMPLERS.clear();
        GL_SAMPLERS.clear();
    }

    public static final byte LINEAR_FILTERING_BIT = 0b1;
    public static final byte CLAMP_BIT = 0b10;
    public static final byte USE_MIPMAPS_BIT = 0b100;
    public static final byte MIPMAP_LINEAR_FILTERING_BIT = 0b1000;
    public static final byte REDUCTION_MIN_BIT = 0b10000;
    public static final byte REDUCTION_MAX_BIT = 0b100000;
}
