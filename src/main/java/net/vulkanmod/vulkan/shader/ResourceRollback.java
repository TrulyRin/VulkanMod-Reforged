package net.vulkanmod.vulkan.shader;

import java.util.ArrayDeque;
import java.util.Objects;

                                                                                 
final class ResourceRollback implements AutoCloseable {
    private final ArrayDeque<Runnable> releases = new ArrayDeque<>();
    private boolean closed;

    void add(Runnable release) {
        if (closed) throw new IllegalStateException("Ownership transaction is closed");
        releases.addFirst(Objects.requireNonNull(release));
    }

    void commit() {
        if (closed) throw new IllegalStateException("Ownership transaction is closed");
        closed = true;
        releases.clear();
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        Throwable failure = null;
        while (!releases.isEmpty()) {
            Runnable release = releases.removeFirst();
            try { release.run(); }
            catch (RuntimeException | Error error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
    }
}
