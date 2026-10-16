package net.vulkanmod.mixin.chunk;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
import net.vulkanmod.render.chunk.ChunkStatusMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerM {
    @Inject(method = "applyLightData(IILnet/minecraft/network/protocol/game/ClientboundLightUpdatePacketData;Z)V", at = @At("RETURN"))
    private void markLightReady(int x, int z, ClientboundLightUpdatePacketData light, boolean trustEdges, CallbackInfo ci) {
        if (ChunkStatusMap.INSTANCE != null) ChunkStatusMap.INSTANCE.setChunkStatus(x, z, ChunkStatusMap.LIGHT_READY);
    }

    @Inject(method = "handleForgetLevelChunk(Lnet/minecraft/network/protocol/game/ClientboundForgetLevelChunkPacket;)V", at = @At("RETURN"))
    private void dropChunkLight(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        if (ChunkStatusMap.INSTANCE != null) ChunkStatusMap.INSTANCE.resetChunkStatus(packet.pos().x, packet.pos().z, ChunkStatusMap.LIGHT_READY);
    }
}
