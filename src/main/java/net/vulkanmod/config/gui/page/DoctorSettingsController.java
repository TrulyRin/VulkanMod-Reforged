package net.vulkanmod.config.gui.page;

import net.vulkanmod.compat.doctor.DoctorAction;
import net.vulkanmod.compat.doctor.DoctorSettingsSnapshot;

public interface DoctorSettingsController {
    DoctorSettingsSnapshot settingsSnapshot();

    DoctorStageResult stage(DoctorAction.StageSettings action);

    boolean hasPendingSettings();

    int pendingSettingsCount();

    long contextToken();

    void focusApplyControl();
}
