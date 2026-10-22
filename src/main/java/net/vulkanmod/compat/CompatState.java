package net.vulkanmod.compat;

import java.util.Set;

public final class CompatState {
    private static final Set<String> CORE_MOD_IDS = Set.of("minecraft", "neoforge", "fml", "vulkanmod");

    private static volatile boolean externalModsPresent = true;
    private static volatile boolean detected = false;

    private CompatState() {
    }

    public static boolean compatActive() {
        ensureDetected();
        return externalModsPresent;
    }

    public static boolean soloMode() {
        return !compatActive();
    }

    private static void ensureDetected() {
        if (detected) {
            return;
        }
        synchronized (CompatState.class) {
            if (!detected) {
                externalModsPresent = computeExternalModsPresent();
                detected = true;
            }
        }
    }

    private static boolean computeExternalModsPresent() {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object mods = modListClass.getMethod("getMods").invoke(modList);
            for (Object modInfo : (Iterable<?>) mods) {
                Object idObj = modInfo.getClass().getMethod("getModId").invoke(modInfo);
                String id = String.valueOf(idObj).toLowerCase();
                if (!CORE_MOD_IDS.contains(id)) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return true;
        }
    }
}
