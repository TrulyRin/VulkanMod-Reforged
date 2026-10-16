package net.vulkanmod.render.optimization;

import net.vulkanmod.render.chunk.build.TaskDispatcher;

public final class AdaptiveChunkUploadBudget {
    private static final float CALM_FRAME_MS = 16.7f;
    private static final float SPIKE_FRAME_MS = 33.3f;
    private static final float EXTREME_FRAME_MS = 66.7f;
    private static final float PEAK_DROP_MULTIPLIER = 1.6f;
    private static final float PEAK_DROP_DELTA_MS = 5.0f;
    private static final float SMOOTHING = 0.18f;
    private static final int SPIKE_COOLDOWN_FRAMES = 4;
    private static final int PEAK_DROP_COOLDOWN_FRAMES = 6;
    private static final long SPIKE_UPLOAD_TIME_BUDGET_NANOS = 1_000_000L;
    private static final long DEFAULT_UPLOAD_TIME_BUDGET_NANOS = 2_000_000L;
    private static final long CALM_UPLOAD_TIME_BUDGET_NANOS = 3_000_000L;
    private static final long TURBO_UPLOAD_TIME_BUDGET_NANOS = 5_000_000L;

    private static float smoothedFrameMs = CALM_FRAME_MS;
    private static float lastFrameMs = CALM_FRAME_MS;
    private static int spikeCooldownFrames;
    private static int calmFrames;
    private static boolean hasSamples;

    private AdaptiveChunkUploadBudget() {
    }

    public static void recordFrameTimeMs(float frameTimeMs) {
        if (!Float.isFinite(frameTimeMs) || frameTimeMs <= 0.0f) {
            return;
        }

        float previousSmoothedFrameMs = smoothedFrameMs;
        boolean peakDrop = hasSamples
                && frameTimeMs >= previousSmoothedFrameMs * PEAK_DROP_MULTIPLIER
                && frameTimeMs - previousSmoothedFrameMs >= PEAK_DROP_DELTA_MS;

        lastFrameMs = frameTimeMs;
        if (frameTimeMs >= SPIKE_FRAME_MS) {
            spikeCooldownFrames = SPIKE_COOLDOWN_FRAMES;
        } else if (peakDrop) {
            spikeCooldownFrames = Math.max(spikeCooldownFrames, PEAK_DROP_COOLDOWN_FRAMES);
        }

        if (!hasSamples) {
            smoothedFrameMs = frameTimeMs;
            hasSamples = true;
            calmFrames = frameTimeMs <= CALM_FRAME_MS ? 1 : 0;
            return;
        }

        smoothedFrameMs += (frameTimeMs - smoothedFrameMs) * SMOOTHING;
        if (!peakDrop && frameTimeMs <= CALM_FRAME_MS && frameTimeMs <= previousSmoothedFrameMs + 3.0f) {
            calmFrames++;
        } else {
            calmFrames = 0;
        }
    }

    public static int chooseBudget(int configuredUploadsPerFrame) {
        return chooseBudget(configuredUploadsPerFrame, 0);
    }

    public static int chooseBudget(int configuredUploadsPerFrame, int pendingUploads) {
        int baseBudget = TaskDispatcher.clampMaxUploadsPerFrame(configuredUploadsPerFrame);

        if (lastFrameMs >= EXTREME_FRAME_MS) {
            consumeCooldownFrame();
            return 1;
        }

        if (lastFrameMs >= SPIKE_FRAME_MS || smoothedFrameMs >= SPIKE_FRAME_MS || spikeCooldownFrames > 0) {
            consumeCooldownFrame();
            return Math.max(1, baseBudget / 3);
        }

        if (calmFrames >= 6 && smoothedFrameMs <= CALM_FRAME_MS && lastFrameMs <= CALM_FRAME_MS + 3.0f) {
            int backlogBoost = 1;
            if (pendingUploads >= baseBudget * 32 && calmFrames >= 12 && lastFrameMs <= CALM_FRAME_MS) {
                backlogBoost = Math.min(6, Math.max(4, baseBudget));
            } else if (pendingUploads >= baseBudget * 12 && calmFrames >= 10 && lastFrameMs <= CALM_FRAME_MS) {
                backlogBoost = 4;
            } else if (pendingUploads >= baseBudget * 6) {
                backlogBoost = 2;
            }
            return TaskDispatcher.clampMaxUploadsPerFrame(baseBudget + backlogBoost);
        }

        return baseBudget;
    }

    public static long uploadTimeBudgetNanos() {
        return uploadTimeBudgetNanos(0);
    }

    public static long uploadTimeBudgetNanos(int pendingUploads) {
        if (lastFrameMs >= SPIKE_FRAME_MS || smoothedFrameMs >= SPIKE_FRAME_MS || spikeCooldownFrames > 0) {
            return SPIKE_UPLOAD_TIME_BUDGET_NANOS;
        }

        if (calmFrames >= 6 && smoothedFrameMs <= CALM_FRAME_MS && lastFrameMs <= CALM_FRAME_MS + 3.0f) {
            if (pendingUploads >= 64 && calmFrames >= 10 && lastFrameMs <= CALM_FRAME_MS) {
                return TURBO_UPLOAD_TIME_BUDGET_NANOS;
            }

            return CALM_UPLOAD_TIME_BUDGET_NANOS;
        }

        return DEFAULT_UPLOAD_TIME_BUDGET_NANOS;
    }

    public static int chooseRebuildScheduleBudget(int pendingRebuilds) {
        if (pendingRebuilds <= 0) {
            return 0;
        }

        if (lastFrameMs >= EXTREME_FRAME_MS) {
            consumeCooldownFrame();
            return 1;
        }

        if (lastFrameMs >= SPIKE_FRAME_MS || smoothedFrameMs >= SPIKE_FRAME_MS || spikeCooldownFrames > 0) {
            consumeCooldownFrame();
            return Math.max(1, Math.min(4, pendingRebuilds));
        }

        if (calmFrames >= 10 && smoothedFrameMs <= CALM_FRAME_MS && lastFrameMs <= CALM_FRAME_MS) {
            if (pendingRebuilds >= 64) {
                return 28;
            }

            if (pendingRebuilds >= 32) {
                return 20;
            }
        }

        return 12;
    }

    private static void consumeCooldownFrame() {
        if (spikeCooldownFrames > 0) {
            spikeCooldownFrames--;
        }
    }

    static void reset() {
        smoothedFrameMs = CALM_FRAME_MS;
        lastFrameMs = CALM_FRAME_MS;
        spikeCooldownFrames = 0;
        calmFrames = 0;
        hasSamples = false;
    }
}

