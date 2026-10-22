package net.vulkanmod.compat.gl;


public final class TextureUnitLimits {
    public static final int COMBINED = 48;

    private TextureUnitLimits() {
    }

    public static boolean isValid(int unit) {
        return unit >= 0 && unit < COMBINED;
    }
}
