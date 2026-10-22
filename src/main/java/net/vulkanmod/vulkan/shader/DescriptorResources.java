package net.vulkanmod.vulkan.shader;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;


final class DescriptorResources implements AutoCloseable {
    interface Backend {
        IntBuffer allocateOffsets(int count);
        void freeOffsets(IntBuffer offsets);
        long createPool(int capacity);
        LongBuffer allocateSetStorage(int capacity);
        void allocateSets(long pool, int capacity, LongBuffer storage);
        void freeSetStorage(LongBuffer storage);
        void destroyPool(long pool);
        void retirePool(long pool);
    }

    private final Backend backend;
    private IntBuffer offsets;
    private Pair pair;
    private boolean closed;

    private DescriptorResources(Backend backend) { this.backend = Objects.requireNonNull(backend, "backend"); }

    static DescriptorResources create(Backend backend, int capacity, int offsetCount) {
        if (capacity <= 0 || offsetCount < 0) throw new IllegalArgumentException("Invalid descriptor allocation size");
        DescriptorResources resources = new DescriptorResources(backend);
        try {
            resources.offsets = Objects.requireNonNull(backend.allocateOffsets(offsetCount), "offset storage");
            if (resources.offsets.capacity() < offsetCount) throw new IllegalStateException("Insufficient offset storage");
            resources.pair = Pair.create(backend, capacity);
            return resources;
        } catch (RuntimeException | Error failure) {
            try { resources.close(); }
            catch (RuntimeException | Error cleanup) { suppress(failure, cleanup); }
            throw failure;
        }
    }

    IntBuffer offsets() { requireOpen(); return offsets; }
    LongBuffer sets() { requireOpen(); return pair.sets; }
    int capacity() { requireOpen(); return pair.capacity; }


    void grow(int capacity) {
        requireOpen();
        if (capacity <= pair.capacity) throw new IllegalArgumentException("Descriptor growth must increase capacity");
        Pair replacement = Pair.create(backend, capacity);
        Pair previous = pair;
        try {

            backend.retirePool(previous.pool);
        } catch (RuntimeException | Error failure) {
            try { replacement.close(backend); }
            catch (RuntimeException | Error cleanup) { suppress(failure, cleanup); }
            throw failure;
        }
        previous.pool = 0;
        pair = replacement;
        previous.close(backend);
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        Pair oldPair = pair;
        IntBuffer oldOffsets = offsets;
        pair = null;
        offsets = null;
        Throwable failure = null;
        if (oldPair != null) {
            try { oldPair.close(backend); }
            catch (RuntimeException | Error cleanup) { failure = cleanup; }
        }
        if (oldOffsets != null) {
            try { backend.freeOffsets(oldOffsets); }
            catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        }
        rethrow(failure);
    }

    static <T> T[] createFrames(int count, IntFunction<T[]> arrayFactory, IntFunction<T> factory, Consumer<T> cleanup) {
        if (count < 0) throw new IllegalArgumentException("Negative descriptor frame count");
        Objects.requireNonNull(factory, "frame factory");
        Objects.requireNonNull(cleanup, "frame cleanup");
        T[] frames = Objects.requireNonNull(arrayFactory.apply(count), "frame array");
        if (frames.length != count) throw new IllegalArgumentException("Incorrect descriptor frame array size");
        try {
            for (int frame = 0; frame < count; frame++) {
                frames[frame] = Objects.requireNonNull(factory.apply(frame), "descriptor frame");
            }
            return frames;
        } catch (RuntimeException | Error failure) {
            try { closeFrames(frames, cleanup); }
            catch (RuntimeException | Error cleanupFailure) { suppress(failure, cleanupFailure); }
            throw failure;
        }
    }

    static <T> void closeFrames(T[] frames, Consumer<T> cleanup) {
        if (frames == null) return;
        Objects.requireNonNull(cleanup, "frame cleanup");
        Throwable failure = null;
        for (int frame = frames.length - 1; frame >= 0; frame--) {
            T resource = frames[frame];
            frames[frame] = null;
            if (resource == null) continue;
            try { cleanup.accept(resource); }
            catch (RuntimeException | Error cleanupFailure) { failure = combine(failure, cleanupFailure); }
        }
        rethrow(failure);
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Descriptor resources are closed");
    }

    private static final class Pair {
        final int capacity;
        long pool;
        LongBuffer sets;

        Pair(int capacity) { this.capacity = capacity; }

        static Pair create(Backend backend, int capacity) {
            Pair pair = new Pair(capacity);
            try {
                pair.pool = backend.createPool(capacity);
                if (pair.pool == 0) throw new IllegalStateException("Descriptor pool creation returned a null handle");
                pair.sets = Objects.requireNonNull(backend.allocateSetStorage(capacity), "descriptor set storage");
                if (pair.sets.remaining() < capacity) throw new IllegalStateException("Insufficient descriptor set storage");
                backend.allocateSets(pair.pool, capacity, pair.sets);
                return pair;
            } catch (RuntimeException | Error failure) {
                try { pair.close(backend); }
                catch (RuntimeException | Error cleanup) { suppress(failure, cleanup); }
                throw failure;
            }
        }

        void close(Backend backend) {
            LongBuffer storage = sets;
            long handle = pool;
            sets = null;
            pool = 0;
            Throwable failure = null;
            if (storage != null) {
                try { backend.freeSetStorage(storage); }
                catch (RuntimeException | Error cleanup) { failure = cleanup; }
            }
            if (handle != 0) {
                try { backend.destroyPool(handle); }
                catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
            }
            rethrow(failure);
        }
    }

    private static Throwable combine(Throwable failure, Throwable cleanup) {
        if (failure == null) return cleanup;
        suppress(failure, cleanup);
        return failure;
    }

    private static void suppress(Throwable failure, Throwable cleanup) {
        if (failure != cleanup) failure.addSuppressed(cleanup);
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }
}
