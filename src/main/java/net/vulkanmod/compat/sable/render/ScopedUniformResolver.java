package net.vulkanmod.compat.sable.render;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.function.Supplier;

public final class ScopedUniformResolver {
    private ScopedUniformResolver() {
    }

    public static MappedBuffer buffer(String name, Supplier<MappedBuffer> globalSupplier) {
        MappedBuffer scoped = scopedBuffer(name);
        return scoped != null ? scoped : globalSupplier.get();
    }

    public static float floatValue(String name, Supplier<Float> globalSupplier) {
        MappedBuffer scoped = scopedBuffer(name);
        return scoped != null ? scoped.getFloat(0) : globalSupplier.get();
    }

    public static int intValue(String name, Supplier<Integer> globalSupplier) {
        MappedBuffer scoped = scopedBuffer(name);
        return scoped != null ? scoped.getInt(0) : globalSupplier.get();
    }

    private static MappedBuffer scopedBuffer(String name) {
        return SublevelRenderScope.current()
                .map(SublevelRenderScope.State::shader)
                .filter(UniformSource.class::isInstance)
                .map(UniformSource.class::cast)
                .map(source -> source.uniformBuffer(name))
                .orElse(null);
    }

    @FunctionalInterface
    public interface UniformSource {
        MappedBuffer uniformBuffer(String name);
    }
}
