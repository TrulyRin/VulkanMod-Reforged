package net.vulkanmod.compat.sable;

import java.util.Locale;

public final class SableCompatibilityOptions {
    public static final String MODE_PROPERTY = "vulkanmod.compat.sable";
    public static final String DEBUG_PROPERTY = "vulkanmod.compat.sable.debug";

    private SableCompatibilityOptions() {
    }

    public static Mode mode() {
        return Mode.parse(System.getProperty(MODE_PROPERTY));
    }

    public static boolean debugEnabled() {
        return Boolean.getBoolean(DEBUG_PROPERTY);
    }

    public enum Mode {
        AUTO,
        OFF;

        public boolean enabled() {
            return this == AUTO;
        }

        public static Mode parse(String value) {
            if (value == null) {
                return AUTO;
            }
            return "off".equals(value.trim().toLowerCase(Locale.ROOT)) ? OFF : AUTO;
        }
    }
}
