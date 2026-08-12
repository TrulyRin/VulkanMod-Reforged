package net.vulkanmod.compat;

import net.vulkanmod.compat.diagnostics.CompatDiagnosticsService;
import net.vulkanmod.compat.diagnostics.CompatDiagnosticsSnapshot;
import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.compat.path.RenderPathOwnership;
import net.vulkanmod.diagnostics.benchmark.BenchmarkFingerprint;
import net.vulkanmod.diagnostics.benchmark.BenchmarkResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public final class CompatReport {
    private CompatReport() {
    }

    public static String generateReportString() {
        return format(CompatDiagnosticsService.capture());
    }

    public static String format(CompatDiagnosticsSnapshot snapshot) {
        StringBuilder report = new StringBuilder(4096);
        report.append("=== Vulkan Reforged Diagnostics Report ===\n");
        report.append("Captured (UTC): ").append(snapshot.capturedAt()).append('\n');
        report.append("Overall status: ").append(snapshot.severity()).append("\n\n");

        var system = snapshot.system();
        report.append("--- Renderer and System ---\n");
        append(report, "VulkanMod", system.vulkanModVersion());
        append(report, "Minecraft", system.minecraftVersion());
        append(report, "NeoForge", system.neoForgeVersion());
        append(report, "GPU", system.gpu());
        append(report, "GPU vendor", system.gpuVendor());
        append(report, "Driver", system.driverVersion());
        append(report, "Vulkan", system.vulkanVersion());
        append(report, "CPU", system.cpu());
        append(report, "Java", system.javaVersion());
        append(report, "OS", system.operatingSystem() + " / " + system.architecture());
        append(report, "GL compatibility", system.glCompatibilityVersion());
        append(report, "Registered GL functions", system.registeredGlFunctions());

        report.append("\n--- Applied Settings ---\n");
        appendFingerprint(report, snapshot.currentSettings());

        var flags = snapshot.runtimeFlags();
        report.append("\n--- Runtime Diagnostics ---\n");
        appendFlag(report, "Diagnostics", flags.diagnostics());
        appendFlag(report, "HUD trace", flags.hudTrace());
        appendFlag(report, "Profiling mixins", flags.profilingMixins());
        appendFlag(report, "Debug mixins", flags.debugMixins());

        report.append("\n--- Installed External Mods ---\n");
        if (snapshot.mods().isEmpty()) {
            report.append("(none)\n");
        } else {
            for (var mod : snapshot.mods()) {
                report.append(" * ").append(mod.displayName()).append(" [").append(mod.id()).append(']')
                        .append(" | version=").append(mod.version())
                        .append(mod.expectedVersionPrefix().isEmpty()
                                ? ""
                                : " | verified-prefix=" + mod.expectedVersionPrefix())
                        .append(" | category=").append(mod.category())
                        .append(" | policy=").append(mod.policyMode())
                        .append(" | verification=").append(mod.versionStatus())
                        .append(" | status=").append(mod.severity())
                        .append('\n');
            }
        }

        report.append("\n--- Render Paths ---\n");
        for (var path : snapshot.renderPaths()) {
            report.append(" * ").append(path.path())
                    .append(" | owner=").append(path.owner())
                    .append(" | mode=").append(path.mode())
                    .append(" | active=").append(path.active())
                    .append('\n');
        }

        report.append("\n--- Session Warnings and Fallbacks ---\n");
        if (snapshot.sessionIssues().isEmpty()) {
            report.append("(none)\n");
        } else {
            for (var issue : snapshot.sessionIssues()) {
                report.append(" * ").append(issue.type())
                        .append(issue.truncated()
                                ? " | omitted=" + issue.omittedCount()
                                : " | key=" + issue.key())
                        .append(" | status=").append(issue.severity())
                        .append('\n');
            }
        }

        report.append("\n--- Latest Current-Settings Benchmark ---\n");
        snapshot.latestBenchmark().ifPresentOrElse(
                result -> appendBenchmark(report, result, snapshot.benchmarkIsStale(result)),
                () -> report.append("(not run this session)\n"));
        report.append("=== End of Report ===");
        return report.toString();
    }

    public static void logReport() {
        logger().info("\n{}", generateReportString());
    }

    public static String generateRuntimeHintString() {
        if (RuntimeOptions.hudTraceEnabled()) {
            return "";
        }

        StringBuilder activePaths = new StringBuilder();
        for (RenderPath path : RenderPath.values()) {
            if (!RenderPathOwnership.isPathActive(path)) {
                continue;
            }

            if (!activePaths.isEmpty()) {
                activePaths.append(", ");
            }
            activePaths.append(path);
        }

        if (activePaths.isEmpty()) {
            return "";
        }

        if (RuntimeOptions.diagnosticsEnabled()) {
            return "Compatibility render paths are active (" + activePaths
                    + "), but HUD trace is disabled. Add JVM arg -D"
                    + RuntimeOptions.HUD_TRACE_PROPERTY + "=true to capture HUD draw state.";
        }

        return "Compatibility render paths are active (" + activePaths
                + "), but diagnostics are disabled. Add JVM args -D"
                + RuntimeOptions.DIAGNOSTICS_PROPERTY + "=true -D"
                + RuntimeOptions.HUD_TRACE_PROPERTY + "=true to capture HUD draw state.";
    }

    public static void logRuntimeHints() {
        String hint = generateRuntimeHintString();
        if (!hint.isEmpty()) {
            logger().info(hint);
        }
    }

    private static void appendFingerprint(StringBuilder report, BenchmarkFingerprint fingerprint) {
        append(report, "Context", fingerprint.playContext() + " / " + fingerprint.dimensionContext());
        append(report, "Framebuffer", fingerprint.framebufferWidth() + "x" + fingerprint.framebufferHeight());
        append(report, "Render scale", fingerprint.renderScale() + "%");
        append(report, "Render distance", fingerprint.renderDistance());
        append(report, "Simulation distance", fingerprint.simulationDistance());
        append(report, "Graphics mode", fingerprint.graphicsMode());
        append(report, "VSync", fingerprint.vsync());
        append(report, "FPS cap", fingerprint.fpsCap());
        append(report, "Performance preset", fingerprint.performancePreset());
        append(report, "Indirect draw", fingerprint.indirectDraw());
        append(report, "Unique opaque layer", fingerprint.uniqueOpaqueLayer());
        append(report, "Adaptive chunk uploads", fingerprint.adaptiveChunkUploads());
        append(report, "Chunk uploads per frame", fingerprint.chunkUploadsPerFrame());
    }

    private static void appendBenchmark(StringBuilder report, BenchmarkResult result,
                                        boolean settingsChanged) {
        append(report, "Completed (UTC)", result.completedAt());
        append(report, "Context or settings changed since run", settingsChanged);
        append(report, "Average FPS", decimal(result.averageFps()));
        append(report, "1% low FPS", decimal(result.onePercentLowFps()));
        append(report, "Average frame time (ms)", decimal(result.averageFrameTimeMs()));
        append(report, "p95 frame time (ms)", decimal(result.p95FrameTimeMs()));
        append(report, "Average CPU render time (ms)", decimal(result.averageCpuRenderTimeMs()));
        append(report, "Frames over 50 ms", result.framesOver50Ms());
        append(report, "Duration (seconds)", decimal(result.durationSeconds()));
        append(report, "Frames", result.frameCount());
    }

    private static void appendFlag(StringBuilder report, String label,
                                   net.vulkanmod.compat.diagnostics.RuntimeFlagDiagnostic flag) {
        report.append(label)
                .append(": requested=").append(flag.requested())
                .append(" | effective=").append(flag.effective())
                .append(" | available=").append(flag.available())
                .append(" | restart-required=").append(flag.restartRequired())
                .append('\n');
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void append(StringBuilder report, String label, Object value) {
        report.append(label).append(": ").append(value).append('\n');
    }

    private static Logger logger() {
        return LogManager.getLogger("VulkanMod-Compat");
    }
}
