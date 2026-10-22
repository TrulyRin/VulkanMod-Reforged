package net.vulkanmod.compat.observer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.vulkanmod.compat.RuntimeOptions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GLCallObserver {
    private static final Logger LOGGER = LogManager.getLogger("GLCallObserver");
    public static boolean ACTIVE = false;

    private static final ConcurrentHashMap<String, AtomicInteger> callCounters = new ConcurrentHashMap<>();
    private static final int LOG_RATE_LIMIT = 1000;

    private static final boolean HUD_TRACE = RuntimeOptions.hudTraceEnabled();

    public static boolean shouldObserve() {
        return ACTIVE || HUD_TRACE;
    }

    public static void observeCall(String callName, String extraInfo) {
        if (!shouldObserve()) return;
        boolean hudTrace = HUD_TRACE;

        if (CompatProfiler.ENABLED) {
            CompatProfiler.glObserverCalls++;
        }

        AtomicInteger counter = callCounters.computeIfAbsent(callName, k -> new AtomicInteger(0));
        int val = counter.incrementAndGet();

        if (val % LOG_RATE_LIMIT == 1) {

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String origin = "unknown";
            for (int i = 3; i < stack.length; i++) {
                String className = stack[i].getClassName();
                if (!className.startsWith("net.vulkanmod") && !className.startsWith("java.lang") && !className.startsWith("net.minecraft")) {
                    origin = stack[i].toString();
                    break;
                }
            }

            String tag = hudTrace ? "[HUDTRACE] GL call" : "[GLObserver] Intercepted GL call";
            LOGGER.info("{}: {} | Info: {} | Origin: {} | Count: {}",
                        tag, callName, extraInfo, origin, val);
        }
    }

    public static void reset() {
        callCounters.clear();
    }
}
