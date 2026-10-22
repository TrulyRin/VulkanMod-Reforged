package net.vulkanmod.vulkan.pass;

import java.util.Objects;


final class MainPassTargets<T, P> implements AutoCloseable {
    interface Backend<T, P> {
        T createScaled(int width, int height, int scale);
        P createPasses(T target);
        void retireScaled(T target);
        void retirePasses(P passes);
    }

    private final T swapchain;
    private final P swapchainPasses;
    private final Backend<T, P> backend;
    private T selected;
    private P passes;
    private T scaled;
    private P scaledPasses;
    private int width = -1;
    private int height = -1;
    private int scale = -1;
    private boolean closed;

    MainPassTargets(T swapchain, Backend<T, P> backend) {
        this.swapchain = Objects.requireNonNull(swapchain);
        this.backend = Objects.requireNonNull(backend);
        this.swapchainPasses = Objects.requireNonNull(backend.createPasses(swapchain));
        activateSwapchain();
    }

    void select(boolean retainScaled, boolean useScaledNow, int width, int height, int scale) {
        requireOpen();
        if (useScaledNow && !retainScaled) throw new IllegalArgumentException("Scaled target is disabled");
        if (!retainScaled) {
            activateSwapchain();
            discardScaled();
            return;
        }
        if (!useScaledNow) {
            activateSwapchain();
            return;
        }
        if (width <= 0 || height <= 0 || scale <= 0) throw new IllegalArgumentException("Invalid scaled extent or scale");
        if (scaled == null || this.width != width || this.height != height || this.scale != scale) {
            T candidate = Objects.requireNonNull(backend.createScaled(width, height, scale));
            P candidatePasses;
            try {
                candidatePasses = Objects.requireNonNull(backend.createPasses(candidate));
            } catch (RuntimeException | Error failure) {
                try { backend.retireScaled(candidate); }
                catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
                throw failure;
            }
            T previous = scaled;
            P previousPasses = scaledPasses;
            scaled = candidate;
            scaledPasses = candidatePasses;
            this.width = width;
            this.height = height;
            this.scale = scale;
            selected = scaled;
            passes = scaledPasses;
            throwFailure(retire(previous, previousPasses, null));
            return;
        }
        selected = scaled;
        passes = scaledPasses;
    }

    void resolveForGui() {
        requireOpen();
        activateSwapchain();
    }

    private void activateSwapchain() {
        selected = swapchain;
        passes = swapchainPasses;
    }

    private void discardScaled() {
        T previous = scaled;
        P previousPasses = scaledPasses;
        scaled = null;
        scaledPasses = null;
        width = height = scale = -1;
        throwFailure(retire(previous, previousPasses, null));
    }

    private Throwable retire(T target, P pair, Throwable failure) {
        if (target != null) {
            try { backend.retireScaled(target); }
            catch (RuntimeException | Error cleanup) { failure = collect(failure, cleanup); }
        }
        if (pair != null) {
            try { backend.retirePasses(pair); }
            catch (RuntimeException | Error cleanup) { failure = collect(failure, cleanup); }
        }
        return failure;
    }

    private static Throwable collect(Throwable failure, Throwable cleanup) {
        if (failure == null) return cleanup;
        if (failure != cleanup) failure.addSuppressed(cleanup);
        return failure;
    }

    private static void throwFailure(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

    T target() { return selected; }
    P passes() { return passes; }
    P swapchainPasses() { return swapchainPasses; }
    P retainedScaledPasses() { return scaledPasses; }
    boolean isScaled() { return scaled != null && selected == scaled; }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Main pass targets are closed");
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        T previous = scaled;
        P previousPasses = scaledPasses;
        scaled = null;
        scaledPasses = null;
        activateSwapchain();
        Throwable failure = retire(previous, previousPasses, null);
        try { backend.retirePasses(swapchainPasses); }
        catch (RuntimeException | Error cleanup) { failure = collect(failure, cleanup); }
        throwFailure(failure);
    }
}
