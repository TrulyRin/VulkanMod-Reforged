package net.vulkanmod.compat.opengl;

                                                                       
public final class GlDispatchGate {
    private GlDispatchGate() { }

    public static boolean shouldEmulate(boolean vulkanBackendReady, long currentOpenGlContext) {
        return vulkanBackendReady && currentOpenGlContext == 0L;
    }
}
