package net.vulkanmod.render;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;


public final class IndexUploadState<B> implements AutoCloseable {
    public interface Backend<B> {
        B create(int bytes, QuadPatchIndices.IndexType type);

        void upload(B buffer, ByteBuffer bytes);

        void retire(B buffer);
        ByteBuffer allocateStaging(int bytes);
        void freeStaging(ByteBuffer bytes);
    }

    private final Backend<B> backend;
    private B indices;
    private B patches;
    private int patchCount;
    private boolean custom;
    private ByteBuffer staging;
    private boolean closed;

    public IndexUploadState(Backend<B> backend) { this.backend = Objects.requireNonNull(backend, "backend"); }






    public void uploadCustom(ByteBuffer source, QuadPatchIndices.IndexType type,
                             int indexCount, int vertexCount, boolean quads) {
        requireOpen();

        B previousPatch = patches;
        patches = null;
        patchCount = 0;
        if (previousPatch != null) backend.retire(previousPatch);
        if (source == null || type == null || indexCount < 0 || vertexCount < 0) {
            throw new IllegalArgumentException("A custom index stream requires a type, source and nonnegative counts");
        }
        if (indexCount > 0 && source.order() != ByteOrder.nativeOrder()) {
            throw new IllegalArgumentException("Custom index streams must use native byte order");
        }
        long extent = (long) indexCount * type.bytes();
        if (extent > Integer.MAX_VALUE || extent > source.remaining()) {
            throw new IllegalArgumentException("Custom index byte extent exceeds its source");
        }
        B replacement = null;
        B companion = null;
        int companionCount = 0;
        try {
            if (extent > 0) {
                replacement = Objects.requireNonNull(backend.create((int) extent, type), "ordinary index storage");
                ByteBuffer ordinary = source.slice(source.position(), (int) extent).order(source.order());
                backend.upload(replacement, ordinary);
                if (quads && indexCount % 6 == 0) {
                    int bytes = QuadPatchIndices.requiredBytes(indexCount, type);
                    ensureStaging(bytes);
                    staging.clear().limit(bytes);
                    boolean supported;
                    try {
                        companionCount = QuadPatchIndices.compact(source, type, indexCount, vertexCount, staging);
                        supported = true;
                    } catch (IllegalArgumentException unsupportedPattern) {

                        supported = false;
                        companionCount = 0;
                    }
                    if (supported) {
                        companion = Objects.requireNonNull(backend.create(bytes, type), "patch index storage");
                        backend.upload(companion, staging);
                    }
                }
            }
        } catch (RuntimeException | Error failure) {
            release(companion, failure);
            release(replacement, failure);
            throw failure;
        }
        B previous = custom ? indices : null;
        indices = replacement;
        custom = true;
        patches = companion;
        patchCount = companionCount;
        if (previous != null) backend.retire(previous);
    }


    public void useBorrowed(B buffer) {
        requireOpen();
        B previous = custom ? indices : null;
        B previousPatch = patches;
        indices = buffer;
        custom = false;
        patches = null;
        patchCount = 0;
        Throwable failure = release(previousPatch, null);
        failure = release(previous, failure);
        rethrow(failure);
    }

    public void clear() { useBorrowed(null); }
    public B indices() { requireOpen(); return indices; }
    public B patchIndices() { requireOpen(); return patches; }
    public int patchIndexCount() { requireOpen(); return patchCount; }
    public boolean isCustom() { requireOpen(); return custom; }

    public B requirePatchIndices() {
        requireOpen();
        if (!custom || patches == null) throw new IllegalStateException("Custom quad indices have no supported patch companion");
        return patches;
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        B previous = custom ? indices : null;
        B previousPatch = patches;
        ByteBuffer previousStaging = staging;
        indices = null;
        patches = null;
        staging = null;
        patchCount = 0;
        custom = false;
        Throwable failure = release(previousPatch, null);
        failure = release(previous, failure);
        if (previousStaging != null) {
            try { backend.freeStaging(previousStaging); }
            catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        }
        rethrow(failure);
    }

    private void ensureStaging(int bytes) {
        if (staging != null && staging.capacity() >= bytes) return;
        int capacity = staging == null ? bytes : (int) Math.max(bytes, Math.min(Integer.MAX_VALUE, (long) staging.capacity() * 2));
        ByteBuffer replacement = Objects.requireNonNull(backend.allocateStaging(capacity), "index staging");
        if (replacement.capacity() < bytes || replacement.isReadOnly()) {
            var failure = new IllegalStateException("Index staging must be writable and cover its requested capacity");
            try { backend.freeStaging(replacement); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
        replacement.order(ByteOrder.nativeOrder());
        ByteBuffer previous = staging;
        staging = replacement;
        if (previous != null) backend.freeStaging(previous);
    }

    private Throwable release(B buffer, Throwable failure) {
        if (buffer != null) {
            try { backend.retire(buffer); }
            catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        }
        return failure;
    }

    private static Throwable combine(Throwable failure, Throwable cleanup) {
        if (failure == null) return cleanup;
        if (failure != cleanup) failure.addSuppressed(cleanup);
        return failure;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Index upload state is closed");
    }
}
