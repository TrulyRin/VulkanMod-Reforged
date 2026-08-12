package net.vulkanmod.compat.diagnostics;

import java.util.Objects;

public record RuntimeFlagsDiagnostic(
        RuntimeFlagDiagnostic diagnostics,
        RuntimeFlagDiagnostic hudTrace,
        RuntimeFlagDiagnostic profilingMixins,
        RuntimeFlagDiagnostic debugMixins) {

    public RuntimeFlagsDiagnostic {
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        hudTrace = Objects.requireNonNull(hudTrace, "hudTrace");
        profilingMixins = Objects.requireNonNull(profilingMixins, "profilingMixins");
        debugMixins = Objects.requireNonNull(debugMixins, "debugMixins");
    }
}
