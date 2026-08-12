package net.vulkanmod.diagnostics.benchmark;

import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.RenderScale;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public final class MinecraftBenchmarkController {
    public enum StartResult {
        STARTED,
        UNAVAILABLE,
        ALREADY_RUNNING
    }

    private static final CurrentSettingsBenchmark BENCHMARK = new CurrentSettingsBenchmark();
    private static final AtomicLong RUN_SEQUENCE = new AtomicLong();
    private static final AtomicLong WORLD_SEQUENCE = new AtomicLong();
    private static WeakReference<Object> observedLevel = new WeakReference<>(null);
    private static long currentWorldSessionId;
    private static long latestIssuedRunId;
    private static boolean active;
    private static long frameStartedAt;
    private static Object runLevel;
    private static ResourceKey<Level> runDimension;
    private static RunGuard runGuard;
    private static int lastCountdownSecond;

    private MinecraftBenchmarkController() {
    }

    public static StartResult start(Minecraft minecraft) {
        if (active) {
            return StartResult.ALREADY_RUNNING;
        }
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return StartResult.UNAVAILABLE;
        }

        BenchmarkFingerprint fingerprint = captureFingerprint(minecraft);
        long worldSessionId = worldSessionId(minecraft);
        long runId = RUN_SEQUENCE.incrementAndGet();
        if (!BENCHMARK.start(fingerprint, System.nanoTime(), runId, worldSessionId)) {
            return StartResult.ALREADY_RUNNING;
        }
        latestIssuedRunId = runId;

        runLevel = minecraft.level;
        runDimension = minecraft.level.dimension();
        runGuard = RunGuard.capture(minecraft);
        frameStartedAt = 0L;
        lastCountdownSecond = -1;
        active = true;
        minecraft.setScreen(null);
        show(minecraft, Component.translatable("vulkanmod.diagnostics.benchmark.started"));
        return StartResult.STARTED;
    }

    public static void onClientTick(Minecraft minecraft) {
        if (!active) {
            return;
        }

        BenchmarkCancelReason invalidReason = invalidReason(minecraft);
        if (invalidReason != BenchmarkCancelReason.NONE) {
            cancel(minecraft, invalidReason);
            return;
        }

        long now = System.nanoTime();
        BENCHMARK.advance(now);

        if (BENCHMARK.state() == BenchmarkState.COUNTDOWN) {
            int seconds = Math.max(1, (int) Math.ceil(BENCHMARK.countdownRemainingNanos(now) / 1_000_000_000d));
            if (seconds != lastCountdownSecond) {
                lastCountdownSecond = seconds;
                show(minecraft, Component.translatable("vulkanmod.diagnostics.benchmark.countdown", seconds));
            }
        } else if (lastCountdownSecond != 0) {
            lastCountdownSecond = 0;
            show(minecraft, Component.translatable("vulkanmod.diagnostics.benchmark.running"));
        }
    }

    public static void onFrameStart() {
        if (!active) {
            return;
        }
        frameStartedAt = System.nanoTime();
    }

    public static void onFrameEnd(Minecraft minecraft) {
        if (!active) {
            return;
        }

        long now = System.nanoTime();
        long cpuRenderNanos = frameStartedAt == 0L ? 0L : Math.max(0L, now - frameStartedAt);
        BENCHMARK.recordFrame(now, cpuRenderNanos);

        if (BENCHMARK.state() == BenchmarkState.COMPLETED) {
            BENCHMARK.latestResult().ifPresent(result -> show(minecraft,
                    Component.translatable("vulkanmod.diagnostics.benchmark.completed",
                            String.format(java.util.Locale.ROOT, "%.1f", result.averageFps()))));
            clearRunContext();
        } else if (BENCHMARK.state() == BenchmarkState.FAILED) {
            show(minecraft, Component.translatable("vulkanmod.diagnostics.benchmark.failed"));
            clearRunContext();
        }
    }

    public static void cancel(Minecraft minecraft, BenchmarkCancelReason reason) {
        if (!active) {
            return;
        }
        BENCHMARK.cancel(reason);
        if (minecraft != null) {
            show(minecraft, Component.translatable("vulkanmod.diagnostics.benchmark.cancelled"));
        }
        clearRunContext();
    }

    public static BenchmarkState state(Minecraft minecraft) {
        if (!active && (minecraft == null || minecraft.level == null || minecraft.player == null)) {
            return BenchmarkState.UNAVAILABLE;
        }
        return BENCHMARK.state();
    }

    public static boolean isActive() {
        return active;
    }

    public static Optional<BenchmarkResult> latestResult() {
        return BENCHMARK.latestResult();
    }

    public static synchronized long worldSessionId(Minecraft minecraft) {
        Object level = minecraft == null ? null : minecraft.level;
        if (level == null) {
            observedLevel.clear();
            currentWorldSessionId = 0L;
            return 0L;
        }
        if (observedLevel.get() != level) {
            observedLevel = new WeakReference<>(level);
            currentWorldSessionId = WORLD_SEQUENCE.incrementAndGet();
        }
        return currentWorldSessionId;
    }

    public static long latestIssuedRunId() {
        return latestIssuedRunId;
    }

    public static boolean latestResultIsStale(Minecraft minecraft) {
        return BENCHMARK.latestResult()
                .map(result -> result.isStale(captureFingerprint(minecraft))
                        || (result.worldSessionId() > 0L
                        && result.worldSessionId() != worldSessionId(minecraft)))
                .orElse(false);
    }

    public static BenchmarkFingerprint captureFingerprint(Minecraft minecraft) {
        if (minecraft == null) {
            return BenchmarkFingerprint.unavailable();
        }

        String dimension = minecraft.level == null
                ? "menu"
                : minecraft.level.dimension().location().toString();
        String playContext = minecraft.level == null
                ? "none"
                : minecraft.hasSingleplayerServer() ? "singleplayer" : "multiplayer";
        GraphicsStatus graphics = minecraft.options.graphicsMode().get();

        return new BenchmarkFingerprint(
                dimension,
                playContext,
                minecraft.getWindow().getWidth(),
                minecraft.getWindow().getHeight(),
                currentRenderScale(),
                minecraft.options.getEffectiveRenderDistance(),
                minecraft.options.simulationDistance().get(),
                graphics.name(),
                minecraft.options.enableVsync().get(),
                minecraft.options.framerateLimit().get(),
                Initializer.CONFIG == null ? 0 : Initializer.CONFIG.performancePreset,
                Initializer.CONFIG != null && Initializer.CONFIG.indirectDraw,
                Initializer.CONFIG != null && Initializer.CONFIG.uniqueOpaqueLayer,
                currentAdaptiveChunkUploads(),
                currentChunkUploadsPerFrame());
    }

    private static BenchmarkCancelReason invalidReason(Minecraft minecraft) {
        if (minecraft == null || minecraft.level == null || minecraft.player == null) {
            return BenchmarkCancelReason.INVALID_CLIENT;
        }
        if (minecraft.level != runLevel || minecraft.level.dimension() != runDimension) {
            return BenchmarkCancelReason.WORLD_CHANGED;
        }
        if (minecraft.screen != null) {
            return BenchmarkCancelReason.SCREEN_OPENED;
        }
        if (!minecraft.isWindowActive()) {
            return BenchmarkCancelReason.FOCUS_LOST;
        }
        if (minecraft.isPaused()) {
            return BenchmarkCancelReason.PAUSED;
        }
        if (runGuard == null || !runGuard.matches(minecraft)) {
            return BenchmarkCancelReason.SETTINGS_CHANGED;
        }
        return BenchmarkCancelReason.NONE;
    }

    private static void show(Minecraft minecraft, Component message) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message, true);
        }
    }

    private static int currentRenderScale() {
        return Initializer.CONFIG == null ? RenderScale.DEFAULT : RenderScale.clamp(Initializer.CONFIG.renderScale);
    }

    private static boolean currentAdaptiveChunkUploads() {
        return Initializer.CONFIG != null && Initializer.CONFIG.adaptiveChunkUploads;
    }

    private static int currentChunkUploadsPerFrame() {
        return Initializer.CONFIG == null ? 0
                : Config.clampChunkUploadsPerFrame(Initializer.CONFIG.chunkUploadsPerFrame);
    }

    private static void clearRunContext() {
        active = false;
        frameStartedAt = 0L;
        runLevel = null;
        runDimension = null;
        runGuard = null;
    }

    private record RunGuard(
            int width,
            int height,
            int renderScale,
            int renderDistance,
            int simulationDistance,
            GraphicsStatus graphics,
            boolean vsync,
            int fpsCap,
            int preset,
            boolean indirectDraw,
            boolean uniqueOpaqueLayer,
            boolean adaptiveChunkUploads,
            int chunkUploadsPerFrame) {

        static RunGuard capture(Minecraft minecraft) {
            return new RunGuard(
                    minecraft.getWindow().getWidth(),
                    minecraft.getWindow().getHeight(),
                    currentRenderScale(),
                    minecraft.options.getEffectiveRenderDistance(),
                    minecraft.options.simulationDistance().get(),
                    minecraft.options.graphicsMode().get(),
                    minecraft.options.enableVsync().get(),
                    minecraft.options.framerateLimit().get(),
                    Initializer.CONFIG == null ? 0 : Initializer.CONFIG.performancePreset,
                    Initializer.CONFIG != null && Initializer.CONFIG.indirectDraw,
                    Initializer.CONFIG != null && Initializer.CONFIG.uniqueOpaqueLayer,
                    currentAdaptiveChunkUploads(),
                    currentChunkUploadsPerFrame());
        }

        boolean matches(Minecraft minecraft) {
            return width == minecraft.getWindow().getWidth()
                    && height == minecraft.getWindow().getHeight()
                    && renderScale == currentRenderScale()
                    && renderDistance == minecraft.options.getEffectiveRenderDistance()
                    && simulationDistance == minecraft.options.simulationDistance().get()
                    && graphics == minecraft.options.graphicsMode().get()
                    && vsync == minecraft.options.enableVsync().get()
                    && fpsCap == minecraft.options.framerateLimit().get()
                    && preset == (Initializer.CONFIG == null ? 0 : Initializer.CONFIG.performancePreset)
                    && indirectDraw == (Initializer.CONFIG != null && Initializer.CONFIG.indirectDraw)
                    && uniqueOpaqueLayer == (Initializer.CONFIG != null && Initializer.CONFIG.uniqueOpaqueLayer)
                    && adaptiveChunkUploads == currentAdaptiveChunkUploads()
                    && chunkUploadsPerFrame == currentChunkUploadsPerFrame();
        }
    }
}
