package net.vulkanmod.vulkan.pass;

import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface MainPass {

    void begin(VkCommandBuffer commandBuffer, MemoryStack stack);

    void end(VkCommandBuffer commandBuffer);

    default void mainTargetBindWrite() {}

    default void mainTargetUnbindWrite() {}

    default void rebindMainTarget() {}

    default void bindAsTexture() {}

    default void resolveRenderScaleForGui() {}

    default int getColorAttachmentGlId() {
        return -1;
    }

    default int getDepthAttachmentGlId() {
        return -1;
    }

    default VulkanImage getColorAttachment() {
        return null;
    }

    default VulkanImage getDepthAttachment() {
        return null;
    }
}
