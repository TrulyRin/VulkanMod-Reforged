package net.vulkanmod.compat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class CompatPolicyManager {
    private static final Map<String, CompatMode> manualOverrides = new HashMap<>();
    private static final Map<String, String> verifiedModVersions = new HashMap<>();
    private static Boolean coexistenceIncompatible = null;

    static {
        verifiedModVersions.putAll(loadVerifiedVersionPrefixes());
    }

    public static void setManualOverride(String modId, CompatMode mode) {
        manualOverrides.put(modId.toLowerCase(), mode);
    }

    public static CompatMode getCompatMode(String modId) {
        if (modId == null || modId.isBlank()) {
            return CompatMode.OFF;
        }

        String normalizedId = modId.toLowerCase(Locale.ROOT);

        CompatCategory category = getCompatCategory(normalizedId);
        if (category == CompatCategory.RENDERER_GL) {
            return CompatMode.INCOMPATIBLE;
        }

        if (manualOverrides.containsKey(normalizedId)) {
            return manualOverrides.get(normalizedId);
        }

        if (!CompatDetector.isModLoaded(normalizedId)) {
            return CompatMode.OFF;
        }

        String activeVersion = CompatDetector.getModVersion(normalizedId);
        String expectedVersion = verifiedModVersions.get(normalizedId);

        if (expectedVersion != null && activeVersion.startsWith(expectedVersion)) {

            return CompatMode.SAFE;
        }

        return CompatMode.SAFE;
    }

    public static CompatCategory getCompatCategory(String modId) {
        String normalizedId = modId.toLowerCase();

        if (CompatMods.contains(CompatMods.RENDERER_GL_MOD_IDS, normalizedId)) {
            return CompatCategory.RENDERER_GL;
        }

        if (CompatMods.contains(CompatMods.LIBRARY_MOD_IDS, normalizedId)) {
            return CompatCategory.LIBRARY;
        }

        if (CompatMods.contains(CompatMods.GAMEPLAY_UI_MOD_IDS, normalizedId)) {
            return CompatCategory.GAMEPLAY_UI;
        }

        return CompatCategory.UNKNOWN;
    }

    public static Optional<String> getVerifiedVersionPrefix(String modId) {
        if (modId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(verifiedModVersions.get(modId.toLowerCase(java.util.Locale.ROOT)));
    }

    public static boolean isCoexistenceIncompatible() {
        if (coexistenceIncompatible == null) {
            coexistenceIncompatible = false;
            for (String modId : CompatMods.RENDERER_GL_MOD_IDS) {
                if (CompatDetector.isModLoaded(modId)) {
                    coexistenceIncompatible = true;
                    break;
                }
            }

            for (String modId : CompatMods.loadLines("/assets/vulkanmod/compat/coexistence_incompatible_mod_ids.txt")) {
                if (CompatDetector.isModLoaded(modId)) {
                    coexistenceIncompatible = true;
                    break;
                }
            }
        }
        return coexistenceIncompatible;
    }

    private static Map<String, String> loadVerifiedVersionPrefixes() {
        Properties properties = new Properties();
        try (var input = CompatPolicyManager.class.getResourceAsStream("/assets/vulkanmod/compat/verified_mod_versions.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException ignored) {
            return Map.of();
        }

        Map<String, String> versions = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            versions.put(name.toLowerCase(), properties.getProperty(name));
        }
        return versions;
    }
}
