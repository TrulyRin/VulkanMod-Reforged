package net.vulkanmod.compat.doctor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorActionPolicyTest {
    @Test
    void acceptsOnlyTheClosedTroubleshootingAllowlist() {
        DoctorAction.StageSettings allowed = new DoctorAction.StageSettings(
                DoctorActionPolicy.CONSERVATIVE_RENDERING_ACTION,
                List.of(
                        change(DoctorSetting.INDIRECT_DRAW, true, false),
                        change(DoctorSetting.UNIQUE_OPAQUE_LAYER, true, false),
                        new DoctorSettingChange(DoctorSetting.RENDER_SCALE,
                                DoctorValue.of(75), DoctorValue.of(100))));

        assertTrue(DoctorActionPolicy.isAllowed(allowed));
        assertFalse(DoctorActionPolicy.isAllowed(new DoctorAction.StageSettings(
                "another-action", allowed.changes())));
        assertFalse(DoctorActionPolicy.isAllowed(new DoctorAction.StageSettings(
                DoctorActionPolicy.CONSERVATIVE_RENDERING_ACTION,
                List.of(change(DoctorSetting.INDIRECT_DRAW, false, true)))));
        assertFalse(DoctorActionPolicy.isAllowed(new DoctorAction.StageSettings(
                DoctorActionPolicy.CONSERVATIVE_RENDERING_ACTION,
                List.of(new DoctorSettingChange(DoctorSetting.RENDER_SCALE,
                        DoctorValue.of(100), DoctorValue.of(75))))));
        assertFalse(DoctorActionPolicy.isAllowed(new DoctorAction.StageSettings(
                DoctorActionPolicy.CONSERVATIVE_RENDERING_ACTION,
                List.of(change(DoctorSetting.INDIRECT_DRAW, true, false),
                        change(DoctorSetting.INDIRECT_DRAW, true, false)))));
    }

    private static DoctorSettingChange change(DoctorSetting setting, boolean before, boolean after) {
        return new DoctorSettingChange(setting, DoctorValue.of(before), DoctorValue.of(after));
    }
}
