package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.compat.gl.GlPixelStore;
import net.vulkanmod.compat.gl.TextureUnitLimits;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GlTexture {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlTexture> map = new Int2ReferenceOpenHashMap<>();
    private static final GlTexture[] boundByUnit = new GlTexture[TextureUnitLimits.COMBINED];
    private static final Int2ReferenceOpenHashMap<ProxyImage> proxyImages2D = new Int2ReferenceOpenHashMap<>();
    private static int boundTextureId = 0;
    private static GlTexture boundTexture;
    private static int activeTexture = 0;


    public static BindingSnapshot captureBindings(long unitMask) {
        long validMask=(1L<<TextureUnitLimits.COMBINED)-1L;
        if((unitMask&~validMask)!=0L) throw new IllegalArgumentException("Texture binding mask exceeds available units");
        GlTexture[] bindings=new GlTexture[Long.bitCount(unitMask)];
        long remaining=unitMask;
        for(int index=0;remaining!=0L;index++) {
            int unit=Long.numberOfTrailingZeros(remaining);
            bindings[index]=boundByUnit[unit];
            remaining&=remaining-1L;
        }
        return new BindingSnapshot(unitMask,bindings);
    }

    public static final class BindingSnapshot {
        private final long unitMask;
        private final GlTexture[] bindings;
        private boolean restored;

        private BindingSnapshot(long unitMask,GlTexture[] bindings) {
            this.unitMask=unitMask; this.bindings=bindings;
        }


        public void restore() {
            if(restored) return;
            restored=true;
            long remaining=unitMask;
            if(remaining==0L) return;
            for(int index=0;remaining!=0L;index++) {
                int unit=Long.numberOfTrailingZeros(remaining);
                GlTexture texture=bindings[index];

                if(texture!=null && map.get(texture.id)!=texture) texture=null;
                boundByUnit[unit]=texture;
                VTextureSelector.bindGlTexture(unit,texture==null?0:texture.id,texture==null?null:texture.vulkanImage);
                remaining&=remaining-1L;
            }
            boundTexture=boundByUnit[activeTexture];
            boundTextureId=boundTexture==null?0:boundTexture.id;
        }
    }

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

        if (id > 0 && boundTexture == null)
            boundTexture = getOrCreate(id);

        boundByUnit[activeTexture] = boundTexture;
        VTextureSelector.bindGlTexture(activeTexture, id,
                boundTexture != null ? boundTexture.vulkanImage : null);
    }

    public static void bindTextureUnit(int unit, int id) {
        if (!TextureUnitLimits.isValid(unit)) return;
        int previous = getActiveTexture();
        activeTexture(GL30.GL_TEXTURE0 + unit);
        try {
            bindTexture(id);
        } finally {
            activeTexture(previous);
        }
    }

    public static void glDeleteTextures(IntBuffer intBuffer) {
        for (int i = intBuffer.position(); i < intBuffer.limit(); i++) {
            glDeleteTextures(intBuffer.get(i));
        }
    }

    public static void glDeleteTextures(int i) {
        GlTexture glTexture = map.remove(i);
        if (glTexture == null) return;
        glTexture.deleted = true;
        GlFramebuffer.textureDeleted(glTexture);
        for (int unit = 0; unit < boundByUnit.length; unit++) {
            if (boundByUnit[unit] == glTexture) boundByUnit[unit] = null;
        }
        VTextureSelector.deleteGlTexture(i);
        glTexture.retireDeletedImageIfUnreferenced();

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

    static boolean hasNonDepthOnlyAlias(VulkanImage image) {
        for (GlTexture texture : map.values()) {
            if (texture.vulkanImage == image && !texture.isKnownDepthOnlyImage(image)) return true;
        }
        return false;
    }

    boolean isKnownDepthOnlyImage(VulkanImage image) {
        if (image == null || this.vulkanImage != image || !this.hasImageMetadata
                || this.target != GL11.GL_TEXTURE_2D || this.depth != 1
                || this.width != image.width || this.height != image.height) return false;
        return switch (this.internalFormat) {
            case GL11.GL_DEPTH_COMPONENT, GL14.GL_DEPTH_COMPONENT16, GL30.GL_DEPTH_COMPONENT24,
                 GL14.GL_DEPTH_COMPONENT32, GL30.GL_DEPTH_COMPONENT32F -> true;
            default -> false;
        };
    }

    public static void activeTexture(int i) {
        int unit = i - GL30.GL_TEXTURE0;
        if (!TextureUnitLimits.isValid(unit)) return;
        activeTexture = unit;
        boundTexture = boundByUnit[unit];
        boundTextureId = boundTexture != null ? boundTexture.id : 0;
        VTextureSelector.setActiveTexture(unit);
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
        if (target == GL11.GL_PROXY_TEXTURE_2D) {
            proxyImage2D(level, internalFormat, width, height, border);
            return;
        }
        if (checkParams(level, width, height))
            return;

        if (boundTexture == null) {
            GlEmulationLog.warnOnce("texImage2D.unbound", "glTexImage2D without a bound texture; upload skipped");
            return;
        }

        boundTexture.allocateIfNeeded(width, height, internalFormat, type);
        VTextureSelector.bindGlTexture(activeTexture, boundTexture.id, boundTexture.vulkanImage);

        texSubImage2D(target, level, 0, 0, width, height, format, type, pixels);
    }

    public static void texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        if (target == GL11.GL_PROXY_TEXTURE_2D) {
            proxyImage2D(level, internalFormat, width, height, border);
            return;
        }
        if (checkParams(level, width, height))
            return;

        if (boundTexture == null) {
            GlEmulationLog.warnOnce("texImage2D.unbound", "glTexImage2D without a bound texture; upload skipped");
            return;
        }

        boundTexture.allocateIfNeeded(width, height, internalFormat, type);
        VTextureSelector.bindGlTexture(activeTexture, boundTexture.id, boundTexture.vulkanImage);

        texSubImage2D(target, level, 0, 0, width, height, format, type, pixels);
    }

    private record ProxyImage(int width, int height, int internalFormat) {
        private static final ProxyImage UNSUPPORTED = new ProxyImage(0, 0, 0);
    }

    private static void proxyImage2D(int level, int internalFormat, int width, int height, int border) {

        if (level < 0 || level >= Integer.SIZE - 1) return;
        int maximum = DeviceManager.deviceProperties == null ? 0
                : DeviceManager.deviceProperties.limits().maxImageDimension2D();
        int levelMaximum = maximum > 0 ? maximum >>> level : 0;
        ProxyImage image = width > 0 && height > 0 && width <= levelMaximum && height <= levelMaximum
                && border == 0 && internalFormat > 0
                ? new ProxyImage(width, height, internalFormat) : ProxyImage.UNSUPPORTED;
        proxyImages2D.put(level, image);
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
            src = unpackBufferView(glBuffer, pixels);
        } else {
            if (pixels != 0L) {
                checkedByteSize((long) width * height, boundTexture.vulkanImage.formatSize);
                src = getByteBuffer(width, height, sourcePixelSize(format, type), getUnpackLayout(width), pixels);
            } else {
                src = null;
            }
        }

        if (src != null)
            boundTexture.uploadSubImage(xOffset, yOffset, width, height, format, type, src, getUnpackLayout(width));
    }

    private static ByteBuffer unpackBufferView(GlBuffer buffer, long offset) {
        if (buffer.data == null || offset < 0 || offset > buffer.data.capacity())
            throw new IllegalArgumentException("Invalid pixel unpack buffer offset or storage");
        return buffer.data.duplicate().clear().position((int) offset).slice();
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
            src = unpackBufferView(glBuffer, 0);
        } else {
            if (glBuffer != null) {
                GlEmulationLog.warnOnce("texSubImage2D.pboConflict",
                        "glTexSubImage2D received a client buffer while a pixel unpack buffer is bound; using the client buffer");
            }
            src = pixels;
        }

        if (src != null)
            boundTexture.uploadSubImage(xOffset, yOffset, width, height, format, type, src, getUnpackLayout(width));
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
        if (target == GL11.GL_PROXY_TEXTURE_2D) {
            ProxyImage image = proxyImages2D.get(level);
            if (image == null) return 0;
            return switch (pName) {
                case GL11.GL_TEXTURE_WIDTH -> image.width();
                case GL11.GL_TEXTURE_HEIGHT -> image.height();
                case GL11.GL_TEXTURE_INTERNAL_FORMAT -> image.internalFormat();
                case GL12.GL_TEXTURE_DEPTH -> image.width() > 0 && image.height() > 0 ? 1 : 0;
                default -> 0;
            };
        }
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
        long destinationBytes = Long.MAX_VALUE;
        if (buffer != null) {

            if (buffer.data == null) {
                GlEmulationLog.warnOnce("getTexImage.emptyPbo", "glGetTexImage into an unallocated pixel pack buffer; readback skipped");
                return;
            }

            if (pixels < 0 || pixels > buffer.data.capacity())
                throw new IllegalArgumentException("Invalid pixel pack buffer offset");
            destinationBytes = buffer.data.capacity() - pixels;
            ptr = MemoryUtil.memAddress0(buffer.data) + pixels;
        } else {
            ptr = pixels;
        }

        ImageUtil.downloadTextureGl(image, level, ptr,
                GlPixelStore.getInteger(GlPixelStore.GL_PACK_ROW_LENGTH),
                GlPixelStore.getInteger(GlPixelStore.GL_PACK_SKIP_ROWS),
                GlPixelStore.getInteger(GlPixelStore.GL_PACK_SKIP_PIXELS),
                GlPixelStore.getInteger(GlPixelStore.GL_PACK_ALIGNMENT), destinationBytes);
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
        publishVulkanImage(vulkanImage);

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

    private void publishVulkanImage(VulkanImage image) {
        if (this.vulkanImage != image) GlFramebuffer.textureImageChanged(this, image);
        this.vulkanImage = image;
        if (map.get(this.id) == this) VTextureSelector.updateGlTexture(this.id, image);
    }

    private int framebufferAttachmentReferences;
    private boolean deleted;
    private boolean deletedImageRetired;

    void retainFramebufferAttachment() { this.framebufferAttachmentReferences++; }

    void releaseFramebufferAttachment() {
        if (this.framebufferAttachmentReferences <= 0) throw new IllegalStateException("Framebuffer texture reference underflow");
        this.framebufferAttachmentReferences--;
        retireDeletedImageIfUnreferenced();
    }

    private void retireDeletedImageIfUnreferenced() {
        if (this.deleted && !this.deletedImageRetired && this.framebufferAttachmentReferences == 0) {
            this.deletedImageRetired = true;
            if (this.vulkanImage != null) MemoryManager.getInstance().addToFreeable(this.vulkanImage);
        }
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
        VulkanImage previous = this.vulkanImage;
        VulkanImage replacement;
        if (VulkanImage.isDepthFormat(vkFormat))
            replacement = VulkanImage.createDepthImage(vkFormat,
                    width, height,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
                    false, true);
        else
            replacement = new VulkanImage.Builder(width, height)
                    .setMipLevels(maxLevel + 1)
                    .setFormat(vkFormat)
                    .addUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                    .createVulkanImage();
        publishVulkanImage(replacement);
        if (previous != null) previous.free();
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

    private void uploadSubImage(int xOffset, int yOffset, int width, int height, int format, int type,
                                ByteBuffer pixels, TextureUploadLayout layout) {
        boolean rgba8Image = vulkanImage.format == VK_FORMAT_R8G8B8A8_UNORM || vulkanImage.format == VK_FORMAT_R8G8B8A8_SRGB;
        int sourcePixelSize = sourcePixelSize(format, type);
        int required = layout.requiredByteSize(width, height, sourcePixelSize);
        boolean rgb = format == GL11.GL_RGB && type == GL11.GL_UNSIGNED_BYTE && rgba8Image;
        boolean bgraPacked = format == GL30.GL_BGRA && rgba8Image
                && (type == GL12.GL_UNSIGNED_INT_8_8_8_8 || type == GL12.GL_UNSIGNED_INT_8_8_8_8_REV);
        boolean bgra = format == GL30.GL_BGRA && rgba8Image
                && (type == GL11.GL_UNSIGNED_BYTE || bgraPacked);
        if (rgba8Image && ((format == GL11.GL_RGB && !rgb) || (format == GL30.GL_BGRA && !bgra)))
            throw new IllegalArgumentException("Unsupported RGB/BGRA source type for RGBA8 conversion");
        if (!rgb && !bgra && sourcePixelSize != vulkanImage.formatSize)
            throw new IllegalArgumentException("Unsupported texture source format/type conversion");
        int outputSize = checkedByteSize((long) width * height, vulkanImage.formatSize);
        if (pixels.remaining() < required)
            throw new IllegalArgumentException("Pixel unpack source is shorter than its selected rows");

        ByteBuffer source = pixels.slice().limit(required).order(ByteOrder.nativeOrder());
        int rowBytes = checkedByteSize(width, sourcePixelSize);
        int rowStride = layout.rowStride(sourcePixelSize);
        int start = layout.sourceByteOffset(sourcePixelSize);
        boolean normalize = rgb || bgra || start != 0 || rowStride != rowBytes;
        ByteBuffer upload = normalize ? MemoryUtil.memAlloc(outputSize) : source;
        try {
            if (normalize) {
                for (int y = 0; y < height; y++) {
                    int row = start + y * rowStride;
                    if (rgb || bgra) {
                        for (int x = 0; x < width; x++) {
                            int in = row + x * sourcePixelSize, out = (y * width + x) * 4;
                            if (bgraPacked) {

                                int pixel = source.getInt(in);
                                if (layout.swapBytes) pixel = Integer.reverseBytes(pixel);
                                boolean reversed = type == GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
                                upload.put(out, (byte) (pixel >>> (reversed ? 16 : 8)));
                                upload.put(out + 1, (byte) (pixel >>> (reversed ? 8 : 16)));
                                upload.put(out + 2, (byte) (pixel >>> (reversed ? 0 : 24)));
                                upload.put(out + 3, (byte) (pixel >>> (reversed ? 24 : 0)));
                            } else {
                                upload.put(out, source.get(in + (bgra ? 2 : 0)));
                                upload.put(out + 1, source.get(in + 1));
                                upload.put(out + 2, source.get(in + (bgra ? 0 : 2)));
                                upload.put(out + 3, rgb ? (byte) 255 : source.get(in + 3));
                            }
                        }
                    } else {
                        upload.put(y * rowBytes, source, row, rowBytes);
                    }
                }
            }

            this.vulkanImage.uploadSubTextureAsync(0, width, height, xOffset, yOffset, 0, 0, width, upload);
        } finally {
            if (normalize) MemoryUtil.memFree(upload);
        }
    }

    private static int sourcePixelSize(int format, int type) {
        int components = switch (format) {
            case GL11.GL_RED, GL11.GL_ALPHA, GL11.GL_LUMINANCE, GL11.GL_DEPTH_COMPONENT, GL11.GL_STENCIL_INDEX -> 1;
            case GL30.GL_RG, GL11.GL_LUMINANCE_ALPHA -> 2;
            case GL11.GL_RGB, GL12.GL_BGR -> 3;
            case GL11.GL_RGBA, GL12.GL_BGRA -> 4;
            case GL30.GL_DEPTH_STENCIL -> 2;
            default -> throw new IllegalArgumentException("Unsupported texture source format");
        };
        return switch (type) {
            case GL11.GL_BYTE, GL11.GL_UNSIGNED_BYTE -> components;
            case GL11.GL_SHORT, GL11.GL_UNSIGNED_SHORT, GL30.GL_HALF_FLOAT -> components * 2;
            case GL11.GL_INT, GL11.GL_UNSIGNED_INT, GL11.GL_FLOAT -> components * 4;
            case GL12.GL_UNSIGNED_BYTE_3_3_2, GL12.GL_UNSIGNED_BYTE_2_3_3_REV -> packedPixelSize(components, 3, 1);
            case GL12.GL_UNSIGNED_SHORT_5_6_5, GL12.GL_UNSIGNED_SHORT_5_6_5_REV -> packedPixelSize(components, 3, 2);
            case GL12.GL_UNSIGNED_SHORT_4_4_4_4, GL12.GL_UNSIGNED_SHORT_4_4_4_4_REV,
                 GL12.GL_UNSIGNED_SHORT_5_5_5_1, GL12.GL_UNSIGNED_SHORT_1_5_5_5_REV -> packedPixelSize(components, 4, 2);
            case GL12.GL_UNSIGNED_INT_8_8_8_8, GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                 GL12.GL_UNSIGNED_INT_10_10_10_2, GL12.GL_UNSIGNED_INT_2_10_10_10_REV -> packedPixelSize(components, 4, 4);
            case GL30.GL_UNSIGNED_INT_10F_11F_11F_REV, GL30.GL_UNSIGNED_INT_5_9_9_9_REV -> packedPixelSize(components, 3, 4);
            case GL30.GL_UNSIGNED_INT_24_8 -> packedPixelSize(components, 2, 4);
            case GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV -> packedPixelSize(components, 2, 8);
            default -> throw new IllegalArgumentException("Unsupported texture source type");
        };
    }

    private static int packedPixelSize(int components, int requiredComponents, int bytes) {
        if (components != requiredComponents) throw new IllegalArgumentException("Invalid packed texture source format/type");
        return bytes;
    }

    private static int checkedByteSize(long count, int bytes) {
        if (count < 0 || bytes <= 0) throw new IllegalArgumentException("Invalid texture source byte size");
        try {
            return Math.toIntExact(Math.multiplyExact(count, bytes));
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Texture source layout exceeds the supported byte span", overflow);
        }
    }

    static TextureUploadLayout getUnpackLayout(int width) {
        int rowLength = GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_ROW_LENGTH);
        if (rowLength == 0) {
            rowLength = width;
        }

        return new TextureUploadLayout(
                rowLength,
                GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_SKIP_ROWS),
                GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_SKIP_PIXELS),
                GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_ALIGNMENT),
                GlPixelStore.getInteger(GlPixelStore.GL_UNPACK_SWAP_BYTES) != 0);
    }

    record TextureUploadLayout(int rowLength, int skipRows, int skipPixels, int alignment, boolean swapBytes) {
        TextureUploadLayout {
            if (rowLength < 0 || skipRows < 0 || skipPixels < 0
                    || (alignment != 1 && alignment != 2 && alignment != 4 && alignment != 8))
                throw new IllegalArgumentException("Invalid pixel unpack layout");
        }

        int rowStride(int formatSize) {
            long bytes = (long) rowLength * formatSize;
            return checkedByteSize((bytes + alignment - 1) / alignment, alignment);
        }

        int sourceByteOffset(int formatSize) {
            return checkedByteSize((long) rowStride(formatSize) * skipRows + (long) skipPixels * formatSize, 1);
        }

        int requiredByteSize(int width, int height, int formatSize) {
            if (width < 0 || height < 0) throw new IllegalArgumentException("Invalid texture upload dimensions");
            if (height == 0 || width == 0) {
                return 0;
            }
            return checkedByteSize((long) sourceByteOffset(formatSize) + (long) (height - 1) * rowStride(formatSize)
                    + (long) width * formatSize, 1);
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
