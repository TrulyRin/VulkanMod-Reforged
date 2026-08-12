package net.vulkanmod.render.optimization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSmoothingSourceTest {
    @Test
    void rebuildSnapshotsUseAPersistentCountAndTimeBudget() throws IOException {
        String graph = source("src/main/java/net/vulkanmod/render/chunk/graph/SectionGraph.java");
        String world = source("src/main/java/net/vulkanmod/render/chunk/WorldRenderer.java");

        assertTrue(graph.contains("ArrayDeque<RenderSection> pendingRebuilds"));
        assertTrue(graph.contains("pendingRebuildSet"));
        assertTrue(graph.contains("DelayedRebuildQueue<RenderSection> delayedRebuilds"));
        assertTrue(graph.contains("promoteEligibleRebuilds"));
        assertTrue(graph.contains("chooseRebuildScheduleTimeBudgetNanos"));
        assertTrue(graph.contains("fixedRebuildTimeBudgetNanos"));
        assertTrue(graph.contains("section.isDirtyFromPlayer()"));
        assertTrue(graph.contains("this.pendingRebuilds.remove(section)"));
        assertTrue(graph.contains("MAX_REBUILD_QUEUE_SCANS_PER_FRAME"));
        assertTrue(graph.contains("taskDispatcher.hasTaskCapacity()"));
        assertTrue(world.contains("sectionGraph.schedulePendingRebuilds();"));
        assertTrue(world.contains("GRAPH_MOVEMENT_THRESHOLD_SQUARED"));

        int schedulerStart = graph.indexOf("public void schedulePendingRebuilds()");
        int schedulerEnd = graph.indexOf("private boolean notInFrustum", schedulerStart);
        assertFalse(graph.substring(schedulerStart, schedulerEnd).contains("scheduleGraphUpdate"));
    }

    @Test
    void dispatcherBoundsBackpressureAndResumesLayerUploads() throws IOException {
        String dispatcher = source("src/main/java/net/vulkanmod/render/chunk/build/TaskDispatcher.java");

        assertTrue(dispatcher.contains("new ArrayBlockingQueue<>("));
        assertTrue(dispatcher.contains("MAX_PENDING_COMPILE_RESULTS"));
        assertTrue(dispatcher.contains("Semaphore taskSignal"));
        assertTrue(dispatcher.contains("shouldServeLowPriority"));
        assertTrue(dispatcher.contains("MAX_QUEUED_BUILD_TASKS"));
        assertTrue(dispatcher.contains("MAX_STALE_RESULTS_PER_FRAME"));
        assertTrue(dispatcher.contains("compileResult.matchesCurrentSection()"));
        assertTrue(dispatcher.contains("failedTasks"));
        assertTrue(dispatcher.contains("recoverFromFailure()"));
        assertTrue(dispatcher.contains("ChunkBuildFailureTracker.record"));
        assertTrue(dispatcher.contains("activeCompileResult"));
        assertTrue(dispatcher.contains("activeRenderLayerIndex"));
        assertTrue(dispatcher.contains("else if (!this.activeCompileResult.matchesCurrentSection())"));
        assertTrue(dispatcher.contains("System.nanoTime() - uploadStartNanos >= uploadTimeBudgetNanos"));
        assertTrue(dispatcher.contains("renderedLayers.remove(renderType)"));
        assertTrue(dispatcher.contains("drawBuffers.stageUpload("));
        assertTrue(dispatcher.contains("drawBuffers.commitFullUpdate("));
        assertTrue(dispatcher.contains("drawBuffers.commitPartialUpdate("));
        assertTrue(dispatcher.contains("fixedUploadTimeBudgetNanos"));
        assertFalse(dispatcher.contains("LockSupport.parkNanos"));
        assertFalse(dispatcher.contains("Long.MAX_VALUE"));
    }

    @Test
    void resumedLayersCommitDrawParametersOnlyAfterTheFullSectionIsReady() throws IOException {
        String buffers = source("src/main/java/net/vulkanmod/render/chunk/buffer/DrawBuffers.java");
        String result = source("src/main/java/net/vulkanmod/render/chunk/build/task/CompileResult.java");

        assertTrue(buffers.contains("class PendingUpload"));
        assertTrue(buffers.contains("commitFullUpdate"));
        assertTrue(buffers.contains("rebindSegment"));
        assertTrue(buffers.contains("deferCurrentSegments"));
        assertTrue(buffers.indexOf("PendingUpload pending = stagedLayers.get(renderType)")
                < buffers.indexOf("stagedLayers.remove(renderType)"));
        assertTrue(result.contains("stagedLayers"));
        assertTrue(result.contains("DrawBuffers.PendingUpload::discard"));
    }

    @Test
    void supersededBuildsAreRejectedByGeneration() throws IOException {
        String section = source("src/main/java/net/vulkanmod/render/chunk/RenderSection.java");
        String result = source("src/main/java/net/vulkanmod/render/chunk/build/task/CompileResult.java");

        assertTrue(section.contains("volatile long buildRevision"));
        assertTrue(section.contains("volatile long transparencyRevision"));
        assertTrue(section.contains("volatile long compiledBuildRevision"));
        assertTrue(section.contains("compiledBuildRevision != this.compileStatus.buildRevision"));
        assertTrue(section.contains("MAX_BUILD_FAILURE_RETRIES"));
        assertTrue(section.contains("consecutiveBuildFailures"));
        assertTrue(section.contains("++this.compileStatus.buildRevision"));
        assertTrue(result.contains("getBuildRevision() == this.buildRevision"));
        assertTrue(result.contains("getTransparencyRevision() == this.transparencyRevision"));
        assertTrue(result.contains("matchesCurrentLocation()"));
    }

    @Test
    void workerAndBoundaryHotPathsAvoidPerChunkGarbage() throws IOException {
        String build = source("src/main/java/net/vulkanmod/render/chunk/build/task/BuildTask.java");
        String resources = source("src/main/java/net/vulkanmod/render/chunk/build/thread/BuilderResources.java");
        String region = source("src/main/java/net/vulkanmod/render/chunk/build/RenderRegionBuilder.java");
        String section = source("src/main/java/net/vulkanmod/render/chunk/RenderSection.java");
        String area = source("src/main/java/net/vulkanmod/render/chunk/buffer/AreaBuffer.java");

        assertTrue(build.contains("builderResources.randomSource"));
        assertFalse(build.contains("RandomSource.create()"));
        assertTrue(build.contains("if (this.cancelled.get()"));
        assertTrue(resources.contains("new BlockState[RenderRegion.BLOCK_COUNT]"));
        assertFalse(region.contains("new DataLayer[RenderRegion.SIZE][]"));
        assertTrue(region.contains("new DataLayer[RenderRegion.SIZE * RenderRegion.LIGHT_LAYER_COUNT]"));
        assertTrue(section.indexOf("if (fullSet.isEmpty())")
                < section.indexOf("addedBlockEntities = Sets.newHashSet(fullSet)"));
        assertTrue(area.contains("occupiedHighWaterBytes"));
        assertTrue(area.contains("this.size >> 1"));
        assertTrue(area.contains("rollbackReservation(segment)"));
        assertTrue(area.contains("this.usedSegments.remove(segment.offset)"));
    }

    @Test
    void adaptiveHistoryResetsAcrossRendererAndWorldTransitions() throws IOException {
        String world = source("src/main/java/net/vulkanmod/render/chunk/WorldRenderer.java");
        String renderer = source("src/main/java/net/vulkanmod/vulkan/Renderer.java");
        int allChanged = world.indexOf("public void allChanged()");
        int setLevel = world.indexOf("public void setLevel", allChanged);
        int nextMethod = world.indexOf("public void addOnAllChangedCallback", setLevel);

        assertTrue(world.substring(allChanged, setLevel).contains("AdaptiveChunkUploadBudget.reset();"));
        assertTrue(world.substring(setLevel, nextMethod).contains("AdaptiveChunkUploadBudget.reset();"));
        assertTrue(renderer.contains("Initializer.CONFIG.adaptiveChunkUploads && chunkUploadWork"));
        assertTrue(renderer.contains("AdaptiveChunkUploadBudget.recordUploadWork(uploadPhaseNanos)"),
                "adaptive pressure must use the complete upload phase, including submission");
        assertTrue(renderer.contains("AdaptiveChunkUploadBudget.recordGpuBackpressure"),
                "recent chunk-caused Vulkan waits must feed the adaptive pressure controller");
        assertTrue(world.contains("createThreads(this.minecraft.hasSingleplayerServer())"),
                "single-player must reserve CPU for the integrated server");
    }

    @Test
    void completionWaitsForPersistentAndDispatchedWork() throws IOException {
        String world = source("src/main/java/net/vulkanmod/render/chunk/WorldRenderer.java");
        String graph = source("src/main/java/net/vulkanmod/render/chunk/graph/SectionGraph.java");
        String mixin = source("src/main/java/net/vulkanmod/mixin/chunk/LevelRendererMixin.java");
        String dispatcher = source("src/main/java/net/vulkanmod/render/chunk/build/TaskDispatcher.java");

        assertTrue(graph.contains("boolean hasPendingRebuilds()"));
        assertTrue(world.contains("boolean hasPendingSectionWork()"));
        assertTrue(mixin.contains("!this.worldRenderer.hasPendingSectionWork()"));
        assertTrue(dispatcher.contains("this.highPriorityTasks.isEmpty()"));
        assertTrue(dispatcher.contains("this.lowPriorityTasks.isEmpty()"));
        assertTrue(dispatcher.contains("this.activeTasks.isEmpty()"));
    }

    private static String source(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
