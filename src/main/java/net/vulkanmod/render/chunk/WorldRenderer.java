package net.vulkanmod.render.chunk;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.render.engine.VkSampler;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.RenderRegionBuilder;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.graph.SectionGraph;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.IndexBuffer;
import net.vulkanmod.vulkan.memory.buffer.IndirectBuffer;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.*;

import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.optimization.AdaptiveChunkUploadBudget;
import net.vulkanmod.render.optimization.ChunkFrameDiagnostics;
import net.vulkanmod.render.optimization.FramePerformanceMetrics;

public class WorldRenderer {
    private static final float GRAPH_MOVEMENT_THRESHOLD_SQUARED = 0.25f;
    private static WorldRenderer INSTANCE;

    private final Minecraft minecraft;
    private ClientLevel level;
    private int renderDistance;
    private final RenderBuffers renderBuffers;

    private Vec3 cameraPos;
    private int lastCameraSectionX;
    private int lastCameraSectionY;
    private int lastCameraSectionZ;
    private float lastCameraX;
    private float lastCameraY;
    private float lastCameraZ;
    private float lastCamRotX;
    private float lastCamRotY;

    private SectionGrid sectionGrid;

    private SectionGraph sectionGraph;
    private boolean graphNeedsUpdate;

    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
    private final OrderedIdentityExtraction<BlockEntity> blockEntityExtraction = new OrderedIdentityExtraction<>();

    private final TaskDispatcher taskDispatcher;

    private double xTransparentOld;
    private double yTransparentOld;
    private double zTransparentOld;

    IndirectBuffer[] indirectBuffers;

    public RenderRegionBuilder renderRegionCache;

    private final List<Runnable> onAllChangedCallbacks = new ObjectArrayList<>();

    private WorldRenderer(RenderBuffers renderBuffers) {
        this.minecraft = Minecraft.getInstance();
        this.renderBuffers = renderBuffers;
        this.renderRegionCache = new RenderRegionBuilder();
        this.taskDispatcher = new TaskDispatcher();
        ChunkTask.setTaskDispatcher(this.taskDispatcher);
        allocateIndirectBuffers();

        BlockRenderer.setBlockColors(this.minecraft.getBlockColors());

        Renderer.getInstance().addOnResizeCallback(() -> {
            if (this.indirectBuffers != null && this.indirectBuffers.length != Renderer.getFramesNum())
                allocateIndirectBuffers();
        });
    }

    private void allocateIndirectBuffers() {
        if (this.indirectBuffers != null)
            Arrays.stream(this.indirectBuffers).forEach(Buffer::scheduleFree);

        this.indirectBuffers = new IndirectBuffer[Renderer.getFramesNum()];

        for (int i = 0; i < this.indirectBuffers.length; ++i) {
            this.indirectBuffers[i] = new IndirectBuffer(1000000, MemoryTypes.HOST_MEM);

        }

    }

    public static WorldRenderer init(RenderBuffers renderBuffers) {
        PipelineManager.init();
        if (INSTANCE != null)
            return INSTANCE;
        else
            return INSTANCE = new WorldRenderer(renderBuffers);
    }

    public static WorldRenderer getInstance() {
        return INSTANCE;
    }

    public static ClientLevel getLevel() {
        return INSTANCE.level;
    }

    public static Vec3 getCameraPos() {
        return INSTANCE.cameraPos;
    }

    public void setupRenderer(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator) {
        if (this.level == null || this.sectionGrid == null || this.sectionGraph == null) return;
        this.cameraPos = camera.position();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.renderDistance) {
            this.allChanged();
        }

        net.minecraft.util.profiling.Profiler.get().push("camera");
        float cameraX = (float) cameraPos.x();
        float cameraY = (float) cameraPos.y();
        float cameraZ = (float) cameraPos.z();
        int sectionX = SectionPos.posToSectionCoord(cameraX);
        int sectionY = SectionPos.posToSectionCoord(cameraY);
        int sectionZ = SectionPos.posToSectionCoord(cameraZ);

        boolean cameraSectionChanged = this.lastCameraSectionX != sectionX
                || this.lastCameraSectionY != sectionY
                || this.lastCameraSectionZ != sectionZ;
        if (cameraSectionChanged) {
            this.lastCameraSectionX = sectionX;
            this.lastCameraSectionY = sectionY;
            this.lastCameraSectionZ = sectionZ;
            this.sectionGrid.repositionCamera(cameraX, cameraZ);
        }

        double entityDistanceScaling = this.minecraft.options.entityDistanceScaling().get();
        Entity.setViewScale(Mth.clamp((double) this.renderDistance / 8.0D, 1.0D, 2.5D) * entityDistanceScaling);

        net.minecraft.util.profiling.Profiler.get().popPush("cull");
        net.minecraft.util.profiling.Profiler.get().popPush("culling");

        net.minecraft.util.profiling.Profiler.get().popPush("update");

        boolean cameraMoved = false;
        float d_xRot = Math.abs(camera.xRot() - this.lastCamRotX);
        float d_yRot = Math.abs(camera.yRot() - this.lastCamRotY);
        cameraMoved |= d_xRot > 2.0f || d_yRot > 2.0f;

        float cameraDeltaX = cameraX - this.lastCameraX;
        float cameraDeltaY = cameraY - this.lastCameraY;
        float cameraDeltaZ = cameraZ - this.lastCameraZ;
        cameraMoved |= cameraSectionChanged
                || cameraDeltaX * cameraDeltaX + cameraDeltaY * cameraDeltaY + cameraDeltaZ * cameraDeltaZ
                >= GRAPH_MOVEMENT_THRESHOLD_SQUARED;
        this.graphNeedsUpdate |= cameraMoved;

        if (!isCapturedFrustum) {

            if (this.graphNeedsUpdate) {
                this.graphNeedsUpdate = false;
                this.lastCameraX = cameraX;
                this.lastCameraY = cameraY;
                this.lastCameraZ = cameraZ;
                this.lastCamRotX = camera.xRot();
                this.lastCamRotY = camera.yRot();

                long graphStart = System.nanoTime();
                this.sectionGraph.update(camera, frustum, spectator);
                ChunkFrameDiagnostics.recordGraphUpdate(System.nanoTime() - graphStart);
            }
        }

        long scheduleStart = System.nanoTime();
        this.sectionGraph.schedulePendingRebuilds();
        ChunkFrameDiagnostics.recordSchedule(System.nanoTime() - scheduleStart,
                this.sectionGraph.readyRebuildCount(), this.sectionGraph.delayedRebuildCount());

        this.indirectBuffers[Renderer.getCurrentFrame()].reset();

        net.minecraft.util.profiling.Profiler.get().pop();
    }

    public boolean uploadSections() {

        long uploadStartedAt = System.nanoTime();

        if (this.sectionGrid == null) {
            return false;
        }

        net.minecraft.util.profiling.Profiler.get().push("upload");

        boolean performedUploadWork = false;
        try {
            if (this.taskDispatcher.updateSections()) {
                this.scheduleGraphUpdate();
            }
            performedUploadWork = this.taskDispatcher.performedUploadWorkLastUpdate();
        } catch (Exception e) {

            Initializer.LOGGER.error("Failed to upload chunk sections; resetting renderer", e);
            allChanged();
        }

        net.minecraft.util.profiling.Profiler.get().pop();
        FramePerformanceMetrics.INSTANCE.recordChunkUpload(System.nanoTime() - uploadStartedAt,
                performedUploadWork, Initializer.CONFIG != null && Initializer.CONFIG.adaptiveChunkUploads);
        return performedUploadWork;
    }

    public boolean isSectionCompiled(BlockPos blockPos) {
        if (this.sectionGrid == null) return false;
        RenderSection renderSection = this.sectionGrid.getSectionAtBlockPos(blockPos);
        return renderSection != null && renderSection.xOffset() == (blockPos.getX() & ~15)
                && renderSection.yOffset() == (blockPos.getY() & ~15)
                && renderSection.zOffset() == (blockPos.getZ() & ~15) && renderSection.isCompiled()
                && renderSection.getLastFrame() == this.getLastFrame();
    }

    public void allChanged() {
        AdaptiveChunkUploadBudget.reset();
        if (this.level != null) {
            PipelineManager.init();

            this.level.clearTintCaches();

            this.renderRegionCache.clear();
            this.taskDispatcher.createThreads(this.minecraft.hasSingleplayerServer());

            this.graphNeedsUpdate = true;

            this.renderDistance = this.minecraft.options.getEffectiveRenderDistance();
            if (this.sectionGrid != null) {
                this.sectionGrid.releaseAllBuffers();
            }

            this.taskDispatcher.clearBatchQueue();
            synchronized (this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }
            synchronized (RenderSection.globalBlockEntitiesMap) {
                RenderSection.globalBlockEntitiesMap.clear();
            }

            this.sectionGrid = new SectionGrid(this.level, this.renderDistance);
            this.sectionGraph = new SectionGraph(this.level, this.sectionGrid, this.taskDispatcher);

            this.onAllChangedCallbacks.forEach(Runnable::run);

            Entity entity = this.minecraft.getCameraEntity();
            if (entity != null) {
                this.sectionGrid.repositionCamera(entity.getX(), entity.getZ());
            }

        }
    }

    public void setLevel(@Nullable ClientLevel level) {
        if (this.level == level && (level == null || this.sectionGrid != null)) return;
        this.taskDispatcher.stopThreads();
        AdaptiveChunkUploadBudget.reset();
        this.lastCameraX = Float.MIN_VALUE;
        this.lastCameraY = Float.MIN_VALUE;
        this.lastCameraZ = Float.MIN_VALUE;
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;

        this.level = level;
        ChunkStatusMap.createInstance(renderDistance);
        if (level != null) {
            this.allChanged();
        } else {
            net.vulkanmod.compat.external.ExternalTerrainRenderBridge.close();
            this.taskDispatcher.stopThreads();
            if (this.sectionGrid != null) {
                this.sectionGrid.releaseAllBuffers();
                this.sectionGrid = null;
            }

            this.sectionGraph = null;
            this.renderRegionCache.clear();
            synchronized (RenderSection.globalBlockEntitiesMap) {
                RenderSection.globalBlockEntitiesMap.clear();
            }
            PipelineManager.destroyPipelines();
            this.graphNeedsUpdate = true;
        }

    }

    public void addOnAllChangedCallback(Runnable runnable) {
        this.onAllChangedCallbacks.add(runnable);
    }

    public void clearOnAllChangedCallbacks() {
        this.onAllChangedCallbacks.clear();
    }

    public void renderSectionLayer(ChunkSectionLayer renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projection) {
        renderTerrainLayer(TerrainRenderType.get(renderType), camX, camY, camZ, modelView, projection);
    }

    public static List<TerrainRenderType> terrainLayersForGroup(ChunkSectionLayerGroup group, boolean uniqueOpaque) {
        return switch (group) {
            case OPAQUE -> uniqueOpaque ? List.of(TerrainRenderType.CUTOUT_MIPPED)
                    : List.of(TerrainRenderType.CUTOUT_MIPPED, TerrainRenderType.CUTOUT);
            case TRANSLUCENT -> List.of(TerrainRenderType.TRANSLUCENT);
            case TRIPWIRE -> List.of(TerrainRenderType.TRIPWIRE);
        };
    }

    public record ChunkDrawContext(Matrix4f modelView, Matrix4f projection, double camX, double camY,
                                   double camZ, GpuTextureView atlas) {
    }

    public void renderGroup(ChunkSectionLayerGroup group, GpuSampler sampler, ChunkDrawContext context) {
        if (this.level == null || this.sectionGraph == null) return;
        PipelineManager.init();
        RenderTarget target = group.outputTarget();
        Matrix4f previousModelView = new Matrix4f(VRenderSystem.getModelViewMatrix().buffer.asFloatBuffer());
        Matrix4f previousProjection = new Matrix4f(VRenderSystem.getProjectionMatrix().buffer.asFloatBuffer());
        GpuTextureView previousAtlas = VRenderSystem.getShaderTexture(0);
        GpuTextureView previousLight = VRenderSystem.getShaderTexture(2);
        VRenderSystem.setShaderTexture(0, context.atlas);
        GpuTextureView light = this.minecraft.gameRenderer.lightTexture().getTextureView();
        VRenderSystem.setShaderTexture(2, light);
        ((VkGpuTexture) context.atlas.texture()).getVulkanImage().setSampler(((VkSampler) sampler).getId());
        ((VkGpuTexture) light.texture()).getVulkanImage().setSampler(((VkSampler) RenderSystem.getSamplerCache()
                .getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR)).getId());
        try (RenderPass ignored = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Vulkan terrain " + group.label(), target.getColorTextureView(), OptionalInt.empty(),
                target.getDepthTextureView(), OptionalDouble.empty())) {
            for (TerrainRenderType layer : terrainLayersForGroup(group, Initializer.CONFIG.uniqueOpaqueLayer)) {
                renderTerrainLayer(layer, context.camX, context.camY, context.camZ, context.modelView, context.projection);
            }
        } finally {
            VRenderSystem.setShaderTexture(0, previousAtlas);
            VRenderSystem.setShaderTexture(2, previousLight);
            VRenderSystem.applyMVP(previousModelView, previousProjection);
            VRenderSystem.setChunkOffset(0, 0, 0);
            TerrainRenderState.prepareWorldTerrainState();
        }
    }

    private void renderTerrainLayer(TerrainRenderType terrainRenderType, double camX, double camY, double camZ,
                                    Matrix4f modelView, Matrix4f projection) {
                                                                                      
                                                                               
        TerrainRenderState.prepareWorldTerrainState();
        if (!Initializer.CONFIG.backFaceCulling) VRenderSystem.disableCull();
        if (terrainRenderType == TerrainRenderType.TRANSLUCENT || terrainRenderType == TerrainRenderType.TRIPWIRE) {
            VRenderSystem.enableBlend();
            VRenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                    GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        this.sortTranslucentSections(camX, camY, camZ);

        net.minecraft.util.profiling.Profiler.get().push("filterempty");
        net.minecraft.util.profiling.Profiler.get().popPush(() -> "render_" + terrainRenderType);

        final boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;
        final boolean indirectDraw = Initializer.CONFIG.indirectDraw && DeviceManager.supportsFastIndirectDraw();

        VRenderSystem.applyMVP(modelView, projection);
        VRenderSystem.setPrimitiveTopologyGL(GL11.GL_TRIANGLES);

        Renderer renderer = Renderer.getInstance();
        GraphicsPipeline pipeline = PipelineManager.getTerrainShader(terrainRenderType);
        renderer.bindGraphicsPipeline(pipeline);

        VTextureSelector.bindShaderTextures(pipeline);

        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer, indexBuffer.indexType.value);

        int currentFrame = Renderer.getCurrentFrame();
        Set<TerrainRenderType> allowedRenderTypes = EnumSet.allOf(TerrainRenderType.class);
        if (allowedRenderTypes.contains(terrainRenderType)) {
            terrainRenderType.setCutoutUniform();
            boolean ubosBound = false;

            final StaticQueue<ChunkArea> chunkAreaQueue = this.sectionGraph.getChunkAreaQueue().queue();
            final int areaCount = chunkAreaQueue.size();
            for (int areaIdx = 0; areaIdx < areaCount; areaIdx++) {
                ChunkArea chunkArea = chunkAreaQueue.get(isTranslucent ? areaCount - 1 - areaIdx : areaIdx);
                var queue = chunkArea.sectionQueue;
                DrawBuffers drawBuffers = chunkArea.drawBuffers;

                if (drawBuffers.getAreaBuffer(terrainRenderType) != null && queue.size() > 0) {

                    if (!ubosBound) {
                                                                                                    
                                                                                                  
                                                                                          
                        renderer.uploadAndBindUBOs(pipeline);
                        ubosBound = true;
                    }

                    drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, terrainRenderType, camX, camY, camZ);

                    if (indirectDraw)
                        drawBuffers.buildDrawBatchesIndirect(indirectBuffers[currentFrame], queue, terrainRenderType);
                    else
                        drawBuffers.buildDrawBatchesDirect(queue, terrainRenderType);
                }
            }
        }

        if (indirectDraw) {
            indirectBuffers[currentFrame].submitUploads();

        }

        if (!indirectDraw) {
            VRenderSystem.setChunkOffset(0, 0, 0);
            renderer.pushConstants(pipeline);
        }

        net.minecraft.util.profiling.Profiler.get().pop();

                                                                                         
    }

    private void sortTranslucentSections(double camX, double camY, double camZ) {
        net.minecraft.util.profiling.Profiler.get().push("translucent_sort");
        double d0 = camX - this.xTransparentOld;
        double d1 = camY - this.yTransparentOld;
        double d2 = camZ - this.zTransparentOld;

        if (d0 * d0 + d1 * d1 + d2 * d2 > 2.0D) {
            this.xTransparentOld = camX;
            this.yTransparentOld = camY;
            this.zTransparentOld = camZ;
            var sectionQueue = this.sectionGraph.getSectionQueue();
            final int sortCount = Math.min(sectionQueue.size(), 15);

            for (int i = 0; i < sortCount; i++) {
                sectionQueue.get(i).resortTransparency(this.taskDispatcher);
            }
        }

        net.minecraft.util.profiling.Profiler.get().pop();
    }

    public Set<BlockEntity> getVisibleBlockEntities() {
        Set<BlockEntity> entities = Collections.newSetFromMap(new IdentityHashMap<>());
        if (this.sectionGraph == null || this.level == null) return entities;
        var sections = this.sectionGraph.getBlockEntitiesSections();
        for (int i = 0; i < sections.size(); i++) {
            entities.addAll(sections.get(i).getCompiledSection().getBlockEntities());
        }
        synchronized (RenderSection.globalBlockEntitiesMap) {
            RenderSection.globalBlockEntitiesMap.values().forEach(entities::addAll);
        }
        entities.addAll(this.level.getGloballyRenderedBlockEntities());
        return entities;
    }

    public void extractBlockEntities(Camera camera, float partialTick, LevelRenderState state, Frustum frustum,
                                     Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress,
                                     boolean outlines) {
        if (this.level == null || this.sectionGraph == null) return;
        Vec3 cameraPosition = camera.position();
        try (var entities = this.blockEntityExtraction.acquire()) {
                                                                                    
                                                                                          
            var sections = this.sectionGraph.getBlockEntitiesSections();
            for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                var localEntities = sections.get(sectionIndex).getCompiledSection().getBlockEntities();
                for (int entityIndex = 0; entityIndex < localEntities.size(); entityIndex++) {
                    entities.addLocal(localEntities.get(entityIndex));
                }
            }
            synchronized (RenderSection.globalBlockEntitiesMap) {
                for (Set<BlockEntity> globals : RenderSection.globalBlockEntitiesMap.values()) {
                    for (BlockEntity entity : globals) entities.addGlobal(entity);
                }
            }
            for (BlockEntity entity : this.level.getGloballyRenderedBlockEntities()) entities.addGlobal(entity);

                                                                                               
            PoseStack poses = null;
            for (int index = 0; index < entities.size(); index++) {
                BlockEntity entity = entities.get(index);
                if (entity.isRemoved()) continue;
                BlockPos position = entity.getBlockPos();
                if (Initializer.CONFIG.blockEntityCulling && !entities.isGlobal(entity)) {
                    double dx = position.getX() + 0.5 - cameraPosition.x;
                    double dy = position.getY() + 0.5 - cameraPosition.y;
                    double dz = position.getZ() + 0.5 - cameraPosition.z;
                    if (dx * dx + dy * dy + dz * dz > 9216.0) continue;
                }
                SortedSet<BlockDestructionProgress> progress = destructionProgress.get(position.asLong());
                ModelFeatureRenderer.CrumblingOverlay overlay = null;
                if (progress != null && !progress.isEmpty()) {
                    if (poses == null) poses = new PoseStack();
                    poses.pushPose();
                    poses.translate(position.getX() - cameraPosition.x, position.getY() - cameraPosition.y,
                            position.getZ() - cameraPosition.z);
                                                                                    
                                                                                        
                    overlay = new ModelFeatureRenderer.CrumblingOverlay(progress.last().getProgress(), poses.last().copy());
                    poses.popPose();
                }
                BlockEntityRenderState extracted = this.minecraft.getBlockEntityRenderDispatcher()
                        .tryExtractRenderState(entity, partialTick, overlay, frustum);
                if (extracted != null) {
                    state.blockEntityRenderStates.add(extracted);
                    if (outlines && entity.hasCustomOutlineRendering(this.minecraft.player)) state.haveGlowingEntities = true;
                }
            }
        }
    }
    public void scheduleGraphUpdate() {
        this.graphNeedsUpdate = true;
    }

    public boolean graphNeedsUpdate() {
        return this.graphNeedsUpdate;
    }

    public boolean hasPendingSectionWork() {
        return this.graphNeedsUpdate
                || this.sectionGraph != null && this.sectionGraph.hasPendingRebuilds()
                || !this.taskDispatcher.isIdle();
    }

    public int getVisibleSectionsCount() {
        return this.sectionGraph == null ? 0 : this.sectionGraph.getSectionQueue().size();
    }

    public void setSectionDirty(int x, int y, int z, boolean flag) {
        if (this.sectionGrid == null || this.level == null || this.level.isOutsideBuildHeight(y << 4)) return;
        RenderSection section = this.sectionGrid.getSectionAtBlockPos(x << 4, y << 4, z << 4);
        if (section == null || section.xOffset() != (x << 4) || section.yOffset() != (y << 4)
                || section.zOffset() != (z << 4)) return;
        this.sectionGrid.setDirty(x, y, z, flag);

        this.renderRegionCache.remove(x, z);
        this.scheduleGraphUpdate();
    }

    public void onChunkUnloaded(int x, int z) {
        if (this.level == null || this.sectionGrid == null) return;
        for (int y = this.level.getMinY(); y < this.level.getMaxY(); y += 16) {
            RenderSection section = this.sectionGrid.getSectionAtBlockPos(x << 4, y, z << 4);
            if (section != null && section.xOffset() == (x << 4) && section.zOffset() == (z << 4)) {
                section.setOrigin(section.xOffset(), section.yOffset(), section.zOffset());
            }
        }
        this.renderRegionCache.remove(x, z);
        this.scheduleGraphUpdate();
    }

    public SectionGrid getSectionGrid() {
        return this.sectionGrid;
    }

    public ChunkAreaManager getChunkAreaManager() {
        return this.sectionGrid.chunkAreaManager;
    }

    public TaskDispatcher getTaskDispatcher() {
        return taskDispatcher;
    }

    public short getLastFrame() {
        return this.sectionGraph == null ? -1 : this.sectionGraph.getLastFrame();
    }

    public int getRenderDistance() {
        return this.renderDistance;
    }

    public String getChunkStatistics() {
        return this.sectionGraph == null ? "C: 0/0" : this.sectionGraph.getStatistics();
    }

    public void cleanUp() {
        this.setLevel(null);
        if (indirectBuffers != null) {
            Arrays.stream(indirectBuffers).forEach(Buffer::scheduleFree);
            this.indirectBuffers = null;
        }
    }

}
