package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.nio.IntBuffer;

public final class GlSampler {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlSampler> map = new Int2ReferenceOpenHashMap<>();
    private static final Int2IntOpenHashMap boundByUnit = new Int2IntOpenHashMap();

    private GlSampler() {
    }

    public static int genSamplers() {
        int id = ID_COUNTER++;
        map.put(id, new GlSampler());
        return id;
    }

    public static void genSamplers(IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) {
            ids.put(i, genSamplers());
        }
    }

    public static void deleteSamplers(int id) {
        map.remove(id);
    }

    public static void deleteSamplers(IntBuffer ids) {
        for (int i = ids.position(); i < ids.limit(); i++) {
            deleteSamplers(ids.get(i));
        }
    }

    public static boolean isSampler(int id) {
        return map.containsKey(id);
    }

    public static void bindSampler(int unit, int sampler) {
        if (sampler == 0) {
            boundByUnit.remove(unit);
        } else {
            boundByUnit.put(unit, sampler);
        }
    }

    public static int getBoundSampler(int unit) {
        return boundByUnit.getOrDefault(unit, 0);
    }

    public static void samplerParameteri(int sampler, int pname, int param) {
        GlSampler glSampler = map.get(sampler);
        if (glSampler != null) {
            glSampler.intParams.put(pname, param);
        }
    }

    public static void samplerParameterf(int sampler, int pname, float param) {
        GlSampler glSampler = map.get(sampler);
        if (glSampler != null) {
            glSampler.floatParams.put(pname, param);
        }
    }

    public static int getSamplerParameteri(int sampler, int pname) {
        GlSampler glSampler = map.get(sampler);
        return glSampler != null ? glSampler.intParams.getOrDefault(pname, 0) : 0;
    }

    public static float getSamplerParameterf(int sampler, int pname) {
        GlSampler glSampler = map.get(sampler);
        return glSampler != null ? glSampler.floatParams.getOrDefault(pname, 0.0f) : 0.0f;
    }

    private final Int2IntOpenHashMap intParams = new Int2IntOpenHashMap();
    private final Int2FloatOpenHashMap floatParams = new Int2FloatOpenHashMap();
}

