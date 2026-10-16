package net.vulkanmod.compat.doctor;

public enum DoctorRule {
    INCOMPATIBLE_RENDERER_MOD(100),
    MOD_INVENTORY_UNAVAILABLE(90),
    RUNTIME_RENDER_FAILURE(80),
    MISSING_OR_STUBBED_GL_ENTRYPOINT(70),
    GL_CONTRACT_GAP(60),
    VERSION_MISMATCH(50),
    UNKNOWN_RECOGNIZED_VERSION(40),
    CONSERVATIVE_RENDERING_TROUBLESHOOTER(30),
    BENCHMARK_STUTTER(20);

    private final int rank;

    DoctorRule(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
