package net.vulkanmod.render.engine;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.vulkan.VK10;

public class VkGpuTexture extends GlTexture {
    private static final Reference2ReferenceOpenHashMap<GlTexture, VkGpuTexture> glToVkMap = new Reference2ReferenceOpenHashMap<>();

    protected VkGlTexture glTexture;
    protected final int id;
    private final Int2ReferenceMap<VkFbo> fboCache = new Int2ReferenceOpenHashMap<>();
    private final DeferredTextureRelease lifetime = new DeferredTextureRelease();
    protected boolean closed;

    VkTextureView fboView;
    boolean needsClear = false;
    int clearColor = 0;
    float depthClearValue = 1.0f;

    protected VkGpuTexture(int usage, String string, TextureFormat textureFormat, int width, int height, int layers, int mipLevel, int id, VkGlTexture glTexture) {
        super(usage, string, textureFormat, width, height, layers, mipLevel, id);
        this.id = id;
        this.glTexture = glTexture;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;

            for (VkFbo fbo : this.fboCache.values()) {
                fbo.close();
            }
            this.fboCache.clear();

            if (this.fboView != null) {
                this.fboView.close();
                this.fboView = null;
            }

            if (this.lifetime.requestClose()) {
                this.releaseVulkanImage();
            }
        }
    }

    @Override
    public void addViews() {
        this.lifetime.addView();
    }

    @Override
    public void removeViews() {
        if (this.lifetime.removeView()) {
            this.releaseVulkanImage();
        }
    }

    private void releaseVulkanImage() {
                                                                            
                                                                      
        VkGlTexture.glDeleteTextures(this.id);
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    public int glId() {
        return this.id;
    }

    public void setClearColor(int clearColor) {
        this.needsClear = true;
        this.clearColor = clearColor;
    }

    public void setDepthClearValue(float depthClearValue) {
        this.needsClear = true;
        this.depthClearValue = depthClearValue;
    }

    public boolean needsClear() {
        return needsClear;
    }

                                                                                          
    public void completeColorOverwrite() {
        this.needsClear = false;
    }

    public VkFbo getFbo(@Nullable GpuTexture depthAttachment) {
        int depthAttachmentId = depthAttachment == null ? 0 : ((VkGpuTexture)depthAttachment).id;

        if (this.fboView == null) {
            VkGpuDevice gpuDevice = (VkGpuDevice) RenderSystem.getDevice();
            this.fboView = (VkTextureView) gpuDevice.createTextureView(this, 0, this.getMipLevels());
        }

        return this.fboCache.computeIfAbsent(depthAttachmentId, j -> new VkFbo(this.fboView, (VkGpuTexture) depthAttachment));
    }

    public VulkanImage getVulkanImage() {
        return glTexture.getVulkanImage();
    }

    public static VkGpuTexture fromGlTexture(GlTexture glTexture) {
        return glToVkMap.computeIfAbsent(glTexture, glTexture1 -> {
            var name = glTexture.getLabel();
            int id = glTexture.glId();
            VkGlTexture vglTexture = VkGlTexture.getTexture(id);
            VkGpuTexture gpuTexture = new VkGpuTexture(0, name, glTexture.getFormat(),
                                                       glTexture.getWidth(0), glTexture.getHeight(0),
                                                       1, glTexture.getMipLevels(),
                                                       glTexture.glId(), vglTexture);

            return gpuTexture;
        });
    }

    public static TextureFormat textureFormat(int format) {
        return switch (format) {
            case VK10.VK_FORMAT_R8G8B8A8_UNORM, VK10.VK_FORMAT_B8G8R8A8_UNORM, VK10.VK_FORMAT_R8G8B8A8_SRGB -> TextureFormat.RGBA8;
            case VK10.VK_FORMAT_R8_UNORM -> TextureFormat.RED8;
            case VK10.VK_FORMAT_D32_SFLOAT -> TextureFormat.DEPTH32;
            default -> null;
        };
    }

    public static int vkFormat(TextureFormat textureFormat) {
        return switch (textureFormat) {
            case RGBA8 -> VK10.VK_FORMAT_R8G8B8A8_UNORM;
            case RED8 -> VK10.VK_FORMAT_R8_UNORM;
            case RED8I -> VK10.VK_FORMAT_R8_SINT;
            case DEPTH32 -> VK10.VK_FORMAT_D32_SFLOAT;
                                                                             
            default -> throw new IllegalArgumentException("Unsupported texture format: " + textureFormat);
        };
    }

    public static int vkImageViewType(int usage) {
        int viewType;
        if ((usage & GpuTexture.USAGE_CUBEMAP_COMPATIBLE) != 0) {
            viewType = VK10.VK_IMAGE_VIEW_TYPE_CUBE;
        }
        else {
            viewType = VK10.VK_IMAGE_VIEW_TYPE_2D;
        }

        return viewType;
    }
}
