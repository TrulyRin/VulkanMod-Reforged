package net.vulkanmod.render.culling;

                                                                              
public final class ParticleCullingPolicy {
    private ParticleCullingPolicy() { }

    public static boolean shouldCull(int mode, double dx, double dy, double dz,
                                     float lookX, float lookY, float lookZ, int sameTypeCount) {
        if (mode <= 0) {
            return false;
        }
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (!Double.isFinite(distanceSquared)) {
            return false;
        }
        double maxDistance;
        double protectedDistance;
        double minDot;
        int maxCount;
        switch (mode) {
            case 1 -> { maxDistance = 48; protectedDistance = 10; minDot = 0.5; maxCount = 128; }
            case 2 -> { maxDistance = 32; protectedDistance = 8; minDot = 0.707; maxCount = 64; }
            default -> { maxDistance = 16; protectedDistance = 5; minDot = 0.866; maxCount = 24; }
        }
        if (distanceSquared <= protectedDistance * protectedDistance) {
            return false;
        }
        if (distanceSquared > maxDistance * maxDistance) {
            return true;
        }
        double dot = (dx * lookX + dy * lookY + dz * lookZ) / Math.sqrt(distanceSquared);
        return dot < minDot || sameTypeCount >= maxCount;
    }
}
