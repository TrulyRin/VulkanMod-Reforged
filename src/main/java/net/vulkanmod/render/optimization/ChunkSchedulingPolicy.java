package net.vulkanmod.render.optimization;

   
                                                                           
                                                                           
                                                          
   
public final class ChunkSchedulingPolicy {
    public static final int MAX_BUILDER_THREADS = 8;
    public static final int MAX_CONSECUTIVE_HIGH_PRIORITY_TASKS = 8;
    public static final int MAX_PENDING_COMPILE_RESULTS = 64;
    public static final int MAX_QUEUED_BUILD_TASKS = 64;
    public static final int MAX_STALE_RESULTS_PER_FRAME = 32;
    public static final int MAX_REBUILD_QUEUE_SCANS_PER_FRAME = 64;
    public static final int MAX_DELAYED_REBUILD_PROMOTIONS_PER_FRAME = 64;
    public static final int MAX_FAILED_TASK_RECOVERIES_PER_FRAME = 16;

       
                                                                            
                                                                           
                                                                            
                            
       
    public static final long REBUILD_COOLDOWN_NANOS = 250_000_000L;

    private static final int FIXED_REBUILD_SCHEDULES = 8;
    private static final long FIXED_REBUILD_TIME_BUDGET_NANOS = 1_000_000L;
    private static final long FIXED_UPLOAD_TIME_BUDGET_NANOS = 2_000_000L;
    private static final long FIXED_UPLOAD_BYTE_BUDGET = 8L * 1024 * 1024;

    private ChunkSchedulingPolicy() {
    }

    public static int clampUploadCeiling(int value) {
        return Math.max(1, Math.min(16, value));
    }

    public static int chooseBuilderThreadCount(int availableProcessors) {
        return chooseBuilderThreadCount(availableProcessors, false);
    }

       
                                                                          
                                                                            
                                                                             
       
    public static int chooseBuilderThreadCount(int availableProcessors, boolean integratedServer) {
        int processors = Math.max(1, availableProcessors);
        int divisor = integratedServer ? 4 : 2;
        return Math.max(1, Math.min(MAX_BUILDER_THREADS, processors / divisor));
    }

    public static int resolveBuilderThreadCount(int configuredThreads, int availableProcessors,
                                                boolean integratedServer) {
        if (configuredThreads <= 0) {
            return chooseBuilderThreadCount(availableProcessors, integratedServer);
        }
        return Math.max(1, Math.min(MAX_BUILDER_THREADS, configuredThreads));
    }

    public static int fixedRebuildScheduleBudget(int pendingRebuilds) {
        return Math.min(Math.max(0, pendingRebuilds), FIXED_REBUILD_SCHEDULES);
    }

    public static long fixedRebuildTimeBudgetNanos(int pendingRebuilds) {
        return pendingRebuilds > 0 ? FIXED_REBUILD_TIME_BUDGET_NANOS : 0L;
    }

    public static long fixedUploadTimeBudgetNanos() {
        return FIXED_UPLOAD_TIME_BUDGET_NANOS;
    }

    public static long fixedUploadByteBudget() {
        return FIXED_UPLOAD_BYTE_BUDGET;
    }

    public static boolean shouldServeLowPriority(int consecutiveHighPriorityTasks,
                                                 boolean highPriorityAvailable,
                                                 boolean lowPriorityAvailable) {
        return lowPriorityAvailable
                && (!highPriorityAvailable
                || consecutiveHighPriorityTasks >= MAX_CONSECUTIVE_HIGH_PRIORITY_TASKS);
    }
}
