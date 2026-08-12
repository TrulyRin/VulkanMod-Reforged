package net.vulkanmod.compat.diagnostics;

import java.util.Objects;

public record SystemDiagnostic(
        String vulkanModVersion,
        String minecraftVersion,
        String neoForgeVersion,
        String gpu,
        String gpuVendor,
        String driverVersion,
        String vulkanVersion,
        String cpu,
        String javaVersion,
        String operatingSystem,
        String architecture,
        String glCompatibilityVersion,
        int registeredGlFunctions) {

    public SystemDiagnostic {
        vulkanModVersion = Objects.requireNonNull(vulkanModVersion, "vulkanModVersion");
        minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        neoForgeVersion = Objects.requireNonNull(neoForgeVersion, "neoForgeVersion");
        gpu = Objects.requireNonNull(gpu, "gpu");
        gpuVendor = Objects.requireNonNull(gpuVendor, "gpuVendor");
        driverVersion = Objects.requireNonNull(driverVersion, "driverVersion");
        vulkanVersion = Objects.requireNonNull(vulkanVersion, "vulkanVersion");
        cpu = Objects.requireNonNull(cpu, "cpu");
        javaVersion = Objects.requireNonNull(javaVersion, "javaVersion");
        operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem");
        architecture = Objects.requireNonNull(architecture, "architecture");
        glCompatibilityVersion = Objects.requireNonNull(glCompatibilityVersion, "glCompatibilityVersion");
    }
}
