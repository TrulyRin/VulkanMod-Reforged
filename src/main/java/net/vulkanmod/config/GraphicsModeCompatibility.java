package net.vulkanmod.config;

import net.minecraft.client.GraphicsStatus;

public final class GraphicsModeCompatibility {
    private GraphicsModeCompatibility() {
    }

    public static GraphicsStatus coerce(GraphicsStatus requested) {
        if (requested == GraphicsStatus.FABULOUS) {
            return GraphicsStatus.FANCY;
        }

        if (requested == GraphicsStatus.FANCY) {
            return GraphicsStatus.FANCY;
        }

        return GraphicsStatus.FAST;
    }

    public static GraphicsStatus[] supportedModes() {
        return new GraphicsStatus[]{GraphicsStatus.FAST, GraphicsStatus.FANCY};
    }
}
