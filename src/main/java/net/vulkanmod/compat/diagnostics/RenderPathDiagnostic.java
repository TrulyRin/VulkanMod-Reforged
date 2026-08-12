package net.vulkanmod.compat.diagnostics;

import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.path.RenderPath;

import java.util.Objects;

public record RenderPathDiagnostic(
        RenderPath path,
        String owner,
        CompatMode mode,
        boolean active,
        DiagnosticsSeverity severity) {

    public RenderPathDiagnostic {
        path = Objects.requireNonNull(path, "path");
        owner = Objects.requireNonNull(owner, "owner");
        mode = Objects.requireNonNull(mode, "mode");
        severity = Objects.requireNonNull(severity, "severity");
    }
}
