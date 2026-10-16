package net.vulkanmod.interfaces;

import net.vulkanmod.render.chunk.WorldRenderer;

public interface CustomChunkDraws {
    void vulkanmod$setChunkDrawContext(WorldRenderer.ChunkDrawContext context);
}
