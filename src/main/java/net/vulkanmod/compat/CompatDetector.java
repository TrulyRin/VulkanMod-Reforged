package net.vulkanmod.compat;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CompatDetector {
    private static final ConcurrentHashMap<String, Boolean> loadedCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> versionCache = new ConcurrentHashMap<>();

    public static boolean isModLoaded(String modId) {
        if (modId == null) return false;
        String key = modId.toLowerCase();
        return loadedCache.computeIfAbsent(key, CompatDetector::checkModLoadedReflective);
    }

    private static boolean checkModLoadedReflective(String modId) {
        try {

            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (Throwable t) {

            return false;
        }
    }

    public static String getModVersion(String modId) {
        if (modId == null) return "UNKNOWN";
        String key = modId.toLowerCase();
        return versionCache.computeIfAbsent(key, CompatDetector::getModVersionReflective);
    }

    private static String getModVersionReflective(String modId) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Optional<?> modContainerOpt = (Optional<?>) modListClass.getMethod("getModContainerById", String.class).invoke(modList, modId);
            if (modContainerOpt.isPresent()) {
                Object modContainer = modContainerOpt.get();
                Object modInfo = modContainer.getClass().getMethod("getModInfo").invoke(modContainer);
                Object versionObj = modInfo.getClass().getMethod("getVersion").invoke(modInfo);
                return versionObj.toString();
            }
        } catch (Throwable t) {

        }
        return "UNKNOWN";
    }
}
