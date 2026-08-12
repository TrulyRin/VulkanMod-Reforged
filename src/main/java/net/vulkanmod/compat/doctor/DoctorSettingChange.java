package net.vulkanmod.compat.doctor;

import java.util.Objects;

public record DoctorSettingChange(
        DoctorSetting setting,
        DoctorValue expectedValue,
        DoctorValue targetValue) {

    public DoctorSettingChange {
        setting = Objects.requireNonNull(setting, "setting");
        expectedValue = Objects.requireNonNull(expectedValue, "expectedValue");
        targetValue = Objects.requireNonNull(targetValue, "targetValue");
        if (expectedValue.getClass() != targetValue.getClass()) {
            throw new IllegalArgumentException("Doctor setting values must have the same type");
        }
        if (expectedValue.equals(targetValue)) {
            throw new IllegalArgumentException("Doctor setting change must not be a no-op");
        }
    }
}
