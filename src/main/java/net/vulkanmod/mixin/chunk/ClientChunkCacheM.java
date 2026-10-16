package net.vulkanmod.mixin.chunk;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.vulkanmod.render.chunk.ChunkStatusMap;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;

@Mixin(ClientChunkCache.class)
public class ClientChunkCacheM {
    @Inject(method = "replaceWithPacketData(IILnet/minecraft/network/FriendlyByteBuf;Ljava/util/Map;Ljava/util/function/Consumer;)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("RETURN"))
    private void markChunkDataReady(int x, int z, FriendlyByteBuf data, Map<Heightmap.Types, long[]> heightmaps,
                                   Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> entities,
                                   CallbackInfoReturnable<LevelChunk> cir) {
        if (cir.getReturnValue() != null && ChunkStatusMap.INSTANCE != null) {
            ChunkStatusMap.INSTANCE.setChunkStatus(x, z, ChunkStatusMap.DATA_READY);
        }
    }

    @Inject(method = "drop(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("RETURN"))
    private void dropChunkData(ChunkPos position, CallbackInfo ci) {
        if (ChunkStatusMap.INSTANCE != null) ChunkStatusMap.INSTANCE.resetChunkStatus(position.x, position.z, ChunkStatusMap.DATA_READY);
        if (WorldRenderer.getInstance() != null) WorldRenderer.getInstance().onChunkUnloaded(position.x, position.z);
    }
}
