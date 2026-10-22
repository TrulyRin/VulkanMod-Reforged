package net.vulkanmod.compat.capabilities;

public final class ExternalRenderPathOptions {
    private static final String EXTERNAL_LOD = "vulkanmod.compat.externalLod";
    private static final String EXTERNAL_LOD_DRAW = "vulkanmod.compat.externalLod.draw";
    private static final String EXTERNAL_LOD_DEBUG_DRAW = "vulkanmod.compat.externalLod.debugDraw";

    private ExternalRenderPathOptions() {
    }

    public static boolean externalLodEnabled() {
        return "on".equalsIgnoreCase(System.getProperty(EXTERNAL_LOD, "off"));
    }

    public static boolean externalLodDrawEnabled() {
        return Boolean.parseBoolean(System.getProperty(EXTERNAL_LOD_DRAW, "true"));
    }

    public static boolean externalLodDebugDrawEnabled() {
        return Boolean.parseBoolean(System.getProperty(EXTERNAL_LOD_DEBUG_DRAW, "false"));
    }
}
