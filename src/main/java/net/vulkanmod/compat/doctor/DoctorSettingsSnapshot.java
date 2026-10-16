package net.vulkanmod.compat.doctor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record DoctorSettingsSnapshot(
        Map<DoctorSetting, DoctorValue> values,
        Set<DoctorSetting> pendingSettings) {

    public DoctorSettingsSnapshot {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(pendingSettings, "pendingSettings");
        EnumMap<DoctorSetting, DoctorValue> copied = new EnumMap<>(DoctorSetting.class);
        copied.putAll(values);
        values = Map.copyOf(copied);
        pendingSettings = Set.copyOf(pendingSettings);
        if (!values.keySet().containsAll(pendingSettings)) {
            throw new IllegalArgumentException("Pending Doctor settings must also be available");
        }
    }

    public Optional<DoctorValue> value(DoctorSetting setting) {
        return Optional.ofNullable(values.get(setting));
    }

    public boolean isAvailable(DoctorSetting setting) {
        return values.containsKey(setting);
    }

    public DoctorSettingsSnapshot withChanges(List<DoctorSettingChange> changes) {
        EnumMap<DoctorSetting, DoctorValue> changed = new EnumMap<>(DoctorSetting.class);
        changed.putAll(values);
        for (DoctorSettingChange change : changes) {
            DoctorValue current = changed.get(change.setting());
            if (!change.expectedValue().equals(current)) {
                throw new IllegalStateException("Doctor settings changed after preview");
            }
            changed.put(change.setting(), change.targetValue());
        }
        return new DoctorSettingsSnapshot(changed, Set.of());
    }

    public boolean matchesExpected(Map<DoctorSetting, DoctorValue> expected) {
        return expected.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(values.get(entry.getKey())));
    }
}
