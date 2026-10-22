package net.vulkanmod.gl;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class CompressedTextureDecoder {
    private CompressedTextureDecoder() {
    }

    private static final int DXT1_RGB = 0x83F0;
    private static final int DXT1_RGBA = 0x83F1;
    private static final int DXT3 = 0x83F2;
    private static final int DXT5 = 0x83F3;
    private static final int SRGB_DXT1 = 0x8C4C;
    private static final int SRGB_A_DXT1 = 0x8C4D;
    private static final int SRGB_A_DXT3 = 0x8C4E;
    private static final int SRGB_A_DXT5 = 0x8C4F;

    public static boolean isSupported(int glInternalFormat) {
        return switch (glInternalFormat) {
            case DXT1_RGB, DXT1_RGBA, DXT3, DXT5, SRGB_DXT1, SRGB_A_DXT1, SRGB_A_DXT3, SRGB_A_DXT5 -> true;
            default -> false;
        };
    }

    public static ByteBuffer decodeToRgba(int format, int width, int height, ByteBuffer data) {
        if (!isSupported(format) || data == null || width <= 0 || height <= 0) {
            return null;
        }

        boolean dxt1 = format == DXT1_RGB || format == DXT1_RGBA || format == SRGB_DXT1 || format == SRGB_A_DXT1;
        boolean explicitAlpha = format == DXT3 || format == SRGB_A_DXT3;
        boolean interpAlpha = format == DXT5 || format == SRGB_A_DXT5;
        boolean punchThrough = format == DXT1_RGBA || format == SRGB_A_DXT1;
        int blockBytes = dxt1 ? 8 : 16;

        int blocksWide = (width + 3) / 4;
        int blocksHigh = (height + 3) / 4;
        int needed = blocksWide * blocksHigh * blockBytes;
        if (data.remaining() < needed) {
            return null;
        }

        byte[] src = new byte[needed];
        data.get(data.position(), src, 0, needed);

        ByteBuffer out = MemoryUtil.memAlloc(width * height * 4);

        int[] cr = new int[4], cg = new int[4], cb = new int[4], ca = new int[4];
        int[] alpha = new int[16];

        for (int by = 0; by < blocksHigh; by++) {
            for (int bx = 0; bx < blocksWide; bx++) {
                int blockOff = (by * blocksWide + bx) * blockBytes;
                int colorOff = dxt1 ? blockOff : blockOff + 8;

                if (explicitAlpha) {
                    for (int i = 0; i < 16; i++) {
                        int b = src[blockOff + (i >> 1)] & 0xFF;
                        int nib = (i & 1) == 0 ? (b & 0x0F) : (b >> 4);
                        alpha[i] = nib * 17;
                    }
                } else if (interpAlpha) {
                    decodeInterpolatedAlpha(src, blockOff, alpha);
                }

                int c0 = (src[colorOff] & 0xFF) | ((src[colorOff + 1] & 0xFF) << 8);
                int c1 = (src[colorOff + 2] & 0xFF) | ((src[colorOff + 3] & 0xFF) << 8);
                cr[0] = ex5(c0 >> 11); cg[0] = ex6(c0 >> 5); cb[0] = ex5(c0); ca[0] = 255;
                cr[1] = ex5(c1 >> 11); cg[1] = ex6(c1 >> 5); cb[1] = ex5(c1); ca[1] = 255;

                boolean fourColor = !dxt1 || c0 > c1;
                if (fourColor) {
                    cr[2] = (2 * cr[0] + cr[1]) / 3; cg[2] = (2 * cg[0] + cg[1]) / 3; cb[2] = (2 * cb[0] + cb[1]) / 3; ca[2] = 255;
                    cr[3] = (cr[0] + 2 * cr[1]) / 3; cg[3] = (cg[0] + 2 * cg[1]) / 3; cb[3] = (cb[0] + 2 * cb[1]) / 3; ca[3] = 255;
                } else {
                    cr[2] = (cr[0] + cr[1]) / 2; cg[2] = (cg[0] + cg[1]) / 2; cb[2] = (cb[0] + cb[1]) / 2; ca[2] = 255;
                    cr[3] = 0; cg[3] = 0; cb[3] = 0; ca[3] = punchThrough ? 0 : 255;
                }

                int idx = (src[colorOff + 4] & 0xFF) | ((src[colorOff + 5] & 0xFF) << 8)
                        | ((src[colorOff + 6] & 0xFF) << 16) | ((src[colorOff + 7] & 0xFF) << 24);

                for (int ty = 0; ty < 4; ty++) {
                    int py = by * 4 + ty;
                    if (py >= height) {
                        continue;
                    }
                    for (int tx = 0; tx < 4; tx++) {
                        int px = bx * 4 + tx;
                        if (px >= width) {
                            continue;
                        }
                        int t = ty * 4 + tx;
                        int ci = (idx >> (2 * t)) & 0x3;
                        int a = (explicitAlpha || interpAlpha) ? alpha[t] : ca[ci];
                        int o = (py * width + px) * 4;
                        out.put(o, (byte) cr[ci]);
                        out.put(o + 1, (byte) cg[ci]);
                        out.put(o + 2, (byte) cb[ci]);
                        out.put(o + 3, (byte) a);
                    }
                }
            }
        }

        return out.position(0);
    }

    private static void decodeInterpolatedAlpha(byte[] src, int blockOff, int[] alpha) {
        int a0 = src[blockOff] & 0xFF, a1 = src[blockOff + 1] & 0xFF;
        int[] av = new int[8];
        av[0] = a0; av[1] = a1;
        if (a0 > a1) {
            for (int k = 1; k <= 6; k++) {
                av[k + 1] = ((7 - k) * a0 + k * a1) / 7;
            }
        } else {
            for (int k = 1; k <= 4; k++) {
                av[k + 1] = ((5 - k) * a0 + k * a1) / 5;
            }
            av[6] = 0;
            av[7] = 255;
        }
        long bits = 0;
        for (int k = 0; k < 6; k++) {
            bits |= ((long) (src[blockOff + 2 + k] & 0xFF)) << (8 * k);
        }
        for (int i = 0; i < 16; i++) {
            alpha[i] = av[(int) ((bits >> (3 * i)) & 0x7)];
        }
    }

    private static int ex5(int x) {
        int v = x & 0x1F;
        return (v << 3) | (v >> 2);
    }

    private static int ex6(int x) {
        int v = x & 0x3F;
        return (v << 2) | (v >> 4);
    }
}
