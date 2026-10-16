package net.vulkanmod.compat.diagnostics;

public enum DiagnosticsSeverity {
    HEALTHY,
    NOTICE,
    WARNING,
    INCOMPATIBLE;

    public static DiagnosticsSeverity max(DiagnosticsSeverity first, DiagnosticsSeverity second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
