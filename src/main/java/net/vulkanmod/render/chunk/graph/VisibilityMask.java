package net.vulkanmod.render.chunk.graph;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.Direction;

                                                                                          
public final class VisibilityMask {
    private static final Direction[] DIRECTIONS = Direction.values();

    private VisibilityMask() {
    }

    public static long fromVisibilitySet(VisibilitySet visibility) {
        long mask = 0;
        for (Direction from : DIRECTIONS) {
            for (Direction to : DIRECTIONS) {
                if (visibility.visibilityBetween(from, to)) mask |= 1L << ((from.ordinal() << 3) + to.ordinal());
            }
        }
        return mask;
    }
}
