package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.chunk.TerrainRenderState;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.Initializer;
import net.vulkanmod.compat.sable.render.SublevelRenderScope;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Final
    private RenderBuffers renderBuffers;

    @Shadow @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    private WorldRenderer worldRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.worldRenderer = WorldRenderer.init(this.renderBuffers);
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        this.worldRenderer.setLevel(clientLevel);
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onAllChanged(CallbackInfo ci) {
        this.worldRenderer.allChanged();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void renderBlockEntities(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        PoseStack poseStack = new PoseStack();

        this.worldRenderer.renderBlockEntities(poseStack, pos.x(), pos.y(), pos.z(), this.destructionProgress, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void prepareLevelRenderState(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        prepareWorldPassRenderState();
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void finishLevelRenderState(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer,
                                        LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        int leakedScopes = SublevelRenderScope.forceUnwind();
        if (leakedScopes > 0) {
            Initializer.LOGGER.error("Leaked Sable render scopes at frame end: {}", leakedScopes);
        }
    }

    @Inject(method = "setupRender", at = @At("HEAD"))
    private void setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator, CallbackInfo ci) {
        this.worldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);
    }

    @Inject(method = "renderSectionLayer", at = @At("HEAD"))
    private void renderSectionLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.worldRenderer.renderSectionLayer(renderType, camX, camY, camZ, modelView, projectionMatrix);
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"))
    private void prepareWeatherRenderState(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        prepareWorldPassRenderState();
    }

    @Inject(method = "renderSky", at = @At("HEAD"))
    private void prepareSkyRenderState(Matrix4f modelView, Matrix4f projection, float partialTick, Camera camera, boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        prepareWorldPassRenderState();
    }

    @Inject(method = "renderClouds", at = @At("HEAD"))
    private void prepareCloudRenderState(PoseStack poseStack, Matrix4f modelView, Matrix4f projection, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        prepareWorldPassRenderState();
    }

    @Inject(method = "renderWorldBorder", at = @At("HEAD"))
    private void prepareWorldBorderRenderState(Camera camera, CallbackInfo ci) {
        prepareWorldPassRenderState();
    }

    @Inject(method = "renderDebug", at = @At("HEAD"))
    private void prepareDebugRenderState(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, CallbackInfo ci) {
        prepareWorldPassRenderState();
    }

    @Unique
    private void prepareWorldPassRenderState() {
        TerrainRenderState.prepareWorldTerrainState();
    }

    @Inject(method = "applyFrustum", at = @At("HEAD"), cancellable = true)
    private void skipVanillaSectionCollection(Frustum frustum, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "isSectionCompiled", at = @At("HEAD"), cancellable = true)
    public void isSectionCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.worldRenderer.isSectionCompiled(blockPos));
    }

    @Inject(method = "onChunkLoaded", at = @At("HEAD"), cancellable = true)
    public void onChunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "setSectionDirty(IIIZ)V", at = @At("HEAD"))
    private void setSectionDirty(int x, int y, int z, boolean flag, CallbackInfo ci) {
        this.worldRenderer.setSectionDirty(x, y, z, flag);
    }

    @Inject(method = "blockChanged", at = @At("HEAD"), cancellable = true)
    private void skipInvisibleLeafUpdates(net.minecraft.world.level.BlockGetter blockGetter, BlockPos pos,
                                          net.minecraft.world.level.block.state.BlockState oldState,
                                          net.minecraft.world.level.block.state.BlockState newState,
                                          int flags, CallbackInfo ci) {
        if (net.vulkanmod.render.culling.LeafUpdateFilter.isInvisibleLeafPropertyChange(oldState, newState)) {
            net.vulkanmod.render.optimization.ChunkFrameDiagnostics.recordLeafDirtySkipped();
            ci.cancel();
        }
    }

    @Inject(method = "getSectionStatistics", at = @At("HEAD"), cancellable = true)
    public void getSectionStatistics(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(this.worldRenderer.getChunkStatistics());
    }

    @Inject(method = "hasRenderedAllSections", at = @At("HEAD"), cancellable = true)
    public void hasRenderedAllSections(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!this.worldRenderer.hasPendingSectionWork());
    }

    @Inject(method = "countRenderedSections", at = @At("HEAD"), cancellable = true)
    public void countRenderedSections(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.worldRenderer.getVisibleSectionsCount());
    }

    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getDepthFar()F"))
    private float getRenderDistanceZFar(GameRenderer instance) {
        return instance.getRenderDistance() * 4F;
    }

}
