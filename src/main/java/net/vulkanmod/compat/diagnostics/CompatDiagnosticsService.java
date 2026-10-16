package net.vulkanmod.compat.diagnostics;

import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.CompatCategory;
import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.CompatMods;
import net.vulkanmod.compat.CompatPolicyManager;
import net.vulkanmod.compat.ExternalClientFaultBoundary;
import net.vulkanmod.compat.RuntimeOptions;
import net.vulkanmod.compat.opengl.GlFunctionRegistry;
import net.vulkanmod.compat.path.RenderPathOwnership;
import net.vulkanmod.diagnostics.benchmark.BenchmarkFingerprint;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;
import net.vulkanmod.diagnostics.benchmark.MinecraftBenchmarkController;
import net.vulkanmod.gl.GlEmulationLog;
import net.vulkanmod.render.optimization.ChunkBuildFailureTracker;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CompatDiagnosticsService {
    public static final String UNKNOWN = "Unknown";
    private static final int MAX_SESSION_KEYS_PER_TYPE = 100;
    private static final Set<String> CORE_MOD_IDS = Set.of("minecraft", "neoforge", "fml", "vulkanmod");
    private static final Comparator<ModDiagnostic> MOD_ORDER = Comparator
            .comparingInt((ModDiagnostic mod) -> mod.severity().ordinal()).reversed()
            .thenComparing(ModDiagnostic::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(ModDiagnostic::id);

    private CompatDiagnosticsService() {
    }

    public static CompatDiagnosticsSnapshot capture() {
        return capture(Clock.systemUTC());
    }

    static CompatDiagnosticsSnapshot capture(Clock clock) {
        InventoryCapture inventoryCapture = captureModInventory();
        List<InventoryMod> inventory = inventoryCapture.mods();
        List<RenderPathOwnership.PathState> paths = RenderPathOwnership.snapshot();
        List<SessionIssueDiagnostic> issues = captureSessionIssues();
        if (!inventoryCapture.available()) {
            issues.add(new SessionIssueDiagnostic(SessionIssueType.MOD_INVENTORY_UNAVAILABLE,
                    "mod_inventory", DiagnosticsSeverity.WARNING));
        }
        Minecraft minecraft = Minecraft.getInstance();
        BenchmarkFingerprint currentSettings = MinecraftBenchmarkController.captureFingerprint(minecraft);
        long worldSessionId = MinecraftBenchmarkController.worldSessionId(minecraft);

        return buildSnapshot(
                clock.instant(),
                captureSystem(inventory),
                captureRuntimeFlags(),
                currentSettings,
                worldSessionId,
                inventory,
                paths,
                issues,
                MinecraftBenchmarkController.latestResult(),
                CompatPolicyManager.isCoexistenceIncompatible());
    }

    static CompatDiagnosticsSnapshot buildSnapshot(
            Instant capturedAt,
            SystemDiagnostic system,
            RuntimeFlagsDiagnostic runtimeFlags,
            BenchmarkFingerprint currentSettings,
            List<InventoryMod> inventory,
            List<RenderPathOwnership.PathState> pathStates,
            List<SessionIssueDiagnostic> issues,
            Optional<BenchmarkResult> latestBenchmark,
            boolean coexistenceIncompatible) {
        return buildSnapshot(capturedAt, system, runtimeFlags, currentSettings, 0L, inventory,
                pathStates, issues, latestBenchmark, coexistenceIncompatible);
    }

    static CompatDiagnosticsSnapshot buildSnapshot(
            Instant capturedAt,
            SystemDiagnostic system,
            RuntimeFlagsDiagnostic runtimeFlags,
            BenchmarkFingerprint currentSettings,
            long worldSessionId,
            List<InventoryMod> inventory,
            List<RenderPathOwnership.PathState> pathStates,
            List<SessionIssueDiagnostic> issues,
            Optional<BenchmarkResult> latestBenchmark,
            boolean coexistenceIncompatible) {

        List<ModDiagnostic> mods = classifyMods(inventory);
        List<RenderPathDiagnostic> paths = pathStates.stream()
                .map(state -> new RenderPathDiagnostic(
                        state.path(),
                        safe(state.owner()),
                        state.mode(),
                        state.mode() != CompatMode.OFF,
                        state.mode() == CompatMode.INCOMPATIBLE
                                ? DiagnosticsSeverity.INCOMPATIBLE
                                : state.mode() == CompatMode.OFF
                                ? DiagnosticsSeverity.HEALTHY
                                : DiagnosticsSeverity.NOTICE))
                .sorted(Comparator.comparing(path -> path.path().ordinal()))
                .toList();
        List<SessionIssueDiagnostic> sortedIssues = issues.stream()
                .sorted(Comparator.comparing((SessionIssueDiagnostic issue) -> issue.type().ordinal())
                        .thenComparing(SessionIssueDiagnostic::truncated)
                        .thenComparing(SessionIssueDiagnostic::key))
                .toList();

        DiagnosticsSeverity severity = coexistenceIncompatible
                ? DiagnosticsSeverity.INCOMPATIBLE
                : DiagnosticsSeverity.HEALTHY;
        for (ModDiagnostic mod : mods) {
            severity = DiagnosticsSeverity.max(severity, mod.severity());
        }
        for (RenderPathDiagnostic path : paths) {
            severity = DiagnosticsSeverity.max(severity, path.severity());
        }
        for (SessionIssueDiagnostic issue : sortedIssues) {
            severity = DiagnosticsSeverity.max(severity, issue.severity());
        }
        if (UNKNOWN.equals(system.gpu()) || UNKNOWN.equals(system.minecraftVersion())
                || UNKNOWN.equals(system.neoForgeVersion())) {
            severity = DiagnosticsSeverity.max(severity, DiagnosticsSeverity.WARNING);
        }

        return new CompatDiagnosticsSnapshot(
                capturedAt,
                severity,
                system,
                runtimeFlags,
                currentSettings,
                worldSessionId,
                mods,
                paths,
                sortedIssues,
                latestBenchmark);
    }

    static List<ModDiagnostic> classifyMods(List<InventoryMod> inventory) {
        Map<String, InventoryMod> deduplicated = new LinkedHashMap<>();
        for (InventoryMod raw : inventory) {
            if (raw == null || raw.id() == null) {
                continue;
            }
            String id = raw.id().trim().toLowerCase(Locale.ROOT);
            if (id.isEmpty() || CORE_MOD_IDS.contains(id)) {
                continue;
            }
            deduplicated.putIfAbsent(id, new InventoryMod(id, safe(raw.displayName()), safe(raw.version())));
        }

        return deduplicated.values().stream()
                .map(CompatDiagnosticsService::classifyMod)
                .sorted(MOD_ORDER)
                .toList();
    }

    static VersionStatus classifyVersion(String modId, String version, boolean recognized) {
        if (recognized && isUnknown(version)) {
            return VersionStatus.UNKNOWN_VERSION;
        }
        Optional<String> expectedPrefix = CompatPolicyManager.getVerifiedVersionPrefix(modId);
        if (expectedPrefix.isPresent()) {
            return version.startsWith(expectedPrefix.get())
                    ? VersionStatus.VERIFIED_PREFIX
                    : VersionStatus.VERSION_MISMATCH;
        }
        return recognized ? VersionStatus.RECOGNIZED_UNPROFILED : VersionStatus.UNTRACKED;
    }

    private static ModDiagnostic classifyMod(InventoryMod mod) {
        CompatCategory category = CompatPolicyManager.getCompatCategory(mod.id());
        boolean relevant = category != CompatCategory.UNKNOWN || CompatMods.contains(CompatMods.REPORT_MOD_IDS, mod.id());
        VersionStatus versionStatus = classifyVersion(mod.id(), mod.version(), relevant);
        CompatMode policyMode = category == CompatCategory.RENDERER_GL
                ? CompatMode.INCOMPATIBLE
                : CompatPolicyManager.getCompatMode(mod.id());
        DiagnosticsSeverity severity = category == CompatCategory.RENDERER_GL
                ? DiagnosticsSeverity.INCOMPATIBLE
                : switch (versionStatus) {
                    case VERIFIED_PREFIX -> DiagnosticsSeverity.HEALTHY;
                    case VERSION_MISMATCH, UNKNOWN_VERSION -> DiagnosticsSeverity.WARNING;
                    case RECOGNIZED_UNPROFILED, UNTRACKED -> DiagnosticsSeverity.NOTICE;
                };

        return new ModDiagnostic(
                mod.id(),
                isUnknown(mod.displayName()) ? mod.id() : mod.displayName(),
                mod.version(),
                CompatPolicyManager.getVerifiedVersionPrefix(mod.id()).orElse(""),
                category,
                policyMode,
                versionStatus,
                severity,
                relevant);
    }

    private static InventoryCapture captureModInventory() {
        List<InventoryMod> mods = new ArrayList<>();
        try {
            for (var mod : ModList.get().getMods()) {
                mods.add(new InventoryMod(mod.getModId(), mod.getDisplayName(), mod.getVersion().toString()));
            }
            return new InventoryCapture(List.copyOf(mods), true);
        } catch (Throwable throwable) {
            Initializer.LOGGER.warn("Unable to capture the loaded mod inventory for Diagnostics", throwable);
            return new InventoryCapture(List.of(), false);
        }
    }

    private static SystemDiagnostic captureSystem(List<InventoryMod> inventory) {
        String minecraftVersion = safeValue(() -> SharedConstants.getCurrentVersion().getName());
        String neoForgeVersion = inventory.stream()
                .filter(mod -> "neoforge".equalsIgnoreCase(mod.id()))
                .map(InventoryMod::version)
                .findFirst()
                .orElseGet(() -> modVersion("neoforge"));

        String gpu = UNKNOWN;
        String vendor = UNKNOWN;
        String driver = UNKNOWN;
        String vulkan = UNKNOWN;
        try {
            Device device = Vulkan.getDevice();
            if (device != null) {
                gpu = safe(device.deviceName);
                vendor = safe(device.vendorIdString);
                driver = safe(device.driverVersion);
                vulkan = safe(device.vkVersion);
            }
        } catch (Throwable ignored) {
        }

        return new SystemDiagnostic(
                safe(Initializer.getVersion()),
                minecraftVersion,
                safe(neoForgeVersion),
                gpu,
                vendor,
                driver,
                vulkan,
                safeValue(() -> net.vulkanmod.vulkan.SystemInfo.cpuInfo),
                safe(System.getProperty("java.version")),
                safe(System.getProperty("os.name")),
                safe(System.getProperty("os.arch")),
                GlFunctionRegistry.REPORTED_GL_VERSION,
                GlFunctionRegistry.registeredCount());
    }

    private static String modVersion(String id) {
        try {
            return ModList.get().getModContainerById(id)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse(UNKNOWN);
        } catch (Throwable ignored) {
            return UNKNOWN;
        }
    }

    private static List<SessionIssueDiagnostic> captureSessionIssues() {
        List<SessionIssueDiagnostic> issues = new ArrayList<>();
        addIssues(issues, SessionIssueType.CONTRACT_GAP, GlEmulationLog.reportedContractGaps());
        addIssues(issues, SessionIssueType.MISSING_GL_ENTRYPOINT, GlFunctionRegistry.reportedMissingFunctions());
        addIssues(issues, SessionIssueType.STUBBED_GL_ENTRYPOINT, GlFunctionRegistry.reportedStubbedFunctions());
        addIssues(issues, SessionIssueType.TRAMPOLINE_FAULT, GlFunctionRegistry.reportedFaultedFunctions());
        addIssues(issues, SessionIssueType.EXTERNAL_FAILURE, ExternalClientFaultBoundary.reportedFailures());
        addIssues(issues, SessionIssueType.CHUNK_BUILD_FAILURE, ChunkBuildFailureTracker.reportedFailures());
        return issues;
    }

    private static void addIssues(List<SessionIssueDiagnostic> destination, SessionIssueType type, Set<String> keys) {
        int added = 0;
        for (String key : keys.stream().sorted().toList()) {
            if (added == MAX_SESSION_KEYS_PER_TYPE) {
                break;
            }
            destination.add(new SessionIssueDiagnostic(type, safe(key), DiagnosticsSeverity.WARNING));
            added++;
        }
        if (keys.size() > added) {
            destination.add(new SessionIssueDiagnostic(type, "", DiagnosticsSeverity.WARNING,
                    keys.size() - added));
        }
    }

    private static RuntimeFlagsDiagnostic captureRuntimeFlags() {
        boolean diagnostics = RuntimeOptions.diagnosticsEnabled();
        boolean hudRequested = RuntimeOptions.hudTraceRequested();
        boolean hudEffective = RuntimeOptions.hudTraceEnabled();
        boolean profilingRequested = RuntimeOptions.profilingMixinsEnabled();
        boolean debugRequested = RuntimeOptions.debugMixinsEnabled();
        return new RuntimeFlagsDiagnostic(
                new RuntimeFlagDiagnostic(diagnostics, diagnostics, true, !diagnostics),
                new RuntimeFlagDiagnostic(hudRequested, hudEffective, true, !hudEffective),
                new RuntimeFlagDiagnostic(profilingRequested, false, false, false),
                new RuntimeFlagDiagnostic(debugRequested, false, false, false));
    }

    private static String safeValue(ValueSupplier supplier) {
        try {
            return safe(supplier.get());
        } catch (Throwable ignored) {
            return UNKNOWN;
        }
    }

    private static String safe(String value) {
        return isUnknown(value) ? UNKNOWN : value.trim();
    }

    private static boolean isUnknown(String value) {
        return value == null || value.isBlank() || "UNKNOWN".equalsIgnoreCase(value)
                || "null".equalsIgnoreCase(value);
    }

    @FunctionalInterface
    private interface ValueSupplier {
        String get() throws Exception;
    }

    record InventoryMod(String id, String displayName, String version) {
    }

    private record InventoryCapture(List<InventoryMod> mods, boolean available) {
    }
}
