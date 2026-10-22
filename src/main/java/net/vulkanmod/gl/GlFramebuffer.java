package net.vulkanmod.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.pass.MainPass;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.vulkan.VK11.VK_ATTACHMENT_LOAD_OP_LOAD;
import static org.lwjgl.vulkan.VK11.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_STENCIL_BIT;

public class GlFramebuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlFramebuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundDrawId = 0;
    private static int boundReadId = 0;
    private static GlFramebuffer boundDrawFramebuffer;
    private static GlFramebuffer boundReadFramebuffer;
    private static boolean drawViewportPending;
    private static long drawViewportRevision;
    private static long borrowedAttachmentGeneration;

    public static void resetBoundFramebuffer() {
        boundDrawFramebuffer = null;
        boundReadFramebuffer = null;
        boundDrawId = 0;
        boundReadId = 0;
        drawViewportPending = false;
        drawViewportRevision = 0;
    }


    public static BindingSnapshot captureBindings() {
        return new BindingSnapshot(boundDrawFramebuffer, boundReadFramebuffer, boundDrawId, boundReadId,
                drawViewportPending, drawViewportRevision);
    }

    public static final class BindingSnapshot {
        private final GlFramebuffer draw, read;
        private final int drawId, readId;
        private final boolean viewportPending;
        private final long viewportRevision;

        private BindingSnapshot(GlFramebuffer draw, GlFramebuffer read, int drawId, int readId,
                                boolean viewportPending, long viewportRevision) {
            this.draw = draw;
            this.read = read;
            this.drawId = drawId;
            this.readId = readId;
            this.viewportPending = viewportPending;
            this.viewportRevision = viewportRevision;
        }

        public void restore() {
            boundDrawFramebuffer = draw;
            boundReadFramebuffer = read;
            boundDrawId = drawId;
            boundReadId = readId;
            drawViewportPending = viewportPending;
            drawViewportRevision = viewportRevision;
        }


        public boolean restoreLiveTargets() {
            boolean liveDraw = drawId == 0 || draw != null && map.get(drawId) == draw;
            if (liveDraw) bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawId);
            if (readId == 0 || read != null && map.get(readId) == read)
                bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readId);
            if (liveDraw) {
                drawViewportPending = viewportPending;
                drawViewportRevision = viewportRevision;
            }
            return liveDraw;
        }
    }


    public static void ensureDrawFramebufferRendering() {
        GlFramebuffer draw = boundDrawFramebuffer;
        Renderer renderer = Renderer.getInstance();
        if (renderer == null || Renderer.skipRendering || !Renderer.isRecording()) return;
        if (renderer.getBoundRenderPass() != null) {
            if (draw != null && renderer.getBoundRenderPass() == draw.renderPass) applyPendingViewport();
            return;
        }

        if (draw == null) {
            renderer.beginMainRenderingPreservingRasterState();
            return;
        }

        draw.ensureNativePair();
        if (draw.framebuffer == null || draw.renderPass == null) return;

        if (renderer.beginRenderingPreservingRasterState(draw.renderPass, draw.framebuffer)) applyPendingViewport();
    }

    private static void applyPendingViewport() {
        if (!drawViewportPending) return;
        if (Renderer.isViewportInitialized() && (drawViewportRevision == Renderer.getViewportRevision()
                || Renderer.isGlViewportSelection())) {
            Renderer.setGlViewport(Renderer.getGlViewport(0), Renderer.getGlViewport(1),
                    Renderer.getGlViewport(2), Renderer.getGlViewport(3));
        }
        drawViewportPending = false;
    }

    public static void beginRendering(GlFramebuffer glFramebuffer) {
        glFramebuffer.ensureNativePair();
        if (glFramebuffer.framebuffer == null) return;
        Renderer renderer = Renderer.getInstance();

        if (renderer.isRecreatingSwapChain()) {
            boundDrawFramebuffer = glFramebuffer;
            boundDrawId = glFramebuffer.id;
            return;
        }
        BindingSnapshot bindings = captureBindings();
        boolean deferredForResize = false;
        try {


            if (!Renderer.isRecording()) renderer.beginFrame();
            if (renderer.isResizeFrameDeferred()) {
                deferredForResize = true;
                return;
            }

            glFramebuffer.ensureNativePair();
            if (glFramebuffer.framebuffer == null || glFramebuffer.renderPass == null)
                throw new IllegalStateException("Framebuffer attachments are unavailable after frame entry");
            if (!renderer.beginRendering(glFramebuffer.renderPass, glFramebuffer.framebuffer))
                throw new IllegalStateException("Framebuffer write requires an active renderer frame");
        } finally {

            bindings.restore();
            if (deferredForResize) {
                boundDrawFramebuffer = glFramebuffer;
                boundDrawId = glFramebuffer.id;
            }
        }

        Framebuffer framebuffer = glFramebuffer.framebuffer;
        int viewWidth = framebuffer.getWidth();
        int viewHeight = framebuffer.getHeight();

        if (glFramebuffer.usesMainTargetOrientation()) {

            Renderer.setViewport(0, 0, viewWidth, viewHeight);
        } else {
            Renderer.setInvertedViewport(0, 0, viewWidth, viewHeight);
        }
        Renderer.applyScissor();

        boundDrawFramebuffer = glFramebuffer;
        boundDrawId = glFramebuffer.id;
        drawViewportPending = false;
    }

    public static int genFramebufferId() {
        int id = ID_COUNTER;
        map.put(id, new GlFramebuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindFramebuffer(int target, int id) {
        if (target == GL30.GL_FRAMEBUFFER) {
            bindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, id);
            bindFramebuffer(GL30.GL_READ_FRAMEBUFFER, id);
            return;
        }

        if (id == 0) {
            if (target == GL30.GL_DRAW_FRAMEBUFFER) {
                Renderer renderer = Renderer.getInstance();
                GlFramebuffer savedRead = boundReadFramebuffer;
                int savedReadId = boundReadId;
                try {
                    if (renderer != null) {
                        if (Renderer.isRecording() && !Renderer.skipRendering)
                            renderer.getMainPass().rebindMainTarget();
                        else renderer.endRenderPass();
                    }
                } finally {
                    boundReadFramebuffer = savedRead;
                    boundReadId = savedReadId;
                    boundDrawFramebuffer = null;
                    boundDrawId = 0;
                    drawViewportPending = false;
                }
            } else if (target == GL30.GL_READ_FRAMEBUFFER) {
                boundReadFramebuffer = null;
                boundReadId = 0;
            }
            return;
        }

        GlFramebuffer glFramebuffer = map.get(id);

        if (glFramebuffer == null) {

            GlEmulationLog.warnOnce("framebuffer.bind.ungenerated",
                    "glBindFramebuffer with ungenerated name; creating it on demand");
            glFramebuffer = new GlFramebuffer(id);
            map.put(id, glFramebuffer);
            if (id >= ID_COUNTER)
                ID_COUNTER = id + 1;
        }

        if (target == GL30.GL_READ_FRAMEBUFFER) {
            boundReadFramebuffer = glFramebuffer;
            boundReadId = id;
            return;
        }

        glFramebuffer.ensureNativePair();
        if (glFramebuffer.framebuffer == null) {
            Renderer renderer = Renderer.getInstance();
            if (renderer != null && renderer.getBoundRenderPass() != null) endRenderPassPreservingBindings(renderer);
        }

        if (boundDrawId == id && boundDrawFramebuffer == glFramebuffer) {
            if (glFramebuffer.framebuffer != null
                    && Renderer.getInstance().getBoundRenderPass() != glFramebuffer.renderPass) {
                beginRendering(glFramebuffer);
            }

            return;
        }

        boundDrawFramebuffer = glFramebuffer;
        boundDrawId = id;
        drawViewportPending = true;
        drawViewportRevision = Renderer.getViewportRevision();

        if (glFramebuffer.framebuffer != null) {
            beginRendering(glFramebuffer);
        }
    }

    public static void deleteFramebuffer(int id) {
        if (id == 0) {
            return;
        }

        GlFramebuffer framebuffer = map.remove(id);

        if (framebuffer == null) {
            return;
        }

        try {
            framebuffer.cleanUp();
            for (int attachment : framebuffer.attachments.keySet().toIntArray()) framebuffer.clearAttachment(attachment);
        } finally {
            if (boundDrawFramebuffer == framebuffer) {
                boundDrawFramebuffer = null;
                boundDrawId = 0;
                drawViewportPending = false;
            }
            if (boundReadFramebuffer == framebuffer) {
                boundReadFramebuffer = null;
                boundReadId = 0;
            }
        }
    }

    public static void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
        if (texTarget != GL11.GL_TEXTURE_2D) {
            return;
        }
        if (level != 0) {
            return;
        }

        GlFramebuffer framebuffer = framebufferForTarget(target);
        if (framebuffer == null)
            return;

        if (texture == 0) {
            framebuffer.clearAttachment(attachment);
            return;
        }

        framebuffer.prepareAttachmentMutation(attachment);
        framebuffer.trackAttachmentTexture(attachment, texture, level);

        if (!isRenderableAttachment(attachment)) {
            return;
        }

        framebuffer.setAttachmentTexture(attachment, texture);
    }

    private static boolean isColorAttachment(int attachment) {
        return attachment >= GL30.GL_COLOR_ATTACHMENT0 && attachment <= GL30.GL_COLOR_ATTACHMENT0 + 31;
    }

    private static boolean isSupportedColorAttachment(int attachment) {
        return attachment == GL30.GL_COLOR_ATTACHMENT0;
    }

    private static boolean isRenderableAttachment(int attachment) {
        return isSupportedColorAttachment(attachment)
                || attachment == GL30.GL_DEPTH_ATTACHMENT
                || attachment == GL30.GL_STENCIL_ATTACHMENT
                || attachment == GL30.GL_DEPTH_STENCIL_ATTACHMENT;
    }

    public static void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        GlFramebuffer framebuffer = framebufferForTarget(target);
        if (framebuffer == null)
            return;

        if (renderbuffer == 0) {
            framebuffer.clearAttachment(attachment);
            return;
        }

        framebuffer.prepareAttachmentMutation(attachment);
        framebuffer.trackAttachmentRenderbuffer(attachment, renderbuffer);

        if (!isRenderableAttachment(attachment)) {
            return;
        }

        framebuffer.setAttachmentRenderbuffer(attachment, renderbuffer);
    }

    public static int glCheckFramebufferStatus(int target) {

        return GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    public static int getFramebufferAttachmentParameteri(int target, int attachment, int pname) {
        GlFramebuffer framebuffer = framebufferForTarget(target);
        if (framebuffer == null) {
            return pname == GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE ? GL11.GL_NONE : 0;
        }

        return framebuffer.getAttachmentParameter(attachment, pname);
    }

    public static int getNamedFramebufferAttachmentParameteri(int framebuffer, int attachment, int pname) {
        GlFramebuffer glFramebuffer = map.get(framebuffer);
        if (glFramebuffer == null) {
            return pname == GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE ? GL11.GL_NONE : 0;
        }

        return glFramebuffer.getAttachmentParameter(attachment, pname);
    }

    public static void blitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1,
                                       int dstX0, int dstY0, int dstX1, int dstY1,
                                       int mask, int filter) {
        requireTransferOwner();
        GlFramebuffer destination = boundDrawFramebuffer;
        BlitImages images = resolveBlitImages(boundReadFramebuffer, destination, mask);
        if (images == null) {
            GlEmulationLog.warnOnce("framebuffer.blit.missingAttachment." + mask + "." + boundReadId + "." + boundDrawId,
                    String.format("glBlitFramebuffer skipped: readFb=%d[%s] drawFb=%d[%s] mask=0x%X src=(%d,%d->%d,%d) dst=(%d,%d->%d,%d) — read/draw attachment unavailable",
                            boundReadId, describeFb(boundReadFramebuffer), boundDrawId, describeFb(boundDrawFramebuffer), mask,
                            srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1));
            return;
        }

        if (!withinBounds(images.source, srcX0, srcY0, srcX1, srcY1)
                || !withinBounds(images.destination, dstX0, dstY0, dstX1, dstY1)) {
            GlEmulationLog.warnOnce("framebuffer.blit.outOfBounds." + mask,
                    String.format("glBlitFramebuffer skipped: out of bounds — src %dx%d (%d,%d->%d,%d) dst %dx%d (%d,%d->%d,%d)",
                            images.source.width, images.source.height, srcX0, srcY0, srcX1, srcY1,
                            images.destination.width, images.destination.height, dstX0, dstY0, dstX1, dstY1));
            return;
        }

        boolean destinationBottomUp = blitDestinationBottomUp(destination, images,
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1);
        blitResolvedImages(images, destinationBottomUp,
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, filter);
    }

    public static void blitNamedFramebuffer(int readFramebuffer, int drawFramebuffer,
                                            int srcX0, int srcY0, int srcX1, int srcY1,
                                            int dstX0, int dstY0, int dstX1, int dstY1,
                                            int mask, int filter) {
        requireTransferOwner();
        GlFramebuffer destination = map.get(drawFramebuffer);
        BlitImages images = resolveBlitImages(map.get(readFramebuffer), destination, mask);
        if (images == null) {
            GlEmulationLog.warnOnce("framebuffer.blitNamed.missingAttachment." + mask + "." + readFramebuffer + "." + drawFramebuffer,
                    String.format("glBlitNamedFramebuffer skipped: readFb=%d[%s] drawFb=%d[%s] mask=0x%X — attachment unavailable",
                            readFramebuffer, describeFb(map.get(readFramebuffer)), drawFramebuffer, describeFb(map.get(drawFramebuffer)), mask));
            return;
        }

        if (!withinBounds(images.source, srcX0, srcY0, srcX1, srcY1)
                || !withinBounds(images.destination, dstX0, dstY0, dstX1, dstY1)) {
            GlEmulationLog.warnOnce("framebuffer.blitNamed.outOfBounds." + mask,
                    "glBlitNamedFramebuffer skipped: blit region out of bounds for resolved image");
            return;
        }

        boolean destinationBottomUp = blitDestinationBottomUp(destination, images,
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1);
        blitResolvedImages(images, destinationBottomUp,
                srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, filter);
    }

    private static void blitResolvedImages(BlitImages images, boolean destinationBottomUp,
                                           int srcX0, int srcY0, int srcX1, int srcY1,
                                           int dstX0, int dstY0, int dstX1, int dstY1, int filter) {
        int physicalSrcY0 = physicalBlitY(images.source, srcY0), physicalSrcY1 = physicalBlitY(images.source, srcY1);
        int physicalDstY0 = physicalBlitY(images.destination, dstY0, destinationBottomUp);
        int physicalDstY1 = physicalBlitY(images.destination, dstY1, destinationBottomUp);
        ImageUtil.blitImage(images.source, images.destination,
                srcX0, physicalSrcY0, srcX1, physicalSrcY1, dstX0, physicalDstY0, dstX1, physicalDstY1,
                images.aspectMask, filter);


        if (srcX0 == srcX1 || physicalSrcY0 == physicalSrcY1 || dstX0 == dstX1 || physicalDstY0 == physicalDstY1)
            return;
        if (images.source.getId() == images.destination.getId()
                && srcX0 == dstX0 && physicalSrcY0 == physicalDstY0 && srcX1 == dstX1 && physicalSrcY1 == physicalDstY1)
            return;
        images.destination.recordRasterOrigin(destinationBottomUp);
    }

    private static void requireTransferOwner() {
        ImageUtil.requireTransferReady();
        if (Renderer.getInstance() == null || !Renderer.isRecording()) return;
        RenderSystem.assertOnRenderThread();
        if (Renderer.skipRendering || Renderer.getCommandBuffer() == null)
            throw new IllegalStateException("Framebuffer transfer requires an active renderer primary");
    }

    private static int physicalBlitY(VulkanImage image, int y) {
        return FramebufferBlitCoordinates.physicalY(y, image.height, image.isRowsBottomUp());
    }

    private static int physicalBlitY(VulkanImage image, int y, boolean bottomUp) {
        return FramebufferBlitCoordinates.physicalY(y, image.height, bottomUp);
    }


    private static boolean blitDestinationBottomUp(GlFramebuffer destination, BlitImages images,
                                                   int srcX0, int srcY0, int srcX1, int srcY1,
                                                   int dstX0, int dstY0, int dstX1, int dstY1) {
        VulkanImage image = images.destination;
        if (image.isRowsBottomUp()) return true;
        if (destination == null || destination.borrowedPairs != null || images.source.getId() == image.getId()
                || srcX1 <= srcX0 || srcY1 <= srcY0
                || dstX0 != 0 || dstY0 != 0 || dstX1 != image.width || dstY1 != image.height)
            return false;
        if (nativeOrBorrowedImage(image)) return false;
        return images.aspectMask == image.aspect || logicalDepthOnlyReplacement(destination, images);
    }


    private static boolean logicalDepthOnlyReplacement(GlFramebuffer destination, BlitImages images) {
        VulkanImage image = images.destination;
        if (images.aspectMask != VK_IMAGE_ASPECT_DEPTH_BIT
                || image.aspect != (VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT)
                || images.source.format != image.format || destination.logicalDepthOnlyTexture(image) == null
                || GlTexture.hasNonDepthOnlyAlias(image) || GlRenderbuffer.hasImageAlias(image)) return false;

        for (GlFramebuffer target : map.values()) {
            if (target.depthAttachment == image && (target.attachments.containsKey(GL30.GL_STENCIL_ATTACHMENT)
                    || target.attachments.containsKey(GL30.GL_DEPTH_STENCIL_ATTACHMENT))) return false;
            var aliases = target.textureAttachments.int2ReferenceEntrySet().fastIterator();
            while (aliases.hasNext()) {
                var alias = aliases.next();
                GlTexture texture = alias.getValue();
                if (texture != null && texture.vulkanImage == image
                        && (alias.getIntKey() != GL30.GL_DEPTH_ATTACHMENT
                        || target.logicalDepthOnlyTexture(image) != texture)) return false;
            }


            var attachments = target.attachments.int2ReferenceEntrySet().fastIterator();
            while (attachments.hasNext()) {
                AttachmentInfo attachment = attachments.next().getValue();
                if (attachment.objectType == GL30.GL_RENDERBUFFER && attachment.renderbufferImage == image) return false;
            }
        }
        return true;
    }

    private GlTexture logicalDepthOnlyTexture(VulkanImage image) {
        AttachmentInfo attachment = this.attachments.get(GL30.GL_DEPTH_ATTACHMENT);
        GlTexture texture = this.textureAttachments.get(GL30.GL_DEPTH_ATTACHMENT);
        return this.depthAttachment == image && attachment != null && attachment.objectType == GL11.GL_TEXTURE
                && attachment.textureLevel == 0 && texture != null && attachment.objectName == texture.id
                && texture.isKnownDepthOnlyImage(image) ? texture : null;
    }


    private static boolean nativeOrBorrowedImage(VulkanImage image) {
        Renderer renderer = Renderer.getInstance();
        MainPass main = renderer == null ? null : renderer.getMainPass();
        if (main != null) {
            if (image == main.getColorAttachment() || image == main.getDepthAttachment()) return true;
            if (main instanceof DefaultMainPass pass) {
                Framebuffer retained = pass.getRetainedWorldFramebuffer();
                if (retained != null && (image == retained.getColorAttachment() || image == retained.getDepthAttachment()))
                    return true;
            }
        }
        var swapchain = Vulkan.getSwapChain();
        if (swapchain != null) {
            if (image == swapchain.getDepthAttachment()) return true;
            var images = swapchain.getImages();
            if (images != null) for (VulkanImage nativeImage : images) if (image == nativeImage) return true;
        }
        for (GlFramebuffer target : map.values()) {
            if (target.borrowedPairs != null && (image == target.colorAttachment || image == target.depthAttachment
                    || target.borrowedPairs.retainsAttachment(image)))
                return true;
        }
        return false;
    }

    static BlitImages resolveBlitImages(int mask) {
        return resolveBlitImages(boundReadFramebuffer, boundDrawFramebuffer, mask);
    }

    static BlitImages resolveNamedBlitImages(int readFramebuffer, int drawFramebuffer, int mask) {
        return resolveBlitImages(map.get(readFramebuffer), map.get(drawFramebuffer), mask);
    }

    private static BlitImages resolveBlitImages(GlFramebuffer readFramebuffer, GlFramebuffer drawFramebuffer, int mask) {
        if ((mask & GL11.GL_COLOR_BUFFER_BIT) != 0) {
            VulkanImage source = readFramebuffer != null ? readFramebuffer.colorAttachment : mainColorImage();
            VulkanImage destination = drawFramebuffer != null ? drawFramebuffer.colorAttachment : mainColorImage();
            return BlitImages.of(source, destination, VK_IMAGE_ASPECT_COLOR_BIT);
        }

        if ((mask & GL11.GL_DEPTH_BUFFER_BIT) != 0) {
            VulkanImage source = readFramebuffer != null ? readFramebuffer.depthAttachment : mainDepthImage();
            VulkanImage destination = drawFramebuffer != null ? drawFramebuffer.depthAttachment : mainDepthImage();
            return BlitImages.of(source, destination, VK_IMAGE_ASPECT_DEPTH_BIT);
        }

        if ((mask & GL11.GL_STENCIL_BUFFER_BIT) != 0) {
            VulkanImage source = readFramebuffer != null ? readFramebuffer.depthAttachment : mainDepthImage();
            VulkanImage destination = drawFramebuffer != null ? drawFramebuffer.depthAttachment : mainDepthImage();
            return BlitImages.of(source, destination, VK_IMAGE_ASPECT_STENCIL_BIT);
        }

        return null;
    }

    private static VulkanImage mainColorImage() {
        Renderer renderer = Renderer.getInstance();
        if (renderer == null) {
            return null;
        }
        MainPass mainPass = renderer.getMainPass();
        return mainPass != null ? mainPass.getColorAttachment() : null;
    }

    private static VulkanImage mainDepthImage() {
        Renderer renderer = Renderer.getInstance();
        if (renderer == null) {
            return null;
        }
        MainPass mainPass = renderer.getMainPass();
        return mainPass != null ? mainPass.getDepthAttachment() : null;
    }

    private static boolean withinBounds(VulkanImage image, int x0, int y0, int x1, int y1) {
        int minX = Math.min(x0, x1);
        int minY = Math.min(y0, y1);
        int maxX = Math.max(x0, x1);
        int maxY = Math.max(y0, y1);
        return minX >= 0 && minY >= 0 && maxX <= image.width && maxY <= image.height;
    }

    private static String describeFb(GlFramebuffer fb) {
        if (fb == null) return "default(0)";
        return "color=" + (fb.colorAttachment != null) + ",depth=" + (fb.depthAttachment != null);
    }

    public static ReadPixelsImage resolveReadPixelsImage(int format) {
        if (boundReadFramebuffer == null) {
            return null;
        }

        if (format == GL11.GL_DEPTH_COMPONENT) {
            return ReadPixelsImage.of(boundReadFramebuffer.depthAttachment, VK_IMAGE_ASPECT_DEPTH_BIT);
        }

        return ReadPixelsImage.of(boundReadFramebuffer.colorAttachment, VK_IMAGE_ASPECT_COLOR_BIT);
    }

    public static void namedFramebufferTexture(int framebuffer, int attachment, int texture, int level) {
        if (level != 0) {
            return;
        }

        GlFramebuffer glFramebuffer = framebufferForName(framebuffer);
        if (glFramebuffer == null) {
            return;
        }

        if (texture == 0) {
            glFramebuffer.clearAttachment(attachment);
            return;
        }

        glFramebuffer.prepareAttachmentMutation(attachment);
        glFramebuffer.trackAttachmentTexture(attachment, texture, level);

        if (isRenderableAttachment(attachment)) {
            glFramebuffer.setAttachmentTexture(attachment, texture);
        }
    }

    public static void namedFramebufferRenderbuffer(int framebuffer, int attachment, int renderbuffertarget, int renderbuffer) {
        GlFramebuffer glFramebuffer = framebufferForName(framebuffer);
        if (glFramebuffer == null) {
            return;
        }

        if (renderbuffer == 0) {
            glFramebuffer.clearAttachment(attachment);
            return;
        }

        glFramebuffer.prepareAttachmentMutation(attachment);
        glFramebuffer.trackAttachmentRenderbuffer(attachment, renderbuffer);

        if (isRenderableAttachment(attachment)) {
            glFramebuffer.setAttachmentRenderbuffer(attachment, renderbuffer);
        }
    }

    public static GlFramebuffer getBoundFramebuffer() {
        return boundDrawFramebuffer;
    }

    public static int getBoundId() {
        return boundDrawId;
    }

    public static int getBoundReadId() {
        return boundReadId;
    }

    public static GlFramebuffer getFramebuffer(int id) {
        return map.get(id);
    }


    public static boolean refreshBorrowedAttachments(int framebufferId, int colorTextureId, int depthTextureId, int capacity) {
        GlFramebuffer target = map.get(framebufferId);
        if (target == null || capacity < 1 || boundDrawFramebuffer == target || boundDrawId == framebufferId) {
            return false;
        }

        GlTexture colorTexture = GlTexture.getTexture(colorTextureId);
        GlTexture depthTexture = GlTexture.getTexture(depthTextureId);
        VulkanImage color = colorTexture != null ? colorTexture.vulkanImage : null;
        VulkanImage depth = depthTexture != null ? depthTexture.vulkanImage : null;
        if (color == null || depth == null || color.width <= 0 || color.height <= 0
                || color.width != depth.width || color.height != depth.height) {
            return false;
        }

        BorrowedFramebufferCache<NativePair> previousCache = target.borrowedPairs;
        BorrowedFramebufferCache<NativePair> cache = previousCache;
        if (cache == null || target.borrowedCapacity != capacity) {
            cache = new BorrowedFramebufferCache<>(capacity,
                    key -> target.borrowedPairFactory != null
                            ? target.borrowedPairFactory.apply(key)
                            : createBorrowedNativePair((VulkanImage) key.colorIdentity(), (VulkanImage) key.depthIdentity()),
                    NativePair::cleanUp);
        }

        NativePair pair;
        try {
            pair = cache.acquire(borrowedAttachmentGeneration,
                    color, color.getId(), color.getImageView(), color.width, color.height, color.format,
                    depth, depth.getId(), depth.getImageView(), depth.width, depth.height, depth.format);
        } catch (RuntimeException | Error failure) {


            if (previousCache != null && target.borrowedCurrentPair != null
                    && !previousCache.retains(target.borrowedCurrentPair)) {
                target.framebuffer = null;
                target.renderPass = null;
                target.colorAttachment = null;
                target.depthAttachment = null;
                target.borrowedCurrentPair = null;
            }
            throw failure;
        }
        NativePair previousOrdinary = previousCache == null && (target.framebuffer != null || target.renderPass != null)
                ? new NativePair(target.framebuffer, target.renderPass) : null;


        target.borrowedPairs = cache;
        target.borrowedCapacity = capacity;
        target.borrowedCurrentPair = pair;
        target.colorAttachment = color;
        target.depthAttachment = depth;
        target.framebuffer = pair.framebuffer;
        target.renderPass = pair.renderPass;
        target.trackBorrowedTexture(GL30.GL_COLOR_ATTACHMENT0, colorTextureId);
        target.trackBorrowedTexture(GL30.GL_DEPTH_ATTACHMENT, depthTextureId);
        if (target.attachments.containsKey(GL30.GL_DEPTH_STENCIL_ATTACHMENT)) {
            target.trackBorrowedTexture(GL30.GL_DEPTH_STENCIL_ATTACHMENT, depthTextureId);
        }
        if (target.attachments.containsKey(GL30.GL_STENCIL_ATTACHMENT)) {
            target.trackBorrowedTexture(GL30.GL_STENCIL_ATTACHMENT, depthTextureId);
        }

        if (previousCache != null && previousCache != cache) previousCache.invalidate();
        if (previousOrdinary != null) previousOrdinary.cleanUp();
        return true;
    }

    public static void invalidateBorrowedAttachments() {
        borrowedAttachmentGeneration++;
        Throwable firstFailure = null;
        for (GlFramebuffer target : map.values()) {
            if (target.borrowedPairs == null) continue;
            try {
                target.cleanUp();
            } catch (RuntimeException | Error failure) {
                firstFailure = appendFailure(firstFailure, failure);
            } finally {
                target.colorAttachment = null;
                target.depthAttachment = null;
            }
        }
        rethrowFailure(firstFailure);
    }

    private static GlFramebuffer framebufferForTarget(int target) {
        return switch (target) {
            case GL30.GL_READ_FRAMEBUFFER -> boundReadFramebuffer;
            case GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_FRAMEBUFFER -> boundDrawFramebuffer;
            default -> null;
        };
    }

    private static GlFramebuffer framebufferForName(int id) {
        if (id == 0) {
            return null;
        }

        GlFramebuffer glFramebuffer = map.get(id);
        if (glFramebuffer != null) {
            return glFramebuffer;
        }

        GlEmulationLog.warnOnce("framebuffer.named.ungenerated",
                "named framebuffer operation with ungenerated name; creating it on demand");
        glFramebuffer = new GlFramebuffer(id);
        map.put(id, glFramebuffer);
        if (id >= ID_COUNTER)
            ID_COUNTER = id + 1;

        return glFramebuffer;
    }

    private final int id;
    Framebuffer framebuffer;
    RenderPass renderPass;
    java.util.function.Function<BorrowedFramebufferCache.Key, NativePair> borrowedPairFactory;
    java.util.function.BiFunction<VulkanImage, VulkanImage, NativePair> ordinaryPairFactory;
    private BorrowedFramebufferCache<NativePair> borrowedPairs;

    private int borrowedCapacity;
    private NativePair borrowedCurrentPair;

    VulkanImage colorAttachment;
    VulkanImage depthAttachment;
    private final Int2ReferenceOpenHashMap<AttachmentInfo> attachments = new Int2ReferenceOpenHashMap<>();
    private final Int2ReferenceOpenHashMap<GlTexture> textureAttachments = new Int2ReferenceOpenHashMap<>();

    GlFramebuffer(int i) {
        this.id = i;
    }


    public boolean isColorAttachmentBottomUp() {
        return this.colorAttachment != null && this.colorAttachment.isRowsBottomUp();
    }


    public boolean usesMainTargetOrientation() {
        if (this.borrowedPairs != null) return true;
        Renderer renderer = Renderer.getInstance();
        return this.colorAttachment != null && renderer != null && renderer.getMainPass() != null
                && this.colorAttachment == renderer.getMainPass().getColorAttachment();
    }

    boolean beginRendering() {
        return Renderer.getInstance().beginRendering(this.renderPass, this.framebuffer);
    }

    void setAttachmentTexture(int attachment, int texture) {
        GlTexture glTexture = GlTexture.getTexture(texture);

        if (glTexture == null) {

            GlEmulationLog.warnOnce("framebuffer.attachTexture.unknown",
                    "glFramebufferTexture2D with unknown texture name; attachment skipped");
            return;
        }

        if (glTexture.vulkanImage == null)
            return;

        switch (attachment) {
            case (GL30.GL_COLOR_ATTACHMENT0) -> this.setColorAttachment(glTexture);

            case (GL30.GL_DEPTH_ATTACHMENT), (GL30.GL_STENCIL_ATTACHMENT), (GL30.GL_DEPTH_STENCIL_ATTACHMENT) -> this.setDepthAttachment(glTexture);

            default -> {
            }
        }
    }

    void setAttachmentRenderbuffer(int attachment, int texture) {
        GlRenderbuffer renderbuffer = GlRenderbuffer.getRenderbuffer(texture);

        if (renderbuffer == null) {

            GlEmulationLog.warnOnce("framebuffer.attachRenderbuffer.unknown",
                    "glFramebufferRenderbuffer with unknown renderbuffer name; attachment skipped");
            return;
        }

        if (renderbuffer.vulkanImage == null)
            return;

        switch (attachment) {
            case (GL30.GL_COLOR_ATTACHMENT0) -> this.setColorAttachment(renderbuffer);

            case (GL30.GL_DEPTH_ATTACHMENT), (GL30.GL_STENCIL_ATTACHMENT), (GL30.GL_DEPTH_STENCIL_ATTACHMENT) -> this.setDepthAttachment(renderbuffer);

            default -> {
            }
        }
    }

    void trackAttachmentTexture(int attachment, int texture, int level) {
        GlTexture next = GlTexture.getTexture(texture);
        GlTexture previous = this.textureAttachments.get(attachment);
        if (previous != next) {
            if (next != null) next.retainFramebufferAttachment();
            this.textureAttachments.put(attachment, next);
            if (previous != null) previous.releaseFramebufferAttachment();
        }
        this.attachments.put(attachment, AttachmentInfo.texture(texture, level));
    }

    private void trackBorrowedTexture(int attachment, int texture) {
        AttachmentInfo previous = this.attachments.get(attachment);
        if (previous == null || previous.objectType != GL11.GL_TEXTURE || previous.objectName != texture || previous.textureLevel != 0
                || this.textureAttachments.get(attachment) != GlTexture.getTexture(texture)) {
            trackAttachmentTexture(attachment, texture, 0);
        }
    }

    void trackAttachmentRenderbuffer(int attachment, int renderbuffer) {
        GlTexture previous = this.textureAttachments.remove(attachment);
        if (previous != null) previous.releaseFramebufferAttachment();
        this.attachments.put(attachment, AttachmentInfo.renderbuffer(renderbuffer));
    }

    void clearAttachment(int attachment) {
        if (!this.attachments.containsKey(attachment)) return;
        if (isRenderableAttachment(attachment)) {
            this.cleanUp();
        }
        this.attachments.remove(attachment);
        if (isRenderableAttachment(attachment)) {
            if (attachment == GL30.GL_COLOR_ATTACHMENT0) this.colorAttachment = null;
            else this.depthAttachment = survivingDepthAttachment();
        }
        GlTexture previous = this.textureAttachments.remove(attachment);
        if (previous != null) previous.releaseFramebufferAttachment();
    }

    private void prepareAttachmentMutation(int attachment) {
        if (!isRenderableAttachment(attachment)) return;
        this.cleanUp();
        if (attachment == GL30.GL_COLOR_ATTACHMENT0) this.colorAttachment = null;
        else this.depthAttachment = null;
    }

    private VulkanImage survivingDepthAttachment() {
        for (int slot : new int[]{GL30.GL_DEPTH_ATTACHMENT, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_STENCIL_ATTACHMENT}) {
            AttachmentInfo info = this.attachments.get(slot);
            if (info == null) continue;
            if (info.objectType == GL11.GL_TEXTURE) {
                GlTexture texture = this.textureAttachments.get(slot);
                if (texture != null && texture.vulkanImage != null) return texture.vulkanImage;
            } else if (info.objectType == GL30.GL_RENDERBUFFER) {
                GlRenderbuffer renderbuffer = GlRenderbuffer.getRenderbuffer(info.objectName);
                if (renderbuffer != null && renderbuffer.vulkanImage != null) return renderbuffer.vulkanImage;
            }
        }
        return null;
    }

    static void textureImageChanged(GlTexture texture, VulkanImage replacement) {
        for (GlFramebuffer target : map.values()) {

            if (target.borrowedPairs != null) continue;
            boolean changed = false;
            for (int attachment : target.textureAttachments.keySet().toIntArray()) {
                if (target.textureAttachments.get(attachment) != texture || !isRenderableAttachment(attachment)) continue;
                if (!changed) target.cleanUp();
                changed = true;
                if (attachment == GL30.GL_COLOR_ATTACHMENT0) target.colorAttachment = replacement;
                else target.depthAttachment = replacement;
            }
        }
    }

    static void textureDeleted(GlTexture texture) {


        for (GlFramebuffer target : map.values()) {
            if (target != boundDrawFramebuffer && target != boundReadFramebuffer) continue;
            for (int attachment : target.textureAttachments.keySet().toIntArray()) {
                if (target.textureAttachments.get(attachment) == texture) target.clearAttachment(attachment);
            }
        }
    }

    private void ensureNativePair() {
        if (this.framebuffer == null && (this.colorAttachment != null || this.depthAttachment != null)) createAndBind();
    }

    int getAttachmentParameter(int attachment, int pname) {
        AttachmentInfo info = this.attachments.get(attachment);
        if (info == null) {
            return pname == GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE ? GL11.GL_NONE : 0;
        }

        return switch (pname) {
            case GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE -> info.objectType;
            case GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME -> info.objectName;
            case GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL -> info.textureLevel;
            case GL30.GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE -> 0;
            default -> 0;
        };
    }

    void setColorAttachment(GlTexture texture) {
        this.colorAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setDepthAttachment(GlTexture texture) {

        this.depthAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setColorAttachment(GlRenderbuffer texture) {
        this.colorAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setDepthAttachment(GlRenderbuffer texture) {

        this.depthAttachment = texture.vulkanImage;
        createAndBind();
    }

    void createAndBind() {

        if (this.colorAttachment == null && this.depthAttachment == null)
            return;

        RenderPass oldRenderPass = this.renderPass;
        boolean wasBound = boundDrawFramebuffer == this && boundDrawId == this.id;
        if (wasBound && oldRenderPass != null && Renderer.getInstance().getBoundRenderPass() == oldRenderPass) {
            endRenderPassPreservingBindings(Renderer.getInstance());
        }

        if (this.framebuffer != null || this.borrowedPairs != null) {
            this.cleanUp();
        }

        if (this.ordinaryPairFactory != null) {
            NativePair pair = this.ordinaryPairFactory.apply(this.colorAttachment, this.depthAttachment);
            this.framebuffer = pair.framebuffer();
            this.renderPass = pair.renderPass();
            return;
        }

        boolean hasDepthImage = this.depthAttachment != null;
        VulkanImage depthImage = this.depthAttachment;

        this.framebuffer = Framebuffer.builder(this.colorAttachment, depthImage).build();
        RenderPass.Builder builder = RenderPass.builder(this.framebuffer);



        if (builder.getColorAttachmentInfo() != null)
            builder.getColorAttachmentInfo()
                    .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                    .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        if (hasDepthImage)
            builder.getDepthAttachmentInfo()
                    .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                    .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        this.renderPass = builder.build();
    }

    public Framebuffer getFramebuffer() {
        ensureNativePair();
        return framebuffer;
    }

    public RenderPass getRenderPass() {
        ensureNativePair();
        return renderPass;
    }

    void cleanUp() {
        Renderer renderer = Renderer.getInstance();
        if (renderer != null && this.renderPass != null && renderer.getBoundRenderPass() == this.renderPass) {
            endRenderPassPreservingBindings(renderer);
        }
        BorrowedFramebufferCache<NativePair> cache = this.borrowedPairs;
        Framebuffer oldFramebuffer = this.framebuffer;
        RenderPass oldRenderPass = this.renderPass;
        this.borrowedPairs = null;
        this.borrowedCapacity = 0;
        this.borrowedCurrentPair = null;
        this.framebuffer = null;
        this.renderPass = null;
        if (cache != null) {
            cache.invalidate();
        } else if (oldFramebuffer != null || oldRenderPass != null) {
            new NativePair(oldFramebuffer, oldRenderPass).cleanUp();
        }
    }

    private static void endRenderPassPreservingBindings(Renderer renderer) {
        BindingSnapshot bindings = captureBindings();
        int logicalWidth = renderer.getViewportLogicalWidth(), logicalHeight = renderer.getViewportLogicalHeight();
        try {
            renderer.endRenderPass();
        } finally {


            bindings.restore();
            Renderer.setViewportScale(logicalWidth, logicalHeight);
        }
    }

    private static NativePair createBorrowedNativePair(VulkanImage color, VulkanImage depth) {
        Framebuffer framebuffer = Framebuffer.builder(color, depth).build();
        try {
            RenderPass.Builder builder = RenderPass.builder(framebuffer);
            builder.getColorAttachmentInfo()
                    .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                    .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            builder.getDepthAttachmentInfo()
                    .setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE)
                    .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            return new NativePair(framebuffer, builder.build());
        } catch (RuntimeException | Error failure) {
            try {
                framebuffer.cleanUp(false);
            } catch (RuntimeException | Error cleanupFailure) {
                appendFailure(failure, cleanupFailure);
            }
            throw failure;
        }
    }

    private static Throwable appendFailure(Throwable first, Throwable later) {
        if (first == null) return later;
        if (first != later) first.addSuppressed(later);
        return first;
    }

    private static void rethrowFailure(Throwable failure) {
        if (failure instanceof RuntimeException runtimeFailure) throw runtimeFailure;
        if (failure instanceof Error errorFailure) throw errorFailure;
    }

    private record AttachmentInfo(int objectType, int objectName, int textureLevel, VulkanImage renderbufferImage) {
        static AttachmentInfo texture(int objectName, int textureLevel) {
            return new AttachmentInfo(GL11.GL_TEXTURE, objectName, textureLevel, null);
        }

        static AttachmentInfo renderbuffer(int objectName) {
            GlRenderbuffer renderbuffer = GlRenderbuffer.getRenderbuffer(objectName);
            return new AttachmentInfo(GL30.GL_RENDERBUFFER, objectName, 0,
                    renderbuffer != null ? renderbuffer.vulkanImage : null);
        }
    }

    record NativePair(Framebuffer framebuffer, RenderPass renderPass) {
        void cleanUp() {
            Throwable firstFailure = null;
            try {
                if (framebuffer != null) framebuffer.cleanUp(false);
            } catch (RuntimeException | Error failure) {
                firstFailure = failure;
            }
            try {
                if (renderPass != null) renderPass.cleanUp();
            } catch (RuntimeException | Error failure) {
                firstFailure = appendFailure(firstFailure, failure);
            }
            rethrowFailure(firstFailure);
        }
    }

    record BlitImages(VulkanImage source, VulkanImage destination, int aspectMask) {
        static BlitImages of(VulkanImage source, VulkanImage destination, int aspectMask) {
            if (source == null || destination == null) {
                return null;
            }

            return new BlitImages(source, destination, aspectMask);
        }
    }

    public record ReadPixelsImage(VulkanImage image, int aspectMask) {
        static ReadPixelsImage of(VulkanImage image, int aspectMask) {
            if (image == null) {
                return null;
            }

            return new ReadPixelsImage(image, aspectMask);
        }
    }
}
