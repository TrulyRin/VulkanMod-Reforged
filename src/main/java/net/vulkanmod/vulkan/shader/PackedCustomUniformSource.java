package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;





public final class PackedCustomUniformSource implements CustomUniformSource {
    private final Descriptor descriptor;
    private final int componentCount;
    private final ByteBuffer writer;
    private final MappedBuffer view;
    private Object pending;
    private boolean pendingTranspose;
    private boolean initialized;

    public PackedCustomUniformSource(Descriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor, "descriptor");
        this.componentCount = descriptor.logicalComponentCount();
        int scalarBytes = descriptor.kind() == ScalarKind.F64 ? Double.BYTES : Integer.BYTES;
        int byteCount = Math.multiplyExact(this.componentCount, scalarBytes);
        this.writer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());

        this.view = MappedBuffer.createFromBuffer(this.writer.duplicate().order(ByteOrder.nativeOrder()));
    }

    @Override public Descriptor descriptor() { return this.descriptor; }
    @Override public boolean initialized() { return this.initialized; }
    @Override public MappedBuffer view() { return this.view; }

    @Override public float previewFloat(int component) {
        if (!this.initialized) throw new IllegalStateException("Custom uniform owner has no accepted value");
        if (this.descriptor.kind() != ScalarKind.F32)
            throw new IllegalStateException("Custom uniform float preview requires F32 values");
        Objects.checkIndex(component, this.componentCount);
        Object payload = this.pending;
        if (payload != null)
            return ((float[]) payload)[sourceIndex(component, this.pendingTranspose && this.descriptor.columns() > 1)];
        return Float.intBitsToFloat(this.writer.getInt(component * Float.BYTES));
    }


    public void accepted(Object primitiveArray, boolean transpose) {
        int count = switch (this.descriptor.kind()) {
            case I32, U32, B32 -> {
                if (!(primitiveArray instanceof int[] words)) throw wrongCarrier("int[]");
                yield words.length;
            }
            case F32 -> {
                if (!(primitiveArray instanceof float[] values)) throw wrongCarrier("float[]");
                yield values.length;
            }
            case F64 -> {
                if (!(primitiveArray instanceof double[] values)) throw wrongCarrier("double[]");
                yield values.length;
            }
        };
        if (count != this.componentCount)
            throw new IllegalArgumentException("Custom uniform " + this.descriptor.kind() + " requires "
                    + this.componentCount + " components, got " + count);
        this.pending = primitiveArray;
        this.pendingTranspose = transpose;
        this.initialized = true;
    }

    @Override public MappedBuffer uploadBuffer() {
        if (!this.initialized) throw new IllegalStateException("Custom uniform owner has no accepted value");
        Object payload = this.pending;
        if (payload != null) {
            pack(payload, this.pendingTranspose);
            this.pending = null;
        }
        return this.view;
    }

    private void pack(Object payload, boolean transpose) {

        boolean reorder = transpose && this.descriptor.columns() > 1;
        switch (this.descriptor.kind()) {
            case I32, U32, B32 -> {
                int[] words = (int[]) payload;
                boolean bool = this.descriptor.kind() == ScalarKind.B32;
                for (int i = 0; i < this.componentCount; i++) {
                    int word = words[sourceIndex(i, reorder)];
                    if (bool) word = word == 0 ? 0 : 1;
                    this.writer.putInt(i * Integer.BYTES, word);
                }
            }
            case F32 -> {
                float[] values = (float[]) payload;
                for (int i = 0; i < this.componentCount; i++)
                    this.writer.putInt(i * Float.BYTES, Float.floatToRawIntBits(values[sourceIndex(i, reorder)]));
            }
            case F64 -> {
                double[] values = (double[]) payload;
                for (int i = 0; i < this.componentCount; i++)
                    this.writer.putLong(i * Double.BYTES, Double.doubleToRawLongBits(values[sourceIndex(i, reorder)]));
            }
        }
    }

    private int sourceIndex(int destination, boolean transpose) {
        if (!transpose) return destination;
        int columns = this.descriptor.columns(), rows = this.descriptor.rows();
        int matrixSize = columns * rows;
        int matrixBase = destination / matrixSize * matrixSize;
        int component = destination % matrixSize;
        return matrixBase + (component % rows) * columns + component / rows;
    }

    private IllegalArgumentException wrongCarrier(String expected) {
        return new IllegalArgumentException("Custom uniform " + this.descriptor.kind() + " requires " + expected);
    }
}
