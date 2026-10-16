package net.vulkanmod.gl;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class GlEmulationLog {
    private static final int DIAGNOSTIC_KEY_LIMIT = 256;
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> CONTRACT_FAMILIES = Set.of(
            "provider",
            "state_query",
            "object_lifetime",
            "texture_image",
            "framebuffer_readback",
            "shader_conversion",
            "draw_path",
            "performance");

    private static final class Log {
        static final org.apache.logging.log4j.Logger IMPL =
                org.apache.logging.log4j.LogManager.getLogger("VulkanMod-GlEmulation");
    }

    private GlEmulationLog() {
    }

    public static void warnContractGap(String family, String operation, String message, Object... args) {
        warnOnce(contractGapKey(family, operation), message, args);
    }

    private static String contractGapKey(String family, String operation) {
        if (family == null || !CONTRACT_FAMILIES.contains(family)) {
            throw new IllegalArgumentException("Unknown GL contract family: " + family);
        }
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("GL contract operation must not be blank");
        }
        return family + "." + operation;
    }

    static void warnOnce(String key, String message, Object... args) {
        if (REPORTED.contains(key) || REPORTED.size() >= DIAGNOSTIC_KEY_LIMIT || !REPORTED.add(key)) {
            return;
        }

        try {
            Log.IMPL.warn(message, args);
        } catch (Throwable ignored) {
        }
    }

    public static Set<String> reportedContractGaps() {
        return Set.copyOf(new TreeSet<>(REPORTED));
    }
}
