package net.vulkanmod.compat.diagnostics;

import net.vulkanmod.diagnostics.benchmark.BenchmarkFingerprint;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CompatDiagnosticsSnapshot(
        Instant capturedAt,
        DiagnosticsSeverity severity,
        SystemDiagnostic system,
        RuntimeFlagsDiagnostic runtimeFlags,
        BenchmarkFingerprint currentSettings,
        long worldSessionId,
        List<ModDiagnostic> mods,
        List<RenderPathDiagnostic> renderPaths,
        List<SessionIssueDiagnostic> sessionIssues,
        Optional<BenchmarkResult> latestBenchmark) {

    public CompatDiagnosticsSnapshot {
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
        severity = Objects.requireNonNull(severity, "severity");
        system = Objects.requireNonNull(system, "system");
        runtimeFlags = Objects.requireNonNull(runtimeFlags, "runtimeFlags");
        currentSettings = Objects.requireNonNull(currentSettings, "currentSettings");
        if (worldSessionId < 0L) {
            throw new IllegalArgumentException("worldSessionId must not be negative");
        }
        mods = List.copyOf(mods);
        renderPaths = List.copyOf(renderPaths);
        sessionIssues = List.copyOf(sessionIssues);
        latestBenchmark = Objects.requireNonNull(latestBenchmark, "latestBenchmark");
    }

    public CompatDiagnosticsSnapshot(
            Instant capturedAt,
            DiagnosticsSeverity severity,
            SystemDiagnostic system,
            RuntimeFlagsDiagnostic runtimeFlags,
            BenchmarkFingerprint currentSettings,
            List<ModDiagnostic> mods,
            List<RenderPathDiagnostic> renderPaths,
            List<SessionIssueDiagnostic> sessionIssues,
            Optional<BenchmarkResult> latestBenchmark) {
        this(capturedAt, severity, system, runtimeFlags, currentSettings, 0L,
                mods, renderPaths, sessionIssues, latestBenchmark);
    }

    public boolean benchmarkIsStale(BenchmarkResult result) {
        Objects.requireNonNull(result, "result");
        boolean provenanceKnown = worldSessionId > 0L || result.worldSessionId() > 0L;
        return result.isStale(currentSettings)
                || (provenanceKnown && result.worldSessionId() != worldSessionId);
    }

    public long count(DiagnosticsSeverity requestedSeverity) {
        return mods.stream().filter(mod -> mod.severity() == requestedSeverity).count();
    }
}
