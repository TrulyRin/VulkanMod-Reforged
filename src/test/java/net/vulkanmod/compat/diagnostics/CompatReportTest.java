package net.vulkanmod.compat.diagnostics;

import net.vulkanmod.compat.CompatCategory;
import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.CompatReport;
import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatReportTest {
    @Test
    void formatsTheHeldSnapshotDeterministicallyAndWithoutPrivateSessionData() {
        BenchmarkResult result = new BenchmarkResult(CompatDiagnosticsServiceTest.fingerprint(),
                60, 45, 16.67, 22, 10, 2, 600, 10,
                Instant.parse("2026-07-16T12:01:00Z"));
        CompatDiagnosticsSnapshot snapshot = new CompatDiagnosticsSnapshot(
                Instant.parse("2026-07-16T12:00:00Z"),
                DiagnosticsSeverity.WARNING,
                CompatDiagnosticsServiceTest.system(),
                CompatDiagnosticsServiceTest.flags(),
                CompatDiagnosticsServiceTest.fingerprint(),
                List.of(new ModDiagnostic("create", "Create", "6.0", "0.5", CompatCategory.GAMEPLAY_UI,
                        CompatMode.SAFE, VersionStatus.VERSION_MISMATCH,
                        DiagnosticsSeverity.WARNING, true)),
                List.of(new RenderPathDiagnostic(RenderPath.GUI, "create", CompatMode.SAFE,
                        true, DiagnosticsSeverity.NOTICE)),
                List.of(new SessionIssueDiagnostic(SessionIssueType.CONTRACT_GAP,
                        "draw.generic", DiagnosticsSeverity.WARNING)),
                Optional.of(result));

        String first = CompatReport.format(snapshot);
        String second = CompatReport.format(snapshot);

        assertEquals(first, second);
        assertTrue(first.contains("Captured (UTC): 2026-07-16T12:00:00Z"));
        assertTrue(first.contains("Create [create]"));
        assertTrue(first.contains("Average FPS: 60.00"));
        assertTrue(first.contains("Indirect draw: false"));
        assertTrue(first.contains("Unique opaque layer: false"));
        assertTrue(first.contains("Adaptive chunk uploads: true"));
        assertTrue(first.contains("Chunk uploads per frame: 5"));
        assertFalse(first.contains("C:\\Users\\"));
        assertFalse(first.toLowerCase().contains("server address"));
        assertFalse(first.toLowerCase().contains("uuid"));
    }
}
