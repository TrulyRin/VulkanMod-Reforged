package net.vulkanmod.render.model.quad;

import java.util.concurrent.ConcurrentHashMap;

public class BakedQuadDeduplicator {

    private static final ConcurrentHashMap<IntArrayKey, int[]> VERTICES_CACHE = new ConcurrentHashMap<>();

    public static int[] deduplicateVertices(int[] vertices) {
        if (vertices == null) return null;
        return VERTICES_CACHE.computeIfAbsent(new IntArrayKey(vertices), key -> key.array);
    }

    private static class IntArrayKey {
        private final int[] array;
        private final int hash;

        public IntArrayKey(int[] array) {
            this.array = array;
            this.hash = java.util.Arrays.hashCode(array);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IntArrayKey)) return false;
            return java.util.Arrays.equals(this.array, ((IntArrayKey) obj).array);
        }
    }
}
