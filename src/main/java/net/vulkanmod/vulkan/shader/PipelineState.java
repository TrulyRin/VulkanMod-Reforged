package net.vulkanmod.vulkan.shader;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL33;

import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

public class PipelineState {
    private static final int DEFAULT_DEPTH_OP = 515;
                                                      

    public static PipelineState.BlendInfo blendInfo = PipelineState.defaultBlendInfo();

    public static final PipelineState DEFAULT = new PipelineState(getAssemblyRasterState(), getBlendState(), getDepthState(), getLogicOpState(), VRenderSystem.getColorMask(), null);

    public static PipelineState currentState = DEFAULT;

    public static PipelineState getCurrentPipelineState(RenderPass renderPass) {
        int assemblyRasterState = getAssemblyRasterState();
        int blendState = getBlendState();
        int currentColorMask = VRenderSystem.getColorMask();
        int depthState = getDepthState();
        int logicOp = getLogicOpState();

        if (currentState.checkEquals(assemblyRasterState, blendState, depthState, logicOp, currentColorMask, renderPass))
            return currentState;
        else
            return currentState = new PipelineState(assemblyRasterState, blendState, depthState, logicOp, currentColorMask, renderPass);
    }

    public static int getBlendState() {
        return BlendState.getState(blendInfo);
    }

    public static int getAssemblyRasterState() {
        return AssemblyRasterState.encode(VRenderSystem.cull, VRenderSystem.topology, VRenderSystem.polygonMode);
    }

    public static int getDepthState() {
        int depthState = 0;

        depthState |= VRenderSystem.depthTest ? DepthState.DEPTH_TEST_BIT : 0;
        depthState |= VRenderSystem.depthMask ? DepthState.DEPTH_MASK_BIT : 0;

        depthState |= DepthState.encodeDepthFun(VRenderSystem.depthFun);

        return depthState;
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
    int colorMask_i;
    int logicOp_i;

    public PipelineState(int assemblyRasterState, int blendState, int depthState, int logicOp, int colorMask,
                         RenderPass renderPass) {
        this.renderPass = renderPass;

        this.assemblyRasterState = assemblyRasterState;
        this.blendState_i = blendState;
        this.depthState_i = depthState;
        this.colorMask_i = colorMask;
        this.logicOp_i = logicOp;
    }

    private boolean checkEquals(int assemblyRasterState, int blendState, int depthState, int logicOp, int colorMask,
                                RenderPass renderPass) {
        return (blendState == this.blendState_i) && (depthState == this.depthState_i)
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
               && this.renderPass == that.renderPass && logicOp_i == that.logicOp_i
               && this.assemblyRasterState == that.assemblyRasterState
               && this.colorMask_i == that.colorMask_i;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blendState_i, depthState_i, logicOp_i, assemblyRasterState, colorMask_i, renderPass);
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

        public BlendInfo(boolean enabled, int srcRgbFactor, int dstRgbFactor, int srcAlphaFactor, int dstAlphaFactor,
                         int blendOp) {
            this.enabled = enabled;
            this.srcRgbFactor = srcRgbFactor;
            this.dstRgbFactor = dstRgbFactor;
            this.srcAlphaFactor = srcAlphaFactor;
            this.dstAlphaFactor = dstAlphaFactor;
            this.blendOp = blendOp;
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
            this.blendOp = glToVulkanBlendOp(i);
        }


        public int createBlendState() {
            return BlendState.getState(this);
        }

        private static int glToVulkanBlendOp(int value) {
            return switch (value) {
                case GL33.GL_FUNC_ADD -> VK_BLEND_OP_ADD;
                case GL33.GL_MIN -> VK_BLEND_OP_MIN;
                case GL33.GL_MAX -> VK_BLEND_OP_MAX;
                case GL33.GL_FUNC_SUBTRACT -> VK_BLEND_OP_SUBTRACT;
                case GL33.GL_FUNC_REVERSE_SUBTRACT -> VK_BLEND_OP_REVERSE_SUBTRACT;
                default -> throw new RuntimeException("unknown blend factor: " + value);
            };
        }

        private static int glToVulkanBlendFactor(int value) {
            return switch (value) {
                case GL11.GL_ONE -> VK_BLEND_FACTOR_ONE;
                case GL11.GL_ZERO -> VK_BLEND_FACTOR_ZERO;
                case GL11.GL_SRC_COLOR -> VK_BLEND_FACTOR_SRC_COLOR;
                case GL11.GL_ONE_MINUS_SRC_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
                case GL11.GL_SRC_ALPHA -> VK_BLEND_FACTOR_SRC_ALPHA;
                case GL11.GL_ONE_MINUS_SRC_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
                case GL11.GL_DST_ALPHA -> VK_BLEND_FACTOR_DST_ALPHA;
                case GL11.GL_ONE_MINUS_DST_ALPHA -> VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
                case GL11.GL_DST_COLOR -> VK_BLEND_FACTOR_DST_COLOR;
                case GL11.GL_ONE_MINUS_DST_COLOR -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
                case GL11.GL_SRC_ALPHA_SATURATE -> VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
                default -> throw new RuntimeException("unknown blend factor: " + value);
            };
        }
    }

    public static class BlendState {
        public static final int SRC_RGB_OFFSET = 0;
        public static final int DST_RGB_OFFSET = 5;
        public static final int SRC_A_OFFSET = 10;
        public static final int DST_A_OFFSET = 15;
        public static final int FUN_OFFSET = 20;

        public static final int ENABLE_BIT = 1 << 24;

        public static final int OP_MASK = 0xF;
        public static final int FACTOR_MASK = 0x1F;

        public static int getState(BlendInfo blendInfo) {
            int s = 0;
            s |= blendInfo.enabled ? ENABLE_BIT : 0;
            s |= encode(blendInfo.srcRgbFactor, SRC_RGB_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.dstRgbFactor, DST_RGB_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.srcAlphaFactor, SRC_A_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.dstAlphaFactor, DST_A_OFFSET, FACTOR_MASK);
            s |= encode(blendInfo.blendOp, FUN_OFFSET, OP_MASK);

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
            return decode(state, FUN_OFFSET, OP_MASK);
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
                case GL11.GL_AND -> VK_LOGIC_OP_AND;
                case GL11.GL_AND_REVERSE -> VK_LOGIC_OP_AND_REVERSE;
                case GL11.GL_AND_INVERTED -> VK_LOGIC_OP_AND_INVERTED;
                case GL11.GL_COPY -> VK_LOGIC_OP_COPY;
                case GL11.GL_NOOP -> VK_LOGIC_OP_NO_OP;
                case GL11.GL_XOR -> VK_LOGIC_OP_XOR;
                case GL11.GL_OR -> VK_LOGIC_OP_OR;
                case GL11.GL_NOR -> VK_LOGIC_OP_NOR;
                case GL11.GL_EQUIV -> VK_LOGIC_OP_EQUIVALENT;
                case GL11.GL_INVERT -> VK_LOGIC_OP_INVERT;
                case GL11.GL_OR_REVERSE -> VK_LOGIC_OP_OR_REVERSE;
                case GL11.GL_COPY_INVERTED -> VK_LOGIC_OP_COPY_INVERTED;
                case GL11.GL_OR_INVERTED -> VK_LOGIC_OP_OR_INVERTED;
                case GL11.GL_NAND -> VK_LOGIC_OP_NAND;
                case GL11.GL_SET -> VK_LOGIC_OP_SET;

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

        public static int encode(boolean cull, int topology, int polygonMode) {
            int state = (polygonMode | (topology << TOPOLOGY_OFFSET));
            state |= ((cull ? VK_CULL_MODE_BACK_BIT : VK_CULL_MODE_NONE) << CULL_MODE_OFFSET);

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
                case GL11.GL_NEVER -> VK_COMPARE_OP_NEVER;
                case GL11.GL_LESS -> VK_COMPARE_OP_LESS;
                case GL11.GL_EQUAL -> VK_COMPARE_OP_EQUAL;
                case GL11.GL_LEQUAL -> VK_COMPARE_OP_LESS_OR_EQUAL;
                case GL11.GL_GREATER -> VK_COMPARE_OP_GREATER;
                case GL11.GL_NOTEQUAL -> VK_COMPARE_OP_NOT_EQUAL;
                case GL11.GL_GEQUAL -> VK_COMPARE_OP_GREATER_OR_EQUAL;
                case GL11.GL_ALWAYS -> VK_COMPARE_OP_ALWAYS;
                default -> throw new RuntimeException("unknown blend factor: %d".formatted(value));
            };
        }

    }
}
