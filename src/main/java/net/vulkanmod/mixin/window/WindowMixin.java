package net.vulkanmod.mixin.window;

import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.compat.EarlyWindowCompat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Final @Shadow private long window;

    @Shadow private boolean vsync;

    @Shadow protected abstract void updateFullscreen(boolean bl);

    @Shadow private boolean fullscreen;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;

    @Shadow private int framebufferWidth;
    @Shadow private int framebufferHeight;

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getHeight();

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void redirect(int hint, int value) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
    private void redirect2(long window) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"))
    private GLCapabilities redirect2() {
        return null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;maxSupportedTextureSize()I"))
    private int redirect3() {
        return 0;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowSizeLimits(JIIII)V"))
    private void redirect4(long window, int minwidth, int minheight, int maxwidth, int maxheight) { }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"), require = 0)
    private void vulkanHint(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        boolean b = (Platform.isGnome() | Platform.isWeston() | Platform.isGeneric()) && Platform.isWayLand();
        GLFW.glfwWindowHint(GLFW_DECORATED, (b ? GLFW_FALSE : GLFW_TRUE));
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J", remap = false), require = 0)
    private long redirectSetupMinecraftWindow(java.util.function.IntSupplier width, java.util.function.IntSupplier height, java.util.function.Supplier title, java.util.function.LongSupplier monitor) {
        long handle = net.neoforged.fml.loading.ImmediateWindowHandler.setupMinecraftWindow(width, height, (java.util.function.Supplier<String>) title, monitor);

        if (GLFW.glfwGetWindowAttrib(handle, GLFW_CLIENT_API) != GLFW_NO_API) {
            net.vulkanmod.Initializer.LOGGER.info("VulkanMod: Intercepted OpenGL early window. Performing Vulkan handoff...");

            EarlyWindowCompat.setHandoffComplete(true);
            EarlyWindowCompat.disableFmlEarlyWindowProvider();

            GLFW.glfwMakeContextCurrent(0L);
            GLFW.glfwDestroyWindow(handle);

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
            long freshWindow = GLFW.glfwCreateWindow(width.getAsInt(), height.getAsInt(), (CharSequence) title.get(), monitor.getAsLong(), 0L);
            if (freshWindow == 0L) {
                throw new RuntimeException("Failed to create fresh contextless Vulkan window during FML handoff");
            }
            net.vulkanmod.Initializer.LOGGER.info("VulkanMod: Vulkan handoff complete. Fresh contextless window created.");
            return freshWindow;
        }

        return handle;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;positionWindow(Ljava/util/Optional;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;)Z", remap = false), require = 0)
    private boolean redirectPositionWindow(java.util.Optional opt, java.util.function.IntConsumer c1, java.util.function.IntConsumer c2, java.util.function.IntConsumer c3, java.util.function.IntConsumer c4) {
        if (EarlyWindowCompat.isHandoffComplete()) {
            return false;
        }
        return net.neoforged.fml.loading.ImmediateWindowHandler.positionWindow((java.util.Optional<Object>) opt, c1, c2, c3, c4);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void getHandle(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        net.vulkanmod.Initializer.LOGGER.info("VulkanMod: WindowMixin initialization finished.");

        if (GLFW.glfwGetWindowAttrib(this.window, GLFW_CLIENT_API) != GLFW_NO_API) {
            net.vulkanmod.Initializer.LOGGER.warn("VulkanMod: Reusing NeoForge early-display window with an existing OpenGL context.");
        }

        VRenderSystem.setWindow(this.window);
    }

    @Overwrite
    public void updateVsync(boolean vsync) {
        this.vsync = vsync;
        Vulkan.setVsync(vsync);
    }

    @Overwrite
    public void toggleFullScreen() {
        this.fullscreen = !this.fullscreen;
        Options.fullscreenDirty = true;
    }

    @Overwrite
    public void updateDisplay() {
        RenderSystem.flipFrame(this.window);

        if (Options.fullscreenDirty) {
            Options.fullscreenDirty = false;
            this.updateFullscreen(this.vsync);
        }
    }

    private boolean wasOnFullscreen = false;

    @Overwrite
    private void setMode() {
        Config config = Initializer.CONFIG;

        long monitor = GLFW.glfwGetPrimaryMonitor();
        if (this.fullscreen) {
            {
                VideoModeSet.VideoMode videoMode = config.videoMode;

                boolean supported;
                VideoModeSet set = VideoModeManager.getFromVideoMode(videoMode);

                if (set != null) {
                    supported = set.hasRefreshRate(videoMode.refreshRate);
                }
                else {
                    supported = false;
                }

                if(!supported) {
                    LOGGER.error("Resolution not supported, using first available as fallback");
                    videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
                }

                if (!this.wasOnFullscreen) {
                    this.windowedX = this.x;
                    this.windowedY = this.y;
                    this.windowedWidth = this.width;
                    this.windowedHeight = this.height;
                }

                this.x = 0;
                this.y = 0;
                this.width = videoMode.width;
                this.height = videoMode.height;
                GLFW.glfwSetWindowMonitor(this.window, monitor, this.x, this.y, this.width, this.height, videoMode.refreshRate);

                this.wasOnFullscreen = true;
            }
        }
        else if (config.windowedFullscreen) {
            VideoModeSet.VideoMode videoMode = VideoModeManager.getOsVideoMode();

            if (!this.wasOnFullscreen) {
                this.windowedX = this.x;
                this.windowedY = this.y;
                this.windowedWidth = this.width;
                this.windowedHeight = this.height;
            }

            int width = videoMode.width;
            int height = videoMode.height;

            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.window, 0L, 0, 0, width, height, -1);

            this.width = width;
            this.height = height;
            this.wasOnFullscreen = true;
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;

            GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_TRUE);

            this.wasOnFullscreen = false;
        }
    }

    @Overwrite
    private void onFramebufferResize(long window, int width, int height) {
        if (window == this.window) {
            int prevWidth = this.getWidth();
            int prevHeight = this.getHeight();

            if(width > 0 && height > 0) {
                this.framebufferWidth = width;
                this.framebufferHeight = height;

                Renderer.scheduleSwapChainUpdate();
            }

        }
    }

    @Overwrite
    private void onResize(long window, int width, int height) {
        this.width = width;
        this.height = height;

        if(width > 0 && height > 0)
            Renderer.scheduleSwapChainUpdate();
    }

}
