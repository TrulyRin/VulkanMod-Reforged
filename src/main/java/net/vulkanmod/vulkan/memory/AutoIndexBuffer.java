package net.vulkanmod.vulkan.memory;

import net.vulkanmod.Initializer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class AutoIndexBuffer {
    public static final int U16_MAX_INDEX_COUNT = 65536;
    public static final int QUAD_U16_MAX_VERTEX_COUNT = U16_MAX_INDEX_COUNT;

    int vertexCount;
    DrawType drawType;
    IndexBuffer indexBuffer;

    public AutoIndexBuffer(int vertexCount, DrawType type) {
        this.drawType = type;

        createIndexBuffer(vertexCount);
    }

    private void createIndexBuffer(int vertexCount) {
        IndexBuffer.IndexType indexType = vertexCount <= U16_MAX_INDEX_COUNT
                ? IndexBuffer.IndexType.SHORT : IndexBuffer.IndexType.INT;
        int bytes = checkedIndexBytes(vertexCount, indexType.size);
        ByteBuffer buffer = bytes == 0 ? MemoryUtil.memAlloc(0)
                : indexType == IndexBuffer.IndexType.INT ? genIntIndices(vertexCount)
                : switch (this.drawType) {
                    case QUADS -> genQuadIndices(vertexCount);
                    case TRIANGLE_FAN -> genTriangleFanIndices(vertexCount);
                    case TRIANGLE_STRIP -> genTriangleStripIndices(vertexCount);
                    case LINES -> genLinesIndices(vertexCount);
                    case DEBUG_LINE_STRIP -> genDebugLineStripIndices(vertexCount);
                    default -> throw new IllegalArgumentException("Unsupported drawType: %s".formatted(this.drawType));
                };
        IndexBuffer replacement = null;
        try {
            replacement = new IndexBuffer(bytes, MemoryTypes.GPU_MEM, indexType);
            replacement.copyBuffer(buffer);
        } catch (RuntimeException | Error failure) {
            if (replacement != null) {
                try { replacement.freeBuffer(); }
                catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            }
            throw failure;
        } finally {
            MemoryUtil.memFree(buffer);
        }
        IndexBuffer previous = this.indexBuffer;
        this.indexBuffer = replacement;
        this.vertexCount = vertexCount;
        if (previous != null) previous.freeBuffer();
    }

    public void checkCapacity(int vertexCount) {
        if (vertexCount < 0) throw new IllegalArgumentException("Negative automatic vertex capacity");
        if(vertexCount > this.vertexCount) {
            long growth = Math.max((long) vertexCount, (long) this.vertexCount * 2);
            if (growth > Integer.MAX_VALUE) throw new IllegalArgumentException("Automatic vertex capacity exceeds supported integer range");
            int newVertexCount = (int) growth;
            Initializer.LOGGER.info("Reallocating AutoIndexBuffer from {} to {}", this.vertexCount, newVertexCount);

            createIndexBuffer(newVertexCount);
        }
    }

    private int checkedIndexBytes(int vertices, int width) {
        if (vertices < 0) throw new IllegalArgumentException("Negative automatic vertex capacity");
        long indices = switch (this.drawType) {
            case QUADS, LINES -> ((long) vertices + 3) / 4 * 6;
            case TRIANGLE_FAN, TRIANGLE_STRIP -> Math.max(0L, (long) vertices - 2) * 3;
            case DEBUG_LINE_STRIP -> Math.max(0L, (long) vertices - 1) * 2;
            default -> throw new IllegalArgumentException("Unsupported drawType: %s".formatted(this.drawType));
        };
        long bytes = indices * width;
        if (bytes > Integer.MAX_VALUE) throw new IllegalArgumentException("Automatic index storage exceeds supported integer range");
        return (int) bytes;
    }

    private ByteBuffer genIntIndices(int vertexCount) {
        if (this.drawType == DrawType.QUADS) return genIntQuadIndices(vertexCount);
        ByteBuffer buffer = MemoryUtil.memAlloc(checkedIndexBytes(vertexCount, Integer.BYTES));
        try {
            IntBuffer indices = buffer.asIntBuffer();
            switch (this.drawType) {
                case LINES -> {
                    for (int i = 0; i < vertexCount; i += 4)
                        indices.put(i).put(i + 1).put(i + 2).put(i + 3).put(i + 2).put(i + 1);
                }
                case TRIANGLE_FAN -> {
                    for (int i = 1; i + 1 < vertexCount; i++) indices.put(0).put(i).put(i + 1);
                }
                case TRIANGLE_STRIP -> {
                    for (int i = 0; i + 2 < vertexCount; i++) {
                        indices.put((i & 1) == 0 ? i : i + 1);
                        indices.put((i & 1) == 0 ? i + 1 : i);
                        indices.put(i + 2);
                    }
                }
                case DEBUG_LINE_STRIP -> {
                    for (int i = 0; i + 1 < vertexCount; i++) indices.put(i).put(i + 1);
                }
                default -> throw new IllegalArgumentException("Unsupported drawType: %s".formatted(this.drawType));
            }
            return buffer;
        } catch (RuntimeException | Error failure) {
            MemoryUtil.memFree(buffer);
            throw failure;
        }
    }

    public static ByteBuffer genQuadIndices(int vertexCount) {
        int indexCount = vertexCount * 3 / 2;
        indexCount = roundUpToDivisible(indexCount, 6);

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for(int i = 0; i < vertexCount; i += 4) {
            idxs.put(j + 0, (short) (i));
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));
            idxs.put(j + 3, (short) (i));
            idxs.put(j + 4, (short) (i + 2));
            idxs.put(j + 5, (short) (i + 3));

            j += 6;
        }

        return buffer;
    }

    public static ByteBuffer genIntQuadIndices(int vertexCount) {
        int indexCount = vertexCount * 3 / 2;
        indexCount = roundUpToDivisible(indexCount, 6);

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Integer.BYTES);
        IntBuffer idxs = buffer.asIntBuffer();

        int j = 0;
        for(int i = 0; i < vertexCount; i += 4) {
            idxs.put(j + 0, (i));
            idxs.put(j + 1, (i + 1));
            idxs.put(j + 2, (i + 2));
            idxs.put(j + 3, (i));
            idxs.put(j + 4, (i + 2));
            idxs.put(j + 5, (i + 3));

            j += 6;
        }

        return buffer;
    }

    public static ByteBuffer genLinesIndices(int vertexCount) {
        int indexCount = vertexCount * 3 / 2;
        indexCount = roundUpToDivisible(indexCount, 6);

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for(int i = 0; i < vertexCount; i += 4) {
            idxs.put(j + 0, (short) (i));
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));
            idxs.put(j + 3, (short) (i + 3));
            idxs.put(j + 4, (short) (i + 2));
            idxs.put(j + 5, (short) (i + 1));

            j += 6;
        }

        return buffer;
    }

    public static ByteBuffer genTriangleFanIndices(int vertexCount) {
        int indexCount = (vertexCount - 2) * 3;
        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount - 2; ++i) {
            idxs.put(j + 0, (short) 0);
            idxs.put(j + 1, (short) (i + 1));
            idxs.put(j + 2, (short) (i + 2));

            j += 3;
        }

        return buffer;
    }

    public static ByteBuffer genTriangleStripIndices(int vertexCount) {
        int indexCount = (vertexCount - 2) * 3;

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount - 2; ++i) {
            if ((i & 1) == 0) {
                idxs.put(j + 0, (short) i);
                idxs.put(j + 1, (short) (i + 1));
                idxs.put(j + 2, (short) (i + 2));
            } else {
                idxs.put(j + 0, (short) (i + 1));
                idxs.put(j + 1, (short) i);
                idxs.put(j + 2, (short) (i + 2));
            }

            j += 3;
        }

        return buffer;
    }

    public static ByteBuffer genDebugLineStripIndices(int vertexCount) {
        int indexCount = (vertexCount - 1) * 2;

        ByteBuffer buffer = MemoryUtil.memAlloc(indexCount * Short.BYTES);
        ShortBuffer idxs = buffer.asShortBuffer();

        int j = 0;
        for (int i = 0; i < vertexCount - 1; ++i) {
            idxs.put(j + 0, (short) i);
            idxs.put(j + 1, (short) (i + 1));

            j += 2;
        }

        return buffer;
    }

    public static int roundUpToDivisible(int n, int d) {
        return ((n + d - 1) / d) * d;
    }

    public IndexBuffer getIndexBuffer() { return this.indexBuffer; }

    public void freeBuffer() {
        this.indexBuffer.freeBuffer();
    }

    public enum DrawType {
        QUADS(7),
        TRIANGLE_FAN(6),
        TRIANGLE_STRIP(5),
        DEBUG_LINE_STRIP(4),
        DEBUG_LINES(3),
        LINES(1);

        public final int n;

        DrawType(int n) {
            this.n = n;
        }

        public static int getIndexCount(DrawType drawType, int vertexCount) {
            return switch (drawType) {
                case QUADS, LINES -> vertexCount * 3 / 2;
                case TRIANGLE_FAN, TRIANGLE_STRIP -> (vertexCount - 2) * 3;
                default -> 0;
            };
        }

        public static int getQuadIndexCount(int vertexCount) {
            return vertexCount * 3 / 2;
        }

        public static int getTriangleStripIndexCount(int vertexCount) {
            return (vertexCount - 2) * 3;
        }
    }
}
