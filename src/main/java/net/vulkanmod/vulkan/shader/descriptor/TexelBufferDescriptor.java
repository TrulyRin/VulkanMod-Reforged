package net.vulkanmod.vulkan.shader.descriptor;

import net.vulkanmod.vulkan.memory.buffer.BufferSlice;

import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_ALL_GRAPHICS;

   
                                                                                         
   
public final class TexelBufferDescriptor implements Descriptor {
    public final String name;
    public final int binding;
    private final BufferSlice bufferSlice = new BufferSlice();
    private int format = VK_FORMAT_UNDEFINED;

    public TexelBufferDescriptor(int binding, String name) {
        this.binding = binding;
        this.name = name;
    }

    @Override
    public int getBinding() {
        return this.binding;
    }

    @Override
    public int getType() {
        return VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER;
    }

    @Override
    public int getStages() {
        return VK_SHADER_STAGE_ALL_GRAPHICS;
    }

    public BufferSlice getBufferSlice() {
        return this.bufferSlice;
    }

    public int getFormat() {
        if (this.format == VK_FORMAT_UNDEFINED) {
            throw new IllegalStateException("Texel buffer format was not assigned for " + this.name);
        }
        return this.format;
    }

    public void setFormat(int format) {
        if (format == VK_FORMAT_UNDEFINED) {
            throw new IllegalArgumentException("Texel buffer format cannot be undefined for " + this.name);
        }
        this.format = format;
    }
}
