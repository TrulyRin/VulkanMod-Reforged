package net.vulkanmod.render.culling;

                                                                   
public final class EntityCullingPolicy {
    private EntityCullingPolicy() { }

    public static boolean isWithinDistance(double distanceSquared, boolean miscellaneous, boolean important) {
        if (important || !Double.isFinite(distanceSquared)) {
            return true;
        }
        double limit = miscellaneous ? 128.0 : 96.0;
        return distanceSquared <= limit * limit;
    }

    public static boolean isSectionVisible(boolean originMatches, short sectionFrame, short currentFrame) {
        return !originMatches || sectionFrame == currentFrame;
    }
}
