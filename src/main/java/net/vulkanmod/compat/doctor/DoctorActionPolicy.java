package net.vulkanmod.compat.doctor;

import java.util.EnumSet;

public final class DoctorActionPolicy {
    public static final String CONSERVATIVE_RENDERING_ACTION = "conservative-rendering";

    private DoctorActionPolicy() {
    }

    public static boolean isAllowed(DoctorAction.StageSettings action) {
        if (action == null
                || !CONSERVATIVE_RENDERING_ACTION.equals(action.id())
                || action.changes().isEmpty()
                || action.changes().size() > DoctorSetting.values().length) {
            return false;
        }

        EnumSet<DoctorSetting> seen = EnumSet.noneOf(DoctorSetting.class);
        for (DoctorSettingChange change : action.changes()) {
            if (!seen.add(change.setting()) || !isAllowed(change)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAllowed(DoctorSettingChange change) {
        return switch (change.setting()) {
            case INDIRECT_DRAW, UNIQUE_OPAQUE_LAYER ->
                    change.expectedValue() instanceof DoctorValue.BooleanValue
                            && change.targetValue().equals(DoctorValue.of(false));
            case RENDER_SCALE ->
                    change.expectedValue() instanceof DoctorValue.IntegerValue
                            && change.targetValue().equals(DoctorValue.of(100));
        };
    }
}
