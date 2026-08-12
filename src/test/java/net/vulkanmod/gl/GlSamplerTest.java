package net.vulkanmod.gl;

import net.vulkanmod.vulkan.texture.SamplerManager;
import org.junit.jupiter.api.Test;

import java.nio.IntBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;

class GlSamplerTest {
    @Test
    void generationBindingAndDeletionAreContextFree() {
        IntBuffer ids = IntBuffer.allocate(2);
        GlSampler.genSamplers(ids);
        int first = ids.get(0);
        int second = ids.get(1);

        assertTrue(GlSampler.isSampler(first));
        assertTrue(GlSampler.isSampler(second));
        GlSampler.bindSampler(3, first);
        assertEquals(first, GlSampler.getBoundSampler(3));
        GlSampler.deleteSamplers(first);
        assertFalse(GlSampler.isSampler(first));
        assertEquals(0, GlSampler.getBoundSampler(3));
    }

    @Test
    void samplerParametersProduceDescriptorVisibleImmutableState() {
        int sampler = GlSampler.genSamplers();
        GlSampler.samplerParameteri(sampler, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        GlSampler.samplerParameteri(sampler, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        GlSampler.samplerParameteri(sampler, GL_TEXTURE_WRAP_S, 0x812F);
        GlSampler.samplerParameterf(sampler, 0x813A, 1.0f);
        GlSampler.samplerParameterf(sampler, 0x813B, 1000.0f);

        SamplerManager.SamplerKey key = GlSampler.samplerKeyForTest(sampler, 5);
        assertNotNull(key);
        assertEquals(GL_LINEAR_MIPMAP_LINEAR, key.minFilter());
        assertEquals(GL_NEAREST, key.magFilter());
        assertEquals(0x812F, key.wrapS());
        assertEquals(1.0f, key.minLod());
        assertEquals(4.0f, key.maxLod());
    }
}
