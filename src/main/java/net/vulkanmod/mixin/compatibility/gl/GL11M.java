package net.vulkanmod.mixin.compatibility.gl;

import net.minecraft.client.Minecraft;
import net.vulkanmod.compat.gl.GlCapabilityState;
import net.vulkanmod.compat.gl.GlIntegerState;
import net.vulkanmod.compat.gl.GlPixelStore;
import net.vulkanmod.compat.opengl.GlDrawContract;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlEmulationLog;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.gl.GlVertexArray;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.texture.ImageUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import org.jetbrains.annotations.Nullable;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(GL11C.class)
public class GL11M {
    private static final int[] viewport = new int[4];

    @Overwrite(remap = false)
    public static void glScissor(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        Renderer.setScissor(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void glBindTexture(@NativeType("GLenum") int target, @NativeType("GLuint") int texture) {
        GlTexture.bindTexture(texture);
    }

    @Overwrite(remap = false)
    public static void glLineWidth(@NativeType("GLfloat") float width) {
        VRenderSystem.setLineWidth(width);
    }

    @NativeType("void")
    @Overwrite(remap = false)
    public static int glGenTextures() {
        return GlTexture.genTextureId();
    }

    @NativeType("GLboolean")
    @Overwrite(remap = false)
    public static boolean glIsEnabled(@NativeType("GLenum") int cap) {
        return GlCapabilityState.isEnabled(cap);
    }

    @Overwrite(remap = false)
    public static void glClear(@NativeType("GLbitfield") int mask) {
        VRenderSystem.clear(mask);
    }

    @NativeType("GLenum")
    @Overwrite(remap = false)
    public static int glGetError() {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glClearColor(@NativeType("GLfloat") float red, @NativeType("GLfloat") float green, @NativeType("GLfloat") float blue, @NativeType("GLfloat") float alpha) {
        VRenderSystem.setClearColor(red, green, blue, alpha);
    }

    @Overwrite(remap = false)
    public static void glDepthMask(@NativeType("GLboolean") boolean flag) {
        VRenderSystem.depthMask(flag);
    }

    @NativeType("void")
    @Overwrite(remap = false)
    public static int glGetInteger(@NativeType("GLenum") int pname) {
        if (pname == GL11.GL_VIEWPORT) {
            ensureViewport();
            return viewport[0];
        }
        if (GlPixelStore.isPixelStoreParameter(pname)) {
            return GlPixelStore.getInteger(pname);
        }
        return GlIntegerState.getInteger(pname);
    }

    @Overwrite(remap = false)
    public static String glGetString(@NativeType("GLenum") int name) {
        return switch (name) {
            case GL11.GL_VERSION -> "3.2.0 VulkanMod Compatibility";
            case GL11.GL_VENDOR -> "VulkanMod";
            case GL11.GL_RENDERER -> "VulkanMod Vulkan Renderer";
            case GL20.GL_SHADING_LANGUAGE_VERSION -> "1.50 VulkanMod Compatibility";
            case GL11.GL_EXTENSIONS -> "";
            default -> "";
        };
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        GlTexture.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") long pixels) {
        GlTexture.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, long pixels) {
        GlTexture.texSubImage2D(target, level, xOffset, yOffset, width, height, format, type, pixels);
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, @Nullable ByteBuffer pixels) {
        GlTexture.texSubImage2D(target, level, xOffset, yOffset, width, height, format, type, pixels);
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, @Nullable IntBuffer pixels) {
        GlTexture.texSubImage2D(target, level, xOffset, yOffset, width, height, format, type, MemoryUtil.memByteBuffer(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {

        if (target == GL11.GL_TEXTURE_2D) {
            GlTexture.texParameteri(target, pname, param);
        }
    }

    @Overwrite(remap = false)
    public static void glTexParameterf(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
        if (target == GL11.GL_TEXTURE_2D) {
            GlTexture.texParameteri(target, pname, (int) param);
        }
    }

    @Overwrite(remap = false)
    public static int glGetTexLevelParameteri(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int pname) {
        return GlTexture.getTexLevelParameter(target, level, pname);
    }

    @Overwrite(remap = false)
    public static void glEnable(@NativeType("GLenum") int target) {
        switch (target) {
            case GL11.GL_DEPTH_TEST -> VRenderSystem.enableDepthTest();
            case GL11.GL_STENCIL_TEST -> VRenderSystem.enableStencilTest();
            case GL11.GL_CULL_FACE -> VRenderSystem.enableCull();
            case GL11.GL_BLEND -> VRenderSystem.enableBlend();
            case GL11.GL_SCISSOR_TEST -> Renderer.setScissorEnabled(true);
            default -> {}
        }
    }

    @Overwrite(remap = false)
    public static void glDisable(@NativeType("GLenum") int target) {
        switch (target) {
            case GL11.GL_DEPTH_TEST -> VRenderSystem.disableDepthTest();
            case GL11.GL_STENCIL_TEST -> VRenderSystem.disableStencilTest();
            case GL11.GL_CULL_FACE -> VRenderSystem.disableCull();
            case GL11.GL_BLEND -> VRenderSystem.disableBlend();
            case GL11.GL_SCISSOR_TEST -> Renderer.setScissorEnabled(false);
            default -> {}
        }
    }

    @Overwrite(remap = false)
    public static void glFinish() {
    }

    @Overwrite(remap = false)
    public static void glFlush() {
    }

    @Overwrite(remap = false)
    public static void glHint(@NativeType("GLenum") int target, @NativeType("GLenum") int hint) {
    }

    @Overwrite(remap = false)
    public static void glDeleteTextures(@NativeType("GLuint const *") int texture) {
        GlTexture.glDeleteTextures(texture);
    }

    @Overwrite(remap = false)
    public static void glDeleteTextures(@NativeType("GLuint const *") IntBuffer textures) {
        GlTexture.glDeleteTextures(textures);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") long pixels) {
        GlTexture.getTexImage(tex, level, format, type, pixels);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") ByteBuffer pixels) {
        GlTexture.getTexImage(tex, level, format, type, MemoryUtil.memAddress(pixels));
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") IntBuffer pixels) {
        GlTexture.getTexImage(tex, level, format, type, MemoryUtil.memAddress(pixels));
    }

    @Overwrite(remap = false)
    public static void glCopyTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {

    }

    @Overwrite(remap = false)
    public static void glViewport(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height) {
        viewport[0] = x;
        viewport[1] = y;
        viewport[2] = width;
        viewport[3] = height;
        net.vulkanmod.vulkan.Renderer.setViewport(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void glGetIntegerv(@NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (pname == GL11.GL_VIEWPORT) {
            ensureViewport();
            for (int i = 0; i < 4 && params.position() + i < params.limit(); i++) {
                params.put(params.position() + i, viewport[i]);
            }
        } else {
            int count = GlIntegerState.getComponentCount(pname);
            for (int i = 0; i < count && params.position() + i < params.limit(); i++) {
                params.put(params.position() + i, GlIntegerState.getInteger(pname, i));
            }
        }
    }

    @Overwrite(remap = false)
    public static void glGetFloatv(@NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer params) {
        if (pname == GL11.GL_VIEWPORT) {
            ensureViewport();
            for (int i = 0; i < 4 && params.position() + i < params.limit(); i++) {
                params.put(params.position() + i, viewport[i]);
            }
        } else if (pname == GL_COLOR_CLEAR_VALUE) {
            for (int i = 0; i < 4 && params.position() + i < params.limit(); i++) {
                params.put(params.position() + i, VRenderSystem.clearColor.get(i));
            }
        } else if (pname == GL11.GL_DEPTH_CLEAR_VALUE) {
            if (params.remaining() > 0) {
                params.put(params.position(), VRenderSystem.clearDepthValue);
            }
        } else {
            int count = GlIntegerState.getComponentCount(pname);
            for (int i = 0; i < count && params.position() + i < params.limit(); i++) {
                params.put(params.position() + i, GlIntegerState.getInteger(pname, i));
            }
        }
    }

    @Overwrite(remap = false)
    public static void glGetFloatv(@NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] params) {
        if (pname == GL11.GL_VIEWPORT) {
            ensureViewport();
            for (int i = 0; i < Math.min(4, params.length); i++) {
                params[i] = viewport[i];
            }
        } else if (pname == GL_COLOR_CLEAR_VALUE) {
            for (int i = 0; i < Math.min(4, params.length); i++) {
                params[i] = VRenderSystem.clearColor.get(i);
            }
        } else if (pname == GL11.GL_DEPTH_CLEAR_VALUE) {
            if (params.length > 0) {
                params[0] = VRenderSystem.clearDepthValue;
            }
        } else {
            int count = Math.min(GlIntegerState.getComponentCount(pname), params.length);
            for (int i = 0; i < count; i++) {
                params[i] = GlIntegerState.getInteger(pname, i);
            }
        }
    }

    @Overwrite(remap = false)
    public static void glGetIntegerv(@NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (pname == GL11.GL_VIEWPORT) {
            ensureViewport();
            System.arraycopy(viewport, 0, params, 0, Math.min(4, params.length));
        } else {
            int count = Math.min(GlIntegerState.getComponentCount(pname), params.length);
            for (int i = 0; i < count; i++) {
                params[i] = GlIntegerState.getInteger(pname, i);
            }
        }
    }

    @Overwrite(remap = false)
    public static boolean glGetBoolean(@NativeType("GLenum") int pname) {
        if (GlIntegerState.isBooleanIntegerState(pname)) {
            return GlIntegerState.getInteger(pname) != GL11.GL_FALSE;
        }

        return GlCapabilityState.isEnabled(pname);
    }

    @Overwrite(remap = false)
    public static void glPolygonMode(@NativeType("GLenum") int face, @NativeType("GLenum") int mode) {
        VRenderSystem.setPolygonModeGL(mode);
    }

    @Overwrite(remap = false)
    public static void glBlendFunc(@NativeType("GLenum") int sfactor, @NativeType("GLenum") int dfactor) {
        VRenderSystem.blendFunc(sfactor, dfactor);
    }

    @Overwrite(remap = false)
    public static void glDepthFunc(@NativeType("GLenum") int func) {
        VRenderSystem.depthFunc(func);
    }

    @Overwrite(remap = false)
    public static void glColorMask(@NativeType("GLboolean") boolean red, @NativeType("GLboolean") boolean green, @NativeType("GLboolean") boolean blue, @NativeType("GLboolean") boolean alpha) {
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    @Overwrite(remap = false)
    public static void glCullFace(@NativeType("GLenum") int mode) {
        VRenderSystem.cullFace(mode);
    }

    @Overwrite(remap = false)
    public static void glFrontFace(@NativeType("GLenum") int mode) {
        VRenderSystem.frontFace(mode);
    }

    @Overwrite(remap = false)
    public static void glStencilFunc(@NativeType("GLenum") int func, @NativeType("GLint") int ref, @NativeType("GLuint") int mask) {
        VRenderSystem.stencilFunc(func, ref, mask);
    }

    @Overwrite(remap = false)
    public static void glStencilMask(@NativeType("GLuint") int mask) {
        VRenderSystem.stencilMask(mask);
    }

    @Overwrite(remap = false)
    public static void glStencilOp(@NativeType("GLenum") int sfail, @NativeType("GLenum") int dpfail, @NativeType("GLenum") int dppass) {
        VRenderSystem.stencilOp(sfail, dpfail, dppass);
    }

    @Overwrite(remap = false)
    public static void glClearStencil(@NativeType("GLint") int stencil) {
        VRenderSystem.clearStencil(stencil);
    }

    @Overwrite(remap = false)
    public static void glClearDepth(@NativeType("GLdouble") double depth) {
        VRenderSystem.clearDepth(depth);
    }

    @Overwrite(remap = false)
    public static void glDrawElements(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("GLenum") int type, @NativeType("void const *") long indices) {
        GlDrawContract.drawElements(mode, count, type, indices);
    }

    @Overwrite(remap = false)
    public static void glDrawArrays(@NativeType("GLenum") int mode, @NativeType("GLint") int first, @NativeType("GLsizei") int count) {
        GlDrawContract.drawArrays(mode, first, count);
    }

    @Overwrite(remap = false)
    public static boolean glIsTexture(@NativeType("GLuint") int texture) {
        return texture == 0 || GlTexture.getTexture(texture) != null;
    }

    @Overwrite(remap = false)
    public static void glPixelStorei(@NativeType("GLenum") int pname, @NativeType("GLint") int param) {
        GlPixelStore.setInteger(pname, param);
    }

    @Overwrite(remap = false)
    public static void glPixelStoref(@NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
        GlPixelStore.setInteger(pname, (int) param);
    }

    @Overwrite(remap = false)
    public static void glDrawBuffer(@NativeType("GLenum") int buf) {
    }

    @Overwrite(remap = false)
    public static void glReadBuffer(@NativeType("GLenum") int src) {
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") ByteBuffer pixels) {
        if (!readPixelsFromFramebuffer(x, y, width, height, format, type, pixels)) {
            zeroReadPixelsFallback(width, height, format, type, pixels);
        }
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") long pixels) {
        if (!readPixelsFromFramebuffer(x, y, width, height, format, type, pixels)) {
            int byteCount = readPixelsByteCount(width, height, format, type);
            if (pixels != 0L && byteCount > 0) {
                MemoryUtil.memSet(pixels, 0, byteCount);
            }
        }
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") IntBuffer pixels) {
        zeroReadPixelsFallback(width, height, format, type, pixels);
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") FloatBuffer pixels) {
        if (!readPixelsFromFramebuffer(x, y, width, height, format, type, pixels)) {
            zeroReadPixelsFallback(width, height, format, type, pixels);
        }
    }

    @Overwrite(remap = false)
    public static void glTexParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
        if (target == GL11.GL_TEXTURE_2D && params != null && params.remaining() > 0) {
            GlTexture.texParameteri(target, pname, params.get(params.position()));
        }
    }

    @Overwrite(remap = false)
    public static void glTexParameterfv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
        if (target == GL11.GL_TEXTURE_2D && params != null && params.remaining() > 0) {
            GlTexture.texParameteri(target, pname, (int) params.get(params.position()));
        }
    }

    @Overwrite(remap = false)
    public static int glGetTexParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlTexture.getTexParameteri(target, pname);
    }

    @Overwrite(remap = false)
    public static float glGetTexParameterf(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlTexture.getTexParameterf(target, pname);
    }

    @Overwrite(remap = false)
    public static void glGetTexParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlTexture.getTexParameteri(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetTexParameterfv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlTexture.getTexParameterf(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glLogicOp(@NativeType("GLenum") int op) {
    }

    @Overwrite(remap = false)
    public static void glPointSize(@NativeType("GLfloat") float size) {
    }

    @Overwrite(remap = false)
    public static void glPolygonOffset(@NativeType("GLfloat") float factor, @NativeType("GLfloat") float units) {
    }

    @Overwrite(remap = false)
    public static void glDepthRange(@NativeType("GLdouble") double zNear, @NativeType("GLdouble") double zFar) {
    }

    @Overwrite(remap = false)
    public static float glGetFloat(@NativeType("GLenum") int pname) {
        if (pname == GL11.GL_LINE_WIDTH) {
            return 1.0f;
        }
        if (pname == GL11.GL_DEPTH_CLEAR_VALUE) {
            return VRenderSystem.clearDepthValue;
        }
        return glGetInteger(pname);
    }

    @Overwrite(remap = false)
    public static double glGetDouble(@NativeType("GLenum") int pname) {
        if (pname == GL11.GL_DEPTH_CLEAR_VALUE) {
            return VRenderSystem.clearDepthValue;
        }
        return glGetInteger(pname);
    }

    @Overwrite(remap = false)
    public static void glGetDoublev(@NativeType("GLenum") int pname, @NativeType("GLdouble *") DoubleBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), glGetDouble(pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetDoublev(@NativeType("GLenum") int pname, @NativeType("GLdouble *") double[] params) {
        if (params != null && params.length > 0) {
            params[0] = glGetDouble(pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetBooleanv(@NativeType("GLenum") int pname, @NativeType("GLboolean *") ByteBuffer params) {
        if (params == null || params.remaining() <= 0) {
            return;
        }

        if (GlIntegerState.isBooleanIntegerState(pname)) {
            int count = GlIntegerState.getComponentCount(pname);
            for (int i = 0; i < count && params.position() + i < params.limit(); i++) {
                params.put(params.position() + i, (byte) (GlIntegerState.getInteger(pname, i) != GL11.GL_FALSE ? 1 : 0));
            }
        } else {
            params.put(params.position(), (byte) (GlCapabilityState.isEnabled(pname) ? 1 : 0));
        }
    }

    @Overwrite(remap = false)
    public static long glGetPointer(@NativeType("GLenum") int pname) {
        return 0L;
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") @Nullable ByteBuffer pixels) {
        GlTexture.texImage1D(target, level, internalformat, width, border, format, type);
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") long pixels) {
        GlTexture.texImage1D(target, level, internalformat, width, border, format, type);
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") long pixels) {
    }

    @Overwrite(remap = false)
    public static void glCopyTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalFormat, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLint") int border) {
    }

    @Overwrite(remap = false)
    public static void glCopyTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int internalFormat, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border) {
    }

    @Overwrite(remap = false)
    public static void glCopyTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width) {
    }

    private static void ensureViewport() {
        if (Renderer.isViewportInitialized()) {
            for (int i = 0; i < 4; i++) {
                viewport[i] = Renderer.getViewportBox(i);
            }
            return;
        }
        if (viewport[2] == 0 || viewport[3] == 0) {
            var window = Minecraft.getInstance().getWindow();
            viewport[0] = 0;
            viewport[1] = 0;
            viewport[2] = window.getWidth();
            viewport[3] = window.getHeight();
        }
    }

    private static void zeroReadPixelsFallback(int width, int height, int format, int type, ByteBuffer pixels) {
        GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels",
                "glReadPixels real Vulkan readback is not implemented yet; returning safe fallback data");
        if (pixels == null) {
            return;
        }

        int byteCount = Math.min(readPixelsByteCount(width, height, format, type), pixels.remaining());
        for (int i = 0; i < byteCount; i++) {
            pixels.put(pixels.position() + i, (byte) 0);
        }
    }

    private static boolean readPixelsFromFramebuffer(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
        if (pixels == null) {
            return true;
        }
        if (pixels.remaining() < readPixelsByteCount(width, height, format, type)) {
            return false;
        }

        return readPixelsFromFramebuffer(x, y, width, height, format, type, MemoryUtil.memAddress(pixels));
    }

    private static boolean readPixelsFromFramebuffer(int x, int y, int width, int height, int format, int type, FloatBuffer pixels) {
        if (pixels == null) {
            return true;
        }
        if (pixels.remaining() < elementsForByteCount(readPixelsByteCount(width, height, format, type), Float.BYTES)) {
            return false;
        }

        return readPixelsFromFramebuffer(x, y, width, height, format, type, MemoryUtil.memAddress(pixels));
    }

    private static boolean readPixelsFromFramebuffer(int x, int y, int width, int height, int format, int type, long pixels) {
        if (pixels == 0L || width <= 0 || height <= 0) {
            return true;
        }
        if (!isSupportedReadPixelsFormat(format, type)) {
            return false;
        }

        GlFramebuffer.ReadPixelsImage readImage = GlFramebuffer.resolveReadPixelsImage(format);
        if (readImage == null) {
            return false;
        }

        try {
            ImageUtil.downloadTextureRegion(readImage.image(), pixels, x, y, width, height, readImage.aspectMask(), bytesPerPixel(format, type));
            return true;
        } catch (RuntimeException e) {
            GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels.downloadFailed",
                    "glReadPixels Vulkan readback failed for format 0x{} type 0x{}; returning safe fallback data",
                    Integer.toHexString(format), Integer.toHexString(type));
            return false;
        }
    }

    private static boolean isSupportedReadPixelsFormat(int format, int type) {
        return (format == GL11.GL_RGBA && type == GL11.GL_UNSIGNED_BYTE)
                || (format == GL11.GL_DEPTH_COMPONENT && type == GL11.GL_FLOAT);
    }

    private static void zeroReadPixelsFallback(int width, int height, int format, int type, IntBuffer pixels) {
        GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels",
                "glReadPixels real Vulkan readback is not implemented yet; returning safe fallback data");
        if (pixels == null) {
            return;
        }

        int elements = Math.min(elementsForByteCount(readPixelsByteCount(width, height, format, type), Integer.BYTES), pixels.remaining());
        for (int i = 0; i < elements; i++) {
            pixels.put(pixels.position() + i, 0);
        }
    }

    private static void zeroReadPixelsFallback(int width, int height, int format, int type, FloatBuffer pixels) {
        GlEmulationLog.warnContractGap("framebuffer_readback", "glReadPixels",
                "glReadPixels real Vulkan readback is not implemented yet; returning safe fallback data");
        if (pixels == null) {
            return;
        }

        int elements = Math.min(elementsForByteCount(readPixelsByteCount(width, height, format, type), Float.BYTES), pixels.remaining());
        for (int i = 0; i < elements; i++) {
            pixels.put(pixels.position() + i, 0.0f);
        }
    }

    private static int readPixelsByteCount(int width, int height, int format, int type) {
        if (width <= 0 || height <= 0) {
            return 0;
        }

        long byteCount = (long) width * height * bytesPerPixel(format, type);
        return byteCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) byteCount;
    }

    private static int bytesPerPixel(int format, int type) {
        int packedSize = packedTypeBytes(type);
        if (packedSize > 0) {
            return packedSize;
        }

        return componentsForFormat(format) * bytesPerComponent(type);
    }

    private static int componentsForFormat(int format) {
        return switch (format) {
            case GL11.GL_RED, GL11.GL_GREEN, GL11.GL_BLUE, GL11.GL_ALPHA,
                 GL11.GL_LUMINANCE, GL11.GL_DEPTH_COMPONENT, GL11.GL_STENCIL_INDEX -> 1;
            case GL11.GL_LUMINANCE_ALPHA -> 2;
            case GL11.GL_RGB, GL12.GL_BGR -> 3;
            case GL11.GL_RGBA, GL12.GL_BGRA -> 4;
            default -> 4;
        };
    }

    private static int bytesPerComponent(int type) {
        return switch (type) {
            case GL11.GL_BYTE, GL11.GL_UNSIGNED_BYTE -> 1;
            case GL11.GL_SHORT, GL11.GL_UNSIGNED_SHORT, GL30.GL_HALF_FLOAT -> 2;
            case GL11.GL_INT, GL11.GL_UNSIGNED_INT, GL11.GL_FLOAT -> 4;
            default -> 1;
        };
    }

    private static int packedTypeBytes(int type) {
        return switch (type) {
            case GL12.GL_UNSIGNED_BYTE_3_3_2, GL12.GL_UNSIGNED_BYTE_2_3_3_REV -> 1;
            case GL12.GL_UNSIGNED_SHORT_5_6_5, GL12.GL_UNSIGNED_SHORT_5_6_5_REV,
                 GL12.GL_UNSIGNED_SHORT_4_4_4_4, GL12.GL_UNSIGNED_SHORT_4_4_4_4_REV,
                 GL12.GL_UNSIGNED_SHORT_5_5_5_1, GL12.GL_UNSIGNED_SHORT_1_5_5_5_REV -> 2;
            case GL12.GL_UNSIGNED_INT_8_8_8_8, GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
                 GL12.GL_UNSIGNED_INT_10_10_10_2, GL12.GL_UNSIGNED_INT_2_10_10_10_REV -> 4;
            default -> 0;
        };
    }

    private static int elementsForByteCount(int byteCount, int elementSize) {
        if (byteCount <= 0) {
            return 0;
        }

        return (byteCount + elementSize - 1) / elementSize;
    }

    private static final int GL13_ACTIVE_TEXTURE = 0x84E0;
    private static final int GL15_BINDING_ARRAY = 0x8894;
    private static final int GL15_BINDING_ELEMENT_ARRAY = 0x8895;
    private static final int GL_COPY_READ_BUFFER_BINDING = 0x8F36;
    private static final int GL_COPY_WRITE_BUFFER_BINDING = 0x8F37;
    private static final int GL_BLEND_EQUATION_RGB = 0x8009;
    private static final int GL_BLEND_EQUATION_ALPHA = 0x883D;
    private static final int GL_COLOR_CLEAR_VALUE = 0x0C22;
    private static final int GL_STENCIL_CLEAR_VALUE = 0x0B91;
    private static final int GL_STENCIL_FUNC = 0x0B92;
    private static final int GL_STENCIL_VALUE_MASK = 0x0B93;
    private static final int GL_STENCIL_FAIL = 0x0B94;
    private static final int GL_STENCIL_PASS_DEPTH_FAIL = 0x0B95;
    private static final int GL_STENCIL_PASS_DEPTH_PASS = 0x0B96;
    private static final int GL_STENCIL_REF = 0x0B97;
    private static final int GL_STENCIL_WRITEMASK = 0x0B98;
}
