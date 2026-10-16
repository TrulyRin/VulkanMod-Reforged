package net.vulkanmod.compat.diagnostics;

import java.util.Objects;

public record SessionIssueDiagnostic(
        SessionIssueType type,
        String key,
        DiagnosticsSeverity severity,
        int omittedCount) {

    public SessionIssueDiagnostic {
        type = Objects.requireNonNull(type, "type");
        key = Objects.requireNonNull(key, "key");
        severity = Objects.requireNonNull(severity, "severity");
        if (omittedCount < 0) {
            throw new IllegalArgumentException("omittedCount must not be negative");
        }
    }

    public SessionIssueDiagnostic(SessionIssueType type, String key, DiagnosticsSeverity severity) {
        this(type, key, severity, 0);
    }

    public boolean truncated() {
        return omittedCount > 0;
    }
}
