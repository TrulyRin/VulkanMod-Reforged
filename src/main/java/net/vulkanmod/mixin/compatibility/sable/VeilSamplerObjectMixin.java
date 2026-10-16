package net.vulkanmod.mixin.compatibility.sable;

import net.vulkanmod.gl.GlSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.texture.SamplerObject", remap = false)
public abstract class VeilSamplerObjectMixin {
    @Redirect(
            method = "create()Lfoundry/veil/api/client/render/texture/SamplerObject;",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glGenSamplers()I"),
            remap = false)
    private static int vulkanmod$genSampler() {
        return GlSampler.genSamplers();
    }

    @Redirect(
            method = "create()Lfoundry/veil/api/client/render/texture/SamplerObject;",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glCreateSamplers()I"),
            remap = false)
    private static int vulkanmod$createSampler() {
        return GlSampler.genSamplers();
    }

    @Redirect(
            method = "create([Lfoundry/veil/api/client/render/texture/SamplerObject;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glGenSamplers(Ljava/nio/IntBuffer;)V"),
            remap = false)
    private static void vulkanmod$genSamplers(IntBuffer samplers) {
        GlSampler.genSamplers(samplers);
    }

    @Redirect(
            method = "create([Lfoundry/veil/api/client/render/texture/SamplerObject;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glCreateSamplers(Ljava/nio/IntBuffer;)V"),
            remap = false)
    private static void vulkanmod$createSamplers(IntBuffer samplers) {
        GlSampler.genSamplers(samplers);
    }

    @Redirect(
            method = {"bind(I)V", "unbind(I)V"},
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindSampler(II)V"),
            remap = false)
    private static void vulkanmod$bindSampler(int unit, int sampler) {
        GlSampler.bindSampler(unit, sampler);
    }

    @Redirect(
            method = {
                    "setFilter(Lfoundry/veil/api/client/render/texture/TextureFilter;)V",
                    "setFilter(ZZ)V",
                    "setCompareFunc(Lfoundry/veil/api/client/render/texture/TextureFilter$CompareFunction;)V",
                    "setWrapX(Lfoundry/veil/api/client/render/texture/TextureFilter$Wrap;)V",
                    "setWrapY(Lfoundry/veil/api/client/render/texture/TextureFilter$Wrap;)V",
                    "setWrapZ(Lfoundry/veil/api/client/render/texture/TextureFilter$Wrap;)V",
                    "setWrap(Lfoundry/veil/api/client/render/texture/TextureFilter$Wrap;Lfoundry/veil/api/client/render/texture/TextureFilter$Wrap;Lfoundry/veil/api/client/render/texture/TextureFilter$Wrap;)V",
                    "setCubeMapSeamless(Z)V"
            },
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glSamplerParameteri(III)V"),
            remap = false)
    private static void vulkanmod$samplerParameteri(int sampler, int pname, int param) {
        GlSampler.samplerParameteri(sampler, pname, param);
    }

    @Redirect(
            method = "setAnisotropy(F)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glSamplerParameterf(IIF)V"),
            remap = false)
    private static void vulkanmod$samplerParameterf(int sampler, int pname, float param) {
        GlSampler.samplerParameterf(sampler, pname, param);
    }

    @Redirect(
            method = "setBorderColor(FFFF)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glSamplerParameterfv(IILjava/nio/FloatBuffer;)V"),
            remap = false)
    private static void vulkanmod$samplerParameterfv(int sampler, int pname, FloatBuffer params) {
        if (params != null && params.hasRemaining()) {
            GlSampler.samplerParameterf(sampler, pname, params.get(params.position()));
        }
    }

    @Redirect(
            method = "setBorderColor(IIII)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glSamplerParameteriv(IILjava/nio/IntBuffer;)V"),
            remap = false)
    private static void vulkanmod$samplerParameteriv(int sampler, int pname, IntBuffer params) {
        if (params != null && params.hasRemaining()) {
            GlSampler.samplerParameteri(sampler, pname, params.get(params.position()));
        }
    }

    @Redirect(
            method = "setBorderColorI(IIII)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glGetSamplerParameterIiv(IILjava/nio/IntBuffer;)V"),
            remap = false)
    private static void vulkanmod$samplerParameterIiv(int sampler, int pname, IntBuffer params) {
        vulkanmod$samplerParameteriv(sampler, pname, params);
    }

    @Redirect(
            method = "setBorderColorUI(IIII)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glGetSamplerParameterIuiv(IILjava/nio/IntBuffer;)V"),
            remap = false)
    private static void vulkanmod$samplerParameterIuiv(int sampler, int pname, IntBuffer params) {
        vulkanmod$samplerParameteriv(sampler, pname, params);
    }

    @Redirect(
            method = "free()V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glDeleteSamplers(I)V"),
            remap = false)
    private void vulkanmod$deleteSampler(int sampler) {
        GlSampler.deleteSamplers(sampler);
    }
}
