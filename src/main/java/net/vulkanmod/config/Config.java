package net.vulkanmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.render.optimization.ChunkSchedulingPolicy;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class Config {
    public static final int MIN_FRAME_QUEUE_SIZE = 2;
    public static final int MAX_FRAME_QUEUE_SIZE = 5;

    public int frameQueueSize = 2;
    public VideoModeSet.VideoMode videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
    public boolean windowedFullscreen = false;
    public int performancePreset = PerformancePreset.BALANCED.id;
    public int chunkUploadsPerFrame = PerformancePreset.BALANCED.chunkUploadsPerFrame;
    public boolean adaptiveChunkUploads = true;
    public int renderScale = RenderScale.DEFAULT;

    public int advCulling = 2;

    public boolean indirectDraw = true;

    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public boolean blockEntityCulling = true;
    public boolean leavesCulling = true;
    public int particleCulling = 2;
    public int device = -1;

    public int ambientOcclusion = 1;
    public boolean textureAnimations = true;
    public int builderThreads = 0;
    public boolean backFaceCulling = true;
    public boolean useWayland = false;

    public static int clampFrameQueueSize(int value) {
        return Math.max(MIN_FRAME_QUEUE_SIZE, Math.min(MAX_FRAME_QUEUE_SIZE, value));
    }

    public static int maxBuilderThreadsForSystem() {
        int manualMaximum = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        return Math.min(ChunkSchedulingPolicy.MAX_BUILDER_THREADS, manualMaximum);
    }

    public static int clampBuilderThreads(int value) {
        return Math.max(0, Math.min(maxBuilderThreadsForSystem(), value));
    }

    public static int clampChunkUploadsPerFrame(int value) {
        return ChunkSchedulingPolicy.clampUploadCeiling(value);
    }

    public void write() {

        Path parent = CONFIG_PATH.getParent();
        if(parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                System.err.println("[VulkanMod] Failed to create config directory: " + parent + " - " + e.getMessage());
                return;
            }
        }

        try {
            Files.write(CONFIG_PATH, Collections.singleton(GSON.toJson(this)));
        } catch (IOException e) {
            System.err.println("[VulkanMod] Failed to write config: " + CONFIG_PATH + " - " + e.getMessage());
        }
    }

    private static Path CONFIG_PATH;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.TRANSIENT)
            .create();

    public static Config load(Path path) {
        Config config;
        Config.CONFIG_PATH = path;

        if (Files.exists(path)) {
            try (FileReader fileReader = new FileReader(path.toFile())) {
                config = GSON.fromJson(fileReader, Config.class);
            }
            catch (IOException exception) {
                throw new RuntimeException(exception.getMessage());
            }
        }
        else {
            config = null;
        }

        if (config != null) {
            config.frameQueueSize = clampFrameQueueSize(config.frameQueueSize);
            config.chunkUploadsPerFrame = clampChunkUploadsPerFrame(config.chunkUploadsPerFrame);
            config.renderScale = RenderScale.clamp(config.renderScale);
            config.builderThreads = clampBuilderThreads(config.builderThreads);
        }

        return config;
    }

    public static boolean readWaylandPreference(Path path) {
        if (!Files.exists(path)) {
            return false;
        }

        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonObject object = JsonParser.parseReader(fileReader).getAsJsonObject();
            return object.has("useWayland") && object.get("useWayland").getAsBoolean();
        } catch (IOException | RuntimeException exception) {
            System.err.println("[VulkanMod] Failed to read the startup Wayland preference: "
                    + exception.getMessage());
            return false;
        }
    }
}
