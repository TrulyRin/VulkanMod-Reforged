package net.vulkanmod.vulkan.shader.layout;

import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1i extends Uniform {
    private static final Supplier<Integer> ZERO_SUPPLIER = () -> 0;

    private Supplier<Integer> intSupplier;

    public Vec1i(Info info) {
        super(info);
    }

    void setSupplier() {
        this.intSupplier = Uniforms.vec1i_uniformMap.get(this.info.name);
        if (this.intSupplier == null) {
            this.intSupplier = ZERO_SUPPLIER;
        }
    }

    @Override
    public void setSupplier(Supplier<MappedBuffer> supplier) {
        this.intSupplier = () -> supplier.get().getInt(0);
    }

    @Override
    public boolean hasSupplier() {
        return this.intSupplier != null;
    }

    void update(long ptr) {
        int i = this.intSupplier.get();
        MemoryUtil.memPutInt(ptr + this.offset, i);
    }
}
