package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.vulkanmod.compat.observer.GLCallObserver;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

import static com.mojang.blaze3d.systems.RenderSystem.*;

@Mixin(value = RenderSystem.class, priority = 900)
public abstract class RenderSystemMixin {

    @Shadow private static Matrix4f projectionMatrix;
    @Shadow private static Matrix4f savedProjectionMatrix;
    @Shadow @Final private static Matrix4fStack modelViewStack;
    @Shadow private static Matrix4f modelViewMatrix;
    @Shadow private static Matrix4f textureMatrix;
    @Shadow @Final private static int[] shaderTextures;
    @Shadow @Final private static float[] shaderColor;
    @Shadow @Final private static Vector3f[] shaderLightDirections;

    @Shadow @Final private static float[] shaderFogColor;

    @Shadow private static @Nullable Thread renderThread;

    @Shadow
    public static void assertOnRenderThread() {
    }

    @Overwrite(remap = false)
    public static void initRenderer(int debugVerbosity, boolean debugSync) {
        net.vulkanmod.Initializer.LOGGER.info("VulkanMod: RenderSystemMixin.initRenderer called.");
        VRenderSystem.initRenderer();

        net.vulkanmod.compat.opengl.GlCapabilitiesFallback.install();

        renderThread.setPriority(Thread.NORM_PRIORITY + 2);
    }

    @Overwrite(remap = false)
    public static void setupDefaultState(int x, int y, int width, int height) { }

    @Overwrite(remap = false)
    public static void enableColorLogicOp() {
        assertOnRenderThread();
        VRenderSystem.enableColorLogicOp();
    }

    @Overwrite(remap = false)
    public static void disableColorLogicOp() {
        assertOnRenderThread();
        VRenderSystem.disableColorLogicOp();
    }

    @Overwrite
    public static void logicOp(GlStateManager.LogicOp op) {
        assertOnRenderThread();
        VRenderSystem.logicOp(op);
    }

    @Overwrite(remap = false)
    public static void activeTexture(int texture) {
        GlTexture.activeTexture(texture);
    }

    @Overwrite(remap = false)
    public static void glGenBuffers(Consumer<Integer> consumer) {}

    @Overwrite(remap = false)
    public static void glGenVertexArrays(Consumer<Integer> consumer) {}

    @Overwrite(remap = false)
    public static int maxSupportedTextureSize() {
        return VRenderSystem.maxSupportedTextureSize();
    }

    @Overwrite(remap = false)
    public static void clear(int mask, boolean getError) {
        VRenderSystem.clear(mask);
    }

    @Overwrite(remap = false)
    public static void clearColor(float r, float g, float b, float a) {
        VRenderSystem.setClearColor(r, g, b, a);
    }

    @Overwrite(remap = false)
    public static void clearDepth(double d) {
        VRenderSystem.clearDepth(d);
    }

    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
    private static void removeSwapBuffers(long window) {
    }

    @Overwrite(remap = false)
    public static void viewport(int x, int y, int width, int height) {
        Renderer.setGlViewport(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void enableScissor(int x, int y, int width, int height) {
        Renderer.setScissorEnabled(true);
        Renderer.setScissor(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void disableScissor() {
        Renderer.resetScissor();
    }

    @Overwrite(remap = false)
    public static void disableDepthTest() {
        assertOnRenderThread();

        VRenderSystem.disableDepthTest();
    }

    @Overwrite(remap = false)
    public static void enableDepthTest() {
        assertOnRenderThreadOrInit();
        VRenderSystem.enableDepthTest();
    }

    @Overwrite(remap = false)
    public static void depthFunc(int i) {
        assertOnRenderThread();
        VRenderSystem.depthFunc(i);
    }

    @Overwrite(remap = false)
    public static void depthMask(boolean b) {
        assertOnRenderThread();
        VRenderSystem.depthMask(b);
    }

    @Overwrite(remap = false)
    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    @Overwrite(remap = false)
    public static void blendEquation(int i) {
        assertOnRenderThread();
        VRenderSystem.blendEquation(i);
    }

    @Overwrite(remap = false)
    public static void enableBlend() {
        VRenderSystem.enableBlend();
        GLCallObserver.observeCall("enableBlend", "");
    }

    @Overwrite(remap = false)
    public static void disableBlend() {
        VRenderSystem.disableBlend();
        GLCallObserver.observeCall("disableBlend", "");
    }

    @Overwrite(remap = false)
    public static void blendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
        assertOnRenderThread();
        GlStateManager._blendFunc(sourceFactor.value, destFactor.value);
    }

    @Overwrite(remap = false)
    public static void blendFunc(int srcFactor, int dstFactor) {
        assertOnRenderThread();
        GlStateManager._blendFunc(srcFactor, dstFactor);
    }

    @Overwrite(remap = false)
    public static void blendFuncSeparate(GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_) {
        assertOnRenderThread();
        GlStateManager._blendFuncSeparate(p_69417_.value, p_69418_.value, p_69419_.value, p_69420_.value);
        if (GLCallObserver.shouldObserve()) {
            GLCallObserver.observeCall("blendFuncSeparate", "srcRgb=" + p_69417_.value + " dstRgb=" + p_69418_.value
                    + " srcAlpha=" + p_69419_.value + " dstAlpha=" + p_69420_.value);
        }
    }

    @Overwrite(remap = false)
    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        assertOnRenderThread();
        GlStateManager._blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
        if (GLCallObserver.shouldObserve()) {
            GLCallObserver.observeCall("blendFuncSeparate", "srcRgb=" + srcFactorRGB + " dstRgb=" + dstFactorRGB
                    + " srcAlpha=" + srcFactorAlpha + " dstAlpha=" + dstFactorAlpha);
        }
    }

    @Overwrite(remap = false)
    public static void enableCull() {
        assertOnRenderThread();
        VRenderSystem.enableCull();
    }

    @Overwrite(remap = false)
    public static void disableCull() {
        assertOnRenderThread();
        VRenderSystem.disableCull();
    }

    @Overwrite(remap = false)
    public static void polygonMode(final int i, final int j) {
        assertOnRenderThread();
        VRenderSystem.setPolygonModeGL(i);
    }

    @Overwrite(remap = false)
    public static void enablePolygonOffset() {
        assertOnRenderThread();
        VRenderSystem.enablePolygonOffset();
    }

    @Overwrite(remap = false)
    public static void disablePolygonOffset() {
        assertOnRenderThread();
        VRenderSystem.disablePolygonOffset();
    }

    @Overwrite(remap = false)
    public static void polygonOffset(float p_69864_, float p_69865_) {
        assertOnRenderThread();
        VRenderSystem.polygonOffset(p_69864_, p_69865_);
    }

    @Inject(method = "setShaderLights(Lorg/joml/Vector3f;Lorg/joml/Vector3f;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void setShaderLights(Vector3f dir0, Vector3f dir1, CallbackInfo ci) {
        shaderLightDirections[0] = dir0;
        shaderLightDirections[1] = dir1;

        VRenderSystem.lightDirection0.buffer.putFloat(0, dir0.x());
        VRenderSystem.lightDirection0.buffer.putFloat(4, dir0.y());
        VRenderSystem.lightDirection0.buffer.putFloat(8, dir0.z());

        VRenderSystem.lightDirection1.buffer.putFloat(0, dir1.x());
        VRenderSystem.lightDirection1.buffer.putFloat(4, dir1.y());
        VRenderSystem.lightDirection1.buffer.putFloat(8, dir1.z());

        ci.cancel();
    }

    @Overwrite(remap = false)
    private static void _setShaderColor(float r, float g, float b, float a) {
        shaderColor[0] = r;
        shaderColor[1] = g;
        shaderColor[2] = b;
        shaderColor[3] = a;

        VRenderSystem.setShaderColor(r, g, b, a);
        if (GLCallObserver.shouldObserve()) {
            GLCallObserver.observeCall("setShaderColor", "r=" + r + " g=" + g + " b=" + b + " a=" + a);
        }
    }

    @Overwrite(remap = false)
    public static void setShaderFogColor(float f, float g, float h, float i) {
        shaderFogColor[0] = f;
        shaderFogColor[1] = g;
        shaderFogColor[2] = h;
        shaderFogColor[3] = i;

        VRenderSystem.setShaderFogColor(f, g, h, i);
    }

    @Overwrite(remap = false)
    public static void setProjectionMatrix(Matrix4f projectionMatrix, VertexSorting vertexSorting) {
        Matrix4f matrix4f = new Matrix4f(projectionMatrix);
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                RenderSystemMixin.projectionMatrix = matrix4f;

                VRenderSystem.applyProjectionMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            RenderSystemMixin.projectionMatrix = matrix4f;

            VRenderSystem.applyProjectionMatrix(matrix4f);
            VRenderSystem.calculateMVP();
        }

    }

    @Overwrite(remap = false)
    public static void setTextureMatrix(Matrix4f matrix4f) {
        Matrix4f matrix4f2 = new Matrix4f(matrix4f);
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                textureMatrix = matrix4f2;
                VRenderSystem.setTextureMatrix(matrix4f);
            });
        } else {
            textureMatrix = matrix4f2;
            VRenderSystem.setTextureMatrix(matrix4f);
        }
    }

    @Overwrite(remap = false)
    public static void resetTextureMatrix() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> textureMatrix.identity());
        } else {
            textureMatrix.identity();
            VRenderSystem.setTextureMatrix(textureMatrix);
        }
    }

    @Overwrite(remap = false)
    public static void applyModelViewMatrix() {
        Matrix4f matrix4f = new Matrix4f(modelViewStack);
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                modelViewMatrix = matrix4f;

                VRenderSystem.applyModelViewMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            modelViewMatrix = matrix4f;

            VRenderSystem.applyModelViewMatrix(matrix4f);
            VRenderSystem.calculateMVP();
        }

    }

    @Overwrite(remap = false)
    private static void _restoreProjectionMatrix() {
        projectionMatrix = savedProjectionMatrix;

        VRenderSystem.applyProjectionMatrix(projectionMatrix);
        VRenderSystem.calculateMVP();
    }

    @Overwrite(remap = false)
    public static void texParameter(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }
}
