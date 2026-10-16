package net.vulkanmod.diagnostics.benchmark;

import java.util.Objects;

public record BenchmarkFingerprint(
        String dimensionContext,
        String playContext,
        int framebufferWidth,
        int framebufferHeight,
        int renderScale,
        int renderDistance,
        int simulationDistance,
        String graphicsMode,
        boolean vsync,
        int fpsCap,
        int performancePreset,
        boolean indirectDraw,
        boolean uniqueOpaqueLayer,
        boolean adaptiveChunkUploads,
        int chunkUploadsPerFrame) {

    private static final boolean DEFAULT_ADAPTIVE_CHUNK_UPLOADS = true;
    private static final int DEFAULT_CHUNK_UPLOADS_PER_FRAME = 5;

    public BenchmarkFingerprint {
        dimensionContext = Objects.requireNonNull(dimensionContext, "dimensionContext");
        playContext = Objects.requireNonNull(playContext, "playContext");
        graphicsMode = Objects.requireNonNull(graphicsMode, "graphicsMode");
    }

    public BenchmarkFingerprint(
            String dimensionContext,
            String playContext,
            int framebufferWidth,
            int framebufferHeight,
            int renderScale,
            int renderDistance,
            int simulationDistance,
            String graphicsMode,
            boolean vsync,
            int fpsCap,
            int performancePreset) {
        this(dimensionContext, playContext, framebufferWidth, framebufferHeight,
                renderScale, renderDistance, simulationDistance, graphicsMode,
                vsync, fpsCap, performancePreset, false, false);
    }

    public BenchmarkFingerprint(
            String dimensionContext,
            String playContext,
            int framebufferWidth,
            int framebufferHeight,
            int renderScale,
            int renderDistance,
            int simulationDistance,
            String graphicsMode,
            boolean vsync,
            int fpsCap,
            int performancePreset,
            boolean indirectDraw,
            boolean uniqueOpaqueLayer) {
        this(dimensionContext, playContext, framebufferWidth, framebufferHeight,
                renderScale, renderDistance, simulationDistance, graphicsMode,
                vsync, fpsCap, performancePreset, indirectDraw, uniqueOpaqueLayer,
                DEFAULT_ADAPTIVE_CHUNK_UPLOADS, DEFAULT_CHUNK_UPLOADS_PER_FRAME);
    }

    public static BenchmarkFingerprint unavailable() {
        return new BenchmarkFingerprint("menu", "none", 0, 0, 100, 0, 0,
                "unknown", false, 0, 0, false, false, false, 0);
    }
}
