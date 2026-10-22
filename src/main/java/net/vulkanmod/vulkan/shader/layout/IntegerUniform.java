package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;


final class IntegerUniform extends Uniform {
    private final boolean booleanValue;

    IntegerUniform(Info info) {
        super(info);
        this.booleanValue = "bool".equals(info.type);
    }

    @Override
    void update(long ptr) {
        if (writeDefault(ptr)) return;
        MappedBuffer source = this.values == null ? null : this.values.get();
        if (source == null || !source.buffer.isDirect() || source.buffer.capacity() < this.size) {
            throw new IllegalArgumentException("Uniform " + this.info.name + " requires "
                    + this.size + " packed integer source bytes");
        }
        long destination = ptr + this.offset;
        if (this.booleanValue) {
            MemoryUtil.memPutInt(destination, MemoryUtil.memGetInt(source.ptr) == 0 ? 0 : 1);
        } else {
            MemoryUtil.memCopy(source.ptr, destination, this.size);
        }
    }
}
