package net.vulkanmod.compat.path;

import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.CompatPolicyManager;

public class RenderPathOwnership {
    private static final CompatMode[] pathModes = new CompatMode[RenderPath.values().length];
    private static final String[] pathOwners = new String[RenderPath.values().length];

    static {

        for (RenderPath path : RenderPath.values()) {
            pathModes[path.ordinal()] = CompatMode.OFF;
            pathOwners[path.ordinal()] = "minecraft";
        }
    }

    public static void assignOwnership(RenderPath path, String ownerModId, CompatMode mode) {
        String normalizedOwner = ownerModId.toLowerCase();
        int idx = path.ordinal();

        if (CompatPolicyManager.isCoexistenceIncompatible()) {
            if (path == RenderPath.GUI || path == RenderPath.TEXT || path == RenderPath.ITEMS || path == RenderPath.HUD_CACHE) {
                pathModes[idx] = CompatMode.SAFE;
                pathOwners[idx] = normalizedOwner;
                return;
            }
        }

        pathModes[idx] = mode;
        pathOwners[idx] = normalizedOwner;
    }

    public static void releaseOwnership(RenderPath path, String ownerModId) {
        String normalizedOwner = ownerModId.toLowerCase();
        int idx = path.ordinal();

        if (!pathOwners[idx].equals(normalizedOwner)) {
            return;
        }

        pathModes[idx] = CompatMode.OFF;
        pathOwners[idx] = "minecraft";
    }

    public static void reset() {
        for (RenderPath path : RenderPath.values()) {
            pathModes[path.ordinal()] = CompatMode.OFF;
            pathOwners[path.ordinal()] = "minecraft";
        }
    }

    public static CompatMode getPathMode(RenderPath path) {
        return pathModes[path.ordinal()];
    }

    public static String getPathOwner(RenderPath path) {
        return pathOwners[path.ordinal()];
    }

    public static boolean isPathActive(RenderPath path) {
        return getPathMode(path) != CompatMode.OFF;
    }
}

