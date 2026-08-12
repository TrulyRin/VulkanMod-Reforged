package net.vulkanmod.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigSafetyTest {
    @TempDir
    Path tempDir;

    @Test
    void frameQueueClampRejectsMalformedValues() {
        assertEquals(2, Config.clampFrameQueueSize(Integer.MIN_VALUE));
        assertEquals(2, Config.clampFrameQueueSize(1));
        assertEquals(3, Config.clampFrameQueueSize(3));
        assertEquals(5, Config.clampFrameQueueSize(6));
        assertEquals(5, Config.clampFrameQueueSize(Integer.MAX_VALUE));
    }

    @Test
    void builderThreadsKeepZeroAsAutoAndClampManualValues() {
        assertEquals(0, Config.clampBuilderThreads(-10));
        assertEquals(0, Config.clampBuilderThreads(0));
        assertTrue(Config.clampBuilderThreads(Integer.MAX_VALUE) >= 1);
        assertTrue(Config.clampBuilderThreads(Integer.MAX_VALUE) <= 8);
    }

    @Test
    void waylandPreferenceCanBeReadBeforeGlfwConfiguration() throws IOException {
        Path config = tempDir.resolve("vulkanmod_settings.json");
        assertFalse(Config.readWaylandPreference(config));

        Files.writeString(config, "{\"useWayland\":true}");
        assertTrue(Config.readWaylandPreference(config));

        Files.writeString(config, "{\"useWayland\":false}");
        assertFalse(Config.readWaylandPreference(config));

        Files.writeString(config, "not-json");
        assertFalse(Config.readWaylandPreference(config));
    }
}
