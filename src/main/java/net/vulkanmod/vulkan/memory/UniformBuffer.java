package net.vulkanmod.vulkan.memory;

import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.UniformUploadBatch;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;

public class UniformBuffer extends Buffer implements UniformUploadBatch.Destination {

    private final static int minOffset = (int) DeviceManager.deviceProperties.limits().minUniformBufferOffsetAlignment();

    public static int getAlignedSize(int uploadSize) {
        return UniformUploadBatch.alignedSize(uploadSize, minOffset);
    }

    public static int getAlignment() {
        return minOffset;
    }

    public UniformBuffer(int size, MemoryType memoryType) {
        super(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, memoryType);
        this.createBuffer(size);
    }

    public void checkCapacity(int size) {
        int required = UniformUploadBatch.requiredCapacity(this.bufferSize, this.usedBytes, size);
        if (required != this.bufferSize) resizeBuffer(required);
    }

    public void updateOffset(int alignedSize) {
        if (alignedSize < 0 || alignedSize > this.bufferSize - this.usedBytes)
            throw new IllegalArgumentException("Uniform upload exceeds reserved buffer capacity");
        usedBytes += alignedSize;
    }

    private void resizeBuffer(int newSize) {
        MemoryManager.getInstance().addToFreeable(this);
        createBuffer(newSize);
    }

    public long getPointer() {
        return this.data.get(0) + usedBytes;
    }
}
