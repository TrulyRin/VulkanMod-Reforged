package net.vulkanmod.compat.sable.flywheel;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.Engine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.vulkanmod.Initializer;

public final class VulkanFlywheelBackend implements Backend {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("vulkanmod", "vulkan");
    private static final VulkanFlywheelBackend INSTANCE = new VulkanFlywheelBackend();
    private static volatile boolean registered;
    private static volatile boolean baselineReady;

    private VulkanFlywheelBackend() {
    }

    public static synchronized boolean register() {
        if (registered) {
            return true;
        }
        if (Backend.REGISTRY.isFrozen()) {
            Initializer.LOGGER.warn("Flywheel registry froze before the Vulkan backend could register");
            return false;
        }
        Backend.REGISTRY.register(ID, INSTANCE);
        registered = true;
        baselineReady = true;
        return true;
    }

    @Override
    public Engine createEngine(LevelAccessor level) {
        if (!isSupported()) {
            throw new IllegalStateException("Vulkan Flywheel baseline is not available");
        }
        return new VulkanFlywheelEngine(level);
    }

    @Override
    public int priority() {
        return 750;
    }

    @Override
    public boolean isSupported() {
        return registered && baselineReady && VulkanFlywheelBackendProbe.probe();
    }
}
