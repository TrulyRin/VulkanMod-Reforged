package net.vulkanmod.vulkan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkUploadSynchronizationSourceTest {
    @Test
    void perFrameUploadsUseGraphicsQueueOrderingInsteadOfCpuFenceWaits() throws IOException {
        String uploads = source("src/main/java/net/vulkanmod/render/chunk/buffer/UploadManager.java");
        String graphicsQueue = source("src/main/java/net/vulkanmod/vulkan/queue/GraphicsQueue.java");
        String renderer = source("src/main/java/net/vulkanmod/vulkan/Renderer.java");

        assertTrue(uploads.contains("Queue queue = DeviceManager.getGraphicsQueue();"));
        assertTrue(uploads.contains("VK_ACCESS_TRANSFER_WRITE_BIT"));
        assertTrue(uploads.contains("MemoryManager.getInstance().addFrameOp(this.commandBuffer::reset);"));
        assertFalse(uploads.contains("Synchronization.INSTANCE.addCommandBuffer(this.commandBuffer);"));

        assertTrue(graphicsQueue.contains("MemoryManager.getInstance().addFrameOp(currentCmdBuffer::reset);"));
        assertFalse(graphicsQueue.contains("Synchronization.INSTANCE.addCommandBuffer(currentCmdBuffer);"));
        assertTrue(renderer.indexOf("MemoryManager.getInstance().initFrame(currentFrame);")
                < renderer.indexOf("UploadManager.INSTANCE.submitUploads();"));
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
