package net.vulkanmod.config;

import net.minecraft.client.Minecraft;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.optimization.AdaptiveChunkUploadBudget;
import net.vulkanmod.vulkan.device.DeviceManager;

public final class PerformancePresetApplier {
    private PerformancePresetApplier() {
    }

    public static void apply(PerformancePreset preset, Config config, Minecraft minecraft) {
        config.performancePreset = preset.id;

        if (preset == PerformancePreset.CUSTOM) {
            return;
        }

        config.advCulling = preset.advCulling;
        config.entityCulling = preset.entityCulling;
        config.blockEntityCulling = preset.blockEntityCulling;
        config.leavesCulling = preset.leavesCulling;
        config.particleCulling = preset.particleCulling;
        config.uniqueOpaqueLayer = preset.uniqueOpaqueLayer;
        config.indirectDraw = preset.indirectDraw && DeviceManager.supportsFastIndirectDraw();
        config.chunkUploadsPerFrame = preset.chunkUploadsPerFrame;
        config.adaptiveChunkUploads = preset.adaptiveChunkUploads;
        config.ambientOcclusion = preset.ambientOcclusion;

        if (minecraft != null && minecraft.options != null) {
            var options = minecraft.options;
            options.renderDistance().set(preset.renderDistance);
            options.simulationDistance().set(preset.simulationDistance);
            options.graphicsMode().set(preset.graphicsStatus);
            options.particles().set(preset.particleStatus);
            options.cloudStatus().set(preset.cloudStatus);
            options.entityShadows().set(preset.entityShadows);
            options.entityDistanceScaling().set(preset.entityDistancePercent * 0.01);
            options.biomeBlendRadius().set(preset.biomeBlendRadius);
            options.ambientOcclusion().set(preset.ambientOcclusion > LightMode.FLAT);
            options.mipmapLevels().set(preset.mipmapLevels);
            options.save();

            if (minecraft.levelRenderer != null) {
                minecraft.levelRenderer.allChanged();
            }
        }

        AdaptiveChunkUploadBudget.reset();
    }
}
