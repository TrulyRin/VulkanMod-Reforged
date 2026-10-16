package net.vulkanmod.config.option;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.network.chat.Component;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.PerformancePreset;
import net.vulkanmod.config.PerformancePresetApplier;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.config.gui.OptionBlock;

import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.optimization.AdaptiveChunkUploadBudget;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public abstract class Options {
    public static boolean fullscreenDirty = false;
    static Config config = Initializer.CONFIG;
    static Minecraft minecraft = Minecraft.getInstance();
    static Window window = minecraft.getWindow();
    static net.minecraft.client.Options minecraftOptions = minecraft.options;

    private static void markPerformancePresetCustom() {
        config.performancePreset = PerformancePreset.CUSTOM.id;
    }

    public static List<OptionPage> getOptionPages() {
        List<OptionPage> optionPages = new ArrayList<>();

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.video").getString(),
                Options.getVideoOpts()));

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.graphics").getString(),
                Options.getGraphicsOpts()));

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.optimizations").getString(),
                Options.getOptimizationOpts()));

        optionPages.add(new OptionPage(
                Component.translatable("vulkanmod.options.pages.other").getString(),
                Options.getOtherOpts()));

        return optionPages;
    }

    public static OptionBlock[] getVideoOpts() {
        var videoMode = config.videoMode;
        var videoModeSet = VideoModeManager.getFromVideoMode(videoMode);

        if (videoModeSet == null) {
            videoModeSet = VideoModeSet.getDummy();
            videoMode = videoModeSet.getVideoMode(-1);
        }

        VideoModeManager.selectedVideoMode = videoMode;
        var refreshRates = videoModeSet.getRefreshRates();

        CyclingOption<Integer> RefreshRate = (CyclingOption<Integer>) new CyclingOption<>(
                Component.translatable("vulkanmod.options.refreshRate"),
                refreshRates.toArray(new Integer[0]),
                (value) -> {
                    VideoModeManager.selectedVideoMode.refreshRate = value;
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> VideoModeManager.selectedVideoMode.refreshRate)
                .setTranslator(refreshRate -> Component.nullToEmpty(refreshRate.toString()))
                .setTooltip(Component.translatable("vulkanmod.options.refreshRate.tooltip"));

        Option<VideoModeSet> resolutionOption = new CyclingOption<>(
                Component.translatable("options.fullscreen.resolution"),
                VideoModeManager.getVideoResolutions(),
                (value) -> {
                    VideoModeManager.selectedVideoMode = value.getVideoMode(RefreshRate.getNewValue());
                    VideoModeManager.applySelectedVideoMode();

                    if (minecraftOptions.fullscreen().get())
                        fullscreenDirty = true;
                },
                () -> {
                    var selectedVideoMode = VideoModeManager.selectedVideoMode;
                    var selectedVideoModeSet = VideoModeManager.getFromVideoMode(selectedVideoMode);

                    return selectedVideoModeSet != null ? selectedVideoModeSet : VideoModeSet.getDummy();
                })
                .setTranslator(resolution -> Component.nullToEmpty(resolution.toString()))
                .setTooltip(Component.translatable("vulkanmod.options.resolution.tooltip"));

        resolutionOption.setOnChange(() -> {
            var newVideoMode = resolutionOption.getNewValue();
            var newRefreshRates = newVideoMode.getRefreshRates().toArray(new Integer[0]);

            RefreshRate.setValues(newRefreshRates);
            RefreshRate.setNewValue(newRefreshRates[newRefreshRates.length - 1]);
        });

                                                                                                 
        var windowModeOption = new CyclingOption<>(Component.translatable("vulkanmod.options.windowMode"),
                WindowMode.values(),
                value -> {
                    minecraftOptions.fullscreen().set(value == WindowMode.EXCLUSIVE_FULLSCREEN);
                    config.windowedFullscreen = (value == WindowMode.WINDOWED_FULLSCREEN);
                    fullscreenDirty = true;
                },
                () -> minecraftOptions.fullscreen().get() ? WindowMode.EXCLUSIVE_FULLSCREEN
                        : config.windowedFullscreen ? WindowMode.WINDOWED_FULLSCREEN
                        : WindowMode.WINDOWED)
                .setTranslator(value -> Component.translatable(WindowMode.getComponentName(value)))
                .setTooltip(Component.translatable("vulkanmod.options.windowMode.tooltip"));

        resolutionOption.setActivationFn(() -> windowModeOption.getNewValue() == WindowMode.EXCLUSIVE_FULLSCREEN);
        RefreshRate.setActivationFn(() -> windowModeOption.getNewValue() == WindowMode.EXCLUSIVE_FULLSCREEN);

        windowModeOption.setOnChange(() -> {
            resolutionOption.updateActiveState();
            RefreshRate.updateActiveState();
        });

        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{
                        windowModeOption,
                        resolutionOption,
                        RefreshRate,
                        new RangeOption(Component.translatable("options.framerateLimit"),
                                10, 260, 10,
                                value -> Component.nullToEmpty(value == 260 ?
                                        Component.translatable("options.framerateLimit.max").getString() :
                                        String.valueOf(value)),
                                 value -> {
                                     minecraftOptions.framerateLimit().set(value);
                                 },
                                 () -> minecraftOptions.framerateLimit().get())
                                .setTooltip(Component.translatable("vulkanmod.options.framerateLimit.tooltip")),
                         new SwitchOption(Component.translatable("options.vsync"),
                                 value -> {
                                     minecraftOptions.enableVsync().set(value);
                                     window.updateVsync(value);
                                 },
                                 () -> minecraftOptions.enableVsync().get())
                                .setTooltip(Component.translatable("vulkanmod.options.vsync.tooltip")),
                }),
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(Component.translatable("options.guiScale"),
                                0, window.calculateScale(0, minecraft.isEnforceUnicode()), 1,
                                value -> Component.translatable((value == 0)
                                        ? "options.guiScale.auto"
                                        : String.valueOf(value)),
                                 value -> {
                                     minecraftOptions.guiScale().set(value);
                                     minecraft.resizeDisplay();
                                 },
                                 () -> (minecraftOptions.guiScale().get()))
                                .setTooltip(Component.translatable("vulkanmod.options.guiScale.tooltip")),
                        new RangeOption(Component.translatable("options.gamma"),
                                0, 100, 1,
                                value -> Component.translatable(switch (value) {
                                    case 0 -> "options.gamma.min";
                                    case 50 -> "options.gamma.default";
                                    case 100 -> "options.gamma.max";
                                    default -> String.valueOf(value);
                                 }),
                                 value -> minecraftOptions.gamma().set(value * 0.01),
                                 () -> (int) (minecraftOptions.gamma().get() * 100.0))
                                .setTooltip(Component.translatable("vulkanmod.options.gamma.tooltip")),
                }),
                new OptionBlock("", new Option<?>[]{
                         new SwitchOption(Component.translatable("options.viewBobbing"),
                                 (value) -> minecraftOptions.bobView().set(value),
                                 () -> minecraftOptions.bobView().get())
                                .setTooltip(Component.translatable("vulkanmod.options.viewBobbing.tooltip")),
                        new CyclingOption<>(Component.translatable("options.attackIndicator"),
                                AttackIndicatorStatus.values(),
                                value -> minecraftOptions.attackIndicator().set(value),
                                () -> minecraftOptions.attackIndicator().get())
                                .setTranslator(AttackIndicatorStatus::caption)
                                .setTooltip(Component.translatable("vulkanmod.options.attackIndicator.tooltip")),
                         new SwitchOption(Component.translatable("options.autosaveIndicator"),
                                 value -> minecraftOptions.showAutosaveIndicator().set(value),
                                 () -> minecraftOptions.showAutosaveIndicator().get())
                                .setTooltip(Component.translatable("vulkanmod.options.autosaveIndicator.tooltip")),
                })
        };
    }

    public static OptionBlock[] getGraphicsOpts() {
        List<OptionBlock> blocks = new ArrayList<>(List.of(
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(Component.translatable("options.renderDistance"),
                                2, 32, 1,
                                (value) -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.renderDistance().set(value);
                                 },
                                 () -> minecraftOptions.renderDistance().get())
                                .setTooltip(Component.translatable("vulkanmod.options.renderDistance.tooltip"))
                                 .setImpact(PerformanceImpact.HIGH),
                        new RangeOption(Component.translatable("options.simulationDistance"),
                                5, 32, 1,
                                (value) -> {
                                    markPerformancePresetCustom();
                                     minecraftOptions.simulationDistance().set(value);
                                 },
                                 () -> minecraftOptions.simulationDistance().get())
                                .setTooltip(Component.translatable("vulkanmod.options.simulationDistance.tooltip")),
                        new CyclingOption<>(Component.translatable("options.prioritizeChunkUpdates"),
                                PrioritizeChunkUpdates.values(),
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.prioritizeChunkUpdates().set(value);
                                },
                                () -> minecraftOptions.prioritizeChunkUpdates().get())
                                .setTranslator(PrioritizeChunkUpdates::caption)
                                .setTooltip(Component.translatable("vulkanmod.options.prioritizeChunkUpdates.tooltip")),
                }),
                new OptionBlock("", new Option<?>[]{
                        new CyclingOption<>(Component.translatable("options.particles"),
                                new ParticleStatus[]{ParticleStatus.MINIMAL, ParticleStatus.DECREASED, ParticleStatus.ALL},
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.particles().set(value);
                                 },
                                 () -> minecraftOptions.particles().get())
                                 .setTranslator(ParticleStatus::caption)
                                .setTooltip(Component.translatable("vulkanmod.options.particles.tooltip"))
                                 .setImpact(PerformanceImpact.MEDIUM),
                        new CyclingOption<>(Component.translatable("options.renderClouds"),
                                CloudStatus.values(),
                                value -> {
                                    markPerformancePresetCustom();
                                     minecraftOptions.cloudStatus().set(value);
                                 },
                                 () -> minecraftOptions.cloudStatus().get())
                                 .setTranslator(CloudStatus::caption)
                                .setTooltip(Component.translatable("vulkanmod.options.renderClouds.tooltip")),
                        new CyclingOption<>(Component.translatable("options.ao"),
                                new Integer[]{LightMode.FLAT, LightMode.SMOOTH, LightMode.SUB_BLOCK},
                                (value) -> {
                                    markPerformancePresetCustom();

                                    if (value > LightMode.FLAT)
                                        minecraftOptions.ambientOcclusion().set(true);
                                    else
                                        minecraftOptions.ambientOcclusion().set(false);

                                    config.ambientOcclusion = value;

                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.ambientOcclusion)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case LightMode.FLAT -> "options.off";
                                    case LightMode.SMOOTH -> "options.on";
                                    case LightMode.SUB_BLOCK -> "vulkanmod.options.ao.subBlock";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                 .setTooltip(Component.translatable("vulkanmod.options.ao.tooltip"))
                                 .setImpact(PerformanceImpact.LOW),
                        new RangeOption(Component.translatable("options.biomeBlendRadius"),
                                0, 7, 1,
                                value -> {
                                    int v = value * 2 + 1;
                                    return Component.nullToEmpty("%d x %d".formatted(v, v));
                                },
                                (value) -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.biomeBlendRadius().set(value);
                                     minecraft.levelRenderer.allChanged();
                                 },
                                 () -> minecraftOptions.biomeBlendRadius().get())
                                .setTooltip(Component.translatable("vulkanmod.options.biomeBlendRadius.tooltip")),
                }),
                new OptionBlock("", new Option<?>[]{
                        new SwitchOption(Component.translatable("options.entityShadows"),
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.entityShadows().set(value);
                                 },
                                 () -> minecraftOptions.entityShadows().get())
                                .setTooltip(Component.translatable("vulkanmod.options.entityShadows.tooltip"))
                                 .setImpact(PerformanceImpact.LOW),
                        new RangeOption(Component.translatable("options.entityDistanceScaling"),
                                50, 500, 25,
                                value -> {
                                    markPerformancePresetCustom();
                                     minecraftOptions.entityDistanceScaling().set(value * 0.01);
                                 },
                                 () -> (int) Math.round(minecraftOptions.entityDistanceScaling().get() * 100.0))
                                .setTooltip(Component.translatable("vulkanmod.options.entityDistanceScaling.tooltip"))
                                 .setImpact(PerformanceImpact.HIGH),
                        new CyclingOption<>(Component.translatable("options.mipmapLevels"),
                                new Integer[]{0, 1, 2, 3, 4},
                                value -> {
                                    markPerformancePresetCustom();
                                    minecraftOptions.mipmapLevels().set(value);
                                    minecraft.updateMaxMipLevel(value);
                                    minecraft.delayTextureReload();
                                 },
                                 () -> minecraftOptions.mipmapLevels().get())
                                 .setTranslator(value -> Component.nullToEmpty(value.toString()))
                                .setTooltip(Component.translatable("vulkanmod.options.mipmapLevels.tooltip"))
                                 .setImpact(PerformanceImpact.LOW)
                })
        ));
        blocks.addAll(List.of(ModernGraphicsOptions.create(minecraft, config)));
        return blocks.toArray(OptionBlock[]::new);
    }

    public static OptionBlock[] getOptimizationOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{
                        new CyclingOption<>(Component.translatable("vulkanmod.options.performancePreset"),
                                PerformancePreset.values(),
                                value -> PerformancePresetApplier.apply(value, config, minecraft),
                                () -> PerformancePreset.byId(config.performancePreset))
                                .setTranslator(value -> Component.translatable(value.translationKey))
                                .setTooltip(Component.translatable("vulkanmod.options.performancePreset.tooltip"))
                }),
                new OptionBlock("", new Option[]{
                        new CyclingOption<>(Component.translatable("vulkanmod.options.advCulling"),
                                new Integer[]{1, 2, 3, 10},
                                value -> {
                                    markPerformancePresetCustom();
                                    config.advCulling = value;
                                },
                                () -> config.advCulling)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case 1 -> "vulkanmod.options.advCulling.aggressive";
                                    case 2 -> "vulkanmod.options.advCulling.normal";
                                    case 3 -> "vulkanmod.options.advCulling.conservative";
                                    case 10 -> "options.off";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                .setTooltip(Component.translatable("vulkanmod.options.advCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.entityCulling"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.entityCulling = value;
                                },
                                () -> config.entityCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.entityCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.blockEntityCulling"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.blockEntityCulling = value;
                                },
                                () -> config.blockEntityCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.blockEntityCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.leavesCulling"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.leavesCulling = value;
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.leavesCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.leavesCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.backfaceCulling"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.backFaceCulling = value;
                                    if (minecraft.level != null) {
                                        minecraft.levelRenderer.allChanged();
                                    }
                                },
                                () -> config.backFaceCulling)
                                .setTooltip(Component.translatable("vulkanmod.options.backfaceCulling.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.uniqueOpaqueLayer"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.uniqueOpaqueLayer = value;
                                    minecraft.levelRenderer.allChanged();
                                },
                                () -> config.uniqueOpaqueLayer)
                                .setKey(OptionKeys.UNIQUE_OPAQUE_LAYER)
                                .setTooltip(Component.translatable("vulkanmod.options.uniqueOpaqueLayer.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.indirectDraw"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.indirectDraw = value && DeviceManager.supportsFastIndirectDraw();
                                },
                                () -> config.indirectDraw && DeviceManager.supportsFastIndirectDraw())
                                .setKey(OptionKeys.INDIRECT_DRAW)
                                .setActivationFn(DeviceManager::supportsFastIndirectDraw)
                                .setTooltip(Component.translatable("vulkanmod.options.indirectDraw.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                        new SwitchOption(Component.translatable("vulkanmod.options.adaptiveChunkUploads"),
                                value -> {
                                    markPerformancePresetCustom();
                                    config.adaptiveChunkUploads = value;
                                    AdaptiveChunkUploadBudget.reset();
                                },
                                () -> config.adaptiveChunkUploads)
                                .setTooltip(Component.translatable("vulkanmod.options.adaptiveChunkUploads.tooltip")),
                        new RangeOption(Component.translatable("vulkanmod.options.chunkUploadsPerFrame"),
                                1, 16, 1,
                                value -> {
                                    markPerformancePresetCustom();
                                    config.chunkUploadsPerFrame = value;
                                    AdaptiveChunkUploadBudget.reset();
                                },
                                () -> Config.clampChunkUploadsPerFrame(config.chunkUploadsPerFrame))
                                .setTooltip(Component.translatable("vulkanmod.options.chunkUploadsPerFrame.tooltip")),
                        new CyclingOption<>(Component.translatable("vulkanmod.options.particleCulling"),
                                new Integer[]{0, 1, 2, 3},
                                value -> {
                                    markPerformancePresetCustom();
                                    config.particleCulling = value;
                                },
                                () -> config.particleCulling)
                                .setTranslator(value -> Component.translatable(switch (value) {
                                    case 0 -> "options.off";
                                    case 1 -> "vulkanmod.options.particleCulling.quality";
                                    case 2 -> "vulkanmod.options.particleCulling.balanced";
                                    case 3 -> "vulkanmod.options.particleCulling.performance";
                                    default -> "vulkanmod.options.unknown";
                                }))
                                .setTooltip(Component.translatable("vulkanmod.options.particleCulling.tooltip")),
                })
        };

    }

    public static OptionBlock[] getOtherOpts() {
        return new OptionBlock[]{
                new OptionBlock("", new Option[]{
                        new RangeOption(Component.translatable("vulkanmod.options.renderScale"),
                                RenderScale.MIN, RenderScale.MAX, RenderScale.STEP,
                                value -> Component.nullToEmpty(value + "%"),
                                value -> {
                                    config.renderScale = RenderScale.clamp(value);
                                    minecraft.resizeDisplay();
                                },
                                () -> RenderScale.clamp(config.renderScale))
                                .setKey(OptionKeys.RENDER_SCALE)
                                .setTooltip(Component.translatable("vulkanmod.options.renderScale.tooltip")),
                        new RangeOption(Component.translatable("vulkanmod.options.frameQueue"),
                                Config.MIN_FRAME_QUEUE_SIZE, Config.MAX_FRAME_QUEUE_SIZE, 1,
                                value -> {
                                    markPerformancePresetCustom();
                                    config.frameQueueSize = Config.clampFrameQueueSize(value);
                                    Renderer.scheduleSwapChainUpdate();
                                }, () -> Config.clampFrameQueueSize(config.frameQueueSize))
                                .setTooltip(Component.translatable("vulkanmod.options.frameQueue.tooltip")),
                        new RangeOption(Component.translatable("vulkanmod.options.builderThreads"),
                                0, Config.maxBuilderThreadsForSystem(), 1,
                                value -> {
                                    config.builderThreads = Config.clampBuilderThreads(value);
                                    if (minecraft.level != null) {
                                        minecraft.levelRenderer.allChanged();
                                    }
                                },
                                () -> Config.clampBuilderThreads(config.builderThreads))
                                .setTranslator(value -> value == 0
                                        ? Component.translatable("vulkanmod.options.builderThreads.auto")
                                        : Component.literal(String.valueOf(value)))
                                .setTooltip(Component.translatable("vulkanmod.options.builderThreads.tooltip"))
                                .setImpact(PerformanceImpact.HIGH),
                         new SwitchOption(Component.translatable("vulkanmod.options.textureAnimations"),
                                 value -> {
                                     config.textureAnimations = value;
                                 },
                                 () -> config.textureAnimations)
                                .setTooltip(Component.translatable("vulkanmod.options.textureAnimations.tooltip")),
                        new SwitchOption(Component.translatable("vulkanmod.options.wayland"),
                                value -> config.useWayland = value,
                                () -> config.useWayland)
                                .setActivationFn(() -> Platform.isLinux()
                                        && GLFW.glfwPlatformSupported(GLFW.GLFW_PLATFORM_WAYLAND))
                                .setTooltip(Component.translatable("vulkanmod.options.wayland.tooltip")),
                        new CyclingOption<>(Component.translatable("vulkanmod.options.deviceSelector"),
                                IntStream.range(-1, DeviceManager.suitableDevices.size()).boxed().toArray(Integer[]::new),
                                value -> config.device = value,
                                () -> config.device)
                                .setTranslator(value -> Component.translatable((value == -1)
                                        ? "vulkanmod.options.deviceSelector.auto"
                                        : DeviceManager.suitableDevices.get(value).deviceName)
                                )
                                 .setTooltip(value -> Component.translatable(
                                         "vulkanmod.options.deviceSelector.tooltip",
                                         DeviceManager.device.deviceName))
                })
        };

    }
}
