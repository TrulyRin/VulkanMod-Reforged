package net.vulkanmod.mixin.compatibility.codechicken;

import com.mojang.blaze3d.shaders.Shader;
import net.vulkanmod.compat.codechicken.CCUniformTypes;
import net.vulkanmod.vulkan.shader.CustomUniformSource;
import net.vulkanmod.vulkan.shader.PackedCustomUniformSource;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Pseudo
@Mixin(targets = "codechicken.lib.render.shader.CCUniform$UniformEntry", remap = false)
public abstract class CCUniformEntryMixin implements CustomUniformSource {
    @Unique
    private PackedCustomUniformSource vulkanmod$uniformSource;

    @Inject(method = "<init>(Ljava/lang/String;Lcodechicken/lib/render/shader/UniformType;ILcom/mojang/blaze3d/shaders/Shader;)V",
            at = @At("TAIL"), require = 1, allow = 1)
    private void vulkanmod$createUniformSource(String name, @Coerce Object type, int componentCount,
                                               Shader owner, CallbackInfo ci) {
        String enumName = ((Enum<?>) type).name();
        this.vulkanmod$uniformSource = new PackedCustomUniformSource(CCUniformTypes.descriptor(enumName, componentCount));
    }

    @Inject(method = "set(Ljava/lang/Object;Z)V",
            at = @At(value = "FIELD", target = "Lcodechicken/lib/render/shader/CCUniform$UniformEntry;cache:Ljava/lang/Object;",
                    opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER), require = 1, allow = 1)
    private void vulkanmod$acceptedUniformChange(Object payload, boolean transpose, CallbackInfo ci) {
        this.vulkanmod$uniformSource.accepted(payload, transpose);
    }

    @Override
    public Descriptor descriptor() {
        return this.vulkanmod$uniformSource.descriptor();
    }

    @Override
    public boolean initialized() {
        return this.vulkanmod$uniformSource.initialized();
    }

    @Override
    public MappedBuffer view() {
        return this.vulkanmod$uniformSource.view();
    }

    @Override
    public float previewFloat(int component) {
        return this.vulkanmod$uniformSource.previewFloat(component);
    }

    @Override
    public MappedBuffer uploadBuffer() {
        return this.vulkanmod$uniformSource.uploadBuffer();
    }
}
