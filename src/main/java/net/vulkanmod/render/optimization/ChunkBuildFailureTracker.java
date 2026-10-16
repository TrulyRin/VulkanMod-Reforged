package net.vulkanmod.render.optimization;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

                                                                            
public final class ChunkBuildFailureTracker {
    private static final int KEY_LIMIT = 64;
    private static final Set<String> FAILURE_KEYS = ConcurrentHashMap.newKeySet();

    private ChunkBuildFailureTracker() {
    }

    public static void record(String taskName, Throwable throwable) {
        if (FAILURE_KEYS.size() >= KEY_LIMIT) {
            return;
        }
        String exceptionType = throwable == null ? "unknown" : throwable.getClass().getName();
        FAILURE_KEYS.add(taskName + ":" + exceptionType);
    }

    public static Set<String> reportedFailures() {
        return Set.copyOf(new TreeSet<>(FAILURE_KEYS));
    }

    static void resetForTests() {
        FAILURE_KEYS.clear();
    }
}
