package net.vulkanmod.compat.doctor;

public sealed interface DoctorValue permits DoctorValue.BooleanValue, DoctorValue.IntegerValue {
    Object rawValue();

    record BooleanValue(boolean value) implements DoctorValue {
        @Override
        public Boolean rawValue() {
            return value;
        }
    }

    record IntegerValue(int value) implements DoctorValue {
        @Override
        public Integer rawValue() {
            return value;
        }
    }

    static DoctorValue of(boolean value) {
        return new BooleanValue(value);
    }

    static DoctorValue of(int value) {
        return new IntegerValue(value);
    }
}
