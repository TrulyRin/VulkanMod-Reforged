package net.vulkanmod.render.engine;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

public class VkTextureView extends GpuTextureView {
    private boolean closed;

    private final Int2ReferenceMap<VkFbo> fboCache = new Int2ReferenceOpenHashMap<>();

    protected VkTextureView(VkGpuTexture gpuTexture, int baseMipLevel, int mipLevels) {
        super(gpuTexture, baseMipLevel, mipLevels);
        gpuTexture.addViews();
    }

    public VkFbo getFbo(@Nullable GpuTexture depthAttachment) {
        int depthAttachmentId = depthAttachment == null ? 0 : ((VkGpuTexture)depthAttachment).id;
        return this.fboCache.computeIfAbsent(depthAttachmentId, j -> new VkFbo(this, (VkGpuTexture) depthAttachment));
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.texture().removeViews();
        }

        for (VkFbo fbo : this.fboCache.values()) {
            fbo.close();
        }
    }

    public VkGpuTexture texture() {
        return (VkGpuTexture) super.texture();
    }
}
