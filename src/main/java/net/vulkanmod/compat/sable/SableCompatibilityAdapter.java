package net.vulkanmod.compat.sable;

import net.vulkanmod.Initializer;
import net.vulkanmod.compat.CompatAdapter;
import net.vulkanmod.compat.CompatDetector;

public final class SableCompatibilityAdapter implements CompatAdapter {
    private static volatile SableCapabilityStatus status = SableCapabilityStatus.disabled();
    private SableContractProbe.Result probe = SableContractProbe.evaluate("UNKNOWN", java.util.Set.of());

    @Override
    public String getModId() {
        return "sable";
    }

    @Override
    public String getDisplayName() {
        return "Sable";
    }

    @Override
    public boolean isAvailable() {
        return SableCompatibilityOptions.mode().enabled() && CompatDetector.isModLoaded(getModId());
    }

    @Override
    public void init() {
        if (!isAvailable()) {
            return;
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        probe = SableContractProbe.inspect(CompatDetector.getModVersion(getModId()), loader);
    }

    @Override
    public void onEnable() {
        if (!isAvailable() || !probe.compatible()) {
            status = SableCapabilityStatus.disabled();
            if (isAvailable()) {
                Initializer.LOGGER.warn("Sable compatibility disabled because required renderer contracts were not found: {}",
                        probe.availableContracts());
            }
            return;
        }
        boolean veil = CompatDetector.isModLoaded("veil") || classPresent("foundry.veil.api.client.render.VeilRenderSystem");
        boolean flywheel = flywheelBackendSupported();
        status = SableCapabilityStatus.fromProbe(probe, veil, flywheel);
        Initializer.LOGGER.info("Sable compatibility active for {} with capabilities {}",
                CompatDetector.getModVersion(getModId()), status.enabledCapabilities());
    }

    @Override
    public void onDisable() {
        status = SableCapabilityStatus.disabled();
    }

    public static SableCapabilityStatus status() {
        return status;
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean flywheelBackendSupported() {
        if (!classPresent("dev.engine_room.flywheel.api.backend.Backend")) {
            return false;
        }
        try {
            Class<?> backend = Class.forName("net.vulkanmod.compat.sable.flywheel.VulkanFlywheelBackend", false,
                    Thread.currentThread().getContextClassLoader());
            Object instance = backend.getMethod("register").invoke(null);
            if (!(instance instanceof Boolean registered) || !registered) {
                return false;
            }
            Object registeredBackend = backend.getField("ID").get(null);
            return registeredBackend != null && Boolean.getBoolean("vulkanmod.compat.sable.flywheel.experimental");
        } catch (Throwable ignored) {
            return false;
        }
    }
}
