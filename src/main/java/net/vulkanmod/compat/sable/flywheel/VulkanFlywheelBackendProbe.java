package net.vulkanmod.compat.sable.flywheel;

import net.vulkanmod.vulkan.Renderer;

final class VulkanFlywheelBackendProbe {
    private VulkanFlywheelBackendProbe() {
    }

    static boolean probe() {
        return Renderer.getInstance() != null
                && Boolean.getBoolean("vulkanmod.compat.sable.flywheel.experimental");
    }
}
