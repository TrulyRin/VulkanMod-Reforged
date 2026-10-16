package net.vulkanmod.compat.sable.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class SublevelRenderScope {
    private static final AtomicLong IDS = new AtomicLong();
    private static final ThreadLocal<Deque<State>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private SublevelRenderScope() {
    }

    public static Token enter(Pass pass, Object shader) {
        return enter(pass, shader, () -> {
        });
    }

    public static Token enter(Pass pass, Object shader, Runnable restore) {
        State state = new State(IDS.incrementAndGet(), pass, shader, Thread.currentThread(), restore);
        STACK.get().push(state);
        return new Token(state);
    }

    public static Optional<State> current() {
        return Optional.ofNullable(STACK.get().peek());
    }

    public static boolean isActive() {
        return !STACK.get().isEmpty();
    }

    public static int depth() {
        return STACK.get().size();
    }

    public static Object activeShader(Supplier<Object> currentShaderSupplier) {
        State state = STACK.get().peek();
        if (state == null) {
            return null;
        }
        return state.shader() != null ? state.shader() : currentShaderSupplier.get();
    }

    public static int forceUnwind() {
        Deque<State> stack = STACK.get();
        int leaked = stack.size();
        while (!stack.isEmpty()) {
            stack.pop().restore().run();
        }
        STACK.remove();
        return leaked;
    }

    public enum Pass {
        TERRAIN,
        SINGLE_BLOCK,
        TRANSLUCENT,
        FLUID,
        AFTER_SECTIONS,
        ENTITIES,
        BLOCK_ENTITIES
    }

    public record State(long id, Pass pass, Object shader, Thread owner, Runnable restore) {
    }

    public static final class Token implements AutoCloseable {
        private final State state;
        private boolean closed;

        private Token(State state) {
            this.state = state;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            Deque<State> stack = STACK.get();
            if (stack.peek() != state) {
                throw new IllegalStateException("Sublevel render scopes must close in LIFO order");
            }
            stack.pop();
            closed = true;
            state.restore().run();
            if (stack.isEmpty()) {
                STACK.remove();
            }
        }
    }
}
