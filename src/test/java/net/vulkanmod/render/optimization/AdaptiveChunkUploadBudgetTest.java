package net.vulkanmod.render.optimization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveChunkUploadBudgetTest {
    private static final int LARGE_UPLOAD_BACKLOG = 128;
    private static final int LARGE_REBUILD_BACKLOG = 128;

    @BeforeEach
    void resetBudget() {
        AdaptiveChunkUploadBudget.reset();
    }

    @Test
    void freshStateUsesTheConfiguredCeilingWithStableCaps() {
        assertEquals(new Decision(16, 2_000_000L, 6_291_456L, 8, 1_000_000L), decision());
        assertEquals(0, AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(0));
        assertEquals(0L, AdaptiveChunkUploadBudget.chooseRebuildScheduleTimeBudgetNanos(0));
        assertEquals("U0 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void unrelatedFrameSpikesNeverThrottleChunkLoading() {
        AdaptiveChunkUploadBudget.recordFrameTimeMs(250.0f);

        assertEquals(new Decision(16, 2_000_000L, 6_291_456L, 8, 1_000_000L), decision());
        assertEquals("U0 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void unrelatedGpuWaitWithoutRecentChunkWorkDoesNotThrottleLoading() {
        AdaptiveChunkUploadBudget.recordGpuBackpressure(100_000_000L);
        AdaptiveChunkUploadBudget.recordFrameTimeMs(100.0f);

        assertEquals(new Decision(16, 2_000_000L, 6_291_456L, 8, 1_000_000L), decision());
        assertEquals("U0 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void gpuBackpressureWithCurrentChunkWorkStepsDownGradually() {
        AdaptiveChunkUploadBudget.recordUploadWork(100_000L);
        AdaptiveChunkUploadBudget.recordGpuBackpressure(20_000_000L);
        AdaptiveChunkUploadBudget.recordFrameTimeMs(25.0f);

        assertEquals(8, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals("U0 G2 R0", AdaptiveChunkUploadBudget.pressureSummary());

        AdaptiveChunkUploadBudget.recordFrameTimeMs(25.0f);
        assertEquals(12, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals("U0 G1 R0", AdaptiveChunkUploadBudget.pressureSummary());

        AdaptiveChunkUploadBudget.recordFrameTimeMs(25.0f);
        assertEquals(16, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals("U0 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void gpuBackpressureFromTheNextFewFramesIsAttributedToRecentUploads() {
        finishFrameWithUploadWork(100_000L);
        AdaptiveChunkUploadBudget.recordGpuBackpressure(20_000_000L);
        AdaptiveChunkUploadBudget.recordFrameTimeMs(25.0f);

        assertEquals(8, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals("U0 G2 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void anExpensiveUploadStepsDownThenRecoversWithoutAThroughputCliff() {
        finishFrameWithUploadWork(5_000_000L);
        assertEquals(8, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals(1_000_000L, AdaptiveChunkUploadBudget.uploadTimeBudgetNanos(LARGE_UPLOAD_BACKLOG));
        assertEquals(3_145_728L, AdaptiveChunkUploadBudget.uploadByteBudget());
        assertEquals(8, AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(LARGE_REBUILD_BACKLOG));
        assertEquals("U2 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());

        finishFrameWithUploadWork(100_000L);
        assertEquals(12, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals(1_500_000L, AdaptiveChunkUploadBudget.uploadTimeBudgetNanos(LARGE_UPLOAD_BACKLOG));
        assertEquals(4_194_304L, AdaptiveChunkUploadBudget.uploadByteBudget());

        finishFrameWithUploadWork(100_000L);
        assertEquals(16, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals("U0 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void moderateUploadCostUsesTheMiddleTier() {
        finishFrameWithUploadWork(2_500_000L);

        assertEquals(12, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals(1_500_000L, AdaptiveChunkUploadBudget.uploadTimeBudgetNanos(LARGE_UPLOAD_BACKLOG));
        assertEquals(4_194_304L, AdaptiveChunkUploadBudget.uploadByteBudget());
        assertEquals("U1 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void expensiveSnapshotPreparationOnlyReducesRebuildScheduling() {
        finishFrameWithRebuildWork(3_000_000L);

        assertEquals(16, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals(4, AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(LARGE_REBUILD_BACKLOG));
        assertEquals(500_000L,
                AdaptiveChunkUploadBudget.chooseRebuildScheduleTimeBudgetNanos(LARGE_REBUILD_BACKLOG));
        assertEquals("U0 G0 R2", AdaptiveChunkUploadBudget.pressureSummary());

        finishFrameWithRebuildWork(100_000L);
        assertEquals(6, AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(LARGE_REBUILD_BACKLOG));
        finishFrameWithRebuildWork(100_000L);
        assertEquals(8, AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(LARGE_REBUILD_BACKLOG));
    }

    @Test
    void uploadAndRebuildPressureAreIndependent() {
        AdaptiveChunkUploadBudget.recordUploadWork(5_000_000L);
        AdaptiveChunkUploadBudget.recordRebuildPreparation(1_500_000L);
        AdaptiveChunkUploadBudget.recordFrameTimeMs(10.0f);

        assertEquals(8, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
        assertEquals(6, AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(LARGE_REBUILD_BACKLOG));
        assertEquals("U2 G0 R1", AdaptiveChunkUploadBudget.pressureSummary());
    }

    @Test
    void adaptiveCountNeverExceedsTheConfiguredCeiling() {
        finishFrameWithUploadWork(5_000_000L);

        assertEquals(1, AdaptiveChunkUploadBudget.chooseBudget(Integer.MIN_VALUE, LARGE_UPLOAD_BACKLOG));
        assertEquals(1, AdaptiveChunkUploadBudget.chooseBudget(0, LARGE_UPLOAD_BACKLOG));
        assertEquals(1, AdaptiveChunkUploadBudget.chooseBudget(1, LARGE_UPLOAD_BACKLOG));
        assertEquals(3, AdaptiveChunkUploadBudget.chooseBudget(3, LARGE_UPLOAD_BACKLOG));
        assertEquals(8, AdaptiveChunkUploadBudget.chooseBudget(Integer.MAX_VALUE, LARGE_UPLOAD_BACKLOG));
        assertEquals(16, AdaptiveChunkUploadBudget.chooseBudget(16),
                "the no-backlog overload still returns the configured ceiling");
    }

    @Test
    void highRefreshRateJitterWithCheapChunkWorkKeepsFullThroughput() {
        for (int i = 0; i < 50; i++) {
            AdaptiveChunkUploadBudget.recordUploadWork(100_000L);
            AdaptiveChunkUploadBudget.recordRebuildPreparation(100_000L);
            AdaptiveChunkUploadBudget.recordFrameTimeMs(i % 2 == 0 ? 7.0f : 40.0f);
            assertEquals(16, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
            assertEquals(8, AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(LARGE_REBUILD_BACKLOG));
        }
    }

    @Test
    void nonemptyQueuesAlwaysRetainAUsefulStarvationPreventionBudget() {
        finishFrameWithUploadWork(Long.MAX_VALUE);
        finishFrameWithRebuildWork(Long.MAX_VALUE);

        assertTrue(AdaptiveChunkUploadBudget.chooseBudget(0, LARGE_UPLOAD_BACKLOG) >= 1);
        assertTrue(AdaptiveChunkUploadBudget.uploadTimeBudgetNanos(LARGE_UPLOAD_BACKLOG) > 0L);
        assertTrue(AdaptiveChunkUploadBudget.uploadByteBudget() > 0L);
        assertTrue(AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(1) >= 1);
        assertTrue(AdaptiveChunkUploadBudget.chooseRebuildScheduleTimeBudgetNanos(1) > 0L);
    }

    @Test
    void invalidFrameSamplesDoNotFinalizeOrChangePressure() {
        AdaptiveChunkUploadBudget.recordUploadWork(5_000_000L);
        Decision fresh = decision();

        float[] invalid = {0.0f, -1.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (float sample : invalid) {
            AdaptiveChunkUploadBudget.recordFrameTimeMs(sample);
            assertEquals(fresh, decision());
        }

        AdaptiveChunkUploadBudget.recordFrameTimeMs(10.0f);
        assertEquals(8, AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG));
    }

    @Test
    void publicResetRestoresFreshStateAndClearsPendingSamples() {
        AdaptiveChunkUploadBudget.recordUploadWork(Long.MAX_VALUE);
        AdaptiveChunkUploadBudget.recordRebuildPreparation(Long.MAX_VALUE);
        AdaptiveChunkUploadBudget.recordFrameTimeMs(10.0f);
        assertEquals("U2 G0 R2", AdaptiveChunkUploadBudget.pressureSummary());

        AdaptiveChunkUploadBudget.recordUploadWork(Long.MAX_VALUE);
        AdaptiveChunkUploadBudget.reset();
        assertEquals(new Decision(16, 2_000_000L, 6_291_456L, 8, 1_000_000L), decision());
        assertEquals("U0 G0 R0", AdaptiveChunkUploadBudget.pressureSummary());
    }

    private static void finishFrameWithUploadWork(long nanos) {
        AdaptiveChunkUploadBudget.recordUploadWork(nanos);
        AdaptiveChunkUploadBudget.recordFrameTimeMs(10.0f);
    }

    private static void finishFrameWithRebuildWork(long nanos) {
        AdaptiveChunkUploadBudget.recordRebuildPreparation(nanos);
        AdaptiveChunkUploadBudget.recordFrameTimeMs(10.0f);
    }

    private static Decision decision() {
        return new Decision(
                AdaptiveChunkUploadBudget.chooseBudget(16, LARGE_UPLOAD_BACKLOG),
                AdaptiveChunkUploadBudget.uploadTimeBudgetNanos(LARGE_UPLOAD_BACKLOG),
                AdaptiveChunkUploadBudget.uploadByteBudget(),
                AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(LARGE_REBUILD_BACKLOG),
                AdaptiveChunkUploadBudget.chooseRebuildScheduleTimeBudgetNanos(LARGE_REBUILD_BACKLOG));
    }

    private record Decision(int uploads, long uploadTimeNanos, long uploadBytes,
                            int rebuilds, long rebuildTimeNanos) {
    }
}
