package net.vulkanmod.diagnostics.benchmark;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class CurrentSettingsBenchmark {
    public static final long DEFAULT_COUNTDOWN_NANOS = 3_000_000_000L;
    public static final long DEFAULT_SAMPLE_NANOS = 10_000_000_000L;
    public static final int DEFAULT_MINIMUM_SAMPLES = 30;
    private static final int DEFAULT_CAPACITY = 16_384;

    private final long countdownNanos;
    private final long sampleNanos;
    private final int minimumSamples;
    private final Supplier<Instant> wallClock;
    private final long[] frameNanos;
    private final long[] cpuNanos;

    private BenchmarkState state = BenchmarkState.IDLE;
    private BenchmarkCancelReason reason = BenchmarkCancelReason.NONE;
    private BenchmarkFingerprint fingerprint;
    private long runId;
    private long worldSessionId;
    private BenchmarkResult latestResult;
    private long countdownStartedAt;
    private long runStartedAt;
    private long previousFrameAt = -1L;
    private int sampleCount;

    public CurrentSettingsBenchmark() {
        this(DEFAULT_COUNTDOWN_NANOS, DEFAULT_SAMPLE_NANOS, DEFAULT_MINIMUM_SAMPLES,
                DEFAULT_CAPACITY, Instant::now);
    }

    CurrentSettingsBenchmark(long countdownNanos, long sampleNanos, int minimumSamples,
                             int capacity, Supplier<Instant> wallClock) {
        if (countdownNanos < 0 || sampleNanos <= 0 || minimumSamples < 1 || capacity < minimumSamples) {
            throw new IllegalArgumentException("Invalid benchmark timing or capacity");
        }
        this.countdownNanos = countdownNanos;
        this.sampleNanos = sampleNanos;
        this.minimumSamples = minimumSamples;
        this.wallClock = Objects.requireNonNull(wallClock, "wallClock");
        this.frameNanos = new long[capacity];
        this.cpuNanos = new long[capacity];
    }

    public boolean start(BenchmarkFingerprint fingerprint, long nowNanos) {
        return start(fingerprint, nowNanos, 0L, 0L);
    }

    public boolean start(BenchmarkFingerprint fingerprint, long nowNanos,
                         long runId, long worldSessionId) {
        if (isActive()) {
            return false;
        }
        if (runId < 0 || worldSessionId < 0) {
            throw new IllegalArgumentException("Benchmark identifiers must not be negative");
        }

        this.fingerprint = Objects.requireNonNull(fingerprint, "fingerprint");
        this.runId = runId;
        this.worldSessionId = worldSessionId;
        this.state = BenchmarkState.COUNTDOWN;
        this.reason = BenchmarkCancelReason.NONE;
        this.countdownStartedAt = nowNanos;
        this.runStartedAt = 0L;
        this.previousFrameAt = -1L;
        this.sampleCount = 0;
        return true;
    }

    public void advance(long nowNanos) {
        if (state != BenchmarkState.COUNTDOWN) {
            return;
        }
        if (nowNanos - countdownStartedAt >= countdownNanos) {
            state = BenchmarkState.RUNNING;
            runStartedAt = nowNanos;
            previousFrameAt = -1L;
        }
    }

    public void recordFrame(long nowNanos, long cpuRenderNanos) {
        if (state != BenchmarkState.RUNNING) {
            return;
        }

        if (previousFrameAt < 0L) {
            previousFrameAt = nowNanos;
            return;
        }

        long interval = nowNanos - previousFrameAt;
        previousFrameAt = nowNanos;
        if (interval > 0L && sampleCount < frameNanos.length) {
            frameNanos[sampleCount] = interval;
            cpuNanos[sampleCount] = Math.max(0L, cpuRenderNanos);
            sampleCount++;
        }

        if (nowNanos - runStartedAt >= sampleNanos || sampleCount == frameNanos.length) {
            complete(nowNanos);
        }
    }

    public void cancel(BenchmarkCancelReason cancelReason) {
        if (!isActive()) {
            return;
        }
        state = BenchmarkState.CANCELLED;
        reason = Objects.requireNonNull(cancelReason, "cancelReason");
    }

    private void complete(long nowNanos) {
        if (sampleCount < minimumSamples) {
            state = BenchmarkState.FAILED;
            reason = BenchmarkCancelReason.TOO_FEW_FRAMES;
            return;
        }

        long[] sortedFrames = Arrays.copyOf(frameNanos, sampleCount);
        Arrays.sort(sortedFrames);

        long totalFrameNanos = 0L;
        long totalCpuNanos = 0L;
        int over50Ms = 0;
        for (int i = 0; i < sampleCount; i++) {
            totalFrameNanos += frameNanos[i];
            totalCpuNanos += cpuNanos[i];
            if (frameNanos[i] > 50_000_000L) {
                over50Ms++;
            }
        }

        int slowestCount = Math.max(1, (int) Math.ceil(sampleCount * 0.01d));
        long slowestTotal = 0L;
        for (int i = sampleCount - slowestCount; i < sampleCount; i++) {
            slowestTotal += sortedFrames[i];
        }
        double slowestAverage = (double) slowestTotal / slowestCount;
        int p95Index = Math.max(0, (int) Math.ceil(sampleCount * 0.95d) - 1);
        long actualDuration = Math.max(1L, nowNanos - runStartedAt);

        latestResult = new BenchmarkResult(
                runId,
                worldSessionId,
                fingerprint,
                1_000_000_000d * sampleCount / totalFrameNanos,
                1_000_000_000d / slowestAverage,
                totalFrameNanos / (double) sampleCount / 1_000_000d,
                sortedFrames[p95Index] / 1_000_000d,
                totalCpuNanos / (double) sampleCount / 1_000_000d,
                over50Ms,
                sampleCount,
                actualDuration / 1_000_000_000d,
                wallClock.get());
        state = BenchmarkState.COMPLETED;
        reason = BenchmarkCancelReason.NONE;
    }

    public boolean isActive() {
        return state == BenchmarkState.COUNTDOWN || state == BenchmarkState.RUNNING;
    }

    public BenchmarkState state() {
        return state;
    }

    public BenchmarkCancelReason reason() {
        return reason;
    }

    public Optional<BenchmarkResult> latestResult() {
        return Optional.ofNullable(latestResult);
    }

    public int sampleCount() {
        return sampleCount;
    }

    public long countdownRemainingNanos(long nowNanos) {
        if (state != BenchmarkState.COUNTDOWN) {
            return 0L;
        }
        return Math.max(0L, countdownNanos - (nowNanos - countdownStartedAt));
    }
}
