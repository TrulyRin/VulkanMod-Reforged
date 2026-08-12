package net.vulkanmod.render.optimization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelayedRebuildQueueTest {
    @Test
    void aDeferredSectionIsPolledOnlyOnceAtItsDeadline() {
        DelayedRebuildQueue<Object> queue = new DelayedRebuildQueue<>();
        Object section = new Object();

        assertTrue(queue.defer(section, 250L));
        assertFalse(queue.defer(section, 300L));
        assertNull(queue.pollEligible(249L));
        assertSame(section, queue.pollEligible(250L));
        assertNull(queue.pollEligible(500L));
        assertTrue(queue.isEmpty());
    }

    @Test
    void playerPromotionRemovesTheDelayedEntryImmediately() {
        DelayedRebuildQueue<Object> queue = new DelayedRebuildQueue<>();
        Object section = new Object();

        queue.defer(section, 250L);
        assertTrue(queue.contains(section));
        assertTrue(queue.remove(section));
        assertFalse(queue.contains(section));
        assertNull(queue.pollEligible(250L));
    }

    @Test
    void membershipUsesIdentityInsteadOfEquals() {
        DelayedRebuildQueue<EqualValue> queue = new DelayedRebuildQueue<>();
        EqualValue first = new EqualValue();
        EqualValue second = new EqualValue();

        assertTrue(queue.defer(first, 10L));
        assertTrue(queue.defer(second, 20L));
        assertEquals(2, queue.size());
        assertSame(first, queue.pollEligible(10L));
        assertSame(second, queue.pollEligible(20L));
    }

    @Test
    void deadlineChecksRemainCorrectAcrossNanoTimeOverflow() {
        DelayedRebuildQueue<Object> queue = new DelayedRebuildQueue<>();
        Object beforeWrapSection = new Object();
        Object afterWrapSection = new Object();
        long beforeWrap = Long.MAX_VALUE - 5L;
        long beforeWrapDeadline = beforeWrap + 4L;
        long afterWrapDeadline = beforeWrap + 10L;

        queue.defer(afterWrapSection, afterWrapDeadline);
        queue.defer(beforeWrapSection, beforeWrapDeadline);
        assertNull(queue.pollEligible(beforeWrap));
        assertSame(beforeWrapSection, queue.pollEligible(beforeWrapDeadline));
        assertSame(afterWrapSection, queue.pollEligible(afterWrapDeadline));
    }

    private static final class EqualValue {
        @Override
        public boolean equals(Object ignored) {
            return true;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
