package net.vulkanmod.config.gui.page;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.compat.CompatReport;
import net.vulkanmod.compat.doctor.CompatibilityDoctor;
import net.vulkanmod.compat.doctor.DoctorAction;
import net.vulkanmod.compat.doctor.DoctorAssessment;
import net.vulkanmod.compat.doctor.DoctorBenchmarkComparison;
import net.vulkanmod.compat.doctor.DoctorBenchmarkEvaluation;
import net.vulkanmod.compat.doctor.DoctorBenchmarkSession;
import net.vulkanmod.compat.doctor.DoctorBenchmarkStatus;
import net.vulkanmod.compat.doctor.DoctorRecommendation;
import net.vulkanmod.compat.doctor.DoctorSetting;
import net.vulkanmod.compat.doctor.DoctorSettingChange;
import net.vulkanmod.compat.doctor.DoctorSettingsSnapshot;
import net.vulkanmod.compat.doctor.DoctorValue;
import net.vulkanmod.compat.diagnostics.CompatDiagnosticsService;
import net.vulkanmod.compat.diagnostics.CompatDiagnosticsSnapshot;
import net.vulkanmod.compat.diagnostics.DiagnosticsSeverity;
import net.vulkanmod.compat.diagnostics.ModDiagnostic;
import net.vulkanmod.compat.diagnostics.RenderPathDiagnostic;
import net.vulkanmod.compat.diagnostics.RuntimeFlagDiagnostic;
import net.vulkanmod.compat.diagnostics.SessionIssueDiagnostic;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;
import net.vulkanmod.diagnostics.benchmark.MinecraftBenchmarkController;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class DiagnosticsPageContent implements SettingsPageContent {
    public static final String PAGE_ID = "vulkanmod:diagnostics";

    private final Minecraft minecraft;
    private final DoctorSettingsController doctorSettings;
    private DiagnosticsList list;
    private CompatDiagnosticsSnapshot snapshot;
    private DoctorAssessment doctorAssessment;
    private DoctorAction.StageSettings previewedDoctorAction;
    private PendingDoctorPlan pendingDoctorPlan;
    private boolean doctorPlanConfirmedForApply;
    private String reportText = "";
    private Component feedback = Component.empty();
    private Component doctorFeedback = Component.empty();
    private Button benchmarkButton;
    private Availability availability;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean narrow;

    public DiagnosticsPageContent(DoctorSettingsController doctorSettings) {
        this.minecraft = Minecraft.getInstance();
        this.doctorSettings = java.util.Objects.requireNonNull(doctorSettings, "doctorSettings");
    }

    @Override
    public String id() {
        return PAGE_ID;
    }

    @Override
    public Component title() {
        return Component.translatable("vulkanmod.diagnostics.title");
    }

    @Override
    public void initialize(int x, int y, int width, int height, int itemHeight) {
        this.x = x;
        this.y = y;
        this.width = Math.max(120, width);
        this.height = Math.max(40, height);
        this.narrow = width < 520;
        this.list = new DiagnosticsList(minecraft, this.width, this.height, this.y, narrow ? 36 : 24);
        this.list.setX(this.x);
        rebuildRows();
    }

    @Override
    public SettingsPageListener listener() {
        return list;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        list.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onSelected() {
        capture();
    }

    @Override
    public void updateAvailability() {
        if (benchmarkButton == null) {
            return;
        }
        boolean hasWorld = minecraft.level != null && minecraft.player != null;
        boolean pending = doctorSettings.hasPendingSettings();
        boolean active = MinecraftBenchmarkController.isActive();
        Availability next = !hasWorld ? Availability.NO_WORLD
                : pending ? Availability.PENDING_SETTINGS
                : active ? Availability.ALREADY_RUNNING
                : Availability.AVAILABLE;
        benchmarkButton.active = next == Availability.AVAILABLE;
        if (next == availability) {
            return;
        }
        availability = next;
        if (next == Availability.NO_WORLD) {
            benchmarkButton.setTooltip(Tooltip.create(Component.translatable(
                    "vulkanmod.diagnostics.benchmark.unavailable.world")));
        } else if (next == Availability.PENDING_SETTINGS) {
            benchmarkButton.setTooltip(Tooltip.create(Component.translatable(
                    "vulkanmod.diagnostics.benchmark.unavailable.pending")));
        } else if (next == Availability.ALREADY_RUNNING) {
            benchmarkButton.setTooltip(Tooltip.create(Component.translatable(
                    "vulkanmod.diagnostics.benchmark.unavailable.running")));
        } else {
            benchmarkButton.setTooltip(Tooltip.create(Component.translatable(
                    "vulkanmod.diagnostics.benchmark.tooltip")));
        }
    }

    public CompatDiagnosticsSnapshot snapshot() {
        return snapshot;
    }

    public String reportText() {
        return reportText;
    }

    private void capture() {
        snapshot = CompatDiagnosticsService.capture();
        boolean invalidatedDraft = validatePendingDoctorPlan();
        doctorAssessment = CompatibilityDoctor.assess(snapshot, doctorSettings.settingsSnapshot());
        reportText = CompatReport.format(snapshot);
        feedback = Component.empty();
        previewedDoctorAction = null;
        if (pendingDoctorPlan == null && !invalidatedDraft) {
            doctorFeedback = Component.empty();
        }
        rebuildRows();
    }

    private void copyReport() {
        if (snapshot == null) {
            capture();
        }
        minecraft.keyboardHandler.setClipboard(reportText);
        feedback = Component.translatable("vulkanmod.diagnostics.action.copy.success");
        rebuildRows();
    }

    private void startBenchmark() {
        if (doctorSettings.hasPendingSettings()) {
            feedback = Component.translatable("vulkanmod.diagnostics.benchmark.unavailable.pending");
            rebuildRows();
            return;
        }

        MinecraftBenchmarkController.StartResult result = MinecraftBenchmarkController.start(minecraft);
        if (result == MinecraftBenchmarkController.StartResult.UNAVAILABLE) {
            feedback = Component.translatable("vulkanmod.diagnostics.benchmark.unavailable.world");
            rebuildRows();
        } else if (result == MinecraftBenchmarkController.StartResult.ALREADY_RUNNING) {
            feedback = Component.translatable("vulkanmod.diagnostics.benchmark.unavailable.running");
            rebuildRows();
        }
    }

    public void onSettingsReset() {
        boolean discardedDoctorPlan = pendingDoctorPlan != null;
        previewedDoctorAction = null;
        pendingDoctorPlan = null;
        doctorPlanConfirmedForApply = false;
        doctorFeedback = discardedDoctorPlan
                ? Component.translatable("vulkanmod.diagnostics.doctor.stage.reset")
                : Component.empty();
        reassessDoctor();
        rebuildRows();
    }

    public void onSettingsApplied() {
        previewedDoctorAction = null;
        if (pendingDoctorPlan != null) {
            boolean comparisonArmed = false;
            boolean appliedAsPreviewed = doctorPlanConfirmedForApply
                    && doctorSettings.settingsSnapshot().matchesExpected(
                    pendingDoctorPlan.expectedSettings().values());
            if (appliedAsPreviewed && pendingDoctorPlan.baseline().isPresent()) {
                comparisonArmed = DoctorBenchmarkSession.arm(
                        pendingDoctorPlan.baseline().orElseThrow(),
                        pendingDoctorPlan.expectedSettings(),
                        pendingDoctorPlan.changedSettings(),
                        pendingDoctorPlan.contextToken(),
                        MinecraftBenchmarkController.latestIssuedRunId(),
                        MinecraftBenchmarkController.captureFingerprint(minecraft));
            } else if (!appliedAsPreviewed || pendingDoctorPlan.baseline().isEmpty()) {
                DoctorBenchmarkSession.clear();
            }
            doctorFeedback = Component.translatable(!appliedAsPreviewed
                    ? "vulkanmod.diagnostics.doctor.stage.applied_modified"
                    : comparisonArmed
                    ? "vulkanmod.diagnostics.doctor.stage.applied"
                    : pendingDoctorPlan.baseline().isPresent()
                    ? "vulkanmod.diagnostics.doctor.stage.applied_baseline_invalid"
                    : "vulkanmod.diagnostics.doctor.stage.applied_no_baseline");
            pendingDoctorPlan = null;
            doctorPlanConfirmedForApply = false;
        } else {
            DoctorBenchmarkSession.invalidate();
        }
        reassessDoctor();
        rebuildRows();
    }

    private void reassessDoctor() {
        if (snapshot != null) {
            doctorAssessment = CompatibilityDoctor.assess(snapshot, doctorSettings.settingsSnapshot());
        }
    }

    public void beforeSettingsApplied() {
        doctorPlanConfirmedForApply = pendingDoctorPlan != null && pendingDoctorPlanMatchesDraft();
    }

    public void onDoctorDraftInvalidated() {
        if (pendingDoctorPlan == null) {
            return;
        }
        previewedDoctorAction = null;
        pendingDoctorPlan = null;
        doctorPlanConfirmedForApply = false;
        doctorFeedback = Component.translatable(
                "vulkanmod.diagnostics.doctor.stage.draft_modified");
        reassessDoctor();
        rebuildRows();
    }

    public void onOptionDraftsRestored() {
        validatePendingDoctorPlan();
        reassessDoctor();
        rebuildRows();
    }

    private boolean validatePendingDoctorPlan() {
        if (pendingDoctorPlan != null && !pendingDoctorPlanMatchesDraft()) {
            onDoctorDraftInvalidated();
            return true;
        }
        return false;
    }

    private boolean pendingDoctorPlanMatchesDraft() {
        return pendingDoctorPlan != null
                && doctorSettings.pendingSettingsCount() == pendingDoctorPlan.changedSettings().size()
                && doctorSettings.settingsSnapshot().matchesExpected(
                pendingDoctorPlan.expectedSettings().values());
    }

    private void rebuildRows() {
        if (list == null) {
            return;
        }

        List<DiagnosticsRow> rows = new ArrayList<>();
        if (snapshot == null) {
            rows.add(textRow(Component.translatable("vulkanmod.diagnostics.empty"), Component.empty(),
                    DiagnosticsSeverity.NOTICE, false));
            list.setRows(rows);
            return;
        }

        rows.add(section("vulkanmod.diagnostics.section.overall"));
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.overall.status"),
                enumValue("severity", snapshot.severity()), snapshot.severity(), false));
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.overall.counts"),
                Component.translatable("vulkanmod.diagnostics.overall.counts.value",
                        snapshot.mods().size(),
                        snapshot.count(DiagnosticsSeverity.WARNING),
                        snapshot.count(DiagnosticsSeverity.INCOMPATIBLE)),
                snapshot.severity(), false));
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.overall.captured"),
                Component.literal(snapshot.capturedAt().toString()), DiagnosticsSeverity.HEALTHY, false));

        Button refresh = button("vulkanmod.diagnostics.action.refresh", ignored -> capture());
        Button copy = button("vulkanmod.diagnostics.action.copy", ignored -> copyReport());
        rows.add(actionRow(refresh, copy));
        if (!feedback.getString().isEmpty()) {
            rows.add(textRow(Component.translatable("vulkanmod.diagnostics.action.result"), feedback,
                    DiagnosticsSeverity.HEALTHY, false));
        }

        addDoctorRows(rows);

        rows.add(section("vulkanmod.diagnostics.section.benchmark"));
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.benchmark.state"),
                enumValue("benchmark.state", MinecraftBenchmarkController.state(minecraft)),
                DiagnosticsSeverity.HEALTHY, false));
        benchmarkButton = button(snapshot.latestBenchmark().isPresent()
                        ? "vulkanmod.diagnostics.benchmark.run_again"
                        : "vulkanmod.diagnostics.benchmark.run",
                ignored -> startBenchmark());
        availability = null;
        rows.add(actionRow(benchmarkButton));
        updateAvailability();
        snapshot.latestBenchmark().ifPresent(result -> addBenchmarkRows(rows, result));

        rows.add(section("vulkanmod.diagnostics.section.system"));
        var system = snapshot.system();
        rows.add(valueRow("vulkanmod.diagnostics.system.vulkanmod", system.vulkanModVersion()));
        rows.add(valueRow("vulkanmod.diagnostics.system.minecraft", system.minecraftVersion()));
        rows.add(valueRow("vulkanmod.diagnostics.system.neoforge", system.neoForgeVersion()));
        rows.add(valueRow("vulkanmod.diagnostics.system.gpu", system.gpu()));
        rows.add(valueRow("vulkanmod.diagnostics.system.vendor", system.gpuVendor()));
        rows.add(valueRow("vulkanmod.diagnostics.system.driver", system.driverVersion()));
        rows.add(valueRow("vulkanmod.diagnostics.system.vulkan", system.vulkanVersion()));
        rows.add(valueRow("vulkanmod.diagnostics.system.cpu", system.cpu()));
        rows.add(valueRow("vulkanmod.diagnostics.system.java", system.javaVersion()));
        rows.add(valueRow("vulkanmod.diagnostics.system.os",
                system.operatingSystem() + " / " + system.architecture()));

        rows.add(section("vulkanmod.diagnostics.section.mods"));
        List<ModDiagnostic> relevantMods = snapshot.mods().stream()
                .filter(ModDiagnostic::compatibilityRelevant)
                .toList();
        if (relevantMods.isEmpty()) {
            rows.add(textRow(Component.translatable("vulkanmod.diagnostics.mods.none"),
                    Component.translatable("vulkanmod.diagnostics.mods.none.value"),
                    DiagnosticsSeverity.HEALTHY, false));
        } else {
            for (ModDiagnostic mod : relevantMods) {
                Component verification = mod.versionStatus() == net.vulkanmod.compat.diagnostics.VersionStatus.VERSION_MISMATCH
                        && !mod.expectedVersionPrefix().isEmpty()
                        ? Component.translatable("vulkanmod.diagnostics.version.version_mismatch_expected",
                                mod.expectedVersionPrefix())
                        : enumValue("version", mod.versionStatus());
                Component value = Component.translatable("vulkanmod.diagnostics.mod.value",
                        mod.version(),
                        enumValue("category", mod.category()),
                        enumValue("mode", mod.policyMode()),
                        verification);
                rows.add(textRow(Component.literal(mod.displayName() + " [" + mod.id() + "]"),
                        value, mod.severity(), false));
            }
        }

        rows.add(section("vulkanmod.diagnostics.section.paths"));
        for (RenderPathDiagnostic path : snapshot.renderPaths()) {
            rows.add(textRow(
                    enumValue("path", path.path()),
                    Component.translatable("vulkanmod.diagnostics.path.value",
                            path.owner(),
                            enumValue("mode", path.mode()),
                            Component.translatable(path.active()
                                    ? "vulkanmod.diagnostics.status.active"
                                    : "vulkanmod.diagnostics.status.inactive")),
                    path.severity(), false));
        }

        rows.add(section("vulkanmod.diagnostics.section.session"));
        if (snapshot.sessionIssues().isEmpty()) {
            rows.add(textRow(Component.translatable("vulkanmod.diagnostics.session.none"),
                    Component.translatable("vulkanmod.diagnostics.session.none.value"),
                    DiagnosticsSeverity.HEALTHY, false));
        } else {
            for (SessionIssueDiagnostic issue : snapshot.sessionIssues()) {
                Component value = issue.truncated()
                        ? Component.translatable("vulkanmod.diagnostics.session.truncated", issue.omittedCount())
                        : issue.type() == net.vulkanmod.compat.diagnostics.SessionIssueType.MOD_INVENTORY_UNAVAILABLE
                        ? Component.translatable("vulkanmod.diagnostics.issue.mod_inventory_unavailable.value")
                        : Component.literal(issue.key());
                rows.add(textRow(enumValue("issue", issue.type()), value,
                        issue.severity(), false));
            }
        }

        rows.add(section("vulkanmod.diagnostics.section.advanced"));
        var flags = snapshot.runtimeFlags();
        rows.add(flagRow("vulkanmod.diagnostics.flag.diagnostics", flags.diagnostics()));
        rows.add(flagRow("vulkanmod.diagnostics.flag.hud_trace", flags.hudTrace()));
        rows.add(flagRow("vulkanmod.diagnostics.flag.profiling_mixins", flags.profilingMixins()));
        rows.add(flagRow("vulkanmod.diagnostics.flag.debug_mixins", flags.debugMixins()));

        list.setRows(rows);
    }

    private void addDoctorRows(List<DiagnosticsRow> rows) {
        rows.add(section("vulkanmod.diagnostics.doctor.section"));
        if (doctorAssessment == null || doctorAssessment.recommendations().isEmpty()) {
            rows.add(textRow(
                    Component.translatable("vulkanmod.diagnostics.doctor.summary"),
                    Component.translatable("vulkanmod.diagnostics.doctor.summary.clean"),
                    DiagnosticsSeverity.HEALTHY, false));
        } else {
            rows.add(textRow(
                    Component.translatable("vulkanmod.diagnostics.doctor.summary"),
                    Component.translatable("vulkanmod.diagnostics.doctor.summary.recommendations",
                            doctorAssessment.recommendations().size()),
                    doctorAssessment.recommendations().getFirst().severity(), false));
        }

        if (!doctorFeedback.getString().isEmpty()) {
            rows.add(textRow(
                    Component.translatable("vulkanmod.diagnostics.doctor.stage.status"),
                    doctorFeedback, DiagnosticsSeverity.NOTICE, false));
        }

        if (pendingDoctorPlan != null) {
            addStagedDoctorPlanRows(rows, pendingDoctorPlan.action());
        }

        if (doctorAssessment != null) {
            int priority = 1;
            for (DoctorRecommendation recommendation : doctorAssessment.recommendations()) {
                String rule = recommendation.rule().name().toLowerCase(Locale.ROOT);
                Component title = Component.translatable("vulkanmod.diagnostics.doctor.rule." + rule + ".title");
                rows.add(textRow(
                        Component.translatable("vulkanmod.diagnostics.doctor.priority", priority, title),
                        doctorSubjects(recommendation), recommendation.severity(), false));
                rows.add(textRow(
                        Component.translatable("vulkanmod.diagnostics.doctor.recommendation"),
                        Component.translatable("vulkanmod.diagnostics.doctor.rule." + rule + ".advice"),
                        recommendation.severity(), false));
                rows.add(textRow(
                        Component.translatable("vulkanmod.diagnostics.doctor.assessment"),
                        doctorRecommendationDetails(recommendation), DiagnosticsSeverity.NOTICE, false));

                recommendation.stagedAction().ifPresent(action -> addDoctorActionRows(rows, action));
                priority++;
            }
        }

        addDoctorBenchmarkRows(rows);
    }

    private void addStagedDoctorPlanRows(List<DiagnosticsRow> rows, DoctorAction.StageSettings action) {
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.doctor.staged.title"),
                Component.translatable("vulkanmod.diagnostics.doctor.staged.count", action.changes().size()),
                DiagnosticsSeverity.NOTICE, false));
        for (DoctorSettingChange change : action.changes()) {
            rows.add(textRow(
                    Component.translatable("vulkanmod.diagnostics.doctor.setting."
                            + change.setting().name().toLowerCase(Locale.ROOT)),
                    Component.translatable("vulkanmod.diagnostics.doctor.preview.change",
                            doctorValue(change.expectedValue(), change.setting()),
                            doctorValue(change.targetValue(), change.setting())),
                    DiagnosticsSeverity.NOTICE, false));
        }
    }

    private static Component doctorRecommendationDetails(DoctorRecommendation recommendation) {
        return Component.translatable("vulkanmod.diagnostics.doctor.assessment.value",
                Component.translatable("vulkanmod.diagnostics.doctor.confidence."
                        + recommendation.confidence().name().toLowerCase(Locale.ROOT)),
                Component.translatable("vulkanmod.diagnostics.doctor.impact."
                        + recommendation.impact().performance().name().toLowerCase(Locale.ROOT)),
                Component.translatable("vulkanmod.diagnostics.doctor.impact."
                        + recommendation.impact().visualQuality().name().toLowerCase(Locale.ROOT)),
                Component.translatable(recommendation.impact().restartRequired()
                        ? "vulkanmod.diagnostics.doctor.restart.yes"
                        : "vulkanmod.diagnostics.doctor.restart.no"));
    }

    private void addDoctorActionRows(List<DiagnosticsRow> rows, DoctorAction.StageSettings action) {
        if (pendingDoctorPlan != null) {
            return;
        }
        if (previewedDoctorAction == null || !previewedDoctorAction.id().equals(action.id())) {
            Button preview = button("vulkanmod.diagnostics.doctor.action.preview",
                    ignored -> {
                        previewedDoctorAction = action;
                        doctorFeedback = Component.empty();
                        rebuildRows();
                    });
            rows.add(actionRow("doctor-action:" + action.id(), preview));
            return;
        }

        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.doctor.preview.title"),
                Component.translatable("vulkanmod.diagnostics.doctor.preview.count", action.changes().size()),
                DiagnosticsSeverity.NOTICE, false));
        for (DoctorSettingChange change : action.changes()) {
            rows.add(textRow(
                    Component.translatable("vulkanmod.diagnostics.doctor.setting."
                            + change.setting().name().toLowerCase(Locale.ROOT)),
                    Component.translatable("vulkanmod.diagnostics.doctor.preview.change",
                            doctorValue(change.expectedValue(), change.setting()),
                            doctorValue(change.targetValue(), change.setting())),
                    DiagnosticsSeverity.NOTICE, false));
        }

        Button stage = button("vulkanmod.diagnostics.doctor.action.stage",
                ignored -> stageDoctorAction(action));
        stage.active = !doctorSettings.hasPendingSettings();
        if (!stage.active) {
            stage.setTooltip(Tooltip.create(Component.translatable(
                    "vulkanmod.diagnostics.doctor.stage.blocked.pending")));
        }
        Button cancel = button("vulkanmod.diagnostics.doctor.action.cancel",
                ignored -> {
                    previewedDoctorAction = null;
                    doctorFeedback = Component.empty();
                    rebuildRows();
                });
        rows.add(actionRow("doctor-action:" + action.id(), stage, cancel));
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.doctor.preview.scope"),
                Component.translatable("vulkanmod.diagnostics.doctor.preview.scope.value"),
                DiagnosticsSeverity.NOTICE, false));
    }

    private void stageDoctorAction(DoctorAction.StageSettings action) {
        DoctorSettingsSnapshot before = doctorSettings.settingsSnapshot();
        DoctorSettingsSnapshot expected;
        try {
            expected = before.withChanges(action.changes());
        } catch (IllegalStateException exception) {
            doctorFeedback = Component.translatable(
                    "vulkanmod.diagnostics.doctor.stage.result.stale_assessment");
            previewedDoctorAction = null;
            rebuildRows();
            return;
        }

        DoctorStageResult result = doctorSettings.stage(action);
        if (result == DoctorStageResult.STAGED) {
            long contextToken = doctorSettings.contextToken();
            Optional<BenchmarkResult> baseline = snapshot.latestBenchmark()
                    .filter(value -> !snapshot.benchmarkIsStale(value))
                    .filter(value -> value.runId() > 0L)
                    .filter(value -> value.worldSessionId() == contextToken);
            EnumSet<DoctorSetting> changedSettings = EnumSet.noneOf(DoctorSetting.class);
            action.changes().forEach(change -> changedSettings.add(change.setting()));
            pendingDoctorPlan = new PendingDoctorPlan(
                    action, baseline, expected, changedSettings, contextToken);
            doctorPlanConfirmedForApply = false;
            doctorFeedback = Component.translatable(
                    "vulkanmod.diagnostics.doctor.stage.success", action.changes().size());
        } else {
            doctorFeedback = Component.translatable(
                    "vulkanmod.diagnostics.doctor.stage.result."
                            + result.name().toLowerCase(Locale.ROOT));
        }
        previewedDoctorAction = null;
        rebuildRows();
        if (result == DoctorStageResult.STAGED) {
            doctorSettings.focusApplyControl();
        }
    }

    private void addDoctorBenchmarkRows(List<DiagnosticsRow> rows) {
        Component status;
        DoctorBenchmarkEvaluation evaluation = null;
        if (pendingDoctorPlan != null) {
            status = Component.translatable(pendingDoctorPlan.baseline().isPresent()
                    ? "vulkanmod.diagnostics.doctor.benchmark.baseline_staged"
                    : "vulkanmod.diagnostics.doctor.benchmark.no_baseline");
        } else {
            evaluation = DoctorBenchmarkSession.evaluate(
                    snapshot.latestBenchmark(), snapshot.currentSettings(),
                    doctorSettings.settingsSnapshot(), doctorSettings.contextToken());
            status = Component.translatable("vulkanmod.diagnostics.doctor.benchmark."
                    + evaluation.status().name().toLowerCase(Locale.ROOT));
        }
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.doctor.benchmark.title"),
                status, evaluation != null && evaluation.status() == DoctorBenchmarkStatus.READY
                        ? DiagnosticsSeverity.HEALTHY : DiagnosticsSeverity.NOTICE, false));

        if (evaluation != null) {
            evaluation.comparison().ifPresent(comparison -> addDoctorComparison(rows, comparison));
        }
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.doctor.benchmark.note"),
                Component.translatable("vulkanmod.diagnostics.doctor.benchmark.disclaimer"),
                DiagnosticsSeverity.NOTICE, false));
    }

    private void addDoctorComparison(List<DiagnosticsRow> rows, DoctorBenchmarkComparison comparison) {
        rows.add(doctorComparisonMetric("average_fps", comparison.before().averageFps(),
                comparison.after().averageFps(), comparison.averageFpsDelta(), true));
        rows.add(doctorComparisonMetric("one_percent_low", comparison.before().onePercentLowFps(),
                comparison.after().onePercentLowFps(), comparison.onePercentLowDelta(), true));
        rows.add(doctorComparisonMetric("p95_frame_time", comparison.before().p95FrameTimeMs(),
                comparison.after().p95FrameTimeMs(), comparison.p95FrameTimeDelta(), false));
        ComparisonDirection stutterDirection = comparisonDirection(
                comparison.before().framesOver50Ms(), comparison.after().framesOver50Ms(), false);
        rows.add(textRow(
                Component.translatable("vulkanmod.diagnostics.benchmark.frames_over_50ms"),
                Component.translatable("vulkanmod.diagnostics.doctor.benchmark.comparison",
                        comparison.before().framesOver50Ms(), comparison.after().framesOver50Ms(),
                        signed(comparison.framesOver50MsDelta()), directionComponent(stutterDirection)),
                comparisonSeverity(stutterDirection), false));
    }

    private DiagnosticsRow doctorComparisonMetric(String metric, double before, double after,
                                                   double delta, boolean higherIsBetter) {
        ComparisonDirection direction = comparisonDirection(before, after, higherIsBetter);
        return textRow(
                Component.translatable("vulkanmod.diagnostics.benchmark." + metric),
                Component.translatable("vulkanmod.diagnostics.doctor.benchmark.comparison",
                        format(before), format(after), signed(delta), directionComponent(direction)),
                comparisonSeverity(direction), false);
    }

    private static ComparisonDirection comparisonDirection(double before, double after,
                                                           boolean higherIsBetter) {
        double delta = after - before;
        double noiseBand = Math.max(0.05, Math.abs(before) * 0.02);
        if (Math.abs(delta) < noiseBand) {
            return ComparisonDirection.UNCHANGED;
        }
        boolean improved = higherIsBetter ? delta > 0.0 : delta < 0.0;
        return improved ? ComparisonDirection.IMPROVED : ComparisonDirection.REGRESSED;
    }

    private static ComparisonDirection comparisonDirection(int before, int after,
                                                           boolean higherIsBetter) {
        int delta = after - before;
        int noiseBand = Math.max(2, (int) Math.ceil(Math.abs(before) * 0.10));
        if (Math.abs(delta) < noiseBand) {
            return ComparisonDirection.UNCHANGED;
        }
        boolean improved = higherIsBetter ? delta > 0 : delta < 0;
        return improved ? ComparisonDirection.IMPROVED : ComparisonDirection.REGRESSED;
    }

    private static Component directionComponent(ComparisonDirection direction) {
        return Component.translatable("vulkanmod.diagnostics.doctor.benchmark.direction."
                + direction.name().toLowerCase(Locale.ROOT));
    }

    private static DiagnosticsSeverity comparisonSeverity(ComparisonDirection direction) {
        return switch (direction) {
            case IMPROVED -> DiagnosticsSeverity.HEALTHY;
            case UNCHANGED -> DiagnosticsSeverity.NOTICE;
            case REGRESSED -> DiagnosticsSeverity.WARNING;
        };
    }

    private Component doctorSubjects(DoctorRecommendation recommendation) {
        if (recommendation.subjects().isEmpty()) {
            return Component.translatable("vulkanmod.diagnostics.doctor.occurrences",
                    recommendation.occurrences());
        }
        String joined = String.join("; ", recommendation.subjects());
        if (recommendation.omittedSubjects() > 0) {
            joined += " +" + recommendation.omittedSubjects();
        }
        return Component.literal(joined);
    }

    private static Component doctorValue(DoctorValue value, DoctorSetting setting) {
        if (value instanceof DoctorValue.BooleanValue bool) {
            return Component.translatable(bool.value() ? "options.on" : "options.off");
        }
        int integer = ((DoctorValue.IntegerValue) value).value();
        return setting == DoctorSetting.RENDER_SCALE
                ? Component.literal(integer + "%")
                : Component.literal(Integer.toString(integer));
    }

    private static String signed(double value) {
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    private static String signed(int value) {
        return String.format(Locale.ROOT, "%+d", value);
    }

    private void addBenchmarkRows(List<DiagnosticsRow> rows, BenchmarkResult result) {
        if (snapshot.benchmarkIsStale(result)) {
            rows.add(textRow(Component.translatable("vulkanmod.diagnostics.benchmark.stale"),
                    Component.translatable("vulkanmod.diagnostics.benchmark.stale.value"),
                    DiagnosticsSeverity.NOTICE, false));
        }
        rows.add(metric("average_fps", result.averageFps()));
        rows.add(metric("one_percent_low", result.onePercentLowFps()));
        rows.add(metric("average_frame_time", result.averageFrameTimeMs()));
        rows.add(metric("p95_frame_time", result.p95FrameTimeMs()));
        rows.add(metric("average_cpu_time", result.averageCpuRenderTimeMs()));
        rows.add(valueRow("vulkanmod.diagnostics.benchmark.frames_over_50ms",
                Integer.toString(result.framesOver50Ms())));
        rows.add(textRow(Component.translatable("vulkanmod.diagnostics.benchmark.duration_frames"),
                Component.translatable("vulkanmod.diagnostics.benchmark.duration_frames.value",
                        format(result.durationSeconds()), result.frameCount()),
                DiagnosticsSeverity.HEALTHY, false));
    }

    private DiagnosticsRow metric(String key, double value) {
        return valueRow("vulkanmod.diagnostics.benchmark." + key, format(value));
    }

    private DiagnosticsRow flagRow(String labelKey, RuntimeFlagDiagnostic flag) {
        String valueKey = !flag.available()
                ? flag.requested()
                ? "vulkanmod.diagnostics.flag.requested_unavailable"
                : "vulkanmod.diagnostics.flag.unavailable"
                : flag.effective()
                ? "vulkanmod.diagnostics.flag.enabled"
                : "vulkanmod.diagnostics.flag.disabled_restart";
        Component value = Component.translatable(valueKey);
        return textRow(Component.translatable(labelKey), value,
                flag.requested() && !flag.effective()
                        ? DiagnosticsSeverity.WARNING
                        : flag.effective() ? DiagnosticsSeverity.NOTICE : DiagnosticsSeverity.HEALTHY,
                false);
    }

    private DiagnosticsRow valueRow(String labelKey, String value) {
        return textRow(Component.translatable(labelKey), Component.literal(value),
                DiagnosticsSeverity.HEALTHY, false);
    }

    private DiagnosticsRow section(String translationKey) {
        return textRow(Component.translatable(translationKey).withStyle(ChatFormatting.BOLD),
                Component.empty(), DiagnosticsSeverity.HEALTHY, true);
    }

    private DiagnosticsRow textRow(Component label, Component value,
                                   DiagnosticsSeverity severity, boolean section) {
        return DiagnosticsRow.text(new DiagnosticTextWidget(minecraft.font, label, value,
                narrow, section, severityColor(severity)));
    }

    private DiagnosticsRow actionRow(Button... buttons) {
        return DiagnosticsRow.actions("actions:" + java.util.Arrays.stream(buttons)
                .map(button -> button.getMessage().getString())
                .reduce((first, second) -> first + "|" + second)
                .orElse("empty"), List.of(buttons));
    }

    private DiagnosticsRow actionRow(String focusKey, Button... buttons) {
        return DiagnosticsRow.actions(focusKey, List.of(buttons));
    }

    private Button button(String translationKey, Button.OnPress onPress) {
        return Button.builder(Component.translatable(translationKey), onPress)
                .size(120, 20)
                .build();
    }

    private static Component enumValue(String family, Enum<?> value) {
        return Component.translatable("vulkanmod.diagnostics." + family + "."
                + value.name().toLowerCase(Locale.ROOT));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static int severityColor(DiagnosticsSeverity severity) {
        return switch (severity) {
            case HEALTHY -> 0xFF8FD694;
            case NOTICE -> 0xFFFFD166;
            case WARNING -> 0xFFFF9F43;
            case INCOMPATIBLE -> 0xFFFF6B6B;
        };
    }

    private static final class DiagnosticsList extends ContainerObjectSelectionList<DiagnosticsRow>
            implements SettingsPageListener {
        DiagnosticsList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
            this.centerListVertically = false;
        }

        void setRows(List<DiagnosticsRow> rows) {
            double previousScroll = scrollAmount();
            DiagnosticsRow previouslyFocused = getFocused();
            String focusKey = previouslyFocused == null ? null : previouslyFocused.focusKey();
            int focusedWidget = previouslyFocused == null ? -1 : previouslyFocused.focusedWidgetIndex();
            setFocused(null);
            replaceEntries(rows);
            setScrollAmount(previousScroll);
            if (focusKey != null) {
                rows.stream()
                        .filter(row -> focusKey.equals(row.focusKey()))
                        .findFirst()
                        .ifPresent(row -> {
                            setFocused(row);
                            row.restoreWidgetFocus(focusedWidget);
                        });
            }
        }

        @Override
        public int getRowWidth() {
            return Math.max(80, getWidth() - 18);
        }

        @Override
        protected int scrollBarX() {
            return getX() + getWidth() - 6;
        }

        @Override
        protected void renderListBackground(GuiGraphics guiGraphics) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics guiGraphics) {
        }
    }

    private static final class DiagnosticsRow extends ContainerObjectSelectionList.Entry<DiagnosticsRow> {
        private final String focusKey;
        private final List<AbstractWidget> widgets;

        private DiagnosticsRow(String focusKey, List<AbstractWidget> widgets) {
            this.focusKey = focusKey;
            this.widgets = widgets;
        }

        static DiagnosticsRow text(DiagnosticTextWidget widget) {
            return new DiagnosticsRow("text:" + widget.getMessage().getString(), List.of(widget));
        }

        static DiagnosticsRow actions(String focusKey, List<Button> buttons) {
            return new DiagnosticsRow(focusKey, new ArrayList<>(buttons));
        }

        String focusKey() {
            return focusKey;
        }

        int focusedWidgetIndex() {
            for (int i = 0; i < widgets.size(); i++) {
                if (widgets.get(i).isFocused()) {
                    return i;
                }
            }
            return -1;
        }

        void restoreWidgetFocus(int index) {
            if (index >= 0 && !widgets.isEmpty()) {
                widgets.get(Math.min(index, widgets.size() - 1)).setFocused(true);
            }
        }

        @Override
        public void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean hovering,
                                  float partialTick) {
                                                                                            
                                                                    
            int top = this.getY();
            int left = this.getX();
            int width = this.getWidth();
            int height = this.getHeight();
            if (widgets.size() == 1 && widgets.getFirst() instanceof DiagnosticTextWidget textWidget) {
                textWidget.setPosition(left, top + 1);
                textWidget.setWidth(width);
                textWidget.setHeight(Math.max(20, height - 2));
                textWidget.render(guiGraphics, mouseX, mouseY, partialTick);
                return;
            }

            int gap = 6;
            int buttonWidth = Math.max(60, (width - gap * Math.max(0, widgets.size() - 1))
                    / Math.max(1, widgets.size()));
            int x = left;
            for (AbstractWidget widget : widgets) {
                widget.setPosition(x, top + Math.max(0, (height - 20) / 2));
                widget.setWidth(buttonWidth);
                widget.setHeight(20);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
                x += buttonWidth + gap;
            }
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }
    }

    private static final class DiagnosticTextWidget extends AbstractWidget {
        private final Font font;
        private final Component label;
        private final Component value;
        private final boolean narrow;
        private final boolean section;
        private final int valueColor;

        DiagnosticTextWidget(Font font, Component label, Component value, boolean narrow,
                             boolean section, int valueColor) {
            super(0, 0, 0, 0, Component.translatable("vulkanmod.diagnostics.narration.row", label, value));
            this.font = font;
            this.label = label;
            this.value = value;
            this.narrow = narrow;
            this.section = section;
            this.valueColor = valueColor;
            setTooltip(Tooltip.create(getMessage()));
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (isFocused()) {
                guiGraphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0xFFE05252);
            }

            int labelY = getY() + (section ? 7 : narrow ? 4 : (getHeight() - font.lineHeight) / 2);
            guiGraphics.drawString(font, clipped(label, section ? getWidth() - 8 : narrow ? getWidth() - 8 : getWidth() / 2 - 8),
                    getX() + 4, labelY, section ? 0xFFFFFFFF : 0xFFE5E5E5, false);
            if (section || value.getString().isEmpty()) {
                return;
            }

            int valueX = narrow ? getX() + 12 : getX() + getWidth() / 2;
            int valueY = narrow ? getY() + 4 + font.lineHeight + 2 : labelY;
            int valueWidth = narrow ? getWidth() - 16 : getWidth() / 2 - 6;
            guiGraphics.drawString(font, clipped(value, valueWidth), valueX, valueY, valueColor, false);
        }

        private Component clipped(Component component, int maxWidth) {
            if (font.width(component) <= maxWidth) {
                return component;
            }
            String ellipsis = "…";
            return Component.literal(font.plainSubstrByWidth(component.getString(),
                    Math.max(0, maxWidth - font.width(ellipsis))) + ellipsis);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            narrationElementOutput.add(NarratedElementType.TITLE, getMessage());
        }
    }

    private record PendingDoctorPlan(
            DoctorAction.StageSettings action,
            Optional<BenchmarkResult> baseline,
            DoctorSettingsSnapshot expectedSettings,
            Set<DoctorSetting> changedSettings,
            long contextToken) {
        private PendingDoctorPlan {
            action = java.util.Objects.requireNonNull(action, "action");
            baseline = java.util.Objects.requireNonNull(baseline, "baseline");
            expectedSettings = java.util.Objects.requireNonNull(expectedSettings, "expectedSettings");
            changedSettings = Set.copyOf(changedSettings);
        }
    }

    private enum ComparisonDirection {
        IMPROVED,
        UNCHANGED,
        REGRESSED
    }

    private enum Availability {
        AVAILABLE,
        NO_WORLD,
        PENDING_SETTINGS,
        ALREADY_RUNNING
    }
}
