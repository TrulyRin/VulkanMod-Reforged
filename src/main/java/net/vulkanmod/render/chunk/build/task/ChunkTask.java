package net.vulkanmod.render.chunk.build.task;

import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.RenderRegion;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ChunkTask {
    public static final boolean BENCH = true;

    protected static TaskDispatcher taskDispatcher;

    public static BuildTask createBuildTask(RenderSection renderSection, RenderRegion renderRegion,
                                            boolean highPriority, long buildRevision) {
        return new BuildTask(renderSection, renderRegion, highPriority, buildRevision);
    }

    protected AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    protected final RenderSection section;
    protected final long buildRevision;
    public boolean highPriority = false;

    ChunkTask(RenderSection renderSection, long buildRevision) {
        this.section = renderSection;
        this.buildRevision = buildRevision;
    }

    public abstract String name();

    public abstract Result runTask(BuilderResources builderResources);

    public void markStarted() {
        this.started.set(true);
    }

    public void cancel() {
        this.cancelled.set(true);

        if (!this.started.get()) {
            this.clearPayload();
        }
    }

    public void discard() {
        this.cancelled.set(true);
        this.clearPayload();
    }

    public boolean isCancelled() {
        return this.cancelled.get();
    }

    public void recoverFromFailure() {
    }

    protected void clearPayload() {
    }

    public static void setTaskDispatcher(TaskDispatcher dispatcher) {
        taskDispatcher = dispatcher;
    }

    public enum Result {
        CANCELLED,
        SUCCESSFUL
    }
}
