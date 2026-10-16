package net.vulkanmod.render.engine;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.util.ARGB;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL33;

public class VkFbo {
    final int glId;
    final VkTextureView colorAttachmentView;
    final VkGpuTexture depthAttachment;

    protected VkFbo(VkTextureView colorAttachmentView, VkGpuTexture depthAttachment) {
                                                                                              
                                                                                               
        this.glId = VkGlFramebuffer.genFramebufferId();
        this.colorAttachmentView = colorAttachmentView;
        this.depthAttachment = depthAttachment;

                        
        VkGlFramebuffer fbo = VkGlFramebuffer.getFramebuffer(this.glId);

        VkGpuTexture colorAttachmentTexture = this.colorAttachmentView.texture();
        fbo.setAttachmentTexture(GL33.GL_COLOR_ATTACHMENT0, colorAttachmentTexture.id);
        if (depthAttachment != null) {
            fbo.setAttachmentTexture(GL33.GL_DEPTH_ATTACHMENT, depthAttachment.id);
        }

        fbo.setLevel(this.colorAttachmentView.baseMipLevel());
    }

    public void bind() {
        VkGlFramebuffer.bindFramebuffer(GL33.GL_FRAMEBUFFER, this.glId);
        clearAttachments();
    }

    protected void clearAttachments() {
        int clear = 0;
        float clearDepth;
        int clearColor;

        VkGpuTexture colorAttachmentTexture = colorAttachmentView.texture();
        if (colorAttachmentTexture.needsClear()) {
            clear |= 0x4000;
            clearColor = colorAttachmentTexture.clearColor;

            VRenderSystem.setClearColor(ARGB.redFloat(clearColor), ARGB.greenFloat(clearColor), ARGB.blueFloat(clearColor), ARGB.alphaFloat(clearColor));

            colorAttachmentTexture.needsClear = false;
        }

        if (depthAttachment != null && depthAttachment.needsClear()) {
            clear |= 0x100;
            clearDepth = depthAttachment.depthClearValue;

            VRenderSystem.clearDepth(clearDepth);

            depthAttachment.needsClear = false;
        }

        if (clear != 0) {
            Renderer.clearAttachments(clear);
        }
    }

    protected void close() {
        VkGlFramebuffer.deleteFramebuffer(this.glId);
    }

    public boolean needsClear() {
        return this.colorAttachmentView.texture().needsClear() || (this.depthAttachment != null && this.depthAttachment.needsClear());
    }
}
