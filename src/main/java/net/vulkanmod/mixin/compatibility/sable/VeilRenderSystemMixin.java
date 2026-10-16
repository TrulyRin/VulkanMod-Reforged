package net.vulkanmod.mixin.compatibility.sable;

import net.vulkanmod.compat.sable.veil.VulkanVeilCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public abstract class VeilRenderSystemMixin {
    @Inject(method = {
            "tessellationSupported",
            "computeSupported",
            "atomicCounterSupported",
            "transformFeedbackSupported",
            "multibindSupported",
            "sparseBuffersSupported",
            "directStateAccessSupported",
            "separateShaderObjectsSupported",
            "clearTextureSupported",
            "copyImageSupported",
            "shaderStorageBufferSupported",
            "programInterfaceQuerySupported",
            "textureAnisotropySupported",
            "textureMirrorClampToEdgeSupported",
            "textureCubeMapSeamlessSupported",
            "textureCubeMapArraySupported",
            "nvDrawTextureSupported",
            "drawIndirectSupported",
            "multiDrawIndirectSupported",
            "gpuShaderFloat64BitSupported",
            "gpuShaderInt64BitSupported",
            "vertexAttribute64BitSupported",
            "bindlessTextureSupported",
            "vertexType10F11F11FRevSupported",
            "pipelineStatisticsQuerySupported"
    }, at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$unsupportedGlFeature(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @Inject(method = "maxCombinedTextureUnits", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$maxCombinedTextureUnits(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(VulkanVeilCapabilities.maxCombinedTextureUnits());
    }

    @Inject(method = "maxColorAttachments", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$maxColorAttachments(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(VulkanVeilCapabilities.maxColorAttachments());
    }

    @Inject(method = "maxSamples", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$maxSamples(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(VulkanVeilCapabilities.maxSamples());
    }
}
