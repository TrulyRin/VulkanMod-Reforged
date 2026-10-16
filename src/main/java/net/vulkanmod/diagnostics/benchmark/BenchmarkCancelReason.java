package net.vulkanmod.diagnostics.benchmark;

public enum BenchmarkCancelReason {
    NONE,
    WORLD_CHANGED,
    SCREEN_OPENED,
    PAUSED,
    FOCUS_LOST,
    INVALID_CLIENT,
    SETTINGS_CHANGED,
    EXPLICIT,
    TOO_FEW_FRAMES
}
