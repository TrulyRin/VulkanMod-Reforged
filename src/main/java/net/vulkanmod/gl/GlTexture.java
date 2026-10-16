package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.compat.gl.GlPixelStore;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GlTexture {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlTexture> map = new Int2ReferenceOpenHashMap<>();
    private static int boundTextureId = 0;
    private static GlTexture boundTexture;
    private static int activeTexture = 0;

    public static void bindIdToImage(int id, VulkanImage vulkanImage) {
        GlTexture texture = getOrCreate(id);
        texture.setVulkanImageReference(vulkanImage);
    }

    private static GlTexture getOrCreate(int id) {
        GlTexture texture = map.get(id);
        if (texture == null) {
            texture = new GlTexture(id);
            map.put(id, texture);
            if (id >= ID_COUNTER)
                ID_COUNTER = id + 1;
        }
        return texture;
    }

    public static int genTextureId() {
        int id = ID_COUNTER;
        map.put(id, new GlTexture(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindTexture(int id) {
        boundTextureId = id;
        boundTexture = map.get(id);

        if (id <= 0)
            return;

        if (boundTexture == null)
            boundTexture = getOrCreate(id);

        VulkanImage vulkanImage = boundTexture.vulkanImage;
        if (vulkanImage != null)
            VTextureSelector.bindTexture(activeTexture, vulkanImage);
    }

    public static void glDeleteTextures(IntBuffer intBuffer) {
        for (int i = intBuffer.position(); i < intBuffer.limit(); i++) {
            glDeleteTextures(intBuffer.get(i));
        }
    }

    public static void glDeleteTextures(int i) {
        GlTexture glTexture = map.remove(i);
        VulkanImage image = glTexture != null ? glTexture.vulkanImage : null;
        if (image != null)
            MemoryManager.getInstance().addToFreeable(image);

        if (glTexture != null && glTexture == boundTexture) {
            boundTexture = null;
            boundTextureId = 0;
        }
    }

    public static GlTexture getTexture(int id) {
        if (id == 0)
            return null;

        return map.get(id);
    }

    public static void activeTexture(int i) {
        activeTexture = i - GL30.GL_TEXTURE0;
        if (Renderer.getInstance() != null) {
            VTextureSelector.setActiveTexture(activeTexture);
        }
    }

    public static int getActiveTexture() {
        return GL30.GL_TEXTURE0 + activeTexture;
    }

    public static int getBoundTextureId(int target) {
        if (target != GL11.GL_TEXTURE_2D) {
            return 0;
        }

        return boundTextureId;
    }

    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, long pixels) {
        if (checkParams(level, width, height))
            return;

        if (boundTexture == null) {
            GlEmulationLog.warnOnce("texImage2D.unbound", "glTexImage2D without a bound texture; upload skipped");
            return;
        }

        boundTexture.allocateIfNeeded(width, height, internalFormat, type);
        VTextureSelector.bindTexture(activeTexture, boundTexture.vulkanImage);

        texSubImage2D(target, level, 0, 0, width, height, format, type, pixels);
    }

    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        if (checkParams(level, width, height))
            return;

        if (boundTexture == null) {
            GlEmulationLog.warnOnce("texImage2D.unbound", "glTexImage2D without a bound texture; upload skipped");
            return;
        }

        boundTexture.allocateIfNeeded(width, height, internalFormat, type);
        VTextureSelector.bindTexture(activeTexture, boundTexture.vulkanImage);

        texSubImage2D(target, level, 0, 0, width, height, format, type, pixels);
    }

    private static boolean checkParams(int level, int width, int height) {
        if (width == 0 || height == 0)
            return true;

        if (level != 0) {

            return true;
        }
        return false;
    }

    public static void texSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, long pixels) {
        if (width == 0 || height == 0)
            return;

        if (boundTexture == null || boundTexture.vulkanImage == null) {
            GlEmulationLog.warnOnce("texSubImage2D.unbound", "glTexSubImage2D without an allocated bound texture; upload skipped");
            return;
        }

        ByteBuffer src;

        GlBuffer glBuffer = GlBuffer.getPixelUnpackBufferBound();
        if (glBuffer != null) {

            glBuffer.data.position((int) pixels);
            src = glBuffer.data;
        } else {
            if (pixels != 0L) {
                src = getByteBuffer(width, height, boundTexture.vulkanImage.formatSize, getUnpackLayout(width), pixels);
            } else {
                src = null;
            }
        }

        if (src != null)
            boundTexture.uploadSubImage(xOffset, yOffset, width, height, format, src, getUnpackLayout(width));
    }

    private static ByteBuffer getByteBuffer(int width, int height, int formatSize, TextureUploadLayout layout, long pixels) {
        ByteBuffer src;
        src = MemoryUtil.memByteBuffer(pixels, layout.requiredByteSize(width, height, formatSize));
        return src;
    }

    public static void texSubImage2D(int target, int level, int xOffset, int yOffset, int width , int height, int format, int type, @Nullable ByteBuffer pixels) {
        if (width == 0 || height == 0)
            return;

        if (boundTexture == null || boundTexture.vulkanImage == null) {
            GlEmulationLog.warnOnce("texSubImage2D.unbound", "glTexSubImage2D without an allocated bound texture; upload skipped");
            return;
        }

        ByteBuffer src;

        GlBuffer glBuffer = GlBuffer.getPixelUnpackBufferBound();
        if (glBuffer != null && pixels == null) {
            glBuffer.data.position(0);
            src = glBuffer.data;
        } else {
            if (glBuffer != null) {
                GlEmulationLog.warnOnce("texSubImage2D.pboConflict",
                        "glTexSubImage2D received a client buffer while a pixel unpack buffer is bound; using the client buffer");
            }
            src = pixels;
        }

        if (src != null)
            boundTexture.uploadSubImage(xOffset, yOffset, width, height, format, src, getUnpackLayout(width));
    }

    public static void compressedTexImage2D(int target, int level, int internalFormat, int width, int height, int border) {
        if (target != GL11.GL_TEXTURE_2D) {
            GlEmulationLog.warnOnce("compressedTexImage2D.target." + target,
                    "glCompressedTexImage2D target 0x{} is not emulated; upload ignored", Integer.toHexString(target));
            return;
        }

        if (checkParams(level, width, height))
            return;

        if (boundTexture == null) {
            GlEmulationLog.warnOnce("compressedTexImage2D.unbound", "glCompressedTexImage2D without a bound texture; upload skipped");
            return;
        }

        boundTexture.setImageMetadata(target, width, height, 1, internalFormat);
        GlEmulationLog.warnOnce("compressedTexImage2D.metadataOnly." + internalFormat,
                "Compressed texture format 0x{} is not decoded yet; recording texture metadata only", Integer.toHexString(internalFormat));
    }

    public static void compressedTexImage2D(int target, int level, int internalFormat, int width, int height, int border, ByteBuffer data) {
        if (target == GL11.GL_TEXTURE_2D && level == 0 && data != null
                && CompressedTextureDecoder.isSupported(internalFormat)) {
            ByteBuffer rgba = null;
            try {
                rgba = CompressedTextureDecoder.decodeToRgba(internalFormat, width, height, data);
                if (rgba != null) {
                    texImage2D(target, level, GL11.GL_RGBA, width, height, border,
                            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, rgba);
                    return;
                }
            } catch (Throwable t) {
                GlEmulationLog.warnOnce("compressedTexImage2D.decodeFailed." + internalFormat,
                        "Compressed texture decode failed for format 0x{}; recording metadata only",
                        Integer.toHexString(internalFormat));
            } finally {
                if (rgba != null) {
                    MemoryUtil.memFree(rgba);
                }
            }
        }

        compressedTexImage2D(target, level, internalFormat, width, height, border);
    }

    public static void texImage3D(int target, int level, int internalFormat, int width, int height, int depth,
                                  int border, int format, int type, @Nullable ByteBuffer data) {
        boolean recorded = recordDimensionalMetadata(target, level, internalFormat, width, height, depth, border, format, type);
        if (recorded && data != null) {
            GlEmulationLog.warnContractGap("texture_image", "glTexImage3D.data",
                    "glTexImage3D data upload is not allocated yet; recorded dimensional metadata for target 0x{}",
                    Integer.toHexString(target));
        }
    }

    private static boolean recordDimensionalMetadata(int target, int level, int internalFormat, int width, int height,
                                                  int depth, int border, int format, int type) {
        if (!isMetadataTextureTarget(target)) {
            GlEmulationLog.warnContractGap("texture_image", "glTexImage3D.target",
                    "glTexImage3D target 0x{} is not emulated; upload ignored", Integer.toHexString(target));
            return false;
        }
        texImage3D(target, level, internalFormat, width, height, depth, border, format, type);
        return true;
    }

    public static void texImage3D(int target, int level, int internalFormat, int width, int height, int depth, int border, int format, int type) {
        if (!isMetadataTextureTarget(target)) {
            GlEmulationLog.warnOnce("texImage3D.target." + target,
                    "glTexImage3D target 0x{} is not emulated; upload ignored", Integer.toHexString(target));
            return;
        }

        if (checkParams(level, width, height) || depth == 0)
            return;

        if (boundTexture == null) {
            GlEmulationLog.warnOnce("texImage3D.unbound", "glTexImage3D without a bound texture; upload skipped");
            return;
        }

        boundTexture.setImageMetadata(target, width, height, depth, internalFormat);
        GlEmulationLog.warnOnce("texImage3D.metadataOnly." + internalFormat,
                "3D/array texture target 0x{} is not allocated yet; recording texture metadata only", Integer.toHexString(target));
    }

    public static void texImage1D(int target, int level, int internalFormat, int width, int border, int format, int type) {
        if (target != GL11.GL_TEXTURE_1D) {
            GlEmulationLog.warnOnce("texImage1D.target." + target,
                    "glTexImage1D target 0x{} is not emulated; upload ignored", Integer.toHexString(target));
            return;
        }

        if (checkParams(level, width, 1))
            return;

        if (boundTexture == null) {
            GlEmulationLog.warnOnce("texImage1D.unbound", "glTexImage1D without a bound texture; upload skipped");
            return;
        }

        boundTexture.setImageMetadata(target, width, 1, 1, internalFormat);
        GlEmulationLog.warnOnce("texImage1D.metadataOnly." + internalFormat,
                "1D texture target is not allocated yet; recording texture metadata only");
    }

    public static void texParameteri(int target, int pName, int param) {
        if (target != GL11.GL_TEXTURE_2D) {
            GlEmulationLog.warnOnce("texParameteri.target." + target,
                    "glTexParameteri target 0x{} is not emulated; parameter ignored", Integer.toHexString(target));
            return;
        }

        if (boundTexture == null)
            return;

        switch (pName) {
            case GL30.GL_TEXTURE_MAX_LEVEL -> boundTexture.setMaxLevel(param);
            case GL30.GL_TEXTURE_MAX_LOD -> boundTexture.setMaxLod(param);
            case GL30.GL_TEXTURE_MIN_LOD -> {}
            case GL30.GL_TEXTURE_LOD_BIAS -> {}

            case GL11.GL_TEXTURE_MAG_FILTER -> boundTexture.setMagFilter(param);
            case GL11.GL_TEXTURE_MIN_FILTER -> boundTexture.setMinFilter(param);

            case GL11.GL_TEXTURE_WRAP_S -> boundTexture.setWrapS(param);
            case GL11.GL_TEXTURE_WRAP_T -> boundTexture.setWrapT(param);

            default -> {}
        }

    }

    public static int getTexParameteri(int target, int pName) {
        if (target != GL11.GL_TEXTURE_2D || boundTexture == null)
            return 0;

        return boundTexture.getTexParameter(pName);
    }

    public static float getTexParameterf(int target, int pName) {
        return getTexParameteri(target, pName);
    }

    public static int getTexLevelParameter(int target, int level, int pName) {
        if (target != GL11.GL_TEXTURE_2D && (boundTexture == null || !boundTexture.hasMetadataForTarget(target))) {
            GlEmulationLog.warnOnce("getTexLevelParameter.target." + target,
                    "glGetTexLevelParameter target 0x{} is not emulated; returning -1", Integer.toHexString(target));
            return -1;
        }

        if (boundTexture == null || boundTexture.vulkanImage == null)
            return boundTexture != null ? boundTexture.getTexLevelParameterFromMetadata(pName) : -1;

        return switch (pName) {
            case GL11.GL_TEXTURE_INTERNAL_FORMAT -> GlUtil.getGlFormat(boundTexture.vulkanImage.format);
            case GL11.GL_TEXTURE_WIDTH -> boundTexture.vulkanImage.width;
            case GL11.GL_TEXTURE_HEIGHT -> boundTexture.vulkanImage.height;

            default -> -1;
        };
    }

    public static void generateMipmap(int target) {
        if (target != GL11.GL_TEXTURE_2D) {
            GlEmulationLog.warnOnce("generateMipmap.target." + target,
                    "glGenerateMipmap target 0x{} is not emulated; call ignored", Integer.toHexString(target));
            return;
        }

        if (boundTexture == null || boundTexture.vulkanImage == null)
            return;

        boundTexture.generateMipmaps();
    }

    public static void getTexImage(int tex, int level, int format, int type, long pixels) {
        if (boundTexture == null || boundTexture.vulkanImage == null) {
            GlEmulationLog.warnOnce("getTexImage.unbound", "glGetTexImage without an allocated bound texture; readback skipped");
            return;
        }

        VulkanImage image = boundTexture.vulkanImage;

        GlBuffer buffer = GlBuffer.getPixelPackBufferBound();
        long ptr;
        if (buffer != null) {

            if (buffer.data == null) {
                GlEmulationLog.warnOnce("getTexImage.emptyPbo", "glGetTexImage into an unallocated pixel pack buffer; readback skipped");
                return;
            }

            buffer.data.position((int) pixels);

            ptr = MemoryUtil.memAddress(buffer.data);
        } else {
            ptr = pixels;
        }

        ImageUtil.downloadTexture(image, ptr);
    }

    public static void setVulkanImage(int id, VulkanImage vulkanImage) {
        GlTexture texture = getOrCreate(id);

        texture.setVulkanImageReference(vulkanImage);
    }

    public static GlTexture getBoundTexture() {
        return boundTexture;
    }

    public static void transitionReadOnly() {
        if (boundTexture != null) {
            boundTexture.transitionReadOnlyImage();
        }
    }

    public static void transitionReadOnly(int id) {
        GlTexture texture = map.get(id);
        if (texture != null) {
            texture.transitionReadOnlyImage();
        }
    }

    final int id;
    VulkanImage vulkanImage;
    int internalFormat;
    int target = GL11.GL_TEXTURE_2D;
    int width;
    int height;
    int depth = 1;
    boolean hasImageMetadata;

    boolean needsUpdate = false;
    int maxLevel = 0;
    int maxLod = 0;
    int minFilter = GL11.GL_LINEAR;
    int magFilter = GL11.GL_LINEAR;

    boolean clamp = true;
    int wrapS = GL30.GL_CLAMP_TO_EDGE;
    int wrapT = GL30.GL_CLAMP_TO_EDGE;

    public GlTexture(int id) {
        this.id = id;
    }

    private void transitionReadOnlyImage() {
        if (this.vulkanImage == null) {
            return;
        }

        Renderer renderer = Renderer.getInstance();
        if (renderer != null && Renderer.isRecording()) {
            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                this.vulkanImage.readOnlyLayout(stack, Renderer.getCommandBuffer());
            }
        } else {
            this.vulkanImage.readOnlyLayout();
        }
    }

    void allocateIfNeeded(int width, int height, int internalFormat, int type) {
        this.internalFormat = internalFormat;
        setImageMetadata(GL11.GL_TEXTURE_2D, width, height, 1, internalFormat);
        int vkFormat = GlUtil.vulkanFormat(internalFormat, type);

        needsUpdate |= vulkanImage == null ||
                vulkanImage.width != width || vulkanImage.height != height ||
                vkFormat != vulkanImage.format;

        if (needsUpdate) {
            allocateImage(width, height, vkFormat);
            updateSampler();

            needsUpdate = false;
        }
    }

    void setImageMetadata(int target, int width, int height, int depth, int internalFormat) {
        this.target = target;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.internalFormat = internalFormat;
        this.hasImageMetadata = true;
    }

    private void setVulkanImageReference(VulkanImage vulkanImage) {
        this.vulkanImage = vulkanImage;

        if (vulkanImage == null) {
            this.width = 0;
            this.height = 0;
            this.depth = 1;
            this.internalFormat = 0;
            this.hasImageMetadata = false;
            return;
        }

        setImageMetadata(GL11.GL_TEXTURE_2D, vulkanImage.width, vulkanImage.height, 1,
                GlUtil.getGlFormat(vulkanImage.format));
    }

    private boolean hasMetadataForTarget(int target) {
        return hasImageMetadata && this.target == target;
    }

    private int getTexLevelParameterFromMetadata(int pName) {
        if (!hasImageMetadata)
            return -1;

        return switch (pName) {
            case GL11.GL_TEXTURE_INTERNAL_FORMAT -> internalFormat;
            case GL11.GL_TEXTURE_WIDTH -> width;
            case GL11.GL_TEXTURE_HEIGHT -> height;
            case GL12.GL_TEXTURE_DEPTH -> depth;
            default -> -1;
        };
    }

    private int getTexParameter(int pName) {
        return switch (pName) {
            case GL11.GL_TEXTURE_MAG_FILTER -> magFilter;
            case GL11.GL_TEXTURE_MIN_FILTER -> minFilter;
            case GL11.GL_TEXTURE_WRAP_S -> wrapS;
            case GL11.GL_TEXTURE_WRAP_T -> wrapT;
            case GL30.GL_TEXTURE_MAX_LEVEL -> maxLevel;
            case GL30.GL_TEXTURE_MAX_LOD -> maxLod;
            case GL30.GL_TEXTURE_MIN_LOD, GL30.GL_TEXTURE_LOD_BIAS -> 0;
            default -> 0;
        };
    }

    private static boolean isMetadataTextureTarget(int target) {
        return target == GL11.GL_TEXTURE_1D || target == GL12.GL_TEXTURE_3D || target == GL30.GL_TEXTURE_2D_ARRAY;
    }

    void allocateImage(int width, int height, int vkFormat) {
        if (this.vulkanImage != null)
            this.vulkanImage.free();

        if (VulkanImage.isDepthFormat(vkFormat))
            this.vulkanImage = VulkanImage.createDepthImage(vkFormat,
                    width, height,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
                    false, true);
        else
            this.vulkanImage = new VulkanImage.Builder(width, height)
                    .setMipLevels(maxLevel + 1)
                    .setFormat(vkFormat)
                    .addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .createVulkanImage();
    }

    void updateSampler() {
        if (vulkanImage == null)
            return;

        byte samplerFlags;
        samplerFlags = clamp ? SamplerManager.CLAMP_BIT : 0;
        samplerFlags |= magFilter == GL11.GL_LINEAR ? SamplerManager.LINEAR_FILTERING_BIT : 0;

        samplerFlags |= switch (minFilter) {
            case GL11.GL_LINEAR_MIPMAP_LINEAR -> SamplerManager.USE_MIPMAPS_BIT | SamplerManager.MIPMAP_LINEAR_FILTERING_BIT;
            case GL11.GL_NEAREST_MIPMAP_NEAREST -> SamplerManager.USE_MIPMAPS_BIT;
            default -> 0;
        };

        vulkanImage.updateTextureSampler(maxLod, samplerFlags);
    }

    private void uploadSubImage(int xOffset, int yOffset, int width, int height, int format, ByteBuffer pixels, TextureUploadLayout layout) {
        boolean rgba8Image = vulkanImage.format == VK_FORMAT_R8G8B8A8_UNORM || vulkanImage.format == VK_FORMAT_R8G8B8A8_SRGB;

        ByteBuffer src;
        if (format == GL11.GL_RGB && rgba8Image) {
            src = GlUtil.RGBtoRGBA_buffer(pixels);
        } else if (format == GL30.GL_BGRA && rgba8Image) {
            src = GlUtil.BGRAtoRGBA_buffer(pixels);
        } else {
            src = pixels;
        }

        this.vulkanImage.uploadSubTextureAsync(0, width, height, xOffset, yOffset,
                layout.skipRows(), layout.skipPixels(), layout.rowLength(), src);

        if (src != pixels) {
            MemoryUtil.memFree(src);
        }
    }

    static TextureUploadLayout getUnpackLayout(int width) {
        int rowLength = GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_ROW_LENGTH);
        if (rowLength <= 0) {
            rowLength = width;
        }

        return new TextureUploadLayout(
                rowLength,
                GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_SKIP_ROWS),
                GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_SKIP_PIXELS));
    }

    record TextureUploadLayout(int rowLength, int skipRows, int skipPixels) {
        int sourceByteOffset(int formatSize) {
            return (rowLength * skipRows + skipPixels) * formatSize;
        }

        int requiredByteSize(int width, int height, int formatSize) {
            if (height <= 0) {
                return 0;
            }
            return ((skipRows + height - 1) * rowLength + skipPixels + width) * formatSize;
        }
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
                GlEmulationLog.warnOnce("texture.magFilter." + v, "Unsupported mag filter 0x{}; keeping previous value", Integer.toHexString(v));
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
                GlEmulationLog.warnOnce("texture.minFilter." + v, "Unsupported min filter 0x{}; keeping previous value", Integer.toHexString(v));
                return;
            }
        }

        this.minFilter = v;
        updateSampler();
    }

    void setWrapS(int v) {
        wrapS = v;
        updateClampMode();
    }

    void setWrapT(int v) {
        wrapT = v;
        updateClampMode();
    }

    private void updateClampMode() {
        this.clamp = wrapS == GL30.GL_CLAMP_TO_EDGE && wrapT == GL30.GL_CLAMP_TO_EDGE;

        updateSampler();
    }

    public VulkanImage getVulkanImage() {
        return vulkanImage;
    }

    public void setVulkanImage(VulkanImage vulkanImage) {
        setVulkanImageReference(vulkanImage);
    }

}

