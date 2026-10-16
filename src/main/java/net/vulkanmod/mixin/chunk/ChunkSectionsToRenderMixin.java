package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.vulkanmod.interfaces.CustomChunkDraws;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSectionsToRender.class)
public class ChunkSectionsToRenderMixin implements CustomChunkDraws {
    @Unique private WorldRenderer.ChunkDrawContext vulkanmod$context;

    @Override
    public void vulkanmod$setChunkDrawContext(WorldRenderer.ChunkDrawContext context) {
        this.vulkanmod$context = context;
    }

    @Inject(method = "renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
            at = @At("HEAD"), cancellable = true)
    private void renderCustomGroup(ChunkSectionLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        if (this.vulkanmod$context != null) {
            WorldRenderer.getInstance().renderGroup(group, sampler, this.vulkanmod$context);
            ci.cancel();
        }
    }
}
