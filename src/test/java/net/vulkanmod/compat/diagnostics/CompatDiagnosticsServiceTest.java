package net.vulkanmod.compat.diagnostics;

import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.compat.path.RenderPathOwnership;
import net.vulkanmod.diagnostics.benchmark.BenchmarkFingerprint;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatDiagnosticsServiceTest {
    @Test
    void classifiesVersionsWithoutChangingCompatibilityPolicy() {
        assertEquals(VersionStatus.VERIFIED_PREFIX,
                CompatDiagnosticsService.classifyVersion("create", "0.5.1", true));
        assertEquals(VersionStatus.VERSION_MISMATCH,
                CompatDiagnosticsService.classifyVersion("create", "6.0.0", true));
        assertEquals(VersionStatus.UNKNOWN_VERSION,
                CompatDiagnosticsService.classifyVersion("create", "UNKNOWN", true));
        assertEquals(VersionStatus.RECOGNIZED_UNPROFILED,
                CompatDiagnosticsService.classifyVersion("jei", "19.0", true));
        assertEquals(VersionStatus.UNKNOWN_VERSION,
                CompatDiagnosticsService.classifyVersion("jei", "Unknown", true));
        assertEquals(VersionStatus.UNTRACKED,
                CompatDiagnosticsService.classifyVersion("example", "1.0", false));
    }

    @Test
    void normalizesDeduplicatesExcludesCoreAndSortsBySeverity() {
        List<ModDiagnostic> mods = CompatDiagnosticsService.classifyMods(List.of(
                new CompatDiagnosticsService.InventoryMod("minecraft", "Minecraft", "1.21.1"),
                new CompatDiagnosticsService.InventoryMod(" Example ", "Example", "1.0"),
                new CompatDiagnosticsService.InventoryMod("example", "Duplicate", "2.0"),
                new CompatDiagnosticsService.InventoryMod("create", "Create", "6.0"),
                new CompatDiagnosticsService.InventoryMod("sodium", "Sodium", "1.0")));

        assertEquals(3, mods.size());
        assertEquals("sodium", mods.get(0).id());
        assertEquals(DiagnosticsSeverity.INCOMPATIBLE, mods.get(0).severity());
        assertEquals("create", mods.get(1).id());
        assertEquals(DiagnosticsSeverity.WARNING, mods.get(1).severity());
        assertEquals("example", mods.get(2).id());
        assertEquals("1.0", mods.get(2).version());
    }

    @Test
    void severityPrecedenceIsDeterministic() {
        CompatDiagnosticsSnapshot notice = snapshot(
                List.of(new RenderPathOwnership.PathState(RenderPath.GUI, "create", CompatMode.SAFE)),
                List.of(), false);
        assertEquals(DiagnosticsSeverity.NOTICE, notice.severity());

        CompatDiagnosticsSnapshot warning = snapshot(
                List.of(new RenderPathOwnership.PathState(RenderPath.GUI, "create", CompatMode.SAFE)),
                List.of(new SessionIssueDiagnostic(SessionIssueType.CONTRACT_GAP, "draw.generic",
                        DiagnosticsSeverity.WARNING)), false);
        assertEquals(DiagnosticsSeverity.WARNING, warning.severity());

        CompatDiagnosticsSnapshot incompatible = snapshot(List.of(), List.of(), true);
        assertEquals(DiagnosticsSeverity.INCOMPATIBLE, incompatible.severity());
    }

    @Test
    void snapshotCollectionsAreImmutableCopies() {
        List<ModDiagnostic> mods = new ArrayList<>();
        CompatDiagnosticsSnapshot snapshot = new CompatDiagnosticsSnapshot(
                Instant.EPOCH, DiagnosticsSeverity.HEALTHY, system(), flags(), fingerprint(),
                mods, new ArrayList<>(), new ArrayList<>(), Optional.empty());
        mods.add(new ModDiagnostic("late", "Late", "1", "", net.vulkanmod.compat.CompatCategory.UNKNOWN,
                CompatMode.OFF, VersionStatus.UNTRACKED, DiagnosticsSeverity.NOTICE, false));

        assertTrue(snapshot.mods().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.mods().add(null));
    }

    @Test
    void benchmarkFreshnessIncludesOpaqueWorldProvenanceWhenAvailable() {
        BenchmarkResult sameWorld = new BenchmarkResult(1L, 9L, fingerprint(),
                60, 40, 16.6, 20, 8, 1, 600, 10, Instant.EPOCH);
        BenchmarkResult priorWorld = new BenchmarkResult(2L, 8L, fingerprint(),
                60, 40, 16.6, 20, 8, 1, 600, 10, Instant.EPOCH);
        CompatDiagnosticsSnapshot snapshot = new CompatDiagnosticsSnapshot(
                Instant.EPOCH, DiagnosticsSeverity.HEALTHY, system(), flags(), fingerprint(), 9L,
                List.of(), List.of(), List.of(), Optional.of(priorWorld));

        assertTrue(!snapshot.benchmarkIsStale(sameWorld));
        assertTrue(snapshot.benchmarkIsStale(priorWorld));
    }

    private static CompatDiagnosticsSnapshot snapshot(List<RenderPathOwnership.PathState> paths,
                                                        List<SessionIssueDiagnostic> issues,
                                                        boolean incompatible) {
        return CompatDiagnosticsService.buildSnapshot(
                Instant.EPOCH, system(), flags(), fingerprint(), List.of(), paths, issues,
                Optional.empty(), incompatible);
    }

    static SystemDiagnostic system() {
        return new SystemDiagnostic("0.2", "1.21.1", "21.1", "GPU", "Vendor", "Driver",
                "1.3", "CPU", "21", "Windows", "amd64", "3.2", 100);
    }

    static RuntimeFlagsDiagnostic flags() {
        RuntimeFlagDiagnostic off = new RuntimeFlagDiagnostic(false, false, true, true);
        return new RuntimeFlagsDiagnostic(off, off,
                new RuntimeFlagDiagnostic(false, false, false, false),
                new RuntimeFlagDiagnostic(false, false, false, false));
    }

    static BenchmarkFingerprint fingerprint() {
        return new BenchmarkFingerprint("minecraft:overworld", "singleplayer", 1920, 1080,
                100, 12, 8, "FANCY", true, 144, 2);
    }
}
