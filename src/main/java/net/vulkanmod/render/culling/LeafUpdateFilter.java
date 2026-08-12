package net.vulkanmod.render.culling;

import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LeafUpdateFilter {

    private LeafUpdateFilter() {
    }

    public static boolean isInvisibleLeafPropertyChange(BlockState oldState, BlockState newState) {
        if (oldState == newState
                || !(oldState.getBlock() instanceof LeavesBlock)
                || oldState.getBlock() != newState.getBlock()) {
            return false;
        }

        BlockState normalized = oldState;
        if (normalized.hasProperty(LeavesBlock.DISTANCE) && newState.hasProperty(LeavesBlock.DISTANCE)) {
            normalized = normalized.setValue(LeavesBlock.DISTANCE, newState.getValue(LeavesBlock.DISTANCE));
        }
        if (normalized.hasProperty(LeavesBlock.PERSISTENT) && newState.hasProperty(LeavesBlock.PERSISTENT)) {
            normalized = normalized.setValue(LeavesBlock.PERSISTENT, newState.getValue(LeavesBlock.PERSISTENT));
        }

        return normalized == newState;
    }
}
