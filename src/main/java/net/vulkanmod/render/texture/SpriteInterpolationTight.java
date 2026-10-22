package net.vulkanmod.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.vulkanmod.mixin.texture.SpriteInterpolationImageAccessor;
import net.vulkanmod.mixin.texture.image.NativeImageAccessor;
import org.lwjgl.system.MemoryUtil;


final class SpriteInterpolationTight {
    private SpriteInterpolationTight() {}

    private static final Memory NATIVE_MEMORY = new Memory() {
        public long pixels(NativeImage image) {
            return (Object) image instanceof NativeImageAccessor accessor ? accessor.getPixels() : 0;
        }
        public long size(NativeImage image) {
            return (Object) image instanceof SpriteInterpolationImageAccessor accessor ? accessor.vulkanmod$getSize() : 0;
        }
    };

    interface Memory {
        long pixels(NativeImage image);
        long size(NativeImage image);
    }

    public static boolean interpolate(NativeImage[] source, NativeImage[] output, int width, int height,
                                      int firstX, int firstY, int nextX, int nextY, double delta) {
        return interpolate(source, output, width, height, firstX, firstY, nextX, nextY, delta, NATIVE_MEMORY);
    }

    static boolean interpolate(NativeImage[] source, NativeImage[] output, int width, int height,
                               int firstX, int firstY, int nextX, int nextY, double delta, Memory memory) {
        if (source == null || output == null || memory == null || source.length != output.length
                || output.length > 31 || width < 1 || height < 1
                || !validOrigin(firstX, width) || !validOrigin(firstY, height)
                || !validOrigin(nextX, width) || !validOrigin(nextY, height)) return false;


        for (int mip = 0; mip < output.length; mip++) {
            int w = width >> mip, h = height >> mip;
            if (w < 1 || h < 1) continue;
            NativeImage input = source[mip], destination = output[mip];
            if (!validImage(input, memory) || !validImage(destination, memory)
                    || !contains(input, firstX * width >> mip, firstY * height >> mip, w, h)
                    || !contains(input, nextX * width >> mip, nextY * height >> mip, w, h)
                    || !contains(destination, 0, 0, w, h)) return false;
        }
        for (int mip = 0; mip < output.length; mip++) {
            if ((width >> mip) < 1 || (height >> mip) < 1) continue;
            NativeImage destination = output[mip];
            long outputPixels = memory.pixels(destination), outputBytes = memory.size(destination);
            for (int other = 0; other < output.length; other++) {
                if ((width >> other) < 1 || (height >> other) < 1) continue;
                if (overlaps(outputPixels, outputBytes, memory.pixels(source[other]), memory.size(source[other]))) return false;
                if (other < mip && overlaps(outputPixels, outputBytes, memory.pixels(output[other]), memory.size(output[other]))) return false;
            }
        }

        double inverse = 1.0 - delta;
        for (int mip = 0; mip < output.length; mip++) {
            int w = width >> mip, h = height >> mip;
            if (w < 1 || h < 1) continue;
            NativeImage input = source[mip], destination = output[mip];
            long pixels = memory.pixels(input), inputStride = 4L * input.getWidth();

            long firstRow = pixels + ((long) (firstY * height >> mip) * input.getWidth()
                    + (firstX * width >> mip)) * 4L;
            long nextRow = pixels + ((long) (nextY * height >> mip) * input.getWidth()
                    + (nextX * width >> mip)) * 4L;
            long outputRow = memory.pixels(destination), outputStride = 4L * destination.getWidth();

            if (inputStride == 4L * w && outputStride == 4L * w) {
                long rowPixels = (long) w * h;
                long firstPixel = firstRow, nextPixel = nextRow, outputPixel = outputRow;
                long pairedPixels = rowPixels & ~1L;
                for (long x = 0; x < pairedPixels; x += 2) {
                    int first = MemoryUtil.memGetInt(firstPixel), next = MemoryUtil.memGetInt(nextPixel);
                    int c16 = (int) (delta * (double) (first >> 16 & 255) + inverse * (double) (next >> 16 & 255));
                    int c8 = (int) (delta * (double) (first >> 8 & 255) + inverse * (double) (next >> 8 & 255));
                    int c0 = (int) (delta * (double) (first & 255) + inverse * (double) (next & 255));
                    MemoryUtil.memPutInt(outputPixel, first & 0xFF000000 | c16 << 16 | c8 << 8 | c0);
                    first = MemoryUtil.memGetInt(firstPixel + 4); next = MemoryUtil.memGetInt(nextPixel + 4);
                    c16 = (int) (delta * (double) (first >> 16 & 255) + inverse * (double) (next >> 16 & 255));
                    c8 = (int) (delta * (double) (first >> 8 & 255) + inverse * (double) (next >> 8 & 255));
                    c0 = (int) (delta * (double) (first & 255) + inverse * (double) (next & 255));
                    MemoryUtil.memPutInt(outputPixel + 4, first & 0xFF000000 | c16 << 16 | c8 << 8 | c0);
                    firstPixel += 8; nextPixel += 8; outputPixel += 8;
                }
                if ((rowPixels & 1L) != 0) {
                    int first = MemoryUtil.memGetInt(firstPixel), next = MemoryUtil.memGetInt(nextPixel);
                    int c16 = (int) (delta * (double) (first >> 16 & 255) + inverse * (double) (next >> 16 & 255));
                    int c8 = (int) (delta * (double) (first >> 8 & 255) + inverse * (double) (next >> 8 & 255));
                    int c0 = (int) (delta * (double) (first & 255) + inverse * (double) (next & 255));
                    MemoryUtil.memPutInt(outputPixel, first & 0xFF000000 | c16 << 16 | c8 << 8 | c0);
                }
            } else {
                for (int y = 0; y < h; y++) {
                    long firstPixel = firstRow, nextPixel = nextRow, outputPixel = outputRow;
                    for (int x = 0; x < w; x++) {
                        int first = MemoryUtil.memGetInt(firstPixel), next = MemoryUtil.memGetInt(nextPixel);
                        int c16 = (int) (delta * (double) (first >> 16 & 255) + inverse * (double) (next >> 16 & 255));
                        int c8 = (int) (delta * (double) (first >> 8 & 255) + inverse * (double) (next >> 8 & 255));
                        int c0 = (int) (delta * (double) (first & 255) + inverse * (double) (next & 255));
                        MemoryUtil.memPutInt(outputPixel, first & 0xFF000000 | c16 << 16 | c8 << 8 | c0);
                        firstPixel += 4; nextPixel += 4; outputPixel += 4;
                    }
                    firstRow += inputStride; nextRow += inputStride; outputRow += outputStride;
                }
            }
        }
        return true;
    }

    private static boolean validOrigin(int frame, int extent) {
        return frame >= 0 && (long) frame * extent <= Integer.MAX_VALUE;
    }

    private static boolean validImage(NativeImage image, Memory memory) {
        if (image == null || image.format() != NativeImage.Format.RGBA || image.getWidth() < 1 || image.getHeight() < 1) return false;
        long rowBytes = 4L * image.getWidth();
        if (rowBytes > Long.MAX_VALUE / image.getHeight()) return false;
        long requiredBytes = rowBytes * image.getHeight(), pixels = memory.pixels(image), size = memory.size(image);
        return pixels > 0 && (pixels & 3) == 0 && size >= requiredBytes && size <= Long.MAX_VALUE - pixels;
    }

    private static boolean contains(NativeImage image, int x, int y, int width, int height) {
        return x >= 0 && y >= 0 && width <= image.getWidth() && height <= image.getHeight()
                && x <= image.getWidth() - width && y <= image.getHeight() - height;
    }

    private static boolean overlaps(long first, long firstBytes, long second, long secondBytes) {
        return first < second + secondBytes && second < first + firstBytes;
    }
}
