package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlTexture;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(ARBMultitexture.class)
public class ARBMultitextureM {
    @Overwrite(remap = false)
    public static void glActiveTextureARB(@NativeType("GLenum") int texture) {
        GlTexture.activeTexture(texture);
    }

    @Overwrite(remap = false)
    public static void glClientActiveTextureARB(@NativeType("GLenum") int texture) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1fARB(@NativeType("GLenum") int target, @NativeType("GLfloat") float s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1sARB(@NativeType("GLenum") int target, @NativeType("GLshort") short s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1iARB(@NativeType("GLenum") int target, @NativeType("GLint") int s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1dARB(@NativeType("GLenum") int target, @NativeType("GLdouble") double s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2fARB(@NativeType("GLenum") int target, @NativeType("GLfloat") float s, @NativeType("GLfloat") float t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2sARB(@NativeType("GLenum") int target, @NativeType("GLshort") short s, @NativeType("GLshort") short t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2iARB(@NativeType("GLenum") int target, @NativeType("GLint") int s, @NativeType("GLint") int t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2dARB(@NativeType("GLenum") int target, @NativeType("GLdouble") double s, @NativeType("GLdouble") double t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3fARB(@NativeType("GLenum") int target, @NativeType("GLfloat") float s, @NativeType("GLfloat") float t, @NativeType("GLfloat") float r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3sARB(@NativeType("GLenum") int target, @NativeType("GLshort") short s, @NativeType("GLshort") short t, @NativeType("GLshort") short r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3iARB(@NativeType("GLenum") int target, @NativeType("GLint") int s, @NativeType("GLint") int t, @NativeType("GLint") int r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3dARB(@NativeType("GLenum") int target, @NativeType("GLdouble") double s, @NativeType("GLdouble") double t, @NativeType("GLdouble") double r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4fARB(@NativeType("GLenum") int target, @NativeType("GLfloat") float s, @NativeType("GLfloat") float t, @NativeType("GLfloat") float r, @NativeType("GLfloat") float q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4sARB(@NativeType("GLenum") int target, @NativeType("GLshort") short s, @NativeType("GLshort") short t, @NativeType("GLshort") short r, @NativeType("GLshort") short q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4iARB(@NativeType("GLenum") int target, @NativeType("GLint") int s, @NativeType("GLint") int t, @NativeType("GLint") int r, @NativeType("GLint") int q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4dARB(@NativeType("GLenum") int target, @NativeType("GLdouble") double s, @NativeType("GLdouble") double t, @NativeType("GLdouble") double r, @NativeType("GLdouble") double q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4fvARB(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4svARB(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4ivARB(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4dvARB(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }
}
