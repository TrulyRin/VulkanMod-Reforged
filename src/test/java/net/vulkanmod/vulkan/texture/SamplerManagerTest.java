package net.vulkanmod.vulkan.texture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;

class SamplerManagerTest {
    @Test
    void translatesOpenGlFilteringWithoutCreatingNativeObjects() {
        assertEquals(VK_FILTER_NEAREST, SamplerManager.toVkFilter(GL_NEAREST));
        assertEquals(VK_FILTER_LINEAR, SamplerManager.toVkFilter(GL_LINEAR_MIPMAP_LINEAR));
        assertEquals(VK_SAMPLER_MIPMAP_MODE_NEAREST,
                SamplerManager.toVkMipmapMode(GL_LINEAR_MIPMAP_NEAREST));
        assertEquals(VK_SAMPLER_MIPMAP_MODE_LINEAR,
                SamplerManager.toVkMipmapMode(GL_NEAREST_MIPMAP_LINEAR));
        assertFalse(SamplerManager.usesMipmaps(GL_LINEAR));
        assertTrue(SamplerManager.usesMipmaps(GL_LINEAR_MIPMAP_LINEAR));
    }

    @Test
    void translatesWrapAndCompareModesConservatively() {
        assertEquals(VK_SAMPLER_ADDRESS_MODE_REPEAT, SamplerManager.toVkAddressMode(GL_REPEAT));
        assertEquals(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE, SamplerManager.toVkAddressMode(0x812F));
        assertEquals(VK_SAMPLER_ADDRESS_MODE_MIRRORED_REPEAT, SamplerManager.toVkAddressMode(0x8370));
        assertEquals(VK_COMPARE_OP_LESS_OR_EQUAL, SamplerManager.toVkCompareOp(GL_LEQUAL));
        assertEquals(VK_COMPARE_OP_ALWAYS, SamplerManager.toVkCompareOp(GL_ALWAYS));
    }
}
