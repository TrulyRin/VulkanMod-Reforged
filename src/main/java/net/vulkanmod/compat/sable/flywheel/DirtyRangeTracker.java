package net.vulkanmod.compat.sable.flywheel;

import java.util.Optional;

public final class DirtyRangeTracker {
    private int start = Integer.MAX_VALUE;
    private int end = Integer.MIN_VALUE;

    public synchronized void mark(int startInclusive, int endExclusive) {
        if (startInclusive < 0 || endExclusive <= startInclusive) {
            return;
        }
        start = Math.min(start, startInclusive);
        end = Math.max(end, endExclusive);
    }

    public synchronized Optional<Range> take() {
        if (start == Integer.MAX_VALUE) {
            return Optional.empty();
        }
        Range range = new Range(start, end);
        start = Integer.MAX_VALUE;
        end = Integer.MIN_VALUE;
        return Optional.of(range);
    }

    public record Range(int startInclusive, int endExclusive) {
    }
}
