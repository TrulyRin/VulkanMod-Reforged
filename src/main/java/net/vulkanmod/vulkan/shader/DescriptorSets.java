package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.BufferSlice;
import net.vulkanmod.vulkan.memory.buffer.UniformBuffer;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.TexelBufferDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;

public class DescriptorSets {
    private static final VkDevice DEVICE = Vulkan.getVkDevice();

    private final Pipeline pipeline;
    private int poolSize = 10;
    private long descriptorPool = VK_NULL_HANDLE;
    private long[] sets;
    private long currentSet;
    private int currentIdx = -1;

    private final long[] boundUBs;
    private final ImageDescriptor.State[] boundTextures;
    private final TexelBufferState[] boundTexelBuffers;
    private final IntBuffer dynamicOffsets;

    DescriptorSets(Pipeline pipeline) {
        this.pipeline = pipeline;
        this.boundTextures = new ImageDescriptor.State[pipeline.imageDescriptors.size()];
        this.boundTexelBuffers = new TexelBufferState[pipeline.texelBufferDescriptors.size()];
        this.dynamicOffsets = MemoryUtil.memAllocInt(pipeline.buffers.size());
        this.boundUBs = new long[pipeline.buffers.size()];

        Arrays.setAll(boundTextures, i -> new ImageDescriptor.State(0, 0));
        Arrays.setAll(boundTexelBuffers, i -> new TexelBufferState(0, 0, 0));

        try (MemoryStack stack = stackPush()) {
            this.createDescriptorPool(stack);
            this.createDescriptorSets(stack);
        }
    }

    public void bindSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int bindPoint) {
        try (MemoryStack stack = stackPush()) {

            this.updateUniforms(uniformBuffer);
            this.updateDescriptorSet(stack, uniformBuffer);

            vkCmdBindDescriptorSets(commandBuffer, bindPoint, pipeline.pipelineLayout,
                                    0, stack.longs(currentSet), dynamicOffsets);
        }
    }

    private void updateUniforms(UniformBuffer globalUB) {
        int i = 0;
        for (UBO ubo : pipeline.getBuffers()) {
                                                                
            if (ubo.getBufferSlice().getBuffer() == null) {
                ubo.setUseGlobalBuffer(true);
                ubo.setUpdate(true);
            }

            boolean useOwnUB = !ubo.useGlobalBuffer();

            int offset;
            if (useOwnUB) {
                BufferSlice bufferSlice = ubo.getBufferSlice();
                offset = (int) bufferSlice.getOffset();
            }
            else {
                offset = (int) globalUB.getUsedBytes();
                int alignedSize = UniformBuffer.getAlignedSize(ubo.getSize());
                globalUB.checkCapacity(alignedSize);

                if (ubo.shouldUpdate()) {
                    ubo.update(globalUB.getPointer());
                }
                globalUB.updateOffset(alignedSize);

                BufferSlice bufferSlice = ubo.getBufferSlice();
                bufferSlice.set(globalUB, offset, alignedSize);
            }

            this.dynamicOffsets.put(i, offset);

            ++i;
        }
    }

    private boolean needsUpdate(UniformBuffer uniformBuffer) {
        if (currentIdx == -1)
            return true;

        for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
            ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
            VulkanImage image = imageDescriptor.getImage();

            if (image == null) {
                throw new NullPointerException(
                        "Pipeline %s: image descriptor %s (binding %d) has no image bound"
                                .formatted(pipeline.name, imageDescriptor.name, imageDescriptor.getBinding()));
            }

            long view = imageDescriptor.getImageView(image);
            long sampler = image.getSampler();

            if (imageDescriptor.isReadOnlyLayout)
                image.readOnlyLayout();

            if (!this.boundTextures[j].isCurrentState(view, sampler)) {
                return true;
            }
        }

        for (int j = 0; j < pipeline.texelBufferDescriptors.size(); ++j) {
            TexelBufferDescriptor descriptor = pipeline.texelBufferDescriptors.get(j);
            BufferSlice slice = descriptor.getBufferSlice();
            Buffer buffer = slice.getBuffer();
            if (buffer == null) {
                throw new IllegalStateException(
                        "Pipeline %s: texel buffer %s (binding %d) has no buffer bound"
                                .formatted(pipeline.name, descriptor.name, descriptor.getBinding()));
            }
            if (!this.boundTexelBuffers[j].isCurrent(buffer.getId(), slice.getOffset(), slice.getSize())) {
                return true;
            }
        }

        for (int j = 0; j < pipeline.buffers.size(); ++j) {
            UBO ubo = pipeline.buffers.get(j);
            Buffer uniformBufferI = ubo.getBufferSlice().getBuffer();


            if (uniformBufferI == null)
                uniformBufferI = uniformBuffer;

            if (this.boundUBs[j] != uniformBufferI.getId()) {
                return true;
            }
        }

        return false;
    }

    private void checkPoolSize(MemoryStack stack) {
        if (this.currentIdx >= this.poolSize) {
            this.poolSize *= 2;

            this.createDescriptorPool(stack);
            this.createDescriptorSets(stack);
            this.currentIdx = 0;
        }
    }

    private void updateDescriptorSet(MemoryStack stack, UniformBuffer uniformBuffer) {

                                    
        if (!needsUpdate(uniformBuffer))
            return;

        this.currentIdx++;

                          
        checkPoolSize(stack);

        this.currentSet = this.sets[this.currentIdx];

        VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(
                pipeline.buffers.size() + pipeline.imageDescriptors.size() + pipeline.texelBufferDescriptors.size(), stack);
        VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[pipeline.buffers.size()];

                                                       
        int i = 0;
        for (UBO ubo : pipeline.getBuffers()) {
            Buffer ub = ubo.getBufferSlice().getBuffer();
            boundUBs[i] = ub.getId();

            bufferInfos[i] = VkDescriptorBufferInfo.calloc(1, stack);
            bufferInfos[i].buffer(boundUBs[i]);
            bufferInfos[i].range(ubo.getSize());

            VkWriteDescriptorSet descriptorWrite = descriptorWrites.get(i);
            descriptorWrite.sType$Default();
            descriptorWrite.dstBinding(ubo.getBinding());
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.descriptorType(ubo.getType());
            descriptorWrite.descriptorCount(1);
            descriptorWrite.pBufferInfo(bufferInfos[i]);
            descriptorWrite.dstSet(currentSet);

            ++i;
        }

        VkDescriptorImageInfo.Buffer[] imageInfo = new VkDescriptorImageInfo.Buffer[pipeline.imageDescriptors.size()];

        for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
            ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
            VulkanImage image = imageDescriptor.getImage();

            if (image == null) {
                throw new NullPointerException(
                        "Pipeline %s: image descriptor %s (binding %d) has no image bound"
                                .formatted(pipeline.name, imageDescriptor.name, imageDescriptor.getBinding()));
            }

            long view = imageDescriptor.getImageView(image);
            long sampler = image.getSampler();
            int layout = imageDescriptor.getLayout();

            if (imageDescriptor.isReadOnlyLayout)
                image.readOnlyLayout();

            imageInfo[j] = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo[j].imageLayout(layout);
            imageInfo[j].imageView(view);

            if (imageDescriptor.useSampler) {
                imageInfo[j].sampler(sampler);
            }

            VkWriteDescriptorSet descriptorWrite = descriptorWrites.get(i);
            descriptorWrite.sType$Default();
            descriptorWrite.dstBinding(imageDescriptor.getBinding());
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.descriptorType(imageDescriptor.getType());
            descriptorWrite.descriptorCount(1);
            descriptorWrite.pImageInfo(imageInfo[j]);
            descriptorWrite.dstSet(currentSet);

            this.boundTextures[j].set(view, sampler);
            ++i;
        }

        LongBuffer[] texelBufferViews = new LongBuffer[pipeline.texelBufferDescriptors.size()];
        for (int j = 0; j < pipeline.texelBufferDescriptors.size(); ++j) {
            TexelBufferDescriptor descriptor = pipeline.texelBufferDescriptors.get(j);
            BufferSlice slice = descriptor.getBufferSlice();
            Buffer buffer = slice.getBuffer();
            if (buffer == null) {
                throw new IllegalStateException(
                        "Pipeline %s: texel buffer %s (binding %d) has no buffer bound"
                                .formatted(pipeline.name, descriptor.name, descriptor.getBinding()));
            }

            long view = buffer.getOrCreateBufferView(descriptor.getFormat(), slice.getOffset(), slice.getSize());
            texelBufferViews[j] = stack.longs(view);

            VkWriteDescriptorSet descriptorWrite = descriptorWrites.get(i);
            descriptorWrite.sType$Default();
            descriptorWrite.dstBinding(descriptor.getBinding());
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.descriptorType(descriptor.getType());
            descriptorWrite.descriptorCount(1);
            descriptorWrite.pTexelBufferView(texelBufferViews[j]);
            descriptorWrite.dstSet(currentSet);

            this.boundTexelBuffers[j].set(buffer.getId(), slice.getOffset(), slice.getSize());
            ++i;
        }

        vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
    }

    private void createDescriptorSets(MemoryStack stack) {
        LongBuffer layouts = MemoryUtil.memAllocLong(this.poolSize);

        for (int i = 0; i < this.poolSize; ++i) {
            layouts.put(i, pipeline.descriptorSetLayout);
        }

        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
        allocInfo.sType$Default();
        allocInfo.descriptorPool(descriptorPool);
        allocInfo.pSetLayouts(layouts);

                                           
        this.sets = new long[this.poolSize];

        int result = vkAllocateDescriptorSets(DEVICE, allocInfo, this.sets);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
        }

        MemoryUtil.memFree(layouts);
    }

    private void createDescriptorPool(MemoryStack stack) {
        int size = pipeline.buffers.size() + pipeline.imageDescriptors.size() + pipeline.texelBufferDescriptors.size();

        VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(size, stack);

        int i = 0;
        for (var buffer : pipeline.getBuffers()) {
            VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(i);
            uniformBufferPoolSize.type(buffer.getType());
            uniformBufferPoolSize.descriptorCount(this.poolSize);

            ++i;
        }

        for (var imageDescriptor : pipeline.getImageDescriptors()) {
            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(i);
            textureSamplerPoolSize.type(imageDescriptor.getType());
            textureSamplerPoolSize.descriptorCount(this.poolSize);

            ++i;
        }

        for (var texelBufferDescriptor : pipeline.getTexelBufferDescriptors()) {
            VkDescriptorPoolSize texelBufferPoolSize = poolSizes.get(i);
            texelBufferPoolSize.type(texelBufferDescriptor.getType());
            texelBufferPoolSize.descriptorCount(this.poolSize);

            ++i;
        }

        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
        poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
        poolInfo.pPoolSizes(poolSizes);
        poolInfo.maxSets(this.poolSize);

        LongBuffer pDescriptorPool = stack.mallocLong(1);

        if (vkCreateDescriptorPool(DEVICE, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create descriptor pool");
        }

        if (this.descriptorPool != VK_NULL_HANDLE) {
            final long oldDescriptorPool = this.descriptorPool;
            MemoryManager.getInstance().addFrameOp(() -> {
                vkDestroyDescriptorPool(DEVICE, oldDescriptorPool, null);
            });
        }

        this.descriptorPool = pDescriptorPool.get(0);
    }

    public void resetIdx() {
        this.currentIdx = -1;
    }

    public void cleanUp() {
        vkResetDescriptorPool(DEVICE, descriptorPool, 0);
        vkDestroyDescriptorPool(DEVICE, descriptorPool, null);

        MemoryUtil.memFree(this.dynamicOffsets);
    }

    private static final class TexelBufferState {
        private long buffer;
        private long offset;
        private int size;

        private TexelBufferState(long buffer, long offset, int size) {
            this.set(buffer, offset, size);
        }

        private boolean isCurrent(long buffer, long offset, int size) {
            return this.buffer == buffer && this.offset == offset && this.size == size;
        }

        private void set(long buffer, long offset, int size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
    }

}
