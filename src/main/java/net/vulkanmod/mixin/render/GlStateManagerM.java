package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.compat.gl.GlIntegerState;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(GlStateManager.class)
public class GlStateManagerM {

    @Shadow private static int activeTexture;

    @Overwrite
    public static void _bindTexture(int i) {

        GlTexture.bindTexture(i);
    }

    @Overwrite
    public static void _activeTexture(int i) {
        activeTexture = i - 33984;
        GlTexture.activeTexture(i);
    }

    @Overwrite(remap = false)
    public static void _disableBlend() {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.disableBlend();
    }

    @Overwrite(remap = false)
    public static void _enableBlend() {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.enableBlend();
    }

    @Overwrite(remap = false)
    public static void _blendFunc(int i, int j) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.blendFunc(i, j);

    }

    @Overwrite(remap = false)
    public static void _blendFuncSeparate(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.blendFuncSeparate(i, j, k, l);

    }

    @Overwrite(remap = false)
    public static void _disableScissorTest() {
        Renderer.setScissorEnabled(false);
    }

    @Overwrite(remap = false)
    public static void _enableScissorTest() {
        Renderer.setScissorEnabled(true);
    }

    @Overwrite(remap = false)
    public static void _enableCull() {
        VRenderSystem.enableCull();
    }

    @Overwrite(remap = false)
    public static void _disableCull() {
        VRenderSystem.disableCull();
    }

    @Overwrite(remap = false)
    public static void _viewport(int x, int y, int width, int height) {
        Renderer.setGlViewport(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void _scissorBox(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static int _getError() {
        return 0;
    }

    @Overwrite(remap = false)
    public static int _getInteger(int pname) {
        return GlIntegerState.getInteger(pname);
    }

    @Overwrite(remap = false)
    public static String _getString(int name) {
        return switch (name) {
            case GL11.GL_VERSION -> "3.2.0 VulkanMod Compatibility";
            case GL11.GL_VENDOR -> "VulkanMod";
            case GL11.GL_RENDERER -> "VulkanMod Vulkan Renderer";
            case GL11.GL_EXTENSIONS -> "";
            default -> "";
        };
    }

    @Overwrite(remap = false)
    public static void _texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable IntBuffer pixels) {
        GlTexture.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels != null ? MemoryUtil.memByteBuffer(pixels) : null);
    }

    @Overwrite(remap = false)
    public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {

    }

    @Overwrite(remap = false)
    public static void _texParameter(int i, int j, int k) {
        GlTexture.texParameteri(i, j, k);
    }

    @Overwrite(remap = false)
    public static void _texParameter(int i, int j, float k) {

    }

    @Overwrite(remap = false)
    public static int _getTexLevelParameter(int i, int j, int k) {
        return GlTexture.getTexLevelParameter(i, j, k);
    }

    @Overwrite(remap = false)
    public static void _pixelStore(int pname, int param) {

    }

    @Overwrite(remap = false)
    public static int _genTexture() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlTexture.genTextureId();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "upload", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private static void vulkanMod$onUpload(int level, int xOffset, int yOffset, int width, int height, com.mojang.blaze3d.platform.NativeImage.Format format, java.nio.IntBuffer intBuffer, java.util.function.Consumer<java.nio.IntBuffer> consumer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        RenderSystem.assertOnRenderThreadOrInit();
        net.vulkanmod.vulkan.texture.VTextureSelector.uploadSubTexture(level, width, height, xOffset, yOffset, 0, 0, width, org.lwjgl.system.MemoryUtil.memByteBuffer(intBuffer));
        consumer.accept(intBuffer);
        ci.cancel();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "_upload", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    private static void vulkanMod$on_Upload(int level, int xOffset, int yOffset, int width, int height, com.mojang.blaze3d.platform.NativeImage.Format format, java.nio.IntBuffer intBuffer, java.util.function.Consumer<java.nio.IntBuffer> consumer, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        RenderSystem.assertOnRenderThreadOrInit();
        net.vulkanmod.vulkan.texture.VTextureSelector.uploadSubTexture(level, width, height, xOffset, yOffset, 0, 0, width, org.lwjgl.system.MemoryUtil.memByteBuffer(intBuffer));
        consumer.accept(intBuffer);
        ci.cancel();
    }

    @Overwrite(remap = false)
    public static void _deleteTexture(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.glDeleteTextures(i);
    }

    @Overwrite(remap = false)
    public static void _colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    @Overwrite(remap = false)
    public static void _depthFunc(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.depthFunc(i);
    }

    @Overwrite(remap = false)
    public static void _clearColor(float f, float g, float h, float i) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.setClearColor(f, g, h, i);
    }

    @Overwrite(remap = false)
    public static void _clearDepth(double d) {
        VRenderSystem.clearDepth(d);
    }

    @Overwrite(remap = false)
    public static void _stencilFunc(int func, int ref, int mask) {
        VRenderSystem.stencilFunc(func, ref, mask);
    }

    @Overwrite(remap = false)
    public static void _stencilMask(int mask) {
        VRenderSystem.stencilMask(mask);
    }

    @Overwrite(remap = false)
    public static void _stencilOp(int sfail, int dpfail, int dppass) {
        VRenderSystem.stencilOp(sfail, dpfail, dppass);
    }

    @Overwrite(remap = false)
    public static void _clearStencil(int stencil) {
        VRenderSystem.clearStencil(stencil);
    }

    @Overwrite(remap = false)
    public static void _clear(int mask, boolean bl) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.clear(mask);
    }

    @Overwrite(remap = false)
    public static void _glUseProgram(int i) {
        GlProgram.useProgram(i);
    }

    @Overwrite(remap = false)
    public static void _disableDepthTest() {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.disableDepthTest();
    }

    @Overwrite(remap = false)
    public static void _enableDepthTest() {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.enableDepthTest();
    }

    @Overwrite(remap = false)
    public static void _depthMask(boolean bl) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.depthMask(bl);

    }

    @Inject(method = "glGenFramebuffers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void genFramebuffers(CallbackInfoReturnable<Integer> cir) {
        RenderSystem.assertOnRenderThreadOrInit();
        cir.setReturnValue(GlFramebuffer.genFramebufferId());
    }

    @Inject(method = "glGenRenderbuffers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void genRenderbuffers(CallbackInfoReturnable<Integer> cir) {
        RenderSystem.assertOnRenderThreadOrInit();
        cir.setReturnValue(GlRenderbuffer.genId());
    }

    @Overwrite(remap = false)
    public static void _glBindFramebuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.bindFramebuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glFramebufferTexture2D(int i, int j, int k, int l, int m) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.framebufferTexture2D(i, j, k, l, m);
    }

    @Overwrite(remap = false)
    public static void _glDeleteFramebuffers(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.deleteFramebuffer(i);
    }

    @Overwrite(remap = false)
    public static void _glDeleteRenderbuffers(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlRenderbuffer.deleteRenderbuffer(i);
    }

    @Overwrite(remap = false)
    public static void _glBindRenderbuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlRenderbuffer.bindRenderbuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glFramebufferRenderbuffer(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.framebufferRenderbuffer(i, j, k, l);
    }

    @Overwrite(remap = false)
    public static void _glRenderbufferStorage(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlRenderbuffer.renderbufferStorage(i, j, k, l);
    }

    @Overwrite(remap = false)
    public static int glCheckFramebufferStatus(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlFramebuffer.glCheckFramebufferStatus(i);
    }

    @Inject(method = "_glGenBuffers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void genBuffers(CallbackInfoReturnable<Integer> cir) {
        RenderSystem.assertOnRenderThreadOrInit();
        cir.setReturnValue(GlBuffer.glGenBuffers());
    }

    @Overwrite(remap = false)
    public static void _glBindBuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBindBuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glBufferData(int i, ByteBuffer byteBuffer, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBufferData(i, byteBuffer, j);
    }

    @Overwrite(remap = false)
    public static void _glBufferData(int i, long l, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBufferData(i, l, j);
    }

    @Overwrite(remap = false)
    @Nullable
    public static ByteBuffer _glMapBuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlBuffer.glMapBuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glUnmapBuffer(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glUnmapBuffer(i);
    }

    @Overwrite(remap = false)
    public static void _glDeleteBuffers(int i) {
        RenderSystem.assertOnRenderThread();
        GlBuffer.glDeleteBuffers(i);
    }

    @Overwrite(remap = false)
    public static void _disableVertexAttribArray(int i) {}
}
