package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1f extends Uniform {
    private static final Supplier<Float> ZERO_SUPPLIER = () -> 0.0F;

    private Supplier<Float> floatSupplier;

    public Vec1f(Info info) {
        super(info);
    }

    void setSupplier() {
        this.floatSupplier = Uniforms.vec1f_uniformMap.get(this.info.name);
        if (this.floatSupplier == null) {
            this.floatSupplier = ZERO_SUPPLIER;
        }
    }

    @Override
    public void setSupplier(Supplier<MappedBuffer> supplier) {
        clearDefault();
        this.floatSupplier = () -> supplier.get().getFloat(0);
    }

    @Override
    public boolean hasSupplier() {
        return this.floatSupplier != null;
    }

    @Override
    public boolean hasExplicitSupplier() {
        return this.floatSupplier != null && this.floatSupplier != ZERO_SUPPLIER;
    }

    void update(long ptr) {
        if (writeDefault(ptr)) return;
        float f = this.floatSupplier.get();
        MemoryUtil.memPutFloat(ptr + this.offset, f);
    }
}
