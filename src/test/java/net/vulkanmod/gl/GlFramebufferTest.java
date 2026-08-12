package net.vulkanmod.gl;

import net.vulkanmod.compat.gl.GlIntegerState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import sun.misc.Unsafe;

import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.pass.MainPass;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;

class GlFramebufferTest {
    @AfterEach
    void resetFramebufferBinding() throws ReflectiveOperationException {
        GlFramebuffer.resetBoundFramebuffer();
        setStaticField(Renderer.class, "INSTANCE", null);
    }

    @Test
    void reportsSeparateReadAndDrawFramebufferBindings() {
        int drawFramebuffer = GlFramebuffer.genFramebufferId();
        int readFramebuffer = GlFramebuffer.genFramebufferId();

        GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);

        assertEquals(drawFramebuffer, GlIntegerState.getInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
        assertEquals(drawFramebuffer, GlIntegerState.getInteger(GL30.GL_FRAMEBUFFER_BINDING));
        assertEquals(readFramebuffer, GlIntegerState.getInteger(GL30.GL_READ_FRAMEBUFFER_BINDING));
    }

    @Test
    void namedFramebufferTextureDoesNotChangeCurrentBindings() {
        int drawFramebuffer = GlFramebuffer.genFramebufferId();
        int readFramebuffer = GlFramebuffer.genFramebufferId();
        int namedFramebuffer = GlFramebuffer.genFramebufferId();

        GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);

        GlFramebuffer.namedFramebufferTexture(namedFramebuffer, GL30.GL_COLOR_ATTACHMENT0, 0, 0);

        assertEquals(drawFramebuffer, GlIntegerState.getInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
        assertEquals(readFramebuffer, GlIntegerState.getInteger(GL30.GL_READ_FRAMEBUFFER_BINDING));
    }

    @Test
    void reportsNamedFramebufferAttachmentParametersWithoutBindingFramebuffer() {
        int drawFramebuffer = GlFramebuffer.genFramebufferId();
        int namedFramebuffer = GlFramebuffer.genFramebufferId();

        GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GlFramebuffer.namedFramebufferTexture(namedFramebuffer, GL30.GL_COLOR_ATTACHMENT0, 303, 0);

        assertEquals(GL11.GL_TEXTURE, GlFramebuffer.getNamedFramebufferAttachmentParameteri(
                namedFramebuffer,
                GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        ));
        assertEquals(303, GlFramebuffer.getNamedFramebufferAttachmentParameteri(
                namedFramebuffer,
                GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
        ));
        assertEquals(drawFramebuffer, GlIntegerState.getInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
    }

    @Test
    void tracksReadAndDrawFramebufferAttachmentsSeparately() {
        int drawFramebuffer = GlFramebuffer.genFramebufferId();
        int readFramebuffer = GlFramebuffer.genFramebufferId();

        GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);

        GlFramebuffer.framebufferTexture2D(
                GL30.GL_DRAW_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL11.GL_TEXTURE_2D,
                101,
                0
        );
        GlFramebuffer.framebufferTexture2D(
                GL30.GL_READ_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL11.GL_TEXTURE_2D,
                202,
                0
        );

        assertEquals(101, GlFramebuffer.getFramebufferAttachmentParameteri(
                GL30.GL_DRAW_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
        ));
        assertEquals(GL11.GL_NONE, GlFramebuffer.getFramebufferAttachmentParameteri(
                GL30.GL_READ_FRAMEBUFFER,
                GL30.GL_COLOR_ATTACHMENT0,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE
        ));
        assertEquals(202, GlFramebuffer.getFramebufferAttachmentParameteri(
                GL30.GL_READ_FRAMEBUFFER,
                GL30.GL_DEPTH_ATTACHMENT,
                GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME
        ));
    }

    @Test
    void resolvesColorBlitImagesFromReadToDrawFramebuffer() throws ReflectiveOperationException {
        VulkanImage source = vulkanImageMetadata(
                VK_FORMAT_R8G8B8A8_UNORM,
                128,
                64,
                VK_IMAGE_ASPECT_COLOR_BIT,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
        );
        VulkanImage destination = vulkanImageMetadata(
                VK_FORMAT_R8G8B8A8_UNORM,
                128,
                64,
                VK_IMAGE_ASPECT_COLOR_BIT,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
        );

        attachReadDrawTextures(GL30.GL_COLOR_ATTACHMENT0, source, destination);

        GlFramebuffer.BlitImages images = GlFramebuffer.resolveBlitImages(GL11.GL_COLOR_BUFFER_BIT);

        assertSame(source, images.source());
        assertSame(destination, images.destination());
        assertEquals(VK_IMAGE_ASPECT_COLOR_BIT, images.aspectMask());
    }

    @Test
    void resolvesNamedColorBlitImagesWithoutChangingCurrentBindings() throws ReflectiveOperationException {
        VulkanImage source = vulkanImageMetadata(
                VK_FORMAT_R8G8B8A8_UNORM,
                128,
                64,
                VK_IMAGE_ASPECT_COLOR_BIT,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
        );
        VulkanImage destination = vulkanImageMetadata(
                VK_FORMAT_R8G8B8A8_UNORM,
                128,
                64,
                VK_IMAGE_ASPECT_COLOR_BIT,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
        );

        int boundReadFramebuffer = GlFramebuffer.genFramebufferId();
        int boundDrawFramebuffer = GlFramebuffer.genFramebufferId();
        int readFramebuffer = GlFramebuffer.genFramebufferId();
        int drawFramebuffer = GlFramebuffer.genFramebufferId();

        GlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, boundReadFramebuffer);
        GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, boundDrawFramebuffer);

        GlFramebuffer.getFramebuffer(readFramebuffer).colorAttachment = source;
        GlFramebuffer.getFramebuffer(drawFramebuffer).colorAttachment = destination;

        GlFramebuffer.BlitImages images = GlFramebuffer.resolveNamedBlitImages(readFramebuffer, drawFramebuffer, GL11.GL_COLOR_BUFFER_BIT);

        assertSame(source, images.source());
        assertSame(destination, images.destination());
        assertEquals(VK_IMAGE_ASPECT_COLOR_BIT, images.aspectMask());
        assertEquals(boundReadFramebuffer, GlIntegerState.getInteger(GL30.GL_READ_FRAMEBUFFER_BINDING));
        assertEquals(boundDrawFramebuffer, GlIntegerState.getInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
    }

    @Test
    void resolvesDepthBlitImagesFromReadToDrawFramebuffer() throws ReflectiveOperationException {
        VulkanImage source = vulkanImageMetadata(
                VK_FORMAT_D32_SFLOAT,
                128,
                64,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
        );
        VulkanImage destination = vulkanImageMetadata(
                VK_FORMAT_D32_SFLOAT,
                128,
                64,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
        );

        attachReadDrawTextures(GL30.GL_DEPTH_ATTACHMENT, source, destination);

        GlFramebuffer.BlitImages images = GlFramebuffer.resolveBlitImages(GL11.GL_DEPTH_BUFFER_BIT);

        assertSame(source, images.source());
        assertSame(destination, images.destination());
        assertEquals(VK_IMAGE_ASPECT_DEPTH_BIT, images.aspectMask());
    }

    @Test
    void resolvesDefaultReadFramebufferDepthBlitForGuiPreviewFbos() throws ReflectiveOperationException {
        VulkanImage source = vulkanImageMetadata(
                VK_FORMAT_D32_SFLOAT,
                1920,
                1009,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
        );
        VulkanImage destination = vulkanImageMetadata(
                VK_FORMAT_D32_SFLOAT,
                1920,
                1009,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
        );
        Renderer renderer = (Renderer) unsafe().allocateInstance(Renderer.class);
        renderer.setMainPass(new MainPass() {
            @Override
            public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
            }

            @Override
            public void end(VkCommandBuffer commandBuffer) {
            }

            @Override
            public VulkanImage getDepthAttachment() {
                return source;
            }
        });
        setStaticField(Renderer.class, "INSTANCE", renderer);

        int drawFramebuffer = GlFramebuffer.genFramebufferId();
        GlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
        GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GlFramebuffer.getFramebuffer(drawFramebuffer).depthAttachment = destination;

        GlFramebuffer.BlitImages images = GlFramebuffer.resolveBlitImages(GL11.GL_DEPTH_BUFFER_BIT);

        assertSame(source, images.source());
        assertSame(destination, images.destination());
        assertEquals(VK_IMAGE_ASPECT_DEPTH_BIT, images.aspectMask());
    }

    @Test
    void resolvesColorReadPixelsImageFromReadFramebuffer() throws ReflectiveOperationException {
        VulkanImage source = vulkanImageMetadata(
                VK_FORMAT_R8G8B8A8_UNORM,
                128,
                64,
                VK_IMAGE_ASPECT_COLOR_BIT,
                VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
        );

        attachReadDrawTextures(GL30.GL_COLOR_ATTACHMENT0, source, source);

        GlFramebuffer.ReadPixelsImage image = GlFramebuffer.resolveReadPixelsImage(GL11.GL_RGBA);

        assertSame(source, image.image());
        assertEquals(VK_IMAGE_ASPECT_COLOR_BIT, image.aspectMask());
    }

    @Test
    void resolvesDepthReadPixelsImageFromReadFramebuffer() throws ReflectiveOperationException {
        VulkanImage source = vulkanImageMetadata(
                VK_FORMAT_D32_SFLOAT,
                128,
                64,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT
        );

        attachReadDrawTextures(GL30.GL_DEPTH_ATTACHMENT, source, source);

        GlFramebuffer.ReadPixelsImage image = GlFramebuffer.resolveReadPixelsImage(GL11.GL_DEPTH_COMPONENT);

        assertSame(source, image.image());
        assertEquals(VK_IMAGE_ASPECT_DEPTH_BIT, image.aspectMask());
    }

    private static void attachReadDrawTextures(int attachment, VulkanImage source, VulkanImage destination) {
        int readFramebuffer = GlFramebuffer.genFramebufferId();
        int drawFramebuffer = GlFramebuffer.genFramebufferId();

        GlFramebuffer.bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        GlFramebuffer.bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);

        GlFramebuffer read = GlFramebuffer.getFramebuffer(readFramebuffer);
        GlFramebuffer draw = GlFramebuffer.getFramebuffer(drawFramebuffer);

        if (attachment == GL30.GL_COLOR_ATTACHMENT0) {
            read.colorAttachment = source;
            draw.colorAttachment = destination;
        } else {
            read.depthAttachment = source;
            draw.depthAttachment = destination;
        }
    }

    private static VulkanImage vulkanImageMetadata(int format, int width, int height, int aspect, int usage) throws ReflectiveOperationException {
        VulkanImage image = (VulkanImage) unsafe().allocateInstance(VulkanImage.class);
        setField(image, "format", format);
        setField(image, "mipLevels", 1);
        setField(image, "width", width);
        setField(image, "height", height);
        setField(image, "formatSize", 4);
        setField(image, "usage", usage);
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

    private static void setStaticField(Class<?> target, String name, Object value) throws ReflectiveOperationException {
        Field field = target.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
