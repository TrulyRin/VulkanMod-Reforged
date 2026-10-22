package net.vulkanmod.gl;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;






final class BorrowedFramebufferCache<P> {
    private final Entry<P>[] entries;
    private final Function<Key, P> factory;
    private final Consumer<P> retirer;
    private long generation;
    private boolean generationKnown;
    private int nextEviction;

    @SuppressWarnings("unchecked")
    BorrowedFramebufferCache(int capacity, Function<Key, P> factory, Consumer<P> retirer) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Borrowed framebuffer cache capacity must be positive");
        }
        this.entries = (Entry<P>[]) new Entry<?>[capacity];
        this.factory = Objects.requireNonNull(factory, "factory");
        this.retirer = Objects.requireNonNull(retirer, "retirer");
    }

    P acquire(long generation,
              Object colorIdentity, long colorImage, long colorView, int colorWidth, int colorHeight, int colorFormat,
              Object depthIdentity, long depthImage, long depthView, int depthWidth, int depthHeight, int depthFormat) {
        if (!this.generationKnown || this.generation != generation) {
            invalidate();
            this.generation = generation;
            this.generationKnown = true;
        }

        int emptySlot = -1;
        for (int i = 0; i < this.entries.length; i++) {
            Entry<P> entry = this.entries[i];
            if (entry == null) {
                if (emptySlot == -1) emptySlot = i;
                continue;
            }

            Key key = entry.key;
            if (key.colorIdentity == colorIdentity
                    && key.colorImage == colorImage
                    && key.colorView == colorView
                    && key.colorWidth == colorWidth
                    && key.colorHeight == colorHeight
                    && key.colorFormat == colorFormat
                    && key.depthIdentity == depthIdentity
                    && key.depthImage == depthImage
                    && key.depthView == depthView
                    && key.depthWidth == depthWidth
                    && key.depthHeight == depthHeight
                    && key.depthFormat == depthFormat) {
                return entry.pair;
            }
        }

        Key key = new Key(generation,
                colorIdentity, colorImage, colorView, colorWidth, colorHeight, colorFormat,
                depthIdentity, depthImage, depthView, depthWidth, depthHeight, depthFormat);
        P pair = Objects.requireNonNull(this.factory.apply(key), "Framebuffer pair factory returned null");


        int slot = emptySlot != -1 ? emptySlot : this.nextEviction;
        Entry<P> previous = this.entries[slot];
        this.entries[slot] = new Entry<>(key, pair);
        this.nextEviction = (slot + 1) % this.entries.length;
        if (previous != null) this.retirer.accept(previous.pair);
        return pair;
    }

    boolean retains(P pair) {
        for (Entry<P> entry : this.entries) {
            if (entry != null && entry.pair == pair) return true;
        }
        return false;
    }

    boolean retainsAttachment(Object identity) {
        for (Entry<P> entry : this.entries) {
            if (entry != null && (entry.key.colorIdentity == identity || entry.key.depthIdentity == identity))
                return true;
        }
        return false;
    }

    void invalidate() {
        this.generationKnown = false;
        this.nextEviction = 0;
        Throwable firstFailure = null;
        for (int i = 0; i < this.entries.length; i++) {
            Entry<P> entry = this.entries[i];

            this.entries[i] = null;
            if (entry != null) {
                try {
                    this.retirer.accept(entry.pair);
                } catch (RuntimeException | Error failure) {
                    if (firstFailure == null) {
                        firstFailure = failure;
                    } else if (firstFailure != failure) {
                        firstFailure.addSuppressed(failure);
                    }
                }
            }
        }
        if (firstFailure instanceof RuntimeException failure) throw failure;
        if (firstFailure instanceof Error failure) throw failure;
    }

    private record Entry<P>(Key key, P pair) {
    }


    record Key(long generation,
               Object colorIdentity, long colorImage, long colorView, int colorWidth, int colorHeight, int colorFormat,
               Object depthIdentity, long depthImage, long depthView, int depthWidth, int depthHeight, int depthFormat) {
    }
}
