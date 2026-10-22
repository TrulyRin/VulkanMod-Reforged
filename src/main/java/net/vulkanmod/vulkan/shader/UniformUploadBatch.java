package net.vulkanmod.vulkan.shader;

import java.nio.IntBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.List;
import java.util.Objects;


public final class UniformUploadBatch {
    private UniformUploadBatch() { }

    public interface Destination {
        long getUsedBytes();

        void checkCapacity(int bytes);
        long getPointer();
        void updateOffset(int bytes);
    }

    public interface Block {
        int getSize();
        Destination getUniformBuffer();
        void update(long pointer);
    }








    public static void update(List<? extends Block> blocks, Destination global, IntBuffer offsets, int alignment) {
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(global, "global");
        Objects.requireNonNull(offsets, "offsets");
        if (alignment <= 0) throw new IllegalArgumentException("Uniform alignment must be positive");
        int count = blocks.size();
        if (count > offsets.limit()) throw new IllegalArgumentException("Insufficient uniform offset entries");
        if (offsets.isReadOnly()) throw new ReadOnlyBufferException();

        long start = checkedEnd(global.getUsedBytes(), 0);
        long cursor = start;
        long peak = start;
        for (int i = 0; i < count; i++) {
            Block block = Objects.requireNonNull(blocks.get(i), "block");
            int size = alignedSize(block.getSize(), alignment);
            Destination own = block.getUniformBuffer();
            if (own == null) {
                cursor = checkedEnd(cursor, size);
                peak = Math.max(peak, cursor);
            } else if (own == global) {
                peak = Math.max(peak, checkedEnd(cursor, size));
            } else {
                checkedEnd(own.getUsedBytes(), size);
            }
        }


        global.checkCapacity((int) (peak - start));
        for (int i = 0; i < count; i++) {
            Block block = blocks.get(i);
            int size = alignedSize(block.getSize(), alignment);
            Destination own = block.getUniformBuffer();
            if (own != null) {
                if (own != global) own.checkCapacity(size);
                offsets.put(i, (int) own.getUsedBytes());
            } else {
                offsets.put(i, (int) global.getUsedBytes());
                block.update(global.getPointer());
                global.updateOffset(size);
            }
        }
    }

    public static int alignedSize(int bytes, int alignment) {
        if (bytes < 0) throw new IllegalArgumentException("Uniform size cannot be negative");
        if (alignment <= 0) throw new IllegalArgumentException("Uniform alignment must be positive");
        long aligned = ((long) bytes + alignment - 1) / alignment * alignment;
        if (aligned > Integer.MAX_VALUE) throw new IllegalArgumentException("Aligned uniform size exceeds the integer range");
        return (int) aligned;
    }

    public static int requiredCapacity(int capacity, int used, int additional) {
        if (capacity < 0 || used < 0 || additional < 0 || used > capacity)
            throw new IllegalArgumentException("Invalid uniform buffer capacity or used/additional byte count");
        long required = (long) used + additional;
        if (required > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Uniform buffer capacity exceeds the integer range");
        if (required <= capacity) return capacity;
        long doubled = Math.max(1, capacity) * 2L;
        return (int) Math.max(required, Math.min(Integer.MAX_VALUE, doubled));
    }

    private static long checkedEnd(long used, int additional) {
        if (used < 0 || used > Integer.MAX_VALUE || used + additional > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Uniform buffer byte range exceeds the integer range");
        return used + additional;
    }
}
