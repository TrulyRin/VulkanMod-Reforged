package net.vulkanmod.vulkan.texture;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.util.VUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class ImageUtil {

    public static void copyBufferToImageCmd(VkCommandBuffer commandBuffer, long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {

        try (MemoryStack stack = stackPush()) {

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(bufferOffset);
            region.bufferRowLength(bufferRowLenght);
            region.bufferImageHeight(bufferImageHeight);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(mipLevel);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(xOffset, yOffset, 0);
            region.imageExtent(VkExtent3D.calloc(stack).set(width, height, 1));

            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
        }
    }

    public static void downloadTexture(VulkanImage image, long ptr) {
        downloadTextureRegion(image, ptr, 0, 0, image.width, image.height, image.aspect, image.formatSize);
    }

    public static void downloadTextureRegion(VulkanImage image, long ptr, int x, int y, int width, int height, int aspectMask, int bytesPerPixel) {
        try (MemoryStack stack = stackPush()) {
            int prevLayout = image.getCurrentLayout();
            CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().beginCommands();
            image.transitionImageLayout(stack, commandBuffer.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

            long imageSize = (long) width * height * bytesPerPixel;

            LongBuffer pStagingBuffer = stack.mallocLong(1);
            PointerBuffer pStagingAllocation = stack.pointers(0L);
            MemoryManager.getInstance().createBuffer(imageSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer,
                    pStagingAllocation);

            copyImageToBuffer(commandBuffer.getHandle(), pStagingBuffer.get(0), image.getId(), 0, width, height, x, y, 0, 0, 0, aspectMask);
            image.transitionImageLayout(stack, commandBuffer.getHandle(), restoredLayout(prevLayout));

            long fence = DeviceManager.getGraphicsQueue().submitCommands(commandBuffer);
            vkWaitForFences(DeviceManager.vkDevice, fence, true, VUtil.UINT64_MAX);

            MemoryManager.MapAndCopy(pStagingAllocation.get(0),
                    (data) -> VUtil.memcpy(data.getByteBuffer(0, (int) imageSize), ptr)
            );

            MemoryManager.freeBuffer(pStagingBuffer.get(0), pStagingAllocation.get(0));
        }
    }

    public static void copyImageToBuffer(VkCommandBuffer commandBuffer, long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight) {
        copyImageToBuffer(commandBuffer, buffer, image, mipLevel, width, height, xOffset, yOffset, bufferOffset, bufferRowLenght, bufferImageHeight, VK_IMAGE_ASPECT_COLOR_BIT);
    }

    public static void copyImageToBuffer(VkCommandBuffer commandBuffer, long buffer, long image, int mipLevel, int width, int height, int xOffset, int yOffset, int bufferOffset, int bufferRowLenght, int bufferImageHeight, int aspectMask) {
        try (MemoryStack stack = stackPush()) {

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(bufferOffset);
            region.bufferRowLength(bufferRowLenght);
            region.bufferImageHeight(bufferImageHeight);
            region.imageSubresource().aspectMask(aspectMask);
            region.imageSubresource().mipLevel(mipLevel);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(xOffset, yOffset, 0);
            region.imageExtent().set(width, height, 1);

            vkCmdCopyImageToBuffer(commandBuffer, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, buffer, region);
        }
    }

    public static void blitImage(VulkanImage source, VulkanImage destination,
                                 int srcX0, int srcY0, int srcX1, int srcY1,
                                 int dstX0, int dstY0, int dstX1, int dstY1,
                                 int aspectMask, int filter) {
        CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().getCommandBuffer();

        try (MemoryStack stack = stackPush()) {
            int sourceLayout = source.getCurrentLayout();
            int destinationLayout = destination.getCurrentLayout();

            source.transitionImageLayout(stack, commandBuffer.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            destination.transitionImageLayout(stack, commandBuffer.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            int sourceWidth = Math.abs(srcX1 - srcX0);
            int sourceHeight = Math.abs(srcY1 - srcY0);
            int destinationWidth = Math.abs(dstX1 - dstX0);
            int destinationHeight = Math.abs(dstY1 - dstY0);

            boolean canCopy = sourceWidth == destinationWidth && sourceHeight == destinationHeight
                    && srcX1 >= srcX0 && srcY1 >= srcY0
                    && dstX1 >= dstX0 && dstY1 >= dstY0;
            if (canCopy) {
                copyImage(commandBuffer.getHandle(), source, destination, srcX0, srcY0, dstX0, dstY0,
                        sourceWidth, sourceHeight, aspectMask);
            } else {
                blitImage(commandBuffer.getHandle(), source, destination, srcX0, srcY0, srcX1, srcY1,
                        dstX0, dstY0, dstX1, dstY1, aspectMask, vulkanBlitFilter(filter, aspectMask));
            }

            source.transitionImageLayout(stack, commandBuffer.getHandle(), restoredLayout(sourceLayout));
            destination.transitionImageLayout(stack, commandBuffer.getHandle(), restoredLayout(destinationLayout));
        }

        long fence = DeviceManager.getGraphicsQueue().endIfNeeded(commandBuffer);
        if (fence != VK_NULL_HANDLE) {
            Synchronization.INSTANCE.addCommandBuffer(commandBuffer);
        }
    }

    private static void copyImage(VkCommandBuffer commandBuffer, VulkanImage source, VulkanImage destination,
                                  int srcX, int srcY, int dstX, int dstY, int width, int height, int aspectMask) {
        try (MemoryStack stack = stackPush()) {
            VkImageCopy.Buffer region = VkImageCopy.calloc(1, stack);
            region.srcSubresource()
                    .aspectMask(aspectMask)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            region.srcOffset().set(srcX, srcY, 0);
            region.dstSubresource()
                    .aspectMask(aspectMask)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);
            region.dstOffset().set(dstX, dstY, 0);
            region.extent().set(width, height, 1);

            vkCmdCopyImage(commandBuffer,
                    source.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    destination.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    region);
        }
    }

    private static void blitImage(VkCommandBuffer commandBuffer, VulkanImage source, VulkanImage destination,
                                  int srcX0, int srcY0, int srcX1, int srcY1,
                                  int dstX0, int dstY0, int dstX1, int dstY1,
                                  int aspectMask, int filter) {
        try (MemoryStack stack = stackPush()) {
            VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
            blit.srcOffsets(0, VkOffset3D.calloc(stack).set(srcX0, srcY0, 0));
            blit.srcOffsets(1, VkOffset3D.calloc(stack).set(srcX1, srcY1, 1));
            blit.srcSubresource()
                    .aspectMask(aspectMask)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);

            blit.dstOffsets(0, VkOffset3D.calloc(stack).set(dstX0, dstY0, 0));
            blit.dstOffsets(1, VkOffset3D.calloc(stack).set(dstX1, dstY1, 1));
            blit.dstSubresource()
                    .aspectMask(aspectMask)
                    .mipLevel(0)
                    .baseArrayLayer(0)
                    .layerCount(1);

            vkCmdBlitImage(commandBuffer,
                    source.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                    destination.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    blit,
                    filter);
        }
    }

    private static int vulkanBlitFilter(int glFilter, int aspectMask) {
        if (glFilter == GL11.GL_LINEAR && aspectMask == VK_IMAGE_ASPECT_COLOR_BIT) {
            return VK_FILTER_LINEAR;
        }

        return VK_FILTER_NEAREST;
    }

    private static int restoredLayout(int previousLayout) {
        if (previousLayout == VK_IMAGE_LAYOUT_UNDEFINED) {
            return VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        }

        return previousLayout;
    }

    public static void generateMipmaps(VulkanImage image) {
        try (MemoryStack stack = stackPush()) {

            CommandPool.CommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().beginCommands();

            image.transitionImageLayout(stack, commandBuffer.getHandle(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            int level, prevLevel;

            for (level = 1; level < image.mipLevels; level++) {
                prevLevel = level - 1;

                VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
                barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                barrier.image(image.getId());

                barrier.subresourceRange().baseMipLevel(prevLevel);
                barrier.subresourceRange().levelCount(1);
                barrier.subresourceRange().baseArrayLayer(0);
                barrier.subresourceRange().layerCount(VK_REMAINING_ARRAY_LAYERS);

                barrier.subresourceRange().aspectMask(image.aspect);

                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer.getHandle(),
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        null,
                        null,
                        barrier);

                prevLevel = level - 1;

                VkImageBlit.Buffer blit = VkImageBlit.calloc(1, stack);
                blit.srcOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0));
                blit.srcOffsets(1, VkOffset3D.calloc(stack).set(image.width >> prevLevel, image.height >> prevLevel, 1));
                blit.srcSubresource()
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(prevLevel)
                        .baseArrayLayer(0)
                        .layerCount(1);

                blit.dstOffsets(0, VkOffset3D.calloc(stack).set(0, 0, 0));
                blit.dstOffsets(1, VkOffset3D.calloc(stack).set(image.width >> level, image.height >> level, 1));
                blit.dstSubresource()
                        .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .mipLevel(level)
                        .baseArrayLayer(0)
                        .layerCount(1);

                vkCmdBlitImage(commandBuffer.getHandle(),
                        image.getId(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        image.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        blit,
                        VK_FILTER_LINEAR);

            }

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image.getId());

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(image.mipLevels - 1);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(VK_REMAINING_ARRAY_LAYERS);

            barrier.subresourceRange().aspectMask(image.aspect);

            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            vkCmdPipelineBarrier(commandBuffer.getHandle(),
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0,
                    null,
                    null,
                    barrier);

            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.subresourceRange().baseMipLevel(image.mipLevels - 1);
            barrier.subresourceRange().levelCount(1);

            vkCmdPipelineBarrier(commandBuffer.getHandle(),
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                    0,
                    null,
                    null,
                    barrier);

            image.setCurrentLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            long fence = DeviceManager.getGraphicsQueue().submitCommands(commandBuffer);

            vkWaitForFences(DeviceManager.vkDevice, fence, true, VUtil.UINT64_MAX);
        }
    }
}
