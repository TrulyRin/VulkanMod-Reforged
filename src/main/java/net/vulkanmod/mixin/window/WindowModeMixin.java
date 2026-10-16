package net.vulkanmod.mixin.window;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowVideoModePolicy;
import net.vulkanmod.config.video.WindowedBoundsTracker;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

                                                                                          
@Mixin(Window.class)
public abstract class WindowModeMixin {
    @Shadow @Final private long handle;
    @Shadow @Final private WindowEventHandler eventHandler;
    @Shadow private boolean fullscreen;
    @Shadow private boolean actuallyFullscreen;
    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;
    @Shadow private void setMode() { throw new AssertionError(); }
    @Unique private WindowedBoundsTracker vulkanmod$normalBounds;

    @Unique
    private WindowedBoundsTracker vulkanmod$boundsTracker() {
                                                              
        if (this.vulkanmod$normalBounds == null) {
            this.vulkanmod$normalBounds = new WindowedBoundsTracker();
        }
        return this.vulkanmod$normalBounds;
    }

    @Inject(method = "setMode", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$applyWindowMode(CallbackInfo ci) {
        Config config = Initializer.CONFIG;
        if (config == null) {
            return;
        }
        WindowedBoundsTracker tracker = this.vulkanmod$boundsTracker();
        if (!this.fullscreen && !config.windowedFullscreen) {
            if (tracker.isActive()) {
                WindowedBoundsTracker.Bounds bounds = tracker.restore();
                this.x = this.windowedX = bounds.x();
                this.y = this.windowedY = bounds.y();
                this.width = this.windowedWidth = bounds.width();
                this.height = this.windowedHeight = bounds.height();
                GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
                GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, GLFW.GLFW_DONT_CARE);
                ci.cancel();
            }
            return;
        }

        Monitor monitor = ((Window) (Object) this).findBestMonitor();
        if (monitor == null) {
            return;
        }
        long monitorHandle = monitor.getMonitor();
        if (!VideoModeManager.monitorToVideoModeSets.containsKey(monitorHandle)) {
            VideoModeManager.addMonitorVideoModes(monitorHandle);
        }
        VideoModeManager.selectedMonitor = monitorHandle;
                                                                                   
                                                                                 
        VideoMode osMode = monitor.getCurrentMode();
        VideoModeSet.VideoMode desktopMode = new VideoModeSet.VideoMode(osMode.getWidth(), osMode.getHeight(),
                osMode.getRedBits(), osMode.getRefreshRate());
        VideoModeManager.osVideoModes.put(monitorHandle, desktopMode);
        VideoModeSet.VideoMode mode = this.fullscreen
                ? WindowVideoModePolicy.resolveExclusiveMode(config.videoMode, VideoModeManager.getVideoResolutions(), desktopMode)
                : desktopMode;
        if (mode == null || mode.width <= 0 || mode.height <= 0 || mode.refreshRate <= 0) {
            return;
        }

        if (GLFW.glfwGetWindowMonitor(this.handle) != 0L) {
            tracker.enter(this.windowedX, this.windowedY, this.windowedWidth, this.windowedHeight);
        } else {
            tracker.enter(this.x, this.y, this.width, this.height);
        }
        this.x = this.fullscreen ? 0 : monitor.getX();
        this.y = this.fullscreen ? 0 : monitor.getY();
        this.width = mode.width;
        this.height = mode.height;
        GLFW.glfwSetWindowAttrib(this.handle, GLFW.GLFW_DECORATED, this.fullscreen ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowMonitor(this.handle, this.fullscreen ? monitorHandle : 0L,
                this.x, this.y, this.width, this.height, this.fullscreen ? mode.refreshRate : GLFW.GLFW_DONT_CARE);
        ci.cancel();
    }

    @Inject(method = "updateDisplay", at = @At("RETURN"))
    private void vulkanmod$applyPendingVideoSettings(TracyFrameCapture capture, CallbackInfo ci) {
        if (Options.fullscreenDirty) {
            Options.fullscreenDirty = false;
            this.setMode();
            this.actuallyFullscreen = this.fullscreen;
            this.eventHandler.resizeDisplay();
        }
    }
}
