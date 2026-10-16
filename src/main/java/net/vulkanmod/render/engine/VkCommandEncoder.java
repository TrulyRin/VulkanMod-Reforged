package net.vulkanmod.render.engine;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.opengl.*;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.queue.GraphicsQueue;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.TexelBufferDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VkCommandEncoder implements CommandEncoder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final VkGpuDevice device;

    @Nullable
    private RenderPipeline lastPipeline;
    private boolean inRenderPass;

    @Nullable
    private EGlProgram lastProgram;

    private int framebufferId = VkGlFramebuffer.genFramebufferId();

    protected VkCommandEncoder(VkGpuDevice glDevice) {
        this.device = glDevice;
    }

    @Override
    public RenderPass createRenderPass(Supplier<String> supplier, GpuTextureView colorAttachmentView, OptionalInt optionalInt) {
        return this.createRenderPass(supplier, colorAttachmentView, optionalInt, null, OptionalDouble.empty());
    }

    @Override
    public RenderPass createRenderPass(Supplier<String> supplier, GpuTextureView colorAttachmentView, OptionalInt optionalInt, @Nullable GpuTextureView depthTexture, OptionalDouble optionalDouble) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            if (optionalDouble.isPresent() && depthTexture == null) {
                LOGGER.warn("Depth clear value was provided but no depth texture is being used");
            }

                                                                                               
                                                                                             
                                                                                              
                                                                                        
            if (colorAttachmentView.isClosed()) {
                throw new IllegalStateException("Color texture is closed");
            } else if (depthTexture != null && depthTexture.isClosed()) {
                throw new IllegalStateException("Depth texture is closed");
            } else {
                this.inRenderPass = true;
                GpuTexture depthTexture1 = depthTexture != null ? depthTexture.texture() : null;
                VkFbo fbo = ((VkTextureView)colorAttachmentView).getFbo(depthTexture1);
                fbo.bind();

                int width = colorAttachmentView.getWidth(0);
                int height = colorAttachmentView.getHeight(0);

                int j = 0;
                if (optionalInt.isPresent()) {
                    int k = optionalInt.getAsInt();
                    VRenderSystem.setClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
                    j |= 16384;
                }

                if (depthTexture != null && optionalDouble.isPresent()) {
                    VRenderSystem.clearDepth(optionalDouble.getAsDouble());
                    j |= 256;
                }

                if (j != 0) {
                    Renderer.clearAttachments(j, width, height);
                }

                Renderer.setViewport(0, 0, width, height);
                this.lastPipeline = null;
                return new VkRenderPass(this, depthTexture != null, true);
            }
        }

    }

    @Override
    public void clearColorTexture(GpuTexture colorAttachment, int clearColor) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }
        else if (Renderer.isRecording()) {
                                                                                                  
            {
                VkGpuTexture vkGpuTexture = (VkGpuTexture) colorAttachment;
                VkGlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
                VkGlFramebuffer.framebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, vkGpuTexture.glId(), 0);

                VkGlFramebuffer.beginRendering(VkGlFramebuffer.getFramebuffer(framebufferId));
                VRenderSystem.setClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
                Renderer.clearAttachments(0x4000);
                Renderer.getInstance().endRenderPass();

                VkFbo fbo = ((VkGpuTexture)colorAttachment).getFbo(null);

                ((VkGpuTexture) colorAttachment).setClearColor(clearColor);

                Framebuffer boundFramebuffer = Renderer.getInstance().getBoundFramebuffer();
                if (boundFramebuffer != null && boundFramebuffer.getColorAttachment() == ((VkGpuTexture) colorAttachment).getVulkanImage()) {
                    fbo.clearAttachments();
                }
            }
        }
        else {
            GraphicsQueue graphicsQueue = DeviceManager.getGraphicsQueue();
            var commandBuffer = graphicsQueue.getCommandBuffer();
            VkGpuTexture vkGpuTexture = (VkGpuTexture) colorAttachment;

            VkGlFramebuffer glFramebuffer = VkGlFramebuffer.getFramebuffer(this.framebufferId);
            glFramebuffer.setAttachmentTexture(GL30.GL_COLOR_ATTACHMENT0, vkGpuTexture.glId());
            glFramebuffer.create();

            Framebuffer framebuffer = glFramebuffer.getFramebuffer();
            var renderPass = glFramebuffer.getRenderPass();
            try (MemoryStack stack = stackPush()) {
                framebuffer.beginRenderPass(commandBuffer.handle, renderPass, stack);
            }

            VRenderSystem.setClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));
            Renderer.clearAttachments(commandBuffer.handle, 0x4000, 0, 0, framebuffer.getWidth(), framebuffer.getHeight());
            renderPass.endRenderPass(commandBuffer.handle);

            long fence = graphicsQueue.submitCommands(commandBuffer);
            Synchronization.waitFence(fence);
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorAttachment, int clearColor, GpuTexture depthAttachment, double clearDepth) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }
        else {
                                                                                                  
            {
                VkFbo fbo = ((VkGpuTexture)colorAttachment).getFbo(depthAttachment);

                ((VkGpuTexture) colorAttachment).setClearColor(clearColor);
                ((VkGpuTexture) depthAttachment).setDepthClearValue((float) clearDepth);

                Framebuffer boundFramebuffer = Renderer.getInstance().getBoundFramebuffer();
                if (boundFramebuffer != null && boundFramebuffer.getColorAttachment() == ((VkGpuTexture) colorAttachment).getVulkanImage()
                    && boundFramebuffer.getDepthAttachment() == ((VkGpuTexture) depthAttachment).getVulkanImage())
                {
                    fbo.clearAttachments();
                }
            }
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorAttachment, int clearColor, GpuTexture depthAttachment, double clearDepth, int x0, int y0, int width, int height) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            VRenderSystem.clearDepth(clearDepth);
            VRenderSystem.setClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));

            int framebufferHeight = colorAttachment.getHeight(0);
            y0 = framebufferHeight - height - y0;

            Framebuffer boundFramebuffer = Renderer.getInstance().getBoundFramebuffer();
            if (boundFramebuffer != null && boundFramebuffer.getColorAttachment() == ((VkGpuTexture) colorAttachment).getVulkanImage()
                && boundFramebuffer.getDepthAttachment() == ((VkGpuTexture) depthAttachment).getVulkanImage())
            {
                Renderer.clearAttachments(0x4100, x0, y0, width, height);
            }
            else {
                VkGpuTexture gpuTexture = (VkGpuTexture) colorAttachment;
                gpuTexture.getFbo(depthAttachment).bind();

                Renderer.clearAttachments(0x4100, x0, y0, width, height);
            }
        }
    }

    @Override
    public void clearDepthTexture(GpuTexture depthAttachment, double clearDepth) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }
        else {
            Framebuffer boundFramebuffer = Renderer.getInstance().getBoundFramebuffer();
            if (boundFramebuffer != null && boundFramebuffer.getDepthAttachment() == ((VkGpuTexture) depthAttachment).getVulkanImage()) {
                VRenderSystem.clearDepth(clearDepth);
                Renderer.clearAttachments(0x100);
            }
            else {
                ((VkGpuTexture) depthAttachment).setDepthClearValue((float) clearDepth);
            }
        }
    }

    public void clearStencilTexture(GpuTexture stencilAttachment, int clearValue) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        }

        Framebuffer boundFramebuffer = Renderer.getInstance().getBoundFramebuffer();
        if (boundFramebuffer != null && boundFramebuffer.getDepthAttachment() == ((VkGpuTexture) stencilAttachment).getVulkanImage()) {
            Renderer.clearAttachments(0x400);
        }
    }

    @Override
    public void writeToBuffer(GpuBufferSlice gpuBufferSlice, ByteBuffer byteBuffer) {
        writeBuffer(gpuBufferSlice, byteBuffer, false);
    }

    void initializeBuffer(VkGpuBuffer buffer, ByteBuffer bytes) {
        writeBuffer(buffer.slice(), bytes, true);
    }

    private void writeBuffer(GpuBufferSlice gpuBufferSlice, ByteBuffer byteBuffer, boolean initialData) {
        if (this.inRenderPass && !initialData) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            VkGpuBuffer vkGpuBuffer = (VkGpuBuffer) gpuBufferSlice.buffer();
            if (vkGpuBuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            }
            else {
                int size = byteBuffer.remaining();
                if (!initialData && (vkGpuBuffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
                    throw new IllegalStateException("Buffer needs USAGE_COPY_DST to be a destination for a write");
                } else if (size > gpuBufferSlice.length()) {
                    throw new IllegalArgumentException("Write exceeds the requested buffer slice");
                } else if (gpuBufferSlice.offset() < 0 || gpuBufferSlice.length() < 0
                        || gpuBufferSlice.offset() > vkGpuBuffer.size() - gpuBufferSlice.length()) {
                    throw new IllegalArgumentException(
                            "Cannot write more data than this buffer can hold (attempting to write " + size + " bytes at offset " + gpuBufferSlice.offset() + " to " + gpuBufferSlice.length() + " slice size)"
                    );
                } else {
                    if (size > 0) uploadBufferData(vkGpuBuffer.getBuffer(), gpuBufferSlice.offset(), byteBuffer);
                }
            }
        }
    }

    protected void uploadBufferData(Buffer destination, long destinationOffset, ByteBuffer bytes) {
        int size = bytes.remaining();
        Renderer renderer = Renderer.getInstance();
                                                                                   
                                                                                  
        Framebuffer activeFramebuffer = this.inRenderPass ? renderer.getBoundFramebuffer() : null;
        net.vulkanmod.vulkan.framebuffer.RenderPass activePass =
                this.inRenderPass ? renderer.getBoundRenderPass() : null;
        VkGlFramebuffer activeGlFramebuffer = this.inRenderPass ? VkGlFramebuffer.getBoundFramebuffer() : null;
        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        stagingBuffer.copyBuffer(size, bytes);
        long sourceOffset = stagingBuffer.getOffset();
                                                                                
                                                                                   
                                                                               
        if (Renderer.isRecording()) {
            recordBufferCopy(stagingBuffer, destination, sourceOffset, destinationOffset, size);
            if (activeGlFramebuffer != null) {
                VkGlFramebuffer.beginRendering(activeGlFramebuffer);
                Renderer.setViewport(0, 0, activeFramebuffer.getWidth(), activeFramebuffer.getHeight());
            } else if (activeFramebuffer != null && activePass != null) {
                renderer.beginRenderPass(activePass, activeFramebuffer);
            }
            return;
        }

        var commandBuffer = renderer.getTransferCb();
        try (MemoryStack stack = stackPush()) {
            if (!commandBuffer.isRecording()) commandBuffer.begin(stack);
            VkBufferMemoryBarrier.Buffer barrier = VkBufferMemoryBarrier.calloc(1, stack)
                    .sType$Default().buffer(destination.getId())
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .size(VK_WHOLE_SIZE);
            vkCmdPipelineBarrier(commandBuffer.handle, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0, null, barrier, null);
            VkBufferCopy.Buffer region = VkBufferCopy.calloc(1, stack)
                    .size(size).srcOffset(sourceOffset).dstOffset(destinationOffset);
            vkCmdCopyBuffer(commandBuffer.handle, stagingBuffer.getId(), destination.getId(), region);
        }
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBuffer gpuBuffer, boolean readable, boolean writable) {
        return this.mapBuffer(gpuBuffer.slice(), readable, writable);
    }

    @Override
    public GpuBuffer.MappedView mapBuffer(GpuBufferSlice gpuBufferSlice, boolean readable, boolean writable) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            VkGpuBuffer gpuBuffer = (VkGpuBuffer)(gpuBufferSlice.buffer());
            if (gpuBuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            } else if (!readable && !writable) {
                throw new IllegalArgumentException("At least read or write must be true");
            } else if (readable && (gpuBuffer.usage() & 1) == 0) {
                throw new IllegalStateException("Buffer is not readable");
            } else if (writable && (gpuBuffer.usage() & 2) == 0) {
                throw new IllegalStateException("Buffer is not writable");
            } else if (gpuBufferSlice.offset() + gpuBufferSlice.length() > gpuBuffer.size()) {
                throw new IllegalArgumentException(
                        "Cannot map more data than this buffer can hold (attempting to map "
                        + gpuBufferSlice.length()
                        + " bytes at offset "
                        + gpuBufferSlice.offset()
                        + " from "
                        + gpuBuffer.size()
                        + " size buffer)"
                );
            } else {
                int i = 0;
                if (readable) {
                    i |= 1;
                }

                if (writable) {
                    i |= 34;
                }

                ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(gpuBuffer.getBuffer().getDataPtr() + gpuBufferSlice.offset(), (int) gpuBufferSlice.length());
                return new VkGpuBuffer.MappedView(0, byteBuffer);
            }
        }
    }

    public void copyToBuffer(GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            VkGpuBuffer vkGpuBuffer = (VkGpuBuffer) gpuBufferSlice.buffer();
            if (vkGpuBuffer.closed) {
                throw new IllegalStateException("Source buffer already closed");
            } else if ((vkGpuBuffer.usage() & GpuBuffer.USAGE_COPY_SRC) == 0) {
                throw new IllegalStateException("Source buffer needs USAGE_COPY_SRC to be a source for a copy");
            } else {
                VkGpuBuffer vkGpuBuffer2 = (VkGpuBuffer) gpuBufferSlice2.buffer();
                if (vkGpuBuffer2.closed) {
                    throw new IllegalStateException("Target buffer already closed");
                } else if ((vkGpuBuffer2.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
                    throw new IllegalStateException("Target buffer needs USAGE_COPY_DST to be a destination for a copy");
                } else if (gpuBufferSlice.length() != gpuBufferSlice2.length()) {
                    long var6 = gpuBufferSlice.length();
                    throw new IllegalArgumentException("Cannot copy from slice of size " + var6 + " to slice of size " + gpuBufferSlice2.length() + ", they must be equal");
                } else if (gpuBufferSlice.offset() < 0 || gpuBufferSlice.length() < 0
                        || gpuBufferSlice.offset() > vkGpuBuffer.size() - gpuBufferSlice.length()) {
                    long var5 = gpuBufferSlice.length();
                    throw new IllegalArgumentException("Cannot copy more data than the source buffer holds (attempting to copy " + var5 + " bytes at offset " + gpuBufferSlice.offset() + " from " + vkGpuBuffer.size() + " size buffer)");
                } else if (gpuBufferSlice2.offset() < 0 || gpuBufferSlice2.length() < 0
                        || gpuBufferSlice2.offset() > vkGpuBuffer2.size() - gpuBufferSlice2.length()) {
                    long var10002 = gpuBufferSlice2.length();
                    throw new IllegalArgumentException("Cannot copy more data than the target buffer can hold (attempting to copy " + var10002 + " bytes at offset " + gpuBufferSlice2.offset() + " to " + vkGpuBuffer2.size() + " size buffer)");
                } else {
                    long length = gpuBufferSlice.length();
                    if (length == 0) return;
                    if (vkGpuBuffer.buffer == vkGpuBuffer2.buffer
                            && gpuBufferSlice.offset() < gpuBufferSlice2.offset() + length
                            && gpuBufferSlice2.offset() < gpuBufferSlice.offset() + length) {
                        throw new IllegalArgumentException("Source and destination buffer copy ranges overlap");
                    }
                    recordBufferCopy(vkGpuBuffer.getBuffer(), vkGpuBuffer2.getBuffer(),
                            gpuBufferSlice.offset(), gpuBufferSlice2.offset(), length);
                }
            }
        }
    }

    @Override
    public void writeToTexture(GpuTexture gpuTexture, NativeImage nativeImage) {
        int i = gpuTexture.getWidth(0);
        int j = gpuTexture.getHeight(0);
        if (nativeImage.getWidth() != i || nativeImage.getHeight() != j) {
            throw new IllegalArgumentException(
                    "Cannot replace texture of size " + i + "x" + j + " with image of size " + nativeImage.getWidth() + "x" + nativeImage.getHeight()
            );
        } else if (gpuTexture.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
        } else {
            this.writeToTexture(gpuTexture, nativeImage, 0, 0, 0, 0, i, j, 0, 0);
        }
    }

    @Override
    public void writeToTexture(GpuTexture gpuTexture, NativeImage nativeImage, int level, int arrayLayer, int xOffset, int yOffset, int width, int height, int unpackSkipPixels, int unpackSkipRows) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (level >= 0 && level < gpuTexture.getMipLevels()) {
            if (unpackSkipPixels + width > nativeImage.getWidth() || unpackSkipRows + height > nativeImage.getHeight()) {
                throw new IllegalArgumentException(
                        "Copy source ("
                        + nativeImage.getWidth()
                        + "x"
                        + nativeImage.getHeight()
                        + ") is not large enough to read a rectangle of "
                        + width
                        + "x"
                        + height
                        + " from "
                        + unpackSkipPixels
                        + "x"
                        + unpackSkipRows
                );
            } else if (xOffset + width > gpuTexture.getWidth(level) || yOffset + height > gpuTexture.getHeight(level)) {
                throw new IllegalArgumentException(
                        "Dest texture (" + width + "x" + height + ") is not large enough to write a rectangle of " + width + "x" + height + " at " + xOffset + "x" + yOffset + " (at mip level " + level + ")"
                );
            } else if (gpuTexture.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else {
                VTextureSelector.setActiveTexture(0);
                var glTexture = VkGlTexture.getTexture(((GlTexture) gpuTexture).glId());
                                                                                             
                VTextureSelector.bindTexture(glTexture.getVulkanImage());
                VTextureSelector.uploadSubTexture(level, arrayLayer, width, height, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, nativeImage.getWidth(), nativeImage.getPointer());
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + level + ", must be >= 0 and < " + gpuTexture.getMipLevels());
        }
    }

    @Override
    public void writeToTexture(GpuTexture gpuTexture, ByteBuffer byteBuffer, NativeImage.Format format, int level, int j, int xOffset, int yOffset, int width, int height) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (level >= 0 && level < gpuTexture.getMipLevels()) {
            if (width * height * format.components() > byteBuffer.remaining()) {
                throw new IllegalArgumentException(
                        "Copy would overrun the source buffer (remaining length of " + byteBuffer.remaining() + ", but copy is " + width + "x" + height + " of format " + format + ")"
                );
            } else if (xOffset + width > gpuTexture.getWidth(level) || yOffset + height > gpuTexture.getHeight(level)) {
                throw new IllegalArgumentException(
                        "Dest texture ("
                        + gpuTexture.getWidth(level)
                        + "x"
                        + gpuTexture.getHeight(level)
                        + ") is not large enough to write a rectangle of "
                        + width
                        + "x"
                        + height
                        + " at "
                        + xOffset
                        + "x"
                        + yOffset
                );
            } else if (gpuTexture.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((gpuTexture.usage() & 1) == 0) {
                throw new IllegalStateException("Color texture must have USAGE_COPY_DST to be a destination for a write");
            } else if (j >= gpuTexture.getDepthOrLayers()) {
                throw new UnsupportedOperationException("Depth or layer is out of range, must be >= 0 and < " + gpuTexture.getDepthOrLayers());
            }
            else {
                VTextureSelector.setActiveTexture(0);
                VTextureSelector.bindTexture(((VkGpuTexture) gpuTexture).getVulkanImage());
                VTextureSelector.uploadSubTexture(level, j, width, height, xOffset, yOffset, 0, 0, width, MemoryUtil.memAddress(byteBuffer));
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + gpuTexture.getMipLevels());
        }
    }

    @Override
    public void copyTextureToBuffer(GpuTexture gpuTexture, GpuBuffer gpuBuffer, long i, Runnable runnable, int j) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            this.copyTextureToBuffer(gpuTexture, gpuBuffer, i, runnable, j, 0, 0, gpuTexture.getWidth(j), gpuTexture.getHeight(j));
        }
    }

    @Override
    public void copyTextureToBuffer(GpuTexture gpuTexture, GpuBuffer gpuBuffer, long dstOffset, Runnable runnable, int mipLevel, int xOffset, int yOffset, int width, int height) {
        VkGpuBuffer vkGpuBuffer = (VkGpuBuffer) gpuBuffer;
        VkGpuTexture vkGpuTexture = (VkGpuTexture) gpuTexture;

        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (mipLevel >= 0 && mipLevel < gpuTexture.getMipLevels()) {
            long copyBytes = (long) width * height * vkGpuTexture.getVulkanImage().formatSize;
            if (width < 0 || height < 0 || dstOffset < 0 || copyBytes < 0
                    || dstOffset > gpuBuffer.size() - copyBytes) {
                throw new IllegalArgumentException(
                        "Buffer of size "
                        + gpuBuffer.size()
                        + " is not large enough to hold "
                        + width
                        + "x"
                        + height
                        + " pixels ("
                        + vkGpuTexture.getVulkanImage().formatSize
                        + " bytes each) starting from offset "
                        + dstOffset
                );
            }
            else if (xOffset < 0 || yOffset < 0 || xOffset > gpuTexture.getWidth(mipLevel) - width
                    || yOffset > gpuTexture.getHeight(mipLevel) - height) {
                throw new IllegalArgumentException(
                        "Copy source texture ("
                        + gpuTexture.getWidth(mipLevel)
                        + "x"
                        + gpuTexture.getHeight(mipLevel)
                        + ") is not large enough to read a rectangle of "
                        + width
                        + "x"
                        + height
                        + " from "
                        + xOffset
                        + ","
                        + yOffset
                );
            } else if (gpuTexture.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (gpuBuffer.isClosed()) {
                throw new IllegalStateException("Destination buffer is closed");
            } else if ((gpuTexture.usage() & GpuTexture.USAGE_COPY_SRC) == 0
                    || (gpuBuffer.usage() & GpuBuffer.USAGE_COPY_DST) == 0) {
                throw new IllegalArgumentException("Readback needs a COPY_SRC texture and COPY_DST buffer");
            } else {
                flushPendingCommands();
                if (width > 0 && height > 0) {
                    ImageUtil.copyImageToBuffer(vkGpuTexture.getVulkanImage(), vkGpuBuffer.getBuffer(), mipLevel, width, height, xOffset, yOffset, dstOffset, width, height);
                }
                runnable.run();
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + gpuTexture.getMipLevels());
        }
    }

    @Override
    public void copyTextureToTexture(GpuTexture gpuTexture, GpuTexture gpuTexture2, int mipLevel, int j, int k, int l, int m, int n, int o) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (mipLevel >= 0 && mipLevel < gpuTexture.getMipLevels() && mipLevel < gpuTexture2.getMipLevels()) {
            if (j < 0 || k < 0 || n < 0 || o < 0
                    || j > gpuTexture2.getWidth(mipLevel) - n || k > gpuTexture2.getHeight(mipLevel) - o) {
                throw new IllegalArgumentException(
                        "Dest texture ("
                        + gpuTexture2.getWidth(mipLevel)
                        + "x"
                        + gpuTexture2.getHeight(mipLevel)
                        + ") is not large enough to write a rectangle of "
                        + n
                        + "x"
                        + o
                        + " at "
                        + j
                        + "x"
                        + k
                );
            } else if (l < 0 || m < 0 || l > gpuTexture.getWidth(mipLevel) - n
                    || m > gpuTexture.getHeight(mipLevel) - o) {
                throw new IllegalArgumentException(
                        "Source texture ("
                        + gpuTexture.getWidth(mipLevel)
                        + "x"
                        + gpuTexture.getHeight(mipLevel)
                        + ") is not large enough to read a rectangle of "
                        + n
                        + "x"
                        + o
                        + " at "
                        + l
                        + "x"
                        + m
                );
            } else if (gpuTexture.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (gpuTexture2.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else if ((gpuTexture.usage() & GpuTexture.USAGE_COPY_SRC) == 0
                    || (gpuTexture2.usage() & GpuTexture.USAGE_COPY_DST) == 0) {
                throw new IllegalArgumentException("Copy needs a COPY_SRC source and COPY_DST destination texture");
            } else {
                if (n == 0 || o == 0) return;
                VulkanImage source = ((VkGpuTexture) gpuTexture).getVulkanImage();
                VulkanImage destination = ((VkGpuTexture) gpuTexture2).getVulkanImage();
                if (source == destination && j < l + n && l < j + n && k < m + o && m < k + o) {
                    throw new IllegalArgumentException("Source and destination image copy regions overlap");
                }
                recordTextureCopy(source, destination, mipLevel, j, k, l, m, n, o);
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + mipLevel + ", must be >= 0 and < " + gpuTexture.getMipLevels() + " and < " + gpuTexture2.getMipLevels());
        }
    }

    @Override
    public GpuFence createFence() {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            return Renderer.getInstance().createGpuFence(this::flushPendingCommands);
        }
    }

    protected void flushPendingCommands() {
        Renderer renderer = Renderer.getInstance();
        if (Renderer.isRecording()) {
            renderer.flushCmds();
        } else {
            renderer.submitUploads();
            DeviceManager.getTransferQueue().waitIdle();
            DeviceManager.getGraphicsQueue().waitIdle();
            renderer.completeGpuSubmissions();
        }
    }

    protected void recordBufferCopy(Buffer source, Buffer destination, long sourceOffset,
                                    long destinationOffset, long length) {
        recordTransfer((stack, commandBuffer) -> {
            VkMemoryBarrier.Buffer before = VkMemoryBarrier.calloc(1, stack).sType$Default()
                    .srcAccessMask(VK_ACCESS_MEMORY_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT | VK_ACCESS_TRANSFER_WRITE_BIT);
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0, before, null, null);
            VkBufferCopy.Buffer region = VkBufferCopy.calloc(1, stack)
                    .srcOffset(sourceOffset).dstOffset(destinationOffset).size(length);
            vkCmdCopyBuffer(commandBuffer, source.getId(), destination.getId(), region);
            VkMemoryBarrier.Buffer after = VkMemoryBarrier.calloc(1, stack).sType$Default()
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT | VK_ACCESS_MEMORY_WRITE_BIT);
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT,
                    0, after, null, null);
        });
    }

    protected void recordTextureCopy(VulkanImage source, VulkanImage destination, int mipLevel,
                                     int destinationX, int destinationY, int sourceX, int sourceY,
                                     int width, int height) {
        recordTransfer((stack, commandBuffer) -> {
            boolean sameImage = source == destination;
            int sourceLayout = sameImage ? VK_IMAGE_LAYOUT_GENERAL : VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
            int destinationLayout = sameImage ? VK_IMAGE_LAYOUT_GENERAL : VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
            source.transitionImageLayout(stack, commandBuffer, sourceLayout);
            destination.transitionImageLayout(stack, commandBuffer, destinationLayout);
            VkImageCopy.Buffer region = VkImageCopy.calloc(1, stack);
            region.srcSubresource().aspectMask(source.aspect).mipLevel(mipLevel).baseArrayLayer(0).layerCount(1);
            region.dstSubresource().aspectMask(destination.aspect).mipLevel(mipLevel).baseArrayLayer(0).layerCount(1);
            region.srcOffset().set(sourceX, sourceY, 0);
            region.dstOffset().set(destinationX, destinationY, 0);
            region.extent().set(width, height, 1);
            vkCmdCopyImage(commandBuffer, source.getId(), sourceLayout, destination.getId(), destinationLayout, region);
            source.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            destination.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        });
    }

    private void recordTransfer(BiConsumer<MemoryStack, VkCommandBuffer> operation) {
        try (MemoryStack stack = stackPush()) {
            if (Renderer.isRecording()) {
                Renderer.getInstance().endRenderPass();
                operation.accept(stack, Renderer.getCommandBuffer());
            } else {
                flushPendingCommands();
                GraphicsQueue queue = DeviceManager.getGraphicsQueue();
                var commands = queue.beginCommands();
                operation.accept(stack, commands.getHandle());
                Synchronization.waitFence(queue.submitCommands(commands));
                commands.reset();
            }
        }
    }

    @Override
    public GpuQuery timerQueryBegin() {
        return VkGpuTimestampQuery.begin();
    }

    @Override
    public void timerQueryEnd(GpuQuery gpuQuery) {
        if (!(gpuQuery instanceof VkGpuTimestampQuery timestamp)) {
            throw new IllegalArgumentException("Timer query does not belong to the Vulkan backend");
        }
        timestamp.end();
    }

    @Override
    public void presentTexture(GpuTextureView gpuTexture) {
                                                                                              
                                                                                  
                                                                                               
                                                                                                
                                                                                              
        VkTextureView view = (VkTextureView) gpuTexture;
        validatePresentationSource(view);
        Renderer renderer = Renderer.getInstance();
                                                                                   
                                                                                      
        if (!Renderer.isRecording()) {
            renderer.beginFrame();
        }
        var swapChain = renderer.getSwapChain();
        if (!swapChain.hasImages() || !swapChain.isAcquired()) {
            return;
        }
        VulkanImage source = validatePresentationSource(view);
        VulkanImage target = swapChain.getColorAttachment();
        if (target == null || target.getId() == 0L) return;
        ImageUtil.blitFramebuffer(source, target);
    }

    private static VulkanImage validatePresentationSource(VkTextureView view) {
        if (view.isClosed() || view.texture().isClosed()) {
            throw new IllegalStateException("Cannot present a closed texture or view");
        }
        VulkanImage image = view.texture().getVulkanImage();
        if (image == null || image.getId() == 0L) {
            throw new IllegalStateException("Cannot present a released Vulkan image");
        }
        return image;
    }

    protected <T> void executeDrawMultiple(
            VkRenderPass renderPass,
            Collection<RenderPass.Draw<T>> collection,
            @Nullable GpuBuffer indexBuffer,
            @Nullable VertexFormat.IndexType indexType,
            Collection<String> perDrawUniforms,
            T object
    ) {
        if (indexType == null) {
            indexType = VertexFormat.IndexType.SHORT;
        }

                                                                               
                                                                             
                                                                             
                                                                                
        Map<String, GpuBufferSlice> passUniforms = new HashMap<>(renderPass.uniforms);
        for (RenderPass.Draw<T> draw : collection) {
            try {
                perDrawUniforms.forEach(renderPass.uniforms::remove);
                VertexFormat.IndexType indexType2 = draw.indexType() == null ? indexType : draw.indexType();
                renderPass.setIndexBuffer(draw.indexBuffer() == null ? indexBuffer : draw.indexBuffer(), indexType2);
                renderPass.setVertexBuffer(draw.slot(), draw.vertexBuffer());
                BiConsumer<T, RenderPass.UniformUploader> biConsumer = draw.uniformUploaderConsumer();
                if (biConsumer != null) {
                    biConsumer.accept(object, renderPass.uniforms::put);
                }
                for (String uniform : perDrawUniforms) {
                    if (!renderPass.uniforms.containsKey(uniform)) {
                        throw new IllegalStateException("Missing per-draw uniform " + uniform);
                    }
                }
                this.executeDraw(renderPass, 0, draw.firstIndex(), draw.indexCount(), indexType2, 1);
            } finally {
                renderPass.uniforms.clear();
                renderPass.uniforms.putAll(passUniforms);
            }
        }
    }

    protected void executeDraw(VkRenderPass renderPass, int vertexOffset, int firstIndex, int vertexCount, @Nullable VertexFormat.IndexType indexType, int instanceCount) {
        if (this.trySetup(renderPass)) {
            if (GlRenderPass.VALIDATION) {
                if (indexType != null) {
                    if (renderPass.indexBuffer == null) {
                        throw new IllegalStateException("Missing index buffer");
                    }

                    if (renderPass.indexBuffer.isClosed()) {
                        throw new IllegalStateException("Index buffer has been closed!");
                    }
                }

                boolean requiresVertexBuffer = !renderPass.pipeline.getVertexFormat().getElements().isEmpty();
                if (requiresVertexBuffer && renderPass.vertexBuffers[0] == null) {
                    throw new IllegalStateException("Missing vertex buffer at slot 0");
                }

                if (renderPass.vertexBuffers[0] != null && renderPass.vertexBuffers[0].isClosed()) {
                    throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
                }
            }

            this.drawFromBuffers(renderPass, vertexOffset, firstIndex, vertexCount, indexType, renderPass.pipeline, instanceCount);
        }
    }

    public void drawFromBuffers(VkRenderPass renderPass, int vertexOffset, int firstIndex, int vertexCount,
                                @Nullable VertexFormat.IndexType indexType, RenderPipeline renderPipeline, int instanceCount)
    {
        if (instanceCount < 1) {
            instanceCount = 1;
        }
        if (vertexOffset < 0) {
            vertexOffset = 0;
        }

        VkCommandBuffer vkCommandBuffer = Renderer.getCommandBuffer();
        VkGpuBuffer vertexBuffer = (VkGpuBuffer)renderPass.vertexBuffers[0];
        try (MemoryStack stack = stackPush()) {
            if (vertexBuffer != null) {
                                                                                                                            
                VK11.vkCmdBindVertexBuffers(vkCommandBuffer, 0, stack.longs(vertexBuffer.buffer.getId()), stack.longs(vertexBuffer.offset));
            }

            if (renderPass.indexBuffer != null) {
                VkGpuBuffer indexBuffer = (VkGpuBuffer)renderPass.indexBuffer;

                int vkIndexType = switch (indexType) {
                    case SHORT -> VK_INDEX_TYPE_UINT16;
                    case INT -> VK_INDEX_TYPE_UINT32;
                };

                                                                                                         
                VK11.vkCmdBindIndexBuffer(vkCommandBuffer, indexBuffer.buffer.getId(), indexBuffer.offset, vkIndexType);
                VK11.vkCmdDrawIndexed(vkCommandBuffer, vertexCount, instanceCount, firstIndex, vertexOffset, 0);
            }
            else {
                var autoIndexBuffer = Renderer.getDrawer().getAutoIndexBuffer(renderPipeline.getVertexFormatMode(), vertexCount);
                if (autoIndexBuffer != null) {
                    int indexCount = autoIndexBuffer.getIndexCount(vertexCount);
                    VK11.vkCmdBindIndexBuffer(vkCommandBuffer, autoIndexBuffer.getIndexBuffer().getId(), 0, autoIndexBuffer.getIndexBuffer().indexType.value);
                    VK11.vkCmdDrawIndexed(vkCommandBuffer, indexCount, instanceCount, firstIndex, vertexOffset, 0);
                }
                else {
                    VK11.vkCmdDraw(vkCommandBuffer, vertexCount, instanceCount, vertexOffset, 0);
                }
            }
        }
    }

    public boolean trySetup(VkRenderPass renderPass) {
        if (VkRenderPass.VALIDATION) {
            if (renderPass.pipeline == null) {
                throw new IllegalStateException("Can't draw without a render pipeline");
            }

            for (RenderPipeline.UniformDescription uniformDescription : renderPass.pipeline.getUniforms()) {
                Object object = renderPass.uniforms.get(uniformDescription.name());
                if (object == null && !GlProgram.BUILT_IN_UNIFORMS.contains(uniformDescription.name())) {
                    throw new IllegalStateException("Missing uniform " + uniformDescription.name() + " (should be " + uniformDescription.type() + ")");
                }
            }

        }

        applyPipelineState(renderPass.pipeline);
        setupUniforms(renderPass);

        if (renderPass.isScissorEnabled()) {
            Renderer.setScissor(
                    renderPass.getScissorX(), renderPass.getScissorY(), renderPass.getScissorWidth(), renderPass.getScissorHeight()
            );
        } else {
            Renderer.resetScissor();
        }

        return bindPipeline(renderPass.pipeline);
    }

    public void setupUniforms(VkRenderPass renderPass) {
        RenderPipeline renderPipeline = renderPass.pipeline;
        Pipeline pipeline = ExtendedRenderPipeline.of(renderPass.pipeline).getPipeline();

        for (UBO ubo : pipeline.getBuffers()) {
            String uniformName = ubo.name;

            GpuBufferSlice gpuBufferSlice = renderPass.uniforms.get(uniformName);

                                                                           
            if (gpuBufferSlice == null) {
                reportUnbound(renderPipeline, "uniform block", uniformName, "falling back to the global buffer");
                ubo.setUseGlobalBuffer(true);
                ubo.setUpdate(true);
                continue;
            }

            VkGpuBuffer gpuBuffer = (VkGpuBuffer) gpuBufferSlice.buffer();

            assert ubo != null;
            ubo.setUseGlobalBuffer(false);
            ubo.getBufferSlice().set(gpuBuffer.buffer, gpuBufferSlice.offset(), (int) gpuBufferSlice.length());
        }

        for (TexelBufferDescriptor texelBufferDescriptor : pipeline.getTexelBufferDescriptors()) {
            GpuBufferSlice gpuBufferSlice = renderPass.uniforms.get(texelBufferDescriptor.name);
            if (gpuBufferSlice == null) {
                throw new IllegalStateException(
                        "Missing texel buffer " + texelBufferDescriptor.name + " for pipeline "
                                + renderPipeline.getLocation());
            }

            if (!(gpuBufferSlice.buffer() instanceof VkGpuBuffer gpuBuffer)) {
                throw new IllegalStateException(
                        "Texel buffer " + texelBufferDescriptor.name + " is not backed by Vulkan");
            }
            if (gpuBuffer.isClosed()) {
                throw new IllegalStateException("Texel buffer " + texelBufferDescriptor.name + " has been closed");
            }
            if ((gpuBuffer.usage() & GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER) == 0) {
                throw new IllegalStateException(
                        "Texel buffer " + texelBufferDescriptor.name + " lacks USAGE_UNIFORM_TEXEL_BUFFER");
            }

            texelBufferDescriptor.getBufferSlice().set(
                    gpuBuffer.buffer, gpuBufferSlice.offset(), Math.toIntExact(gpuBufferSlice.length()));
        }

        for (ImageDescriptor imageDescriptor : pipeline.getImageDescriptors()) {
            String uniformName = imageDescriptor.name;
            int samplerIndex = imageDescriptor.imageIdx;

            var textureSampler = renderPass.samplers.get(uniformName);
            if (textureSampler == null) {
                textureSampler = optionalEntitySampler(renderPipeline, uniformName);
            }
            if (textureSampler == null) {
                reportUnbound(renderPipeline, "sampler", uniformName, "not supplied by the render pass");
                continue;
            }

            VkTextureView textureView = textureSampler.view();
            VkGpuTexture gpuTexture = textureView.texture();
            if (gpuTexture.isClosed()) {
                reportUnbound(renderPipeline, "sampler", uniformName, "texture already closed");
                continue;
            }

            VTextureSelector.bindTexture(samplerIndex, gpuTexture.getVulkanImage());
            gpuTexture.getVulkanImage().setSampler(textureSampler.sampler().getId());
        }

    }

    private static final Identifier ENTITY_SHADER = Identifier.withDefaultNamespace("core/entity");

                                                                                          
                                                                                            
                                                                                              
                                                                                              
    private static @Nullable VkRenderPass.TextureViewAndSampler optionalEntitySampler(
            RenderPipeline renderPipeline, String name) {
        String unusedDefine = switch (name) {
            case "Sampler1" -> "NO_OVERLAY";
            case "Sampler2" -> "EMISSIVE";
            default -> null;
        };
        if (unusedDefine == null || renderPipeline.getSamplers().contains(name)
                || !ENTITY_SHADER.equals(renderPipeline.getVertexShader())
                || !ENTITY_SHADER.equals(renderPipeline.getFragmentShader())
                || !renderPipeline.getShaderDefines().flags().contains(unusedDefine)) {
            return null;
        }

        var gameRenderer = Minecraft.getInstance().gameRenderer;
        GpuTextureView view = name.equals("Sampler1")
                ? gameRenderer.overlayTexture().getTextureView()
                : gameRenderer.lightTexture().getTextureView();
        var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        return new VkRenderPass.TextureViewAndSampler((VkTextureView) view, (VkSampler) sampler);
    }

       
                                                                                                
                                                                                              
                                                                                                
                                    
       
    private static final Set<String> REPORTED_UNBOUND = ConcurrentHashMap.newKeySet();

    private static void reportUnbound(RenderPipeline renderPipeline, String kind, String name, String reason) {
        String key = renderPipeline.getLocation() + "/" + kind + "/" + name;

        if (REPORTED_UNBOUND.add(key)) {
            LOGGER.warn("VulkanMod: {} '{}' unbound for pipeline {} ({}); draws will sample undefined data",
                        kind, name, renderPipeline.getLocation(), reason);
        }
    }

    public boolean bindPipeline(RenderPipeline renderPipeline) {
        Pipeline pipeline = ExtendedRenderPipeline.of(renderPipeline).getPipeline();

        if (pipeline == null) {
            return false;
        }

        Renderer renderer = Renderer.getInstance();
        renderer.bindGraphicsPipeline((GraphicsPipeline) pipeline);
                                                        

        renderer.uploadAndBindUBOs(pipeline);

        return true;
    }

    public void applyPipelineState(RenderPipeline renderPipeline) {
        if (this.lastPipeline != renderPipeline) {
            this.lastPipeline = renderPipeline;
            if (renderPipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
                VRenderSystem.enableDepthTest();
                VRenderSystem.depthFunc(GlConst.toGl(renderPipeline.getDepthTestFunction()));
            } else {
                VRenderSystem.disableDepthTest();
            }

            if (renderPipeline.isCull()) {
                VRenderSystem.enableCull();
            } else {
                VRenderSystem.disableCull();
            }

            if (renderPipeline.getBlendFunction().isPresent()) {
                VRenderSystem.enableBlend();
                BlendFunction blendFunction = renderPipeline.getBlendFunction().get();
                VRenderSystem.blendFuncSeparate(
                        GlConst.toGl(blendFunction.sourceColor()),
                        GlConst.toGl(blendFunction.destColor()),
                        GlConst.toGl(blendFunction.sourceAlpha()),
                        GlConst.toGl(blendFunction.destAlpha())
                );
            } else {
                VRenderSystem.disableBlend();
            }

            VRenderSystem.setPolygonModeGL(GlConst.toGl(renderPipeline.getPolygonMode()));
            VRenderSystem.depthMask(renderPipeline.isWriteDepth());
            VRenderSystem.colorMask(renderPipeline.isWriteColor(), renderPipeline.isWriteColor(), renderPipeline.isWriteColor(), renderPipeline.isWriteAlpha());
            if (renderPipeline.getDepthBiasConstant() == 0.0F && renderPipeline.getDepthBiasScaleFactor() == 0.0F) {
                VRenderSystem.disablePolygonOffset();
            } else {
                VRenderSystem.polygonOffset(renderPipeline.getDepthBiasScaleFactor(), renderPipeline.getDepthBiasConstant());
                VRenderSystem.enablePolygonOffset();
            }

            switch (renderPipeline.getColorLogic()) {
                case NONE:
                    VRenderSystem.disableColorLogicOp();
                    break;
                case OR_REVERSE:
                    VRenderSystem.enableColorLogicOp();
                    VRenderSystem.logicOp(5387);
            }

            VRenderSystem.setPrimitiveTopologyGL(GlConst.toGl(renderPipeline.getVertexFormatMode()));
        }
    }

    public void finishRenderPass(boolean forceEnd) {
        if (forceEnd) {
            Renderer.getInstance().endRenderPass();
        }

        this.inRenderPass = false;
    }

    protected VkGpuDevice getDevice() {
        return this.device;
    }
}
