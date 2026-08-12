package net.vulkanmod.vulkan.framebuffer;

import net.vulkanmod.vulkan.texture.VulkanImage;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;

class FramebufferTest {
    @Test
    void buildsFramebufferForDepthOnlyAttachment() throws ReflectiveOperationException {
        VulkanImage depth = vulkanImageMetadata(
                VK_FORMAT_D32_SFLOAT,
                64,
                32,
                VK_IMAGE_ASPECT_DEPTH_BIT
        );

        Framebuffer framebuffer = Framebuffer.builder(null, depth).build();

        assertFalse(framebuffer.hasColorAttachment);
        assertTrue(framebuffer.hasDepthAttachment);
        assertSame(depth, framebuffer.getDepthAttachment());
        assertNull(framebuffer.getColorAttachment());
        assertTrue((depth.aspect & VK_IMAGE_ASPECT_DEPTH_BIT) != 0);
    }

    private static VulkanImage vulkanImageMetadata(int format, int width, int height, int aspect) throws ReflectiveOperationException {
        VulkanImage image = (VulkanImage) unsafe().allocateInstance(VulkanImage.class);
        setField(image, "format", format);
        setField(image, "mipLevels", 1);
        setField(image, "width", width);
        setField(image, "height", height);
        setField(image, "formatSize", Float.BYTES);
        setField(image, "usage", VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        setField(image, "aspect", aspect);
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
