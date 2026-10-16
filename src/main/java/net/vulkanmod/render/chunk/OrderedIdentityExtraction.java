package net.vulkanmod.render.chunk;

import java.util.ArrayList;
import java.util.IdentityHashMap;

   
                                                                           
                                                                                  
   
final class OrderedIdentityExtraction<T> {
    private Scratch<T> first;
    private int createdScratchCount;

    Scratch<T> acquire() {
        if (first == null) first = createScratch();
        Scratch<T> scratch = first;
        while (scratch.inUse) {
            if (scratch.next == null) scratch.next = createScratch();
            scratch = scratch.next;
        }
        scratch.inUse = true;
        return scratch;
    }

    private Scratch<T> createScratch() {
        createdScratchCount++;
        return new Scratch<>();
    }

    int createdScratchCount() {
        return createdScratchCount;
    }

    static final class Scratch<T> implements AutoCloseable {
        private final IdentityHashMap<T, Boolean> globals = new IdentityHashMap<>();
        private final ArrayList<T> ordered = new ArrayList<>();
        private Scratch<T> next;
        private boolean inUse;

        void addLocal(T entity) {
            if (!globals.containsKey(entity)) {
                globals.put(entity, Boolean.FALSE);
                ordered.add(entity);
            }
        }

        void addGlobal(T entity) {
            if (globals.put(entity, Boolean.TRUE) == null) ordered.add(entity);
        }

        int size() {
            return ordered.size();
        }

        T get(int index) {
            return ordered.get(index);
        }

        boolean isGlobal(T entity) {
            return globals.get(entity) == Boolean.TRUE;
        }

        @Override
        public void close() {
            globals.clear();
            ordered.clear();
            inUse = false;
        }
    }
}
