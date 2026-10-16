package net.vulkanmod.compat.doctor;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record DoctorAssessment(
        Instant diagnosticsCapturedAt,
        List<DoctorRecommendation> recommendations) {

    public DoctorAssessment {
        diagnosticsCapturedAt = Objects.requireNonNull(diagnosticsCapturedAt, "diagnosticsCapturedAt");
        recommendations = List.copyOf(recommendations);
    }
}
