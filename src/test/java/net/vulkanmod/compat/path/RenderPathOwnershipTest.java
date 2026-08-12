package net.vulkanmod.compat.path;

import net.vulkanmod.compat.CompatMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderPathOwnershipTest {
    @AfterEach
    void reset() {
        RenderPathOwnership.reset();
    }

    @Test
    void snapshotsKeepOwnerAndModePairedAndImmutable() {
        RenderPathOwnership.assignOwnership(RenderPath.GUI, "Create", CompatMode.SAFE);
        List<RenderPathOwnership.PathState> captured = RenderPathOwnership.snapshot();
        RenderPathOwnership.releaseOwnership(RenderPath.GUI, "create");

        RenderPathOwnership.PathState oldGui = captured.get(RenderPath.GUI.ordinal());
        assertEquals("create", oldGui.owner());
        assertEquals(CompatMode.SAFE, oldGui.mode());
        assertThrows(UnsupportedOperationException.class, () -> captured.add(oldGui));
        assertEquals("minecraft", RenderPathOwnership.getPathOwner(RenderPath.GUI));
        assertEquals(CompatMode.OFF, RenderPathOwnership.getPathMode(RenderPath.GUI));
    }

    @Test
    void concurrentReadersNeverObserveAStaggeredOwnerModePair() throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        Thread writer = new Thread(() -> {
            await(start);
            for (int i = 0; i < 5_000; i++) {
                RenderPathOwnership.assignOwnership(RenderPath.GUI, "create", CompatMode.SAFE);
                RenderPathOwnership.releaseOwnership(RenderPath.GUI, "create");
            }
        });
        Thread reader = new Thread(() -> {
            await(start);
            try {
                for (int i = 0; i < 5_000; i++) {
                    var state = RenderPathOwnership.snapshot().get(RenderPath.GUI.ordinal());
                    boolean defaultPair = state.owner().equals("minecraft") && state.mode() == CompatMode.OFF;
                    boolean claimedPair = state.owner().equals("create") && state.mode() == CompatMode.SAFE;
                    if (!defaultPair && !claimedPair) {
                        throw new AssertionError("staggered path state: " + state);
                    }
                }
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        writer.start();
        reader.start();
        start.countDown();
        writer.join();
        reader.join();
        assertNull(failure.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
