package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Uniform {
    protected Supplier<MappedBuffer> values;
    private static final int[] ZERO_DEFAULT = new int[0];
    private int[] defaultWords;

    Info info;
    protected long offset;
    protected int size;

    Uniform(Info info) {
        this.info = info;
        this.offset = info.offset * 4L;
        this.size = info.size * 4;
        this.setSupplier();
    }

    void setSupplier() {
        this.values = switch (info.type) {
            case "mat4" -> Uniforms.mat4f_uniformMap.get(info.name);
            case "vec4" -> Uniforms.vec4f_uniformMap.get(info.name);
            case "vec3" -> Uniforms.vec3f_uniformMap.get(info.name);
            case "vec2" -> Uniforms.vec2f_uniformMap.get(info.name);

            default -> null;
        };
    }

    public void setSupplier(Supplier<MappedBuffer> supplier) {
        clearDefault();
        this.values = supplier;
    }

    public boolean hasSupplier() {
        return this.defaultWords != null || this.values != null;
    }

    public boolean hasExplicitSupplier() {
        return this.values != null;
    }

    public final void setZeroDefault() {
        this.defaultWords = ZERO_DEFAULT;
    }


    public final void setDefaultWords(int[] words) {
        int count = this.info.arrayLength > 0 ? this.info.arrayLength : "mat3".equals(this.info.type) ? 9 : this.info.size;
        if (words.length != count) throw new IllegalArgumentException("Wrong default word count for " + this.info.name);
        boolean zero = true;
        for (int word : words) zero &= word == 0;
        this.defaultWords = zero ? ZERO_DEFAULT : words.clone();
        if (!zero && "bool".equals(this.info.type)) {
            for (int i = 0; i < this.defaultWords.length; i++) {
                this.defaultWords[i] = this.defaultWords[i] == 0 ? 0 : 1;
            }
        }
    }

    protected final void clearDefault() {
        this.defaultWords = null;
    }

    protected final boolean writeDefault(long ptr) {
        if (this.defaultWords == null) return false;
        long destination = ptr + this.offset;
        if (this.defaultWords == ZERO_DEFAULT) {
            MemoryUtil.memSet(destination, 0, this.size);
        } else {
            for (int i = 0; i < this.defaultWords.length; i++) {
                long wordOffset = this.info.arrayLength > 0 ? (long) i * this.info.arrayStride
                        : "mat3".equals(this.info.type) ? (i / 3) * 4L + i % 3 : i;
                MemoryUtil.memPutInt(destination + wordOffset * 4, this.defaultWords[i]);
            }
        }
        return true;
    }

    public String getName() {
        return this.info.name;
    }

    void update(long ptr) {
        if (writeDefault(ptr)) return;
        MappedBuffer src = values.get();

        MemoryUtil.memCopy(src.ptr, ptr + this.offset, this.size);
    }

    public static Uniform createField(Info info) {
        if (info.arrayLength > 0) return new FixedFloatArray(info);
        return switch (info.type) {
            case "mat4", "vec3", "vec4", "vec2" -> new Uniform(info);
            case "mat3" -> new Mat3(info);
            case "float" -> new Vec1f(info);
            case "int" -> new Vec1i(info);
            case "bool", "ivec2", "ivec3", "ivec4" -> new IntegerUniform(info);
            default -> throw new RuntimeException("not admitted type: " + info.type);
        };
    }

    public int getOffset() {
        return info.offset;
    }

    public int getSize() { return info.size; }

    public String getType() { return info.type; }

    public int getArrayLength() { return info.arrayLength; }

    public String toString() {
        return String.format("%s: %s offset: %d", info.type, info.name, info.offset);
    }

    public static Info createUniformInfo(String type, String name, int count) {
        return switch (type) {
            case "matrix4x4" -> new Info("mat4", name, 4, 16);
            case "float" -> switch (count) {
                case 4 -> new Info("vec4", name, 4, 4);
                case 3 -> new Info("vec3", name, 4, 3);
                case 2 -> new Info("vec2", name, 2, 2);
                case 1 -> new Info("float", name, 1, 1);

                default -> throw new IllegalStateException("Unexpected value: " + count);
            };
            case "int" -> switch (count) {
                case 1 -> new Info("int", name, 1, 1);
                case 2 -> new Info("ivec2", name, 2, 2);
                case 3 -> new Info("ivec3", name, 4, 3);
                case 4 -> new Info("ivec4", name, 4, 4);
                default -> throw new IllegalArgumentException("Unsupported integer uniform component count for " + name + ": " + count);
            };
            default -> throw new RuntimeException("not admitted type..");
        };
    }

    public static Info createUniformInfo(String type, String name) {
        return switch (type) {
            case "mat4" -> new Info(type, name, 4, 16);

            case "mat3" -> new Info(type, name, 4, 12);

            case "vec4", "ivec4" -> new Info(type, name, 4, 4);
            case "vec3", "ivec3" -> new Info(type, name, 4, 3);
            case "vec2", "ivec2" -> new Info(type, name, 2, 2);

            case "float", "int", "bool" -> new Info(type, name, 1, 1);

            default -> throw new RuntimeException("not admitted type: " + type);
        };
    }


    public static Info createUniformArrayInfo(String type, String name, int length) {
        if (!"float".equals(type) || length <= 0 || length > Integer.MAX_VALUE / 16) {
            throw new IllegalArgumentException("Unsupported fixed uniform array: " + type + " " + name + "[" + length + "]");
        }
        return new Info(type, name, 4, length * 4, length, 4);
    }

    public static class Info {
        final String type;
        final String name;
        final int align;
        final int size;
        final int arrayLength;
        final int arrayStride;
        int offset;

        Info(String type, String name, int align, int size) {
            this(type, name, align, size, 0, 0);
        }

        Info(String type, String name, int align, int size, int arrayLength, int arrayStride) {
            this.type = type;
            this.name = name;
            this.align = align;
            this.size = size;
            this.arrayLength = arrayLength;
            this.arrayStride = arrayStride;
        }

        int getSizeBytes() { return 4 * this.size; }

        int computeAlignmentOffset(int builderOffset) {
            return this.offset = builderOffset + ((align - (builderOffset % align)) % align);
        }
    }
}
