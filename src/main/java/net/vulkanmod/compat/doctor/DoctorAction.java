package net.vulkanmod.compat.doctor;

import java.util.List;
import java.util.Objects;

public sealed interface DoctorAction permits DoctorAction.StageSettings, DoctorAction.Guidance, DoctorAction.CopyReport {
    record StageSettings(String id, List<DoctorSettingChange> changes) implements DoctorAction {
        public StageSettings {
            id = Objects.requireNonNull(id, "id");
            changes = List.copyOf(changes);
            if (id.isBlank() || changes.isEmpty()) {
                throw new IllegalArgumentException("A staged Doctor action needs an id and at least one change");
            }
        }
    }

    record Guidance(DoctorGuidance kind) implements DoctorAction {
        public Guidance {
            kind = Objects.requireNonNull(kind, "kind");
        }
    }

    record CopyReport() implements DoctorAction {
    }
}
