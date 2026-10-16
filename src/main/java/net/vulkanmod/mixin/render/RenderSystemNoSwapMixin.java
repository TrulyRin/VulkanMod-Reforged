package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Renderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

   
                                                                            
   
@Mixin(RenderSystem.class)
public abstract class RenderSystemNoSwapMixin {

    @Redirect(method = "flipFrame",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"),
            remap = false)
    private static void vulkanmod$skipGlfwSwapBuffers(long window) {
        Renderer.getInstance().endFrame();
    }

                                                                          
                                                                             
                                                                            
    @Inject(method = "initRenderer",
            at = @At(value = "FIELD",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;DEVICE:Lcom/mojang/blaze3d/systems/GpuDevice;",
                    opcode = Opcodes.PUTSTATIC,
                    shift = At.Shift.AFTER))
    private static void vulkanmod$initVulkanFrameRenderer(CallbackInfo ci) {
        Renderer.initRenderer();
    }
}
