package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.nio.IntBuffer;

public final class GlQuery {
    public static final int GL_QUERY_COUNTER_BITS = 0x8864;
    public static final int GL_CURRENT_QUERY = 0x8865;
    public static final int GL_QUERY_RESULT = 0x8866;
    public static final int GL_QUERY_RESULT_AVAILABLE = 0x8867;

    public static final int GL_SAMPLES_PASSED = 0x8914;
    public static final int GL_ANY_SAMPLES_PASSED = 0x8C2F;
    public static final int GL_ANY_SAMPLES_PASSED_CONSERVATIVE = 0x8D6A;
    public static final int GL_PRIMITIVES_GENERATED = 0x8C87;
    public static final int GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN = 0x8C88;
    public static final int GL_TIME_ELAPSED = 0x88BF;
    public static final int GL_TIMESTAMP = 0x8E28;

    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlQuery> map = new Int2ReferenceOpenHashMap<>();
    private static final Int2IntOpenHashMap activeQueryByTarget = new Int2IntOpenHashMap();

    private GlQuery() {
    }

    public static int genQueries() {
        int id = ID_COUNTER++;
        map.put(id, new GlQuery());
        return id;
    }

    public static void genQueries(IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) {
            ids.put(i, genQueries());
        }
    }

    public static void deleteQueries(int id) {
        map.remove(id);
    }

    public static void deleteQueries(IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) {
            deleteQueries(ids.get(i));
        }
    }

    public static boolean isQuery(int id) {
        return map.containsKey(id);
    }

    public static void beginQuery(int target, int id) {
        GlQuery query = map.get(id);
        if (query == null) {
            query = new GlQuery();
            map.put(id, query);
        }

        query.target = target;
        query.startNanos = System.nanoTime();
        activeQueryByTarget.put(target, id);
    }

    public static void endQuery(int target) {
        int id = activeQueryByTarget.getOrDefault(target, 0);
        activeQueryByTarget.remove(target);

        GlQuery query = map.get(id);
        if (query == null) {
            return;
        }

        query.result = switch (target) {
            case GL_SAMPLES_PASSED, GL_ANY_SAMPLES_PASSED, GL_ANY_SAMPLES_PASSED_CONSERVATIVE -> 1L;
            case GL_TIME_ELAPSED -> System.nanoTime() - query.startNanos;
            default -> 0L;
        };
    }

    public static void queryCounter(int id, int target) {
        GlQuery query = map.get(id);
        if (query == null) {
            query = new GlQuery();
            map.put(id, query);
        }

        query.target = target;
        query.result = System.nanoTime();
    }

    public static int getQueryi(int target, int pname) {
        return switch (pname) {
            case GL_CURRENT_QUERY -> activeQueryByTarget.getOrDefault(target, 0);
            case GL_QUERY_COUNTER_BITS -> 32;
            default -> 0;
        };
    }

    public static long getQueryObject(int id, int pname) {
        if (pname == GL_QUERY_RESULT_AVAILABLE) {
            return 1L;
        }

        GlQuery query = map.get(id);
        return query != null ? query.result : 0L;
    }

    private int target;
    private long startNanos;
    private long result;
}
