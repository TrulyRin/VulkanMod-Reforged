package net.vulkanmod.render.optimization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSchedulingPolicyTest {
    @Test
    void builderThreadsLeaveHeadroomAndHaveAHardCap() {
        assertEquals(1, ChunkSchedulingPolicy.chooseBuilderThreadCount(Integer.MIN_VALUE));
        assertEquals(1, ChunkSchedulingPolicy.chooseBuilderThreadCount(1));
        assertEquals(2, ChunkSchedulingPolicy.chooseBuilderThreadCount(4));
        assertEquals(4, ChunkSchedulingPolicy.chooseBuilderThreadCount(8));
        assertEquals(8, ChunkSchedulingPolicy.chooseBuilderThreadCount(16));
        assertEquals(8, ChunkSchedulingPolicy.chooseBuilderThreadCount(Integer.MAX_VALUE));
    }

    @Test
    void integratedServerReservesExtraCpuForWorldGenerationAndTicks() {
        assertEquals(1, ChunkSchedulingPolicy.chooseBuilderThreadCount(1, true));
        assertEquals(1, ChunkSchedulingPolicy.chooseBuilderThreadCount(4, true));
        assertEquals(2, ChunkSchedulingPolicy.chooseBuilderThreadCount(8, true));
        assertEquals(4, ChunkSchedulingPolicy.chooseBuilderThreadCount(16, true));
        assertEquals(8, ChunkSchedulingPolicy.chooseBuilderThreadCount(Integer.MAX_VALUE, true));

        assertEquals(4, ChunkSchedulingPolicy.chooseBuilderThreadCount(8, false));
    }

    @Test
    void configuredBuilderThreadsUseAutoOrClampManualValues() {
        assertEquals(2, ChunkSchedulingPolicy.resolveBuilderThreadCount(0, 8, true));
        assertEquals(4, ChunkSchedulingPolicy.resolveBuilderThreadCount(0, 8, false));
        assertEquals(1, ChunkSchedulingPolicy.resolveBuilderThreadCount(1, 8, false));
        assertEquals(8, ChunkSchedulingPolicy.resolveBuilderThreadCount(99, 8, false));
        assertEquals(1, ChunkSchedulingPolicy.resolveBuilderThreadCount(-1, 1, true));
    }

    @Test
    void queueCapsKeepWorkersFedWithoutUnboundedMemoryGrowth() {
        assertEquals(64, ChunkSchedulingPolicy.MAX_QUEUED_BUILD_TASKS);
        assertEquals(64, ChunkSchedulingPolicy.MAX_PENDING_COMPILE_RESULTS);
        assertEquals(64, ChunkSchedulingPolicy.MAX_REBUILD_QUEUE_SCANS_PER_FRAME);
        assertEquals(64, ChunkSchedulingPolicy.MAX_DELAYED_REBUILD_PROMOTIONS_PER_FRAME);
    }

    @Test
    void disabledAdaptiveModeStillHasCountTimeAndByteSafetyCaps() {
        assertEquals(0, ChunkSchedulingPolicy.fixedRebuildScheduleBudget(0));
        assertEquals(1, ChunkSchedulingPolicy.fixedRebuildScheduleBudget(1));
        assertEquals(8, ChunkSchedulingPolicy.fixedRebuildScheduleBudget(128));
        assertEquals(0L, ChunkSchedulingPolicy.fixedRebuildTimeBudgetNanos(0));
        assertEquals(1_000_000L, ChunkSchedulingPolicy.fixedRebuildTimeBudgetNanos(1));
        assertEquals(2_000_000L, ChunkSchedulingPolicy.fixedUploadTimeBudgetNanos());
        assertEquals(8_388_608L, ChunkSchedulingPolicy.fixedUploadByteBudget());
    }

    @Test
    void uploadCeilingClampHandlesMalformedLegacyValues() {
        assertEquals(1, ChunkSchedulingPolicy.clampUploadCeiling(Integer.MIN_VALUE));
        assertEquals(1, ChunkSchedulingPolicy.clampUploadCeiling(0));
        assertEquals(1, ChunkSchedulingPolicy.clampUploadCeiling(1));
        assertEquals(16, ChunkSchedulingPolicy.clampUploadCeiling(16));
        assertEquals(16, ChunkSchedulingPolicy.clampUploadCeiling(17));
        assertEquals(16, ChunkSchedulingPolicy.clampUploadCeiling(Integer.MAX_VALUE));
    }

    @Test
    void lowPriorityWorkCannotBeStarvedByAnEndlessHighPriorityQueue() {
        assertFalse(ChunkSchedulingPolicy.shouldServeLowPriority(0, true, true));
        assertFalse(ChunkSchedulingPolicy.shouldServeLowPriority(7, true, true));
        assertTrue(ChunkSchedulingPolicy.shouldServeLowPriority(8, true, true));
        assertTrue(ChunkSchedulingPolicy.shouldServeLowPriority(0, false, true));
        assertFalse(ChunkSchedulingPolicy.shouldServeLowPriority(100, true, false));
    }
}
