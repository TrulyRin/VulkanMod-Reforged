package net.vulkanmod.render.chunk.graph;

final class SectionTraversalPolicy {
    private SectionTraversalPolicy() {
    }

    static boolean shouldUseDirectionalCulling(boolean smartCull, boolean spectatorInsideSolid, boolean hasSkyLight) {
        return smartCull && !spectatorInsideSolid && hasSkyLight;
    }
}
