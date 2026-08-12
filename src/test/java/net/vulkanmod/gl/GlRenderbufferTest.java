package net.vulkanmod.gl;

import net.vulkanmod.vulkan.texture.VulkanImage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL30;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;

class GlRenderbufferTest {
    private int renderbuffer;

    @AfterEach
    void resetBinding() {
        GlRenderbuffer.bindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        if (renderbuffer != 0) {
            GlRenderbuffer.deleteRenderbuffer(renderbuffer);
        }
    }

    @Test
    void reportsBoundRenderbufferStorageParameters() throws ReflectiveOperationException {
        renderbuffer = GlRenderbuffer.genId();
        GlRenderbuffer.bindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
        attachImageMetadata(renderbuffer, GL30.GL_RGBA8, 320, 180);

        assertEquals(320, GlRenderbuffer.getRenderbufferParameter(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_WIDTH));
        assertEquals(180, GlRenderbuffer.getRenderbufferParameter(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_HEIGHT));
        assertEquals(GL30.GL_RGBA8, GlRenderbuffer.getRenderbufferParameter(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_INTERNAL_FORMAT));
        assertEquals(8, GlRenderbuffer.getRenderbufferParameter(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_RED_SIZE));
        assertEquals(8, GlRenderbuffer.getRenderbufferParameter(GL30.GL_RENDERBUFFER, GL30.GL_RENDERBUFFER_ALPHA_SIZE));
    }

    @Test
    void reportsNamedRenderbufferStorageParametersWithoutChangingBinding() throws ReflectiveOperationException {
        int boundRenderbuffer = GlRenderbuffer.genId();
        renderbuffer = GlRenderbuffer.genId();

        GlRenderbuffer.bindRenderbuffer(GL30.GL_RENDERBUFFER, boundRenderbuffer);
        attachImageMetadata(renderbuffer, GL30.GL_RGBA8, 64, 32);

        assertEquals(64, GlRenderbuffer.getNamedRenderbufferParameter(renderbuffer, GL30.GL_RENDERBUFFER_WIDTH));
        assertEquals(32, GlRenderbuffer.getNamedRenderbufferParameter(renderbuffer, GL30.GL_RENDERBUFFER_HEIGHT));
        assertEquals(boundRenderbuffer, GlRenderbuffer.getBoundId());

        GlRenderbuffer.deleteRenderbuffer(boundRenderbuffer);
    }

    private static void attachImageMetadata(int id, int internalFormat, int width, int height) throws ReflectiveOperationException {
        GlRenderbuffer renderbuffer = GlRenderbuffer.getRenderbuffer(id);
        renderbuffer.internalFormat = internalFormat;
        renderbuffer.setVulkanImage(vulkanImageMetadata(width, height));
    }

    private static VulkanImage vulkanImageMetadata(int width, int height) throws ReflectiveOperationException {
        VulkanImage image = (VulkanImage) unsafe().allocateInstance(VulkanImage.class);
        setField(image, "format", VK_FORMAT_R8G8B8A8_UNORM);
        setField(image, "mipLevels", 1);
        setField(image, "width", width);
        setField(image, "height", height);
        setField(image, "formatSize", 4);
        setField(image, "usage", 0);
        setField(image, "aspect", VulkanImage.getAspect(VK_FORMAT_R8G8B8A8_UNORM));
        return image;
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
