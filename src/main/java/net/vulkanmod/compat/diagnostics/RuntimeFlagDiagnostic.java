package net.vulkanmod.compat.diagnostics;

public record RuntimeFlagDiagnostic(
        boolean requested,
        boolean effective,
        boolean available,
        boolean restartRequired) {
}
