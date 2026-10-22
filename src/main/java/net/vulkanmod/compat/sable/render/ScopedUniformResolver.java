package net.vulkanmod.compat.sable.render;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.function.Supplier;

public final class ScopedUniformResolver {
    private ScopedUniformResolver() {
    }

    public static MappedBuffer buffer(String name, Supplier<MappedBuffer> globalSupplier) {
        return buffer(name, null, globalSupplier);
    }

    public static MappedBuffer buffer(String name, String type, Supplier<MappedBuffer> globalSupplier) {
        MappedBuffer scoped = scopedBuffer(name, type);
        return scoped != null ? scoped : globalSupplier.get();
    }

    public static float floatValue(String name, Supplier<Float> globalSupplier) {
        MappedBuffer scoped = scopedBuffer(name, "float");
        return scoped != null ? scoped.getFloat(0) : globalSupplier.get();
    }

    public static int intValue(String name, Supplier<Integer> globalSupplier) {
        MappedBuffer scoped = scopedBuffer(name, "int");
        return scoped != null ? scoped.getInt(0) : globalSupplier.get();
    }

    public static MappedBuffer mvp(Supplier<MappedBuffer> globalSupplier) {
        Object shader = SublevelRenderScope.current().map(SublevelRenderScope.State::shader).orElse(null);
        MappedBuffer scoped = shader instanceof UniformSource source ? SableDrawState.mvp(source) : null;
        return scoped != null ? scoped : globalSupplier.get();
    }

    private static MappedBuffer scopedBuffer(String name, String type) {
        return SublevelRenderScope.current()
                .map(SublevelRenderScope.State::shader)
                .filter(UniformSource.class::isInstance)
                .map(UniformSource.class::cast)
                .map(source -> type == null ? source.uploadUniformBuffer(name) : source.uploadUniformBuffer(name, type))
                .orElse(null);
    }

    @FunctionalInterface
    public interface UniformSource {
        MappedBuffer uniformBuffer(String name);

        default MappedBuffer uploadUniformBuffer(String name) { return uniformBuffer(name); }

        default MappedBuffer uploadUniformBuffer(String name, String type) { return uploadUniformBuffer(name); }
    }
}
