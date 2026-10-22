package net.vulkanmod.gl;

import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;


public final class GlReadPixels {
    private GlReadPixels() {}

    public static void readPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        if (!readPixelsFromFramebuffer(x, y, width, height, format, type, pixels)) {
            zeroReadPixelsFallback(width, height, format, type, pixels);
        }
    }

    public static void readPixels(int x, int y, int width, int height, int format, int type, long pixels) {
        if (!readPixelsFromFramebuffer(x, y, width, height, format, type, pixels)) {
            int byteCount = readPixelsByteCount(width, height, format, type);
            if (pixels != 0L && byteCount > 0) {
                MemoryUtil.memSet(pixels, 0, byteCount);
            }
        }
    }

    public static void readPixels(int x, int y, int width, int height, int format, int type, IntBuffer pixels) {
        zeroReadPixelsFallback(width, height, format, type, pixels);
    }

    public static void readPixels(int x, int y, int width, int height, int format, int type, FloatBuffer pixels) {
        if (!readPixelsFromFramebuffer(x, y, width, height, format, type, pixels)) {
            zeroReadPixelsFallback(width, height, format, type, pixels);
        }
    }

    private static void zeroReadPixelsFallback(int width, int height, int format, int type, ByteBuffer pixels) {
        GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels",
                "glReadPixels real Vulkan readback is not implemented yet; returning safe fallback data");
        if (pixels == null) {
            return;
        }

        int byteCount = Math.min(readPixelsByteCount(width, height, format, type), pixels.remaining());
        for (int i = 0; i < byteCount; i++) {
            pixels.put(pixels.position() + i, (byte) 0);
        }
    }

    private static boolean readPixelsFromFramebuffer(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        if (pixels == null) {
            return true;
        }
        if (pixels.remaining() < readPixelsByteCount(width, height, format, type)) {
            return false;
        }

        return readPixelsFromFramebuffer(x, y, width, height, format, type, MemoryUtil.memAddress(pixels));
    }

    private static boolean readPixelsFromFramebuffer(int x, int y, int width, int height, int format, int type, FloatBuffer pixels) {
        if (pixels == null) {
            return true;
        }
        if (pixels.remaining() < elementsForByteCount(readPixelsByteCount(width, height, format, type), Float.BYTES)) {
            return false;
        }

        return readPixelsFromFramebuffer(x, y, width, height, format, type, MemoryUtil.memAddress(pixels));
    }

    private static boolean readPixelsFromFramebuffer(int x, int y, int width, int height, int format, int type, long pixels) {
        if (pixels == 0L || width <= 0 || height <= 0) {
            return true;
        }
        if (!isSupportedReadPixelsFormat(format, type)) {
            return false;
        }

        GlFramebuffer.ReadPixelsImage readImage = GlFramebuffer.resolveReadPixelsImage(format);
        if (readImage == null) {
            return false;
        }

        try {
            if (format == GL11.GL_DEPTH_COMPONENT && type == GL11.GL_FLOAT) {
                return readDepthPixels(readImage, pixels, x, y, width, height);
            }
            ImageUtil.downloadTextureRegion(readImage.image(), pixels, x, y, width, height, readImage.aspectMask(), bytesPerPixel(format, type));
            return true;
        } catch (RuntimeException e) {
            GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels.downloadFailed",
                    "glReadPixels Vulkan readback failed for format 0x{} type 0x{}; returning safe fallback data",
                    Integer.toHexString(format), Integer.toHexString(type));
            return false;
        }
    }

    private static boolean readDepthPixels(GlFramebuffer.ReadPixelsImage readImage, long pixels,
                                           int x, int y, int width, int height) {
        VulkanImage image = readImage.image();
        boolean packedDepth = image.format == VK_FORMAT_D24_UNORM_S8_UINT
                || image.format == VK_FORMAT_X8_D24_UNORM_PACK32;
        if (!packedDepth && image.format != VK_FORMAT_D32_SFLOAT
                && image.format != VK_FORMAT_D32_SFLOAT_S8_UINT) {
            return false;
        }
        if (x < 0 || y < 0 || width > image.width || height > image.height
                || x > image.width - width || y > image.height - height) {
            return false;
        }


        boolean bottomUp = image.isRowsBottomUp();
        int sourceY = bottomUp ? y : image.height - y - height;
        ImageUtil.downloadTextureRegion(image, pixels, x, sourceY, width, height,
                readImage.aspectMask(), Float.BYTES);
        if (packedDepth) {
            long count = (long) width * height;
            for (long texel = 0; texel < count; texel++) {
                long address = pixels + texel * Float.BYTES;
                int depth = MemoryUtil.memGetInt(address) & 0x00ffffff;
                MemoryUtil.memPutFloat(address, depth / 16777215.0f);
            }
        }
        if (!bottomUp) {
            long rowBytes = (long) width * Float.BYTES;
            for (int row = 0; row < height / 2; row++) {
                long first = pixels + row * rowBytes;
                long last = pixels + (height - 1L - row) * rowBytes;
                for (int column = 0; column < width; column++) {
                    long offset = (long) column * Float.BYTES;
                    int value = MemoryUtil.memGetInt(first + offset);
                    MemoryUtil.memPutInt(first + offset, MemoryUtil.memGetInt(last + offset));
                    MemoryUtil.memPutInt(last + offset, value);
                }
            }
        }
        return true;
    }

    private static boolean isSupportedReadPixelsFormat(int format, int type) {
        return (format == GL11.GL_RGBA && type == GL11.GL_UNSIGNED_BYTE)
                || (format == GL11.GL_DEPTH_COMPONENT && type == GL11.GL_FLOAT);
    }

    private static void zeroReadPixelsFallback(int width, int height, int format, int type, IntBuffer pixels) {
        GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels",
                "glReadPixels real Vulkan readback is not implemented yet; returning safe fallback data");
        if (pixels == null) {
            return;
        }

        int elements = Math.min(elementsForByteCount(readPixelsByteCount(width, height, format, type), Integer.BYTES), pixels.remaining());
        for (int i = 0; i < elements; i++) {
            pixels.put(pixels.position() + i, 0);
        }
    }

    private static void zeroReadPixelsFallback(int width, int height, int format, int type, FloatBuffer pixels) {
        GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels",
                "glReadPixels real Vulkan readback is not implemented yet; returning safe fallback data");
        if (pixels == null) {
            return;
        }

        int elements = Math.min(elementsForByteCount(readPixelsByteCount(width, height, format, type), Float.BYTES), pixels.remaining());
        for (int i = 0; i < elements; i++) {
            pixels.put(pixels.position() + i, 0.0f);
        }
    }

    private static int readPixelsByteCount(int width, int height, int format, int type) {
        if (width <= 0 || height <= 0) {
            return 0;
        }

        long byteCount = (long) width * height * bytesPerPixel(format, type);
        return byteCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) byteCount;
    }

    private static int bytesPerPixel(int format, int type) {
        int packedSize = packedTypeBytes(type);
        if (packedSize > 0) {
            return packedSize;
        }

        return componentsForFormat(format) * bytesPerComponent(type);
    }

    private static int componentsForFormat(int format) {
        return switch (format) {
            case GL11.GL_RED, GL11.GL_GREEN, GL11.GL_BLUE, GL11.GL_ALPHA,
                 GL11.GL_LUMINANCE, GL11.GL_DEPTH_COMPONENT, GL11.GL_STENCIL_INDEX -> 1;
            case GL11.GL_LUMINANCE_ALPHA -> 2;
            case GL11.GL_RGB, GL12.GL_BGR -> 3;
            case GL11.GL_RGBA, GL12.GL_BGRA -> 4;
            default -> 4;
        };
    }

    private static int bytesPerComponent(int type) {
        return switch (type) {
            case GL11.GL_BYTE, GL11.GL_UNSIGNED_BYTE -> 1;
            case GL11.GL_SHORT, GL11.GL_UNSIGNED_SHORT, GL30.GL_HALF_FLOAT -> 2;
            case GL11.GL_INT, GL11.GL_UNSIGNED_INT, GL11.GL_FLOAT -> 4;
            default -> 1;
        };
    }

    private static int packedTypeBytes(int type) {
        return switch (type) {
            case GL12.GL_UNSIGNED_BYTE_3_3_2, GL12.GL_UNSIGNED_BYTE_2_3_3_REV -> 1;
            case GL12.GL_UNSIGNED_SHORT_5_6_5, GL12.GL_UNSIGNED_SHORT_5_6_5_REV,
                 GL12.GL_UNSIGNED_SHORT_4_4_4_4, GL12.GL_UNSIGNED_SHORT_4_4_4_4_REV,
                 GL12.GL_UNSIGNED_SHORT_5_5_5_1, GL12.GL_UNSIGNED_SHORT_1_5_5_5_REV -> 2;
            case GL12.GL_UNSIGNED_INT_8_8_8_8, GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                 GL12.GL_UNSIGNED_INT_10_10_10_2, GL12.GL_UNSIGNED_INT_2_10_10_10_REV -> 4;
            default -> 0;
        };
    }

    private static int elementsForByteCount(int byteCount, int elementSize) {
        if (byteCount <= 0) {
            return 0;
        }

        return (byteCount + elementSize - 1) / elementSize;
    }

}
