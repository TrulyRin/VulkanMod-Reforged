package net.vulkanmod.render.chunk.build;

import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.build.task.CompileResult;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.optimization.AdaptiveChunkUploadBudget;
import net.vulkanmod.render.optimization.ChunkBuildFailureTracker;
import net.vulkanmod.render.optimization.ChunkFrameDiagnostics;
import net.vulkanmod.render.optimization.ChunkSchedulingPolicy;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;

import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskDispatcher {
    private final BlockingQueue<CompileResult> compileResults = new ArrayBlockingQueue<>(
            ChunkSchedulingPolicy.MAX_PENDING_COMPILE_RESULTS);
    public final ThreadBuilderPack fixedBuffers;

    private volatile boolean stopThreads;
    private boolean performedUploadWorkLastUpdate;
    private Thread[] threads;
    private BuilderResources[] resources;
    private final AtomicInteger idleThreads = new AtomicInteger();
    private final Queue<ChunkTask> highPriorityTasks = new ConcurrentLinkedQueue<>();
    private final Queue<ChunkTask> lowPriorityTasks = new ConcurrentLinkedQueue<>();
    private final Set<ChunkTask> activeTasks = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkTask> failedTasks = new ConcurrentLinkedQueue<>();
    private final Semaphore taskSignal = new Semaphore(0);
    private final AtomicInteger loggedTaskFailures = new AtomicInteger();
    private int consecutiveHighPriorityTasks;

    private CompileResult activeCompileResult;
    private int activeRenderLayerIndex;

    public TaskDispatcher() {
        this.fixedBuffers = new ThreadBuilderPack();

        this.stopThreads = true;
    }

    public void createThreads() {
        createThreads(false);
    }

    public void createThreads(boolean integratedServer) {
        int n = ChunkSchedulingPolicy.resolveBuilderThreadCount(Initializer.CONFIG.builderThreads,
                Runtime.getRuntime().availableProcessors(), integratedServer);
        createThreads(n, integratedServer);
    }

    public void createThreads(int n) {
        createThreads(n, false);
    }

    private void createThreads(int n, boolean integratedServer) {
        if(!this.stopThreads) {
            this.stopThreads();
        }

        n = Math.max(1, Math.min(ChunkSchedulingPolicy.MAX_BUILDER_THREADS, n));
        this.stopThreads = false;
        this.taskSignal.drainPermits();
        this.consecutiveHighPriorityTasks = 0;

        if(this.resources != null) {
            closeResources(this.resources);
        }

        this.threads = new Thread[n];
        this.resources = new BuilderResources[n];

        for (int i = 0; i < n; i++) {
            BuilderResources builderResources = new BuilderResources();
            Thread thread = new Thread(() -> runTaskThread(builderResources),
                    "Builder-" + i);
            thread.setPriority(Thread.MIN_PRIORITY);

            this.threads[i] = thread;
            this.resources[i] = builderResources;
            thread.start();
        }

        Initializer.LOGGER.info("Chunk builders: {} threads ({} logical processors, {} mode)",
                n, Runtime.getRuntime().availableProcessors(),
                integratedServer ? "single-player" : "remote");
    }

    private void runTaskThread(BuilderResources builderResources) {
        while(!this.stopThreads) {
            try {
                this.idleThreads.incrementAndGet();
                this.taskSignal.acquire();
            } catch (InterruptedException e) {
                if (this.stopThreads) {
                    return;
                }
                continue;
            } finally {
                this.idleThreads.decrementAndGet();
            }

            if (this.stopThreads) {
                return;
            }

            ChunkTask task = this.pollTask();
            if(task == null) {
                continue;
            }

            this.activeTasks.add(task);
            try {
                if (this.stopThreads) {
                    task.cancel();
                    return;
                }
                task.markStarted();
                long buildStart = System.nanoTime();
                task.runTask(builderResources);
                ChunkFrameDiagnostics.recordBuild(System.nanoTime() - buildStart);
            } catch (Throwable throwable) {
                builderResources.clear();
                if (throwable instanceof VirtualMachineError virtualMachineError) {
                    throw virtualMachineError;
                }
                ChunkBuildFailureTracker.record(task.name(), throwable);
                this.failedTasks.offer(task);
                if (this.loggedTaskFailures.getAndIncrement() < 4) {
                    Initializer.LOGGER.error("Chunk builder task {} failed; scheduling a safe retry", task.name(), throwable);
                }
            } finally {
                this.activeTasks.remove(task);
            }
        }
    }

    public synchronized boolean schedule(ChunkTask chunkTask) {
        if(chunkTask == null)
            return false;

        if (this.stopThreads) {
            chunkTask.discard();
            return false;
        }

        if (this.highPriorityTasks.size() + this.lowPriorityTasks.size()
                >= ChunkSchedulingPolicy.MAX_QUEUED_BUILD_TASKS) {
            chunkTask.discard();
            return false;
        }

        if (chunkTask.highPriority) {
            this.highPriorityTasks.offer(chunkTask);
        } else {
            this.lowPriorityTasks.offer(chunkTask);
        }

        this.taskSignal.release();
        return true;
    }

    public synchronized boolean hasTaskCapacity() {
        return !this.stopThreads
                && this.highPriorityTasks.size() + this.lowPriorityTasks.size()
                < ChunkSchedulingPolicy.MAX_QUEUED_BUILD_TASKS;
    }

    @Nullable
    private synchronized ChunkTask pollTask() {
        boolean highAvailable = !this.highPriorityTasks.isEmpty();
        boolean lowAvailable = !this.lowPriorityTasks.isEmpty();
        ChunkTask task;

        if (ChunkSchedulingPolicy.shouldServeLowPriority(
                this.consecutiveHighPriorityTasks, highAvailable, lowAvailable)) {
            task = this.lowPriorityTasks.poll();
            this.consecutiveHighPriorityTasks = 0;
        } else {
            task = this.highPriorityTasks.poll();
            if (task != null) {
                this.consecutiveHighPriorityTasks++;
            } else {
                task = this.lowPriorityTasks.poll();
                this.consecutiveHighPriorityTasks = 0;
            }
        }

        if (task != null && task.isCancelled()) {
            if (task.highPriority && this.consecutiveHighPriorityTasks > 0) {
                this.consecutiveHighPriorityTasks--;
            }
            task.discard();
            return null;
        }

        return task;
    }

    public void stopThreads() {
        if(this.stopThreads)
            return;

        Thread[] threadsToJoin;
        synchronized (this) {
            if (this.stopThreads) {
                return;
            }
            this.stopThreads = true;
            threadsToJoin = this.threads;
            this.activeTasks.forEach(ChunkTask::cancel);
            this.taskSignal.release(threadsToJoin.length);
        }

        boolean interrupted = false;
        for (Thread thread : threadsToJoin) {
            boolean joined = false;
            while (!joined) {
                try {
                    thread.join();
                    joined = true;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }

        this.clearBatchQueue();
        closeResources(this.resources);
        this.fixedBuffers.closeAll();
        this.resources = null;
        this.threads = null;

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

    }

    private static void closeResources(BuilderResources[] resourcesArray) {
        if (resourcesArray == null) {
            return;
        }

        for (BuilderResources resources : resourcesArray) {
            if (resources != null) {
                resources.close();
            }
        }
    }

    public boolean updateSections() {
        this.performedUploadWorkLastUpdate = false;
        this.recoverFailedTasks();

        boolean completedFullUpdate = false;
        int completedUpdatesThisFrame = 0;
        int discardedResultsThisFrame = 0;
        int uploadStepsThisFrame = 0;
        int maxUploadsPerFrame = this.getMaxUploadsPerFrame();
        int pendingUploads = this.compileResults.size() + (this.activeCompileResult == null ? 0 : 1);
        long uploadStartNanos = System.nanoTime();
        long uploadTimeBudgetNanos = Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.uploadTimeBudgetNanos(pendingUploads)
                : ChunkSchedulingPolicy.fixedUploadTimeBudgetNanos();
                                                                            
                                                                            
        long uploadByteBudget = Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.uploadByteBudget()
                : ChunkSchedulingPolicy.fixedUploadByteBudget();
        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        long uploadStartBytes = stagingBuffer.getUsedBytes();

        while(completedUpdatesThisFrame < maxUploadsPerFrame) {
            if (this.activeCompileResult == null) {
                CompileResult result = this.compileResults.poll();
                if (result == null) {
                    break;
                }

                if (!result.matchesCurrentSection()) {
                    result.releaseBuffers();
                    discardedResultsThisFrame++;
                    if (discardedResultsThisFrame >= ChunkSchedulingPolicy.MAX_STALE_RESULTS_PER_FRAME
                            || System.nanoTime() - uploadStartNanos >= uploadTimeBudgetNanos) {
                        break;
                    }
                    continue;
                }

                this.activeCompileResult = result;
                this.activeRenderLayerIndex = 0;
            } else if (!this.activeCompileResult.matchesCurrentSection()) {
                this.activeCompileResult.releaseBuffers();
                this.activeCompileResult = null;
                this.activeRenderLayerIndex = 0;
                discardedResultsThisFrame++;
                if (discardedResultsThisFrame >= ChunkSchedulingPolicy.MAX_STALE_RESULTS_PER_FRAME
                        || System.nanoTime() - uploadStartNanos >= uploadTimeBudgetNanos) {
                    break;
                }
                continue;
            }

                                                                           
                                                                            
                                                                       

            boolean completed = this.uploadNextLayer(this.activeCompileResult);
            uploadStepsThisFrame++;
            if (completed) {
                boolean fullUpdate = this.activeCompileResult.fullUpdate;
                this.activeCompileResult.releaseBuffers();
                this.activeCompileResult = null;
                this.activeRenderLayerIndex = 0;
                completedUpdatesThisFrame++;
                completedFullUpdate |= fullUpdate;
            }

            if (uploadStepsThisFrame > 0
                    && (System.nanoTime() - uploadStartNanos >= uploadTimeBudgetNanos
                    || stagingBuffer.getUsedBytes() - uploadStartBytes >= uploadByteBudget)) {
                break;
            }
        }

        long stagedBytes = stagingBuffer.getUsedBytes() - uploadStartBytes;
        int pendingAfter = this.compileResults.size() + (this.activeCompileResult == null ? 0 : 1);
        this.performedUploadWorkLastUpdate = uploadStepsThisFrame > 0;
        ChunkFrameDiagnostics.recordUploads(completedUpdatesThisFrame, stagedBytes, pendingAfter);

        return completedFullUpdate;
    }

    public boolean performedUploadWorkLastUpdate() {
        return this.performedUploadWorkLastUpdate;
    }

    private int getMaxUploadsPerFrame() {
        int configuredUploads = Initializer.CONFIG.chunkUploadsPerFrame;
        int pendingUploads = this.compileResults.size() + (this.activeCompileResult == null ? 0 : 1);
        return Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.chooseBudget(configuredUploads, pendingUploads)
                : clampMaxUploadsPerFrame(configuredUploads);
    }

    public static int clampMaxUploadsPerFrame(int uploadsPerFrame) {
        return ChunkSchedulingPolicy.clampUploadCeiling(uploadsPerFrame);
    }

    public boolean scheduleSectionUpdate(CompileResult compileResult, ChunkTask owner) {
        try {
            while (!this.stopThreads && !owner.isCancelled() && compileResult.matchesCurrentSection()) {
                if (this.compileResults.offer(compileResult, 10L, TimeUnit.MILLISECONDS)) {
                    return true;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        compileResult.releaseBuffers();
        return false;
    }

    private void recoverFailedTasks() {
        int recovered = 0;
        ChunkTask failedTask;
        while (recovered < ChunkSchedulingPolicy.MAX_FAILED_TASK_RECOVERIES_PER_FRAME
                && (failedTask = this.failedTasks.poll()) != null) {
            failedTask.recoverFromFailure();
            recovered++;
        }
    }

    private boolean uploadNextLayer(CompileResult compileResult) {
        RenderSection section = compileResult.renderSection;
        ChunkArea renderArea = section.getChunkArea();
        DrawBuffers drawBuffers = renderArea.getDrawBuffers();

        if(compileResult.fullUpdate) {
            while (this.activeRenderLayerIndex < TerrainRenderType.VALUES.length) {
                TerrainRenderType renderType = TerrainRenderType.VALUES[this.activeRenderLayerIndex++];
                UploadBuffer uploadBuffer = compileResult.renderedLayers.remove(renderType);

                if(uploadBuffer != null) {
                    DrawBuffers.PendingUpload pendingUpload = drawBuffers.stageUpload(
                            section, uploadBuffer, renderType, true);
                    compileResult.stagedLayers.put(renderType, pendingUpload);
                    if (this.activeRenderLayerIndex == TerrainRenderType.VALUES.length) {
                        drawBuffers.commitFullUpdate(section, compileResult.stagedLayers);
                        compileResult.updateSection();
                        return true;
                    }
                    return false;
                }
            }

            drawBuffers.commitFullUpdate(section, compileResult.stagedLayers);
            compileResult.updateSection();
            return true;
        }

        UploadBuffer uploadBuffer = compileResult.renderedLayers.remove(TerrainRenderType.TRANSLUCENT);
        if (uploadBuffer != null) {
            DrawBuffers.PendingUpload pendingUpload = drawBuffers.stageUpload(
                    section, uploadBuffer, TerrainRenderType.TRANSLUCENT, false);
            compileResult.stagedLayers.put(TerrainRenderType.TRANSLUCENT, pendingUpload);
            drawBuffers.commitPartialUpdate(section, pendingUpload);
            compileResult.stagedLayers.remove(TerrainRenderType.TRANSLUCENT);
        }
        return true;
    }

    public boolean isIdle() {
        return this.threads != null
                && this.idleThreads.get() == this.threads.length
                && this.highPriorityTasks.isEmpty()
                && this.lowPriorityTasks.isEmpty()
                && this.activeTasks.isEmpty()
                && this.failedTasks.isEmpty()
                && this.compileResults.isEmpty()
                && this.activeCompileResult == null;
    }

    public void clearBatchQueue() {
        while(!this.highPriorityTasks.isEmpty()) {
            ChunkTask chunkTask = this.highPriorityTasks.poll();
            if (chunkTask != null) {
                chunkTask.discard();
            }
        }

        while(!this.lowPriorityTasks.isEmpty()) {
            ChunkTask chunkTask = this.lowPriorityTasks.poll();
            if (chunkTask != null) {
                chunkTask.discard();
            }
        }

        CompileResult compileResult;
        while ((compileResult = this.compileResults.poll()) != null) {
            compileResult.releaseBuffers();
        }

        if (this.activeCompileResult != null) {
            this.activeCompileResult.releaseBuffers();
            this.activeCompileResult = null;
            this.activeRenderLayerIndex = 0;
        }

        this.failedTasks.clear();

        this.taskSignal.drainPermits();
        this.consecutiveHighPriorityTasks = 0;
    }

    public String getStats() {
        int taskCount = highPriorityTasks.size() + lowPriorityTasks.size();
        int uploadCount = this.compileResults.size() + (this.activeCompileResult == null ? 0 : 1);
        return String.format("iT: %d Ts: %d uQ: %d", this.idleThreads.get(), taskCount, uploadCount);
    }

    public BuilderResources[] getResourcesArray() {
        return resources;
    }
}
