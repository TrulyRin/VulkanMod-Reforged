package net.vulkanmod.compat.gl;

import static org.lwjgl.opengl.GL40C.GL_PATCH_VERTICES;


public final class GlPatchState {
    private static int patchVertices = 3;
    private static int maxPatchVertices;

    private GlPatchState() {}

    public static int getPatchVertices() { return patchVertices; }

    public static int getMaxPatchVertices() { return maxPatchVertices; }


    public static void configureLimit(int limit) {
        if (limit < 0 || limit > 0 && limit < 3) {
            throw new IllegalArgumentException("Patch limit must be zero or support the GL default of three vertices");
        }
        maxPatchVertices = limit;
        patchVertices = 3;
    }

    public static void patchParameteri(int pname, int value) {
        if (pname != GL_PATCH_VERTICES) throw new IllegalArgumentException("Integer patch parameter must be GL_PATCH_VERTICES");
        if (maxPatchVertices == 0) throw new IllegalArgumentException("Tessellation patch state has no enabled native device limit");
        if (value < 1 || value > maxPatchVertices) {
            throw new IllegalArgumentException("Patch vertex count must be between one and the enabled device limit");
        }
        patchVertices = value;
    }
}
