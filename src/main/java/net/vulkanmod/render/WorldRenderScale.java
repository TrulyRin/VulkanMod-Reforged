package net.vulkanmod.render;

import net.vulkanmod.config.RenderScale;

import java.util.Objects;

                                                                                                
public final class WorldRenderScale<T> implements AutoCloseable {
    public interface Backend<T> {
        T create(int width, int height, T original);

        void clear(T target);

        void endRendering();

        void resolve(T source, T destination);

        void release(T target);
    }

    private final Backend<T> backend;
    private T scaledTarget;
    private T activeTarget;
    private Thread activeThread;
    private T originalTarget;
    private int scaledWidth;
    private int scaledHeight;
    private int scale;
    private boolean releaseAfterRendering;

    public WorldRenderScale(Backend<T> backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public T targetOrOriginal(T original) {
        return this.activeTarget != null && this.activeThread == Thread.currentThread()
                ? this.activeTarget : original;
    }

    public void render(T original, int width, int height, int requestedScale,
                       boolean enabled, Runnable worldRenderer) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(worldRenderer, "worldRenderer");
        if (this.activeTarget != null) {
            if (this.activeThread != Thread.currentThread()) {
                throw new IllegalStateException("A scaled world render is active on another thread");
            }
                                                                                                  
                                                                         
            worldRenderer.run();
            return;
        }

        int requested = RenderScale.clamp(requestedScale);
        if (!enabled || !RenderScale.isScaled(requested) || width <= 0 || height <= 0) {
            this.close();
            worldRenderer.run();
            return;
        }

        this.ensureTarget(original, width, height, requested);
        this.activeThread = Thread.currentThread();
        this.activeTarget = this.scaledTarget;
        Throwable failure = null;
        try {
            this.backend.clear(this.scaledTarget);
            worldRenderer.run();
        } catch (RuntimeException | Error worldFailure) {
            failure = worldFailure;
        } finally {
                                                                                                       
            this.activeTarget = null;
            this.activeThread = null;
            try {
                this.backend.endRendering();
            } catch (RuntimeException | Error endFailure) {
                if (failure == null) {
                    failure = endFailure;
                } else if (failure != endFailure) {
                    failure.addSuppressed(endFailure);
                }
            }
        }

        if (failure != null) {
            this.releaseAfterFailure(failure);
            throwUnchecked(failure);
        }

        if (this.releaseAfterRendering) {
            this.releaseTarget();
            return;
        }

        try {
            this.backend.resolve(this.scaledTarget, original);
        } catch (RuntimeException | Error resolveFailure) {
            this.releaseAfterFailure(resolveFailure);
            throw resolveFailure;
        }
    }

    private void ensureTarget(T original, int width, int height, int requestedScale) {
        int nextWidth = RenderScale.scaleDimension(width, requestedScale);
        int nextHeight = RenderScale.scaleDimension(height, requestedScale);
        if (this.scaledTarget != null && this.originalTarget == original
                && this.scaledWidth == nextWidth && this.scaledHeight == nextHeight
                && this.scale == requestedScale) {
            return;
        }

        this.releaseTarget();
        this.scaledTarget = Objects.requireNonNull(
                this.backend.create(nextWidth, nextHeight, original), "scaled target");
        this.originalTarget = original;
        this.scaledWidth = nextWidth;
        this.scaledHeight = nextHeight;
        this.scale = requestedScale;
    }

    @Override
    public void close() {
        if (this.activeTarget != null) {
                                                                                                               
            this.releaseAfterRendering = true;
        } else {
            this.releaseTarget();
        }
    }

    private void releaseTarget() {
        T owned = this.scaledTarget;
        this.scaledTarget = null;
        this.originalTarget = null;
        this.releaseAfterRendering = false;
        if (owned != null) {
            this.backend.release(owned);
        }
    }

    private void releaseAfterFailure(Throwable failure) {
        try {
            this.releaseTarget();
        } catch (RuntimeException | Error releaseFailure) {
            if (releaseFailure != failure) {
                failure.addSuppressed(releaseFailure);
            }
        }
    }

    private static void throwUnchecked(Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        throw (RuntimeException) failure;
    }
}
