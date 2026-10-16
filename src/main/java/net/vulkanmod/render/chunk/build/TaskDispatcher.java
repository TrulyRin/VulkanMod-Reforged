package net.vulkanmod.render.chunk.build;

import com.google.common.collect.Queues;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.build.task.ChunkTask;
import net.vulkanmod.render.chunk.build.task.CompileResult;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.optimization.AdaptiveChunkUploadBudget;
import net.vulkanmod.render.vertex.TerrainRenderType;

import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.concurrent.locks.LockSupport;

public class TaskDispatcher {
    private static final int MAX_PENDING_COMPILE_RESULTS = 128;
    private final Queue<CompileResult> compileResults = Queues.newLinkedBlockingDeque();
    public final ThreadBuilderPack fixedBuffers;

    private volatile boolean stopThreads;
    private Thread[] threads;
    private BuilderResources[] resources;
    private int idleThreads;
    private final Queue<ChunkTask> highPriorityTasks = Queues.newConcurrentLinkedQueue();
    private final Queue<ChunkTask> lowPriorityTasks = Queues.newConcurrentLinkedQueue();

    public TaskDispatcher() {
        this.fixedBuffers = new ThreadBuilderPack();

        this.stopThreads = true;
    }

    public void createThreads() {
        int n = Math.max((Runtime.getRuntime().availableProcessors() - 1) / 2, 1);
        createThreads(n);
    }

    public void createThreads(int n) {
        if(!this.stopThreads) {
            this.stopThreads();
        }

        this.stopThreads = false;

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
    }

    private void runTaskThread(BuilderResources builderResources) {
        while(!this.stopThreads) {
            ChunkTask task = this.pollTask();

            if(task == null)
                synchronized (this) {
                    try {
                        this.idleThreads++;
                        this.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    this.idleThreads--;
                }

            if(task == null)
                continue;

            task.markStarted();
            task.runTask(builderResources);
        }
    }

    public void schedule(ChunkTask chunkTask) {
        if(chunkTask == null)
            return;

        if (chunkTask.highPriority) {
            this.highPriorityTasks.offer(chunkTask);
        } else {
            this.lowPriorityTasks.offer(chunkTask);
        }

        synchronized (this) {
            this.notify();
        }
    }

    @Nullable
    private ChunkTask pollTask() {
        ChunkTask task = this.highPriorityTasks.poll();

        if(task == null)
            task = this.lowPriorityTasks.poll();

        return task;
    }

    public void stopThreads() {
        if(this.stopThreads)
            return;

        this.stopThreads = true;

        synchronized (this) {
            this.notifyAll();
        }

        for (Thread thread : this.threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        this.clearBatchQueue();
        closeResources(this.resources);
        this.fixedBuffers.closeAll();
        this.resources = null;
        this.threads = null;

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
        CompileResult result;
        boolean flag = false;
        int uploadsThisFrame = 0;
        int maxUploadsPerFrame = this.getMaxUploadsPerFrame();
        int pendingUploads = this.compileResults.size();
        long uploadStartNanos = System.nanoTime();
        long uploadTimeBudgetNanos = Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.uploadTimeBudgetNanos(pendingUploads)
                : Long.MAX_VALUE;

        while(uploadsThisFrame < maxUploadsPerFrame && (result = this.compileResults.poll()) != null) {
            flag = true;
            doSectionUpdate(result);
            uploadsThisFrame++;

            if (uploadsThisFrame > 0 && System.nanoTime() - uploadStartNanos >= uploadTimeBudgetNanos) {
                break;
            }
        }

        return flag;
    }

    private int getMaxUploadsPerFrame() {
        int configuredUploads = Initializer.CONFIG.chunkUploadsPerFrame;
        return Initializer.CONFIG.adaptiveChunkUploads
                ? AdaptiveChunkUploadBudget.chooseBudget(configuredUploads, this.compileResults.size())
                : clampMaxUploadsPerFrame(configuredUploads);
    }

    public static int clampMaxUploadsPerFrame(int uploadsPerFrame) {
        return Math.max(1, Math.min(16, uploadsPerFrame));
    }

    public void scheduleSectionUpdate(CompileResult compileResult) {
        while (!this.stopThreads && this.compileResults.size() >= MAX_PENDING_COMPILE_RESULTS) {
            LockSupport.parkNanos(1_000_000L);
        }

        if (this.stopThreads) {
            compileResult.releaseBuffers();
            return;
        }

        this.compileResults.add(compileResult);
    }

    private void doSectionUpdate(CompileResult compileResult) {
        if (!compileResult.matchesCurrentSection()) {
            compileResult.releaseBuffers();
            return;
        }

        RenderSection section = compileResult.renderSection;
        ChunkArea renderArea = section.getChunkArea();
        DrawBuffers drawBuffers = renderArea.getDrawBuffers();

        if(compileResult.fullUpdate) {
            var renderLayers = compileResult.renderedLayers;
            for(TerrainRenderType renderType : TerrainRenderType.VALUES) {
                UploadBuffer uploadBuffer = renderLayers.get(renderType);

                if(uploadBuffer != null) {
                    drawBuffers.upload(section, uploadBuffer, renderType);
                } else {
                    section.getDrawParameters(renderType).reset(renderArea, renderType);
                }
            }

            compileResult.updateSection();
        }
        else {
            UploadBuffer uploadBuffer = compileResult.renderedLayers.get(TerrainRenderType.TRANSLUCENT);
            drawBuffers.upload(section, uploadBuffer, TerrainRenderType.TRANSLUCENT);
        }
    }

    public boolean isIdle() { return this.idleThreads == this.threads.length && this.compileResults.isEmpty(); }

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
    }

    public String getStats() {
        int taskCount = highPriorityTasks.size() + lowPriorityTasks.size();
        return String.format("iT: %d Ts: %d uQ: %d", this.idleThreads, taskCount, this.compileResults.size());
    }

    public BuilderResources[] getResourcesArray() {
        return resources;
    }
}

