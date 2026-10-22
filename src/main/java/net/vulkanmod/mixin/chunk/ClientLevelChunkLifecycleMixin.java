package net.vulkanmod.mixin.chunk;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.vulkanmod.render.chunk.ChunkStatusMap;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class ClientLevelChunkLifecycleMixin {

    @Inject(method = "onChunkLoaded", at = @At("HEAD"))
    private void vulkanmod$chunkDataLoaded(ChunkPos position, CallbackInfo ci) {
        WorldRenderer renderer = WorldRenderer.getInstance();
        ChunkStatusMap status = ChunkStatusMap.INSTANCE;
        if (renderer == null || status == null || WorldRenderer.getLevel() != (Object) this) return;


        ClientLevel level = (ClientLevel) (Object) this;
        if (level.getChunkSource().getChunk(position.x, position.z, ChunkStatus.FULL, false) == null) return;

        renderer.renderRegionCache.remove(position.x, position.z);
        status.setChunkStatus(position.x, position.z, ChunkStatusMap.DATA_READY);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    private void vulkanmod$chunkDataUnloaded(LevelChunk chunk, CallbackInfo ci) {
        WorldRenderer renderer = WorldRenderer.getInstance();
        ChunkStatusMap status = ChunkStatusMap.INSTANCE;
        if (renderer == null || status == null || WorldRenderer.getLevel() != (Object) this) return;

        ChunkPos position = chunk.getPos();
        renderer.renderRegionCache.remove(position.x, position.z);

        status.resetChunkStatus(position.x, position.z, ChunkStatusMap.DATA_READY);
    }
}
