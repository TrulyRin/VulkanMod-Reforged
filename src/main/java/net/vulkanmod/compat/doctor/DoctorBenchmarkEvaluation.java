package net.vulkanmod.compat.doctor;

import java.util.Objects;
import java.util.Optional;

public record DoctorBenchmarkEvaluation(
        DoctorBenchmarkStatus status,
        Optional<DoctorBenchmarkComparison> comparison) {

    public DoctorBenchmarkEvaluation {
        status = Objects.requireNonNull(status, "status");
        comparison = Objects.requireNonNull(comparison, "comparison");
        if ((status == DoctorBenchmarkStatus.READY) != comparison.isPresent()) {
            throw new IllegalArgumentException("Only a ready Doctor benchmark has a comparison");
        }
    }

    public static DoctorBenchmarkEvaluation status(DoctorBenchmarkStatus status) {
        return new DoctorBenchmarkEvaluation(status, Optional.empty());
    }

    public static DoctorBenchmarkEvaluation ready(DoctorBenchmarkComparison comparison) {
        return new DoctorBenchmarkEvaluation(DoctorBenchmarkStatus.READY, Optional.of(comparison));
    }
}
