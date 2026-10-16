package net.vulkanmod.compat.diagnostics;

import net.vulkanmod.compat.CompatCategory;
import net.vulkanmod.compat.CompatMode;

import java.util.Objects;

public record ModDiagnostic(
        String id,
        String displayName,
        String version,
        String expectedVersionPrefix,
        CompatCategory category,
        CompatMode policyMode,
        VersionStatus versionStatus,
        DiagnosticsSeverity severity,
        boolean compatibilityRelevant) {

    public ModDiagnostic {
        id = Objects.requireNonNull(id, "id");
        displayName = Objects.requireNonNull(displayName, "displayName");
        version = Objects.requireNonNull(version, "version");
        expectedVersionPrefix = Objects.requireNonNull(expectedVersionPrefix, "expectedVersionPrefix");
        category = Objects.requireNonNull(category, "category");
        policyMode = Objects.requireNonNull(policyMode, "policyMode");
        versionStatus = Objects.requireNonNull(versionStatus, "versionStatus");
        severity = Objects.requireNonNull(severity, "severity");
    }
}
