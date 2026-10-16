package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.nio.IntBuffer;

public final class GlVertexArray {
    private static int nextId = 1;
    private static int boundId;
    private static final IntOpenHashSet ARRAYS = new IntOpenHashSet();
    private static final Int2ObjectOpenHashMap<Int2IntOpenHashMap> ATTRIBUTE_DIVISORS = new Int2ObjectOpenHashMap<>();

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
        ATTRIBUTE_DIVISORS.remove(id);
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

    public static void vertexAttribDivisor(int index, int divisor) {
        vertexArrayVertexAttribDivisor(boundId, index, divisor);
    }

    public static void vertexArrayVertexAttribDivisor(int array, int index, int divisor) {
        if (array != 0 && !ARRAYS.contains(array)) {
            return;
        }
        if (divisor == 0) {
            Int2IntOpenHashMap attributes = ATTRIBUTE_DIVISORS.get(array);
            if (attributes != null) {
                attributes.remove(index);
                if (attributes.isEmpty()) ATTRIBUTE_DIVISORS.remove(array);
            }
        } else {
            ATTRIBUTE_DIVISORS.computeIfAbsent(array, ignored -> new Int2IntOpenHashMap()).put(index, divisor);
        }
    }

    public static int getVertexAttribDivisor(int index) {
        Int2IntOpenHashMap attributes = ATTRIBUTE_DIVISORS.get(boundId);
        return attributes == null ? 0 : attributes.get(index);
    }

    public static int getVertexAttribi(int index, int pname) {
        return pname == 0x88FE                                      ? getVertexAttribDivisor(index) : 0;
    }
}
