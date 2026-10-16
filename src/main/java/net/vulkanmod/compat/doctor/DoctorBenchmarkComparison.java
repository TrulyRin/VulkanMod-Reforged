package net.vulkanmod.compat.doctor;

import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;

import java.util.Objects;

public record DoctorBenchmarkComparison(
        BenchmarkResult before,
        BenchmarkResult after) {

    public DoctorBenchmarkComparison {
        before = Objects.requireNonNull(before, "before");
        after = Objects.requireNonNull(after, "after");
    }

    public double averageFpsDelta() {
        return after.averageFps() - before.averageFps();
    }

    public double onePercentLowDelta() {
        return after.onePercentLowFps() - before.onePercentLowFps();
    }

    public double p95FrameTimeDelta() {
        return after.p95FrameTimeMs() - before.p95FrameTimeMs();
    }

    public int framesOver50MsDelta() {
        return after.framesOver50Ms() - before.framesOver50Ms();
    }
}
