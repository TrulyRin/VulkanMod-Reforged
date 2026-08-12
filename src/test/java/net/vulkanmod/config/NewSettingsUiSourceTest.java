package net.vulkanmod.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewSettingsUiSourceTest {

    @Test
    void optionScreenHasSearchAndUndoWiring() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/gui/VOptionScreen.java"));

        assertTrue(source.contains("performSearch(widget.getInput())"));
        assertTrue(source.contains("vulkanmod.options.searchFieldPlaceholder"));
        assertTrue(source.contains("vulkanmod.options.buttons.undo"));
        assertTrue(source.contains("page.resetToOriginalState();"));
        // Fork behavior: presets rewrite many settings at once, so apply must rebuild pages.
        assertTrue(source.contains("this.rebuildPagesFromCurrentOptions();"));
        // Fork identity: support link stays on the fork's Ko-fi, not upstream's.
        assertTrue(source.contains("https://ko-fi.com/rindw"));
        assertFalse(source.contains("ko-fi.com/xcollateral"));
    }

    @Test
    void modSettingsRegistryRegistersReforgedEntry() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/gui/ModSettingsRegistry.java"));

        assertTrue(source.contains("Component.literal(\"VulkanMod Reforged\")"));
        assertTrue(source.contains("vlogo_transparent.png"));
        assertTrue(source.contains("Options::getOptionPages"));
        assertTrue(source.contains("Initializer.CONFIG.write()"));
    }

    @Test
    void windowModeOptionMapsOntoForkFullscreenSemantics() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/option/Options.java"));

        // WindowMode must translate to the fork's config fields consumed by WindowMixin.setMode:
        // fullscreen=true -> exclusive; windowedFullscreen flag -> borderless.
        assertTrue(source.contains("minecraftOptions.fullscreen().set(value == WindowMode.EXCLUSIVE_FULLSCREEN);"));
        assertTrue(source.contains("config.windowedFullscreen = (value == WindowMode.WINDOWED_FULLSCREEN);"));
        assertTrue(source.contains("windowModeOption.getNewValue() == WindowMode.EXCLUSIVE_FULLSCREEN"));
        // The old separate switches must be gone.
        assertFalse(source.contains("vulkanmod.options.windowedFullscreen"));
    }

    @Test
    void chunkUploadOptionsExposeTheCeilingAndResetAdaptiveState() throws IOException {
        String options = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/option/Options.java"));
        String presets = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/PerformancePresetApplier.java"));

        assertTrue(options.contains("Component.translatable(\"vulkanmod.options.chunkUploadsPerFrame\")"));
        assertTrue(options.contains("1, 16, 1,"));
        assertTrue(options.contains("config.chunkUploadsPerFrame = value;"));
        assertTrue(options.contains("Config.clampChunkUploadsPerFrame(config.chunkUploadsPerFrame)"));
        assertTrue(options.lines()
                .filter(line -> line.contains("AdaptiveChunkUploadBudget.reset();"))
                .count() >= 2);
        assertTrue(presets.contains("AdaptiveChunkUploadBudget.reset();"));
    }

    @Test
    void benchmarkUsesTheEffectiveClampedChunkUploadCeiling() throws IOException {
        String config = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/Config.java"));
        String controller = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/diagnostics/benchmark/MinecraftBenchmarkController.java"));

        assertTrue(config.contains("clampChunkUploadsPerFrame"));
        assertTrue(config.contains("config.chunkUploadsPerFrame = clampChunkUploadsPerFrame"));
        assertTrue(controller.contains("Config.clampChunkUploadsPerFrame"));
    }

    @Test
    void updateCheckerTargetsForkReleasesNotUpstream() throws IOException {
        String checker = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/UpdateChecker.java"));
        String screen = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/gui/VOptionScreen.java"));

        assertTrue(checker.contains("https://github.com/TrulyRin/VulkanMod-Reforged/releases"));
        assertFalse(checker.contains("modrinth.com/mod/vulkanmod"));
        assertTrue(screen.contains("UpdateChecker.RELEASES_URL"));
    }

    @Test
    void langFileHasNewUiKeys() throws IOException {
        String lang = Files.readString(Path.of(
                "src/main/resources/assets/vulkanmod/lang/en_us.json"));

        assertTrue(lang.contains("\"vulkanmod.options.searchFieldPlaceholder\""));
        assertTrue(lang.contains("\"vulkanmod.options.buttons.undo\""));
        assertTrue(lang.contains("\"vulkanmod.options.buttons.update_available\""));
        assertTrue(lang.contains("\"vulkanmod.options.performanceImpact\""));
        assertTrue(lang.contains("\"vulkanmod.options.windowMode\""));
        assertTrue(lang.contains("\"vulkanmod.options.chunkUploadsPerFrame\""));
        assertTrue(lang.contains("\"vulkanmod.options.chunkUploadsPerFrame.tooltip\""));
    }

    @Test
    void everyConfigurableFeatureHasADescription() throws IOException {
        String options = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/config/option/Options.java"));
        String lang = Files.readString(Path.of(
                "src/main/resources/assets/vulkanmod/lang/en_us.json"));

        long optionCount = Pattern.compile(
                "new (?:RangeOption|SwitchOption|CyclingOption<>)\\(")
                .matcher(options).results().count();
        long tooltipCount = Pattern.compile("\\.setTooltip\\(")
                .matcher(options).results().count();

        assertEquals(38, optionCount, "update the description inventory when a feature is added");
        assertEquals(optionCount, tooltipCount,
                "every configurable feature must provide a useful description");

        var tooltipKeys = Pattern.compile(
                "Component\\.translatable\\(\\s*\"(vulkanmod\\.options\\.[^\"]+\\.tooltip)\"")
                .matcher(options);
        while (tooltipKeys.find()) {
            assertTrue(lang.contains("\"" + tooltipKeys.group(1) + "\""),
                    "missing English description for " + tooltipKeys.group(1));
        }
    }
}
