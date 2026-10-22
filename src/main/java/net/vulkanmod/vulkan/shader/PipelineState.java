package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_EQUAL;

public class PipelineState {
    private static final int DEFAULT_DEPTH_OP = 515;

    public static PipelineState.BlendInfo blendInfo = PipelineState.defaultBlendInfo();

    public static final PipelineState DEFAULT = new PipelineState(getAssemblyRasterState(), getBlendState(), getDepthState(), getStencilState(), getLogicOpState(), VRenderSystem.getColorMask(), null);

    public static PipelineState currentState = DEFAULT;

    public static PipelineState getCurrentPipelineState(RenderPass renderPass) {
        int assemblyRasterState = getAssemblyRasterState();
        int blendState = getBlendState();
        int currentColorMask = VRenderSystem.getColorMask();
        int depthState = getDepthState();
        int stencilState = getStencilState();
        int logicOp = getLogicOpState();

        if(currentState.checkEquals(assemblyRasterState, blendState, depthState, stencilState, logicOp, currentColorMask, renderPass))
            return currentState;
        else
            return currentState = new PipelineState(assemblyRasterState, blendState, depthState, stencilState, logicOp, currentColorMask, renderPass);
    }

    public static int getBlendState() {
        return BlendState.getState(blendInfo);
    }

    public static int getAssemblyRasterState() {
        int cullMode = VRenderSystem.cull ? VRenderSystem.cullFace : VK_CULL_MODE_NONE;
        int frontFace = VRenderSystem.frontFace;


        if (Renderer.isRasterRowsBottomUp()) {
            frontFace = frontFace == VK_FRONT_FACE_COUNTER_CLOCKWISE
                    ? VK_FRONT_FACE_CLOCKWISE : VK_FRONT_FACE_COUNTER_CLOCKWISE;
        }
        return AssemblyRasterState.encode(cullMode, VRenderSystem.topology, VRenderSystem.polygonMode, frontFace);
    }

    public static int getDepthState() {
        int depthState = 0;

        depthState |= VRenderSystem.depthTest ? DepthState.DEPTH_TEST_BIT : 0;
        depthState |= VRenderSystem.depthMask ? DepthState.DEPTH_MASK_BIT : 0;

        depthState |= DepthState.encodeDepthFun(VRenderSystem.depthFun);

        return depthState;
    }

    public static int getStencilState() {
        return StencilState.encode(VRenderSystem.stencilTest,
                VRenderSystem.stencilFunc,
                VRenderSystem.stencilFailOp,
                VRenderSystem.stencilPassOp,
                VRenderSystem.stencilDepthFailOp);
    }

    public static int getLogicOpState() {
        int logicOpState = 0;

        logicOpState |= VRenderSystem.logicOp ? LogicOpState.ENABLE_BIT : 0;

        logicOpState |= LogicOpState.encodeLogicOpFun(VRenderSystem.logicOpFun);

        return logicOpState;
    }

    final RenderPass renderPass;

    int assemblyRasterState;
    int blendState_i;
    int depthState_i;
    int stencilState_i;
    int colorMask_i;
    int logicOp_i;

    public PipelineState(int assemblyRasterState, int blendState, int depthState, int stencilState, int logicOp, int colorMask, RenderPass renderPass) {
        this.renderPass = renderPass;

        this.assemblyRasterState = assemblyRasterState;
        this.blendState_i = blendState;
        this.depthState_i = depthState;
        this.stencilState_i = stencilState;
        this.colorMask_i = colorMask;
        this.logicOp_i = logicOp;
    }

    private boolean checkEquals(int assemblyRasterState, int blendState, int depthState, int stencilState, int logicOp, int colorMask, RenderPass renderPass) {
        return (blendState == this.blendState_i) && (depthState == this.depthState_i)
                && (stencilState == this.stencilState_i)
                && renderPass == this.renderPass && logicOp == this.logicOp_i
                && (assemblyRasterState == this.assemblyRasterState)
                && colorMask == this.colorMask_i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PipelineState that = (PipelineState) o;
        return (blendState_i == that.blendState_i) && (depthState_i == that.depthState_i)
                && (stencilState_i == that.stencilState_i)
                && this.renderPass == that.renderPass && logicOp_i == that.logicOp_i
                && this.assemblyRasterState == that.assemblyRasterState
                && this.colorMask_i == that.colorMask_i;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blendState_i, depthState_i, stencilState_i, logicOp_i, assemblyRasterState, colorMask_i, renderPass);
    }

    public boolean stencilTestEnabled() {
        return StencilState.stencilTest(stencilState_i);
    }

    public static BlendInfo defaultBlendInfo() {
        return new BlendInfo(true, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
                VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_OP_ADD);
    }

    public static class BlendInfo {
        public boolean enabled;
        public int srcRgbFactor;
        public int dstRgbFactor;
        public int srcAlphaFactor;
        public int dstAlphaFactor;
        public int blendOp;
        public int blendOpRgb;
        public int blendOpAlpha;

        public BlendInfo(boolean enabled, int srcRgbFactor, int dstRgbFactor, int srcAlphaFactor, int dstAlphaFactor, int blendOp) {
            this.enabled = enabled;
            this.srcRgbFactor = srcRgbFactor;
            this.dstRgbFactor = dstRgbFactor;
            this.srcAlphaFactor = srcAlphaFactor;
            this.dstAlphaFactor = dstAlphaFactor;
            this.blendOp = blendOp;
            this.blendOpRgb = blendOp;
            this.blendOpAlpha = blendOp;
        }

        public void setBlendFunction(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
            this.srcRgbFactor = glToVulkanBlendFactor(sourceFactor.value);
            this.srcAlphaFactor = glToVulkanBlendFactor(sourceFactor.value);
            this.dstRgbFactor = glToVulkanBlendFactor(destFactor.value);
            this.dstAlphaFactor = glToVulkanBlendFactor(destFactor.value);
        }

        public void setBlendFuncSeparate(GlStateManager.SourceFactor srcRgb, GlStateManager.DestFactor dstRgb, GlStateManager.SourceFactor srcAlpha, GlStateManager.DestFactor dstAlpha) {
            this.srcRgbFactor = glToVulkanBlendFactor(srcRgb.value);
            this.srcAlphaFactor = glToVulkanBlendFactor(srcAlpha.value);
            this.dstRgbFactor = glToVulkanBlendFactor(dstRgb.value);
            this.dstAlphaFactor = glToVulkanBlendFactor(dstAlpha.value);
        }

        public void setBlendFunction(int sourceFactor, int destFactor) {
            this.srcRgbFactor = glToVulkanBlendFactor(sourceFactor);
            this.srcAlphaFactor = glToVulkanBlendFactor(sourceFactor);
            this.dstRgbFactor = glToVulkanBlendFactor(destFactor);
            this.dstAlphaFactor = glToVulkanBlendFactor(destFactor);
        }

        public void setBlendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this.srcRgbFactor = glToVulkanBlendFactor(srcRgb);
            this.srcAlphaFactor = glToVulkanBlendFactor(srcAlpha);
            this.dstRgbFactor = glToVulkanBlendFactor(dstRgb);
            this.dstAlphaFactor = glToVulkanBlendFactor(dstAlpha);
        }

        public void setBlendOp(int i) {
            setBlendOpSeparate(i, i);
        }

        public void setBlendOpSeparate(int rgb, int alpha) {
            this.blendOpRgb = glToVulkanBlendOp(rgb);
            this.blendOpAlpha = glToVulkanBlendOp(alpha);
            this.blendOp = this.blendOpRgb;
        }

        public int createBlendState() {
            return BlendState.getState(this);
        }

        private static int glToVulkanBlendOp(int value) {
            return switch (value) {
                case 0x8006 -> VK_BLEND_OP_ADD;
                case 0x8007 -> VK_BLEND_OP_MIN;
                case 0x8008 -> VK_BLEND_OP_MAX;
                case 0x800A -> VK_BLEND_OP_SUBTRACT;
                case 0x800B -> VK_BLEND_OP_REVERSE_SUBTRACT;
                default -> throw new RuntimeException("unknown blend factor: " + value);

            };
        }

        private static int glToVulkanBlendFactor(int value) {
            return switch (value) {
                case 1 -> VK_BLEND_FACTOR_ONE;
                case 0 -> VK_BLEND_FACTOR_ZERO;
                case 771 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
                case 770 -> VK_BLEND_FACTOR_SRC_ALPHA;
                case 773 -> VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
                case 772 -> VK_BLEND_FACTOR_DST_ALPHA;
                case 775 -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
                case 769 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
                case 774 -> VK_BLEND_FACTOR_DST_COLOR;
                case 768 -> VK_BLEND_FACTOR_SRC_COLOR;
                case 776 -> VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
                case 32769 -> VK_BLEND_FACTOR_CONSTANT_COLOR;
                case 32770 -> VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
                case 32771 -> VK_BLEND_FACTOR_CONSTANT_ALPHA;
                case 32772 -> VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
                default -> throw new RuntimeException("unknown blend factor: " + value);

            };
        }
    }

    public static class BlendState {
        public static final int SRC_RGB_OFFSET = 0;
        public static final int DST_RGB_OFFSET = 5;
        public static final int SRC_A_OFFSET = 10;
        public static final int DST_A_OFFSET = 15;
        public static final int COLOR_FUN_OFFSET = 20;
        public static final int ALPHA_FUN_OFFSET = 23;

        public static final int ENABLE_BIT = 1 << 30;

        public static final int OP_MASK = 0x7;
        public static final int FACTOR_MASK = 0x1F;

        public static int getState(BlendInfo blendInfo) {
            int s = 0;
            s |= blendInfo.enabled ? ENABLE_BIT : 0;
            s |= encode(blendInfo.srcRgbFactor, SRC_RGB_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.dstRgbFactor, DST_RGB_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.srcAlphaFactor, SRC_A_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.dstAlphaFactor, DST_A_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.blendOpRgb, COLOR_FUN_OFFSET, OP_MASK);
            s |= encode(blendInfo.blendOpAlpha, ALPHA_FUN_OFFSET, OP_MASK);

            return s;
        }

        public static boolean enable(int i) {
            return (i & ENABLE_BIT) != 0;
        }

        public static int encode(int i, int offset, int mask) {
            return (i & mask) << offset;
        }

        public static int decode(int i, int offset, int bits) {
            return (i >>> offset) & bits;
        }

        public static int getSrcRgbFactor(int s) {
            return decode(s, SRC_RGB_OFFSET, FACTOR_MASK);
        }

        public static int getDstRgbFactor(int s) {
            return decode(s, DST_RGB_OFFSET, FACTOR_MASK);
        }

        public static int getSrcAlphaFactor(int s) {
            return decode(s, SRC_A_OFFSET, FACTOR_MASK);
        }

        public static int getDstAlphaFactor(int s) {
            return decode(s, DST_A_OFFSET, FACTOR_MASK);
        }

        public static int blendOp(int state) {
            return getColorBlendOp(state);
        }

        public static int getColorBlendOp(int state) {
            return decode(state, COLOR_FUN_OFFSET, OP_MASK);
        }

        public static int getAlphaBlendOp(int state) {
            return decode(state, ALPHA_FUN_OFFSET, OP_MASK);
        }

    }

    public abstract static class LogicOpState {
        public static final int ENABLE_BIT = 1;

        public static final int FUN_OFFSET = 1;
        public static final int FUN_BITS = 5;

        public static boolean enable(int i) {
            return (i & ENABLE_BIT) != 0;
        }

        public static int encodeLogicOpFun(int glFun) {
            int fun = glToVulkan(glFun);

            return fun << FUN_OFFSET;
        }

        public static int decodeFun(int state) {
            return state >>> FUN_OFFSET;
        }

        public static int glToVulkan(int f) {
            return switch (f) {
                case 5387 -> VK_LOGIC_OP_OR_REVERSE;

                default -> VK_LOGIC_OP_AND;
            };
        }

    }

    public abstract static class AssemblyRasterState {
        public static final int POLYGON_MODE_MASK = 7;

        public static final int TOPOLOGY_OFFSET = 3;
        public static final int TOPOLOGY_BITS = 4;
        public static final int TOPOLOGY_MASK = 0b11111;

        public static final int CULL_MODE_OFFSET = TOPOLOGY_OFFSET + TOPOLOGY_BITS;
        public static final int CULL_MODE_BITS = 2;
        public static final int CULL_MODE_MASK = 0b11;

        public static final int FRONT_FACE_OFFSET = CULL_MODE_OFFSET + CULL_MODE_BITS;
        public static final int FRONT_FACE_BITS = 1;
        public static final int FRONT_FACE_MASK = 0b1;

        public static int encode(boolean cull, int topology, int polygonMode) {
            return encode(cull ? VK_CULL_MODE_BACK_BIT : VK_CULL_MODE_NONE,
                    topology,
                    polygonMode,
                    VK_FRONT_FACE_COUNTER_CLOCKWISE);
        }

        public static int encode(int cullMode, int topology, int polygonMode, int frontFace) {
            int state = (polygonMode | (topology << TOPOLOGY_OFFSET));
            state |= ((cullMode & CULL_MODE_MASK) << CULL_MODE_OFFSET);
            state |= ((frontFace & FRONT_FACE_MASK) << FRONT_FACE_OFFSET);

            return state;
        }

        public static int decodeTopology(int state) {
            return (state >>> TOPOLOGY_OFFSET) & TOPOLOGY_MASK;
        }

        public static int decodePolygonMode(int state) {
            return state & POLYGON_MODE_MASK;
        }

        public static int decodeCullMode(int state) {
            return (state >>> CULL_MODE_OFFSET) & CULL_MODE_MASK;
        }

        public static int decodeFrontFace(int state) {
            return (state >>> FRONT_FACE_OFFSET) & FRONT_FACE_MASK;
        }
    }

    public static abstract class ColorMask {

        public static int getColorMask(boolean r, boolean g, boolean b, boolean a) {
            return (r ? VK_COLOR_COMPONENT_R_BIT : 0)
                    | (g ? VK_COLOR_COMPONENT_G_BIT : 0)
                    | (b ? VK_COLOR_COMPONENT_B_BIT : 0)
                    | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

    }

    public static abstract class DepthState {
        public static final int DEPTH_TEST_BIT = 1;
        public static final int DEPTH_MASK_BIT = 2;

        public static final int DEPTH_FUN_OFFSET = 2;
        public static final int DEPTH_FUN_BITS = 4;

        public static boolean depthTest(int i) {
            return (i & DEPTH_TEST_BIT) != 0;
        }

        public static boolean depthMask(int i) {
            return (i & DEPTH_MASK_BIT) != 0;
        }

        public static int encodeDepthFun(int glFun) {
            int fun = glToVulkan(glFun);

            return fun << DEPTH_FUN_OFFSET;
        }

        public static int decodeDepthFun(int state) {
            return state >>> DEPTH_FUN_OFFSET;
        }

        private static int glToVulkan(int value) {
            return switch (value) {
                case 512 -> VK_COMPARE_OP_NEVER;
                case 513 -> VK_COMPARE_OP_LESS;
                case 514 -> VK_COMPARE_OP_EQUAL;
                case 515 -> VK_COMPARE_OP_LESS_OR_EQUAL;
                case 516 -> VK_COMPARE_OP_GREATER;
                case 517 -> VK_COMPARE_OP_NOT_EQUAL;
                case 518 -> VK_COMPARE_OP_GREATER_OR_EQUAL;
                case 519 -> VK_COMPARE_OP_ALWAYS;
                default -> throw new RuntimeException("unknown depth function: " + value);

            };
        }

    }

    public static abstract class StencilState {
        public static final int STENCIL_TEST_BIT = 1;

        public static final int COMPARE_OP_OFFSET = 1;
        public static final int STENCIL_FAIL_OP_OFFSET = 4;
        public static final int STENCIL_PASS_OP_OFFSET = 7;
        public static final int STENCIL_DEPTH_FAIL_OP_OFFSET = 10;

        public static final int COMPARE_OP_MASK = 0b111;
        public static final int STENCIL_OP_MASK = 0b111;

        public static int encode(boolean enabled, int glCompareOp, int glFailOp, int glPassOp, int glDepthFailOp) {
            int state = enabled ? STENCIL_TEST_BIT : 0;
            state |= encodeCompareOp(glCompareOp);
            state |= encodeStencilOp(glFailOp) << STENCIL_FAIL_OP_OFFSET;
            state |= encodeStencilOp(glPassOp) << STENCIL_PASS_OP_OFFSET;
            state |= encodeStencilOp(glDepthFailOp) << STENCIL_DEPTH_FAIL_OP_OFFSET;

            return state;
        }

        public static boolean stencilTest(int state) {
            return (state & STENCIL_TEST_BIT) != 0;
        }

        public static int encodeCompareOp(int glFun) {
            return glToVulkanCompareOp(glFun) << COMPARE_OP_OFFSET;
        }

        public static int encodeStencilOp(int glOp) {
            return switch (glOp) {
                case 0x0000 -> VK_STENCIL_OP_ZERO;
                case 0x1E00 -> VK_STENCIL_OP_KEEP;
                case 0x1E01 -> VK_STENCIL_OP_REPLACE;
                case 0x1E02 -> VK_STENCIL_OP_INCREMENT_AND_CLAMP;
                case 0x1E03 -> VK_STENCIL_OP_DECREMENT_AND_CLAMP;
                case 0x150A -> VK_STENCIL_OP_INVERT;
                case 0x8507 -> VK_STENCIL_OP_INCREMENT_AND_WRAP;
                case 0x8508 -> VK_STENCIL_OP_DECREMENT_AND_WRAP;
                default -> throw new RuntimeException("unknown stencil op: " + glOp);
            };
        }

        public static int decodeCompareOp(int state) {
            return (state >>> COMPARE_OP_OFFSET) & COMPARE_OP_MASK;
        }

        public static int decodeFailOp(int state) {
            return (state >>> STENCIL_FAIL_OP_OFFSET) & STENCIL_OP_MASK;
        }

        public static int decodePassOp(int state) {
            return (state >>> STENCIL_PASS_OP_OFFSET) & STENCIL_OP_MASK;
        }

        public static int decodeDepthFailOp(int state) {
            return (state >>> STENCIL_DEPTH_FAIL_OP_OFFSET) & STENCIL_OP_MASK;
        }

        private static int glToVulkanCompareOp(int value) {
            return switch (value) {
                case 512 -> VK_COMPARE_OP_NEVER;
                case 513 -> VK_COMPARE_OP_LESS;
                case 514 -> VK_COMPARE_OP_EQUAL;
                case 515 -> VK_COMPARE_OP_LESS_OR_EQUAL;
                case 516 -> VK_COMPARE_OP_GREATER;
                case 517 -> VK_COMPARE_OP_NOT_EQUAL;
                case 518 -> VK_COMPARE_OP_GREATER_OR_EQUAL;
                case 519 -> VK_COMPARE_OP_ALWAYS;
                default -> throw new RuntimeException("unknown stencil compare function: " + value);
            };
        }
    }
}
