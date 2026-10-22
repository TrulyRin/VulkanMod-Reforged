package net.vulkanmod.compat;

public enum CompatMode {
    OFF,
    OBSERVE,
    SAFE,
    BRIDGED,
    FAST,
    INCOMPATIBLE;

    public boolean enablesAdapter() {
        return this == SAFE || this == BRIDGED || this == FAST;
    }
}
