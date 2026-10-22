package net.vulkanmod.config;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.ParticleStatus;
import net.vulkanmod.render.chunk.build.light.LightMode;

public enum PerformancePreset {

    CUSTOM(0, "vulkanmod.options.performancePreset.custom", false, true, 8, 2, true, true, true, 2, true, 10, 6, GraphicsStatus.FAST, ParticleStatus.DECREASED, CloudStatus.OFF, false, 75, 1, LightMode.SMOOTH, 1),
    POTATO(1, "vulkanmod.options.performancePreset.potato", true, true, 3, 1, true, true, true, 3, true, 5, 5, GraphicsStatus.FAST, ParticleStatus.MINIMAL, CloudStatus.OFF, false, 50, 0, LightMode.FLAT, 0),
    BALANCED(2, "vulkanmod.options.performancePreset.balanced", true, true, 5, 2, true, true, true, 2, true, 8, 6, GraphicsStatus.FAST, ParticleStatus.DECREASED, CloudStatus.OFF, false, 75, 1, LightMode.SMOOTH, 2),
    QUALITY(3, "vulkanmod.options.performancePreset.quality", true, true, 16, 2, true, true, true, 1, true, 16, 16, GraphicsStatus.FANCY, ParticleStatus.ALL, CloudStatus.FANCY, true, 100, 5, LightMode.SMOOTH, 4);

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
    public final GraphicsStatus graphicsStatus;
    public final ParticleStatus particleStatus;
    public final CloudStatus cloudStatus;
    public final boolean entityShadows;
    public final int entityDistancePercent;
    public final int biomeBlendRadius;
    public final int ambientOcclusion;
    public final int mipmapLevels;

    PerformancePreset(int id, String translationKey, boolean indirectDraw, boolean adaptiveChunkUploads,
                      int chunkUploadsPerFrame, int advCulling,
                      boolean entityCulling, boolean blockEntityCulling, boolean leavesCulling, int particleCulling,
                      boolean uniqueOpaqueLayer, int renderDistance, int simulationDistance,
                      GraphicsStatus graphicsStatus, ParticleStatus particleStatus, CloudStatus cloudStatus,
                      boolean entityShadows, int entityDistancePercent, int biomeBlendRadius, int ambientOcclusion,
                      int mipmapLevels) {
        this.id = id;
        this.translationKey = translationKey;
        this.indirectDraw = indirectDraw;
        this.adaptiveChunkUploads = adaptiveChunkUploads;
        this.chunkUploadsPerFrame = chunkUploadsPerFrame;
        this.advCulling = advCulling;
        this.entityCulling = entityCulling;
        this.blockEntityCulling = blockEntityCulling;
        this.leavesCulling = leavesCulling;
        this.particleCulling = particleCulling;
        this.uniqueOpaqueLayer = uniqueOpaqueLayer;
        this.renderDistance = renderDistance;
        this.simulationDistance = simulationDistance;
        this.graphicsStatus = graphicsStatus;
        this.particleStatus = particleStatus;
        this.cloudStatus = cloudStatus;
        this.entityShadows = entityShadows;
        this.entityDistancePercent = entityDistancePercent;
        this.biomeBlendRadius = biomeBlendRadius;
        this.ambientOcclusion = ambientOcclusion;
        this.mipmapLevels = mipmapLevels;
    }

    public static PerformancePreset byId(int id) {
        for (PerformancePreset preset : values()) {
            if (preset.id == id) {
                return preset;
            }
        }

        return CUSTOM;
    }
}
