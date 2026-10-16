package net.vulkanmod.render.texture;

import net.vulkanmod.render.texture.SpriteUtil;

import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.Queue;

public class ImageUploadHelper {

    public static final ImageUploadHelper INSTANCE = new ImageUploadHelper();

    final Queue queue;
    private CommandPool.CommandBuffer currentCmdBuffer;

    public ImageUploadHelper() {
        queue = DeviceManager.getGraphicsQueue();
    }

    public void submitCommands() {
        this.submitCommands(true);
    }

    public void submitCommands(boolean useSemaphore) {
        if (this.currentCmdBuffer == null) {
            return;
        }

        SpriteUtil.transitionLayouts(this.currentCmdBuffer.getHandle());

        queue.submitCommands(this.currentCmdBuffer, useSemaphore);
        Synchronization.INSTANCE.addCommandBuffer(this.currentCmdBuffer, useSemaphore);

        this.currentCmdBuffer = null;
    }

    public CommandPool.CommandBuffer getOrStartCommandBuffer() {
        if (this.currentCmdBuffer == null) {
            this.currentCmdBuffer = this.queue.beginCommands();
        }

        return this.currentCmdBuffer;
    }

    public CommandPool.CommandBuffer getCommandBuffer() {
        return this.currentCmdBuffer;
    }
}
