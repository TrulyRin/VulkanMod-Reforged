package net.vulkanmod.mixin.window;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.neoforged.fml.loading.EarlyLoadingScreenController;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.lwjgl.glfw.GLFW.*;

   
                                                                              
                                                                               
                                                                        
   
@Mixin(Window.class)
public abstract class WindowNoApiMixin {
    @Shadow @Final private long handle;
    @Shadow private boolean vsync;
    @Shadow private int width;
    @Shadow private int height;

    @Inject(method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"),
            require = 0)
    private void vulkanmod$setNoApiHint(WindowEventHandler windowEventHandler, ScreenManager screenManager,
                                        DisplayData displayData, String preferredFullscreenVideoMode, String title,
                                        CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vulkanmod$initializeVideoModes(WindowEventHandler windowEventHandler, ScreenManager screenManager,
                                                DisplayData displayData, String preferredFullscreenVideoMode,
                                                String title, CallbackInfo ci) {
        VideoModeManager.init((Window) (Object) this);
        VideoModeManager.checkConfigVideoMode(Initializer.CONFIG);
    }

    @Inject(method = "takeOverWindow", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void vulkanmod$replaceOpenGlEarlyWindow(EarlyLoadingScreenController controller, String title,
                                                    CallbackInfoReturnable<Long> cir) {
        long handle = cir.getReturnValue();
        if (GLFW.glfwGetWindowAttrib(handle, GLFW_CLIENT_API) == GLFW_NO_API) {
            return;
        }

        Initializer.LOGGER.info("VulkanMod: Intercepted OpenGL early window. Performing Vulkan handoff...");

        GLFW.glfwMakeContextCurrent(0L);
        GLFW.glfwDestroyWindow(handle);

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long freshWindow = GLFW.glfwCreateWindow(Math.max(this.width, 1), Math.max(this.height, 1), title, 0L, 0L);
        if (freshWindow == 0L) {
            throw new RuntimeException("Failed to create contextless Vulkan window during NeoForge early-window handoff");
        }

        Initializer.LOGGER.info("VulkanMod: Vulkan handoff complete. Fresh contextless window created.");
        cir.setReturnValue(freshWindow);
    }

    @Inject(method = "updateVsync", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$updateVsyncWithoutGlContext(boolean enableVsync, CallbackInfo ci) {
        this.vsync = enableVsync;
        Vulkan.setVsync(enableVsync);
        ci.cancel();
    }

    @Inject(method = "onFramebufferResize", at = @At("RETURN"))
    private void vulkanmod$recreateRestoredSwapchain(long window, int framebufferWidth, int framebufferHeight, CallbackInfo ci) {
                                                                                  
                                                                       
        if (window == this.handle && framebufferWidth > 0 && framebufferHeight > 0 && Renderer.getInstance() != null) {
            Renderer.scheduleSwapChainUpdate();
        }
    }
}
