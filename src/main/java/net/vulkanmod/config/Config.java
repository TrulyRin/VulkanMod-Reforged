package net.vulkanmod.config;

import net.vulkanmod.render.optimization.ChunkSchedulingPolicy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.render.chunk.build.light.LightMode;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public class Config {
    public static final int MIN_FRAME_QUEUE_SIZE = 2;
    public static final int MAX_FRAME_QUEUE_SIZE = 5;
    private static final int DEFAULT_PERFORMANCE_PRESET_ID = 2;

    public VideoModeSet.VideoMode videoMode = VideoModeSet.getDummy().getVideoMode();
    public int windowMode = 0;

    public int advCulling = 2;
    public boolean indirectDraw = true;

    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public int device = -1;
    public boolean useWayland = false;

    public int ambientOcclusion = 1;
    public int frameQueueSize = 2;
    public int builderThreads = 0;

    public boolean backFaceCulling = true;
    public boolean textureAnimations = true;

                                                                     
    public int chunkUploadsPerFrame = 5;
    public boolean adaptiveChunkUploads = true;
    public boolean blockEntityCulling = true;
    public boolean leavesCulling = true;

                                                     
    public boolean windowedFullscreen = false;
                                                                                  
    public int performancePreset = DEFAULT_PERFORMANCE_PRESET_ID;
    public int renderScale = RenderScale.DEFAULT;
    public int particleCulling = 2;

    public static int clampChunkUploadsPerFrame(int value) {
        return ChunkSchedulingPolicy.clampUploadCeiling(value);
    }

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

    public void write() {
        if (this.configPath == null) {
            throw new IllegalStateException("Config has no bound path");
        }

        try {
            writeAtomically(this.configPath, GSON.toJson(this));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write VulkanMod config to " + this.configPath, e);
        }
    }

    public void write(Path path) {
        this.configPath = normalize(path);
        this.write();
    }

    private transient Path configPath;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.TRANSIENT)
            .create();

    public static Config load(Path path) {
        Path normalizedPath = normalize(path);
        if (!Files.exists(normalizedPath)) {
            Config config = defaults(normalizedPath);
            config.write();
            return config;
        }

        try {
            String json = Files.readString(normalizedPath, StandardCharsets.UTF_8);
            Config config = GSON.fromJson(json, Config.class);
            if (config == null) {
                throw new JsonParseException("Config root is null");
            }
            config.configPath = normalizedPath;
            config.sanitize();
            return config;
        } catch (JsonParseException malformed) {
            try {
                backupCorruptConfig(normalizedPath);
            } catch (IOException backupFailure) {
                throw new IllegalStateException(
                        "Refusing to replace malformed VulkanMod config before it is backed up", backupFailure);
            }

            Config config = defaults(normalizedPath);
            config.write();
            return config;
        } catch (IOException readFailure) {
            throw new IllegalStateException("Failed to read VulkanMod config from " + normalizedPath, readFailure);
        }
    }

    public static Config defaults(Path path) {
        Config config = new Config();
        config.configPath = normalize(path);
        return config;
    }

    public static boolean readWaylandPreference(Path path) {
        Path normalizedPath = normalize(path);
        if (!Files.exists(normalizedPath)) {
            return false;
        }

        try {
            JsonObject object = JsonParser.parseString(
                    Files.readString(normalizedPath, StandardCharsets.UTF_8)).getAsJsonObject();
            return object.has("useWayland") && object.get("useWayland").getAsBoolean();
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    static void writeAtomically(Path path, String contents) throws IOException {
        Path normalizedPath = normalize(path);
        Path parent = normalizedPath.getParent();
        if (parent == null) {
            throw new IOException("Config path has no parent: " + normalizedPath);
        }

        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "." + normalizedPath.getFileName() + ".", ".tmp");
        try {
            Files.writeString(temporary, contents, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temporary, normalizedPath,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, normalizedPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void backupCorruptConfig(Path path) throws IOException {
        Path backup = path.resolveSibling(path.getFileName() + ".corrupt.bak");
        int suffix = 1;
        while (Files.exists(backup)) {
            backup = path.resolveSibling(path.getFileName() + ".corrupt." + suffix++ + ".bak");
        }
        Files.copy(path, backup);
    }

    private void sanitize() {
        this.frameQueueSize = clampFrameQueueSize(this.frameQueueSize);
        this.builderThreads = clampBuilderThreads(this.builderThreads);
        this.chunkUploadsPerFrame = clampChunkUploadsPerFrame(this.chunkUploadsPerFrame);
        this.renderScale = RenderScale.clamp(this.renderScale);
                                                                                        
        if (this.advCulling != 10) {
            this.advCulling = Math.max(1, Math.min(3, this.advCulling));
        }
        this.particleCulling = Math.max(0, Math.min(3, this.particleCulling));
        this.ambientOcclusion = Math.max(LightMode.FLAT, Math.min(LightMode.SUB_BLOCK, this.ambientOcclusion));
        if (this.performancePreset < 0 || this.performancePreset > 3) {
            this.performancePreset = DEFAULT_PERFORMANCE_PRESET_ID;
        }
    }

    private static Path normalize(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Config path must not be null");
        }
        return path.toAbsolutePath().normalize();
    }
}
