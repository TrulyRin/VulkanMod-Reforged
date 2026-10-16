package net.vulkanmod.compat.sable.veil;

import net.vulkanmod.compat.gl.TextureUnitLimits;

import java.util.EnumSet;

public final class VulkanVeilCapabilities {
    private static final EnumSet<Feature> SUPPORTED = EnumSet.of(
            Feature.FRAMEBUFFER_OBJECTS,
            Feature.SAMPLER_OBJECTS);

    private VulkanVeilCapabilities() {
    }

    public static boolean supported(Feature feature) {
        return SUPPORTED.contains(feature);
    }

    public static int maxCombinedTextureUnits() {
        return TextureUnitLimits.COMBINED;
    }

    public static int maxColorAttachments() {
        return 1;
    }

    public static int maxSamples() {
        return 1;
    }

    public enum Feature {
        FRAMEBUFFER_OBJECTS,
        SAMPLER_OBJECTS,
        COMPUTE_SHADERS,
        TESSELLATION,
        ATOMIC_COUNTERS,
        TRANSFORM_FEEDBACK,
        MULTIBIND,
        SPARSE_BUFFERS,
        DIRECT_STATE_ACCESS,
        SEPARATE_SHADER_OBJECTS,
        SHADER_STORAGE_BUFFERS,
        PROGRAM_INTERFACE_QUERY,
        DRAW_INDIRECT,
        MULTI_DRAW_INDIRECT,
        BINDLESS_TEXTURES
    }
}
