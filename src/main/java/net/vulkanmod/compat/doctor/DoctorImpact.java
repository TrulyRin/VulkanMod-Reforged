package net.vulkanmod.compat.doctor;

import java.util.Objects;

public record DoctorImpact(
        ImpactRating performance,
        ImpactRating visualQuality,
        boolean restartRequired) {

    public DoctorImpact {
        performance = Objects.requireNonNull(performance, "performance");
        visualQuality = Objects.requireNonNull(visualQuality, "visualQuality");
    }

    public static DoctorImpact none() {
        return new DoctorImpact(ImpactRating.NONE, ImpactRating.NONE, false);
    }
}
