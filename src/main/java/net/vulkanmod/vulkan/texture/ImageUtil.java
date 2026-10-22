package net.vulkanmod.vulkan.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.GraphicsQueue;
import net.vulkanmod.vulkan.util.VUtil;
import net.vulkanmod.vulkan.util.VkResult;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class ImageUtil {
    private static volatile ReadbackLease terminalReadback;

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


    public static void downloadTextureGl(VulkanImage image, int level, long ptr) {
        downloadTextureGl(image, level, ptr, 0, 0, 0, 1, Long.MAX_VALUE);
    }

    public static void downloadTextureGl(VulkanImage image, int level, long ptr,
                                         int rowLength, int skipRows, int skipPixels, int alignment,
                                         long destinationBytes) {
        if (level < 0 || level >= image.mipLevels || level >= 32)
            throw new IllegalArgumentException("Invalid texture download mip level");
        if (rowLength < 0 || skipRows < 0 || skipPixels < 0 || destinationBytes < 0
                || (alignment != 1 && alignment != 2 && alignment != 4 && alignment != 8))
            throw new IllegalArgumentException("Invalid pixel pack layout");
        int width = Math.max(1, image.width >> level), height = Math.max(1, image.height >> level);
        long rowStride, offset, output;
        try {
            long rowBytes = Math.multiplyExact((long) (rowLength == 0 ? width : rowLength), image.formatSize);
            rowStride = Math.multiplyExact(Math.addExact(rowBytes, alignment - 1) / alignment, alignment);
            offset = Math.addExact(Math.multiplyExact((long) skipRows, rowStride),
                    Math.multiplyExact((long) skipPixels, image.formatSize));
            long writtenBytes = Math.addExact(offset, Math.addExact(Math.multiplyExact(height - 1L, rowStride),
                    Math.multiplyExact((long) width, image.formatSize)));
            if (writtenBytes > destinationBytes)
                throw new IllegalArgumentException("Pixel pack destination is shorter than its selected rows");
            Math.addExact(ptr, writtenBytes);
            output = Math.addExact(ptr, offset);
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Pixel pack destination layout overflows its address span", overflow);
        }
        downloadTextureRegion(image, output, level, 0, 0, width, height, image.aspect, image.formatSize,
                rowStride, image.requiresGlDownloadFlip(level));
    }

    public static void downloadTextureRegion(VulkanImage image, long ptr, int x, int y, int width, int height, int aspectMask, int bytesPerPixel) {
        downloadTextureRegion(image, ptr, 0, x, y, width, height, aspectMask, bytesPerPixel,
                (long) width * bytesPerPixel, false);
    }

    private static void downloadTextureRegion(VulkanImage image, long ptr, int level, int x, int y,
                                              int width, int height, int aspectMask, int bytesPerPixel,
                                              long rowStride, boolean reverseRows) {
        Renderer renderer = recordingRenderer();
        if (renderer != null) {
            renderer.flushCmds(() -> {
                downloadTextureRegionRaw(image, ptr, level, x, y, width, height, aspectMask, bytesPerPixel, rowStride, reverseRows);
                return null;
            });
        } else {
            downloadTextureRegionRaw(image, ptr, level, x, y, width, height, aspectMask, bytesPerPixel, rowStride, reverseRows);
        }
    }

    private static synchronized void downloadTextureRegionRaw(VulkanImage image, long ptr, int level, int x, int y,
                                                              int width, int height, int aspectMask, int bytesPerPixel,
                                                              long rowStride, boolean reverseRows) {
        requireTransferReady();
        GraphicsQueue queue = DeviceManager.getGraphicsQueue();
        ReadbackLease lease = new ReadbackLease();
        int previousLayout = image.getCurrentLayout();
        Throwable failure = null;
        try (MemoryStack stack = stackPush()) {
            lease.commands = queue.beginCommands();
            long imageSize = (long) width * height * bytesPerPixel;
            LongBuffer pStagingBuffer = stack.mallocLong(1);
            PointerBuffer pStagingAllocation = stack.pointers(0L);
            MemoryManager.getInstance().createBuffer(imageSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pStagingBuffer,
                    pStagingAllocation);
            lease.buffer = pStagingBuffer.get(0);
            lease.allocation = pStagingAllocation.get(0);
            lease.ownsStaging = true;

            VkCommandBuffer commandBuffer = lease.commands.getHandle();
            image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            copyImageToBuffer(commandBuffer, lease.buffer, image.getId(), level, width, height, x, y, 0, 0, 0, aspectMask);
            image.transitionImageLayout(stack, commandBuffer, restoredLayout(previousLayout));

            long fence = lease.commands.getFence();
            synchronized (queue) {
                lease.check(vkEndCommandBuffer(commandBuffer), "End readback command buffer");
                lease.check(vkResetFences(DeviceManager.vkDevice, fence), "Reset readback fence");
                VkSubmitInfo submit = VkSubmitInfo.calloc(stack).sType$Default().pCommandBuffers(stack.pointers(commandBuffer));
                lease.mayBePending = true;
                lease.check(vkQueueSubmit(queue.queue(), submit, fence), "Submit readback command buffer");
            }
            lease.check(vkWaitForFences(DeviceManager.vkDevice, fence, true, VUtil.UINT64_MAX), "Wait for readback fence");
            lease.completed = true;

            MemoryManager.MapAndCopy(lease.allocation, data -> {
                long rowBytes = (long) width * bytesPerPixel;
                if (!reverseRows && rowStride == rowBytes) {
                    VUtil.memcpy(data.getByteBuffer(0, (int) imageSize), ptr);
                } else {
                    long source = data.get(0);
                    for (int row = 0; row < height; row++) {
                        int sourceRow = reverseRows ? height - 1 - row : row;
                        MemoryUtil.memCopy(source + sourceRow * rowBytes, ptr + row * rowStride, rowBytes);
                    }
                }
            });
        } catch (RuntimeException | Error error) {
            failure = error;
            if (lease.mayBePending && !lease.completed) {
                try {
                    int idleResult;
                    synchronized (queue) {
                        idleResult = vkQueueWaitIdle(queue.queue());
                    }
                    if (idleResult == VK_SUCCESS || idleResult == VK_ERROR_DEVICE_LOST) {
                        lease.completed = true;
                        lease.deviceLost = idleResult == VK_ERROR_DEVICE_LOST;
                    }
                    if (idleResult != VK_SUCCESS) error.addSuppressed(resultFailure("Recover readback queue", idleResult));
                } catch (RuntimeException | Error recoveryFailure) {
                    suppress(error, recoveryFailure);
                }
                if (!lease.completed) {
                    lease.failure = error;
                    terminalReadback = lease;
                    requestTerminalStop(lease);
                }
            }
            throw error;
        } finally {
            if (!lease.mayBePending || lease.completed) {
                if (failure != null && !lease.mayBePending) image.setCurrentLayout(previousLayout);
                lease.release(queue, failure);
            }
        }
    }

    public static void requireTransferReady() {
        ReadbackLease lease = terminalReadback;
        if (lease == null) return;
        requestTerminalStop(lease);
        throw new IllegalStateException("Image transfers stopped after incomplete readback", lease.failure);
    }

    public static synchronized void cleanupTerminalReadback(int deviceIdleResult) {
        ReadbackLease lease = terminalReadback;
        if (lease == null) return;
        if (deviceIdleResult != VK_SUCCESS && deviceIdleResult != VK_ERROR_DEVICE_LOST) {
            suppress(lease.failure, resultFailure("Wait for terminal readback device", deviceIdleResult));
            rethrow(lease.failure);
        }
        try {
            lease.freeStaging();
        } catch (RuntimeException | Error cleanupFailure) {
            suppress(lease.failure, cleanupFailure);
            rethrow(lease.failure);
        }
        terminalReadback = null;
    }

    private static void requestTerminalStop(ReadbackLease lease) {
        synchronized (lease) {
            if (lease.stopRequested || !RenderSystem.isOnRenderThread()) return;
            lease.stopRequested = true;
            try {
                Minecraft.getInstance().stop();
            } catch (RuntimeException | Error stopFailure) {
                suppress(lease.failure, stopFailure);
            }
        }
    }

    private static IllegalStateException resultFailure(String operation, int result) {
        return new IllegalStateException(operation + " failed: " + VkResult.decode(result));
    }

    private static void suppress(Throwable primary, Throwable secondary) {
        if (primary != secondary) primary.addSuppressed(secondary);
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        throw (Error) failure;
    }

    private static final class ReadbackLease {
        CommandPool.CommandBuffer commands;
        long buffer, allocation;
        boolean ownsStaging, mayBePending, completed, deviceLost, stopRequested;
        Throwable failure;

        void check(int result, String operation) {
            if (result == VK_SUCCESS) return;
            if (result == VK_ERROR_DEVICE_LOST) {
                completed = true;
                deviceLost = true;
            }
            throw resultFailure(operation, result);
        }

        void freeStaging() {
            if (!ownsStaging) return;
            ownsStaging = false;
            MemoryManager.freeBuffer(buffer, allocation);
        }

        void release(GraphicsQueue queue, Throwable primary) {
            Throwable cleanupFailure = null;
            try {
                freeStaging();
            } catch (RuntimeException | Error error) {
                cleanupFailure = error;
            }
            if (commands != null && !deviceLost) {
                try {
                    synchronized (queue) {
                        commands.reset();
                    }
                } catch (RuntimeException | Error error) {
                    if (cleanupFailure == null) cleanupFailure = error;
                    else suppress(cleanupFailure, error);
                }
            }
            if (cleanupFailure != null) {
                if (primary != null) suppress(primary, cleanupFailure);
                else rethrow(cleanupFailure);
            }
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
        requireTransferReady();
        TransferKind transfer = preflightTransfer(source, destination,
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, aspectMask, filter);
        if (transfer == TransferKind.NO_OP) return;

        Renderer renderer = recordingRenderer();
        CommandPool.CommandBuffer standalone = null;
        VkCommandBuffer commandBuffer;
        if (renderer != null) {
            if (renderer.getBoundRenderPass() != null) {
                var bindings = GlFramebuffer.captureBindings();
                int logicalWidth = renderer.getViewportLogicalWidth(), logicalHeight = renderer.getViewportLogicalHeight();
                try {
                    renderer.endRenderPass();
                } finally {
                    bindings.restore();
                    Renderer.setViewportScale(logicalWidth, logicalHeight);
                }
                if (renderer.getBoundRenderPass() != null) throw new IllegalStateException("Image transfer requires an ended render pass");
            }
            commandBuffer = Renderer.getCommandBuffer();
        } else {
            standalone = DeviceManager.getGraphicsQueue().getCommandBuffer();
            commandBuffer = standalone.getHandle();
        }

        try (MemoryStack stack = stackPush()) {
            int sourceLayout = source.getCurrentLayout();
            int destinationLayout = destination.getCurrentLayout();
            boolean aliased = source.getId() == destination.getId();

            try {
                if (aliased) {
                    transitionAliasedImage(stack, commandBuffer, source, VK_IMAGE_LAYOUT_GENERAL, true);
                    destination.setCurrentLayout(VK_IMAGE_LAYOUT_GENERAL);
                } else {
                    source.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                    destination.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                }

                switch (transfer) {
                    case COPY -> copyImage(commandBuffer, source, destination,
                            Math.min(srcX0, srcX1), Math.min(srcY0, srcY1),
                            Math.min(dstX0, dstX1), Math.min(dstY0, dstY1),
                            Math.abs(srcX1 - srcX0), Math.abs(srcY1 - srcY0), aspectMask);
                    case ROW_COPY -> copyImageRows(commandBuffer, source, destination,
                            srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, aspectMask);
                    case BLIT -> blitImage(commandBuffer, source, destination, srcX0, srcY0, srcX1, srcY1,
                            dstX0, dstY0, dstX1, dstY1, aspectMask, vulkanBlitFilter(filter, aspectMask));
                    default -> throw new IllegalStateException("Unexpected empty image transfer");
                }
            } finally {
                if (aliased) {
                    transitionAliasedImage(stack, commandBuffer, source, restoredLayout(sourceLayout), false);
                    destination.setCurrentLayout(source.getCurrentLayout());
                } else {
                    source.transitionImageLayout(stack, commandBuffer, restoredLayout(sourceLayout));
                    destination.transitionImageLayout(stack, commandBuffer, restoredLayout(destinationLayout));
                }
            }
        }

        if (standalone != null) {
            long fence = DeviceManager.getGraphicsQueue().endIfNeeded(standalone);
            if (fence != VK_NULL_HANDLE) Synchronization.INSTANCE.addCommandBuffer(standalone);
        }
    }

    private enum TransferKind { NO_OP, COPY, ROW_COPY, BLIT }


    private static TransferKind preflightTransfer(VulkanImage source, VulkanImage destination,
                                                 int srcX0, int srcY0, int srcX1, int srcY1,
                                                 int dstX0, int dstY0, int dstX1, int dstY1,
                                                 int aspectMask, int glFilter) {
        if (source == null || destination == null || source.getId() == VK_NULL_HANDLE || destination.getId() == VK_NULL_HANDLE)
            throw new IllegalArgumentException("Image transfer requires two allocated images");
        if (aspectMask == 0 || (aspectMask & ~source.aspect) != 0 || (aspectMask & ~destination.aspect) != 0)
            throw new IllegalArgumentException("Image transfer aspects are unavailable in its images");
        if ((source.getUsage() & VK_IMAGE_USAGE_TRANSFER_SRC_BIT) == 0
                || (destination.getUsage() & VK_IMAGE_USAGE_TRANSFER_DST_BIT) == 0)
            throw new IllegalArgumentException("Image transfer requires TRANSFER_SRC and TRANSFER_DST image usage");
        if ((aspectMask & (VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT)) != 0 && source.format != destination.format)
            throw new IllegalArgumentException("Depth/stencil image transfers require identical native formats");
        requireTransferRegion(source, srcX0, srcY0, srcX1, srcY1);
        requireTransferRegion(destination, dstX0, dstY0, dstX1, dstY1);

        boolean aliased = source.getId() == destination.getId();
        if (aliased && (source.format != destination.format || source.width != destination.width
                || source.height != destination.height || source.mipLevels != destination.mipLevels
                || source.getCurrentLayout() != destination.getCurrentLayout()))
            throw new IllegalArgumentException("Image transfer aliases disagree about their backing image");

        int sourceWidth = Math.abs(srcX1 - srcX0), sourceHeight = Math.abs(srcY1 - srcY0);
        int destinationWidth = Math.abs(dstX1 - dstX0), destinationHeight = Math.abs(dstY1 - dstY0);
        if (sourceWidth == 0 || sourceHeight == 0 || destinationWidth == 0 || destinationHeight == 0)
            return TransferKind.NO_OP;
        if (aliased && srcX0 == dstX0 && srcY0 == dstY0 && srcX1 == dstX1 && srcY1 == dstY1)
            return TransferKind.NO_OP;
        if (aliased && transferRegionsOverlap(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1))
            throw new IllegalArgumentException("Image transfer source and destination overlap the same image");

        boolean equalExtent = sourceWidth == destinationWidth && sourceHeight == destinationHeight;
        boolean sameXDirection = (srcX1 > srcX0) == (dstX1 > dstX0);
        boolean sameYDirection = (srcY1 > srcY0) == (dstY1 > dstY0);
        if (source.format == destination.format && equalExtent && sameXDirection)
            return sameYDirection ? TransferKind.COPY : TransferKind.ROW_COPY;

        int filter = vulkanBlitFilter(glFilter, aspectMask);
        if (aliased && filter == VK_FILTER_LINEAR && transferRegionsOverlap(
                Math.max(0, Math.min(srcX0, srcX1) - 1), Math.max(0, Math.min(srcY0, srcY1) - 1),
                Math.min(source.width, Math.max(srcX0, srcX1) + 1L), Math.min(source.height, Math.max(srcY0, srcY1) + 1L),
                dstX0, dstY0, dstX1, dstY1))
            throw new IllegalArgumentException("Filtered image transfer may sample its own destination");
        requireBlitFeatures(source.format, destination.format, filter);
        return TransferKind.BLIT;
    }

    private static void requireTransferRegion(VulkanImage image, int x0, int y0, int x1, int y1) {
        if (image.width <= 0 || image.height <= 0 || Math.min(x0, x1) < 0 || Math.min(y0, y1) < 0
                || Math.max(x0, x1) > image.width || Math.max(y0, y1) > image.height)
            throw new IllegalArgumentException("Image transfer region is outside its image extent");
    }

    private static boolean transferRegionsOverlap(long srcX0, long srcY0, long srcX1, long srcY1,
                                                   long dstX0, long dstY0, long dstX1, long dstY1) {
        return Math.min(srcX0, srcX1) < Math.max(dstX0, dstX1) && Math.min(dstX0, dstX1) < Math.max(srcX0, srcX1)
                && Math.min(srcY0, srcY1) < Math.max(dstY0, dstY1) && Math.min(dstY0, dstY1) < Math.max(srcY0, srcY1);
    }

    private static void requireBlitFeatures(int sourceFormat, int destinationFormat, int filter) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties properties = VkFormatProperties.calloc(stack);
            vkGetPhysicalDeviceFormatProperties(DeviceManager.physicalDevice, sourceFormat, properties);
            int sourceFeatures = properties.optimalTilingFeatures();
            if (sourceFormat != destinationFormat)
                vkGetPhysicalDeviceFormatProperties(DeviceManager.physicalDevice, destinationFormat, properties);
            int destinationFeatures = properties.optimalTilingFeatures();
            if ((sourceFeatures & VK_FORMAT_FEATURE_BLIT_SRC_BIT) == 0 || (destinationFeatures & VK_FORMAT_FEATURE_BLIT_DST_BIT) == 0)
                throw new IllegalArgumentException("Native formats do not support this image blit: " + sourceFormat + " -> " + destinationFormat);
            if (filter == VK_FILTER_LINEAR && (sourceFeatures & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0)
                throw new IllegalArgumentException("Native source format does not support linear image blits: " + sourceFormat);
        }
    }


    private static void transitionAliasedImage(MemoryStack stack, VkCommandBuffer commandBuffer, VulkanImage image,
                                               int newLayout, boolean preparingTransfer) {
        if (image.getCurrentLayout() == newLayout) return;
        VulkanImage.transitionLayout(stack, commandBuffer, image, image.getCurrentLayout(), newLayout,
                preparingTransfer ? VK_PIPELINE_STAGE_ALL_COMMANDS_BIT : VK_PIPELINE_STAGE_TRANSFER_BIT,
                preparingTransfer ? VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT : VK_ACCESS_TRANSFER_READ_BIT | VK_ACCESS_TRANSFER_WRITE_BIT,
                preparingTransfer ? VK_PIPELINE_STAGE_TRANSFER_BIT : VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                preparingTransfer ? VK_ACCESS_TRANSFER_READ_BIT | VK_ACCESS_TRANSFER_WRITE_BIT : VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT);
    }

    private static Renderer recordingRenderer() {
        requireTransferReady();
        Renderer renderer = Renderer.getInstance();
        if (renderer == null || !Renderer.isRecording()) return null;
        RenderSystem.assertOnRenderThread();
        if (Renderer.skipRendering || Renderer.getCommandBuffer() == null)
            throw new IllegalStateException("Image transfer requires an active renderer primary");
        return renderer;
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
                    source.getId(), source.getCurrentLayout(),
                    destination.getId(), destination.getCurrentLayout(),
                    region);
        }
    }

    private static void copyImageRows(VkCommandBuffer commandBuffer, VulkanImage source, VulkanImage destination,
                                      int srcX0, int srcY0, int srcX1, int srcY1,
                                      int dstX0, int dstY0, int dstX1, int dstY1, int aspectMask) {
        try (MemoryStack stack = stackPush()) {
            int height = Math.abs(srcY1 - srcY0), width = Math.abs(srcX1 - srcX0);
            VkImageCopy.Buffer regions = VkImageCopy.calloc(Math.min(height, 128), stack);
            for (int row = 0; row < height; ) {
                int count = Math.min(128, height - row);
                regions.limit(count);
                for (int i = 0; i < count; i++) {
                    VkImageCopy region = regions.get(i);
                    region.srcSubresource().aspectMask(aspectMask).mipLevel(0).baseArrayLayer(0).layerCount(1);
                    region.dstSubresource().aspectMask(aspectMask).mipLevel(0).baseArrayLayer(0).layerCount(1);
                    region.srcOffset().set(Math.min(srcX0, srcX1), srcY1 > srcY0 ? srcY0 + row + i : srcY0 - row - i - 1, 0);
                    region.dstOffset().set(Math.min(dstX0, dstX1), dstY1 > dstY0 ? dstY0 + row + i : dstY0 - row - i - 1, 0);
                    region.extent().set(width, 1, 1);
                }
                vkCmdCopyImage(commandBuffer, source.getId(), source.getCurrentLayout(),
                        destination.getId(), destination.getCurrentLayout(), regions);
                row += count;
            }
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
                    source.getId(), source.getCurrentLayout(),
                    destination.getId(), destination.getCurrentLayout(),
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
            image.recordGeneratedMipmaps();
        }
    }
}
