package net.vulkanmod.compat.doctor;

import net.vulkanmod.compat.CompatCategory;
import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.diagnostics.CompatDiagnosticsSnapshot;
import net.vulkanmod.compat.diagnostics.DiagnosticsSeverity;
import net.vulkanmod.compat.diagnostics.ModDiagnostic;
import net.vulkanmod.compat.diagnostics.RuntimeFlagDiagnostic;
import net.vulkanmod.compat.diagnostics.RuntimeFlagsDiagnostic;
import net.vulkanmod.compat.diagnostics.SessionIssueDiagnostic;
import net.vulkanmod.compat.diagnostics.SessionIssueType;
import net.vulkanmod.compat.diagnostics.SystemDiagnostic;
import net.vulkanmod.compat.diagnostics.VersionStatus;
import net.vulkanmod.diagnostics.benchmark.BenchmarkFingerprint;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatibilityDoctorTest {
    @Test
    void cleanSnapshotHasNoRecommendations() {
        DoctorAssessment assessment = CompatibilityDoctor.assess(
                snapshot(List.of(), List.of(), Optional.empty()), settings(false, false, 100));

        assertTrue(assessment.recommendations().isEmpty());
    }

    @Test
    void incompatibleRendererIsAdviceOnlyAndSuppressesTroubleshootingChanges() {
        ModDiagnostic renderer = new ModDiagnostic(
                "sodium", "Sodium", "1.0", "", CompatCategory.RENDERER_GL,
                CompatMode.INCOMPATIBLE, VersionStatus.RECOGNIZED_UNPROFILED,
                DiagnosticsSeverity.INCOMPATIBLE, true);
        SessionIssueDiagnostic gap = new SessionIssueDiagnostic(
                SessionIssueType.CONTRACT_GAP, "draw", DiagnosticsSeverity.WARNING);

        DoctorAssessment assessment = CompatibilityDoctor.assess(
                snapshot(List.of(renderer), List.of(gap), Optional.empty()), settings(true, true, 75));

        assertEquals(DoctorRule.INCOMPATIBLE_RENDERER_MOD,
                assessment.recommendations().getFirst().rule());
        assertTrue(assessment.recommendations().stream()
                .noneMatch(recommendation -> recommendation.stagedAction().isPresent()));
    }

    @Test
    void graphicsContractIssueOffersOnlyTheThreeAllowlistedDraftChanges() {
        List<SessionIssueDiagnostic> issues = List.of(
                new SessionIssueDiagnostic(SessionIssueType.CONTRACT_GAP, "draw", DiagnosticsSeverity.WARNING),
                new SessionIssueDiagnostic(SessionIssueType.MISSING_GL_ENTRYPOINT, "glThing", DiagnosticsSeverity.WARNING),
                new SessionIssueDiagnostic(SessionIssueType.TRAMPOLINE_FAULT, "glOther", DiagnosticsSeverity.WARNING));

        DoctorAssessment assessment = CompatibilityDoctor.assess(
                snapshot(List.of(), issues, Optional.empty()), settings(true, true, 75));
        DoctorAction.StageSettings action = assessment.recommendations().stream()
                .map(DoctorRecommendation::stagedAction)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow();

        assertEquals(List.of(
                        DoctorSetting.INDIRECT_DRAW,
                        DoctorSetting.UNIQUE_OPAQUE_LAYER,
                        DoctorSetting.RENDER_SCALE),
                action.changes().stream().map(DoctorSettingChange::setting).toList());
        assertTrue(DoctorActionPolicy.isAllowed(action));
        assertTrue(assessment.recommendations().stream()
                .filter(recommendation -> recommendation.rule() != DoctorRule.CONSERVATIVE_RENDERING_TROUBLESHOOTER)
                .noneMatch(recommendation -> recommendation.stagedAction().isPresent()));
    }

    @Test
    void appliedOrPendingTargetsAreNotOverwritten() {
        EnumMap<DoctorSetting, DoctorValue> values = values(true, false, 100);
        DoctorSettingsSnapshot settings = new DoctorSettingsSnapshot(
                values, Set.of(DoctorSetting.INDIRECT_DRAW));
        DoctorAssessment assessment = CompatibilityDoctor.assess(
                snapshot(List.of(), List.of(new SessionIssueDiagnostic(
                        SessionIssueType.CONTRACT_GAP, "draw", DiagnosticsSeverity.WARNING)), Optional.empty()),
                settings);

        DoctorAction.StageSettings action = assessment.recommendations().stream()
                .map(DoctorRecommendation::stagedAction)
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(null);
        assertEquals(null, action);
        assertFalse(assessment.recommendations().stream()
                .anyMatch(recommendation -> recommendation.rule()
                        == DoctorRule.CONSERVATIVE_RENDERING_TROUBLESHOOTER));
    }

    @Test
    void recommendationOrderingAndSubjectsIgnoreInputOrder() {
        ModDiagnostic alpha = versionMismatch("alpha", "Alpha", "2.0", "1.0");
        ModDiagnostic beta = versionMismatch("beta", "Beta", "3.0", "2.0");
        SessionIssueDiagnostic inventory = new SessionIssueDiagnostic(
                SessionIssueType.MOD_INVENTORY_UNAVAILABLE, "inventory", DiagnosticsSeverity.WARNING);

        DoctorAssessment first = CompatibilityDoctor.assess(
                snapshot(List.of(beta, alpha), List.of(inventory), Optional.empty()), settings(false, false, 100));
        DoctorAssessment second = CompatibilityDoctor.assess(
                snapshot(List.of(alpha, beta), List.of(inventory), Optional.empty()), settings(false, false, 100));

        assertEquals(first, second);
        assertEquals(DoctorRule.MOD_INVENTORY_UNAVAILABLE, first.recommendations().getFirst().rule());
        DoctorRecommendation versions = first.recommendations().stream()
                .filter(recommendation -> recommendation.rule() == DoctorRule.VERSION_MISMATCH)
                .findFirst().orElseThrow();
        assertTrue(versions.subjects().get(0).startsWith("Alpha"));
        assertThrows(UnsupportedOperationException.class,
                () -> first.recommendations().add(versions));
    }

    @Test
    void groupedRecommendationSubjectsAreBounded() {
        List<ModDiagnostic> mods = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            mods.add(versionMismatch("mod" + i, "Mod " + i, "9.0", "1.0"));
        }

        DoctorRecommendation versions = CompatibilityDoctor.assess(
                        snapshot(mods, List.of(), Optional.empty()), settings(false, false, 100))
                .recommendations().stream()
                .filter(recommendation -> recommendation.rule() == DoctorRule.VERSION_MISMATCH)
                .findFirst().orElseThrow();

        assertEquals(8, versions.subjects().size());
        assertEquals(4, versions.omittedSubjects());
        assertEquals(12, versions.occurrences());
    }

    @Test
    void truncatedIssueCountsDoNotCountTheSyntheticMarker() {
        List<SessionIssueDiagnostic> issues = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            issues.add(new SessionIssueDiagnostic(
                    SessionIssueType.CONTRACT_GAP, "gap" + i, DiagnosticsSeverity.WARNING));
        }
        issues.add(new SessionIssueDiagnostic(
                SessionIssueType.CONTRACT_GAP, "", DiagnosticsSeverity.WARNING, 156));

        DoctorRecommendation contract = CompatibilityDoctor.assess(
                        snapshot(List.of(), issues, Optional.empty()), settings(false, false, 100))
                .recommendations().stream()
                .filter(recommendation -> recommendation.rule() == DoctorRule.GL_CONTRACT_GAP)
                .findFirst().orElseThrow();

        assertEquals(256, contract.occurrences());
    }

    @Test
    void staleBenchmarkIsIgnoredAndFreshStutterIsAdviceOnly() {
        BenchmarkResult result = benchmark(fingerprint(75), Instant.parse("2026-07-16T12:00:10Z"), 45.0, 4);
        CompatDiagnosticsSnapshot stale = snapshot(List.of(), List.of(), Optional.of(result));
        assertFalse(CompatibilityDoctor.assess(stale, settings(false, false, 100)).recommendations().stream()
                .anyMatch(recommendation -> recommendation.rule() == DoctorRule.BENCHMARK_STUTTER));

        BenchmarkResult freshResult = benchmark(fingerprint(100), Instant.parse("2026-07-16T12:00:10Z"), 45.0, 4);
        CompatDiagnosticsSnapshot fresh = snapshot(List.of(), List.of(), Optional.of(freshResult));
        DoctorRecommendation recommendation = CompatibilityDoctor.assess(fresh, settings(false, false, 100))
                .recommendations().stream()
                .filter(value -> value.rule() == DoctorRule.BENCHMARK_STUTTER)
                .findFirst().orElseThrow();
        assertTrue(recommendation.stagedAction().isEmpty());
    }

    @Test
    void matchingFingerprintFromAnotherWorldIsStaleAndCannotDriveAdvice() {
        BenchmarkResult oldWorld = new BenchmarkResult(
                4L, 17L, fingerprint(100), 60, 40, 16.6, 45, 8,
                4, 600, 10, Instant.parse("2026-07-16T12:00:10Z"));
        RuntimeFlagDiagnostic off = new RuntimeFlagDiagnostic(false, false, true, true);
        RuntimeFlagsDiagnostic flags = new RuntimeFlagsDiagnostic(off, off, off, off);
        SystemDiagnostic system = new SystemDiagnostic("0.2", "1.21.1", "21.1", "GPU", "Vendor",
                "Driver", "1.3", "CPU", "21", "Windows", "amd64", "3.2", 100);
        CompatDiagnosticsSnapshot currentWorld = new CompatDiagnosticsSnapshot(
                Instant.EPOCH, DiagnosticsSeverity.WARNING, system, flags, fingerprint(100), 18L,
                List.of(), List.of(), List.of(), Optional.of(oldWorld));

        assertTrue(currentWorld.benchmarkIsStale(oldWorld));
        assertFalse(CompatibilityDoctor.assess(currentWorld, settings(false, false, 100))
                .recommendations().stream()
                .anyMatch(recommendation -> recommendation.rule() == DoctorRule.BENCHMARK_STUTTER));
    }

    private static CompatDiagnosticsSnapshot snapshot(List<ModDiagnostic> mods,
                                                        List<SessionIssueDiagnostic> issues,
                                                        Optional<BenchmarkResult> result) {
        RuntimeFlagDiagnostic off = new RuntimeFlagDiagnostic(false, false, true, true);
        RuntimeFlagsDiagnostic flags = new RuntimeFlagsDiagnostic(off, off, off, off);
        SystemDiagnostic system = new SystemDiagnostic("0.2", "1.21.1", "21.1", "GPU", "Vendor",
                "Driver", "1.3", "CPU", "21", "Windows", "amd64", "3.2", 100);
        return new CompatDiagnosticsSnapshot(Instant.EPOCH, DiagnosticsSeverity.WARNING, system, flags,
                fingerprint(100), new ArrayList<>(mods), List.of(), new ArrayList<>(issues), result);
    }

    private static DoctorSettingsSnapshot settings(boolean indirect, boolean uniqueLayer, int renderScale) {
        return new DoctorSettingsSnapshot(values(indirect, uniqueLayer, renderScale), Set.of());
    }

    private static EnumMap<DoctorSetting, DoctorValue> values(boolean indirect, boolean uniqueLayer,
                                                               int renderScale) {
        EnumMap<DoctorSetting, DoctorValue> values = new EnumMap<>(DoctorSetting.class);
        values.put(DoctorSetting.INDIRECT_DRAW, DoctorValue.of(indirect));
        values.put(DoctorSetting.UNIQUE_OPAQUE_LAYER, DoctorValue.of(uniqueLayer));
        values.put(DoctorSetting.RENDER_SCALE, DoctorValue.of(renderScale));
        return values;
    }

    private static ModDiagnostic versionMismatch(String id, String name, String version, String expected) {
        return new ModDiagnostic(id, name, version, expected, CompatCategory.GAMEPLAY_UI,
                CompatMode.SAFE, VersionStatus.VERSION_MISMATCH, DiagnosticsSeverity.WARNING, true);
    }

    private static BenchmarkFingerprint fingerprint(int renderScale) {
        return new BenchmarkFingerprint("minecraft:overworld", "singleplayer", 1920, 1080,
                renderScale, 12, 8, "FANCY", true, 144, 2);
    }

    private static BenchmarkResult benchmark(BenchmarkFingerprint fingerprint, Instant completedAt,
                                              double p95, int framesOver50) {
        return new BenchmarkResult(fingerprint, 60, 40, 16.6, p95, 8,
                framesOver50, 600, 10, completedAt);
    }
}
