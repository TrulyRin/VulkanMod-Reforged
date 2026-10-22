package net.vulkanmod.compat.observer;

import net.vulkanmod.Initializer;
import net.vulkanmod.compat.RuntimeOptions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class GuiRenderTrace {
    public static final ThreadLocal<Boolean> inGuiRender = ThreadLocal.withInitial(() -> false);
    private static final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private static final int LOG_RATE_LIMIT = 200;

    private static final boolean TRACE_ENABLED = RuntimeOptions.hudTraceEnabled();

    private GuiRenderTrace() {
    }

    public static boolean isActive() {
        return TRACE_ENABLED && inGuiRender.get();
    }

    public static void beginGuiRender() {
        if (TRACE_ENABLED) {
            inGuiRender.set(true);
        }
    }

    public static void endGuiRender() {
        if (TRACE_ENABLED) {
            inGuiRender.set(false);
        }
    }

    public static void logBoundary(String phase, String details) {
        log("boundary." + phase, phase + " " + details);
    }

    public static void logDraw(String details) {
        if (!isActive()) {
            return;
        }

        log("draw", details);
    }

    public static void logUboUpload(String details) {
        if (!isActive()) {
            return;
        }

        log("ubo", details);
    }

    public static void log(String key, String details) {
        if (!RuntimeOptions.hudTraceEnabled()) {
            return;
        }

        AtomicInteger counter = counters.computeIfAbsent(key, ignored -> new AtomicInteger());
        int value = counter.incrementAndGet();
        if (value == 1 || value % LOG_RATE_LIMIT == 0) {
            Initializer.LOGGER.info("[HUDTRACE] {} | {} | count={}", key, details, value);
        }
    }

    public static void reset() {
        counters.clear();
        inGuiRender.remove();
    }
}
