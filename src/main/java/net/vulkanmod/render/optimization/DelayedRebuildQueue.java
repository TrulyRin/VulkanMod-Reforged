package net.vulkanmod.render.optimization;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.PriorityQueue;

public final class DelayedRebuildQueue<T> {
    private final PriorityQueue<Entry<T>> queue = new PriorityQueue<>(DelayedRebuildQueue::compareEntries);
    private final Map<T, Entry<T>> entries = new IdentityHashMap<>();
    private long sequence;

    public boolean defer(T value, long eligibleNanos) {
        if (this.entries.containsKey(value)) {
            return false;
        }

        Entry<T> entry = new Entry<>(value, eligibleNanos, this.sequence++);
        this.entries.put(value, entry);
        this.queue.add(entry);
        return true;
    }

    public T pollEligible(long nowNanos) {
        Entry<T> entry = this.queue.peek();
        if (entry == null || nowNanos - entry.eligibleNanos < 0L) {
            return null;
        }

        this.queue.remove();
        this.entries.remove(entry.value);
        return entry.value;
    }

    public boolean remove(T value) {
        Entry<T> entry = this.entries.remove(value);
        if (entry == null) {
            return false;
        }
        this.queue.remove(entry);
        return true;
    }

    public boolean contains(T value) {
        return this.entries.containsKey(value);
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    public int size() {
        return this.entries.size();
    }

    private static int compareEntries(Entry<?> left, Entry<?> right) {
        long deadlineDifference = left.eligibleNanos - right.eligibleNanos;
        if (deadlineDifference < 0L) {
            return -1;
        }
        if (deadlineDifference > 0L) {
            return 1;
        }
        return Long.compare(left.sequence, right.sequence);
    }

    private static final class Entry<T> {
        private final T value;
        private final long eligibleNanos;
        private final long sequence;

        private Entry(T value, long eligibleNanos, long sequence) {
            this.value = value;
            this.eligibleNanos = eligibleNanos;
            this.sequence = sequence;
        }
    }
}
