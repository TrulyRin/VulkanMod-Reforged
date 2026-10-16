package net.vulkanmod.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.LevelRenderer;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.optimization.AdaptiveChunkUploadBudget;
import net.vulkanmod.vulkan.device.DeviceManager;

public final class PerformancePresetApplier {
    private static final ThreadLocal<Transaction> ACTIVE_TRANSACTION = new ThreadLocal<>();

    private PerformancePresetApplier() {
    }

    public static void apply(PerformancePreset preset, Config config, Minecraft minecraft) {
        config.performancePreset = preset.id;
        if (preset == PerformancePreset.CUSTOM) return;

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

        if (minecraft == null || minecraft.options == null) {
            AdaptiveChunkUploadBudget.reset();
            return;
        }

        Transaction previous = ACTIVE_TRANSACTION.get();
        Transaction transaction = findTransaction(minecraft, previous);
        boolean outermost = transaction == null;
        if (outermost) {
            transaction = new Transaction(minecraft, previous);
            ACTIVE_TRANSACTION.set(transaction);
        }

        boolean applied = false;
        try {
            applyNativeOptions(preset, minecraft.options);
            config.performancePreset = preset.id;
            applied = true;
        } finally {
            if (outermost) {
                                                                                       
                if (previous == null) ACTIVE_TRANSACTION.remove();
                else ACTIVE_TRANSACTION.set(previous);
                try {
                    transaction.finish(applied);
                } finally {
                    AdaptiveChunkUploadBudget.reset();
                }
            }
        }
    }

    private static void applyNativeOptions(PerformancePreset preset, Options options) {
        int maximumAnisotropy = Math.max(1, DeviceManager.getMaxSupportedAnisotropy());
        int maximumBit = 31 - Integer.numberOfLeadingZeros(maximumAnisotropy);
        int anisotropyBit = Math.max(1, Math.min(preset.maxAnisotropyBit, maximumBit));
        TextureFilteringMethod filtering = preset.textureFiltering == TextureFilteringMethod.ANISOTROPIC && maximumBit == 0
                ? TextureFilteringMethod.NONE : preset.textureFiltering;

                                                                                      
                                                                                      
        options.renderDistance().set(preset.renderDistance);
        options.simulationDistance().set(preset.simulationDistance);
        options.prioritizeChunkUpdates().set(preset.prioritizeChunkUpdates);
        options.particles().set(preset.particleStatus);
        options.cloudStatus().set(preset.cloudStatus);
        options.entityShadows().set(preset.entityShadows);
        options.entityDistanceScaling().set(preset.entityDistancePercent * 0.01);
        options.biomeBlendRadius().set(preset.biomeBlendRadius);
        options.ambientOcclusion().set(preset.ambientOcclusion > LightMode.FLAT);
        options.mipmapLevels().set(preset.mipmapLevels);
        options.textureFiltering().set(filtering);
        options.maxAnisotropyBit().set(anisotropyBit);
        options.cloudRange().set(preset.cloudRange);
        options.weatherRadius().set(preset.weatherRadius);
        options.cutoutLeaves().set(preset.cutoutLeaves);
        options.menuBackgroundBlurriness().set(preset.menuBackgroundBlurriness);
        options.vignette().set(preset.vignette);
                                                                     
        options.chunkSectionFadeInTime().set(0.0);
        options.improvedTransparency().set(false);
    }

                                                                                           
    public static boolean deferTerrainRebuild(LevelRenderer renderer) {
        if (renderer == null) return false;
        for (Transaction transaction = ACTIVE_TRANSACTION.get(); transaction != null; transaction = transaction.previous) {
            if (transaction.renderer == renderer) return true;
        }
        return false;
    }

    private static Transaction findTransaction(Minecraft minecraft, Transaction transaction) {
        for (; transaction != null; transaction = transaction.previous) {
            if (transaction.minecraft == minecraft) return transaction;
        }
        return null;
    }

    private static final class Transaction {
        final Minecraft minecraft;
        final LevelRenderer renderer;
        final Transaction previous;
        final TextureSettings previousTextures;

        Transaction(Minecraft minecraft, Transaction previous) {
            this.minecraft = minecraft;
            this.renderer = minecraft.levelRenderer;
            this.previous = previous;
            this.previousTextures = TextureSettings.capture(minecraft.options);
        }

        void finish(boolean applied) {
            try {
                TextureSettings currentTextures = TextureSettings.capture(minecraft.options);
                if (!previousTextures.equals(currentTextures)) {
                    minecraft.updateMaxMipLevel(currentTextures.mipLevels);
                    minecraft.delayTextureReload();
                }
                if (applied) minecraft.options.save();
            } finally {
                                                                                
                if (renderer != null) renderer.allChanged();
            }
        }
    }

    private record TextureSettings(int mipLevels, TextureFilteringMethod filtering, Integer anisotropyBit) {
        static TextureSettings capture(Options options) {
            return new TextureSettings(options.mipmapLevels().get(), options.textureFiltering().get(), options.maxAnisotropyBit().get());
        }
    }
}
