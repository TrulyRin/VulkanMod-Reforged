package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.function.Supplier;


public final class LegacyProjectionSupplier implements Supplier<MappedBuffer> {
    private static final int MATRIX_BYTES = 16 * Float.BYTES;
    private final Supplier<MappedBuffer> source;
    private final MappedBuffer converted;

    public LegacyProjectionSupplier(MappedBuffer source) {
        requireMatrix(Objects.requireNonNull(source, "source"));
        this.source = () -> source;
        this.converted = allocateConverted();
    }

    private LegacyProjectionSupplier(Supplier<MappedBuffer> source) {
        this.source = Objects.requireNonNull(source, "source");
        this.converted = allocateConverted();
    }

    public static LegacyProjectionSupplier fromSource(Supplier<MappedBuffer> source) {
        return new LegacyProjectionSupplier(source);
    }

    private static void requireMatrix(MappedBuffer source) {
        if (source == null) throw new IllegalArgumentException("A legacy projection source is missing");
        if (!source.buffer.isDirect() || source.buffer.capacity() < MATRIX_BYTES) {
            throw new IllegalArgumentException("A legacy projection requires at least 64 bytes of direct matrix storage");
        }
    }

    private static MappedBuffer allocateConverted() {
        return MappedBuffer.createFromBuffer(ByteBuffer.allocateDirect(MATRIX_BYTES).order(ByteOrder.nativeOrder()));
    }

    @Override
    public MappedBuffer get() {
        MappedBuffer source = this.source.get();
        requireMatrix(source);

        for (int offset = 0; offset < MATRIX_BYTES; offset += Float.BYTES) {
            converted.putFloat(offset, source.getFloat(offset));
        }

        for (int column = 0; column < 4; column++) {
            int zOffset = (column * 4 + 2) * Float.BYTES;
            converted.putFloat(zOffset,
                    (converted.getFloat(zOffset) + converted.getFloat(zOffset + Float.BYTES)) * .5f);
        }
        return converted;
    }
}
