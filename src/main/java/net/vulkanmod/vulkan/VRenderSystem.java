package net.vulkanmod.vulkan;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.util.VUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.vulkan.VK10.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public abstract class VRenderSystem {
    private static final float DEFAULT_DEPTH_VALUE = 1.0f;

    private static long window;

    public static boolean depthTest = true;
    public static boolean depthMask = true;
    public static int depthFun = 515;
    public static boolean stencilTest = false;
    public static int stencilFunc = GL11.GL_ALWAYS;
    public static int stencilRef = 0;
    public static int stencilFuncMask = 0xFF;
    public static int stencilFailOp = GL11.GL_KEEP;
    public static int stencilDepthFailOp = GL11.GL_KEEP;
    public static int stencilPassOp = GL11.GL_KEEP;
    public static int stencilWriteMask = 0xFF;
    public static int topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
    public static int polygonMode = VK_POLYGON_MODE_FILL;
    public static int cullFace = VK_CULL_MODE_BACK_BIT;
    public static int frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
    public static boolean canSetLineWidth = false;

    public static int colorMask = PipelineState.ColorMask.getColorMask(true, true, true, true);

    public static boolean cull = true;

    public static boolean logicOp = false;
    public static int logicOpFun = 0;

    public static float clearDepthValue = DEFAULT_DEPTH_VALUE;
    public static int clearStencilValue = 0;
    public static FloatBuffer clearColor = MemoryUtil.memCallocFloat(4);

    public static MappedBuffer modelViewMatrix = new MappedBuffer(16 * 4);
    public static MappedBuffer projectionMatrix = new MappedBuffer(16 * 4);
    public static MappedBuffer TextureMatrix = new MappedBuffer(16 * 4);
    public static MappedBuffer MVP = new MappedBuffer(16 * 4);
    private static final Matrix4f vulkanProjectionMatrix = new Matrix4f();

    private static final FloatBuffer modelViewFloatView = modelViewMatrix.buffer.asFloatBuffer();
    private static final FloatBuffer projectionFloatView = projectionMatrix.buffer.asFloatBuffer();
    private static final FloatBuffer textureMatrixFloatView = TextureMatrix.buffer.asFloatBuffer();
    private static final Matrix4f scratchModelView = new Matrix4f();
    private static final Matrix4f scratchMVP = new Matrix4f();

    public static MappedBuffer ChunkOffset = new MappedBuffer(3 * 4);
    public static MappedBuffer lightDirection0 = new MappedBuffer(3 * 4);
    public static MappedBuffer lightDirection1 = new MappedBuffer(3 * 4);

    public static MappedBuffer shaderColor = new MappedBuffer(4 * 4);
    public static MappedBuffer shaderFogColor = new MappedBuffer(4 * 4);
    public static FloatBuffer blendColor = MemoryUtil.memCallocFloat(4);

    public static MappedBuffer screenSize = new MappedBuffer(2 * 4);

    public static float alphaCutout = 0.0f;

    private static final float[] depthBias = new float[2];

    public static void initRenderer() {
        Vulkan.initVulkan(window);
    }

    public static MappedBuffer getScreenSize() {
        updateScreenSize();
        return screenSize;
    }

    public static void updateScreenSize() {
        Window window = Minecraft.getInstance().getWindow();

        screenSize.putFloat(0, (float) window.getWidth());
        screenSize.putFloat(4, (float) window.getHeight());
    }

    public static void setWindow(long window) {
        VRenderSystem.window = window;
    }

    public static ByteBuffer getChunkOffset() {
        return ChunkOffset.buffer;
    }

    public static int maxSupportedTextureSize() {
        return DeviceManager.deviceProperties.limits().maxImageDimension2D();
    }

    public static void applyMVP(Matrix4f MV, Matrix4f P) {
        applyModelViewMatrix(MV);
        applyProjectionMatrix(P);
        calculateMVP();
    }

    public static void applyModelViewMatrix(Matrix4f mat) {
        mat.get(modelViewFloatView);

    }

    public static void applyProjectionMatrix(Matrix4f mat) {
        vulkanProjectionMatrix.set(mat);

        vulkanProjectionMatrix.m02((mat.m02() + mat.m03()) * 0.5F);
        vulkanProjectionMatrix.m12((mat.m12() + mat.m13()) * 0.5F);
        vulkanProjectionMatrix.m22((mat.m22() + mat.m23()) * 0.5F);
        vulkanProjectionMatrix.m32((mat.m32() + mat.m33()) * 0.5F);
        vulkanProjectionMatrix.get(projectionFloatView);
    }

    public static void calculateMVP() {
        scratchModelView.set(modelViewFloatView);
        scratchMVP.set(projectionFloatView);

        scratchMVP.mul(scratchModelView).get(MVP.buffer);
    }

    public static void setTextureMatrix(Matrix4f mat) {
        mat.get(textureMatrixFloatView);
    }

    public static MappedBuffer getTextureMatrix() {
        return TextureMatrix;
    }

    public static MappedBuffer getModelViewMatrix() {
        return modelViewMatrix;
    }

    public static MappedBuffer getProjectionMatrix() {
        return projectionMatrix;
    }

    public static MappedBuffer getMVP() {
        calculateMVP();
        return MVP;
    }

    public static void setChunkOffset(float f1, float f2, float f3) {
        long ptr = ChunkOffset.ptr;
        VUtil.UNSAFE.putFloat(ptr, f1);
        VUtil.UNSAFE.putFloat(ptr + 4, f2);
        VUtil.UNSAFE.putFloat(ptr + 8, f3);
    }

    public static void setShaderColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(shaderColor, f1, f2, f3, f4);
    }

    public static void setShaderFogColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(shaderFogColor, f1, f2, f3, f4);
    }

    public static void blendColor(float red, float green, float blue, float alpha) {
        blendColor.put(0, red);
        blendColor.put(1, green);
        blendColor.put(2, blue);
        blendColor.put(3, alpha);
    }

    public static MappedBuffer getShaderColor() {
        return shaderColor;
    }

    public static MappedBuffer getShaderFogColor() {
        return shaderFogColor;
    }

    public static void setClearColor(float f1, float f2, float f3, float f4) {
        ColorUtil.setRGBA_Buffer(clearColor, f1, f2, f3, f4);
    }

    public static void clear(int mask) {
        Renderer.clearAttachments(mask);
    }

    public static void clearDepth(double depth) {
        clearDepthValue = (float) depth;
    }

    public static void clearStencil(int stencil) {
        clearStencilValue = stencil;
    }

    public static void disableDepthTest() {
        depthTest = false;
    }

    public static void depthMask(boolean b) {
        depthMask = b;
    }

    public static void setPrimitiveTopologyGL(final int mode) {
        VRenderSystem.topology = switch (mode) {
            case GL11.GL_POINTS -> VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
            case GL11.GL_LINES, GL11.GL_LINE_STRIP  -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case GL11.GL_TRIANGLE_FAN, GL11.GL_TRIANGLES, GL11.GL_TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;

            default -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        };
    }

    public static void setPrimitiveTopology(VertexFormat.Mode mode) {
        VRenderSystem.topology = switch (mode) {
            case LINES, LINE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            case DEBUG_LINE_STRIP, DEBUG_LINES -> VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            case QUADS, TRIANGLE_FAN, TRIANGLES, TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            default -> throw new RuntimeException(String.format("Unknown primitive topology: %s", mode));
        };
    }

    public static void setPolygonModeGL(final int mode) {
        VRenderSystem.polygonMode = switch (mode) {
            case GL11.GL_POINT -> VK_POLYGON_MODE_POINT;
            case GL11.GL_LINE -> VK_POLYGON_MODE_LINE;
            case GL11.GL_FILL -> VK_POLYGON_MODE_FILL;
            default -> throw new RuntimeException(String.format("Unknown GL polygon mode: %s", mode));
        };
    }

    public static void cullFace(final int mode) {
        VRenderSystem.cullFace = switch (mode) {
            case GL11.GL_FRONT -> VK_CULL_MODE_FRONT_BIT;
            case GL11.GL_BACK -> VK_CULL_MODE_BACK_BIT;
            case GL11.GL_FRONT_AND_BACK -> VK_CULL_MODE_FRONT_AND_BACK;
            default -> throw new RuntimeException(String.format("Unknown GL cull face: %s", mode));
        };
    }

    public static void frontFace(final int mode) {
        VRenderSystem.frontFace = switch (mode) {
            case GL11.GL_CW -> VK_FRONT_FACE_CLOCKWISE;
            case GL11.GL_CCW -> VK_FRONT_FACE_COUNTER_CLOCKWISE;
            default -> throw new RuntimeException(String.format("Unknown GL front face: %s", mode));
        };
    }

    public static void setLineWidth(final float width) {
        if (canSetLineWidth) {
            Renderer.setLineWidth(width);
        }
    }

    public static void colorMask(boolean b, boolean b1, boolean b2, boolean b3) {
        colorMask = PipelineState.ColorMask.getColorMask(b, b1, b2, b3);
    }

    public static int getColorMask() {
        return colorMask;
    }

    public static void enableDepthTest() {
        depthTest = true;
    }

    public static void enableStencilTest() {
        stencilTest = true;
    }

    public static void disableStencilTest() {
        stencilTest = false;
    }

    public static void enableCull() {
        cull = true;
    }

    public static void disableCull() {
        cull = false;
    }

    public static void depthFunc(int depthFun) {
        VRenderSystem.depthFun = depthFun;
    }

    public static void stencilFunc(int func, int ref, int mask) {
        stencilFunc = func;
        stencilRef = ref;
        stencilFuncMask = mask;
    }

    public static void stencilOp(int sfail, int dpfail, int dppass) {
        stencilFailOp = sfail;
        stencilDepthFailOp = dpfail;
        stencilPassOp = dppass;
    }

    public static void stencilMask(int mask) {
        stencilWriteMask = mask;
    }

    public static void enableBlend() {
        PipelineState.blendInfo.enabled = true;
    }

    public static void disableBlend() {
        PipelineState.blendInfo.enabled = false;
    }

    public static void blendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
        PipelineState.blendInfo.setBlendFunction(sourceFactor, destFactor);
    }

    public static void blendFunc(int srcFactor, int dstFactor) {
        PipelineState.blendInfo.setBlendFunction(srcFactor, dstFactor);
    }

    public static void blendFuncSeparate(GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_) {
        PipelineState.blendInfo.setBlendFuncSeparate(p_69417_, p_69418_, p_69419_, p_69420_);
    }

    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        PipelineState.blendInfo.setBlendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
    }

    public static void blendEquation(int mode) {
        PipelineState.blendInfo.setBlendOp(mode);
    }

    public static void blendEquationSeparate(int modeRGB, int modeAlpha) {
        PipelineState.blendInfo.setBlendOpSeparate(modeRGB, modeAlpha);
    }

    public static void enableColorLogicOp() {
        logicOp = true;
    }

    public static void disableColorLogicOp() {
        logicOp = false;
    }

    public static void logicOp(GlStateManager.LogicOp logicOp) {
        logicOpFun = logicOp.value;
    }

    public static void polygonOffset(float v, float v1) {
        depthBias[0] = v;
        depthBias[1] = v1;
    }

    public static void enablePolygonOffset() {
        Renderer.setDepthBias(depthBias[0], depthBias[1]);
    }

    public static void disablePolygonOffset() {
        Renderer.setDepthBias(0.0F, 0.0F);
    }

}

