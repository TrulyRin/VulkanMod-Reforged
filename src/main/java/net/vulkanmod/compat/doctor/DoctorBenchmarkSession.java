package net.vulkanmod.compat.doctor;

import net.vulkanmod.diagnostics.benchmark.BenchmarkFingerprint;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class DoctorBenchmarkSession {
    private static volatile Session session;

    private DoctorBenchmarkSession() {
    }

    public static synchronized boolean arm(
            BenchmarkResult before,
            DoctorSettingsSnapshot expectedSettings,
            Set<DoctorSetting> changedSettings,
            long worldSessionId,
            long minimumAfterRunId,
            BenchmarkFingerprint expectedAfterFingerprint) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(expectedSettings, "expectedSettings");
        Objects.requireNonNull(changedSettings, "changedSettings");
        Objects.requireNonNull(expectedAfterFingerprint, "expectedAfterFingerprint");

        if (worldSessionId <= 0L
                || before.worldSessionId() != worldSessionId
                || before.runId() <= 0L
                || before.runId() > minimumAfterRunId
                || changedSettings.isEmpty()
                || !valid(before)
                || !validTransition(before.fingerprint(), expectedAfterFingerprint, changedSettings)) {
            session = null;
            return false;
        }

        Baseline baseline = new Baseline(before, expectedSettings.values(), changedSettings,
                worldSessionId, minimumAfterRunId, expectedAfterFingerprint);
        session = new Session(baseline, null, null);
        return true;
    }

    public static synchronized void invalidate() {
        Session captured = session;
        if (captured != null) {
            session = new Session(captured.baseline(), null, DoctorBenchmarkStatus.SETTINGS_CHANGED);
        }
    }

    public static synchronized void clear() {
        session = null;
    }

    public static boolean hasBaseline() {
        return session != null;
    }

    public static synchronized DoctorBenchmarkEvaluation evaluate(
            Optional<BenchmarkResult> latest,
            BenchmarkFingerprint currentFingerprint,
            DoctorSettingsSnapshot currentSettings,
            long worldSessionId) {
        Objects.requireNonNull(latest, "latest");
        Objects.requireNonNull(currentFingerprint, "currentFingerprint");
        Objects.requireNonNull(currentSettings, "currentSettings");

        Session captured = session;
        if (captured == null) {
            return DoctorBenchmarkEvaluation.status(DoctorBenchmarkStatus.NONE);
        }
        if (captured.terminalStatus() != null) {
            return DoctorBenchmarkEvaluation.status(captured.terminalStatus());
        }

        Baseline baseline = captured.baseline();
        if (worldSessionId <= 0L || worldSessionId != baseline.worldSessionId()) {
            return terminate(captured, DoctorBenchmarkStatus.CONTEXT_MISMATCH);
        }
        if (!currentSettings.pendingSettings().isEmpty()
                || !currentSettings.matchesExpected(baseline.expectedSettings())
                || !currentFingerprint.equals(baseline.expectedAfterFingerprint())) {
            return terminate(captured, DoctorBenchmarkStatus.SETTINGS_CHANGED);
        }
        if (captured.comparison() != null) {
            return DoctorBenchmarkEvaluation.ready(captured.comparison());
        }

        BenchmarkResult after = latest.orElse(null);
        if (after == null || after.runId() <= baseline.minimumAfterRunId()) {
            return DoctorBenchmarkEvaluation.status(DoctorBenchmarkStatus.AWAITING_AFTER);
        }
        if (after.worldSessionId() != baseline.worldSessionId()) {
            return terminate(captured, DoctorBenchmarkStatus.CONTEXT_MISMATCH);
        }
        if (!after.fingerprint().equals(baseline.expectedAfterFingerprint())) {
            return terminate(captured, DoctorBenchmarkStatus.SETTINGS_CHANGED);
        }
        if (!valid(after)) {
            return terminate(captured, DoctorBenchmarkStatus.INVALID_RESULT);
        }

        DoctorBenchmarkComparison comparison = new DoctorBenchmarkComparison(baseline.before(), after);
        session = new Session(baseline, comparison, null);
        return DoctorBenchmarkEvaluation.ready(comparison);
    }

    private static DoctorBenchmarkEvaluation terminate(Session captured, DoctorBenchmarkStatus status) {
        session = new Session(captured.baseline(), null, status);
        return DoctorBenchmarkEvaluation.status(status);
    }

    private static boolean validTransition(BenchmarkFingerprint before,
                                           BenchmarkFingerprint expected,
                                           Set<DoctorSetting> changes) {
        boolean changesRenderScale = changes.contains(DoctorSetting.RENDER_SCALE);
        boolean changesIndirect = changes.contains(DoctorSetting.INDIRECT_DRAW);
        boolean changesUniqueLayer = changes.contains(DoctorSetting.UNIQUE_OPAQUE_LAYER);
        boolean changesPreset = changesIndirect || changesUniqueLayer;

        return before.dimensionContext().equals(expected.dimensionContext())
                && before.playContext().equals(expected.playContext())
                && before.framebufferWidth() == expected.framebufferWidth()
                && before.framebufferHeight() == expected.framebufferHeight()
                && (changesRenderScale
                    ? expected.renderScale() == 100
                    : before.renderScale() == expected.renderScale())
                && before.renderDistance() == expected.renderDistance()
                && before.simulationDistance() == expected.simulationDistance()
                && before.graphicsMode().equals(expected.graphicsMode())
                && before.vsync() == expected.vsync()
                && before.fpsCap() == expected.fpsCap()
                && (changesPreset
                    ? expected.performancePreset() == 0
                    : before.performancePreset() == expected.performancePreset())
                && (changesIndirect
                    ? !expected.indirectDraw()
                    : before.indirectDraw() == expected.indirectDraw())
                && (changesUniqueLayer
                    ? !expected.uniqueOpaqueLayer()
                    : before.uniqueOpaqueLayer() == expected.uniqueOpaqueLayer())
                && before.adaptiveChunkUploads() == expected.adaptiveChunkUploads()
                && before.chunkUploadsPerFrame() == expected.chunkUploadsPerFrame();
    }

    private static boolean valid(BenchmarkResult result) {
        return result.runId() > 0L
                && result.worldSessionId() > 0L
                && positiveFinite(result.averageFps())
                && positiveFinite(result.onePercentLowFps())
                && positiveFinite(result.averageFrameTimeMs())
                && positiveFinite(result.p95FrameTimeMs())
                && Double.isFinite(result.averageCpuRenderTimeMs())
                && result.averageCpuRenderTimeMs() >= 0.0
                && result.framesOver50Ms() >= 0
                && result.frameCount() >= 30
                && result.framesOver50Ms() <= result.frameCount()
                && positiveFinite(result.durationSeconds());
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0;
    }

    private record Session(
            Baseline baseline,
            DoctorBenchmarkComparison comparison,
            DoctorBenchmarkStatus terminalStatus) {
    }

    private record Baseline(
            BenchmarkResult before,
            Map<DoctorSetting, DoctorValue> expectedSettings,
            Set<DoctorSetting> changedSettings,
            long worldSessionId,
            long minimumAfterRunId,
            BenchmarkFingerprint expectedAfterFingerprint) {
        private Baseline {
            before = Objects.requireNonNull(before, "before");
            EnumMap<DoctorSetting, DoctorValue> copied = new EnumMap<>(DoctorSetting.class);
            copied.putAll(expectedSettings);
            expectedSettings = Map.copyOf(copied);
            changedSettings = Set.copyOf(changedSettings);
            expectedAfterFingerprint = Objects.requireNonNull(
                    expectedAfterFingerprint, "expectedAfterFingerprint");
        }
    }
}
