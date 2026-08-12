package net.vulkanmod.render.optimization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkFrameDiagnosticsTest {
    @Test
    void spikeThresholdIsInclusiveAtTwentyFiveMilliseconds() {
        assertFalse(ChunkFrameDiagnostics.isSpike(Math.nextDown(25.0f)));
        assertTrue(ChunkFrameDiagnostics.isSpike(25.0f));
        assertTrue(ChunkFrameDiagnostics.isSpike(100.0f));
    }

    @Test
    void spikeLineAttributesEveryBucket() {
        String line = ChunkFrameDiagnostics.formatSpikeLine(
                41.2f, 12L, 3,
                3_100_000L, 9, 6 * 1024 * 1024, 31,
                2, 24 * 1024 * 1024,
                800_000L, 2, 600_000L, 500_000L, 213, 17,
                1_900_000L, 5_300_000L,
                4, 5,
                3, 87_200_000L, 45_100_000L,
                14_600_000L, 2_500_000L);

        assertEquals("[ChunkDiag] spike 41.2ms (gc 12ms, +3 quiet): upload 3.10ms 9 sec 6144KB uQ 31 | "
                + "realloc 2x 24576KB | sched 0.80ms prep 2x 0.60ms max 0.50ms rQ 213+17 | "
                + "graph 1.90ms | fence 5.30ms | "
                + "areas -4 +5 | build 3x 87.2ms max 45.1ms | gpuWait 14.6ms | present 2.5ms", line);
    }
}
