package net.vulkanmod.diagnostics.benchmark;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentSettingsBenchmarkTest {
    private static final BenchmarkFingerprint FINGERPRINT = new BenchmarkFingerprint(
            "minecraft:overworld", "singleplayer", 1920, 1080, 100,
            12, 8, "FANCY", true, 144, 2);

    @Test
    void followsCountdownRunningAndCompletedStates() {
        CurrentSettingsBenchmark benchmark = benchmark(3_000_000_000L, 10_000_000_000L, 30, 200);
        long started = 1_000_000_000L;

        assertTrue(benchmark.start(FINGERPRINT, started));
        assertEquals(BenchmarkState.COUNTDOWN, benchmark.state());
        benchmark.advance(started + 2_999_999_999L);
        assertEquals(BenchmarkState.COUNTDOWN, benchmark.state());

        long now = started + 3_000_000_000L;
        benchmark.advance(now);
        assertEquals(BenchmarkState.RUNNING, benchmark.state());
        benchmark.recordFrame(now, 2_000_000L); // Seed frame.
        for (int i = 0; i < 100; i++) {
            now += 100_000_000L;
            benchmark.recordFrame(now, 2_000_000L);
        }

        assertEquals(BenchmarkState.COMPLETED, benchmark.state());
        BenchmarkResult result = benchmark.latestResult().orElseThrow();
        assertEquals(10.0, result.averageFps(), 0.0001);
        assertEquals(10.0, result.onePercentLowFps(), 0.0001);
        assertEquals(100.0, result.averageFrameTimeMs(), 0.0001);
        assertEquals(100.0, result.p95FrameTimeMs(), 0.0001);
        assertEquals(2.0, result.averageCpuRenderTimeMs(), 0.0001);
        assertEquals(100, result.framesOver50Ms());
        assertEquals(100, result.frameCount());
        assertEquals(10.0, result.durationSeconds(), 0.0001);
    }

    @Test
    void calculatesNearestRankP95AndSlowestOnePercent() {
        CurrentSettingsBenchmark benchmark = benchmark(0L, Long.MAX_VALUE, 1, 100);
        long now = 1L;
        assertTrue(benchmark.start(FINGERPRINT, now));
        benchmark.advance(now);
        benchmark.recordFrame(now, 1_000_000L);

        for (int millis = 1; millis <= 100; millis++) {
            now += millis * 1_000_000L;
            benchmark.recordFrame(now, 2_000_000L);
        }

        BenchmarkResult result = benchmark.latestResult().orElseThrow();
        assertEquals(95.0, result.p95FrameTimeMs(), 0.0001);
        assertEquals(10.0, result.onePercentLowFps(), 0.0001);
        assertEquals(50.5, result.averageFrameTimeMs(), 0.0001);
        assertEquals(50, result.framesOver50Ms());
    }

    @Test
    void fewerThanThirtySamplesFailsWithoutPublishingResult() {
        CurrentSettingsBenchmark benchmark = benchmark(0L, 1_000_000_000L, 30, 100);
        long now = 1L;
        benchmark.start(FINGERPRINT, now);
        benchmark.advance(now);
        benchmark.recordFrame(now, 1L);
        for (int i = 0; i < 10; i++) {
            now += 100_000_000L;
            benchmark.recordFrame(now, 1L);
        }

        assertEquals(BenchmarkState.FAILED, benchmark.state());
        assertEquals(BenchmarkCancelReason.TOO_FEW_FRAMES, benchmark.reason());
        assertTrue(benchmark.latestResult().isEmpty());
    }

    @Test
    void cancellationAndFailurePreservePreviousCompletedResult() {
        CurrentSettingsBenchmark benchmark = benchmark(0L, Long.MAX_VALUE, 1, 2);
        long now = 1L;
        benchmark.start(FINGERPRINT, now);
        benchmark.advance(now);
        benchmark.recordFrame(now, 1L);
        benchmark.recordFrame(now + 16_000_000L, 1L);
        benchmark.recordFrame(now + 32_000_000L, 1L);
        BenchmarkResult completed = benchmark.latestResult().orElseThrow();

        assertTrue(benchmark.start(FINGERPRINT, now + 40_000_000L));
        benchmark.cancel(BenchmarkCancelReason.FOCUS_LOST);
        assertEquals(BenchmarkState.CANCELLED, benchmark.state());
        assertSame(completed, benchmark.latestResult().orElseThrow());
    }

    @Test
    void completedResultRetainsRunAndWorldSessionIdentifiers() {
        CurrentSettingsBenchmark benchmark = benchmark(0L, Long.MAX_VALUE, 1, 2);
        long now = 1L;
        assertTrue(benchmark.start(FINGERPRINT, now, 7L, 11L));
        benchmark.advance(now);
        benchmark.recordFrame(now, 1L);
        benchmark.recordFrame(now + 16_000_000L, 1L);
        benchmark.recordFrame(now + 32_000_000L, 1L);

        BenchmarkResult result = benchmark.latestResult().orElseThrow();
        assertEquals(7L, result.runId());
        assertEquals(11L, result.worldSessionId());
    }

    @Test
    void rejectsASecondStartWhileActive() {
        CurrentSettingsBenchmark benchmark = benchmark(3L, 10L, 1, 10);
        assertTrue(benchmark.start(FINGERPRINT, 1L));
        assertFalse(benchmark.start(FINGERPRINT, 2L));
        assertEquals(BenchmarkState.COUNTDOWN, benchmark.state());
    }

    @Test
    void fingerprintDetectsEverySettingsChange() {
        BenchmarkResult result = new BenchmarkResult(FINGERPRINT, 60, 50, 16.6,
                20, 10, 1, 600, 10, Instant.EPOCH);
        assertFalse(result.isStale(FINGERPRINT));
        List<BenchmarkFingerprint> changes = List.of(
                fingerprint("minecraft:the_nether", FINGERPRINT.playContext(), 1920, 1080, 100, 12, 8, "FANCY", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), "multiplayer", 1920, 1080, 100, 12, 8, "FANCY", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1280, 1080, 100, 12, 8, "FANCY", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 720, 100, 12, 8, "FANCY", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 1080, 75, 12, 8, "FANCY", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 1080, 100, 10, 8, "FANCY", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 1080, 100, 12, 6, "FANCY", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 1080, 100, 12, 8, "FAST", true, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 1080, 100, 12, 8, "FANCY", false, 144, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 1080, 100, 12, 8, "FANCY", true, 120, 2),
                fingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(), 1920, 1080, 100, 12, 8, "FANCY", true, 144, 3),
                new BenchmarkFingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(),
                        1920, 1080, 100, 12, 8, "FANCY", true, 144, 2, true, false),
                new BenchmarkFingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(),
                        1920, 1080, 100, 12, 8, "FANCY", true, 144, 2, false, true),
                new BenchmarkFingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(),
                        1920, 1080, 100, 12, 8, "FANCY", true, 144, 2,
                        FINGERPRINT.indirectDraw(), FINGERPRINT.uniqueOpaqueLayer(),
                        !FINGERPRINT.adaptiveChunkUploads(), FINGERPRINT.chunkUploadsPerFrame()),
                new BenchmarkFingerprint(FINGERPRINT.dimensionContext(), FINGERPRINT.playContext(),
                        1920, 1080, 100, 12, 8, "FANCY", true, 144, 2,
                        FINGERPRINT.indirectDraw(), FINGERPRINT.uniqueOpaqueLayer(),
                        FINGERPRINT.adaptiveChunkUploads(), FINGERPRINT.chunkUploadsPerFrame() + 1));
        changes.forEach(changed -> assertTrue(result.isStale(changed), changed.toString()));
    }

    private static CurrentSettingsBenchmark benchmark(long countdown, long sample, int minimum, int capacity) {
        return new CurrentSettingsBenchmark(countdown, sample, minimum, capacity,
                () -> Instant.parse("2026-07-16T12:00:00Z"));
    }

    private static BenchmarkFingerprint fingerprint(String dimension, String playContext, int width, int height,
                                                     int renderScale, int renderDistance, int simulationDistance,
                                                     String graphics, boolean vsync, int fpsCap, int preset) {
        return new BenchmarkFingerprint(dimension, playContext, width, height, renderScale, renderDistance,
                simulationDistance, graphics, vsync, fpsCap, preset);
    }
}
