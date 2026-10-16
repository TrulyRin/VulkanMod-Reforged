package net.vulkanmod.compat.observer;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F10;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

                                                                                           
public final class CompatBenchmarkShortcut {
    public enum Phase { NONE, BASELINE, OBSERVE }

    private CompatBenchmarkShortcut() { }

    public static Phase selectPhase(int key, int action, boolean leftAltDown, boolean matchingWindow,
                                    boolean inWorld, boolean screenOpen, boolean alreadyRunning,
                                    boolean baselineCompleted) {
        if (key != GLFW_KEY_F10 || action != GLFW_PRESS || !leftAltDown || !matchingWindow
                || !inWorld || screenOpen || alreadyRunning) return Phase.NONE;
        return baselineCompleted ? Phase.OBSERVE : Phase.BASELINE;
    }
}
