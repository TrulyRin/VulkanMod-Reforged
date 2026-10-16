package net.vulkanmod.gl;

import com.mojang.blaze3d.pipeline.RenderTarget;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.vulkan.VK11.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK11.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

public class GlFramebuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlFramebuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlFramebuffer boundFramebuffer;

    public static void resetBoundFramebuffer() {
        boundFramebuffer = null;
        boundId = 0;
    }

    public static void beginRendering(GlFramebuffer glFramebuffer) {
        Renderer.getInstance().beginRendering(glFramebuffer.renderPass, glFramebuffer.framebuffer);

        Framebuffer framebuffer = glFramebuffer.framebuffer;
        int viewWidth = framebuffer.getWidth();
        int viewHeight = framebuffer.getHeight();

        Renderer.setInvertedViewport(0, 0, viewWidth, viewHeight);
        Renderer.setScissor(0, 0, viewWidth, viewHeight);

        VRenderSystem.disableCull();

        boundFramebuffer = glFramebuffer;
        boundId = glFramebuffer.id;
    }

    public static int genFramebufferId() {
        int id = ID_COUNTER;
        map.put(id, new GlFramebuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindFramebuffer(int target, int id) {

        if (id == 0) {
            Renderer renderer = Renderer.getInstance();
            if (renderer == null) {
                boundFramebuffer = null;
                boundId = 0;
                return;
            }

            renderer.endRenderPass();

            if (Renderer.isRecording()) {
                RenderTarget renderTarget = Minecraft.getInstance().getMainRenderTarget();
                renderTarget.bindWrite(true);
            }

            boundFramebuffer = null;
            boundId = 0;
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

        if (boundId == id && boundFramebuffer == glFramebuffer) {
            if (glFramebuffer.framebuffer != null
                    && Renderer.getInstance().getBoundRenderPass() != glFramebuffer.renderPass) {
                beginRendering(glFramebuffer);
            }

            return;
        }

        boundFramebuffer = glFramebuffer;
        boundId = id;

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

        framebuffer.cleanUp();

        if (boundFramebuffer == framebuffer) {
            boundFramebuffer = null;
            boundId = 0;
        }
    }

    public static void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
        if (texTarget != GL11.GL_TEXTURE_2D) {
            return;
        }
        if (level != 0) {
            return;
        }

        if (boundFramebuffer == null)
            return;

        if (texture == 0) {
            boundFramebuffer.clearAttachment(attachment);
            return;
        }

        boundFramebuffer.trackAttachmentTexture(attachment, texture, level);

        if (!isRenderableAttachment(attachment)) {
            return;
        }

        boundFramebuffer.setAttachmentTexture(attachment, texture);
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
        if (boundFramebuffer == null)
            return;

        if (renderbuffer == 0) {
            boundFramebuffer.clearAttachment(attachment);
            return;
        }

        boundFramebuffer.trackAttachmentRenderbuffer(attachment, renderbuffer);

        if (!isRenderableAttachment(attachment)) {
            return;
        }

        boundFramebuffer.setAttachmentRenderbuffer(attachment, renderbuffer);
    }

    public static int glCheckFramebufferStatus(int target) {

        return GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    public static int getFramebufferAttachmentParameteri(int target, int attachment, int pname) {
        if (boundFramebuffer == null) {
            return pname == GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE ? GL11.GL_NONE : 0;
        }

        return boundFramebuffer.getAttachmentParameter(attachment, pname);
    }

    public static void namedFramebufferTexture(int framebuffer, int attachment, int texture, int level) {
        int previous = getBoundId();
        bindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        try {
            framebufferTexture2D(GL30.GL_FRAMEBUFFER, attachment, GL11.GL_TEXTURE_2D, texture, level);
        } finally {
            bindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
        }
    }

    public static void namedFramebufferRenderbuffer(int framebuffer, int attachment, int renderbuffertarget, int renderbuffer) {
        int previous = getBoundId();
        bindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        try {
            framebufferRenderbuffer(GL30.GL_FRAMEBUFFER, attachment, renderbuffertarget, renderbuffer);
        } finally {
            bindFramebuffer(GL30.GL_FRAMEBUFFER, previous);
        }
    }

    public static GlFramebuffer getBoundFramebuffer() {
        return boundFramebuffer;
    }

    public static int getBoundId() {
        return boundId;
    }

    public static GlFramebuffer getFramebuffer(int id) {
        return map.get(id);
    }

    private final int id;
    Framebuffer framebuffer;
    RenderPass renderPass;

    VulkanImage colorAttachment;
    VulkanImage depthAttachment;
    private final Int2ReferenceOpenHashMap<AttachmentInfo> attachments = new Int2ReferenceOpenHashMap<>();

    GlFramebuffer(int i) {
        this.id = i;
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
        this.attachments.put(attachment, AttachmentInfo.texture(texture, level));
    }

    void trackAttachmentRenderbuffer(int attachment, int renderbuffer) {
        this.attachments.put(attachment, AttachmentInfo.renderbuffer(renderbuffer));
    }

    void clearAttachment(int attachment) {
        this.attachments.remove(attachment);
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

        if (this.colorAttachment == null)
            return;

        RenderPass oldRenderPass = this.renderPass;
        boolean wasBound = boundFramebuffer == this && boundId == this.id;
        if (wasBound && oldRenderPass != null && Renderer.getInstance().getBoundRenderPass() == oldRenderPass) {
            Renderer.getInstance().endRenderPass();
            boundFramebuffer = this;
            boundId = this.id;
        }

        if (this.framebuffer != null) {
            this.cleanUp();
        }

        boolean hasDepthImage = this.depthAttachment != null;
        VulkanImage depthImage = this.depthAttachment;

        this.framebuffer = Framebuffer.builder(this.colorAttachment, depthImage).build();
        RenderPass.Builder builder = RenderPass.builder(this.framebuffer);

        builder.getColorAttachmentInfo()
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        if (hasDepthImage)
            builder.getDepthAttachmentInfo()
                    .setOps(VK_ATTACHMENT_LOAD_OP_CLEAR, VK_ATTACHMENT_STORE_OP_STORE)
                    .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        this.renderPass = builder.build();
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public RenderPass getRenderPass() {
        return renderPass;
    }

    void cleanUp() {
        if (this.framebuffer != null) {
            this.framebuffer.cleanUp(false);
        }

        if (this.renderPass != null) {
            this.renderPass.cleanUp();
        }

        this.framebuffer = null;
        this.renderPass = null;
    }

    private record AttachmentInfo(int objectType, int objectName, int textureLevel) {
        static AttachmentInfo texture(int objectName, int textureLevel) {
            return new AttachmentInfo(GL11.GL_TEXTURE, objectName, textureLevel);
        }

        static AttachmentInfo renderbuffer(int objectName) {
            return new AttachmentInfo(GL30.GL_RENDERBUFFER, objectName, 0);
        }
    }
}

