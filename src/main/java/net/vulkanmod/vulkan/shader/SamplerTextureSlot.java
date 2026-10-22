package net.vulkanmod.vulkan.shader;

public final class SamplerTextureSlot {
    private SamplerTextureSlot() {
    }

    public static int getTextureIdx(String name) {
        Integer textureIdx = getKnownTextureIdx(name);
        if (textureIdx == null) {
            throw new IllegalStateException("Unknown sampler name: " + name);
        }
        return textureIdx;
    }

    public static int getTextureIdxOrDefault(String name, int defaultTextureIdx) {
        Integer textureIdx = getKnownTextureIdx(name);
        return textureIdx != null ? textureIdx : defaultTextureIdx;
    }

    private static Integer getKnownTextureIdx(String name) {
        return switch (name) {
            case "Sampler0", "DiffuseSampler" -> 0;
            case "Sampler1" -> 1;
            case "Sampler2" -> 2;
            case "Sampler3" -> 3;
            case "Sampler4" -> 4;
            case "Sampler5" -> 5;
            case "Sampler6" -> 6;
            case "Sampler7" -> 7;
            case "uLightMap" -> 0;
            default -> null;
        };
    }
}
