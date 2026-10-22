package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public final class GlSync {
    public static final int GL_OBJECT_TYPE = 0x9112;
    public static final int GL_SYNC_CONDITION = 0x9113;
    public static final int GL_SYNC_STATUS = 0x9114;
    public static final int GL_SYNC_FLAGS = 0x9115;
    public static final int GL_SYNC_FENCE = 0x9116;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    public static final int GL_SIGNALED = 0x9119;
    public static final int GL_ALREADY_SIGNALED = 0x911A;

    private static long HANDLE_COUNTER = 0x564B_0000_0001L;
    private static final LongOpenHashSet liveHandles = new LongOpenHashSet();

    private GlSync() {
    }

    public static long fenceSync(int condition, int flags) {
        long handle = HANDLE_COUNTER++;
        liveHandles.add(handle);
        return handle;
    }

    public static boolean isSync(long sync) {
        return liveHandles.contains(sync);
    }

    public static void deleteSync(long sync) {
        liveHandles.remove(sync);
    }

    public static int clientWaitSync(long sync, int flags, long timeout) {
        return GL_ALREADY_SIGNALED;
    }

    public static void waitSync(long sync, int flags, long timeout) {
    }

    public static int getSynci(long sync, int pname) {
        return switch (pname) {
            case GL_OBJECT_TYPE -> GL_SYNC_FENCE;
            case GL_SYNC_CONDITION -> GL_SYNC_GPU_COMMANDS_COMPLETE;
            case GL_SYNC_STATUS -> GL_SIGNALED;
            default -> 0;
        };
    }
}
