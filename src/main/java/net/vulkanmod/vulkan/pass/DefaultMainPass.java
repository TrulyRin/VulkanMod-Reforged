package net.vulkanmod.vulkan.pass;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.RenderScale;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.MainDepthSnapshot;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class DefaultMainPass implements MainPass {

    public static DefaultMainPass create() {
        return new DefaultMainPass();
    }

    private RenderTarget mainTarget;
    private final SwapChain swapChain;
    private Framebuffer mainFramebuffer;
    private MainPassTargets<Target, Passes> targets;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;
    private RenderPass presentRenderPass;

    private boolean renderScaleResolvedThisFrame;

    DefaultMainPass() {
        this.mainTarget = Minecraft.getInstance().getMainRenderTarget();
        this.swapChain = Vulkan.getSwapChain();
        this.mainFramebuffer = this.swapChain;

        this.targets = createTargets();
        try {
            this.presentRenderPass = createPresentRenderPass();
        } catch (RuntimeException | Error failure) {
            suppress(failure, this.targets::close);
            throw failure;
        }
        selectTarget();
    }

    private MainPassTargets<Target, Passes> createTargets() {
        return new MainPassTargets<>(new Target(this.swapChain, -1, -1), new MainPassTargets.Backend<>() {
            public Target createScaled(int width, int height, int scale) { return createScaledTarget(width, height); }
            public Passes createPasses(Target target) { return createRenderPasses(target.framebuffer()); }
            public void retireScaled(Target target) { disposeScaledTarget(target); }
            public void retirePasses(Passes passes) { retireAll(passes.main()::cleanUp, passes.load()::cleanUp); }
        });
    }

    private Passes createRenderPasses(Framebuffer framebuffer) {
        RenderPass.Builder builder = RenderPass.builder(framebuffer);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);

        RenderPass main = builder.build();

        try {
            builder = RenderPass.builder(framebuffer);
            builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
            builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
            builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            return new Passes(main, builder.build());
        } catch (RuntimeException | Error failure) {
            suppress(failure, main::cleanUp);
            throw failure;
        }
    }


    public boolean isMainRenderPass(RenderPass pass) {
        return pass != null && (pass == this.mainRenderPass || pass == this.auxRenderPass);
    }

    private RenderPass createPresentRenderPass() {
        RenderPass.Builder builder = RenderPass.builder(this.swapChain);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);

        return builder.build();
    }

    private record Target(Framebuffer framebuffer, int colorGlId, int depthGlId) {}
    private record Passes(RenderPass main, RenderPass load) {}

    private void selectTarget() {
        this.mainFramebuffer = this.targets.target().framebuffer();
        this.mainRenderPass = this.targets.passes().main();
        this.auxRenderPass = this.targets.passes().load();
    }

    @Override
    public RenderPass resumeRenderPass(RenderPass interrupted) {
        Passes nativePasses = this.targets.swapchainPasses();
        if (interrupted == this.presentRenderPass || interrupted == nativePasses.main()
                || interrupted == nativePasses.load()) {
            return nativePasses.load();
        }
        Passes worldPasses = this.targets.retainedScaledPasses();
        if (worldPasses != null && (interrupted == worldPasses.main() || interrupted == worldPasses.load())) {
            return worldPasses.load();
        }
        return interrupted;
    }


    public Framebuffer getRetainedWorldFramebuffer() {
        Passes retained = this.targets != null ? this.targets.retainedScaledPasses() : null;
        return retained != null ? retained.load().getFramebuffer() : null;
    }

    private void ensureMainFramebuffer() {
        int scale = RenderScale.clamp(Initializer.CONFIG.renderScale);
        Minecraft minecraft = Minecraft.getInstance();
        boolean retainScaled = RenderScale.isScaled(scale) && this.swapChain.getWidth() > 0
                && this.swapChain.getHeight() > 0 && minecraft.level != null;
        boolean useScaledNow = retainScaled && !this.renderScaleResolvedThisFrame && minecraft.screen == null;
        try {
            this.targets.select(retainScaled, useScaledNow,
                    RenderScale.scaleDimension(this.swapChain.getWidth(), scale),
                    RenderScale.scaleDimension(this.swapChain.getHeight(), scale), scale);
        } finally {

            selectTarget();
        }
    }

    private Target createScaledTarget(int width, int height) {
        VulkanImage colorImage = null;
        VulkanImage depthImage = null;
        int color = -1;
        int depth = -1;
        try {
            colorImage = VulkanImage.builder(width, height).setFormat(Framebuffer.DEFAULT_FORMAT)
                    .setUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT
                            | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    .setLinearFiltering(true).setClamp(true).createVulkanImage();
            requireImage(colorImage);
            depthImage = VulkanImage.createDepthImage(Vulkan.getDefaultDepthFormat(), width, height,
                    MainDepthSnapshot.depthAttachmentUsage(), false, true);
            requireImage(depthImage);
            Framebuffer framebuffer = Framebuffer.builder(colorImage, depthImage).setLinearFiltering(true).build();
            color = GlTexture.genTextureId();
            GlTexture.bindIdToImage(color, colorImage);
            depth = GlTexture.genTextureId();
            GlTexture.bindIdToImage(depth, depthImage);
            return new Target(framebuffer, color, depth);
        } catch (RuntimeException | Error failure) {
            rollbackTarget(failure, colorImage, depthImage, color, depth);
            throw failure;
        }
    }

    private static void requireImage(VulkanImage image) {
        if (image == null || image.getId() == 0 || image.getImageView() == 0)
            throw new IllegalStateException("Scaled main attachment allocation is unavailable");
    }

    private static void rollbackTarget(Throwable failure, VulkanImage colorImage, VulkanImage depthImage,
                                       int color, int depth) {
        if (color != -1) suppress(failure, () -> retireOwnedAlias(color));
        if (depth != -1) suppress(failure, () -> retireOwnedAlias(depth));
        if (colorImage != null) suppress(failure, colorImage::free);
        if (depthImage != null) suppress(failure, depthImage::free);
    }

    private void disposeScaledTarget(Target target) {
        retireAll(GlFramebuffer::invalidateBorrowedAttachments,
                target.framebuffer()::cleanUp,
                () -> retireOwnedAlias(target.colorGlId()),
                () -> retireOwnedAlias(target.depthGlId()));
    }


    static void retireOwnedAlias(int id) {
        if (GlTexture.getTexture(id) == null) return;
        GlTexture.setVulkanImage(id, null);
        GlTexture.glDeleteTextures(id);
    }

    private static void suppress(Throwable primary, Runnable release) {
        try { release.run(); }
        catch (RuntimeException | Error cleanup) { if (cleanup != primary) primary.addSuppressed(cleanup); }
    }

    private static void retireAll(Runnable... releases) {
        Throwable failure = null;
        for (Runnable release : releases) {
            try { release.run(); }
            catch (RuntimeException | Error cleanup) {
                if (failure == null) failure = cleanup;
                else if (failure != cleanup) failure.addSuppressed(cleanup);
            }
        }
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }


    @Override public void onResize() {
        if (this.targets == null) throw new IllegalStateException("Main pass is closed");
        MainPassTargets<Target, Passes> replacement = createTargets();
        RenderPass replacementPresent;
        try {
            replacementPresent = createPresentRenderPass();
        } catch (RuntimeException | Error failure) {
            suppress(failure, replacement::close);
            throw failure;
        }
        MainPassTargets<Target, Passes> previous = this.targets;
        RenderPass previousPresent = this.presentRenderPass;
        this.targets = replacement;
        this.presentRenderPass = replacementPresent;
        this.renderScaleResolvedThisFrame = false;
        selectTarget();
        retireAll(previous::close, previousPresent::cleanUp);
    }

    @Override public void cleanUp() {
        MainPassTargets<Target, Passes> previous = this.targets;
        if (previous == null) return;
        RenderPass previousPresent = this.presentRenderPass;
        this.targets = null;
        this.presentRenderPass = null;
        this.mainRenderPass = null;
        this.auxRenderPass = null;
        this.mainFramebuffer = null;
        retireAll(previous::close, previousPresent::cleanUp);
    }

    private boolean isUsingScaledFramebuffer() {
        return this.targets.isScaled();
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

        Renderer.setViewport(0, 0, this.swapChain.getWidth(), this.swapChain.getHeight());
        Renderer.applyScissor();
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
        this.targets.resolveForGui();
        selectTarget();
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
            Renderer.setViewport(0, 0, this.swapChain.getWidth(), this.swapChain.getHeight());
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
            return this.targets.target().colorGlId();
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
            return this.targets.target().depthGlId();
        }

        return Vulkan.getSwapChain().getDepthAttachmentGlId();
    }
}
