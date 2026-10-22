package net.vulkanmod.config;

public final class RenderScale {
    public static final int MIN = 50;
    public static final int MAX = 100;
    public static final int STEP = 5;
    public static final int DEFAULT = 100;

    private RenderScale() {
    }

    public static int clamp(int scale) {
        return Math.max(MIN, Math.min(MAX, scale));
    }

    public static boolean isScaled(int scale) {
        return clamp(scale) < MAX;
    }

    public static int scaleDimension(int dimension, int scale) {
        return Math.max(1, Math.round(dimension * (clamp(scale) / 100.0f)));
    }
}
