package net.vulkanmod.diagnostics.benchmark;

import java.time.Instant;
import java.util.Objects;

public record BenchmarkResult(
        long runId,
        long worldSessionId,
        BenchmarkFingerprint fingerprint,
        double averageFps,
        double onePercentLowFps,
        double averageFrameTimeMs,
        double p95FrameTimeMs,
        double averageCpuRenderTimeMs,
        int framesOver50Ms,
        int frameCount,
        double durationSeconds,
        Instant completedAt) {

    public BenchmarkResult {
        if (runId < 0 || worldSessionId < 0) {
            throw new IllegalArgumentException("Benchmark identifiers must not be negative");
        }
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        completedAt = Objects.requireNonNull(completedAt, "completedAt");
    }

    public BenchmarkResult(
            BenchmarkFingerprint fingerprint,
            double averageFps,
            double onePercentLowFps,
            double averageFrameTimeMs,
            double p95FrameTimeMs,
            double averageCpuRenderTimeMs,
            int framesOver50Ms,
            int frameCount,
            double durationSeconds,
            Instant completedAt) {
        this(0L, 0L, fingerprint, averageFps, onePercentLowFps,
                averageFrameTimeMs, p95FrameTimeMs, averageCpuRenderTimeMs,
                framesOver50Ms, frameCount, durationSeconds, completedAt);
    }

    public boolean isStale(BenchmarkFingerprint current) {
        return !fingerprint.equals(current);
    }
}
