package net.vulkanmod.config.option;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.GraphicsModeCompatibility;
import net.vulkanmod.config.PerformancePreset;
import net.vulkanmod.config.gui.OptionBlock;
import net.vulkanmod.render.chunk.build.light.LightMode;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

                                                                                   
public final class ModernGraphicsOptions {
    private ModernGraphicsOptions() {
    }

    public static OptionBlock[] create(Minecraft minecraft, Config config) {
        var options = minecraft.options;
        CyclingOption<TextureFilteringMethod> filtering = new CyclingOption<>(options.textureFiltering().caption,
                TextureFilteringMethod.values(),
                value -> applyNative(minecraft, config, options.textureFiltering(), value, true, true),
                () -> options.textureFiltering().get());
        filtering.setTranslator(TextureFilteringMethod::caption);
        filtering.setTooltip(value -> Component.translatable(switch (value) {
            case NONE -> "options.textureFiltering.none.tooltip";
            case RGSS -> "options.textureFiltering.rgss.tooltip";
            case ANISOTROPIC -> "options.textureFiltering.anisotropic.tooltip";
        }));

        int maximum = Math.max(1, RenderSystem.getDevice().getMaxSupportedAnisotropy());
        int maximumBit = Math.min(3, 31 - Integer.numberOfLeadingZeros(maximum));
        Consumer<Integer> applyAnisotropy = value -> {
            if (maximumBit > 0) {
                applyNative(minecraft, config, options.maxAnisotropyBit(),
                        Math.max(1, Math.min(value, maximumBit)), true, true);
            }
        };
        Supplier<Integer> anisotropyValue = () -> maximumBit == 0 ? 0
                : Math.max(1, Math.min(options.maxAnisotropyBit().get(), maximumBit));
        Option<Integer> anisotropy = maximumBit > 1
                ? new RangeOption(options.maxAnisotropyBit().caption, 1, maximumBit, 1,
                        value -> Component.translatable("options.multiplier", Integer.toString(1 << value)),
                        applyAnisotropy, anisotropyValue)
                : new CyclingOption<>(options.maxAnisotropyBit().caption, new Integer[]{maximumBit},
                        applyAnisotropy, anisotropyValue)
                        .setTranslator(value -> Component.translatable("options.multiplier", Integer.toString(1 << value)));
        anisotropy.setTooltip(Component.translatable("options.maxAnisotropy.tooltip"));
        anisotropy.setActivationFn(() -> maximumBit > 0 && filtering.getNewValue() == TextureFilteringMethod.ANISOTROPIC);
        filtering.setOnChange(() -> {
                                                                                     
            if (anisotropy.widget != null) anisotropy.updateActiveState();
        });

        return new OptionBlock[]{
                new OptionBlock("", new Option<?>[]{filtering, anisotropy}),
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(options.cloudRange().caption, 2, 128, 1,
                                value -> Component.translatable("options.chunks", value),
                                value -> applyNative(minecraft, config, options.cloudRange(), value, false, true),
                                () -> options.cloudRange().get()),
                        new RangeOption(options.weatherRadius().caption, 3, 10, 1,
                                value -> Component.translatable("options.blocks", value),
                                value -> applyNative(minecraft, config, options.weatherRadius(), value, false, true),
                                () -> options.weatherRadius().get())
                                .setTooltip(Component.translatable("options.weatherRadius.tooltip")),
                        new SwitchOption(options.cutoutLeaves().caption,
                                value -> applyNative(minecraft, config, options.cutoutLeaves(), value, false, true),
                                () -> options.cutoutLeaves().get())
                                .setTooltip(Component.translatable("options.cutoutLeaves.tooltip"))
                }),
                new OptionBlock("", new Option<?>[]{
                        new RangeOption(options.menuBackgroundBlurriness().caption, 0, 10, 1,
                                value -> value == 0 ? Component.translatable("options.off") : Component.literal(value.toString()),
                                value -> applyNative(minecraft, config, options.menuBackgroundBlurriness(), value, false, true),
                                () -> options.menuBackgroundBlurriness().get())
                                .setTooltip(Component.translatable("options.accessibility.menu_background_blurriness.tooltip")),
                        new SwitchOption(options.vignette().caption,
                                value -> applyNative(minecraft, config, options.vignette(), value, false, true),
                                () -> options.vignette().get())
                                .setTooltip(Component.translatable("options.vignette.tooltip")),
                        new CyclingOption<>(options.inactivityFpsLimit().caption, InactivityFpsLimit.values(),
                                value -> applyNative(minecraft, config, options.inactivityFpsLimit(), value, false, false),
                                () -> options.inactivityFpsLimit().get())
                                .setTranslator(InactivityFpsLimit::caption)
                                .setTooltip(value -> Component.translatable(value == InactivityFpsLimit.AFK
                                        ? "options.inactivityFpsLimit.afk.tooltip" : "options.inactivityFpsLimit.minimized.tooltip"))
                }),
                new OptionBlock("", new Option<?>[]{
                                                                                         
                                                                                    
                        new RangeOption(options.chunkSectionFadeInTime().caption, 0, 40, 1,
                                value -> value == 0 ? Component.translatable("options.chunkFade.none")
                                        : Component.translatable("options.chunkFade.seconds", String.format(Locale.ROOT, "%.2f", value / 20.0)),
                                value -> {}, () -> (int) Math.round(options.chunkSectionFadeInTime().get() * 20.0))
                                .setActivationFn(() -> false)
                                .setTooltip(unavailableTooltip("options.chunkFade.tooltip")),
                        new SwitchOption(options.improvedTransparency().caption,
                                value -> {}, () -> options.improvedTransparency().get())
                                .setActivationFn(() -> false)
                                .setTooltip(unavailableTooltip("options.improvedTransparency.tooltip"))
                })
        };
    }

    public static Option<GraphicsPreset> graphicsPreset(Minecraft minecraft, Config config) {
        var options = minecraft.options;
        return new CyclingOption<>(Component.translatable("options.graphics.preset"),
                GraphicsModeCompatibility.supportedModes(), value -> {
            GraphicsPreset requested = GraphicsModeCompatibility.coerce(value);
            if (options.graphicsPreset().get() == requested) return;
            TextureSettings previousTextures = TextureSettings.capture(options);
            int previousAo = config.ambientOcclusion;
            options.graphicsPreset().set(requested);
            config.performancePreset = PerformancePreset.CUSTOM.id;
            if (requested != GraphicsPreset.CUSTOM) {
                config.ambientOcclusion = options.ambientOcclusion().get() ? LightMode.SMOOTH : LightMode.FLAT;
                if (previousAo != config.ambientOcclusion && minecraft.levelRenderer != null) {
                    minecraft.levelRenderer.allChanged();
                }
            }
            if (!previousTextures.equals(TextureSettings.capture(options))) reloadTextures(minecraft);
            options.save();
        }, () -> GraphicsModeCompatibility.coerce(options.graphicsPreset().get()))
                .setTranslator(value -> Component.translatable(value.getKey()))
                .setTooltip(Component.translatable("options.graphics.preset.tooltip"));
    }

    private static <T> void applyNative(Minecraft minecraft, Config config, OptionInstance<T> option,
                                       T value, boolean textureSetting, boolean performanceSetting) {
        T previous = option.get();
        if (Objects.equals(previous, value)) return;
        option.set(value);                                                                  
        if (Objects.equals(previous, option.get())) return;
        if (performanceSetting) config.performancePreset = PerformancePreset.CUSTOM.id;
        if (textureSetting) reloadTextures(minecraft);
        minecraft.options.save();
    }

    private static void reloadTextures(Minecraft minecraft) {
        minecraft.updateMaxMipLevel(minecraft.options.mipmapLevels().get());
        minecraft.delayTextureReload();
    }

    private static Component unavailableTooltip(String nativeTooltip) {
        return Component.translatable(nativeTooltip).append("\n")
                .append(Component.translatable("vulkanmod.diagnostics.flag.unavailable"));
    }

    private record TextureSettings(int mipLevels, TextureFilteringMethod filtering, int anisotropyBit) {
        static TextureSettings capture(net.minecraft.client.Options options) {
            return new TextureSettings(options.mipmapLevels().get(), options.textureFiltering().get(), options.maxAnisotropyBit().get());
        }
    }
}
