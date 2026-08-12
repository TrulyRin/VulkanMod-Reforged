package net.vulkanmod.gl;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlUtilTest {
    @Test
    void mapsLegacyDepthComponent32AsDepthTextureFormat() {
        assertEquals(
                GlUtil.vulkanFormat(GL30.GL_DEPTH_COMPONENT24, GL11.GL_FLOAT),
                GlUtil.vulkanFormat(GL14.GL_DEPTH_COMPONENT32, GL11.GL_FLOAT)
        );
    }

    @Test
    void mapsLegacyDepthComponent32AsDepthInternalFormat() {
        assertEquals(
                GlUtil.vulkanFormat(GL30.GL_DEPTH_COMPONENT24),
                GlUtil.vulkanFormat(GL14.GL_DEPTH_COMPONENT32)
        );
    }
}
