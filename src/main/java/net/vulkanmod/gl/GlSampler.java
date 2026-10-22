package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.gl.TextureUnitLimits;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VulkanImage;

import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;

public final class GlSampler {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlSampler> map = new Int2ReferenceOpenHashMap<>();
    private static final Int2IntOpenHashMap boundByUnit = new Int2IntOpenHashMap();
    private static final Set<Integer> invalidSamplerWarnings = new HashSet<>();
    private static final int[] EMPTY_BINDINGS = new int[0];
    private static final GlSampler[] EMPTY_OWNERS = new GlSampler[0];


    public static BindingSnapshot captureBindings() {
        int count=boundByUnit.size();
        if(count==0) return new BindingSnapshot(EMPTY_BINDINGS,EMPTY_BINDINGS,EMPTY_OWNERS);
        int[] units=new int[count],names=new int[count];
        GlSampler[] owners=new GlSampler[count];
        int index=0;
        for(var entry:boundByUnit.int2IntEntrySet()) {
            units[index]=entry.getIntKey(); names[index]=entry.getIntValue(); owners[index]=map.get(names[index]);
            index++;
        }
        return new BindingSnapshot(units,names,owners);
    }

    public static final class BindingSnapshot {
        private final int[] units,names;
        private final GlSampler[] owners;
        private boolean restored;

        private BindingSnapshot(int[] units,int[] names,GlSampler[] owners) {
            this.units=units; this.names=names; this.owners=owners;
        }


        public void restore() {
            if(restored) return;
            restored=true;
            boundByUnit.clear();
            for(int index=0;index<units.length;index++) {
                if(owners[index]!=null && map.get(names[index])==owners[index]) boundByUnit.put(units[index],names[index]);
            }
        }
    }

    private static final int GL_TEXTURE_WRAP_R = 0x8072;
    private static final int GL_TEXTURE_MIN_LOD = 0x813A;
    private static final int GL_TEXTURE_MAX_LOD = 0x813B;
    private static final int GL_TEXTURE_LOD_BIAS = 0x8501;
    private static final int GL_TEXTURE_COMPARE_MODE = 0x884C;
    private static final int GL_TEXTURE_COMPARE_FUNC = 0x884D;

    private GlSampler() {
        intParams.put(GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        intParams.put(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        intParams.put(GL_TEXTURE_WRAP_S, GL_REPEAT);
        intParams.put(GL_TEXTURE_WRAP_T, GL_REPEAT);
        intParams.put(GL_TEXTURE_WRAP_R, GL_REPEAT);
        intParams.put(GL_TEXTURE_COMPARE_MODE, GL_NONE);
        intParams.put(GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
        floatParams.put(GL_TEXTURE_MIN_LOD, -1000.0f);
        floatParams.put(GL_TEXTURE_MAX_LOD, 1000.0f);
        floatParams.put(GL_TEXTURE_LOD_BIAS, 0.0f);
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
        var iterator = boundByUnit.int2IntEntrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getIntValue() == id) {
                iterator.remove();
            }
        }
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
        if (!TextureUnitLimits.isValid(unit)) {
            return;
        }
        if (sampler == 0) {
            boundByUnit.remove(unit);
        } else if (map.containsKey(sampler)) {
            boundByUnit.put(unit, sampler);
        } else if (invalidSamplerWarnings.add(sampler)) {
            Initializer.LOGGER.warn("Ignoring bind of unknown OpenGL sampler {} on texture unit {}",
                    sampler, unit);
        }
    }

    public static int getBoundSampler(int unit) {
        return boundByUnit.getOrDefault(unit, 0);
    }

    public static void samplerParameteri(int sampler, int pname, int param) {
        GlSampler glSampler = map.get(sampler);
        if (glSampler != null) {
            if (isFloatParameter(pname)) {
                glSampler.floatParams.put(pname, param);
            } else {
                glSampler.intParams.put(pname, param);
            }
        }
    }

    public static void samplerParameterf(int sampler, int pname, float param) {
        GlSampler glSampler = map.get(sampler);
        if (glSampler != null) {
            if (isFloatParameter(pname)) {
                glSampler.floatParams.put(pname, param);
            } else {
                glSampler.intParams.put(pname, (int) param);
            }
        }
    }

    public static int getSamplerParameteri(int sampler, int pname) {
        GlSampler glSampler = map.get(sampler);
        if (glSampler == null) {
            return 0;
        }
        if (glSampler.intParams.containsKey(pname)) {
            return glSampler.intParams.get(pname);
        }
        return (int) glSampler.floatParams.getOrDefault(pname, 0.0f);
    }

    public static float getSamplerParameterf(int sampler, int pname) {
        GlSampler glSampler = map.get(sampler);
        if (glSampler == null) {
            return 0.0f;
        }
        if (glSampler.floatParams.containsKey(pname)) {
            return glSampler.floatParams.get(pname);
        }
        return glSampler.intParams.getOrDefault(pname, 0);
    }

    public static long resolveSampler(int textureUnit, VulkanImage image) {
        int samplerId = getBoundSampler(textureUnit);
        if (samplerId == 0) {
            return image.getSampler();
        }

        GlSampler sampler = map.get(samplerId);
        if (sampler == null) {
            boundByUnit.remove(textureUnit);
            return image.getSampler();
        }

        return SamplerManager.getTextureSampler(sampler.toSamplerKey(image.mipLevels));
    }

    SamplerManager.SamplerKey toSamplerKey(int mipLevels) {
        float availableMaxLod = Math.max(0, mipLevels - 1);
        float minLod = Math.max(0.0f, getFloatParam(GL_TEXTURE_MIN_LOD, 0.0f));
        float maxLod = Math.min(availableMaxLod, getFloatParam(GL_TEXTURE_MAX_LOD, availableMaxLod));
        if (maxLod < minLod) {
            minLod = maxLod;
        }
        return new SamplerManager.SamplerKey(
                intParams.getOrDefault(GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR),
                intParams.getOrDefault(GL_TEXTURE_MAG_FILTER, GL_LINEAR),
                intParams.getOrDefault(GL_TEXTURE_WRAP_S, GL_REPEAT),
                intParams.getOrDefault(GL_TEXTURE_WRAP_T, GL_REPEAT),
                intParams.getOrDefault(GL_TEXTURE_WRAP_R, GL_REPEAT),
                minLod,
                maxLod,
                getFloatParam(GL_TEXTURE_LOD_BIAS, 0.0f),
                intParams.getOrDefault(GL_TEXTURE_COMPARE_MODE, GL_NONE),
                intParams.getOrDefault(GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL));
    }

    static SamplerManager.SamplerKey samplerKeyForTest(int sampler, int mipLevels) {
        GlSampler value = map.get(sampler);
        return value == null ? null : value.toSamplerKey(mipLevels);
    }

    private float getFloatParam(int pname, float fallback) {
        if (floatParams.containsKey(pname)) {
            return floatParams.get(pname);
        }
        if (intParams.containsKey(pname)) {
            return intParams.get(pname);
        }
        return fallback;
    }

    private static boolean isFloatParameter(int pname) {
        return pname == GL_TEXTURE_MIN_LOD
                || pname == GL_TEXTURE_MAX_LOD
                || pname == GL_TEXTURE_LOD_BIAS;
    }

    private final Int2IntOpenHashMap intParams = new Int2IntOpenHashMap();
    private final Int2FloatOpenHashMap floatParams = new Int2FloatOpenHashMap();
}
