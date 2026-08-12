package net.vulkanmod.compat.doctor;

import net.vulkanmod.diagnostics.benchmark.BenchmarkFingerprint;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DoctorBenchmarkSessionTest {
    private static final long WORLD_ID = 42L;
    private static final BenchmarkFingerprint BEFORE_FINGERPRINT = fingerprint(75, 2, true, true);
    private static final BenchmarkFingerprint AFTER_FINGERPRINT = fingerprint(100, 0, false, false);
    private static final Set<DoctorSetting> CHANGES = Set.of(
            DoctorSetting.INDIRECT_DRAW,
            DoctorSetting.UNIQUE_OPAQUE_LAYER,
            DoctorSetting.RENDER_SCALE);

    @AfterEach
    void clearSession() {
        DoctorBenchmarkSession.clear();
    }

    @Test
    void retainsBaselineAndPinsTheFirstComparableAfterResult() {
        BenchmarkResult before = result(1, WORLD_ID, BEFORE_FINGERPRINT,
                Instant.parse("2026-07-16T12:00:00Z"), 50, 35, 30, 8);
        DoctorSettingsSnapshot expected = settings(false, false, 100);
        arm(before, expected, 1);

        assertEquals(DoctorBenchmarkStatus.AWAITING_AFTER,
                evaluate(before, AFTER_FINGERPRINT, expected, WORLD_ID).status());

        BenchmarkResult after = result(2, WORLD_ID, AFTER_FINGERPRINT,
                Instant.parse("2026-07-16T12:01:00Z"), 70, 52, 20, 2);
        DoctorBenchmarkEvaluation ready = evaluate(after, AFTER_FINGERPRINT, expected, WORLD_ID);
        DoctorBenchmarkComparison comparison = ready.comparison().orElseThrow();
        assertEquals(DoctorBenchmarkStatus.READY, ready.status());
        assertEquals(20.0, comparison.averageFpsDelta(), 0.0001);
        assertEquals(17.0, comparison.onePercentLowDelta(), 0.0001);
        assertEquals(-10.0, comparison.p95FrameTimeDelta(), 0.0001);
        assertEquals(-6, comparison.framesOver50MsDelta());

        BenchmarkResult later = result(3, WORLD_ID, AFTER_FINGERPRINT,
                Instant.parse("2026-07-16T12:02:00Z"), 10, 5, 90, 30);
        DoctorBenchmarkComparison pinned = evaluate(later, AFTER_FINGERPRINT, expected, WORLD_ID)
                .comparison().orElseThrow();
        assertEquals(after, pinned.after());
    }

    @Test
    void resultIssuedBeforeApplyCannotBecomeTheAfterRun() {
        BenchmarkResult before = result(1, WORLD_ID, BEFORE_FINGERPRINT,
                Instant.EPOCH, 50, 35, 30, 8);
        DoctorSettingsSnapshot expected = settings(false, false, 100);
        arm(before, expected, 3);
        BenchmarkResult preApply = result(2, WORLD_ID, AFTER_FINGERPRINT,
                Instant.ofEpochSecond(20), 70, 50, 20, 2);

        assertEquals(DoctorBenchmarkStatus.AWAITING_AFTER,
                evaluate(preApply, AFTER_FINGERPRINT, expected, WORLD_ID).status());
    }

    @Test
    void baselineCannotBeStampedOntoAnotherWorld() {
        BenchmarkResult worldA = result(1, 7L, BEFORE_FINGERPRINT,
                Instant.EPOCH, 50, 35, 30, 8);
        DoctorBenchmarkSession.arm(worldA, settings(false, false, 100), CHANGES,
                WORLD_ID, 1L, AFTER_FINGERPRINT);

        assertFalse(DoctorBenchmarkSession.hasBaseline());
        assertEquals(DoctorBenchmarkStatus.NONE,
                evaluate(worldA, AFTER_FINGERPRINT, settings(false, false, 100), WORLD_ID).status());
    }

    @Test
    void contextAndSettingsInvalidationArePermanent() {
        BenchmarkResult before = result(1, WORLD_ID, BEFORE_FINGERPRINT,
                Instant.EPOCH, 50, 35, 30, 8);
        DoctorSettingsSnapshot expected = settings(false, false, 100);
        arm(before, expected, 1);
        BenchmarkResult after = result(2, WORLD_ID, AFTER_FINGERPRINT,
                Instant.ofEpochSecond(20), 60, 45, 25, 3);

        assertEquals(DoctorBenchmarkStatus.CONTEXT_MISMATCH,
                evaluate(after, AFTER_FINGERPRINT, expected, 99L).status());
        assertEquals(DoctorBenchmarkStatus.CONTEXT_MISMATCH,
                evaluate(after, AFTER_FINGERPRINT, expected, WORLD_ID).status());

        DoctorBenchmarkSession.clear();
        arm(before, expected, 1);
        assertEquals(DoctorBenchmarkStatus.SETTINGS_CHANGED,
                evaluate(after, AFTER_FINGERPRINT, settings(true, false, 100), WORLD_ID).status());
        assertEquals(DoctorBenchmarkStatus.SETTINGS_CHANGED,
                evaluate(after, AFTER_FINGERPRINT, expected, WORLD_ID).status());
    }

    @Test
    void unrelatedApplyInvalidatesEvenAReadyComparison() {
        BenchmarkResult before = result(1, WORLD_ID, BEFORE_FINGERPRINT,
                Instant.EPOCH, 50, 35, 30, 8);
        DoctorSettingsSnapshot expected = settings(false, false, 100);
        arm(before, expected, 1);
        BenchmarkResult after = result(2, WORLD_ID, AFTER_FINGERPRINT,
                Instant.ofEpochSecond(20), 60, 45, 25, 3);
        assertEquals(DoctorBenchmarkStatus.READY,
                evaluate(after, AFTER_FINGERPRINT, expected, WORLD_ID).status());

        DoctorBenchmarkSession.invalidate();
        assertEquals(DoctorBenchmarkStatus.SETTINGS_CHANGED,
                evaluate(after, AFTER_FINGERPRINT, expected, WORLD_ID).status());
    }

    @Test
    void rejectsTransitionsThatChangeChunkUploadSettings() {
        BenchmarkResult before = result(1, WORLD_ID, BEFORE_FINGERPRINT,
                Instant.EPOCH, 50, 35, 30, 8);
        DoctorSettingsSnapshot expected = settings(false, false, 100);
        List<BenchmarkFingerprint> changedUploads = List.of(
                fingerprint(100, 0, false, false,
                        !AFTER_FINGERPRINT.adaptiveChunkUploads(), AFTER_FINGERPRINT.chunkUploadsPerFrame()),
                fingerprint(100, 0, false, false,
                        AFTER_FINGERPRINT.adaptiveChunkUploads(), AFTER_FINGERPRINT.chunkUploadsPerFrame() + 1));

        for (BenchmarkFingerprint changed : changedUploads) {
            DoctorBenchmarkSession.clear();
            assertFalse(DoctorBenchmarkSession.arm(before, expected, CHANGES,
                    WORLD_ID, 1L, changed));
        }
    }

    @Test
    void validatesEveryResultField() {
        BenchmarkResult before = result(1, WORLD_ID, BEFORE_FINGERPRINT,
                Instant.EPOCH, 50, 35, 30, 8);
        DoctorSettingsSnapshot expected = settings(false, false, 100);
        List<BenchmarkResult> invalid = List.of(
                custom(2, Double.NaN, 40, 16, 20, 8, 0, 600, 10),
                custom(2, 60, -1, 16, 20, 8, 0, 600, 10),
                custom(2, 60, 40, -1, 20, 8, 0, 600, 10),
                custom(2, 60, 40, 16, Double.POSITIVE_INFINITY, 8, 0, 600, 10),
                custom(2, 60, 40, 16, 20, -1, 0, 600, 10),
                custom(2, 60, 40, 16, 20, 8, -1, 600, 10),
                custom(2, 60, 40, 16, 20, 8, 601, 600, 10),
                custom(2, 60, 40, 16, 20, 8, 0, 29, 10),
                custom(2, 60, 40, 16, 20, 8, 0, 600, Double.POSITIVE_INFINITY));

        for (BenchmarkResult result : invalid) {
            DoctorBenchmarkSession.clear();
            arm(before, expected, 1);
            assertEquals(DoctorBenchmarkStatus.INVALID_RESULT,
                    evaluate(result, AFTER_FINGERPRINT, expected, WORLD_ID).status(), result.toString());
        }
    }

    private static void arm(BenchmarkResult before, DoctorSettingsSnapshot expected, long minimumRunId) {
        DoctorBenchmarkSession.arm(before, expected, CHANGES, WORLD_ID, minimumRunId, AFTER_FINGERPRINT);
    }

    private static DoctorBenchmarkEvaluation evaluate(BenchmarkResult latest,
                                                       BenchmarkFingerprint current,
                                                       DoctorSettingsSnapshot settings,
                                                       long worldId) {
        return DoctorBenchmarkSession.evaluate(Optional.ofNullable(latest), current, settings, worldId);
    }

    private static DoctorSettingsSnapshot settings(boolean indirect, boolean unique, int scale) {
        EnumMap<DoctorSetting, DoctorValue> values = new EnumMap<>(DoctorSetting.class);
        values.put(DoctorSetting.INDIRECT_DRAW, DoctorValue.of(indirect));
        values.put(DoctorSetting.UNIQUE_OPAQUE_LAYER, DoctorValue.of(unique));
        values.put(DoctorSetting.RENDER_SCALE, DoctorValue.of(scale));
        return new DoctorSettingsSnapshot(values, Set.of());
    }

    private static BenchmarkFingerprint fingerprint(int scale, int preset, boolean indirect, boolean unique) {
        return new BenchmarkFingerprint("minecraft:overworld", "singleplayer", 1920, 1080,
                scale, 12, 8, "FANCY", true, 144, preset, indirect, unique);
    }

    private static BenchmarkFingerprint fingerprint(int scale, int preset, boolean indirect, boolean unique,
                                                    boolean adaptiveUploads, int chunkUploadsPerFrame) {
        return new BenchmarkFingerprint("minecraft:overworld", "singleplayer", 1920, 1080,
                scale, 12, 8, "FANCY", true, 144, preset, indirect, unique,
                adaptiveUploads, chunkUploadsPerFrame);
    }

    private static BenchmarkResult result(long runId, long worldId, BenchmarkFingerprint fingerprint,
                                          Instant time, double fps, double low, double p95, int stutters) {
        return new BenchmarkResult(runId, worldId, fingerprint, fps, low, 16.6, p95, 8,
                stutters, 600, 10, time);
    }

    private static BenchmarkResult custom(long runId, double fps, double low, double averageFrame,
                                          double p95, double cpu, int stutters, int frames, double duration) {
        return new BenchmarkResult(runId, WORLD_ID, AFTER_FINGERPRINT, fps, low, averageFrame,
                p95, cpu, stutters, frames, duration, Instant.ofEpochSecond(20));
    }
}
