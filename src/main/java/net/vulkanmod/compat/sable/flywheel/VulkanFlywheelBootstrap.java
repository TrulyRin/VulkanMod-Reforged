package net.vulkanmod.compat.sable.flywheel;

import net.vulkanmod.Initializer;

public final class VulkanFlywheelBootstrap {
    private VulkanFlywheelBootstrap() {
    }

    public static boolean registerIfPresent() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null || loader.getResource("dev/engine_room/flywheel/api/backend/Backend.class") == null) {
            return false;
        }
        try {
            Class<?> backend = Class.forName(
                    "net.vulkanmod.compat.sable.flywheel.VulkanFlywheelBackend", true, loader);
            return (boolean) backend.getMethod("register").invoke(null);
        } catch (Throwable throwable) {
            Initializer.LOGGER.warn("Unable to register the optional Vulkan Flywheel backend", throwable);
            return false;
        }
    }
}
