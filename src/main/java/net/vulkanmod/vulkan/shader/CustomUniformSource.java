package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.Objects;


public interface CustomUniformSource {
    enum ScalarKind { I32, U32, B32, F32, F64 }


    record Descriptor(ScalarKind kind, int columns, int rows, int arrayElements) {
        public Descriptor {
            Objects.requireNonNull(kind, "kind");
            if (columns < 1 || columns > 4 || rows < 1 || rows > 4)
                throw new IllegalArgumentException("Custom uniform columns and rows must be 1..4");
            if (arrayElements < 1)
                throw new IllegalArgumentException("Custom uniform array element count must be positive");
            Math.multiplyExact(Math.multiplyExact(columns, rows), arrayElements);
        }

        public int logicalComponentCount() {
            return Math.multiplyExact(Math.multiplyExact(columns, rows), arrayElements);
        }
    }

    Descriptor descriptor();


    boolean initialized();


    MappedBuffer view();


    float previewFloat(int component);


    MappedBuffer uploadBuffer();
}
