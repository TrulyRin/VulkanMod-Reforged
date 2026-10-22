package net.vulkanmod.compat.gl;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public final class GlPixelStore {
    public static final int GL_UNPACK_SWAP_BYTES = 0x0CF0;
    public static final int GL_UNPACK_ROW_LENGTH = 0x0CF2;
    public static final int GL_UNPACK_SKIP_ROWS = 0x0CF3;
    public static final int GL_UNPACK_SKIP_PIXELS = 0x0CF4;
    public static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    public static final int GL_PACK_SWAP_BYTES = 0x0D00;
    public static final int GL_PACK_ROW_LENGTH = 0x0D02;
    public static final int GL_PACK_SKIP_ROWS = 0x0D03;
    public static final int GL_PACK_SKIP_PIXELS = 0x0D04;
    public static final int GL_PACK_ALIGNMENT = 0x0D05;

    private static final Int2IntOpenHashMap values = new Int2IntOpenHashMap();

    static {
        values.put(GL_UNPACK_ALIGNMENT, 4);
        values.put(GL_PACK_ALIGNMENT, 4);
    }

    private GlPixelStore() {
    }

    public static boolean isPixelStoreParameter(int pname) {
        return (pname >= GL_UNPACK_SWAP_BYTES && pname <= GL_UNPACK_ALIGNMENT)
                || (pname >= GL_PACK_SWAP_BYTES && pname <= GL_PACK_ALIGNMENT);
    }

    public static void setInteger(int pname, int value) {
        values.put(pname, value);
    }

    public static int getInteger(int pname) {
        return values.getOrDefault(pname, 0);
    }
}
