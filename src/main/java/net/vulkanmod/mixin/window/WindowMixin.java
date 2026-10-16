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
    @Final @Shadow(remap = false) private long f_85349_;

    @Shadow(remap = false) private boolean f_85369_;

    @Shadow(remap = false) protected abstract void m_85431_(boolean bl);

    @Shadow(remap = false) private boolean f_85355_;

    @Shadow(remap = false) @Final private static Logger f_85345_;

    @Shadow(remap = false) private int f_85350_;
    @Shadow(remap = false) private int f_85351_;
    @Shadow(remap = false) private int f_85352_;
    @Shadow(remap = false) private int f_85353_;
    @Shadow(remap = false) private int f_85357_;
    @Shadow(remap = false) private int f_85358_;
    @Shadow(remap = false) private int f_85359_;
    @Shadow(remap = false) private int f_85360_;

    @Shadow(remap = false) private int f_85361_;
    @Shadow(remap = false) private int f_85362_;

    @Shadow(remap = false) public abstract int m_85441_();

    @Shadow(remap = false) public abstract int m_85442_();

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false), require = 0)
    private void redirect(int hint, int value) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V", remap = false), require = 0)
    private void redirect2(long window) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;", remap = false), require = 0)
    private GLCapabilities redirect2() {
        return null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;maxSupportedTextureSize()I"), require = 0)
    private int redirect3() {
        return 0;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSetWindowSizeLimits(JIIII)V", remap = false), require = 0)
    private void redirect4(long window, int minwidth, int minheight, int maxwidth, int maxheight) { }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", remap = false), require = 0)
    private void vulkanHint(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        boolean b = (Platform.isGnome() | Platform.isWeston() | Platform.isGeneric()) && Platform.isWayLand();
        GLFW.glfwWindowHint(GLFW_DECORATED, (b ? GLFW_FALSE : GLFW_TRUE));
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J", remap = false), require = 0)
    private long redirectSetupMinecraftWindow(java.util.function.IntSupplier width, java.util.function.IntSupplier height, java.util.function.Supplier title, java.util.function.LongSupplier monitor) {
        long handle = net.minecraftforge.fml.loading.ImmediateWindowHandler.setupMinecraftWindow(width, height, (java.util.function.Supplier<String>) title, monitor);

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

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;positionWindow(Ljava/util/Optional;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;Ljava/util/function/IntConsumer;)Z", remap = false), require = 0)
    private boolean redirectPositionWindow(java.util.Optional opt, java.util.function.IntConsumer c1, java.util.function.IntConsumer c2, java.util.function.IntConsumer c3, java.util.function.IntConsumer c4) {
        if (EarlyWindowCompat.isHandoffComplete()) {
            return false;
        }
        return net.minecraftforge.fml.loading.ImmediateWindowHandler.positionWindow((java.util.Optional<Object>) opt, c1, c2, c3, c4);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void getHandle(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        net.vulkanmod.Initializer.LOGGER.info("VulkanMod: WindowMixin initialization finished.");

        if (GLFW.glfwGetWindowAttrib(this.f_85349_, GLFW_CLIENT_API) != GLFW_NO_API) {
            net.vulkanmod.Initializer.LOGGER.warn("VulkanMod: Reusing NeoForge early-display window with an existing OpenGL context.");
        }

        VRenderSystem.setWindow(this.f_85349_);
    }

    @Overwrite(remap = false)
    public void m_85409_(boolean vsync) {
        this.f_85369_ = vsync;
        Vulkan.setVsync(vsync);
    }

    @Overwrite(remap = false)
    public void m_85438_() {
        this.f_85355_ = !this.f_85355_;
        Options.fullscreenDirty = true;
    }

    @Overwrite(remap = false)
    public void m_85435_() {
        RenderSystem.flipFrame(this.f_85349_);

        if (Options.fullscreenDirty) {
            Options.fullscreenDirty = false;
            this.m_85431_(this.f_85369_);
        }
    }

    private boolean wasOnFullscreen = false;

    @Overwrite(remap = false)
    private void m_85453_() {
        Config config = Initializer.CONFIG;

        long monitor = GLFW.glfwGetPrimaryMonitor();
        if (this.f_85355_) {
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
                    f_85345_.error("Resolution not supported, using first available as fallback");
                    videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
                }

                if (!this.wasOnFullscreen) {
                    this.f_85350_ = this.f_85357_;
                    this.f_85351_ = this.f_85358_;
                    this.f_85352_ = this.f_85359_;
                    this.f_85353_ = this.f_85360_;
                }

                this.f_85357_ = 0;
                this.f_85358_ = 0;
                this.f_85359_ = videoMode.width;
                this.f_85360_ = videoMode.height;
                GLFW.glfwSetWindowMonitor(this.f_85349_, monitor, this.f_85357_, this.f_85358_, this.f_85359_, this.f_85360_, videoMode.refreshRate);

                this.wasOnFullscreen = true;
            }
        }
        else if (config.windowedFullscreen) {
            VideoModeSet.VideoMode videoMode = VideoModeManager.getOsVideoMode();

            if (!this.wasOnFullscreen) {
                this.f_85350_ = this.f_85357_;
                this.f_85351_ = this.f_85358_;
                this.f_85352_ = this.f_85359_;
                this.f_85353_ = this.f_85360_;
            }

            int width = videoMode.width;
            int height = videoMode.height;

            GLFW.glfwSetWindowAttrib(this.f_85349_, GLFW_DECORATED, GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.f_85349_, 0L, 0, 0, width, height, -1);

            this.f_85359_ = width;
            this.f_85360_ = height;
            this.wasOnFullscreen = true;
        } else {
            this.f_85357_ = this.f_85350_;
            this.f_85358_ = this.f_85351_;
            this.f_85359_ = this.f_85352_;
            this.f_85360_ = this.f_85353_;

            GLFW.glfwSetWindowMonitor(this.f_85349_, 0L, this.f_85357_, this.f_85358_, this.f_85359_, this.f_85360_, -1);
            GLFW.glfwSetWindowAttrib(this.f_85349_, GLFW_DECORATED, GLFW_TRUE);

            this.wasOnFullscreen = false;
        }
    }

    @Overwrite(remap = false)
    private void m_85415_(long window, int width, int height) {
        if (window == this.f_85349_) {
            int prevWidth = this.m_85441_();
            int prevHeight = this.m_85442_();

            if(width > 0 && height > 0) {
                this.f_85361_ = width;
                this.f_85362_ = height;

                Renderer.scheduleSwapChainUpdate();
            }

        }
    }

    @Overwrite(remap = false)
    private void m_85427_(long window, int width, int height) {
        this.f_85359_ = width;
        this.f_85360_ = height;

        if(width > 0 && height > 0)
            Renderer.scheduleSwapChainUpdate();
    }

}

