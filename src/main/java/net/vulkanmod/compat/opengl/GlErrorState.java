package net.vulkanmod.compat.opengl;

                                                                                      
final class GlErrorState {
    private static final ThreadLocal<Integer> ERROR = ThreadLocal.withInitial(() -> 0);

    private GlErrorState() { }

    static void record(int error) {
        if (ERROR.get() == 0) ERROR.set(error);
    }

    static int drain() {
        int result = ERROR.get();
        ERROR.set(0);
        return result;
    }
}
