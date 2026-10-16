package net.vulkanmod.compat.diagnostics;

public enum SessionIssueType {
    CONTRACT_GAP,
    MISSING_GL_ENTRYPOINT,
    STUBBED_GL_ENTRYPOINT,
    TRAMPOLINE_FAULT,
    EXTERNAL_FAILURE,
    CHUNK_BUILD_FAILURE,
    MOD_INVENTORY_UNAVAILABLE
}
