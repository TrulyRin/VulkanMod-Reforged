package net.vulkanmod.compat.sable;

import java.util.EnumSet;
import java.util.Set;

public record SableCapabilityStatus(Set<SableCapability> enabledCapabilities) {
    public SableCapabilityStatus {
        enabledCapabilities = Set.copyOf(enabledCapabilities);
    }

    public static SableCapabilityStatus disabled() {
        return new SableCapabilityStatus(Set.of());
    }

    public static SableCapabilityStatus fromProbe(SableContractProbe.Result probe,
                                                   boolean veilAvailable,
                                                   boolean flywheelAvailable) {
        if (!probe.compatible()) {
            return disabled();
        }
        EnumSet<SableCapability> enabled = EnumSet.of(
                SableCapability.TERRAIN_TRANSFORMS,
                SableCapability.LAYERED_RENDERING,
                SableCapability.ENTITY_RENDERING,
                SableCapability.LIGHTING_AND_TINT);
        if (veilAvailable) {
            enabled.add(SableCapability.FRAMEBUFFER_INTEROPERABILITY);
        }
        if (flywheelAvailable) {
            enabled.add(SableCapability.NATIVE_FLYWHEEL_BACKEND);
        }
        return new SableCapabilityStatus(enabled);
    }

    public boolean enabled(SableCapability capability) {
        return enabledCapabilities.contains(capability);
    }
}
