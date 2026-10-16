package net.vulkanmod.mixin.compatibility.sable;

import net.vulkanmod.gl.GlSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.ext.VeilMultiBind$1", remap = false)
public abstract class VeilMultiBindLegacySamplerMixin {
    @Redirect(
            method = {"bindSamplers(ILjava/nio/IntBuffer;)V", "bindSamplers(I[I)V"},
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindSampler(II)V"),
            remap = false,
            require = 2)
    private static void vulkanmod$bindSampler(int unit, int sampler) {
        GlSampler.bindSampler(unit, sampler);
    }
}
