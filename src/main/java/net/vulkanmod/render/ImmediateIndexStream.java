package net.vulkanmod.render;

import java.nio.ByteBuffer;
import java.util.Objects;

                                                                                                
public final class ImmediateIndexStream<B> implements AutoCloseable {
    public enum Type {
        U16(2), U32(4);
        private final int bytes;
        Type(int bytes) { this.bytes = bytes; }
        public int bytes() { return bytes; }
    }
    public interface Backend<B> {
        B create(int capacity, Type type);
        void append(B buffer, ByteBuffer selectedBytes);
        void reset(B buffer);
                                                                                                        
        void retire(B buffer);
    }
    private static final class Slot<B> { B buffer; int capacity, used; }
    private final Backend<B> backend;
    private final int initialCapacity;
    private final Slot<B> shorts = new Slot<>(), ints = new Slot<>();
    private B prepared;
    private int count, offset;
    private boolean closed;

    public ImmediateIndexStream(Backend<B> backend, int initialCapacity) {
        this.backend = Objects.requireNonNull(backend, "backend");
        if (initialCapacity <= 0) throw new IllegalArgumentException("Positive immediate index capacity required");
        this.initialCapacity = initialCapacity;
    }
    public void upload(ByteBuffer source, Type type, int indexCount) {
        requireOpen(); clearPrepared();
        if (source == null || type == null || indexCount < 0) {
            throw new IllegalArgumentException("Source, index type and nonnegative index count required");
        }
        long required = (long) indexCount * type.bytes();
        if (required > Integer.MAX_VALUE || required > source.remaining()) {
            throw new IllegalArgumentException("Supplied index extent exceeds the selected source");
        }
        int bytes = (int) required;
        if (bytes == 0) return;
                                                                                                           
        ByteBuffer selected = source.slice(); selected.limit(bytes);
        Slot<B> slot = type == Type.U16 ? shorts : ints;
        if (slot.buffer == null || bytes > slot.capacity - slot.used) {
            int capacity = (int) Math.max(bytes, Math.max(initialCapacity,
                    Math.min(Integer.MAX_VALUE, (long) slot.capacity * 2)));
            B replacement = Objects.requireNonNull(backend.create(capacity, type), "immediate index storage");
            try { backend.append(replacement, selected); }
            catch (RuntimeException | Error failure) { retire(replacement, failure); throw failure; }
            B previous = slot.buffer;
            slot.buffer = replacement; slot.capacity = capacity; slot.used = bytes;
                                                                                                   
            if (previous != null) backend.retire(previous);
            offset = 0;
        } else {
            int start = slot.used;
            try { backend.append(slot.buffer, selected); }
            catch (RuntimeException | Error failure) {
                                                                                                     
                B poisoned = slot.buffer; slot.buffer = null; slot.capacity = slot.used = 0;
                retire(poisoned, failure); throw failure;
            }
            slot.used += bytes; offset = start;
        }
        prepared = slot.buffer; count = indexCount;
    }
    public B buffer() { requireOpen(); return prepared; }
    public int indexCount() { requireOpen(); return count; }
    public int offset() { requireOpen(); return offset; }
                                                                  
    public void reset() {
        requireOpen(); clearPrepared();
        Throwable failure = reset(shorts, null);
        rethrow(reset(ints, failure));
    }
    @Override public void close() {
        if (closed) return;
        closed = true; clearPrepared();
        B oldShorts = shorts.buffer, oldInts = ints.buffer;
        shorts.buffer = ints.buffer = null;
        Throwable failure = retire(oldShorts, null);
        rethrow(retire(oldInts, failure));
    }
    private Throwable reset(Slot<B> slot, Throwable primary) {
        if (slot.buffer == null) return primary;
        try { backend.reset(slot.buffer); slot.used = 0; }
        catch (RuntimeException | Error failure) {
            B poisoned = slot.buffer; slot.buffer = null; slot.capacity = slot.used = 0;
            primary = combine(primary, failure); primary = retire(poisoned, primary);
        }
        return primary;
    }
    private Throwable retire(B buffer, Throwable primary) {
        if (buffer != null) {
            try { backend.retire(buffer); }
            catch (RuntimeException | Error failure) { primary = combine(primary, failure); }
        }
        return primary;
    }
    private void clearPrepared() { prepared = null; count = offset = 0; }
    private static Throwable combine(Throwable primary, Throwable next) {
        if (primary == null) return next;
        if (primary != next) primary.addSuppressed(next);
        return primary;
    }
    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }
    private void requireOpen() { if (closed) throw new IllegalStateException("Immediate index stream is closed"); }
}
