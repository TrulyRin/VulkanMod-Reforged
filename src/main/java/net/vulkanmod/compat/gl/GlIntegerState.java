package net.vulkanmod.compat.gl;

import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.gl.GlVertexArray;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.PipelineState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40C;

import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_CONSTANT_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_CONSTANT_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_DST_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_DST_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_COLOR;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ZERO;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_MAX;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_MIN;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_REVERSE_SUBTRACT;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_SUBTRACT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;

public final class GlIntegerState {
    private GlIntegerState() {
    }

    public static int getInteger(int pname) {
        if (pname == GL30.GL_READ_FRAMEBUFFER_BINDING) {
            return GlFramebuffer.getBoundReadId();
        }

        if (pname == GL30.GL_FRAMEBUFFER_BINDING || pname == GL30.GL_DRAW_FRAMEBUFFER_BINDING) {
            return GlFramebuffer.getBoundId();
        }

        return switch (pname) {
            case GL11.GL_VIEWPORT -> Renderer.getGlViewport(0);
            case GL40C.GL_PATCH_VERTICES -> GlPatchState.getPatchVertices();
            case GL40C.GL_MAX_PATCH_VERTICES -> GlPatchState.getMaxPatchVertices();
            case GL20.GL_CURRENT_PROGRAM -> GlProgram.currentProgramId();
            case GL30.GL_VERTEX_ARRAY_BINDING -> GlVertexArray.boundId();
            case GL15_BINDING_ARRAY, GL15_BINDING_ELEMENT_ARRAY, GL32.GL_PIXEL_PACK_BUFFER_BINDING,
                    GL32.GL_PIXEL_UNPACK_BUFFER_BINDING, GL_COPY_READ_BUFFER_BINDING,
                    GL_COPY_WRITE_BUFFER_BINDING -> GlBuffer.getBoundId(pname);
            case GL11.GL_TEXTURE_BINDING_2D -> GlTexture.getBoundTextureId(GL11.GL_TEXTURE_2D);
            case GL13_ACTIVE_TEXTURE -> GlTexture.getActiveTexture();
            case GL_BLEND_EQUATION_RGB -> vulkanBlendOpToGl(PipelineState.blendInfo.blendOpRgb);
            case GL_BLEND_EQUATION_ALPHA -> vulkanBlendOpToGl(PipelineState.blendInfo.blendOpAlpha);
            case GL14.GL_BLEND_SRC_RGB -> vulkanBlendFactorToGl(PipelineState.blendInfo.srcRgbFactor);
            case GL14.GL_BLEND_DST_RGB -> vulkanBlendFactorToGl(PipelineState.blendInfo.dstRgbFactor);
            case GL14.GL_BLEND_SRC_ALPHA -> vulkanBlendFactorToGl(PipelineState.blendInfo.srcAlphaFactor);
            case GL14.GL_BLEND_DST_ALPHA -> vulkanBlendFactorToGl(PipelineState.blendInfo.dstAlphaFactor);
            case GL11.GL_COLOR_WRITEMASK -> getInteger(pname, 0);
            case GL11.GL_DEPTH_WRITEMASK -> VRenderSystem.depthMask ? GL11.GL_TRUE : GL11.GL_FALSE;
            case GL11.GL_DEPTH_FUNC -> VRenderSystem.depthFun;
            case GL_STENCIL_FUNC -> VRenderSystem.stencilFunc;
            case GL_STENCIL_REF -> VRenderSystem.stencilRef;
            case GL_STENCIL_VALUE_MASK -> VRenderSystem.stencilFuncMask;
            case GL_STENCIL_FAIL -> VRenderSystem.stencilFailOp;
            case GL_STENCIL_PASS_DEPTH_FAIL -> VRenderSystem.stencilDepthFailOp;
            case GL_STENCIL_PASS_DEPTH_PASS -> VRenderSystem.stencilPassOp;
            case GL_STENCIL_WRITEMASK -> VRenderSystem.stencilWriteMask;
            case GL_STENCIL_CLEAR_VALUE -> VRenderSystem.clearStencilValue;
            case GL11.GL_CULL_FACE_MODE -> switch (VRenderSystem.cullFace) {
                case org.lwjgl.vulkan.VK10.VK_CULL_MODE_FRONT_BIT -> GL11.GL_FRONT;
                case org.lwjgl.vulkan.VK10.VK_CULL_MODE_BACK_BIT -> GL11.GL_BACK;
                case org.lwjgl.vulkan.VK10.VK_CULL_MODE_FRONT_AND_BACK -> GL11.GL_FRONT_AND_BACK;
                default -> GL11.GL_BACK;
            };
            case GL11.GL_FRONT_FACE -> VRenderSystem.frontFace == org.lwjgl.vulkan.VK10.VK_FRONT_FACE_CLOCKWISE
                    ? GL11.GL_CW : GL11.GL_CCW;
            case GL11.GL_POLYGON_MODE -> GL11.GL_FILL;
            case GL30.GL_MAJOR_VERSION -> 3;
            case GL30.GL_MINOR_VERSION -> 2;
            case GL11.GL_MAX_TEXTURE_SIZE, GL32.GL_MAX_CUBE_MAP_TEXTURE_SIZE,
                    GL32.GL_MAX_RENDERBUFFER_SIZE -> VRenderSystem.maxSupportedTextureSize();

            case GL32.GL_MAX_COLOR_ATTACHMENTS, GL32.GL_MAX_DRAW_BUFFERS, GL32.GL_MAX_CLIP_DISTANCES -> 8;
            case GL32.GL_MAX_SAMPLES -> 4;
            case GL32.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> TextureUnitLimits.COMBINED;
            case GL32.GL_MAX_TEXTURE_IMAGE_UNITS, GL32.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS,
                    GL32.GL_MAX_VERTEX_ATTRIBS, GL_MAX_VERTEX_ATTRIB_BINDINGS -> 16;
            case GL32.GL_MAX_VERTEX_UNIFORM_COMPONENTS, GL32.GL_MAX_FRAGMENT_UNIFORM_COMPONENTS,
                    GL_MAX_UNIFORM_LOCATIONS -> 1024;
            case GL32.GL_MAX_VERTEX_UNIFORM_BLOCKS, GL32.GL_MAX_FRAGMENT_UNIFORM_BLOCKS,
                    GL32.GL_MAX_GEOMETRY_UNIFORM_BLOCKS -> 12;
            case GL32.GL_MAX_COMBINED_UNIFORM_BLOCKS, GL32.GL_MAX_UNIFORM_BUFFER_BINDINGS -> 36;
            case GL32.GL_MAX_UNIFORM_BLOCK_SIZE, GL32.GL_MAX_TEXTURE_BUFFER_SIZE -> 65536;
            case GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT -> 256;
            case GL32.GL_MAX_VERTEX_OUTPUT_COMPONENTS -> 64;
            case GL32.GL_MAX_FRAGMENT_INPUT_COMPONENTS -> 128;
            case GL32.GL_MAX_VARYING_COMPONENTS -> 60;
            case GL32.GL_MAX_ARRAY_TEXTURE_LAYERS, GL32.GL_MAX_3D_TEXTURE_SIZE -> 256;
            case GL_MAX_VERTEX_ATTRIB_RELATIVE_OFFSET -> 2047;
            default -> 0;
        };
    }

    public static int getInteger(int pname, int index) {
        if (pname == GL11.GL_VIEWPORT) {
            return Renderer.getGlViewport(index);
        }

        if (pname == GL11.GL_COLOR_WRITEMASK) {
            return colorMaskComponent(index) ? GL11.GL_TRUE : GL11.GL_FALSE;
        }

        if (pname == GL11.GL_SCISSOR_BOX) {
            return Renderer.getScissorBox(index);
        }

        return getInteger(pname);
    }

    public static int getComponentCount(int pname) {
        return (pname == GL11.GL_COLOR_WRITEMASK || pname == GL11.GL_SCISSOR_BOX || pname == GL11.GL_VIEWPORT) ? 4 : 1;
    }

    public static boolean isBooleanIntegerState(int pname) {
        return pname == GL11.GL_COLOR_WRITEMASK || pname == GL11.GL_DEPTH_WRITEMASK;
    }

    private static final int GL13_ACTIVE_TEXTURE = 0x84E0;
    private static final int GL15_BINDING_ARRAY = 0x8894;
    private static final int GL15_BINDING_ELEMENT_ARRAY = 0x8895;
    private static final int GL_COPY_READ_BUFFER_BINDING = 0x8F36;
    private static final int GL_COPY_WRITE_BUFFER_BINDING = 0x8F37;
    private static final int GL_BLEND_EQUATION_RGB = 0x8009;
    private static final int GL_BLEND_EQUATION_ALPHA = 0x883D;
    private static final int GL_STENCIL_CLEAR_VALUE = 0x0B91;
    private static final int GL_STENCIL_FUNC = 0x0B92;
    private static final int GL_STENCIL_VALUE_MASK = 0x0B93;
    private static final int GL_STENCIL_FAIL = 0x0B94;
    private static final int GL_STENCIL_PASS_DEPTH_FAIL = 0x0B95;
    private static final int GL_STENCIL_PASS_DEPTH_PASS = 0x0B96;
    private static final int GL_STENCIL_REF = 0x0B97;
    private static final int GL_STENCIL_WRITEMASK = 0x0B98;
    private static final int GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT = 0x8A34;
    private static final int GL_MAX_UNIFORM_LOCATIONS = 0x826E;
    private static final int GL_MAX_VERTEX_ATTRIB_RELATIVE_OFFSET = 0x82D9;
    private static final int GL_MAX_VERTEX_ATTRIB_BINDINGS = 0x82DA;

    private static boolean colorMaskComponent(int index) {
        int component = switch (index) {
            case 0 -> VK_COLOR_COMPONENT_R_BIT;
            case 1 -> VK_COLOR_COMPONENT_G_BIT;
            case 2 -> VK_COLOR_COMPONENT_B_BIT;
            case 3 -> VK_COLOR_COMPONENT_A_BIT;
            default -> 0;
        };

        return (VRenderSystem.getColorMask() & component) != 0;
    }

    private static int vulkanBlendFactorToGl(int factor) {
        return switch (factor) {
            case VK_BLEND_FACTOR_ZERO -> GL11.GL_ZERO;
            case VK_BLEND_FACTOR_ONE -> GL11.GL_ONE;
            case VK_BLEND_FACTOR_CONSTANT_COLOR -> GL14.GL_CONSTANT_COLOR;
            case VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR -> GL14.GL_ONE_MINUS_CONSTANT_COLOR;
            case VK_BLEND_FACTOR_CONSTANT_ALPHA -> GL14.GL_CONSTANT_ALPHA;
            case VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA -> GL14.GL_ONE_MINUS_CONSTANT_ALPHA;
            case VK_BLEND_FACTOR_SRC_COLOR -> GL11.GL_SRC_COLOR;
            case VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR -> GL11.GL_ONE_MINUS_SRC_COLOR;
            case VK_BLEND_FACTOR_DST_COLOR -> GL11.GL_DST_COLOR;
            case VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR -> GL11.GL_ONE_MINUS_DST_COLOR;
            case VK_BLEND_FACTOR_SRC_ALPHA -> GL11.GL_SRC_ALPHA;
            case VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA -> GL11.GL_ONE_MINUS_SRC_ALPHA;
            case VK_BLEND_FACTOR_DST_ALPHA -> GL11.GL_DST_ALPHA;
            case VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA -> GL11.GL_ONE_MINUS_DST_ALPHA;
            case VK_BLEND_FACTOR_SRC_ALPHA_SATURATE -> GL11.GL_SRC_ALPHA_SATURATE;
            default -> 0;
        };
    }

    private static int vulkanBlendOpToGl(int op) {
        return switch (op) {
            case VK_BLEND_OP_ADD -> GL14.GL_FUNC_ADD;
            case VK_BLEND_OP_MIN -> GL14.GL_MIN;
            case VK_BLEND_OP_MAX -> GL14.GL_MAX;
            case VK_BLEND_OP_SUBTRACT -> GL14.GL_FUNC_SUBTRACT;
            case VK_BLEND_OP_REVERSE_SUBTRACT -> GL14.GL_FUNC_REVERSE_SUBTRACT;
            default -> GL14.GL_FUNC_ADD;
        };
    }
}
