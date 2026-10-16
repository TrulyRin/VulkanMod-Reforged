package net.vulkanmod.compat.sable.veil;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntToLongFunction;

                                                                                           
final class ProgramPipelineCache<F, P> {
    @FunctionalInterface
    interface Factory<F, P> { P create(int program, long generation, F format); }

    private final Factory<F, P> factory;
    private final IntToLongFunction generations;
    private final Consumer<P> retirement;
    private final Int2ReferenceOpenHashMap<Owner<F, P>> owners = new Int2ReferenceOpenHashMap<>();

    ProgramPipelineCache(IntToLongFunction generations, Factory<F, P> factory, Consumer<P> retirement) {
        this.generations = Objects.requireNonNull(generations);
        this.factory = Objects.requireNonNull(factory);
        this.retirement = Objects.requireNonNull(retirement);
    }

    Entry<P> resolve(int program, long generation, F format) {
        Objects.requireNonNull(format, "actual draw format");
        if (program <= 0 || generation <= 0 || generations.applyAsLong(program) != generation) {
            throw new IllegalStateException("Program generation is no longer current");
        }
        Owner<F, P> owner = owners.get(program);
        if (owner != null && owner.generation != generation) {
            invalidate(program);
            owner = null;
        }
        if (owner == null) {
            owner = new Owner<>(generation);
            owners.put(program, owner);
        }
        Entry<P> cached = owner.formats.get(format);
        if (cached != null) return cached;

        Entry<P> created;
        try {
            created = new Entry<>(Objects.requireNonNull(factory.create(program, generation, format),
                    "Program pipeline factory returned null"), null);
        } catch (RuntimeException failure) {
            created = new Entry<>(null, failure);
        }
        if (generations.applyAsLong(program) != generation || owners.get(program) != owner) {
            var stale = new IllegalStateException("Program generation changed during pipeline construction");
            try {
                if (owners.get(program) == owner) invalidate(program);
            } catch (RuntimeException failure) { stale.addSuppressed(failure); }
            try { retire(created); }
            catch (RuntimeException failure) { stale.addSuppressed(failure); }
            throw stale;
        }
        owner.formats.put(format, created);
        return created;
    }

    void invalidate(int program) {
        Owner<F, P> removed = owners.remove(program);
        if (removed != null) retireAll(removed.formats.values());
    }

    void clear() {
        var removed = new ArrayList<>(owners.values());
        owners.clear();
        RuntimeException failure = null;
        for (Owner<F, P> owner : removed) {
            try { retireAll(owner.formats.values()); }
            catch (RuntimeException error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    private void retireAll(Iterable<Entry<P>> entries) {
        RuntimeException failure = null;
        for (Entry<P> entry : entries) {
            try { retire(entry); }
            catch (RuntimeException error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }

    private void retire(Entry<P> entry) {
        if (entry.retired) return;
        entry.retired = true;
        if (entry.resource != null) retirement.accept(entry.resource);
    }

    private static final class Owner<F, P> {
        final long generation;
        final Map<F, Entry<P>> formats = new HashMap<>();
        Owner(long generation) { this.generation = generation; }
    }

    static final class Entry<P> {
        private final P resource;
        private final RuntimeException failure;
        private boolean retired;
        private boolean failureReported;

        Entry(P resource, RuntimeException failure) { this.resource = resource; this.failure = failure; }
        P resource() { return resource; }
        RuntimeException failure() { return failure; }
        boolean retired() { return retired; }
        boolean shouldReportFailure() {
            if (failure == null || failureReported) return false;
            failureReported = true;
            return true;
        }
    }
}
