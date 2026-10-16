package net.vulkanmod.compat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;

public final class CompatMods {
    public static final String[] LIBRARY_MOD_IDS = loadLines("/assets/vulkanmod/compat/library_mod_ids.txt");
    public static final String[] GAMEPLAY_UI_MOD_IDS = loadLines("/assets/vulkanmod/compat/gameplay_ui_mod_ids.txt");
    public static final String[] RENDERER_GL_MOD_IDS = loadLines("/assets/vulkanmod/compat/renderer_gl_incompatible_mod_ids.txt");
    public static final String[] EXTRA_REPORT_MOD_IDS = loadLines("/assets/vulkanmod/compat/report_mod_ids.txt");
    public static final String[] REPORT_MOD_IDS = mergeUnique(
            LIBRARY_MOD_IDS,
            GAMEPLAY_UI_MOD_IDS,
            RENDERER_GL_MOD_IDS,
            EXTRA_REPORT_MOD_IDS
    );

    private CompatMods() {
    }

    static String[] loadLines(String resourcePath) {
        try (var input = CompatMods.class.getResourceAsStream(resourcePath)) {
            if (input == null) {
                return new String[0];
            }

            try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .toArray(String[]::new);
            }
        } catch (IOException ignored) {
            return new String[0];
        }
    }

    public static boolean contains(String[] modIds, String modId) {
        if (modId == null) {
            return false;
        }

        for (String entry : modIds) {
            if (entry.equalsIgnoreCase(modId)) {
                return true;
            }
        }
        return false;
    }

    private static String[] mergeUnique(String[]... groups) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (String[] group : groups) {
            for (String id : group) {
                ids.add(id.toLowerCase());
            }
        }
        return ids.toArray(String[]::new);
    }
}
