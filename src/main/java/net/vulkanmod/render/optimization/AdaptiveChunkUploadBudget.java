package net.vulkanmod.render.optimization;

public final class AdaptiveChunkUploadBudget {
    private static final int PRESSURE_NONE = 0;
    private static final int PRESSURE_MODERATE = 1;
    private static final int PRESSURE_HIGH = 2;

    private static final long UPLOAD_SOFT_LIMIT_NANOS = 2_000_000L;
    private static final long UPLOAD_HARD_LIMIT_NANOS = 4_000_000L;
    private static final long GPU_BACKPRESSURE_SOFT_LIMIT_NANOS = 4_000_000L;
    private static final long GPU_BACKPRESSURE_HARD_LIMIT_NANOS = 12_000_000L;
    private static final long REBUILD_SOFT_LIMIT_NANOS = 1_000_000L;
    private static final long REBUILD_HARD_LIMIT_NANOS = 2_000_000L;
    private static final int RECENT_UPLOAD_ACTIVITY_FRAMES = 4;

    private static final long NORMAL_UPLOAD_TIME_BUDGET_NANOS = 2_000_000L;
    private static final long MODERATE_UPLOAD_TIME_BUDGET_NANOS = 1_500_000L;
    private static final long HIGH_UPLOAD_TIME_BUDGET_NANOS = 1_000_000L;

    private static final long NORMAL_UPLOAD_BYTE_BUDGET = 6L * 1024 * 1024;
    private static final long MODERATE_UPLOAD_BYTE_BUDGET = 4L * 1024 * 1024;
    private static final long HIGH_UPLOAD_BYTE_BUDGET = 3L * 1024 * 1024;

    private static final int NORMAL_REBUILD_SCHEDULES = 8;
    private static final int MODERATE_REBUILD_SCHEDULES = 6;
    private static final int HIGH_REBUILD_SCHEDULES = 4;

    private static final long NORMAL_REBUILD_TIME_BUDGET_NANOS = 1_000_000L;
    private static final long MODERATE_REBUILD_TIME_BUDGET_NANOS = 750_000L;
    private static final long HIGH_REBUILD_TIME_BUDGET_NANOS = 500_000L;

    private static long frameUploadWorkNanos;
    private static long frameGpuBackpressureNanos;
    private static long frameRebuildPreparationNanos;
    private static int uploadPressure;
    private static int gpuPressure;
    private static int rebuildPressure;
    private static int recentUploadActivityFrames;

    private AdaptiveChunkUploadBudget() {
    }

    public static void recordUploadWork(long nanos) {
        if (nanos > 0L) {
            frameUploadWorkNanos = saturatingAdd(frameUploadWorkNanos, nanos);
        }
    }

    public static void recordRebuildPreparation(long nanos) {
        if (nanos > 0L) {
            frameRebuildPreparationNanos = saturatingAdd(frameRebuildPreparationNanos, nanos);
        }
    }

    public static void recordGpuBackpressure(long nanos) {
        if (nanos > 0L && (frameUploadWorkNanos > 0L || recentUploadActivityFrames > 0)) {
            frameGpuBackpressureNanos = saturatingAdd(frameGpuBackpressureNanos, nanos);
        }
    }

    public static void recordFrameTimeMs(float frameTimeMs) {
        if (!Float.isFinite(frameTimeMs) || frameTimeMs <= 0.0f) {
            return;
        }

        uploadPressure = nextPressure(uploadPressure, frameUploadWorkNanos,
                UPLOAD_SOFT_LIMIT_NANOS, UPLOAD_HARD_LIMIT_NANOS);
        gpuPressure = nextPressure(gpuPressure, frameGpuBackpressureNanos,
                GPU_BACKPRESSURE_SOFT_LIMIT_NANOS, GPU_BACKPRESSURE_HARD_LIMIT_NANOS);
        rebuildPressure = nextPressure(rebuildPressure, frameRebuildPreparationNanos,
                REBUILD_SOFT_LIMIT_NANOS, REBUILD_HARD_LIMIT_NANOS);

        if (frameUploadWorkNanos > 0L) {
            recentUploadActivityFrames = RECENT_UPLOAD_ACTIVITY_FRAMES;
        } else if (recentUploadActivityFrames > 0) {
            recentUploadActivityFrames--;
        }

        frameUploadWorkNanos = 0L;
        frameGpuBackpressureNanos = 0L;
        frameRebuildPreparationNanos = 0L;
    }

    public static int chooseBudget(int configuredUploadsPerFrame) {
        return chooseBudget(configuredUploadsPerFrame, 0);
    }

    public static int chooseBudget(int configuredUploadsPerFrame, int pendingUploads) {
        int ceiling = ChunkSchedulingPolicy.clampUploadCeiling(configuredUploadsPerFrame);
        int effectivePressure = effectiveUploadPressure();
        if (pendingUploads <= 0 || effectivePressure == PRESSURE_NONE) {
            return ceiling;
        }

        if (effectivePressure == PRESSURE_HIGH) {
            return Math.min(ceiling, Math.max(4, (ceiling + 1) / 2));
        }

        return Math.min(ceiling, Math.max(6, (ceiling * 3 + 3) / 4));
    }

    public static long uploadTimeBudgetNanos() {
        return uploadTimeBudgetNanos(0);
    }

    public static long uploadTimeBudgetNanos(int pendingUploads) {
        return switch (effectiveUploadPressure()) {
            case PRESSURE_HIGH -> HIGH_UPLOAD_TIME_BUDGET_NANOS;
            case PRESSURE_MODERATE -> MODERATE_UPLOAD_TIME_BUDGET_NANOS;
            default -> NORMAL_UPLOAD_TIME_BUDGET_NANOS;
        };
    }

    public static long uploadByteBudget() {
        return switch (effectiveUploadPressure()) {
            case PRESSURE_HIGH -> HIGH_UPLOAD_BYTE_BUDGET;
            case PRESSURE_MODERATE -> MODERATE_UPLOAD_BYTE_BUDGET;
            default -> NORMAL_UPLOAD_BYTE_BUDGET;
        };
    }

    public static int chooseRebuildScheduleBudget(int pendingRebuilds) {
        if (pendingRebuilds <= 0) {
            return 0;
        }

        int budget = switch (rebuildPressure) {
            case PRESSURE_HIGH -> HIGH_REBUILD_SCHEDULES;
            case PRESSURE_MODERATE -> MODERATE_REBUILD_SCHEDULES;
            default -> NORMAL_REBUILD_SCHEDULES;
        };
        return Math.min(pendingRebuilds, budget);
    }

    public static long chooseRebuildScheduleTimeBudgetNanos(int pendingRebuilds) {
        if (pendingRebuilds <= 0) {
            return 0L;
        }

        return switch (rebuildPressure) {
            case PRESSURE_HIGH -> HIGH_REBUILD_TIME_BUDGET_NANOS;
            case PRESSURE_MODERATE -> MODERATE_REBUILD_TIME_BUDGET_NANOS;
            default -> NORMAL_REBUILD_TIME_BUDGET_NANOS;
        };
    }

    public static String pressureSummary() {
        return "U" + uploadPressure + " G" + gpuPressure + " R" + rebuildPressure;
    }

    public static void reset() {
        frameUploadWorkNanos = 0L;
        frameGpuBackpressureNanos = 0L;
        frameRebuildPreparationNanos = 0L;
        uploadPressure = PRESSURE_NONE;
        gpuPressure = PRESSURE_NONE;
        rebuildPressure = PRESSURE_NONE;
        recentUploadActivityFrames = 0;
    }

    private static int effectiveUploadPressure() {
        return Math.max(uploadPressure, gpuPressure);
    }

    private static int nextPressure(int currentPressure, long workNanos,
                                    long softLimitNanos, long hardLimitNanos) {
        if (workNanos >= hardLimitNanos) {
            return PRESSURE_HIGH;
        }
        if (workNanos >= softLimitNanos) {
            return Math.max(PRESSURE_MODERATE, currentPressure);
        }
        return Math.max(PRESSURE_NONE, currentPressure - 1);
    }

    private static long saturatingAdd(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
