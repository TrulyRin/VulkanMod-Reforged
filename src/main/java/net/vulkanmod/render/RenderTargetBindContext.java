package net.vulkanmod.render;


public final class RenderTargetBindContext {
    private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);
    private static final class State { int depth; }

    private RenderTargetBindContext() { }

    public static boolean isInternal() {
        return STATE.get().depth != 0;
    }


    public static void runInternal(Runnable bind) {
        State state = STATE.get();
        int previous = state.depth;
        state.depth = previous + 1;
        try {
            bind.run();
        } finally {
            state.depth = previous;
        }
    }
}
