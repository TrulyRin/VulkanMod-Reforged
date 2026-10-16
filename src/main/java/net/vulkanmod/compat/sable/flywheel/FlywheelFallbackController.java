package net.vulkanmod.compat.sable.flywheel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class FlywheelFallbackController {
    private final AtomicReference<String> pendingReason = new AtomicReference<>();
    private final AtomicBoolean demoted = new AtomicBoolean();

    public void requestDemotion(String reason) {
        if (!demoted.get()) {
            pendingReason.compareAndSet(null, reason == null ? "unknown" : reason);
        }
    }

    public boolean applyAtFrameBoundary() {
        if (pendingReason.getAndSet(null) == null) {
            return false;
        }
        return demoted.compareAndSet(false, true);
    }

    public boolean demoted() {
        return demoted.get();
    }
}
