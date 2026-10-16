package net.vulkanmod.compat.observer;

import net.minecraft.client.Minecraft;
import net.vulkanmod.compat.CompatDetector;
import net.vulkanmod.compat.CompatMode;
import net.vulkanmod.compat.CompatPolicyManager;
import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.compat.path.RenderPathOwnership;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class CompatProfiler {
    public static boolean ENABLED = false;

    public static int glObserverCalls = 0;
    public static long textureUploadBytes = 0;
    public static long bufferUploadBytes = 0;
    public static int vulkanAllocations = 0;

    public static int shaderCompileCount = 0;
    public static long spirvCompileTimeNanos = 0;
    public static int shaderCacheHits = 0;
    public static int shaderCacheMisses = 0;

    private static final int BENCHMARK_FRAMES = 300;
    private static final float[] frameTimes = new float[BENCHMARK_FRAMES];
    private static final float[] cpuRenderTimes = new float[BENCHMARK_FRAMES];
    private static final int[] frameObserverCalls = new int[BENCHMARK_FRAMES];
    private static final long[] frameTexBytes = new long[BENCHMARK_FRAMES];
    private static final long[] frameBufBytes = new long[BENCHMARK_FRAMES];
    private static final int[] frameAllocations = new int[BENCHMARK_FRAMES];

    private static int currentBenchFrame = 0;
    private static boolean isBenchmarking = false;
    private static boolean benchmarkPhase = false;
    private static boolean baselineCompleted = false;

    private static float baselineFPS;
    private static float baseline1PercentLow;
    private static float baselineCpuRenderTime;
    private static int baselineObserverCalls;
    private static long baselineTexBytes;
    private static long baselineBufBytes;
    private static int baselineAllocations;

    private static int baselineShaderCompileCount;
    private static long baselineSpirvTimeNanos;
    private static int baselineShaderCacheHits;
    private static int baselineShaderCacheMisses;
    private static int baselineUniqueVariants;

    public static long cpuFrameStart = 0;
    private static long lastFrameEnd = 0;

    public static void startBenchmark(boolean activePhase) {
        isBenchmarking = true;
        benchmarkPhase = activePhase;
        currentBenchFrame = 0;
        lastFrameEnd = 0;
        Arrays.fill(frameTimes, 0.0f);
        Arrays.fill(cpuRenderTimes, 0.0f);
        Arrays.fill(frameObserverCalls, 0);
        Arrays.fill(frameTexBytes, 0L);
        Arrays.fill(frameBufBytes, 0L);
        Arrays.fill(frameAllocations, 0);

        resetFrameCounters();
        shaderCompileCount = 0;
        spirvCompileTimeNanos = 0;
        shaderCacheHits = 0;
        shaderCacheMisses = 0;

        ENABLED = true;
        GLCallObserver.ACTIVE = activePhase;

        Minecraft.getInstance().gui.getChat().addMessage(
            net.minecraft.network.chat.Component.literal(
                "Â§6[CompatProfiler] Starting 300-frame benchmark phase: " + (activePhase ? "Â§aCOMPAT / OBSERVE" : "Â§bBASELINE")
            )
        );
    }

    public static void recordFrame(float cpuRenderTimeMs) {
        if (!isBenchmarking) return;

        long now = System.nanoTime();
        float overallFrameTime = 0.0f;
        if (lastFrameEnd != 0) {
            overallFrameTime = (now - lastFrameEnd) * 0.000001f;
        } else {
            overallFrameTime = cpuRenderTimeMs;
        }
        lastFrameEnd = now;

        frameTimes[currentBenchFrame] = overallFrameTime;
        cpuRenderTimes[currentBenchFrame] = cpuRenderTimeMs;
        frameObserverCalls[currentBenchFrame] = glObserverCalls;
        frameTexBytes[currentBenchFrame] = textureUploadBytes;
        frameBufBytes[currentBenchFrame] = bufferUploadBytes;
        frameAllocations[currentBenchFrame] = vulkanAllocations;

        currentBenchFrame++;
        if (currentBenchFrame >= BENCHMARK_FRAMES) {
            completePhase();
        }
    }

    public static void resetFrameCounters() {
        glObserverCalls = 0;
        textureUploadBytes = 0;
        bufferUploadBytes = 0;
        vulkanAllocations = 0;
    }

    public static int getUniqueShaderVariants() {
        int count = 0;
        for (Pipeline pipeline : Pipeline.PIPELINES) {
            if (pipeline instanceof GraphicsPipeline gp) {
                count += gp.getVariantCount();
            }
        }
        return count;
    }

    private static void completePhase() {
        isBenchmarking = false;
        ENABLED = false;

        float avgFps = calculateFPS(frameTimes);
        float lowFps = calculate1PercentLow(frameTimes);
        float avgCpuTime = calculateAverage(cpuRenderTimes);

        int totalObserverCalls = sumArray(frameObserverCalls);
        long totalTexBytes = sumArray(frameTexBytes);
        long totalBufBytes = sumArray(frameBufBytes);
        int totalAllocations = sumArray(frameAllocations);

        if (!benchmarkPhase) {

            baselineFPS = avgFps;
            baseline1PercentLow = lowFps;
            baselineCpuRenderTime = avgCpuTime;
            baselineObserverCalls = totalObserverCalls;
            baselineTexBytes = totalTexBytes;
            baselineBufBytes = totalBufBytes;
            baselineAllocations = totalAllocations;

            baselineShaderCompileCount = shaderCompileCount;
            baselineSpirvTimeNanos = spirvCompileTimeNanos;
            baselineShaderCacheHits = shaderCacheHits;
            baselineShaderCacheMisses = shaderCacheMisses;
            baselineUniqueVariants = getUniqueShaderVariants();

            baselineCompleted = true;

            Minecraft.getInstance().gui.getChat().addMessage(
                net.minecraft.network.chat.Component.literal(
                    "Â§a[CompatProfiler] Baseline completed! FPS: " + String.format("%.2f", avgFps) + " | 1% Lows: " + String.format("%.2f", lowFps)
                )
            );
            Minecraft.getInstance().gui.getChat().addMessage(
                net.minecraft.network.chat.Component.literal(
                    "Â§eToggle compatibility options / OBSERVE mode, then press Left ALT + F10 again for the COMPAT phase."
                )
            );
        } else {

            writeReport(avgFps, lowFps, avgCpuTime, totalObserverCalls, totalTexBytes, totalBufBytes, totalAllocations);
            GLCallObserver.ACTIVE = false;
            baselineCompleted = false;
        }
    }

    private static float calculateFPS(float[] times) {
        float sum = 0.0f;
        int count = 0;
        for (float t : times) {
            if (t > 0.0f) {
                sum += t;
                count++;
            }
        }
        if (count == 0) return 0.0f;
        return 1000.0f / (sum / count);
    }

    private static float calculate1PercentLow(float[] times) {
        int validCount = 0;
        for (float t : times) {
            if (t > 0.0f) validCount++;
        }
        if (validCount == 0) return 0.0f;

        float[] sorted = new float[validCount];
        int idx = 0;
        for (float t : times) {
            if (t > 0.0f) {
                sorted[idx++] = t;
            }
        }
        Arrays.sort(sorted);
        int lowCount = Math.max(1, validCount / 100);
        float sum = 0.0f;
        for (int i = validCount - lowCount; i < validCount; i++) {
            sum += sorted[i];
        }
        return 1000.0f / (sum / lowCount);
    }

    private static float calculateAverage(float[] values) {
        float sum = 0.0f;
        for (float v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static int sumArray(int[] values) {
        int sum = 0;
        for (int v : values) {
            sum += v;
        }
        return sum;
    }

    private static long sumArray(long[] values) {
        long sum = 0L;
        for (long v : values) {
            sum += v;
        }
        return sum;
    }

    private static void writeReport(float compatFPS, float compatLow, float compatCpuTime,
                                    int compatObserverCalls, long compatTexBytes, long compatBufBytes, int compatAllocations) {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("      VulkanMod Compatibility & Performance Profile Report\n");
        sb.append("============================================================\n\n");

        sb.append("--- Mod Compatibility Settings ---\n");
        for (String mod : net.vulkanmod.compat.CompatMods.REPORT_MOD_IDS) {
            sb.append(String.format(" * Mod ID: %-18s | Active Mode: %s\n", mod, CompatPolicyManager.getCompatMode(mod)));
        }
        sb.append("\n--- Render Path Ownership Allocations ---\n");
        for (RenderPath path : RenderPath.values()) {
            sb.append(String.format(" * Path: %-15s | Owner: %-12s | Mode: %s\n",
                    path, RenderPathOwnership.getPathOwner(path), RenderPathOwnership.getPathMode(path)));
        }
        sb.append("\n------------------------------------------------------------\n");
        sb.append(String.format(" Performance Metric        | %-14s | %-14s | Delta \n", "Baseline (OFF)", "Observe/Compat"));
        sb.append("------------------------------------------------------------\n");
        sb.append(String.format(" Average FPS               | %-14.2f | %-14.2f | %+.2f%%\n",
                baselineFPS, compatFPS, ((compatFPS - baselineFPS) / baselineFPS) * 100.0f));
        sb.append(String.format(" 1%% Low FPS                | %-14.2f | %-14.2f | %+.2f%%\n",
                baseline1PercentLow, compatLow, ((compatLow - baseline1PercentLow) / baseline1PercentLow) * 100.0f));
        sb.append(String.format(" CPU Render Thread (ms)    | %-14.4f | %-14.4f | %+.2f%%\n",
                baselineCpuRenderTime, compatCpuTime, ((compatCpuTime - baselineCpuRenderTime) / baselineCpuRenderTime) * 100.0f));
        sb.append("------------------------------------------------------------\n");
        sb.append(String.format(" GL Observer Intercepts    | %-14d | %-14d | %+d\n",
                baselineObserverCalls, compatObserverCalls, compatObserverCalls - baselineObserverCalls));
        sb.append(String.format(" Texture Upload Bytes      | %-14d | %-14d | %+d\n",
                baselineTexBytes, compatTexBytes, compatTexBytes - baselineTexBytes));
        sb.append(String.format(" Buffer Upload Bytes       | %-14d | %-14d | %+d\n",
                baselineBufBytes, compatBufBytes, compatBufBytes - baselineBufBytes));
        sb.append(String.format(" Vulkan Device Allocations | %-14d | %-14d | %+d\n",
                baselineAllocations, compatAllocations, compatAllocations - baselineAllocations));

        sb.append("\n--- Shader Cache Diagnostics ---\n");
        int currentVariants = getUniqueShaderVariants();
        double baselineSpirvMs = baselineSpirvTimeNanos * 0.000001;
        double compatSpirvMs = spirvCompileTimeNanos * 0.000001;

        sb.append(String.format(" Shader Compile Count      | %-14d | %-14d | %+d\n",
                baselineShaderCompileCount, shaderCompileCount, shaderCompileCount - baselineShaderCompileCount));
        sb.append(String.format(" Shader Cache Hits         | %-14d | %-14d | %+d\n",
                baselineShaderCacheHits, shaderCacheHits, shaderCacheHits - baselineShaderCacheHits));
        sb.append(String.format(" Shader Cache Misses       | %-14d | %-14d | %+d\n",
                baselineShaderCacheMisses, shaderCacheMisses, shaderCacheMisses - baselineShaderCacheMisses));
        sb.append(String.format(" Unique Shader Variants    | %-14d | %-14d | %+d\n",
                baselineUniqueVariants, currentVariants, currentVariants - baselineUniqueVariants));
        sb.append(String.format(" SPIR-V Compile Time (ms)  | %-14.4f | %-14.4f | %+.2f%%\n",
                baselineSpirvMs, compatSpirvMs, baselineSpirvMs > 0 ? ((compatSpirvMs - baselineSpirvMs) / baselineSpirvMs) * 100.0f : 0.0f));
        sb.append("============================================================\n");

        try (FileWriter writer = new FileWriter("vulkanmod_compat_profile.txt")) {
            writer.write(sb.toString());
            Minecraft.getInstance().gui.getChat().addMessage(
                net.minecraft.network.chat.Component.literal(
                    "Â§a[CompatProfiler] Benchmark complete! Saved performance profile comparison report to: Â§evulkanmod_compat_profile.txt"
                )
            );
        } catch (IOException e) {
            Minecraft.getInstance().gui.getChat().addMessage(
                net.minecraft.network.chat.Component.literal(
                    "Â§c[CompatProfiler] Failed to write report: " + e.getMessage()
                )
            );
        }
    }

    public static boolean isBenchmarking() {
        return isBenchmarking;
    }

    public static boolean hasBaseline() {
        return baselineCompleted;
    }
}

