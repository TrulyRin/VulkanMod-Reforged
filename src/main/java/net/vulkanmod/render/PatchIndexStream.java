package net.vulkanmod.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;


public final class PatchIndexStream<B> implements AutoCloseable {
    public interface Backend<B> {
        B create(int capacity, QuadPatchIndices.IndexType type);
        void append(B buffer, ByteBuffer bytes);
        void reset(B buffer);
        void retire(B buffer);
        ByteBuffer allocateStaging(int capacity);
        void freeStaging(ByteBuffer bytes);
    }
    private static final class Slot<B> { B buffer; int capacity, used; }
    private final Backend<B> backend;
    private final int initialCapacity;
    private final Slot<B> shorts = new Slot<>(), ints = new Slot<>();
    private ByteBuffer staging;
    private B prepared;
    private int count, byteOffset;
    private boolean closed;

    public PatchIndexStream(Backend<B> backend, int initialCapacity) {
        this.backend = Objects.requireNonNull(backend, "backend");
        if (initialCapacity <= 0) throw new IllegalArgumentException("Positive initial patch capacity required");
        this.initialCapacity = initialCapacity;
    }
    public void upload(ByteBuffer source, QuadPatchIndices.IndexType type, int triangleIndices, int vertexCount) {
        requireOpen();
        clearPrepared();
        int bytes = QuadPatchIndices.requiredBytes(triangleIndices, type);
        if (source == null || vertexCount < 0) throw new IllegalArgumentException("Patch source and nonnegative vertex count required");
        long sourceBytes = (long) triangleIndices * type.bytes();
        if (sourceBytes > Integer.MAX_VALUE || sourceBytes > source.remaining()) {
            throw new IllegalArgumentException("Patch source has too few triangle index bytes");
        }
        if (bytes == 0) return;
        ensureStaging(bytes);
        staging.clear().limit(bytes);

        int indices = QuadPatchIndices.compact(source, type, triangleIndices, vertexCount, staging);
        Slot<B> slot = type == QuadPatchIndices.IndexType.U16 ? shorts : ints;
        if (slot.buffer == null || bytes > slot.capacity - slot.used) {
            int capacity = (int) Math.max(bytes, Math.max(initialCapacity,
                    Math.min(Integer.MAX_VALUE, (long) slot.capacity * 2)));
            B replacement = Objects.requireNonNull(backend.create(capacity, type), "patch storage");
            try { backend.append(replacement, staging); }
            catch (RuntimeException | Error failure) { release(replacement, failure); throw failure; }
            B previous = slot.buffer;
            slot.buffer = replacement;
            slot.capacity = capacity;
            slot.used = bytes;
            if (previous != null) backend.retire(previous);
            byteOffset = 0;
        } else {
            int offset = slot.used;
            try { backend.append(slot.buffer, staging); }
            catch (RuntimeException | Error failure) {

                B poisoned = slot.buffer;
                slot.buffer = null;
                slot.capacity = slot.used = 0;
                release(poisoned, failure);
                throw failure;
            }
            slot.used += bytes;
            byteOffset = offset;
        }
        prepared = slot.buffer;
        count = indices;
    }
    public B buffer() { requireOpen(); return prepared; }
    public int indexCount() { requireOpen(); return count; }
    public int offset() { requireOpen(); return byteOffset; }
    public void reset() {
        requireOpen(); clearPrepared();
        Throwable failure = reset(shorts, null);
        rethrow(reset(ints, failure));
    }
    @Override public void close() {
        if (closed) return;
        closed = true; clearPrepared();
        B previousShorts = shorts.buffer, previousInts = ints.buffer;
        ByteBuffer previousStaging = staging;
        shorts.buffer = ints.buffer = null;
        staging = null;
        Throwable failure = release(previousShorts, null);
        failure = release(previousInts, failure);
        if (previousStaging != null) {
            try { backend.freeStaging(previousStaging); }
            catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        }
        rethrow(failure);
    }
    private Throwable reset(Slot<B> slot, Throwable failure) {
        if (slot.buffer == null) return failure;
        try { backend.reset(slot.buffer); slot.used = 0; }
        catch (RuntimeException | Error reset) {
            B poisoned = slot.buffer;
            slot.buffer = null; slot.capacity = slot.used = 0;
            failure = combine(failure, reset);
            failure = release(poisoned, failure);
        }
        return failure;
    }
    private void ensureStaging(int bytes) {
        if (staging != null && staging.capacity() >= bytes) return;
        int capacity = staging == null ? bytes : (int) Math.max(bytes,
                Math.min(Integer.MAX_VALUE, (long) staging.capacity() * 2));
        ByteBuffer replacement = Objects.requireNonNull(backend.allocateStaging(capacity), "patch staging");
        if (replacement.isReadOnly() || replacement.capacity() < bytes) {
            var failure = new IllegalStateException("Patch staging must be writable and cover its capacity");
            try { backend.freeStaging(replacement); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
        replacement.order(ByteOrder.nativeOrder());
        ByteBuffer previous = staging; staging = replacement;
        if (previous != null) backend.freeStaging(previous);
    }
    private void clearPrepared() { prepared = null; count = byteOffset = 0; }
    private Throwable release(B buffer, Throwable failure) {
        if (buffer != null) {
            try { backend.retire(buffer); }
            catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        }
        return failure;
    }
    private static Throwable combine(Throwable failure, Throwable next) {
        if (failure == null) return next;
        if (failure != next) failure.addSuppressed(next);
        return failure;
    }
    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }
    private void requireOpen() { if (closed) throw new IllegalStateException("Patch stream is closed"); }
}
