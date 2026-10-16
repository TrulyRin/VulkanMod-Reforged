package net.vulkanmod.render.optimization;

import net.vulkanmod.Initializer;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Behavior-neutral spike attribution for chunk streaming. All record calls
 * happen on the render thread; when a frame crosses the spike threshold, one
 * rate-limited log line states where the frame's chunk-related milliseconds
 * went so slow frames can be attributed to uploads, buffer churn, graph
 * work, fence waits, GC, or (when every bucket is near zero) work outside
 * the renderer such as integrated-server worldgen.
 */
public final class ChunkFrameDiagnostics {
    static final float SPIKE_FRAME_MS = 25.0f;
    private static final long MIN_LOG_INTERVAL_NANOS = 250_000_000L;

    private static final long STATS_INTERVAL_NANOS = 30_000_000_000L;

    private static List<GarbageCollectorMXBean> gcBeans;
    private static long lastGcTimeMs = -1;
    private static long lastLogNanos;
    private static long lastStatsLogNanos;
    private static int suppressedSpikes;

    private static long uploadPhaseNanos;
    private static int uploadedSections;
    private static long uploadedBytes;
    private static int pendingUploadsAfter;
    private static long graphUpdateNanos;
    private static long scheduleNanos;
    private static int readyRebuilds;
    private static int delayedRebuilds;
    private static int rebuildPreparations;
    private static long rebuildPreparationNanos;
    private static long rebuildPreparationMaxNanos;
    private static long fenceWaitNanos;
    private static int reallocCount;
    private static long reallocCopiedBytes;
    private static int areasReleased;
    private static int areaBuffersCreated;
    private static long gpuWaitNanos;
    private static long presentWaitNanos;

                                                                       
    private static final AtomicInteger workerBuildsCompleted = new AtomicInteger();
    private static final AtomicLong workerBuildNanosTotal = new AtomicLong();
    private static final AtomicLong workerBuildNanosMax = new AtomicLong();

                                                                            
                                                               
    private static final AtomicInteger leafDirtySkips = new AtomicInteger();
    private static final AtomicInteger rebuildDefers = new AtomicInteger();

    private ChunkFrameDiagnostics() {
    }

    public static void recordUploadPhase(long nanos) {
        uploadPhaseNanos += nanos;
    }

    public static void recordUploads(int completedSections, long stagedBytes, int pendingAfter) {
        uploadedSections += completedSections;
        uploadedBytes += stagedBytes;
        pendingUploadsAfter = pendingAfter;
    }

    public static void recordGraphUpdate(long nanos) {
        graphUpdateNanos += nanos;
    }

    public static void recordSchedule(long nanos, int readyRebuildCount, int delayedRebuildCount) {
        scheduleNanos += nanos;
        readyRebuilds = readyRebuildCount;
        delayedRebuilds = delayedRebuildCount;
    }

    public static void recordRebuildPreparation(long nanos) {
        rebuildPreparations++;
        rebuildPreparationNanos += nanos;
        rebuildPreparationMaxNanos = Math.max(rebuildPreparationMaxNanos, nanos);
    }

    public static void recordFenceWait(long nanos) {
        fenceWaitNanos += nanos;
    }

    public static void recordRealloc(long copiedBytes) {
        reallocCount++;
        reallocCopiedBytes += copiedBytes;
    }

    public static void recordAreaReleased() {
        areasReleased++;
    }

    public static void recordAreaBufferCreated() {
        areaBuffersCreated++;
    }

    public static void recordBuild(long nanos) {
        workerBuildsCompleted.incrementAndGet();
        workerBuildNanosTotal.addAndGet(nanos);
        workerBuildNanosMax.accumulateAndGet(nanos, Math::max);
    }

    public static void recordLeafDirtySkipped() {
        leafDirtySkips.incrementAndGet();
    }

    public static void recordRebuildDeferred() {
        rebuildDefers.incrementAndGet();
    }

    public static void recordGpuWait(long nanos) {
        gpuWaitNanos += nanos;
    }

    public static void recordPresentWait(long nanos) {
        presentWaitNanos += nanos;
    }

    public static void endFrame(float frameMs) {
        long gcDeltaMs = sampleGcDeltaMs();
        int builds = workerBuildsCompleted.getAndSet(0);
        long buildNanos = workerBuildNanosTotal.getAndSet(0);
        long buildMaxNanos = workerBuildNanosMax.getAndSet(0);

        if (isSpike(frameMs)) {
            long now = System.nanoTime();
            if (now - lastLogNanos >= MIN_LOG_INTERVAL_NANOS) {
                Initializer.LOGGER.info(formatSpikeLine(frameMs, gcDeltaMs, suppressedSpikes,
                        uploadPhaseNanos, uploadedSections, uploadedBytes, pendingUploadsAfter,
                        reallocCount, reallocCopiedBytes,
                        scheduleNanos, rebuildPreparations, rebuildPreparationNanos, rebuildPreparationMaxNanos,
                        readyRebuilds, delayedRebuilds, graphUpdateNanos, fenceWaitNanos,
                        areasReleased, areaBuffersCreated,
                        builds, buildNanos, buildMaxNanos,
                        gpuWaitNanos, presentWaitNanos));
                lastLogNanos = now;
                suppressedSpikes = 0;
            } else {
                suppressedSpikes++;
            }
        }

        uploadPhaseNanos = 0;
        uploadedSections = 0;
        uploadedBytes = 0;
        graphUpdateNanos = 0;
        scheduleNanos = 0;
        rebuildPreparations = 0;
        rebuildPreparationNanos = 0;
        rebuildPreparationMaxNanos = 0;
        fenceWaitNanos = 0;
        reallocCount = 0;
        reallocCopiedBytes = 0;
        areasReleased = 0;
        areaBuffersCreated = 0;
        gpuWaitNanos = 0;
        presentWaitNanos = 0;

        logPeriodicBufferStats();
    }

       
                                                                           
                                                                        
                                                                  
       
    private static void logPeriodicBufferStats() {
        long now = System.nanoTime();
        if (now - lastStatsLogNanos < STATS_INTERVAL_NANOS) {
            return;
        }
        lastStatsLogNanos = now;

        net.vulkanmod.render.chunk.WorldRenderer worldRenderer =
                net.vulkanmod.render.chunk.WorldRenderer.getInstance();
        if (worldRenderer == null || worldRenderer.getSectionGrid() == null) {
            return;
        }

        String adaptiveState = Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.pressureSummary()
                : "off";
        Initializer.LOGGER.info("[ChunkDiag] buffers: {} | leafDirtySkips {} | rebuildDefers {} | adaptive {}",
                String.join(" | ", worldRenderer.getChunkAreaManager().getStats()),
                leafDirtySkips.get(), rebuildDefers.get(), adaptiveState);
    }

    static boolean isSpike(float frameMs) {
        return frameMs >= SPIKE_FRAME_MS;
    }

    static String formatSpikeLine(float frameMs, long gcDeltaMs, int suppressed,
                                  long uploadNanos, int sections, long stagedBytes, int uQ,
                                  int reallocs, long reallocBytes,
                                  long schedNanos, int preparations, long preparationNanos,
                                  long preparationMaxNanos, int readyRQ, int delayedRQ,
                                  long graphNanos, long fenceNanos,
                                  int released, int created,
                                  int builds, long buildNanos, long buildMaxNanos,
                                  long gpuWaitNanos, long presentNanos) {
        return String.format(
                "[ChunkDiag] spike %.1fms (gc %dms, +%d quiet): upload %.2fms %d sec %dKB uQ %d | "
                        + "realloc %dx %dKB | sched %.2fms prep %dx %.2fms max %.2fms rQ %d+%d | "
                        + "graph %.2fms | fence %.2fms | "
                        + "areas -%d +%d | build %dx %.1fms max %.1fms | gpuWait %.1fms | present %.1fms",
                frameMs, gcDeltaMs, suppressed,
                uploadNanos / 1_000_000.0, sections, stagedBytes / 1024, uQ,
                reallocs, reallocBytes / 1024,
                schedNanos / 1_000_000.0,
                preparations, preparationNanos / 1_000_000.0, preparationMaxNanos / 1_000_000.0,
                readyRQ, delayedRQ,
                graphNanos / 1_000_000.0,
                fenceNanos / 1_000_000.0,
                released, created,
                builds, buildNanos / 1_000_000.0, buildMaxNanos / 1_000_000.0,
                gpuWaitNanos / 1_000_000.0, presentNanos / 1_000_000.0);
    }

    private static long sampleGcDeltaMs() {
        if (gcBeans == null) {
            gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        }

        long total = 0;
        for (int i = 0; i < gcBeans.size(); i++) {
            long time = gcBeans.get(i).getCollectionTime();
            if (time > 0) {
                total += time;
            }
        }

        long delta = lastGcTimeMs < 0 ? 0 : total - lastGcTimeMs;
        lastGcTimeMs = total;
        return Math.max(0, delta);
    }
}
