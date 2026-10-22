package net.vulkanmod.render.chunk;

import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;


public final class VanillaSectionInvalidation {
    public static final VanillaSectionInvalidation INSTANCE = new VanillaSectionInvalidation();

    private final AtomicReference<Registry> pending = new AtomicReference<>();
    private final ThreadLocal<Depth> internalDepth = ThreadLocal.withInitial(Depth::new);
    private volatile Registry active;
    private long nextEpoch;


    public void updateOwner(Object candidateRenderer, Object mainRenderer, Object owner, Object[] sections) {
        if (candidateRenderer == null || candidateRenderer != mainRenderer) return;
        if (owner == null) this.clear();
        else this.activate(owner, sections);
    }

    public synchronized void activate(Object owner, Object[] sections) {
        Objects.requireNonNull(owner, "owner");
        IdentityHashMap<Object, Boolean> members = new IdentityHashMap<>();
        for (Object section : sections) {
            if (section != null) members.put(section, Boolean.TRUE);
        }
        Registry replacement = new Registry(++this.nextEpoch, owner, members);

        this.pending.set(null);
        this.active = replacement;
    }

    public synchronized void clear() {
        this.active = null;
        this.pending.set(null);
    }

    public boolean isActiveOwner(Object owner) {
        Registry current = this.active;
        return current != null && current.owner == owner;
    }

    public void request(Object section) {
        Registry current = this.active;
        if (current == null || !current.members.containsKey(section) || this.internalDepth.get().value != 0) return;

        this.pending.accumulateAndGet(current, VanillaSectionInvalidation::newest);
    }

    public boolean consume() {
        Registry requested = this.pending.getAndSet(null);
        return requested != null && requested == this.active;
    }

    public boolean hasPending() {
        Registry current = this.active;
        return current != null && this.pending.get() == current;
    }

    public void beginInternal() {
        this.internalDepth.get().value++;
    }

    public void endInternal() {
        Depth depth = this.internalDepth.get();
        if (depth.value == 0) throw new IllegalStateException("Unbalanced vanilla section dirty scope");
        depth.value--;
    }

    private static Registry newest(Registry previous, Registry request) {
        return previous == null || previous.epoch < request.epoch ? request : previous;
    }

    private record Registry(long epoch, Object owner, IdentityHashMap<Object, Boolean> members) { }

    private static final class Depth {
        int value;
    }
}
