package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.compat.opengl.GlDrawContract;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(GL14C.class)
public class GL14M {

    @Overwrite(remap = false)
    public static void glBlendFuncSeparate(@NativeType("GLenum") int sfactorRGB, @NativeType("GLenum") int dfactorRGB, @NativeType("GLenum") int sfactorAlpha, @NativeType("GLenum") int dfactorAlpha) {
        VRenderSystem.blendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha);
    }

    @Overwrite(remap = false)
    public static void glBlendEquation(@NativeType("GLenum") int mode) {
        VRenderSystem.blendEquation(mode);
    }

    @Overwrite(remap = false)
    public static void glBlendColor(@NativeType("GLfloat") float red, @NativeType("GLfloat") float green, @NativeType("GLfloat") float blue, @NativeType("GLfloat") float alpha) {
        VRenderSystem.blendColor(red, green, blue, alpha);
    }

    @Overwrite(remap = false)
    public static void glMultiDrawArrays(@NativeType("GLenum") int mode, @NativeType("GLint const *") IntBuffer first, @NativeType("GLsizei const *") IntBuffer count) {
        for (int i = first.position(); i < first.limit() && i < count.limit(); i++) {
            GlDrawContract.drawArrays(mode, first.get(i), count.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glMultiDrawArrays(@NativeType("GLenum") int mode, @NativeType("GLint const *") int[] first, @NativeType("GLsizei const *") int[] count) {
        for (int i = 0; i < first.length && i < count.length; i++) {
            GlDrawContract.drawArrays(mode, first[i], count[i]);
        }
    }

    @Overwrite(remap = false)
    public static void glMultiDrawElements(@NativeType("GLenum") int mode, @NativeType("GLsizei const *") IntBuffer count, @NativeType("GLenum") int type, @NativeType("void const **") PointerBuffer indices) {
        for (int i = count.position(); i < count.limit() && i < indices.limit(); i++) {
            GlDrawContract.drawElements(mode, count.get(i), type, indices.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glMultiDrawElements(@NativeType("GLenum") int mode, @NativeType("GLsizei const *") int[] count, @NativeType("GLenum") int type, @NativeType("void const **") PointerBuffer indices) {
        for (int i = 0; i < count.length && i < indices.limit(); i++) {
            GlDrawContract.drawElements(mode, count[i], type, indices.get(i));
        }
    }

    @Overwrite(remap = false)
    public static void glPointParameterf(@NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glPointParameteri(@NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glPointParameterfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glPointParameterfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
    }

    @Overwrite(remap = false)
    public static void glPointParameteriv(@NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glPointParameteriv(@NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
    }
}
