package net.vulkanmod.compat.opengl;

import org.lwjgl.PointerBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.FunctionProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.function.IntFunction;

                                                                                                   
public final class GlCapabilitiesFallback {
    private static volatile GLCapabilities capabilities;

    private GlCapabilitiesFallback() { }

    public static GLCapabilities createVerifiedCapabilities() {
        try {
            Constructor<GLCapabilities> constructor = GLCapabilities.class.getDeclaredConstructor(
                    FunctionProvider.class, Set.class, boolean.class, IntFunction.class);
            if (!constructor.trySetAccessible()) {
                throw new IllegalStateException("LWJGL's OpenGL module does not open capability construction; native GL remains intact");
            }
                                                                                
                                                                              
                                                                                 
                                                                          
            Set<String> lookupGroups = Set.of("OpenGL11", "OpenGL15", "OpenGL30", "OpenGL31", "OpenGL32",
                    "GL_ARB_vertex_buffer_object");
            return constructor.newInstance(EmulatedGlFunctionProvider.INSTANCE, lookupGroups, true,
                    (IntFunction<PointerBuffer>) BufferUtils::createPointerBuffer);
        } catch (ReflectiveOperationException failure) {
            Throwable cause = failure instanceof InvocationTargetException target ? target.getCause() : failure;
            throw new IllegalStateException("Unable to construct verified OpenGL compatibility capabilities", cause);
        }
    }

    public static synchronized boolean install(boolean vulkanBackendReady) {
        if (!GlDispatchGate.shouldEmulate(vulkanBackendReady, GLFW.glfwGetCurrentContext())) {
            return false;
        }
        GLCapabilities verified = capabilities;
        if (verified == null) {
            verified = createVerifiedCapabilities();
            capabilities = verified;
        }
        GL.setCapabilities(verified);
        return true;
    }

    public static boolean shouldEmulate() {
        return capabilities != null && GlDispatchGate.shouldEmulate(true, GLFW.glfwGetCurrentContext());
    }

    public static GLCapabilities forCurrentThread() {
        GLCapabilities verified = capabilities;
        if (verified == null || !shouldEmulate()) {
            throw new IllegalStateException("Verified GL emulation is not active on this contextless thread");
        }
        GL.setCapabilities(verified);
        return verified;
    }
}
