package net.vulkanmod.compat;

public final class RuntimeOptions {
    public static final String DIAGNOSTICS_PROPERTY = "vulkanmod.diagnostics";
    public static final String HUD_TRACE_PROPERTY = "vulkanmod.hudTrace";
    public static final String PROFILING_MIXINS_PROPERTY = "vulkanmod.profilingMixins";
    public static final String DEBUG_MIXINS_PROPERTY = "vulkanmod.debugMixins";

    private RuntimeOptions() {
    }

    public static boolean diagnosticsEnabled() {
        return Boolean.getBoolean(DIAGNOSTICS_PROPERTY);
    }

    public static boolean hudTraceEnabled() {
        return diagnosticsEnabled() && hudTraceRequested();
    }

    public static boolean hudTraceRequested() {
        return Boolean.getBoolean(HUD_TRACE_PROPERTY);
    }

    public static boolean profilingMixinsEnabled() {
        return Boolean.getBoolean(PROFILING_MIXINS_PROPERTY);
    }

    public static boolean debugMixinsEnabled() {
        return Boolean.getBoolean(DEBUG_MIXINS_PROPERTY);
    }

    public static boolean externalLodEnabled() {
        return net.vulkanmod.compat.capabilities.ExternalRenderPathOptions.externalLodEnabled();
    }

    public static boolean externalLodDrawEnabled() {
        return net.vulkanmod.compat.capabilities.ExternalRenderPathOptions.externalLodDrawEnabled();
    }

    public static boolean externalLodDebugDrawEnabled() {
        return net.vulkanmod.compat.capabilities.ExternalRenderPathOptions.externalLodDebugDrawEnabled();
    }
}
