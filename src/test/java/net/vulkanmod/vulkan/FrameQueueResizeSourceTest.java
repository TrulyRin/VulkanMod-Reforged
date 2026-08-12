package net.vulkanmod.vulkan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameQueueResizeSourceTest {
    @Test
    void liveFrameQueueChangePreservesGlobalGpuResources() throws IOException {
        String renderer = source("src/main/java/net/vulkanmod/vulkan/Renderer.java");
        String memoryManager = source("src/main/java/net/vulkanmod/vulkan/memory/MemoryManager.java");

        assertTrue(renderer.contains("MemoryManager.resizeFrames(newFramesNum);"));
        assertFalse(renderer.contains("MemoryManager.createInstance(newFramesNum);"));
        assertTrue(memoryManager.contains("public static synchronized void resizeFrames(int frames)"));

        String resizeBody = memoryManager.substring(
                memoryManager.indexOf("public static synchronized void resizeFrames(int frames)"),
                memoryManager.indexOf("MemoryManager() {"));
        assertFalse(resizeBody.contains("cleanUp();"));
        assertFalse(resizeBody.contains("buffers.clear();"));
        assertFalse(resizeBody.contains("images.clear();"));
        assertTrue(resizeBody.contains("INSTANCE.freeBuffers(frame);"));
        assertTrue(resizeBody.contains("INSTANCE.freeSegments(frame);"));
    }

    private static String source(String relativePath) throws IOException {
        Path cwd = Path.of("").toAbsolutePath();
        Path candidate = cwd.resolve(relativePath);
        if (!Files.exists(candidate) && cwd.getParent() != null) {
            candidate = cwd.getParent().resolve(relativePath);
        }
        return Files.readString(candidate);
    }
}
