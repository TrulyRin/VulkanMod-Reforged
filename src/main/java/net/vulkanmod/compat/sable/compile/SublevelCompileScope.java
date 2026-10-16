package net.vulkanmod.compat.sable.compile;

import java.util.Optional;
import java.util.concurrent.Callable;

public final class SublevelCompileScope {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private SublevelCompileScope() {
    }

    public static Token enter(Context context) {
        Context previous = CURRENT.get();
        if (context == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(context);
        }
        return () -> {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        };
    }

    public static Optional<Context> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static <T> Callable<T> capture(Callable<T> task) {
        Context captured = CURRENT.get();
        return () -> {
            try (Token ignored = enter(captured)) {
                return task.call();
            }
        };
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Context(Object transform, int sublevelId, int lightingSceneId) {
    }

    @FunctionalInterface
    public interface Token extends AutoCloseable {
        @Override
        void close();
    }
}
