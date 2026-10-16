package net.vulkanmod.config.video;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.clamp;
import static org.lwjgl.glfw.GLFW.*;

public abstract class VideoModeManager {
    public static final Long2ObjectOpenHashMap<VideoModeSet[]> monitorToVideoModeSets = new Long2ObjectOpenHashMap<>();
    public static final Long2ObjectOpenHashMap<VideoModeSet.VideoMode> osVideoModes = new Long2ObjectOpenHashMap<>();

    public static long selectedMonitor;
    public static VideoModeSet.VideoMode selectedVideoMode;

    public static void init(Window window) {
        monitorToVideoModeSets.clear();
        osVideoModes.clear();

        PointerBuffer monitorHandles = GLFW.glfwGetMonitors();
        if (monitorHandles != null) {
            for (int i = 0; i < monitorHandles.limit(); i++) {
                addMonitorVideoModes(monitorHandles.get(i));
            }
        } else {
            Initializer.LOGGER.warn("GLFW returned no monitors while initializing VulkanMod video modes");
        }

        selectBestMonitor(window);
    }

    public static void addMonitorVideoModes(long monitor) {
        if (monitor == 0L) {
            return;
        }

        VideoModeSet[] resolutions = getVideoResolutions(monitor);
        if (resolutions.length > 0) {
            monitorToVideoModeSets.put(monitor, resolutions);
        }

        GLFWVidMode currentMode = GLFW.glfwGetVideoMode(monitor);
        if (currentMode != null) {
            osVideoModes.put(monitor, toVideoMode(currentMode));
        }
    }

    public static void removeMonitor(long monitor) {
        monitorToVideoModeSets.remove(monitor);
        osVideoModes.remove(monitor);
    }

    public static void applySelectedVideoMode() {
        if (selectedVideoMode != null) {
            Initializer.CONFIG.videoMode = selectedVideoMode;
        }
    }

    public static VideoModeSet[] getVideoResolutions() {
        VideoModeSet[] videoModeSets = monitorToVideoModeSets.get(selectedMonitor);
        return hasModes(videoModeSets) ? videoModeSets : fallbackResolutions();
    }

    public static VideoModeSet getFirstAvailable() {
        if (monitorToVideoModeSets.isEmpty()) {
            return VideoModeSet.getDummy();
        }

        VideoModeSet[] videoModeSets = monitorToVideoModeSets.get(selectedMonitor);
        if (!hasModes(videoModeSets)) {
            videoModeSets = monitorToVideoModeSets.get(glfwGetPrimaryMonitor());
        }
        if (!hasModes(videoModeSets)) {
            for (VideoModeSet[] candidate : monitorToVideoModeSets.values()) {
                if (hasModes(candidate)) {
                    videoModeSets = candidate;
                    break;
                }
            }
        }

        if (hasModes(videoModeSets))
            return videoModeSets[videoModeSets.length - 1];

        return VideoModeSet.getDummy();
    }

    public static VideoModeSet.VideoMode getOsVideoMode() {
        VideoModeSet.VideoMode videoMode = osVideoModes.get(selectedMonitor);
        if (videoMode == null && !osVideoModes.isEmpty()) {
            videoMode = osVideoModes.get(glfwGetPrimaryMonitor());
        }
        if (videoMode == null && !osVideoModes.isEmpty()) {
            videoMode = osVideoModes.values().iterator().next();
        }
        return videoMode != null ? videoMode : VideoModeSet.getDummy().getVideoMode();
    }

    public static VideoModeSet.VideoMode getCurrentVideoMode(long monitor){
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);

        if (vidMode == null) {
            Initializer.LOGGER.warn("Unable to query current video mode for monitor {}", monitor);
            return VideoModeSet.getDummy().getVideoMode();
        }

        return toVideoMode(vidMode);
    }

    public static VideoModeSet[] getVideoResolutions(long monitor) {
        GLFWVidMode.Buffer buffer = GLFW.glfwGetVideoModes(monitor);

        if (buffer == null) {
            Initializer.LOGGER.warn("Unable to enumerate video modes for monitor {}", monitor);
            return new VideoModeSet[0];
        }

        List<VideoModeSet> videoModeSets = new ArrayList<>();

        int currWidth = 0, currHeight = 0, currBitDepth = 0;
        VideoModeSet videoModeSet = null;

        for (int i = 0; i < buffer.limit(); i++) {
            buffer.position(i);
            int bitDepth = buffer.redBits();
            if (buffer.redBits() < 8 || buffer.greenBits() != bitDepth || buffer.blueBits() != bitDepth)
                continue;

            int width = buffer.width();
            int height = buffer.height();
            int refreshRate = buffer.refreshRate();

            if (currWidth != width || currHeight != height || currBitDepth != bitDepth) {
                currWidth = width;
                currHeight = height;
                currBitDepth = bitDepth;

                videoModeSet = new VideoModeSet(currWidth, currHeight, currBitDepth);
                videoModeSets.add(videoModeSet);
            }

            videoModeSet.addRefreshRate(refreshRate);
        }

        VideoModeSet[] arr = new VideoModeSet[videoModeSets.size()];
        videoModeSets.toArray(arr);

        return arr;
    }

    public static VideoModeSet getFromVideoMode(VideoModeSet.VideoMode videoMode) {
        if (videoMode == null) {
            return null;
        }
        for (var set : getVideoResolutions()) {
            if (set.width == videoMode.width && set.height == videoMode.height)
                return set;
        }

        return null;
    }

    public static VideoModeSet getVideoModeSet(VideoModeSet.VideoMode videoMode) {
        return getFromVideoMode(videoMode);
    }

    public static void selectBestMonitor(Window window) {
        Monitor bestMonitor = window.findBestMonitor();
        selectedMonitor = bestMonitor != null ? bestMonitor.getMonitor() : 0L;

        if (!monitorToVideoModeSets.containsKey(selectedMonitor)) {
            long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
            selectedMonitor = monitorToVideoModeSets.containsKey(primaryMonitor)
                    ? primaryMonitor
                    : firstEnumeratedMonitor();
        }
    }

    public static void checkConfigVideoMode(Config config) {
        if (config == null) {
            return;
        }
        var videoMode = config.videoMode;
        if (videoMode == null || videoMode.width <= 0 || videoMode.height <= 0 || videoMode.refreshRate <= 0) {
            VideoModeSet fallback = getFirstAvailable();
            if (fallback != VideoModeSet.getDummy()) {
                config.videoMode = fallback.getVideoMode();
                config.write();
            }
        }
    }

    private static boolean hasModes(VideoModeSet[] videoModeSets) {
        return videoModeSets != null && videoModeSets.length > 0;
    }

    private static VideoModeSet[] fallbackResolutions() {
        return new VideoModeSet[]{VideoModeSet.getDummy()};
    }

    private static long firstEnumeratedMonitor() {
        if (monitorToVideoModeSets.isEmpty()) {
            return 0L;
        }
        return monitorToVideoModeSets.keySet().iterator().nextLong();
    }

    private static VideoModeSet.VideoMode toVideoMode(GLFWVidMode videoMode) {
        return new VideoModeSet.VideoMode(
                videoMode.width(), videoMode.height(), videoMode.redBits(), videoMode.refreshRate());
    }
}
