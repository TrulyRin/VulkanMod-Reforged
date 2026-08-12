package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GlRenderbuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlRenderbuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlRenderbuffer bound;

    public static int genId() {
        int id = ID_COUNTER;
        map.put(id, new GlRenderbuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindRenderbuffer(int target, int id) {
        boundId = id;
        bound = map.get(id);

        if (id <= 0)
            return;

        if (bound == null) {
            bound = new GlRenderbuffer(id);
            map.put(id, bound);
            if (id >= ID_COUNTER)
                ID_COUNTER = id + 1;
        }

        VulkanImage vulkanImage = bound.vulkanImage;
        if (vulkanImage != null)
            VTextureSelector.bindTexture(vulkanImage);
    }

    public static void deleteRenderbuffer(int i) {
        map.remove(i);
    }

    public static GlRenderbuffer getRenderbuffer(int id) {
        return map.get(id);
    }

    public static void renderbufferStorage(int target, int internalFormat, int width, int height) {
        renderbufferStorage(target, 0, internalFormat, width, height);
    }

    public static void renderbufferStorageMultisample(int target, int samples, int internalFormat, int width, int height) {
        renderbufferStorage(target, Math.max(samples, 0), internalFormat, width, height);
    }

    private static void renderbufferStorage(int target, int samples, int internalFormat, int width, int height) {
        if (width == 0 || height == 0)
            return;

        if (bound == null) {
            GlEmulationLog.warnOnce("renderbufferStorage.unbound", "glRenderbufferStorage without a bound renderbuffer; allocation skipped");
            return;
        }

        bound.internalFormat = internalFormat;
        bound.samples = samples;

        bound.allocateIfNeeded(width, height, internalFormat);
    }

    public static void namedRenderbufferStorage(int renderbuffer, int internalFormat, int width, int height) {
        int previous = boundId;
        bindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
        try {
            renderbufferStorage(GL30.GL_RENDERBUFFER, internalFormat, width, height);
        } finally {
            bindRenderbuffer(GL30.GL_RENDERBUFFER, previous);
        }
    }

    public static void namedRenderbufferStorageMultisample(int renderbuffer, int samples, int internalFormat, int width, int height) {
        int previous = boundId;
        bindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
        try {
            renderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, internalFormat, width, height);
        } finally {
            bindRenderbuffer(GL30.GL_RENDERBUFFER, previous);
        }
    }

    public static int getRenderbufferParameter(int target, int pname) {
        if (target != GL30.GL_RENDERBUFFER) {
            return 0;
        }

        return getRenderbufferParameter(bound, pname);
    }

    public static int getNamedRenderbufferParameter(int renderbuffer, int pname) {
        return getRenderbufferParameter(map.get(renderbuffer), pname);
    }

    private static int getRenderbufferParameter(GlRenderbuffer renderbuffer, int pname) {
        if (renderbuffer == null || renderbuffer.vulkanImage == null) {
            return 0;
        }

        return switch (pname) {
            case GL30.GL_RENDERBUFFER_WIDTH -> renderbuffer.vulkanImage.width;
            case GL30.GL_RENDERBUFFER_HEIGHT -> renderbuffer.vulkanImage.height;
            case GL30.GL_RENDERBUFFER_INTERNAL_FORMAT -> renderbuffer.internalFormat;
            case GL30.GL_RENDERBUFFER_RED_SIZE -> colorBits(renderbuffer.internalFormat, 0);
            case GL30.GL_RENDERBUFFER_GREEN_SIZE -> colorBits(renderbuffer.internalFormat, 1);
            case GL30.GL_RENDERBUFFER_BLUE_SIZE -> colorBits(renderbuffer.internalFormat, 2);
            case GL30.GL_RENDERBUFFER_ALPHA_SIZE -> colorBits(renderbuffer.internalFormat, 3);
            case GL30.GL_RENDERBUFFER_DEPTH_SIZE -> depthBits(renderbuffer.internalFormat);
            case GL30.GL_RENDERBUFFER_STENCIL_SIZE -> stencilBits(renderbuffer.internalFormat);
            case GL30.GL_RENDERBUFFER_SAMPLES -> renderbuffer.samples;
            default -> 0;
        };
    }

    private static int colorBits(int internalFormat, int component) {
        return switch (internalFormat) {
            case GL11.GL_RGBA, GL30.GL_RGBA8, GL21.GL_SRGB8_ALPHA8, GL30.GL_BGRA -> 8;
            case GL11.GL_RGB, GL30.GL_RGB8, GL21.GL_SRGB8 -> component < 3 ? 8 : 0;
            case GL11.GL_RED, GL30.GL_R8 -> component == 0 ? 8 : 0;
            case GL30.GL_RG, GL30.GL_RG8 -> component < 2 ? 8 : 0;
            case GL30.GL_R16, GL30.GL_R16F -> component == 0 ? 16 : 0;
            case GL30.GL_RG16, GL30.GL_RG16F -> component < 2 ? 16 : 0;
            case GL30.GL_RGB16F -> component < 3 ? 16 : 0;
            case GL30.GL_RGBA16F -> 16;
            case GL30.GL_R32F -> component == 0 ? 32 : 0;
            case GL30.GL_RG32F -> component < 2 ? 32 : 0;
            case GL30.GL_RGBA32F -> 32;
            case GL30.GL_R11F_G11F_B10F -> component == 0 || component == 1 ? 11 : component == 2 ? 10 : 0;
            case GL11.GL_RGB10_A2 -> component < 3 ? 10 : 2;
            default -> 0;
        };
    }

    private static int depthBits(int internalFormat) {
        return switch (internalFormat) {
            case GL14.GL_DEPTH_COMPONENT16 -> 16;
            case GL30.GL_DEPTH_COMPONENT24, GL30.GL_DEPTH24_STENCIL8 -> 24;
            case GL11.GL_DEPTH_COMPONENT, GL14.GL_DEPTH_COMPONENT32, GL30.GL_DEPTH_COMPONENT32F,
                    GL30.GL_DEPTH32F_STENCIL8 -> 32;
            default -> 0;
        };
    }

    private static int stencilBits(int internalFormat) {
        return switch (internalFormat) {
            case GL30.GL_STENCIL_INDEX8, GL30.GL_DEPTH24_STENCIL8, GL30.GL_DEPTH32F_STENCIL8 -> 8;
            default -> 0;
        };
    }

    public static void texParameteri(int target, int pName, int param) {
        if (target != GL11.GL_TEXTURE_2D) {
            GlEmulationLog.warnOnce("renderbuffer.texParameteri.target." + target,
                    "Renderbuffer texParameteri target 0x{} is not emulated; parameter ignored", Integer.toHexString(target));
            return;
        }

        if (bound == null)
            return;

        switch (pName) {
            case GL30.GL_TEXTURE_MAX_LEVEL -> bound.setMaxLevel(param);
            case GL30.GL_TEXTURE_MAX_LOD -> bound.setMaxLod(param);
            case GL30.GL_TEXTURE_MIN_LOD -> {}
            case GL30.GL_TEXTURE_LOD_BIAS -> {}

            case GL11.GL_TEXTURE_MAG_FILTER -> bound.setMagFilter(param);
            case GL11.GL_TEXTURE_MIN_FILTER -> bound.setMinFilter(param);

            default -> {
            }
        }

    }

    public static int getTexLevelParameter(int target, int level, int pName) {
        if (bound == null || target == GL11.GL_TEXTURE_2D)
            return -1;

        return switch (pName) {
            case GL11.GL_TEXTURE_INTERNAL_FORMAT -> GlUtil.getGlFormat(bound.vulkanImage.format);
            case GL11.GL_TEXTURE_WIDTH -> bound.vulkanImage.width;
            case GL11.GL_TEXTURE_HEIGHT -> bound.vulkanImage.height;

            default -> -1;
        };
    }

    public static void generateMipmap(int target) {
        if (target != GL11.GL_TEXTURE_2D) {
            GlEmulationLog.warnOnce("renderbuffer.generateMipmap.target." + target,
                    "Renderbuffer generateMipmap target 0x{} is not emulated; call ignored", Integer.toHexString(target));
            return;
        }

        if (bound == null || bound.vulkanImage == null)
            return;

        bound.generateMipmaps();
    }

    public static void setVulkanImage(int id, VulkanImage vulkanImage) {
        GlRenderbuffer texture = map.get(id);
        if (texture == null) {
            texture = new GlRenderbuffer(id);
            map.put(id, texture);
            if (id >= ID_COUNTER)
                ID_COUNTER = id + 1;
        }

        texture.vulkanImage = vulkanImage;
    }

    public static GlRenderbuffer getBound() {
        return bound;
    }

    public static int getBoundId() {
        return boundId;
    }

    final int id;
    VulkanImage vulkanImage;
    int internalFormat;
    int samples;

    boolean needsUpdate = false;
    int maxLevel = 0;
    int maxLod = 0;
    int minFilter, magFilter = GL11.GL_LINEAR;

    public GlRenderbuffer(int id) {
        this.id = id;
    }

    void allocateIfNeeded(int width, int height, int format) {
        int vkFormat = GlUtil.vulkanFormat(format);

        needsUpdate |= vulkanImage == null ||
                vulkanImage.width != width || vulkanImage.height != height ||
                vkFormat != vulkanImage.format;

        if (needsUpdate) {
            allocateImage(width, height, vkFormat);
            updateSampler();

            needsUpdate = false;
        }
    }

    void allocateImage(int width, int height, int vkFormat) {
        if (this.vulkanImage != null)
            this.vulkanImage.free();

        if (VulkanImage.isDepthFormat(vkFormat))
            this.vulkanImage = VulkanImage.createDepthImage(vkFormat,
                    width, height,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
                    false, true);
        else
            this.vulkanImage = new VulkanImage.Builder(width, height)
                    .setMipLevels(maxLevel + 1)
                    .setFormat(vkFormat)
                    .addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .createVulkanImage();

        VTextureSelector.bindTexture(this.vulkanImage);
    }

    void updateSampler() {
        if (vulkanImage == null)
            return;

        byte samplerFlags = magFilter == GL11.GL_LINEAR ? SamplerManager.LINEAR_FILTERING_BIT : 0;

        samplerFlags |= switch (minFilter) {
            case GL11.GL_LINEAR_MIPMAP_LINEAR ->
                    SamplerManager.USE_MIPMAPS_BIT | SamplerManager.MIPMAP_LINEAR_FILTERING_BIT;
            case GL11.GL_NEAREST_MIPMAP_NEAREST -> SamplerManager.USE_MIPMAPS_BIT;
            default -> 0;
        };

        vulkanImage.updateTextureSampler(maxLod, samplerFlags);
    }

    private void uploadImage(ByteBuffer pixels) {
        int width = this.vulkanImage.width;
        int height = this.vulkanImage.height;

        if (internalFormat == GL11.GL_RGB && vulkanImage.format == VK_FORMAT_R8G8B8A8_UNORM) {
            ByteBuffer RGBA_buffer = GlUtil.RGBtoRGBA_buffer(pixels);
            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, RGBA_buffer);
            MemoryUtil.memFree(RGBA_buffer);
        } else
            this.vulkanImage.uploadSubTextureAsync(0, width, height, 0, 0, 0, 0, 0, pixels);

    }

    void generateMipmaps() {

        ImageUtil.generateMipmaps(vulkanImage);
    }

    void setMaxLevel(int l) {
        l = Math.max(l, 0);

        if (maxLevel != l) {
            maxLevel = l;
            needsUpdate = true;
        }
    }

    void setMaxLod(int l) {
        l = Math.max(l, 0);

        if (maxLod != l) {
            maxLod = l;
            updateSampler();
        }
    }

    void setMagFilter(int v) {
        switch (v) {
            case GL11.GL_LINEAR, GL11.GL_NEAREST -> {
            }

            default -> {
                GlEmulationLog.warnOnce("renderbuffer.magFilter." + v, "Unsupported mag filter 0x{}; keeping previous value", Integer.toHexString(v));
                return;
            }
        }

        this.magFilter = v;
        updateSampler();
    }

    void setMinFilter(int v) {
        switch (v) {
            case GL11.GL_LINEAR, GL11.GL_NEAREST,
                 GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_NEAREST_MIPMAP_LINEAR,
                 GL11.GL_LINEAR_MIPMAP_NEAREST, GL11.GL_NEAREST_MIPMAP_NEAREST -> {
            }

            default -> {
                GlEmulationLog.warnOnce("renderbuffer.minFilter." + v, "Unsupported min filter 0x{}; keeping previous value", Integer.toHexString(v));
                return;
            }
        }

        this.minFilter = v;
        updateSampler();
    }

    public VulkanImage getVulkanImage() {
        return vulkanImage;
    }

    public void setVulkanImage(VulkanImage vulkanImage) {
        this.vulkanImage = vulkanImage;
    }
}
