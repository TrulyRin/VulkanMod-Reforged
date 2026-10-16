package net.vulkanmod.config;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.server.level.ParticleStatus;
import net.vulkanmod.render.chunk.build.light.LightMode;

public enum PerformancePreset {

                                                                                          
    CUSTOM(0, "vulkanmod.options.performancePreset.custom",
            new ChunkOptions(false, true, 8, 2, true, true, true, 2, true),
            new GraphicsOptions(10, 6, ParticleStatus.DECREASED, CloudStatus.OFF, false, 75, 1, LightMode.SMOOTH, 1,
                    TextureFilteringMethod.NONE, 1, 32, 5, false, 0, false, PrioritizeChunkUpdates.NONE)),
    POTATO(1, "vulkanmod.options.performancePreset.potato",
            new ChunkOptions(true, true, 3, 1, true, true, true, 3, true),
            new GraphicsOptions(5, 5, ParticleStatus.MINIMAL, CloudStatus.OFF, false, 50, 0, LightMode.FLAT, 0,
                    TextureFilteringMethod.NONE, 1, 8, 3, false, 0, false, PrioritizeChunkUpdates.NONE)),
    BALANCED(2, "vulkanmod.options.performancePreset.balanced",
            new ChunkOptions(true, true, 5, 2, true, true, true, 2, true),
            new GraphicsOptions(8, 6, ParticleStatus.DECREASED, CloudStatus.OFF, false, 75, 1, LightMode.SMOOTH, 2,
                    TextureFilteringMethod.NONE, 1, 32, 5, false, 0, false, PrioritizeChunkUpdates.NONE)),
    QUALITY(3, "vulkanmod.options.performancePreset.quality",
            new ChunkOptions(true, true, 16, 2, true, true, true, 1, true),
            new GraphicsOptions(16, 12, ParticleStatus.ALL, CloudStatus.FANCY, true, 100, 2, LightMode.SMOOTH, 4,
                    TextureFilteringMethod.ANISOTROPIC, 2, 64, 10, true, 5, true, PrioritizeChunkUpdates.PLAYER_AFFECTED));

    public final int id;
    public final String translationKey;
    public final boolean indirectDraw;
    public final boolean adaptiveChunkUploads;
    public final int chunkUploadsPerFrame;
    public final int advCulling;
    public final boolean entityCulling;
    public final boolean blockEntityCulling;
    public final boolean leavesCulling;
    public final int particleCulling;
    public final boolean uniqueOpaqueLayer;
    public final int renderDistance;
    public final int simulationDistance;
    public final ParticleStatus particleStatus;
    public final CloudStatus cloudStatus;
    public final boolean entityShadows;
    public final int entityDistancePercent;
    public final int biomeBlendRadius;
    public final int ambientOcclusion;
    public final int mipmapLevels;
    public final TextureFilteringMethod textureFiltering;
    public final int maxAnisotropyBit;
    public final int cloudRange;
    public final int weatherRadius;
    public final boolean cutoutLeaves;
    public final int menuBackgroundBlurriness;
    public final boolean vignette;
    public final PrioritizeChunkUpdates prioritizeChunkUpdates;

    PerformancePreset(int id, String translationKey, ChunkOptions chunks, GraphicsOptions graphics) {
        this.id = id;
        this.translationKey = translationKey;
        this.indirectDraw = chunks.indirectDraw;
        this.adaptiveChunkUploads = chunks.adaptiveChunkUploads;
        this.chunkUploadsPerFrame = chunks.chunkUploadsPerFrame;
        this.advCulling = chunks.advCulling;
        this.entityCulling = chunks.entityCulling;
        this.blockEntityCulling = chunks.blockEntityCulling;
        this.leavesCulling = chunks.leavesCulling;
        this.particleCulling = chunks.particleCulling;
        this.uniqueOpaqueLayer = chunks.uniqueOpaqueLayer;
        this.renderDistance = graphics.renderDistance;
        this.simulationDistance = graphics.simulationDistance;
        this.particleStatus = graphics.particleStatus;
        this.cloudStatus = graphics.cloudStatus;
        this.entityShadows = graphics.entityShadows;
        this.entityDistancePercent = graphics.entityDistancePercent;
        this.biomeBlendRadius = graphics.biomeBlendRadius;
        this.ambientOcclusion = graphics.ambientOcclusion;
        this.mipmapLevels = graphics.mipmapLevels;
        this.textureFiltering = graphics.textureFiltering;
        this.maxAnisotropyBit = graphics.maxAnisotropyBit;
        this.cloudRange = graphics.cloudRange;
        this.weatherRadius = graphics.weatherRadius;
        this.cutoutLeaves = graphics.cutoutLeaves;
        this.menuBackgroundBlurriness = graphics.menuBackgroundBlurriness;
        this.vignette = graphics.vignette;
        this.prioritizeChunkUpdates = graphics.prioritizeChunkUpdates;
    }

    private record ChunkOptions(boolean indirectDraw, boolean adaptiveChunkUploads, int chunkUploadsPerFrame,
                                int advCulling, boolean entityCulling, boolean blockEntityCulling,
                                boolean leavesCulling, int particleCulling, boolean uniqueOpaqueLayer) {}

    private record GraphicsOptions(int renderDistance, int simulationDistance, ParticleStatus particleStatus,
                                   CloudStatus cloudStatus, boolean entityShadows, int entityDistancePercent,
                                   int biomeBlendRadius, int ambientOcclusion, int mipmapLevels,
                                   TextureFilteringMethod textureFiltering, int maxAnisotropyBit,
                                   int cloudRange, int weatherRadius, boolean cutoutLeaves,
                                   int menuBackgroundBlurriness, boolean vignette,
                                   PrioritizeChunkUpdates prioritizeChunkUpdates) {}

    public static PerformancePreset byId(int id) {
        for (PerformancePreset preset : values()) {
            if (preset.id == id) return preset;
        }
        return CUSTOM;
    }
}
