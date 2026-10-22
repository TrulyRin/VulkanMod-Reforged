package net.vulkanmod.render.texture;

import com.mojang.blaze3d.platform.NativeImage;


public final class SpriteInterpolation {
    private SpriteInterpolation() {}

    public static boolean interpolate(NativeImage[] source, NativeImage[] output, int width, int height,
                                      int firstX, int firstY, int nextX, int nextY, double delta) {
        if (width >= 32 && tightMipStrides(source, output, width, height)) {
            return SpriteInterpolationTight.interpolate(source, output, width, height,
                    firstX, firstY, nextX, nextY, delta);
        }
        return SpriteInterpolationScalar.interpolate(source, output, width, height,
                firstX, firstY, nextX, nextY, delta);
    }

    private static boolean tightMipStrides(NativeImage[] source, NativeImage[] output, int width, int height) {
        if (source == null || output == null || source.length != output.length
                || output.length > 31 || height < 1) return false;
        for (int mip = 0; mip < output.length; mip++) {
            int w = width >> mip, h = height >> mip;
            if (w < 1 || h < 1) continue;
            NativeImage input = source[mip], destination = output[mip];
            if (input == null || destination == null || input.getWidth() != w || destination.getWidth() != w)
                return false;
        }
        return true;
    }

    interface Memory extends SpriteInterpolationScalar.Memory, SpriteInterpolationTight.Memory {}

    static boolean interpolate(NativeImage[] source, NativeImage[] output, int width, int height,
                               int firstX, int firstY, int nextX, int nextY, double delta, Memory memory) {
        if (width >= 32 && tightMipStrides(source, output, width, height)) {
            return SpriteInterpolationTight.interpolate(source, output, width, height,
                    firstX, firstY, nextX, nextY, delta, memory);
        }
        return SpriteInterpolationScalar.interpolate(source, output, width, height,
                firstX, firstY, nextX, nextY, delta, memory);
    }
}
