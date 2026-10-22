package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.shaders.Uniform;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.Objects;
import java.util.function.Supplier;


public final class LegacyUniformSupplier implements Supplier<MappedBuffer> {
    private final net.vulkanmod.vulkan.shader.layout.Uniform destination;
    private final Supplier<Uniform> owners;
    private final Supplier<MappedBuffer> ordinaryStorage;
    private Uniform owner;
    private CustomUniformSource custom;
    private MappedBuffer ordinary;

    public LegacyUniformSupplier(net.vulkanmod.vulkan.shader.layout.Uniform destination,
                                 Supplier<Uniform> owners, Supplier<MappedBuffer> ordinaryStorage) {
        this.destination = Objects.requireNonNull(destination, "destination");
        this.owners = Objects.requireNonNull(owners, "owners");
        this.ordinaryStorage = Objects.requireNonNull(ordinaryStorage, "ordinaryStorage");
        replaceOwner(owners.get());
    }

    @Override
    public MappedBuffer get() {
        Uniform current = this.owners.get();
        if (current != this.owner) replaceOwner(current);
        return this.custom == null ? this.ordinary : this.custom.uploadBuffer();
    }

    private void replaceOwner(Uniform current) {
        if (current == null) throw invalid("owner was removed");
        CustomUniformSource nextCustom = current instanceof CustomUniformSource source ? source : null;
        MappedBuffer nextOrdinary = null;
        if (nextCustom != null) {
            if (!nextCustom.initialized()) throw invalid("custom owner has no initialized value");
            validate(nextCustom.descriptor());
            requireStorage(nextCustom.view(), Math.multiplyExact(nextCustom.descriptor().logicalComponentCount(), Integer.BYTES));
        } else {
            nextOrdinary = this.ordinaryStorage.get();
            if (nextOrdinary == null) throw invalid("owner has no supported buffer type " + current.getType());
            CustomUniformSource.Descriptor descriptor = vanillaDescriptor(current.getType(), nextOrdinary);
            validate(descriptor);
            requireStorage(nextOrdinary, Math.multiplyExact(descriptor.logicalComponentCount(), Integer.BYTES));
        }
        this.owner = current;
        this.custom = nextCustom;
        this.ordinary = nextOrdinary;
    }

    private CustomUniformSource.Descriptor vanillaDescriptor(int type, MappedBuffer storage) {
        if (type < 0 || type > 10) throw invalid("unsupported native owner type " + type);
        var kind = type <= 3 ? CustomUniformSource.ScalarKind.I32 : CustomUniformSource.ScalarKind.F32;
        int columns = type >= 8 ? type - 6 : 1;
        int rows = type >= 8 ? columns : type <= 3 ? type + 1 : type - 3;
        int components = columns * rows;
        if (storage.buffer.capacity() % (components * Integer.BYTES) != 0) throw invalid("native owner has an incomplete value");
        int elements = storage.buffer.capacity() / (components * Integer.BYTES);
        if (elements < 1) throw invalid("native owner has no complete value");
        return new CustomUniformSource.Descriptor(kind, columns, rows, elements);
    }

    private void validate(CustomUniformSource.Descriptor descriptor) {
        requireMatches(descriptor, this.destination.getType(), this.destination.getArrayLength(), this.destination.getName());
    }

    public static void requireMatches(CustomUniformSource.Descriptor descriptor, String type, int arrayLength, String name) {
        requireSupported(descriptor, name);
        var family = switch (type) {
            case "float", "vec2", "vec3", "vec4", "mat3", "mat4" -> CustomUniformSource.ScalarKind.F32;
            case "int", "ivec2", "ivec3", "ivec4" -> CustomUniformSource.ScalarKind.I32;
            case "bool" -> descriptor.kind() == CustomUniformSource.ScalarKind.B32
                    ? CustomUniformSource.ScalarKind.B32 : CustomUniformSource.ScalarKind.I32;
            default -> throw new IllegalArgumentException("Legacy uniform " + name + ": unsupported destination type " + type);
        };
        int columns = "mat3".equals(type) ? 3 : "mat4".equals(type) ? 4 : 1;
        int rows = columns > 1 ? columns : type.contains("vec") ? type.charAt(type.length() - 1) - '0' : 1;
        int elements = arrayLength > 0 ? arrayLength : 1;
        if (descriptor.kind() != family || descriptor.columns() != columns
                || descriptor.rows() != rows || descriptor.arrayElements() != elements) {
            throw new IllegalArgumentException("Legacy uniform " + name + ": source " + descriptor
                    + " does not match " + type + " with " + elements + " element(s)");
        }
    }

    public static void requireSupported(CustomUniformSource.Descriptor descriptor, String name) {
        boolean supportedFamily = descriptor.kind() == CustomUniformSource.ScalarKind.I32
                || descriptor.kind() == CustomUniformSource.ScalarKind.F32
                || descriptor.kind() == CustomUniformSource.ScalarKind.B32;
        boolean supportedMatrix = descriptor.columns() == 1 || descriptor.kind() == CustomUniformSource.ScalarKind.F32
                && descriptor.columns() == descriptor.rows() && (descriptor.columns() == 3 || descriptor.columns() == 4);
        boolean supportedArray = descriptor.arrayElements() == 1 || descriptor.kind() == CustomUniformSource.ScalarKind.F32
                && descriptor.columns() == 1 && descriptor.rows() == 1;
        boolean supportedBoolean = descriptor.kind() != CustomUniformSource.ScalarKind.B32
                || descriptor.columns() == 1 && descriptor.rows() == 1 && descriptor.arrayElements() == 1;
        if (!supportedFamily || !supportedMatrix || !supportedArray || !supportedBoolean)
            throw new IllegalArgumentException("Unsupported legacy uniform source " + name + ": " + descriptor);
    }

    private void requireStorage(MappedBuffer storage, int bytes) {
        if (storage == null || !storage.buffer.isDirect() || storage.buffer.capacity() < bytes)
            throw invalid("owner requires at least " + bytes + " direct packed bytes");
    }

    private IllegalArgumentException invalid(String reason) {
        return new IllegalArgumentException("Legacy uniform " + this.destination.getName() + ": " + reason);
    }
}
