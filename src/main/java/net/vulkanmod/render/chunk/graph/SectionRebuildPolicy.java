package net.vulkanmod.render.chunk.graph;


public final class SectionRebuildPolicy {
    private static final double NEARBY_DISTANCE_SQUARED=768.0;

    public static boolean isUrgent(boolean mainThreadHint,int originX,int originY,int originZ,
                                   int cameraX,int cameraY,int cameraZ) {
        if(mainThreadHint) return true;
        double dx=(double)originX+8.0-cameraX;
        double dy=(double)originY+8.0-cameraY;
        double dz=(double)originZ+8.0-cameraZ;
        return dx*dx+dy*dy+dz*dz<NEARBY_DISTANCE_SQUARED;
    }
    private SectionRebuildPolicy() {}
}
