package net.vulkanmod.gl;

import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.apache.commons.lang3.Validate;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public abstract class GlUtil {

    public static SPIRVUtils.ShaderKind extToShaderKind(String ext) {
        return switch (ext) {
            case ".vsh" -> SPIRVUtils.ShaderKind.VERTEX_SHADER;
            case ".fsh" -> SPIRVUtils.ShaderKind.FRAGMENT_SHADER;
            default -> throw new RuntimeException("unknown shader type: " + ext);
        };
    }

    public static ByteBuffer RGBtoRGBA_buffer(ByteBuffer in) {
        Validate.isTrue(in.remaining() % 3 == 0, "Unexpected buffer stride");

        int outSize = in.remaining() * 4 / 3;
        ByteBuffer out = MemoryUtil.memAlloc(outSize);

        int j = 0;
        for (int i = 0; i < outSize; i+=4, j+=3) {
            out.put(i, in.get(j));
            out.put(i + 1, in.get(j + 1));
            out.put(i + 2, in.get(j + 2));
            out.put(i + 3, (byte) 0xFF);
        }

        return out;
    }

    public static ByteBuffer BGRAtoRGBA_buffer(ByteBuffer in) {
        Validate.isTrue(in.remaining() % 4 == 0, "Unexpected buffer stride");

        int outSize = in.remaining();
        ByteBuffer out = MemoryUtil.memAlloc(outSize);

        long ptr = MemoryUtil.memAddress0(out);

        long srcPtr = MemoryUtil.memAddress0(in);

        for (int i = 0; i < outSize ; i += 4) {
            int color = MemoryUtil.memGetInt(srcPtr + i);

            color = (color << 24) & 0xFF000000 | (color >> 8) & 0xFFFFFF;

            MemoryUtil.memPutInt(ptr + i, color);
        }

        return out;
    }

    public static int vulkanFormat(int glFormat, int type) {
        return switch (glFormat) {
            case GL30.GL_R8 -> VK_FORMAT_R8_UNORM;
            case GL30.GL_RG8 -> VK_FORMAT_R8G8_UNORM;
            case GL30.GL_RGB8, GL30.GL_RGBA8 -> VK_FORMAT_R8G8B8A8_UNORM;
            case GL30.GL_R16 -> VK_FORMAT_R16_UNORM;
            case GL30.GL_RG16 -> VK_FORMAT_R16G16_UNORM;
            case GL30.GL_R32F -> VK_FORMAT_R32_SFLOAT;
            case GL30.GL_RG32F -> VK_FORMAT_R32G32_SFLOAT;
            case GL21.GL_SRGB8_ALPHA8, GL21.GL_SRGB8 -> VK_FORMAT_R8G8B8A8_SRGB;
            case GL30.GL_R11F_G11F_B10F -> VK_FORMAT_B10G11R11_UFLOAT_PACK32;
            case GL11.GL_RGB10_A2 -> VK_FORMAT_A2B10G10R10_UNORM_PACK32;
            case GL31.GL_R8_SNORM -> VK_FORMAT_R8_SNORM;
            case GL31.GL_RG8_SNORM -> VK_FORMAT_R8G8_SNORM;
            case GL31.GL_RGB8_SNORM, GL31.GL_RGBA8_SNORM -> VK_FORMAT_R8G8B8A8_SNORM;
            case GL30.GL_R16F -> VK_FORMAT_R16_SFLOAT;
            case GL30.GL_RG16F -> VK_FORMAT_R16G16_SFLOAT;
            case GL30.GL_RGB16F, GL30.GL_RGBA16F -> VK_FORMAT_R16G16B16A16_SFLOAT;
            case GL30.GL_RGBA32F -> VK_FORMAT_R32G32B32A32_SFLOAT;
            case GL11.GL_RGBA ->
                    switch (type) {
                        case GL11.GL_FLOAT -> VK_FORMAT_R32G32B32A32_SFLOAT;

                        default -> VK_FORMAT_R8G8B8A8_UNORM;
                    };
            case GL11.GL_RGB -> VK_FORMAT_R8G8B8A8_UNORM;
            case GL30.GL_BGRA -> VK_FORMAT_B8G8R8A8_UNORM;
            case GL30.GL_UNSIGNED_INT_8_8_8_8_REV ->
                    switch (type) {
                        case GL11.GL_UNSIGNED_BYTE -> VK_FORMAT_R8G8B8A8_UINT;
                        default -> VK_FORMAT_R8G8B8A8_UNORM;
                    };
            case GL30.GL_RG ->
                    switch (type) {
                        case GL11.GL_FLOAT -> VK_FORMAT_R32G32_SFLOAT;
                        case GL11.GL_UNSIGNED_SHORT -> VK_FORMAT_R16G16_UNORM;
                        default -> VK_FORMAT_R8G8_UNORM;
                    };
            case GL11.GL_RED ->
                    switch (type) {
                        case GL11.GL_FLOAT -> VK_FORMAT_R32_SFLOAT;
                        case GL11.GL_UNSIGNED_SHORT -> VK_FORMAT_R16_UNORM;
                        default -> VK_FORMAT_R8_UNORM;
                    };
            case GL11.GL_DEPTH_COMPONENT, GL30.GL_DEPTH_COMPONENT32F, GL14.GL_DEPTH_COMPONENT32, GL30.GL_DEPTH_COMPONENT24, GL14.GL_DEPTH_COMPONENT16 ->
                    Vulkan.getDefaultDepthFormat();

            case GL30.GL_DEPTH32F_STENCIL8, GL30.GL_DEPTH24_STENCIL8, GL30.GL_DEPTH_STENCIL -> VK_FORMAT_D32_SFLOAT_S8_UINT;
            case GL30.GL_STENCIL_INDEX8 -> VK_FORMAT_S8_UINT;

            default -> {
                GlEmulationLog.warnOnce("vulkanFormat.texture." + glFormat,
                        "Unmapped GL texture format 0x{} (type 0x{}); defaulting to RGBA8", Integer.toHexString(glFormat), Integer.toHexString(type));
                yield VK_FORMAT_R8G8B8A8_UNORM;
            }
        };
    }

    public static int vulkanFormat(int glInternalFormat) {
        return switch (glInternalFormat) {
            case GL11.GL_RGBA, GL11.GL_RGB, GL30.GL_RGB8, GL30.GL_RGBA8 -> VK_FORMAT_R8G8B8A8_UNORM;
            case GL30.GL_BGRA -> VK_FORMAT_B8G8R8A8_UNORM;
            case GL30.GL_RG, GL30.GL_RG8 -> VK_FORMAT_R8G8_UNORM;
            case GL11.GL_RED, GL30.GL_R8 -> VK_FORMAT_R8_UNORM;
            case GL31.GL_R8_SNORM -> VK_FORMAT_R8_SNORM;
            case GL31.GL_RG8_SNORM -> VK_FORMAT_R8G8_SNORM;
            case GL31.GL_RGB8_SNORM, GL31.GL_RGBA8_SNORM -> VK_FORMAT_R8G8B8A8_SNORM;
            case GL30.GL_R16 -> VK_FORMAT_R16_UNORM;
            case GL30.GL_RG16 -> VK_FORMAT_R16G16_UNORM;
            case GL30.GL_R16F -> VK_FORMAT_R16_SFLOAT;
            case GL30.GL_RG16F -> VK_FORMAT_R16G16_SFLOAT;
            case GL30.GL_R32F -> VK_FORMAT_R32_SFLOAT;
            case GL30.GL_RG32F -> VK_FORMAT_R32G32_SFLOAT;
            case GL30.GL_RGB16F, GL30.GL_RGBA16F -> VK_FORMAT_R16G16B16A16_SFLOAT;
            case GL30.GL_RGBA32F -> VK_FORMAT_R32G32B32A32_SFLOAT;
            case GL21.GL_SRGB8_ALPHA8, GL21.GL_SRGB8 -> VK_FORMAT_R8G8B8A8_SRGB;
            case GL30.GL_R11F_G11F_B10F -> VK_FORMAT_B10G11R11_UFLOAT_PACK32;
            case GL11.GL_RGB10_A2 -> VK_FORMAT_A2B10G10R10_UNORM_PACK32;
            case GL30.GL_UNSIGNED_INT_8_8_8_8_REV -> VK_FORMAT_R8G8B8A8_UINT;
            case GL11.GL_DEPTH_COMPONENT, GL30.GL_DEPTH_COMPONENT32F, GL14.GL_DEPTH_COMPONENT32, GL30.GL_DEPTH_COMPONENT24, GL14.GL_DEPTH_COMPONENT16 ->
                    Vulkan.getDefaultDepthFormat();

            case GL30.GL_DEPTH32F_STENCIL8, GL30.GL_DEPTH24_STENCIL8, GL30.GL_DEPTH_STENCIL -> VK_FORMAT_D32_SFLOAT_S8_UINT;
            case GL30.GL_STENCIL_INDEX8 -> VK_FORMAT_S8_UINT;

            default -> {
                GlEmulationLog.warnOnce("vulkanFormat.internal." + glInternalFormat,
                        "Unmapped GL internal format 0x{}; defaulting to RGBA8", Integer.toHexString(glInternalFormat));
                yield VK_FORMAT_R8G8B8A8_UNORM;
            }
        };
    }

    public static int getGlFormat(int vFormat) {
        return switch (vFormat) {
            case VK_FORMAT_R8G8B8A8_UNORM, VK_FORMAT_B8G8R8A8_UNORM -> GL30.GL_RGBA8;
            case VK_FORMAT_R8G8B8A8_SRGB -> GL21.GL_SRGB8_ALPHA8;
            case VK_FORMAT_R8G8_UNORM -> GL30.GL_RG8;
            case VK_FORMAT_R8_UNORM -> GL30.GL_R8;
            case VK_FORMAT_R8G8B8A8_SNORM -> GL31.GL_RGBA8_SNORM;
            case VK_FORMAT_R8G8_SNORM -> GL31.GL_RG8_SNORM;
            case VK_FORMAT_R8_SNORM -> GL31.GL_R8_SNORM;
            case VK_FORMAT_R16_UNORM -> GL30.GL_R16;
            case VK_FORMAT_R16G16_UNORM -> GL30.GL_RG16;
            case VK_FORMAT_R16_SFLOAT -> GL30.GL_R16F;
            case VK_FORMAT_R16G16_SFLOAT -> GL30.GL_RG16F;
            case VK_FORMAT_R16G16B16A16_SFLOAT -> GL30.GL_RGBA16F;
            case VK_FORMAT_R32_SFLOAT -> GL30.GL_R32F;
            case VK_FORMAT_R32G32_SFLOAT -> GL30.GL_RG32F;
            case VK_FORMAT_R32G32B32A32_SFLOAT -> GL30.GL_RGBA32F;
            case VK_FORMAT_B10G11R11_UFLOAT_PACK32 -> GL30.GL_R11F_G11F_B10F;
            case VK_FORMAT_A2B10G10R10_UNORM_PACK32 -> GL11.GL_RGB10_A2;
            case VK_FORMAT_D16_UNORM -> GL30.GL_DEPTH_COMPONENT16;
            case VK_FORMAT_X8_D24_UNORM_PACK32 -> GL30.GL_DEPTH_COMPONENT24;
            case VK_FORMAT_D32_SFLOAT -> GL30.GL_DEPTH_COMPONENT32F;
            case VK_FORMAT_S8_UINT -> GL30.GL_STENCIL_INDEX8;
            case VK_FORMAT_D16_UNORM_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT -> GL30.GL_DEPTH24_STENCIL8;
            case VK_FORMAT_D32_SFLOAT_S8_UINT -> GL30.GL_DEPTH32F_STENCIL8;

            default -> GL11.GL_RGBA;
        };
    }
}
