package net.vulkanmod.render.optimization;

import net.vulkanmod.compat.observer.CompatProfiler;

import java.util.function.LongSupplier;

                                                                      
public final class FramePerformanceMetrics {
    public static final FramePerformanceMetrics INSTANCE = new FramePerformanceMetrics(System::nanoTime);
    private final LongSupplier clock;
    private long startedAt;
    private boolean active;

    public FramePerformanceMetrics(LongSupplier clock) { this.clock = clock; }

    public void beginFrame() {
        startedAt = clock.getAsLong();
        active = true;
        CompatProfiler.cpuFrameStart = startedAt;
    }

    public void recordChunkUpload(long nanos, boolean performedChunkWork, boolean adaptive) {
        ChunkFrameDiagnostics.recordUploadPhase(Math.max(0L, nanos));
        if (adaptive && performedChunkWork) AdaptiveChunkUploadBudget.recordUploadWork(nanos);
    }

    public void recordInFlightWait(long nanos, boolean adaptive) {
        ChunkFrameDiagnostics.recordGpuWait(Math.max(0L, nanos));
        if (adaptive) AdaptiveChunkUploadBudget.recordGpuBackpressure(nanos);
    }

    public void recordTransferWait(long nanos, boolean adaptive) {
        ChunkFrameDiagnostics.recordFenceWait(Math.max(0L, nanos));
        if (adaptive) AdaptiveChunkUploadBudget.recordGpuBackpressure(nanos);
    }

    public void recordAcquireWait(long nanos, boolean adaptive) {
                                                                               
        ChunkFrameDiagnostics.recordGpuWait(Math.max(0L, nanos));
    }

    public void recordPresentWait(long nanos, boolean adaptive) {
        ChunkFrameDiagnostics.recordPresentWait(Math.max(0L, nanos));
    }

    public void finishFrame(boolean adaptive) {
        if (!active) return;
        active = false;
        long nanos = Math.max(1L, clock.getAsLong() - startedAt);
        float cpuRenderTimeMs = nanos * 0.000001f;
        if (adaptive) AdaptiveChunkUploadBudget.recordFrameTimeMs(cpuRenderTimeMs);
        else AdaptiveChunkUploadBudget.reset();
        if (CompatProfiler.ENABLED) CompatProfiler.recordFrame(cpuRenderTimeMs);
        CompatProfiler.resetFrameCounters();
        CompatProfiler.cpuFrameStart = 0L;
        ChunkFrameDiagnostics.endFrame(cpuRenderTimeMs);
    }
}
