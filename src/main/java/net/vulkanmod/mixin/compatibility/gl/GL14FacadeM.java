package net.vulkanmod.mixin.compatibility.gl;

import org.lwjgl.opengl.GL14;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

@Mixin(GL14.class)
public class GL14FacadeM {
    @Overwrite(remap = false)
    public static void glFogCoordf(@NativeType("GLfloat") float coord) {
    }

    @Overwrite(remap = false)
    public static void glFogCoordd(@NativeType("GLdouble") double coord) {
    }

    @Overwrite(remap = false)
    public static void glFogCoordfv(@NativeType("GLfloat const *") FloatBuffer coord) {
    }

    @Overwrite(remap = false)
    public static void glFogCoordfv(@NativeType("GLfloat const *") float[] coord) {
    }

    @Overwrite(remap = false)
    public static void glFogCoorddv(@NativeType("GLdouble const *") DoubleBuffer coord) {
    }

    @Overwrite(remap = false)
    public static void glFogCoorddv(@NativeType("GLdouble const *") double[] coord) {
    }

    @Overwrite(remap = false)
    public static void glFogCoordPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glFogCoordPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glFogCoordPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glFogCoordPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3b(@NativeType("GLbyte") byte red, @NativeType("GLbyte") byte green, @NativeType("GLbyte") byte blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3s(@NativeType("GLshort") short red, @NativeType("GLshort") short green, @NativeType("GLshort") short blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3i(@NativeType("GLint") int red, @NativeType("GLint") int green, @NativeType("GLint") int blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3f(@NativeType("GLfloat") float red, @NativeType("GLfloat") float green, @NativeType("GLfloat") float blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3d(@NativeType("GLdouble") double red, @NativeType("GLdouble") double green, @NativeType("GLdouble") double blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3ub(@NativeType("GLubyte") byte red, @NativeType("GLubyte") byte green, @NativeType("GLubyte") byte blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3us(@NativeType("GLushort") short red, @NativeType("GLushort") short green, @NativeType("GLushort") short blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3ui(@NativeType("GLuint") int red, @NativeType("GLuint") int green, @NativeType("GLuint") int blue) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3bv(@NativeType("GLbyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3ubv(@NativeType("GLubyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3usv(@NativeType("GLushort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3usv(@NativeType("GLushort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3uiv(@NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColor3uiv(@NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glSecondaryColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2i(@NativeType("GLint") int x, @NativeType("GLint") int y) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2s(@NativeType("GLshort") short x, @NativeType("GLshort") short y) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos2dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3i(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLint") int z) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3s(@NativeType("GLshort") short x, @NativeType("GLshort") short y, @NativeType("GLshort") short z) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glWindowPos3dv(@NativeType("GLdouble const *") double[] v) {
    }
}
