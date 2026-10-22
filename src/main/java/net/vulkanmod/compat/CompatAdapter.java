package net.vulkanmod.compat;

public interface CompatAdapter {
    String getModId();
    String getDisplayName();
    boolean isAvailable();
    void init();
    void onEnable();
    void onDisable();
}
