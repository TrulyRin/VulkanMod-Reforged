package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.nio.IntBuffer;

public final class GlVertexArray {
    private static int nextId = 1;
    private static int boundId;
    private static final IntOpenHashSet ARRAYS = new IntOpenHashSet();

    private GlVertexArray() {
    }

    public static int genVertexArray() {
        int id = nextId++;
        ARRAYS.add(id);
        return id;
    }

    public static void genVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            arrays.put(i, genVertexArray());
        }
    }

    public static void bindVertexArray(int id) {
        if (id != 0 && !ARRAYS.contains(id)) {
            ARRAYS.add(id);
        }
        boundId = id;
    }

    public static void deleteVertexArray(int id) {
        ARRAYS.remove(id);
        if (boundId == id) {
            boundId = 0;
        }
    }

    public static void deleteVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            deleteVertexArray(arrays.get(i));
        }
    }

    public static boolean isVertexArray(int id) {
        return id == 0 || ARRAYS.contains(id);
    }

    public static int boundId() {
        return boundId;
    }
}

