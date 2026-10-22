package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;


final class FixedFloatArray extends Uniform {
    FixedFloatArray(Info info) {
        super(info);
    }

    @Override
    void update(long ptr) {
        if (writeDefault(ptr)) return;
        MappedBuffer source = this.values == null ? null : this.values.get();
        int packedBytes = this.info.arrayLength * Float.BYTES;
        if (source == null || !source.buffer.isDirect() || source.buffer.capacity() < packedBytes) {
            throw new IllegalArgumentException("Uniform array " + this.info.name
                    + " requires " + packedBytes + " packed source bytes");
        }
        long destination = ptr + this.offset;
        for (int i = 0; i < this.info.arrayLength; i++) {
            MemoryUtil.memCopy(source.ptr + (long) i * Float.BYTES,
                    destination + (long) i * this.info.arrayStride * Float.BYTES, Float.BYTES);
        }
    }
}
