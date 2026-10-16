package net.vulkanmod.compat;

import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public final class ExternalClientFaultBoundary {
    private static final int DIAGNOSTIC_KEY_LIMIT = 256;
    private static final String PREFIX_RESOURCE = "/assets/vulkanmod/compat/client_tick_exception_prefixes.txt";
    private static final Set<String> STACK_PREFIXES = loadStackPrefixes();
    private static final Set<String> REPORTED_FAILURES = ConcurrentHashMap.newKeySet();

    private ExternalClientFaultBoundary() {
    }

    public static void runClientTick(Minecraft minecraft) {
        runGuarded(minecraft::tick, "client tick");
    }

    public static void runGuarded(Runnable runnable, String phase) {
        runGuarded(runnable, phase, ExternalClientFaultBoundary::logSuppressedFailure, true);
    }

    public static void runGuarded(Runnable runnable, String phase, BiConsumer<String, RuntimeException> reporter) {
        runGuarded(runnable, phase, reporter, false);
    }

    private static void runGuarded(Runnable runnable, String phase, BiConsumer<String, RuntimeException> reporter, boolean rateLimit) {
        try {
            runnable.run();
        } catch (RuntimeException exception) {
            if (!shouldSuppress(exception)) {
                throw exception;
            }

            String key = firstMatchingStackClass(exception);
            if (!rateLimit || addReportedFailure(key)) {
                reporter.accept(phase + ":" + key, exception);
            }
        }
    }

    public static boolean shouldSuppress(Throwable throwable) {
        return firstMatchingStackClass(throwable) != null;
    }

    private static String firstMatchingStackClass(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            for (StackTraceElement frame : current.getStackTrace()) {
                String className = frame.getClassName();
                if (isAllowlisted(className)) {
                    return className;
                }
            }
        }

        return null;
    }

    private static boolean isAllowlisted(String className) {
        for (String prefix : STACK_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> loadStackPrefixes() {
        try (InputStream stream = ExternalClientFaultBoundary.class.getResourceAsStream(PREFIX_RESOURCE)) {
            if (stream == null) {
                return Set.of();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .map(String::trim)
                        .filter(line -> !line.isEmpty())
                        .filter(line -> !line.startsWith("#"))
                        .collect(Collectors.toUnmodifiableSet());
            }
        } catch (Exception exception) {
            Initializer.LOGGER.warn("Unable to load external client fault boundary prefixes", exception);
            return Set.of();
        }
    }

    private static void logSuppressedFailure(String key, RuntimeException exception) {
        Initializer.LOGGER.warn("Suppressed external failure at {}. Gameplay may continue with that external feature disabled for this session.",
                key, exception);
    }

    public static Set<String> reportedFailures() {
        return Set.copyOf(new TreeSet<>(REPORTED_FAILURES));
    }

    private static boolean addReportedFailure(String key) {
        if (key == null || REPORTED_FAILURES.contains(key) || REPORTED_FAILURES.size() >= DIAGNOSTIC_KEY_LIMIT) {
            return false;
        }
        return REPORTED_FAILURES.add(key);
    }
}
