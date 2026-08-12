package net.vulkanmod.compat.doctor;

import net.vulkanmod.compat.diagnostics.DiagnosticsSeverity;

import java.util.List;
import java.util.Objects;

public record DoctorRecommendation(
        String id,
        DoctorRule rule,
        DiagnosticsSeverity severity,
        DoctorConfidence confidence,
        List<String> subjects,
        int omittedSubjects,
        int occurrences,
        DoctorImpact impact,
        List<DoctorAction> actions) {

    public DoctorRecommendation {
        id = Objects.requireNonNull(id, "id");
        rule = Objects.requireNonNull(rule, "rule");
        severity = Objects.requireNonNull(severity, "severity");
        confidence = Objects.requireNonNull(confidence, "confidence");
        subjects = List.copyOf(subjects);
        impact = Objects.requireNonNull(impact, "impact");
        actions = List.copyOf(actions);
        if (id.isBlank() || omittedSubjects < 0 || occurrences < 1) {
            throw new IllegalArgumentException("Invalid Doctor recommendation metadata");
        }
    }

    public java.util.Optional<DoctorAction.StageSettings> stagedAction() {
        return actions.stream()
                .filter(DoctorAction.StageSettings.class::isInstance)
                .map(DoctorAction.StageSettings.class::cast)
                .findFirst();
    }
}
