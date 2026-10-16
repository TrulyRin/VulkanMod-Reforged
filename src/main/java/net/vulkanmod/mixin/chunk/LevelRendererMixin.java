package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.WorldBorderRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.interfaces.CustomChunkDraws;
import net.vulkanmod.config.PerformancePresetApplier;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.ChunkStatusMap;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.culling.LeafUpdateFilter;
import net.vulkanmod.render.optimization.ChunkFrameDiagnostics;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.SortedSet;
import java.util.function.Consumer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private RenderBuffers renderBuffers;
    @Shadow @Final private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
    @Shadow private ClientLevel level;
    @Shadow private Frustum capturedFrustum;
    @Shadow @Final private WorldBorderRenderer worldBorderRenderer;
    @Shadow public abstract boolean shouldShowEntityOutlines();
    @Unique private WorldRenderer vulkanmod$renderer;
    @Unique private Matrix4f vulkanmod$projection = new Matrix4f();
    @Unique private int vulkanmod$borderSectionX = Integer.MIN_VALUE;
    @Unique private int vulkanmod$borderSectionY = Integer.MIN_VALUE;
    @Unique private int vulkanmod$borderSectionZ = Integer.MIN_VALUE;

    @Unique
    private WorldRenderer vulkanmod$renderer() {
        if (this.vulkanmod$renderer == null) this.vulkanmod$renderer = WorldRenderer.init(this.renderBuffers);
        return this.vulkanmod$renderer;
    }

    @Inject(method = "setLevel(Lnet/minecraft/client/multiplayer/ClientLevel;)V", at = @At("RETURN"))
    private void setCustomLevel(ClientLevel level, CallbackInfo ci) {
        if (level != null || this.vulkanmod$renderer != null) vulkanmod$renderer().setLevel(level);
    }

    @Inject(method = "allChanged()V", at = @At("HEAD"), cancellable = true)
    private void deferPresetTerrainRebuild(CallbackInfo ci) {
        if (PerformancePresetApplier.deferTerrainRebuild((LevelRenderer) (Object) this)) ci.cancel();
    }

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void rebuildCustomTerrain(CallbackInfo ci) {
                                                                               
        if (PerformancePresetApplier.deferTerrainRebuild((LevelRenderer) (Object) this)) return;
        if (this.level != null) {
            WorldRenderer renderer = vulkanmod$renderer();
            if (WorldRenderer.getLevel() != this.level) renderer.setLevel(this.level);
            else renderer.allChanged();
        }
    }

    @Inject(method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
            at = @At("HEAD"))
    private void captureTypedProjection(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outlines,
                                       Camera camera, Matrix4f modelView, Matrix4f projection, Matrix4f cullingProjection,
                                       GpuBufferSlice fog, Vector4f fogColor, boolean sky, CallbackInfo ci) {
        this.vulkanmod$projection.set(projection);
    }

    @Inject(method = "cullTerrain(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void cullCustomTerrain(Camera camera, Frustum frustum, boolean spectator, CallbackInfo ci) {
        var position = camera.position();
        int x = SectionPos.posToSectionCoord(position.x());
        int y = SectionPos.posToSectionCoord(position.y());
        int z = SectionPos.posToSectionCoord(position.z());
        if (x != this.vulkanmod$borderSectionX || y != this.vulkanmod$borderSectionY || z != this.vulkanmod$borderSectionZ) {
            this.vulkanmod$borderSectionX = x;
            this.vulkanmod$borderSectionY = y;
            this.vulkanmod$borderSectionZ = z;
            this.worldBorderRenderer.invalidate();
        }
        vulkanmod$renderer().setupRenderer(camera, frustum, this.capturedFrustum != null, spectator);
        ci.cancel();
    }

    @Inject(method = "compileSections(Lnet/minecraft/client/Camera;)V", at = @At("HEAD"), cancellable = true)
    private void uploadCustomSections(Camera camera, CallbackInfo ci) {
        vulkanmod$renderer().uploadSections();
        ci.cancel();
    }

    @Inject(method = "prepareChunkRenders(Lorg/joml/Matrix4fc;DDD)Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;",
            at = @At("HEAD"), cancellable = true)
    private void prepareCustomDrawGroups(Matrix4fc modelView, double x, double y, double z,
                                         CallbackInfoReturnable<ChunkSectionsToRender> cir) {
        GpuTextureView atlas = this.minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        EnumMap<ChunkSectionLayer, java.util.List<com.mojang.blaze3d.systems.RenderPass.Draw<GpuBufferSlice[]>>> draws =
                new EnumMap<>(ChunkSectionLayer.class);
        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) draws.put(layer, new ArrayList<>());
        ChunkSectionsToRender carrier = new ChunkSectionsToRender(atlas, draws, 0, new GpuBufferSlice[0]);
        ((CustomChunkDraws) (Object) carrier).vulkanmod$setChunkDrawContext(new WorldRenderer.ChunkDrawContext(
                new Matrix4f(modelView), new Matrix4f(this.vulkanmod$projection), x, y, z, atlas));
        cir.setReturnValue(carrier);
    }

    @Inject(method = "extractVisibleBlockEntities(Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/state/LevelRenderState;Lnet/minecraft/client/renderer/culling/Frustum;)V",
            at = @At("HEAD"), cancellable = true)
    private void extractCustomBlockEntities(Camera camera, float partialTick, LevelRenderState state, Frustum frustum, CallbackInfo ci) {
        vulkanmod$renderer().extractBlockEntities(camera, partialTick, state, frustum, this.destructionProgress, shouldShowEntityOutlines());
        ci.cancel();
    }

    @Inject(method = "iterateVisibleBlockEntities(Ljava/util/function/Consumer;)V", at = @At("HEAD"), cancellable = true)
    private void iterateCustomBlockEntities(Consumer<BlockEntity> consumer, CallbackInfo ci) {
        if (this.vulkanmod$renderer != null) this.vulkanmod$renderer.getVisibleBlockEntities().forEach(consumer);
        ci.cancel();
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void dirtyCustomSection(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
        if (this.vulkanmod$renderer != null) this.vulkanmod$renderer.setSectionDirty(x, y, z, playerChanged);
        ci.cancel();
    }

    @Inject(method = "onSectionBecomingNonEmpty(J)V", at = @At("HEAD"), cancellable = true)
    private void dirtyNewlyNonEmptySection(long position, CallbackInfo ci) {
        if (this.vulkanmod$renderer != null) {
            this.vulkanmod$renderer.setSectionDirty(SectionPos.x(position), SectionPos.y(position), SectionPos.z(position), false);
        }
        ci.cancel();
    }

    @Inject(method = "blockChanged(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;I)V",
            at = @At("HEAD"), cancellable = true)
    private void skipInvisibleLeafUpdates(BlockGetter world, BlockPos pos, BlockState previous, BlockState next, int flags, CallbackInfo ci) {
        if (LeafUpdateFilter.isInvisibleLeafPropertyChange(previous, next)) {
            ChunkFrameDiagnostics.recordLeafDirtySkipped();
            ci.cancel();
        }
    }

    @Inject(method = "onChunkReadyToRender(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("HEAD"), cancellable = true)
    private void markChunkReady(ChunkPos position, CallbackInfo ci) {
        if (ChunkStatusMap.INSTANCE != null) ChunkStatusMap.INSTANCE.setChunkStatus(position.x, position.z, ChunkStatusMap.LIGHT_READY);
        if (this.vulkanmod$renderer != null) this.vulkanmod$renderer.scheduleGraphUpdate();
        ci.cancel();
    }

    @Inject(method = "needsUpdate()V", at = @At("RETURN"))
    private void scheduleCustomGraph(CallbackInfo ci) {
        if (this.vulkanmod$renderer != null) this.vulkanmod$renderer.scheduleGraphUpdate();
    }

    @Inject(method = "isSectionCompiledAndVisible(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void customSectionCompiled(BlockPos position, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.vulkanmod$renderer != null && this.vulkanmod$renderer.isSectionCompiled(position));
    }

    @Inject(method = "countRenderedSections()I", at = @At("HEAD"), cancellable = true)
    private void customVisibleCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.vulkanmod$renderer == null ? 0 : this.vulkanmod$renderer.getVisibleSectionsCount());
    }

    @Inject(method = "getSectionStatistics()Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void customStatistics(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(this.vulkanmod$renderer == null ? "C: 0/0" : this.vulkanmod$renderer.getChunkStatistics());
    }

    @Inject(method = "hasRenderedAllSections()Z", at = @At("HEAD"), cancellable = true)
    private void customAllSectionsReady(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.vulkanmod$renderer == null || !this.vulkanmod$renderer.hasPendingSectionWork());
    }

    @Inject(method = "onResourceManagerReload(Lnet/minecraft/server/packs/resources/ResourceManager;)V", at = @At("RETURN"))
    private void reloadCustomPipelines(ResourceManager resources, CallbackInfo ci) {
        PipelineManager.reload();
        if (this.vulkanmod$renderer != null) this.vulkanmod$renderer.allChanged();
    }

    @Inject(method = "close()V", at = @At("HEAD"))
    private void closeCustomTerrain(CallbackInfo ci) {
        if (this.vulkanmod$renderer != null) this.vulkanmod$renderer.cleanUp();
    }
}
