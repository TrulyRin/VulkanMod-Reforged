package net.vulkanmod.compat.opengl;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

public final class GlCapabilitiesFallback {
    private static boolean installed;

    private GlCapabilitiesFallback() {
    }

    public static synchronized void install() {
        if (installed) {
            return;
        }

        GL.destroy();
        GL.create(EmulatedGlFunctionProvider.INSTANCE);
        GLCapabilities capabilities = GL.createCapabilities(true);
        installed = true;

        GlTrampolines.logInfo(
                "Installed emulated GL capabilities: {} functions registered, version={}, GL20={}, GL30={}, GL33={}",
                GlFunctionRegistry.registeredCount(), GlFunctionRegistry.REPORTED_GL_VERSION,
                capabilities.OpenGL20, capabilities.OpenGL30, capabilities.OpenGL33);
    }
}
