package net.vulkanmod.compat.path;

import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.CompatPolicyManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class RenderPathOwnership {
    private static volatile PathState[] states = defaultStates();

    public static synchronized void assignOwnership(RenderPath path, String ownerModId, CompatMode mode) {
        String normalizedOwner = ownerModId.toLowerCase(Locale.ROOT);
        int idx = path.ordinal();
        PathState[] updated = states.clone();

        if (CompatPolicyManager.isCoexistenceIncompatible()) {
            if (path == RenderPath.GUI || path == RenderPath.TEXT || path == RenderPath.ITEMS || path == RenderPath.HUD_CACHE) {
                updated[idx] = new PathState(path, normalizedOwner, CompatMode.SAFE);
                states = updated;
                return;
            }
        }

        updated[idx] = new PathState(path, normalizedOwner, mode);
        states = updated;
    }

    public static synchronized void releaseOwnership(RenderPath path, String ownerModId) {
        String normalizedOwner = ownerModId.toLowerCase(Locale.ROOT);
        int idx = path.ordinal();
        PathState[] current = states;

        if (!current[idx].owner().equals(normalizedOwner)) {
            return;
        }

        PathState[] updated = current.clone();
        updated[idx] = new PathState(path, "minecraft", CompatMode.OFF);
        states = updated;
    }

    public static synchronized void reset() {
        states = defaultStates();
    }

    public static CompatMode getPathMode(RenderPath path) {
        return states[path.ordinal()].mode();
    }

    public static String getPathOwner(RenderPath path) {
        return states[path.ordinal()].owner();
    }

    public static boolean isPathActive(RenderPath path) {
        return states[path.ordinal()].mode() != CompatMode.OFF;
    }

    public static List<PathState> snapshot() {
        PathState[] captured = states;
        return List.copyOf(Arrays.asList(captured.clone()));
    }

    private static PathState[] defaultStates() {
        RenderPath[] paths = RenderPath.values();
        PathState[] defaults = new PathState[paths.length];
        for (RenderPath path : paths) {
            defaults[path.ordinal()] = new PathState(path, "minecraft", CompatMode.OFF);
        }
        return defaults;
    }

    public record PathState(RenderPath path, String owner, CompatMode mode) {
    }
}
