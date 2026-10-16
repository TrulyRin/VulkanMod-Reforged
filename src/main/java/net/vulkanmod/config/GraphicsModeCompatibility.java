package net.vulkanmod.config;

import net.minecraft.client.GraphicsPreset;

public final class GraphicsModeCompatibility {
    private GraphicsModeCompatibility() {
    }

    public static GraphicsPreset coerce(GraphicsPreset requested) {
        if (requested == GraphicsPreset.FABULOUS) {
            return GraphicsPreset.FANCY;
        }

        if (requested == GraphicsPreset.CUSTOM) {
            return GraphicsPreset.CUSTOM;
        }

        if (requested == GraphicsPreset.FANCY) {
            return GraphicsPreset.FANCY;
        }

        return GraphicsPreset.FAST;
    }

    public static GraphicsPreset[] supportedModes() {
        return new GraphicsPreset[]{GraphicsPreset.FAST, GraphicsPreset.FANCY, GraphicsPreset.CUSTOM};
    }
}
