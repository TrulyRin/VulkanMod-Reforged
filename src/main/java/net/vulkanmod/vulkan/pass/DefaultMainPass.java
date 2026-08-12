package net.vulkanmod.vulkan.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkViewport;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class DefaultMainPass implements MainPass {

    public static DefaultMainPass create() {
        return new DefaultMainPass();
    }

    private RenderTarget mainTarget;
    private final SwapChain swapChain;
    private Framebuffer mainFramebuffer;
    private Framebuffer scaledFramebuffer;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;
    private RenderPass presentRenderPass;

    private int scaledFramebufferWidth = -1;
    private int scaledFramebufferHeight = -1;
    private int scaledFramebufferScale = RenderScale.DEFAULT;
    private int scaledColorAttachmentGlId = -1;
    private int scaledDepthAttachmentGlId = -1;
    private boolean renderScaleResolvedThisFrame;

    DefaultMainPass() {
        this.mainTarget = Minecraft.getInstance().getMainRenderTarget();
        this.swapChain = Vulkan.getSwapChain();
        this.mainFramebuffer = this.swapChain;

        createRenderPasses();
        createPresentRenderPass();
    }

    private void createRenderPasses() {
        RenderPass.Builder builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);

        this.mainRenderPass = builder.build();

        builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        this.auxRenderPass = builder.build();
    }

    private void createPresentRenderPass() {
        RenderPass.Builder builder = RenderPass.builder(this.swapChain);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_DONT_CARE);

        this.presentRenderPass = builder.build();
    }

    private void setMainFramebuffer(Framebuffer framebuffer) {
        if (this.mainFramebuffer == framebuffer) {
            return;
        }

        if (this.mainRenderPass != null) {
            this.mainRenderPass.cleanUp();
        }
        if (this.auxRenderPass != null) {
            this.auxRenderPass.cleanUp();
        }

        this.mainFramebuffer = framebuffer;
        createRenderPasses();
    }

    private void ensureMainFramebuffer() {
        int scale = RenderScale.clamp(Initializer.CONFIG.renderScale);

        if (!shouldUseScaledFramebuffer(scale)) {
            disposeScaledFramebuffer();
            setMainFramebuffer(this.swapChain);
            return;
        }

        int scaledWidth = RenderScale.scaleDimension(this.swapChain.getWidth(), scale);
        int scaledHeight = RenderScale.scaleDimension(this.swapChain.getHeight(), scale);

        if (this.scaledFramebuffer == null
                || this.scaledFramebufferWidth != scaledWidth
                || this.scaledFramebufferHeight != scaledHeight
                || this.scaledFramebufferScale != scale) {
            disposeScaledFramebuffer();

            this.scaledFramebuffer = Framebuffer.builder(scaledWidth, scaledHeight, 1, true)
                    .setLinearFiltering(true)
                    .build();
            this.scaledColorAttachmentGlId = GlTexture.genTextureId();
            GlTexture.bindIdToImage(this.scaledColorAttachmentGlId, this.scaledFramebuffer.getColorAttachment());
            this.scaledDepthAttachmentGlId = GlTexture.genTextureId();
            GlTexture.bindIdToImage(this.scaledDepthAttachmentGlId, this.scaledFramebuffer.getDepthAttachment());
            this.scaledFramebufferWidth = scaledWidth;
            this.scaledFramebufferHeight = scaledHeight;
            this.scaledFramebufferScale = scale;
        }

        setMainFramebuffer(this.scaledFramebuffer);
    }

    private boolean shouldUseScaledFramebuffer(int scale) {
        Minecraft minecraft = Minecraft.getInstance();
        return RenderScale.isScaled(scale)
                && this.swapChain.getWidth() > 0
                && this.swapChain.getHeight() > 0
                && !this.renderScaleResolvedThisFrame
                && minecraft.level != null
                && minecraft.screen == null;
    }

    private void disposeScaledFramebuffer() {
        if (this.scaledFramebuffer != null) {
            this.scaledFramebuffer.cleanUp();
            this.scaledFramebuffer = null;
        }

        if (this.scaledColorAttachmentGlId != -1) {
            GlTexture.setVulkanImage(this.scaledColorAttachmentGlId, null);
            this.scaledColorAttachmentGlId = -1;
        }

        if (this.scaledDepthAttachmentGlId != -1) {
            GlTexture.setVulkanImage(this.scaledDepthAttachmentGlId, null);
            this.scaledDepthAttachmentGlId = -1;
        }

        this.scaledFramebufferWidth = -1;
        this.scaledFramebufferHeight = -1;
        this.scaledFramebufferScale = RenderScale.DEFAULT;
    }

    private boolean isUsingScaledFramebuffer() {
        return this.scaledFramebuffer != null && this.mainFramebuffer == this.scaledFramebuffer;
    }

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        this.renderScaleResolvedThisFrame = false;
        ensureMainFramebuffer();

        VulkanImage colorAttachment = this.mainFramebuffer.getColorAttachment();
        colorAttachment.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        this.mainFramebuffer.beginRenderPass(commandBuffer, this.mainRenderPass, stack);

        if (isUsingScaledFramebuffer()) {
            Renderer.setViewportScale(this.swapChain.getWidth(), this.swapChain.getHeight());
        } else {
            Renderer.clearViewportScale();
        }

        VkViewport.Buffer pViewport = this.mainFramebuffer.viewport(stack);
        vkCmdSetViewport(commandBuffer, 0, pViewport);

        VkRect2D.Buffer pScissor = this.mainFramebuffer.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, pScissor);
    }

    @Override
    public void end(VkCommandBuffer commandBuffer) {
        Renderer.getInstance().endRenderPass(commandBuffer);

        try(MemoryStack stack = MemoryStack.stackPush()) {
            SwapChain swapChain = Vulkan.getSwapChain();

            if (isUsingScaledFramebuffer()) {
                resolveScaledFramebufferToSwapchain(commandBuffer, false);
            }

            swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        }

        int result = vkEndCommandBuffer(commandBuffer);
        if(result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }
    }

    @Override
    public void resolveRenderScaleForGui() {
        if (!isUsingScaledFramebuffer() || this.renderScaleResolvedThisFrame) {
            return;
        }

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getInstance().endRenderPass(commandBuffer);
        resolveScaledFramebufferToSwapchain(commandBuffer, true);
        this.renderScaleResolvedThisFrame = true;
        setMainFramebuffer(this.swapChain);
    }

    private void resolveScaledFramebufferToSwapchain(VkCommandBuffer commandBuffer, boolean keepRendering) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            this.swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            this.swapChain.beginRenderPass(commandBuffer, this.presentRenderPass, stack);
            Renderer.clearViewportScale();
            Renderer.resetViewport();
            Renderer.resetScissor();
            VTextureSelector.bindTexture(this.mainFramebuffer.getColorAttachment());
            DrawUtil.blitRenderScaleToScreen();

            if (!keepRendering) {
                Renderer.getInstance().endRenderPass(commandBuffer);
            }
        }
    }

    public void rebindMainTarget() {
        ensureMainFramebuffer();
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if(boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            return;

        Renderer.getInstance().endRenderPass(commandBuffer);

        try(MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.beginRenderPass(commandBuffer, this.auxRenderPass, stack);
            if (isUsingScaledFramebuffer()) {
                Renderer.setViewportScale(this.swapChain.getWidth(), this.swapChain.getHeight());
            } else {
                Renderer.clearViewportScale();
            }
        }

    }

    @Override
    public void bindAsTexture() {
        ensureMainFramebuffer();
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if(boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        VTextureSelector.bindTexture(this.mainFramebuffer.getColorAttachment());
    }

    public int getColorAttachmentGlId() {
        ensureMainFramebuffer();

        if (isUsingScaledFramebuffer()) {
            return this.scaledColorAttachmentGlId;
        }

        return Vulkan.getSwapChain().getColorAttachmentGlId();
    }

    @Override
    public VulkanImage getColorAttachment() {
        ensureMainFramebuffer();
        return this.mainFramebuffer != null ? this.mainFramebuffer.getColorAttachment() : null;
    }

    @Override
    public VulkanImage getDepthAttachment() {
        ensureMainFramebuffer();
        return this.mainFramebuffer != null ? this.mainFramebuffer.getDepthAttachment() : null;
    }

    @Override
    public int getDepthAttachmentGlId() {
        ensureMainFramebuffer();

        if (isUsingScaledFramebuffer()) {
            return this.scaledDepthAttachmentGlId;
        }

        return Vulkan.getSwapChain().getDepthAttachmentGlId();
    }
}
