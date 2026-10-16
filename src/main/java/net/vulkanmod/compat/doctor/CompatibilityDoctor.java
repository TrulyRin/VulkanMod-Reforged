package net.vulkanmod.compat.doctor;

import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.diagnostics.CompatDiagnosticsSnapshot;
import net.vulkanmod.compat.diagnostics.DiagnosticsSeverity;
import net.vulkanmod.compat.diagnostics.ModDiagnostic;
import net.vulkanmod.compat.diagnostics.SessionIssueDiagnostic;
import net.vulkanmod.compat.diagnostics.SessionIssueType;
import net.vulkanmod.compat.diagnostics.VersionStatus;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class CompatibilityDoctor {
    private static final int MAX_SUBJECTS = 8;
    private static final double STUTTER_P95_MS = 33.33;

    private static final Comparator<DoctorRecommendation> ORDER =
            Comparator.comparingInt((DoctorRecommendation recommendation) -> recommendation.severity().ordinal())
                    .reversed()
                    .thenComparing(Comparator.comparingInt(
                            (DoctorRecommendation recommendation) -> recommendation.rule().rank()).reversed())
                    .thenComparing(DoctorRecommendation::id);

    private CompatibilityDoctor() {
    }

    public static DoctorAssessment assess(CompatDiagnosticsSnapshot snapshot,
                                          DoctorSettingsSnapshot settings) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(settings, "settings");

        List<DoctorRecommendation> recommendations = new ArrayList<>();
        List<ModDiagnostic> rendererConflicts = snapshot.mods().stream()
                .filter(mod -> mod.policyMode() == CompatMode.INCOMPATIBLE)
                .toList();
        boolean hasRendererConflict = !rendererConflicts.isEmpty();

        if (hasRendererConflict) {
            recommendations.add(recommendation(
                    "renderer-conflict", DoctorRule.INCOMPATIBLE_RENDERER_MOD,
                    DiagnosticsSeverity.INCOMPATIBLE, DoctorConfidence.HIGH,
                    rendererConflicts.stream().map(CompatibilityDoctor::modSubject).toList(),
                    rendererConflicts.size(), DoctorImpact.none(),
                    List.of(new DoctorAction.Guidance(DoctorGuidance.REMOVE_RENDERER_REPLACEMENT),
                            new DoctorAction.CopyReport())));
        }

        List<SessionIssueDiagnostic> inventoryFailures = issues(snapshot, SessionIssueType.MOD_INVENTORY_UNAVAILABLE);
        if (!inventoryFailures.isEmpty()) {
            recommendations.add(recommendation(
                    "mod-inventory-unavailable", DoctorRule.MOD_INVENTORY_UNAVAILABLE,
                    maxSeverity(inventoryFailures), DoctorConfidence.HIGH, List.of(),
                    occurrences(inventoryFailures), DoctorImpact.none(),
                    List.of(new DoctorAction.Guidance(DoctorGuidance.RETRY_DIAGNOSTICS_CAPTURE),
                            new DoctorAction.CopyReport())));
        }

        List<SessionIssueDiagnostic> runtimeFailures = snapshot.sessionIssues().stream()
                .filter(issue -> issue.type() == SessionIssueType.TRAMPOLINE_FAULT
                        || issue.type() == SessionIssueType.EXTERNAL_FAILURE)
                .toList();
        if (!runtimeFailures.isEmpty()) {
            recommendations.add(recommendation(
                    "runtime-render-failure", DoctorRule.RUNTIME_RENDER_FAILURE,
                    maxSeverity(runtimeFailures), DoctorConfidence.HIGH, List.of(),
                    occurrences(runtimeFailures), DoctorImpact.none(),
                    List.of(new DoctorAction.Guidance(DoctorGuidance.UPDATE_OR_REPORT_MOD),
                            new DoctorAction.CopyReport())));
        }

        List<SessionIssueDiagnostic> entrypointIssues = snapshot.sessionIssues().stream()
                .filter(issue -> issue.type() == SessionIssueType.MISSING_GL_ENTRYPOINT
                        || issue.type() == SessionIssueType.STUBBED_GL_ENTRYPOINT)
                .toList();
        if (!entrypointIssues.isEmpty()) {
            recommendations.add(recommendation(
                    "missing-or-stubbed-entrypoint", DoctorRule.MISSING_OR_STUBBED_GL_ENTRYPOINT,
                    maxSeverity(entrypointIssues), DoctorConfidence.HIGH, List.of(),
                    occurrences(entrypointIssues), DoctorImpact.none(),
                    List.of(new DoctorAction.Guidance(DoctorGuidance.UPDATE_OR_REPORT_MOD),
                            new DoctorAction.CopyReport())));
        }

        List<SessionIssueDiagnostic> contractGaps = issues(snapshot, SessionIssueType.CONTRACT_GAP);
        if (!contractGaps.isEmpty()) {
            recommendations.add(recommendation(
                    "gl-contract-gap", DoctorRule.GL_CONTRACT_GAP,
                    maxSeverity(contractGaps), DoctorConfidence.MEDIUM, List.of(),
                    occurrences(contractGaps), DoctorImpact.none(),
                    List.of(new DoctorAction.Guidance(DoctorGuidance.UPDATE_OR_REPORT_MOD),
                            new DoctorAction.CopyReport())));
        }

        List<ModDiagnostic> versionMismatches = snapshot.mods().stream()
                .filter(mod -> mod.policyMode() != CompatMode.INCOMPATIBLE)
                .filter(mod -> mod.versionStatus() == VersionStatus.VERSION_MISMATCH)
                .toList();
        if (!versionMismatches.isEmpty()) {
            recommendations.add(recommendation(
                    "version-mismatch", DoctorRule.VERSION_MISMATCH,
                    DiagnosticsSeverity.WARNING, DoctorConfidence.HIGH,
                    versionMismatches.stream().map(CompatibilityDoctor::versionSubject).toList(),
                    versionMismatches.size(), DoctorImpact.none(),
                    List.of(new DoctorAction.Guidance(DoctorGuidance.UPDATE_OR_ROLL_BACK_VERSION))));
        }

        List<ModDiagnostic> unknownVersions = snapshot.mods().stream()
                .filter(mod -> mod.policyMode() != CompatMode.INCOMPATIBLE)
                .filter(ModDiagnostic::compatibilityRelevant)
                .filter(mod -> mod.versionStatus() == VersionStatus.UNKNOWN_VERSION)
                .toList();
        if (!unknownVersions.isEmpty()) {
            recommendations.add(recommendation(
                    "unknown-recognized-version", DoctorRule.UNKNOWN_RECOGNIZED_VERSION,
                    DiagnosticsSeverity.WARNING, DoctorConfidence.MEDIUM,
                    unknownVersions.stream().map(CompatibilityDoctor::modSubject).toList(),
                    unknownVersions.size(), DoctorImpact.none(),
                    List.of(new DoctorAction.Guidance(DoctorGuidance.VERIFY_MOD_VERSION))));
        }

        boolean hasGraphicsContractIssue = !contractGaps.isEmpty()
                || !entrypointIssues.isEmpty()
                || runtimeFailures.stream().anyMatch(issue -> issue.type() == SessionIssueType.TRAMPOLINE_FAULT);
        if (hasGraphicsContractIssue && !hasRendererConflict) {
            List<DoctorSettingChange> changes = conservativeChanges(settings);
            if (!changes.isEmpty()) {
                List<DoctorAction> actions = List.of(
                        new DoctorAction.Guidance(DoctorGuidance.TRY_CONSERVATIVE_SETTINGS),
                        new DoctorAction.StageSettings(
                                DoctorActionPolicy.CONSERVATIVE_RENDERING_ACTION, changes));
                recommendations.add(recommendation(
                        "conservative-rendering-troubleshooter",
                        DoctorRule.CONSERVATIVE_RENDERING_TROUBLESHOOTER,
                        DiagnosticsSeverity.NOTICE, DoctorConfidence.LOW, List.of(), 1,
                        conservativeImpact(settings), actions));
            }
        }

        if (!hasRendererConflict) {
            snapshot.latestBenchmark()
                    .filter(result -> !snapshot.benchmarkIsStale(result))
                    .filter(CompatibilityDoctor::showsStutter)
                    .ifPresent(result -> recommendations.add(recommendation(
                            "benchmark-stutter", DoctorRule.BENCHMARK_STUTTER,
                            DiagnosticsSeverity.NOTICE, DoctorConfidence.MEDIUM, List.of(),
                            Math.max(1, result.framesOver50Ms()),
                            new DoctorImpact(ImpactRating.LOW, ImpactRating.NONE, false),
                            List.of(new DoctorAction.Guidance(
                                    DoctorGuidance.RETEST_PERFORMANCE_IN_SAME_SCENE)))));
        }

        recommendations.sort(ORDER);
        return new DoctorAssessment(snapshot.capturedAt(), recommendations);
    }

    private static List<DoctorSettingChange> conservativeChanges(DoctorSettingsSnapshot settings) {
        List<DoctorSettingChange> changes = new ArrayList<>(3);
        addChange(settings, changes, DoctorSetting.INDIRECT_DRAW, DoctorValue.of(false));
        addChange(settings, changes, DoctorSetting.UNIQUE_OPAQUE_LAYER, DoctorValue.of(false));
        addChange(settings, changes, DoctorSetting.RENDER_SCALE, DoctorValue.of(100));
        return List.copyOf(changes);
    }

    private static DoctorImpact conservativeImpact(DoctorSettingsSnapshot settings) {
        int renderScale = settings.value(DoctorSetting.RENDER_SCALE)
                .filter(DoctorValue.IntegerValue.class::isInstance)
                .map(DoctorValue.IntegerValue.class::cast)
                .map(DoctorValue.IntegerValue::value)
                .orElse(100);
        ImpactRating scaleImpact = renderScale <= 50
                ? ImpactRating.HIGH
                : renderScale < 100 ? ImpactRating.MEDIUM : ImpactRating.NONE;
        ImpactRating performance = scaleImpact == ImpactRating.HIGH
                ? ImpactRating.HIGH
                : ImpactRating.MEDIUM;
        return new DoctorImpact(performance, scaleImpact, false);
    }

    private static void addChange(DoctorSettingsSnapshot settings,
                                  List<DoctorSettingChange> changes,
                                  DoctorSetting setting,
                                  DoctorValue target) {
        if (settings.pendingSettings().contains(setting)) {
            return;
        }
        settings.value(setting)
                .filter(current -> current.getClass() == target.getClass())
                .filter(current -> !current.equals(target))
                .ifPresent(current -> changes.add(new DoctorSettingChange(setting, current, target)));
    }

    private static boolean showsStutter(BenchmarkResult result) {
        int stutterThreshold = Math.max(3, (int) Math.ceil(result.frameCount() * 0.01));
        return result.p95FrameTimeMs() >= STUTTER_P95_MS
                || result.framesOver50Ms() >= stutterThreshold;
    }

    private static List<SessionIssueDiagnostic> issues(CompatDiagnosticsSnapshot snapshot,
                                                        SessionIssueType type) {
        return snapshot.sessionIssues().stream()
                .filter(issue -> issue.type() == type)
                .toList();
    }

    private static int occurrences(List<SessionIssueDiagnostic> issues) {
        long total = issues.stream()
                .mapToLong(issue -> issue.truncated() ? issue.omittedCount() : 1L)
                .sum();
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, total));
    }

    private static DiagnosticsSeverity maxSeverity(List<SessionIssueDiagnostic> issues) {
        return issues.stream()
                .map(SessionIssueDiagnostic::severity)
                .reduce(DiagnosticsSeverity.HEALTHY, DiagnosticsSeverity::max);
    }

    private static String modSubject(ModDiagnostic mod) {
        return mod.displayName() + " [" + mod.id() + "] " + mod.version();
    }

    private static String versionSubject(ModDiagnostic mod) {
        String expected = mod.expectedVersionPrefix().isBlank()
                ? "?"
                : mod.expectedVersionPrefix() + ".x";
        return modSubject(mod) + " -> " + expected;
    }

    private static DoctorRecommendation recommendation(
            String id,
            DoctorRule rule,
            DiagnosticsSeverity severity,
            DoctorConfidence confidence,
            Collection<String> subjects,
            int occurrences,
            DoctorImpact impact,
            List<DoctorAction> actions) {
        List<String> sorted = subjects.stream()
                .filter(Objects::nonNull)
                .filter(subject -> !subject.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                .toList();
        int retained = Math.min(MAX_SUBJECTS, sorted.size());
        return new DoctorRecommendation(id, rule, severity, confidence,
                sorted.subList(0, retained), sorted.size() - retained,
                Math.max(1, occurrences), impact, actions);
    }
}
