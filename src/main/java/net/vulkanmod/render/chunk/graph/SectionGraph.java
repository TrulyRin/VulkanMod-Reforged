package net.vulkanmod.render.chunk.graph;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.render.chunk.*;
import net.vulkanmod.render.chunk.build.RenderRegionBuilder;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.frustum.VFrustum;
import net.vulkanmod.render.optimization.AdaptiveChunkUploadBudget;
import net.vulkanmod.render.optimization.ChunkFrameDiagnostics;
import net.vulkanmod.render.optimization.ChunkSchedulingPolicy;
import net.vulkanmod.render.optimization.DelayedRebuildQueue;
import net.vulkanmod.render.chunk.util.AreaSetQueue;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import org.joml.FrustumIntersection;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class SectionGraph {
    Minecraft minecraft;
    private final Level level;

    private final SectionGrid sectionGrid;
    private final ChunkAreaManager chunkAreaManager;
    private final TaskDispatcher taskDispatcher;
    private final ResettableQueue<RenderSection> sectionQueue = new ResettableQueue<>();
    private AreaSetQueue chunkAreaQueue;
    private short lastFrame = 0;

    private final ResettableQueue<RenderSection> blockEntitiesSections = new ResettableQueue<>();
    private final ResettableQueue<RenderSection> rebuildQueue = new ResettableQueue<>();
    private final ArrayDeque<RenderSection> pendingRebuilds = new ArrayDeque<>();
    private final Set<RenderSection> pendingRebuildSet =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final DelayedRebuildQueue<RenderSection> delayedRebuilds = new DelayedRebuildQueue<>();

    private VFrustum frustum;

    public RenderRegionBuilder renderRegionCache;
    int nonEmptyChunks;

    public SectionGraph(Level level, SectionGrid sectionGrid, TaskDispatcher taskDispatcher) {
        this.level = level;
        this.sectionGrid = sectionGrid;
        this.chunkAreaManager = sectionGrid.getChunkAreaManager();
        this.taskDispatcher = taskDispatcher;

        this.chunkAreaQueue = new AreaSetQueue(sectionGrid.getChunkAreaManager().size);
        this.minecraft = Minecraft.getInstance();
        this.renderRegionCache = WorldRenderer.getInstance().renderRegionCache;
    }

    public void update(Camera camera, Frustum frustum, boolean spectator) {
        BlockPos blockpos = camera.getBlockPosition();

        this.minecraft.getProfiler().popPush("update");

        boolean spectatorInsideSolid = spectator && this.level.getBlockState(blockpos).isSolidRender(this.level, blockpos);
        boolean flag = SectionTraversalPolicy.shouldUseDirectionalCulling(this.minecraft.smartCull, spectatorInsideSolid, this.level.dimensionType().hasSkyLight());

        this.frustum = (((FrustumMixed) (frustum)).customFrustum()).offsetToFullyIncludeCameraCube(8);
        this.sectionGrid.updateFrustumVisibility(this.frustum);

        this.minecraft.getProfiler().push("partial_update");

        this.initUpdate();
        this.initializeQueueForFullUpdate(camera);

        if (flag) {
            this.updateRenderChunks();
            if (this.nonEmptyChunks == 0) {
                retryWithoutDirectionalCulling(camera);
            }
        } else {
            this.updateRenderChunksSpectator();
        }

        this.enqueueVisibleRebuilds();

        this.minecraft.getProfiler().pop();
    }

    private void retryWithoutDirectionalCulling(Camera camera) {
        this.initUpdate();
        this.initializeQueueForFullUpdate(camera);
        this.updateRenderChunksSpectator();
    }

    private void initializeQueueForFullUpdate(Camera camera) {
        Vec3 vec3 = camera.getPosition();
        BlockPos blockpos = camera.getBlockPosition();
        RenderSection renderSection = this.sectionGrid.getSectionAtBlockPos(blockpos);

        if (renderSection == null) {
            boolean flag = blockpos.getY() > this.level.getMinBuildHeight();
            int y = flag ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
            int x = Mth.floor(vec3.x / 16.0D) * 16;
            int z = Mth.floor(vec3.z / 16.0D) * 16;

            int renderDistance = WorldRenderer.getInstance().getRenderDistance();
            int side = renderDistance * 2 + 1;

            this.sectionQueue.ensureCapacity(side * side);

            for (int x1 = -renderDistance; x1 <= renderDistance; ++x1) {
                for (int z1 = -renderDistance; z1 <= renderDistance; ++z1) {

                    RenderSection renderSection1 = this.sectionGrid.getSectionAtBlockPos(
                            x + SectionPos.sectionToBlockCoord(x1, 8), y, z + SectionPos.sectionToBlockCoord(z1, 8));
                    if (renderSection1 != null) {
                        initFirstNode(renderSection1, this.lastFrame);
                        this.sectionQueue.add(renderSection1);
                    }
                }
            }

        } else {
            initFirstNode(renderSection, this.lastFrame);

            this.sectionQueue.add(renderSection);
        }

    }

    private static void initFirstNode(RenderSection renderSection, short frame) {
        renderSection.mainDir = 7;
        renderSection.sourceDirs = (byte) (1 << 7);
        renderSection.directions = (byte) 0xFF;
        renderSection.setLastFrame(frame);
        renderSection.visibility |= INIT_VISIBILITY;
        renderSection.directionChanges = 0;
        renderSection.steps = 0;
    }

    private static final long INIT_VISIBILITY = computeInitVisibility();

    private static long computeInitVisibility() {
        long vis = 0;
        for (int dir = 0; dir < 6; dir++) {
            vis |= 1L << ((6 << 3) + dir);
            vis |= 1L << ((7 << 3) + dir);
        }

        return vis;
    }

    private void initUpdate() {
        this.resetUpdateQueues();

        this.lastFrame++;
        this.nonEmptyChunks = 0;
    }

    private void resetUpdateQueues() {
        this.chunkAreaQueue.clear();
        this.sectionGrid.getChunkAreaManager().resetQueues();
        this.sectionQueue.clear();
        this.blockEntitiesSections.clear();
        this.rebuildQueue.clear();
    }

    private void updateRenderChunks() {
        int maxDirectionsChanges = Initializer.CONFIG.advCulling - 1;

        while (this.sectionQueue.hasNext()) {
            RenderSection renderSection = this.sectionQueue.poll();

            if (notInFrustum(renderSection))
                continue;

            if (renderSection.directionChanges > maxDirectionsChanges)
                continue;

            if (!renderSection.isCompletelyEmpty()) {
                renderSection.getChunkArea().sectionQueue.add(renderSection);
                this.chunkAreaQueue.add(renderSection.getChunkArea());
                this.nonEmptyChunks++;
            }

            if (renderSection.containsBlockEntities()) {
                this.blockEntitiesSections.ensureCapacity(1);
                this.blockEntitiesSections.add(renderSection);
            }

            if (renderSection.isDirty()) {
                this.rebuildQueue.ensureCapacity(1);
                this.rebuildQueue.add(renderSection);
            }

            byte dirs = (byte) (renderSection.getVisibilityDirs() & renderSection.getDirections());

            visitAdjacentNodes(renderSection, dirs);
        }
    }

    private void enqueueVisibleRebuilds() {
        long nowNanos = System.nanoTime();
        for (int i = 0; i < this.rebuildQueue.size(); i++) {
            RenderSection section = this.rebuildQueue.get(i);

            boolean bypassDelay = section.isDirtyFromPlayer()
                    || !section.isInRebuildCooldown(nowNanos, ChunkSchedulingPolicy.REBUILD_COOLDOWN_NANOS);
            if (bypassDelay && this.delayedRebuilds.remove(section)) {
                this.enqueueReadyRebuild(section, section.isDirtyFromPlayer());
                continue;
            }

            if (this.delayedRebuilds.contains(section)) {
                continue;
            }

            this.enqueueReadyRebuild(section, section.isDirtyFromPlayer());
        }
        this.rebuildQueue.clear();
    }

    private void enqueueReadyRebuild(RenderSection section, boolean first) {
        if (!this.pendingRebuildSet.add(section)) {
            if (first && this.pendingRebuilds.remove(section)) {
                this.pendingRebuilds.addFirst(section);
            }
            return;
        }

        if (first) {
            this.pendingRebuilds.addFirst(section);
        } else {
            this.pendingRebuilds.addLast(section);
        }
    }

    private void promoteEligibleRebuilds(long nowNanos) {
        for (int promoted = 0;
             promoted < ChunkSchedulingPolicy.MAX_DELAYED_REBUILD_PROMOTIONS_PER_FRAME;
             promoted++) {
            RenderSection section = this.delayedRebuilds.pollEligible(nowNanos);
            if (section == null) {
                break;
            }
            this.enqueueReadyRebuild(section, section.isDirtyFromPlayer());
        }
    }

    public void schedulePendingRebuilds() {
        long startNanos = System.nanoTime();
        this.promoteEligibleRebuilds(startNanos);

        int readyCount = this.pendingRebuilds.size();
        if (readyCount == 0) {
            return;
        }

        int maxScheduledRebuilds = Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.chooseRebuildScheduleBudget(readyCount)
                : ChunkSchedulingPolicy.fixedRebuildScheduleBudget(readyCount);
        long scheduleTimeBudgetNanos = Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.chooseRebuildScheduleTimeBudgetNanos(readyCount)
                : ChunkSchedulingPolicy.fixedRebuildTimeBudgetNanos(readyCount);
        int scheduledRebuilds = 0;
        int processedRebuilds = 0;

        while (scheduledRebuilds < maxScheduledRebuilds
                && processedRebuilds < ChunkSchedulingPolicy.MAX_REBUILD_QUEUE_SCANS_PER_FRAME
                && !this.pendingRebuilds.isEmpty()) {
            if (processedRebuilds > 0 && System.nanoTime() - startNanos >= scheduleTimeBudgetNanos) {
                break;
            }

            if (!this.taskDispatcher.hasTaskCapacity()) {
                break;
            }

            RenderSection section = this.pendingRebuilds.removeFirst();
            this.pendingRebuildSet.remove(section);
            processedRebuilds++;

            if (!section.isDirty() || section.getLastFrame() != this.lastFrame) {
                continue;
            }

            if (!section.isDirtyFromPlayer()
                    && section.isInRebuildCooldown(startNanos, ChunkSchedulingPolicy.REBUILD_COOLDOWN_NANOS)) {
                if (this.delayedRebuilds.defer(section,
                        section.rebuildCooldownDeadlineNanos(ChunkSchedulingPolicy.REBUILD_COOLDOWN_NANOS))) {
                    ChunkFrameDiagnostics.recordRebuildDeferred();
                }
                continue;
            }

            long prepareStartNanos = System.nanoTime();
            boolean scheduled = section.rebuildChunkAsync(this.taskDispatcher, this.renderRegionCache);
            long prepareEndNanos = System.nanoTime();
            long preparationNanos = prepareEndNanos - prepareStartNanos;
            if (Initializer.CONFIG.adaptiveChunkUploads) {
                AdaptiveChunkUploadBudget.recordRebuildPreparation(preparationNanos);
            }
            ChunkFrameDiagnostics.recordRebuildPreparation(preparationNanos);

            if (scheduled) {
                section.setNotDirty();
                section.markRebuildScheduled(prepareEndNanos);
                scheduledRebuilds++;
            } else if (!this.taskDispatcher.hasTaskCapacity()) {
                if (this.pendingRebuildSet.add(section)) {
                    this.pendingRebuilds.addFirst(section);
                }
                break;
            }
        }
    }

    private boolean notInFrustum(RenderSection renderSection) {
        byte frustumRes = renderSection.getChunkArea().inFrustum(renderSection.frustumIndex);
        if (frustumRes > FrustumIntersection.INTERSECT) {
            return true;
        } else if (frustumRes == FrustumIntersection.INTERSECT) {
            return !frustum.testFrustum(renderSection.xOffset, renderSection.yOffset, renderSection.zOffset,
                    renderSection.xOffset + 16, renderSection.yOffset + 16, renderSection.zOffset + 16);
        }
        return false;
    }

    private void visitAdjacentNodes(RenderSection renderSection, byte dirs) {
        dirs &= renderSection.adjDirs;

        this.sectionQueue.ensureCapacity(6);

        RenderSection relativeSection;

        relativeSection = renderSection.adjDown;
        checkToAdd(renderSection, relativeSection, (byte) GraphDirections.DOWN, (byte) GraphDirections.UP, dirs);

        relativeSection = renderSection.adjUp;
        checkToAdd(renderSection, relativeSection, (byte) GraphDirections.UP, (byte) GraphDirections.DOWN, dirs);

        relativeSection = renderSection.adjNorth;
        checkToAdd(renderSection, relativeSection, (byte) GraphDirections.NORTH, (byte) GraphDirections.SOUTH, dirs);

        relativeSection = renderSection.adjSouth;
        checkToAdd(renderSection, relativeSection, (byte) GraphDirections.SOUTH, (byte) GraphDirections.NORTH, dirs);

        relativeSection = renderSection.adjWest;
        checkToAdd(renderSection, relativeSection, (byte) GraphDirections.WEST, (byte) GraphDirections.EAST, dirs);

        relativeSection = renderSection.adjEast;
        checkToAdd(renderSection, relativeSection, (byte) GraphDirections.EAST, (byte) GraphDirections.WEST, dirs);
    }

    private void checkToAdd(RenderSection renderSection, RenderSection relativeSection, byte dir, byte opposite, byte dirs) {
        if ((dirs & (1 << dir)) != 0) {
            addNode(renderSection, relativeSection, dir, opposite);
        }
    }

    private void updateRenderChunksSpectator() {
        while (this.sectionQueue.hasNext()) {
            RenderSection renderSection = this.sectionQueue.poll();

            if (notInFrustum(renderSection))
                continue;

            if (!renderSection.isCompletelyEmpty()) {
                renderSection.getChunkArea().sectionQueue.add(renderSection);
                this.chunkAreaQueue.add(renderSection.getChunkArea());
                this.nonEmptyChunks++;
            }

            if (renderSection.isDirty()) {
                this.rebuildQueue.ensureCapacity(1);
                this.rebuildQueue.add(renderSection);
            }

            byte dirs = (byte) (renderSection.adjDirs & renderSection.getDirections());

            visitAdjacentNodes(renderSection, dirs);
        }
    }

    private void addNode(RenderSection renderSection, RenderSection relativeSection, byte direction, byte opposite) {
        if (relativeSection.getLastFrame() != this.lastFrame) {
            relativeSection.setLastFrame(this.lastFrame);

            relativeSection.mainDir = direction;
            relativeSection.sourceDirs = (byte) (1 << direction);

            final byte steps = (byte) (renderSection.steps + 1);
            relativeSection.directionChanges = (byte) (steps < 10 ? 0 : 127);
            relativeSection.steps = steps;

            relativeSection.directions = (byte) (renderSection.directions & ~(1 << opposite));
            this.sectionQueue.add(relativeSection);
        }

        relativeSection.addDir(direction);

        boolean increase = (renderSection.sourceDirs & 1 << direction) == 0 && !renderSection.isCompletelyEmpty();
        byte dc = increase ? (byte) (renderSection.directionChanges + 1) : renderSection.directionChanges;

        relativeSection.directionChanges = dc < relativeSection.directionChanges ? dc : relativeSection.directionChanges;
    }

    public AreaSetQueue getChunkAreaQueue() {
        return this.chunkAreaQueue;
    }

    public ResettableQueue<RenderSection> getSectionQueue() {
        return this.sectionQueue;
    }

    public ResettableQueue<RenderSection> getBlockEntitiesSections() {
        return this.blockEntitiesSections;
    }

    public short getLastFrame() {
        return lastFrame;
    }

    public boolean hasPendingRebuilds() {
        return !this.pendingRebuilds.isEmpty() || !this.delayedRebuilds.isEmpty();
    }

    public int pendingRebuildCount() {
        return this.pendingRebuilds.size() + this.delayedRebuilds.size();
    }

    public int readyRebuildCount() {
        return this.pendingRebuilds.size();
    }

    public int delayedRebuildCount() {
        return this.delayedRebuilds.size();
    }

    public String getStatistics() {
        int totalSections = this.sectionGrid.getSectionCount();
        int sections = this.sectionQueue.size();
        int renderDistance = WorldRenderer.getInstance().getRenderDistance();
        String tasksInfo = this.taskDispatcher == null ? "null" : this.taskDispatcher.getStats();

        return String.format("Chunks: %d(%d)/%d D: %d rQ: %d+%d, %s", this.nonEmptyChunks, sections,
                totalSections, renderDistance, this.pendingRebuilds.size(), this.delayedRebuilds.size(), tasksInfo);
    }

    public VFrustum getFrustum() {
        return this.frustum;
    }
}

