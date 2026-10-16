package net.vulkanmod.compat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

                                                                                         
public final class CompatibilityGuard {
    private CompatibilityGuard() {
    }

    public static void failIfIncompatible() {
        failIfIncompatible(CompatDetector::isModLoaded);
    }

    static void failIfIncompatible(Predicate<String> isLoaded) {
        List<String> conflicts = findLoadedIncompatibleRenderers(isLoaded);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException(
                    "VulkanMod Reforged cannot start with incompatible renderer mod(s): "
                            + String.join(", ", conflicts)
                            + ". Remove them from this Minecraft 1.21.11 profile, or disable VulkanMod Reforged. "
                            + "No safe OpenGL/Vulkan coexistence path is qualified."
            );
        }
    }

    static List<String> findLoadedIncompatibleRenderers(Predicate<String> isLoaded) {
        Set<String> conflicts = new LinkedHashSet<>();
        for (String modId : CompatMods.RENDERER_GL_MOD_IDS) {
            String normalized = modId.toLowerCase(Locale.ROOT);
            if (isLoaded.test(normalized)) {
                conflicts.add(normalized);
            }
        }
        return List.copyOf(new ArrayList<>(conflicts));
    }
}
