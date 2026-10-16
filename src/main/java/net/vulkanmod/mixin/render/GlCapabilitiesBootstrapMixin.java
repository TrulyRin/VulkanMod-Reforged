package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.opengl.GlCapabilitiesFallback;
import net.vulkanmod.compat.opengl.GlFunctionRegistry;
import net.vulkanmod.render.engine.VkGpuDevice;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

                                                                                            
@Mixin(RenderSystem.class)
public abstract class GlCapabilitiesBootstrapMixin {
    @Inject(method = "initRenderer", at = @At("RETURN"))
    private static void vulkanmod$installVerifiedCompatibility(CallbackInfo ci) {
        if (RenderSystem.getDevice() instanceof VkGpuDevice && Renderer.getInstance() != null
                && GlCapabilitiesFallback.install(true)) {
            Initializer.LOGGER.info("Installed {} verified OpenGL compatibility functions on the contextless Vulkan render thread",
                    GlFunctionRegistry.registeredCount());
        }
    }
}
