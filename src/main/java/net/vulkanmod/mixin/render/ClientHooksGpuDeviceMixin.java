package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import net.neoforged.neoforge.client.ClientHooks;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.engine.VkGpuDevice;
import net.vulkanmod.vulkan.Vulkan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

   
                                                                          
                                                                    
   
@Mixin(ClientHooks.class)
public abstract class ClientHooksGpuDeviceMixin {

    @Inject(method = "createGpuDevice", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$createGpuDevice(long window, int debugLevel, boolean syncDebug,
                                                  ShaderSource shaderSource, boolean enableDebugLabels,
                                                  CallbackInfoReturnable<GpuDevice> cir) {
        Initializer.LOGGER.info("VulkanMod: installing Vulkan GpuDevice");
                                                                                
                                 
        Vulkan.initVulkan(window);
        cir.setReturnValue(new VkGpuDevice(window, debugLevel, syncDebug, shaderSource, enableDebugLabels));
    }
}
