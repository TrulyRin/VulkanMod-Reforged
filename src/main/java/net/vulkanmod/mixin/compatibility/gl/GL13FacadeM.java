package net.vulkanmod.mixin.compatibility.gl;

import org.lwjgl.opengl.GL13;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(GL13.class)
public class GL13FacadeM {
    @Overwrite(remap = false)
    public static void glClientActiveTexture(@NativeType("GLenum") int texture) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1f(@NativeType("GLenum") int target, @NativeType("GLfloat") float s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1s(@NativeType("GLenum") int target, @NativeType("GLshort") short s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1i(@NativeType("GLenum") int target, @NativeType("GLint") int s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1d(@NativeType("GLenum") int target, @NativeType("GLdouble") double s) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1iv(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2f(@NativeType("GLenum") int target, @NativeType("GLfloat") float s, @NativeType("GLfloat") float t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2s(@NativeType("GLenum") int target, @NativeType("GLshort") short s, @NativeType("GLshort") short t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2i(@NativeType("GLenum") int target, @NativeType("GLint") int s, @NativeType("GLint") int t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2d(@NativeType("GLenum") int target, @NativeType("GLdouble") double s, @NativeType("GLdouble") double t) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2iv(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3f(@NativeType("GLenum") int target, @NativeType("GLfloat") float s, @NativeType("GLfloat") float t, @NativeType("GLfloat") float r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3s(@NativeType("GLenum") int target, @NativeType("GLshort") short s, @NativeType("GLshort") short t, @NativeType("GLshort") short r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3i(@NativeType("GLenum") int target, @NativeType("GLint") int s, @NativeType("GLint") int t, @NativeType("GLint") int r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3d(@NativeType("GLenum") int target, @NativeType("GLdouble") double s, @NativeType("GLdouble") double t, @NativeType("GLdouble") double r) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3iv(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4f(@NativeType("GLenum") int target, @NativeType("GLfloat") float s, @NativeType("GLfloat") float t, @NativeType("GLfloat") float r, @NativeType("GLfloat") float q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4s(@NativeType("GLenum") int target, @NativeType("GLshort") short s, @NativeType("GLshort") short t, @NativeType("GLshort") short r, @NativeType("GLshort") short q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4i(@NativeType("GLenum") int target, @NativeType("GLint") int s, @NativeType("GLint") int t, @NativeType("GLint") int r, @NativeType("GLint") int q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4d(@NativeType("GLenum") int target, @NativeType("GLdouble") double s, @NativeType("GLdouble") double t, @NativeType("GLdouble") double r, @NativeType("GLdouble") double q) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4iv(@NativeType("GLenum") int target, @NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1iv(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord1dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2iv(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord2dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3iv(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord3dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4fv(@NativeType("GLenum") int target, @NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4sv(@NativeType("GLenum") int target, @NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4iv(@NativeType("GLenum") int target, @NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glMultiTexCoord4dv(@NativeType("GLenum") int target, @NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glLoadTransposeMatrixf(@NativeType("GLfloat const *") FloatBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glLoadTransposeMatrixd(@NativeType("GLdouble const *") DoubleBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glMultTransposeMatrixf(@NativeType("GLfloat const *") FloatBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glMultTransposeMatrixd(@NativeType("GLdouble const *") DoubleBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glLoadTransposeMatrixf(@NativeType("GLfloat const *") float[] m) {
    }

    @Overwrite(remap = false)
    public static void glLoadTransposeMatrixd(@NativeType("GLdouble const *") double[] m) {
    }

    @Overwrite(remap = false)
    public static void glMultTransposeMatrixf(@NativeType("GLfloat const *") float[] m) {
    }

    @Overwrite(remap = false)
    public static void glMultTransposeMatrixd(@NativeType("GLdouble const *") double[] m) {
    }
}
