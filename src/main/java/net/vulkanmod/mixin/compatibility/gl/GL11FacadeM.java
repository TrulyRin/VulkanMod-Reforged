package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.compat.gl.GlCapabilityState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.BitSet;

@Mixin(GL11.class)
public class GL11FacadeM {
    private static final BitSet vulkanmod$displayLists = new BitSet();
    private static int vulkanmod$nextDisplayListId = 1;

    private static void vulkanmod$zero(ByteBuffer data) {
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, (byte) 0);
        }
    }

    private static void vulkanmod$zero(IntBuffer data) {
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, 0);
        }
    }

    private static void vulkanmod$zero(ShortBuffer data) {
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, (short) 0);
        }
    }

    private static void vulkanmod$zero(FloatBuffer data) {
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, 0.0F);
        }
    }

    private static void vulkanmod$zero(DoubleBuffer data) {
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, 0.0);
        }
    }

    private static void vulkanmod$zero(PointerBuffer data) {
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, 0L);
        }
    }

    private static void vulkanmod$zero(int[] data) {
        for (int i = 0; i < data.length; ++i) {
            data[i] = 0;
        }
    }

    private static void vulkanmod$zero(short[] data) {
        for (int i = 0; i < data.length; ++i) {
            data[i] = 0;
        }
    }

    private static void vulkanmod$zero(float[] data) {
        for (int i = 0; i < data.length; ++i) {
            data[i] = 0.0F;
        }
    }

    private static void vulkanmod$zero(double[] data) {
        for (int i = 0; i < data.length; ++i) {
            data[i] = 0.0;
        }
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable ShortBuffer data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Short.BYTES).order(ByteOrder.nativeOrder());
        ShortBuffer view = bytes.asShortBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            view.put(data.get(i));
        }
        return bytes;
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable IntBuffer data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Integer.BYTES).order(ByteOrder.nativeOrder());
        IntBuffer view = bytes.asIntBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            view.put(data.get(i));
        }
        return bytes;
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable FloatBuffer data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Float.BYTES).order(ByteOrder.nativeOrder());
        FloatBuffer view = bytes.asFloatBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            view.put(data.get(i));
        }
        return bytes;
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable DoubleBuffer data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Double.BYTES).order(ByteOrder.nativeOrder());
        DoubleBuffer view = bytes.asDoubleBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            view.put(data.get(i));
        }
        return bytes;
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable short[] data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Short.BYTES).order(ByteOrder.nativeOrder());
        bytes.asShortBuffer().put(data);
        return bytes;
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable int[] data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Integer.BYTES).order(ByteOrder.nativeOrder());
        bytes.asIntBuffer().put(data);
        return bytes;
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable float[] data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Float.BYTES).order(ByteOrder.nativeOrder());
        bytes.asFloatBuffer().put(data);
        return bytes;
    }

    @Nullable
    private static ByteBuffer vulkanmod$copyBytes(@Nullable double[] data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Double.BYTES).order(ByteOrder.nativeOrder());
        bytes.asDoubleBuffer().put(data);
        return bytes;
    }

    @Overwrite(remap = false)
    public static void glAlphaFunc(@NativeType("GLenum") int func, @NativeType("GLfloat") float ref) {
    }

    @Overwrite(remap = false)
    public static void glBegin(@NativeType("GLenum") int mode) {
    }

    @Overwrite(remap = false)
    public static void glEnd() {
    }

    @Overwrite(remap = false)
    public static void glVertex2f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y) {
    }

    @Overwrite(remap = false)
    public static void glVertex3f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z) {
    }

    @Overwrite(remap = false)
    public static void glVertex4f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z, @NativeType("GLfloat") float w) {
    }

    @Overwrite(remap = false)
    public static void glColor3f(@NativeType("GLfloat") float red, @NativeType("GLfloat") float green, @NativeType("GLfloat") float blue) {
    }

    @Overwrite(remap = false)
    public static void glColor4f(@NativeType("GLfloat") float red, @NativeType("GLfloat") float green, @NativeType("GLfloat") float blue, @NativeType("GLfloat") float alpha) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1f(@NativeType("GLfloat") float s) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2f(@NativeType("GLfloat") float s, @NativeType("GLfloat") float t) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3f(@NativeType("GLfloat") float s, @NativeType("GLfloat") float t, @NativeType("GLfloat") float r) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4f(@NativeType("GLfloat") float s, @NativeType("GLfloat") float t, @NativeType("GLfloat") float r, @NativeType("GLfloat") float q) {
    }

    @Overwrite(remap = false)
    public static void glNormal3f(@NativeType("GLfloat") float nx, @NativeType("GLfloat") float ny, @NativeType("GLfloat") float nz) {
    }

    @Overwrite(remap = false)
    public static void glVertex2s(@NativeType("GLshort") short x, @NativeType("GLshort") short y) {
    }

    @Overwrite(remap = false)
    public static void glVertex2i(@NativeType("GLint") int x, @NativeType("GLint") int y) {
    }

    @Overwrite(remap = false)
    public static void glVertex2d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y) {
    }

    @Overwrite(remap = false)
    public static void glVertex3s(@NativeType("GLshort") short x, @NativeType("GLshort") short y, @NativeType("GLshort") short z) {
    }

    @Overwrite(remap = false)
    public static void glVertex3i(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLint") int z) {
    }

    @Overwrite(remap = false)
    public static void glVertex3d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glVertex4s(@NativeType("GLshort") short x, @NativeType("GLshort") short y, @NativeType("GLshort") short z, @NativeType("GLshort") short w) {
    }

    @Overwrite(remap = false)
    public static void glVertex4i(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLint") int z, @NativeType("GLint") int w) {
    }

    @Overwrite(remap = false)
    public static void glVertex4d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z, @NativeType("GLdouble") double w) {
    }

    @Overwrite(remap = false)
    public static void glColor3b(@NativeType("GLbyte") byte red, @NativeType("GLbyte") byte green, @NativeType("GLbyte") byte blue) {
    }

    @Overwrite(remap = false)
    public static void glColor3s(@NativeType("GLshort") short red, @NativeType("GLshort") short green, @NativeType("GLshort") short blue) {
    }

    @Overwrite(remap = false)
    public static void glColor3i(@NativeType("GLint") int red, @NativeType("GLint") int green, @NativeType("GLint") int blue) {
    }

    @Overwrite(remap = false)
    public static void glColor3d(@NativeType("GLdouble") double red, @NativeType("GLdouble") double green, @NativeType("GLdouble") double blue) {
    }

    @Overwrite(remap = false)
    public static void glColor3ub(@NativeType("GLubyte") byte red, @NativeType("GLubyte") byte green, @NativeType("GLubyte") byte blue) {
    }

    @Overwrite(remap = false)
    public static void glColor3us(@NativeType("GLushort") short red, @NativeType("GLushort") short green, @NativeType("GLushort") short blue) {
    }

    @Overwrite(remap = false)
    public static void glColor3ui(@NativeType("GLuint") int red, @NativeType("GLuint") int green, @NativeType("GLuint") int blue) {
    }

    @Overwrite(remap = false)
    public static void glColor4b(@NativeType("GLbyte") byte red, @NativeType("GLbyte") byte green, @NativeType("GLbyte") byte blue, @NativeType("GLbyte") byte alpha) {
    }

    @Overwrite(remap = false)
    public static void glColor4s(@NativeType("GLshort") short red, @NativeType("GLshort") short green, @NativeType("GLshort") short blue, @NativeType("GLshort") short alpha) {
    }

    @Overwrite(remap = false)
    public static void glColor4i(@NativeType("GLint") int red, @NativeType("GLint") int green, @NativeType("GLint") int blue, @NativeType("GLint") int alpha) {
    }

    @Overwrite(remap = false)
    public static void glColor4d(@NativeType("GLdouble") double red, @NativeType("GLdouble") double green, @NativeType("GLdouble") double blue, @NativeType("GLdouble") double alpha) {
    }

    @Overwrite(remap = false)
    public static void glColor4ub(@NativeType("GLubyte") byte red, @NativeType("GLubyte") byte green, @NativeType("GLubyte") byte blue, @NativeType("GLubyte") byte alpha) {
    }

    @Overwrite(remap = false)
    public static void glColor4us(@NativeType("GLushort") short red, @NativeType("GLushort") short green, @NativeType("GLushort") short blue, @NativeType("GLushort") short alpha) {
    }

    @Overwrite(remap = false)
    public static void glColor4ui(@NativeType("GLuint") int red, @NativeType("GLuint") int green, @NativeType("GLuint") int blue, @NativeType("GLuint") int alpha) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1s(@NativeType("GLshort") short s) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1i(@NativeType("GLint") int s) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1d(@NativeType("GLdouble") double s) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2s(@NativeType("GLshort") short s, @NativeType("GLshort") short t) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2i(@NativeType("GLint") int s, @NativeType("GLint") int t) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2d(@NativeType("GLdouble") double s, @NativeType("GLdouble") double t) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3s(@NativeType("GLshort") short s, @NativeType("GLshort") short t, @NativeType("GLshort") short r) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3i(@NativeType("GLint") int s, @NativeType("GLint") int t, @NativeType("GLint") int r) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3d(@NativeType("GLdouble") double s, @NativeType("GLdouble") double t, @NativeType("GLdouble") double r) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4s(@NativeType("GLshort") short s, @NativeType("GLshort") short t, @NativeType("GLshort") short r, @NativeType("GLshort") short q) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4i(@NativeType("GLint") int s, @NativeType("GLint") int t, @NativeType("GLint") int r, @NativeType("GLint") int q) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4d(@NativeType("GLdouble") double s, @NativeType("GLdouble") double t, @NativeType("GLdouble") double r, @NativeType("GLdouble") double q) {
    }

    @Overwrite(remap = false)
    public static void glNormal3b(@NativeType("GLbyte") byte nx, @NativeType("GLbyte") byte ny, @NativeType("GLbyte") byte nz) {
    }

    @Overwrite(remap = false)
    public static void glNormal3s(@NativeType("GLshort") short nx, @NativeType("GLshort") short ny, @NativeType("GLshort") short nz) {
    }

    @Overwrite(remap = false)
    public static void glNormal3i(@NativeType("GLint") int nx, @NativeType("GLint") int ny, @NativeType("GLint") int nz) {
    }

    @Overwrite(remap = false)
    public static void glNormal3d(@NativeType("GLdouble") double nx, @NativeType("GLdouble") double ny, @NativeType("GLdouble") double nz) {
    }

    @Overwrite(remap = false)
    public static void glVertex2fv(@NativeType("GLfloat const *") FloatBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex2sv(@NativeType("GLshort const *") ShortBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex2iv(@NativeType("GLint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex2dv(@NativeType("GLdouble const *") DoubleBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3fv(@NativeType("GLfloat const *") FloatBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3sv(@NativeType("GLshort const *") ShortBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3iv(@NativeType("GLint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3dv(@NativeType("GLdouble const *") DoubleBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4fv(@NativeType("GLfloat const *") FloatBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4sv(@NativeType("GLshort const *") ShortBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4iv(@NativeType("GLint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4dv(@NativeType("GLdouble const *") DoubleBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glColor3bv(@NativeType("GLbyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor3sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor3iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor3fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor3dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor3ubv(@NativeType("GLubyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor3usv(@NativeType("GLushort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor3uiv(@NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4bv(@NativeType("GLbyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4ubv(@NativeType("GLubyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4usv(@NativeType("GLushort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glColor4uiv(@NativeType("GLuint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3fv(@NativeType("GLfloat const *") FloatBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3bv(@NativeType("GLbyte const *") ByteBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3sv(@NativeType("GLshort const *") ShortBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3iv(@NativeType("GLint const *") IntBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3dv(@NativeType("GLdouble const *") DoubleBuffer v) {
    }

    @Overwrite(remap = false)
    public static void glVertex2fv(@NativeType("GLfloat const *") float[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex2sv(@NativeType("GLshort const *") short[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex2iv(@NativeType("GLint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex2dv(@NativeType("GLdouble const *") double[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3fv(@NativeType("GLfloat const *") float[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3sv(@NativeType("GLshort const *") short[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3iv(@NativeType("GLint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex3dv(@NativeType("GLdouble const *") double[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4fv(@NativeType("GLfloat const *") float[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4sv(@NativeType("GLshort const *") short[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4iv(@NativeType("GLint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glVertex4dv(@NativeType("GLdouble const *") double[] coords) {
    }

    @Overwrite(remap = false)
    public static void glColor3sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor3iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor3fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor3dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor3usv(@NativeType("GLushort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor3uiv(@NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor4sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor4iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor4fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor4dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor4usv(@NativeType("GLushort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glColor4uiv(@NativeType("GLuint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord1dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord2dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord3dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glTexCoord4dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3fv(@NativeType("GLfloat const *") float[] v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3sv(@NativeType("GLshort const *") short[] v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3iv(@NativeType("GLint const *") int[] v) {
    }

    @Overwrite(remap = false)
    public static void glNormal3dv(@NativeType("GLdouble const *") double[] v) {
    }

    @Overwrite(remap = false)
    public static void glVertexPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glVertexPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glVertexPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glVertexPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glVertexPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glColorPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glTexCoordPointer(@NativeType("GLint") int size, @NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glNormalPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glNormalPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glNormalPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glNormalPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glNormalPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") FloatBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") DoubleBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") short[] pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") int[] pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") float[] pointer) {
    }

    @Overwrite(remap = false)
    public static void glInterleavedArrays(@NativeType("GLenum") int format, @NativeType("GLsizei") int stride, @NativeType("void const *") double[] pointer) {
    }

    @Overwrite(remap = false)
    public static synchronized int glGenLists(@NativeType("GLsizei") int range) {
        if (range <= 0) {
            return 0;
        }

        long next = (long) vulkanmod$nextDisplayListId + range;
        if (next > Integer.MAX_VALUE) {
            return 0;
        }

        int first = vulkanmod$nextDisplayListId;
        vulkanmod$nextDisplayListId += range;
        vulkanmod$displayLists.set(first, first + range);
        return first;
    }

    @Overwrite(remap = false)
    public static synchronized void glDeleteLists(@NativeType("GLuint") int list, @NativeType("GLsizei") int range) {
        if (list <= 0 || range <= 0) {
            return;
        }

        long end = (long) list + range;
        vulkanmod$displayLists.clear(list, end > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) end);
    }

    @Overwrite(remap = false)
    public static synchronized boolean glIsList(@NativeType("GLuint") int list) {
        return list > 0 && vulkanmod$displayLists.get(list);
    }

    @Overwrite(remap = false)
    public static void glNewList(@NativeType("GLuint") int list, @NativeType("GLenum") int mode) {
    }

    @Overwrite(remap = false)
    public static void glEndList() {
    }

    @Overwrite(remap = false)
    public static void glCallList(@NativeType("GLuint") int list) {
    }

    @Overwrite(remap = false)
    public static void glCallLists(@NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer lists) {
    }

    @Overwrite(remap = false)
    public static void glCallLists(@NativeType("void const *") ByteBuffer lists) {
    }

    @Overwrite(remap = false)
    public static void glCallLists(@NativeType("void const *") ShortBuffer lists) {
    }

    @Overwrite(remap = false)
    public static void glCallLists(@NativeType("void const *") IntBuffer lists) {
    }

    @Overwrite(remap = false)
    public static void glListBase(@NativeType("GLuint") int base) {
    }

    @Overwrite(remap = false)
    public static void glBitmap(@NativeType("GLsizei") int w, @NativeType("GLsizei") int h, @NativeType("GLfloat") float xOrig, @NativeType("GLfloat") float yOrig, @NativeType("GLfloat") float xInc, @NativeType("GLfloat") float yInc, @Nullable @NativeType("GLubyte const *") ByteBuffer data) {
    }

    @Overwrite(remap = false)
    public static void glBitmap(@NativeType("GLsizei") int w, @NativeType("GLsizei") int h, @NativeType("GLfloat") float xOrig, @NativeType("GLfloat") float yOrig, @NativeType("GLfloat") float xInc, @NativeType("GLfloat") float yInc, @NativeType("GLubyte const *") long data) {
    }

    @Overwrite(remap = false)
    public static void glCopyPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int type) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") long pixels) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") ShortBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") IntBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") FloatBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") short[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") int[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glDrawPixels(@NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") float[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2i(@NativeType("GLint") int x, @NativeType("GLint") int y) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2s(@NativeType("GLshort") short x, @NativeType("GLshort") short y) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3i(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLint") int z) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3s(@NativeType("GLshort") short x, @NativeType("GLshort") short y, @NativeType("GLshort") short z) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4i(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLint") int z, @NativeType("GLint") int w) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4s(@NativeType("GLshort") short x, @NativeType("GLshort") short y, @NativeType("GLshort") short z, @NativeType("GLshort") short w) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4f(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z, @NativeType("GLfloat") float w) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4d(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z, @NativeType("GLdouble") double w) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2iv(@NativeType("GLint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2sv(@NativeType("GLshort const *") ShortBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2fv(@NativeType("GLfloat const *") FloatBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2dv(@NativeType("GLdouble const *") DoubleBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3iv(@NativeType("GLint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3sv(@NativeType("GLshort const *") ShortBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3fv(@NativeType("GLfloat const *") FloatBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3dv(@NativeType("GLdouble const *") DoubleBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4iv(@NativeType("GLint const *") IntBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4sv(@NativeType("GLshort const *") ShortBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4fv(@NativeType("GLfloat const *") FloatBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4dv(@NativeType("GLdouble const *") DoubleBuffer coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2iv(@NativeType("GLint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2sv(@NativeType("GLshort const *") short[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2fv(@NativeType("GLfloat const *") float[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos2dv(@NativeType("GLdouble const *") double[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3iv(@NativeType("GLint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3sv(@NativeType("GLshort const *") short[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3fv(@NativeType("GLfloat const *") float[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos3dv(@NativeType("GLdouble const *") double[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4iv(@NativeType("GLint const *") int[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4sv(@NativeType("GLshort const *") short[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4fv(@NativeType("GLfloat const *") float[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRasterPos4dv(@NativeType("GLdouble const *") double[] coords) {
    }

    @Overwrite(remap = false)
    public static void glRecti(@NativeType("GLint") int x1, @NativeType("GLint") int y1, @NativeType("GLint") int x2, @NativeType("GLint") int y2) {
    }

    @Overwrite(remap = false)
    public static void glRects(@NativeType("GLshort") short x1, @NativeType("GLshort") short y1, @NativeType("GLshort") short x2, @NativeType("GLshort") short y2) {
    }

    @Overwrite(remap = false)
    public static void glRectf(@NativeType("GLfloat") float x1, @NativeType("GLfloat") float y1, @NativeType("GLfloat") float x2, @NativeType("GLfloat") float y2) {
    }

    @Overwrite(remap = false)
    public static void glRectd(@NativeType("GLdouble") double x1, @NativeType("GLdouble") double y1, @NativeType("GLdouble") double x2, @NativeType("GLdouble") double y2) {
    }

    @Overwrite(remap = false)
    public static void glRectiv(@NativeType("GLint const *") IntBuffer v1, @NativeType("GLint const *") IntBuffer v2) {
    }

    @Overwrite(remap = false)
    public static void glRectsv(@NativeType("GLshort const *") ShortBuffer v1, @NativeType("GLshort const *") ShortBuffer v2) {
    }

    @Overwrite(remap = false)
    public static void glRectfv(@NativeType("GLfloat const *") FloatBuffer v1, @NativeType("GLfloat const *") FloatBuffer v2) {
    }

    @Overwrite(remap = false)
    public static void glRectdv(@NativeType("GLdouble const *") DoubleBuffer v1, @NativeType("GLdouble const *") DoubleBuffer v2) {
    }

    @Overwrite(remap = false)
    public static void glRectiv(@NativeType("GLint const *") int[] v1, @NativeType("GLint const *") int[] v2) {
    }

    @Overwrite(remap = false)
    public static void glRectsv(@NativeType("GLshort const *") short[] v1, @NativeType("GLshort const *") short[] v2) {
    }

    @Overwrite(remap = false)
    public static void glRectfv(@NativeType("GLfloat const *") float[] v1, @NativeType("GLfloat const *") float[] v2) {
    }

    @Overwrite(remap = false)
    public static void glRectdv(@NativeType("GLdouble const *") double[] v1, @NativeType("GLdouble const *") double[] v2) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord1f(@NativeType("GLfloat") float u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord1fv(@NativeType("GLfloat const *") FloatBuffer u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord1d(@NativeType("GLdouble") double u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord1dv(@NativeType("GLdouble const *") DoubleBuffer u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord2f(@NativeType("GLfloat") float u, @NativeType("GLfloat") float v) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord2fv(@NativeType("GLfloat const *") FloatBuffer u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord2d(@NativeType("GLdouble") double u, @NativeType("GLdouble") double v) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord2dv(@NativeType("GLdouble const *") DoubleBuffer u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord1fv(@NativeType("GLfloat const *") float[] u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord1dv(@NativeType("GLdouble const *") double[] u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord2fv(@NativeType("GLfloat const *") float[] u) {
    }

    @Overwrite(remap = false)
    public static void glEvalCoord2dv(@NativeType("GLdouble const *") double[] u) {
    }

    @Overwrite(remap = false)
    public static void glEvalMesh1(@NativeType("GLenum") int mode, @NativeType("GLint") int i1, @NativeType("GLint") int i2) {
    }

    @Overwrite(remap = false)
    public static void glEvalMesh2(@NativeType("GLenum") int mode, @NativeType("GLint") int i1, @NativeType("GLint") int i2, @NativeType("GLint") int j1, @NativeType("GLint") int j2) {
    }

    @Overwrite(remap = false)
    public static void glEvalPoint1(@NativeType("GLint") int i) {
    }

    @Overwrite(remap = false)
    public static void glEvalPoint2(@NativeType("GLint") int i, @NativeType("GLint") int j) {
    }

    @Overwrite(remap = false)
    public static void glGetMapiv(@NativeType("GLenum") int target, @NativeType("GLenum") int query, @NativeType("GLint *") IntBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static int glGetMapi(@NativeType("GLenum") int target, @NativeType("GLenum") int query) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetMapfv(@NativeType("GLenum") int target, @NativeType("GLenum") int query, @NativeType("GLfloat *") FloatBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static float glGetMapf(@NativeType("GLenum") int target, @NativeType("GLenum") int query) {
        return 0.0F;
    }

    @Overwrite(remap = false)
    public static void glGetMapdv(@NativeType("GLenum") int target, @NativeType("GLenum") int query, @NativeType("GLdouble *") DoubleBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static double glGetMapd(@NativeType("GLenum") int target, @NativeType("GLenum") int query) {
        return 0.0;
    }

    @Overwrite(remap = false)
    public static void glGetMapiv(@NativeType("GLenum") int target, @NativeType("GLenum") int query, @NativeType("GLint *") int[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetMapfv(@NativeType("GLenum") int target, @NativeType("GLenum") int query, @NativeType("GLfloat *") float[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetMapdv(@NativeType("GLenum") int target, @NativeType("GLenum") int query, @NativeType("GLdouble *") double[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glMap1f(@NativeType("GLenum") int target, @NativeType("GLfloat") float u1, @NativeType("GLfloat") float u2, @NativeType("GLint") int stride, @NativeType("GLint") int order, @NativeType("GLfloat const *") FloatBuffer points) {
    }

    @Overwrite(remap = false)
    public static void glMap1d(@NativeType("GLenum") int target, @NativeType("GLdouble") double u1, @NativeType("GLdouble") double u2, @NativeType("GLint") int stride, @NativeType("GLint") int order, @NativeType("GLdouble const *") DoubleBuffer points) {
    }

    @Overwrite(remap = false)
    public static void glMap2f(@NativeType("GLenum") int target, @NativeType("GLfloat") float u1, @NativeType("GLfloat") float u2, @NativeType("GLint") int ustride, @NativeType("GLint") int uorder, @NativeType("GLfloat") float v1, @NativeType("GLfloat") float v2, @NativeType("GLint") int vstride, @NativeType("GLint") int vorder, @NativeType("GLfloat const *") FloatBuffer points) {
    }

    @Overwrite(remap = false)
    public static void glMap2d(@NativeType("GLenum") int target, @NativeType("GLdouble") double u1, @NativeType("GLdouble") double u2, @NativeType("GLint") int ustride, @NativeType("GLint") int uorder, @NativeType("GLdouble") double v1, @NativeType("GLdouble") double v2, @NativeType("GLint") int vstride, @NativeType("GLint") int vorder, @NativeType("GLdouble const *") DoubleBuffer points) {
    }

    @Overwrite(remap = false)
    public static void glMap1f(@NativeType("GLenum") int target, @NativeType("GLfloat") float u1, @NativeType("GLfloat") float u2, @NativeType("GLint") int stride, @NativeType("GLint") int order, @NativeType("GLfloat const *") float[] points) {
    }

    @Overwrite(remap = false)
    public static void glMap1d(@NativeType("GLenum") int target, @NativeType("GLdouble") double u1, @NativeType("GLdouble") double u2, @NativeType("GLint") int stride, @NativeType("GLint") int order, @NativeType("GLdouble const *") double[] points) {
    }

    @Overwrite(remap = false)
    public static void glMap2f(@NativeType("GLenum") int target, @NativeType("GLfloat") float u1, @NativeType("GLfloat") float u2, @NativeType("GLint") int ustride, @NativeType("GLint") int uorder, @NativeType("GLfloat") float v1, @NativeType("GLfloat") float v2, @NativeType("GLint") int vstride, @NativeType("GLint") int vorder, @NativeType("GLfloat const *") float[] points) {
    }

    @Overwrite(remap = false)
    public static void glMap2d(@NativeType("GLenum") int target, @NativeType("GLdouble") double u1, @NativeType("GLdouble") double u2, @NativeType("GLint") int ustride, @NativeType("GLint") int uorder, @NativeType("GLdouble") double v1, @NativeType("GLdouble") double v2, @NativeType("GLint") int vstride, @NativeType("GLint") int vorder, @NativeType("GLdouble const *") double[] points) {
    }

    @Overwrite(remap = false)
    public static void glMapGrid1f(@NativeType("GLint") int un, @NativeType("GLfloat") float u1, @NativeType("GLfloat") float u2) {
    }

    @Overwrite(remap = false)
    public static void glMapGrid1d(@NativeType("GLint") int un, @NativeType("GLdouble") double u1, @NativeType("GLdouble") double u2) {
    }

    @Overwrite(remap = false)
    public static void glMapGrid2f(@NativeType("GLint") int un, @NativeType("GLfloat") float u1, @NativeType("GLfloat") float u2, @NativeType("GLint") int vn, @NativeType("GLfloat") float v1, @NativeType("GLfloat") float v2) {
    }

    @Overwrite(remap = false)
    public static void glMapGrid2d(@NativeType("GLint") int un, @NativeType("GLdouble") double u1, @NativeType("GLdouble") double u2, @NativeType("GLint") int vn, @NativeType("GLdouble") double v1, @NativeType("GLdouble") double v2) {
    }

    @Overwrite(remap = false)
    public static void glClipPlane(@NativeType("GLenum") int plane, @NativeType("GLdouble const *") DoubleBuffer equation) {
    }

    @Overwrite(remap = false)
    public static void glClipPlane(@NativeType("GLenum") int plane, @NativeType("GLdouble const *") double[] equation) {
    }

    @Overwrite(remap = false)
    public static void glGetClipPlane(@NativeType("GLenum") int plane, @NativeType("GLdouble *") DoubleBuffer equation) {
        vulkanmod$zero(equation);
    }

    @Overwrite(remap = false)
    public static void glGetClipPlane(@NativeType("GLenum") int plane, @NativeType("GLdouble *") double[] equation) {
        vulkanmod$zero(equation);
    }

    @Overwrite(remap = false)
    public static void glFogi(@NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glFogiv(@NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glFogiv(@NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
    }

    @Overwrite(remap = false)
    public static void glFogf(@NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glFogfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glFogfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
    }

    @Overwrite(remap = false)
    public static void glLightModeli(@NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glLightModelf(@NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glLightModeliv(@NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glLightModeliv(@NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
    }

    @Overwrite(remap = false)
    public static void glLightModelfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glLightModelfv(@NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
    }

    @Overwrite(remap = false)
    public static void glLighti(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glLightf(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glLightiv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glLightiv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
    }

    @Overwrite(remap = false)
    public static void glLightfv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glLightfv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
    }

    @Overwrite(remap = false)
    public static void glGetLightiv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetLightiv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static int glGetLighti(@NativeType("GLenum") int light, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetLightfv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetLightfv(@NativeType("GLenum") int light, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static float glGetLightf(@NativeType("GLenum") int light, @NativeType("GLenum") int pname) {
        return 0.0F;
    }

    @Overwrite(remap = false)
    public static void glMateriali(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glMaterialf(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glMaterialiv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glMaterialiv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
    }

    @Overwrite(remap = false)
    public static void glMaterialfv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glMaterialfv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
    }

    @Overwrite(remap = false)
    public static void glGetMaterialiv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetMaterialiv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetMaterialfv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetMaterialfv(@NativeType("GLenum") int face, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glTexGeni(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glTexGeniv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glTexGeniv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
    }

    @Overwrite(remap = false)
    public static void glTexGenf(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glTexGenfv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glTexGenfv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
    }

    @Overwrite(remap = false)
    public static void glTexGend(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLdouble") double param) {
    }

    @Overwrite(remap = false)
    public static void glTexGendv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLdouble const *") DoubleBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glTexGendv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLdouble const *") double[] params) {
    }

    @Overwrite(remap = false)
    public static void glGetTexGeniv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetTexGeniv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static int glGetTexGeni(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetTexGenfv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetTexGenfv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static float glGetTexGenf(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname) {
        return 0.0F;
    }

    @Overwrite(remap = false)
    public static void glGetTexGendv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLdouble *") DoubleBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetTexGendv(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname, @NativeType("GLdouble *") double[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static double glGetTexGend(@NativeType("GLenum") int coord, @NativeType("GLenum") int pname) {
        return 0.0;
    }

    @Overwrite(remap = false)
    public static void glClearIndex(@NativeType("GLfloat") float c) {
    }

    @Overwrite(remap = false)
    public static void glEdgeFlag(@NativeType("GLboolean") boolean flag) {
    }

    @Overwrite(remap = false)
    public static void glEdgeFlagv(@NativeType("GLboolean const *") ByteBuffer flag) {
    }

    @Overwrite(remap = false)
    public static void glEdgeFlagPointer(@NativeType("GLsizei") int stride, @NativeType("GLboolean const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glEdgeFlagPointer(@NativeType("GLsizei") int stride, @NativeType("GLboolean const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glFeedbackBuffer(@NativeType("GLenum") int type, @NativeType("GLfloat *") FloatBuffer buffer) {
        vulkanmod$zero(buffer);
    }

    @Overwrite(remap = false)
    public static void glFeedbackBuffer(@NativeType("GLenum") int type, @NativeType("GLfloat *") float[] buffer) {
        vulkanmod$zero(buffer);
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapfv(@NativeType("GLenum") int map, @NativeType("GLfloat *") FloatBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapfv(@NativeType("GLenum") int map, @NativeType("GLfloat *") long data) {
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapfv(@NativeType("GLenum") int map, @NativeType("GLfloat *") float[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapusv(@NativeType("GLenum") int map, @NativeType("GLushort *") ShortBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapusv(@NativeType("GLenum") int map, @NativeType("GLushort *") long data) {
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapusv(@NativeType("GLenum") int map, @NativeType("GLushort *") short[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapuiv(@NativeType("GLenum") int map, @NativeType("GLuint *") IntBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapuiv(@NativeType("GLenum") int map, @NativeType("GLuint *") long data) {
    }

    @Overwrite(remap = false)
    public static void glGetPixelMapuiv(@NativeType("GLenum") int map, @NativeType("GLuint *") int[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glIndexi(@NativeType("GLint") int c) {
    }

    @Overwrite(remap = false)
    public static void glIndexub(@NativeType("GLubyte") byte c) {
    }

    @Overwrite(remap = false)
    public static void glIndexs(@NativeType("GLshort") short c) {
    }

    @Overwrite(remap = false)
    public static void glIndexf(@NativeType("GLfloat") float c) {
    }

    @Overwrite(remap = false)
    public static void glIndexd(@NativeType("GLdouble") double c) {
    }

    @Overwrite(remap = false)
    public static void glIndexiv(@NativeType("GLint const *") IntBuffer index) {
    }

    @Overwrite(remap = false)
    public static void glIndexiv(@NativeType("GLint const *") int[] index) {
    }

    @Overwrite(remap = false)
    public static void glIndexubv(@NativeType("GLubyte const *") ByteBuffer index) {
    }

    @Overwrite(remap = false)
    public static void glIndexsv(@NativeType("GLshort const *") ShortBuffer index) {
    }

    @Overwrite(remap = false)
    public static void glIndexsv(@NativeType("GLshort const *") short[] index) {
    }

    @Overwrite(remap = false)
    public static void glIndexfv(@NativeType("GLfloat const *") FloatBuffer index) {
    }

    @Overwrite(remap = false)
    public static void glIndexfv(@NativeType("GLfloat const *") float[] index) {
    }

    @Overwrite(remap = false)
    public static void glIndexdv(@NativeType("GLdouble const *") DoubleBuffer index) {
    }

    @Overwrite(remap = false)
    public static void glIndexdv(@NativeType("GLdouble const *") double[] index) {
    }

    @Overwrite(remap = false)
    public static void glIndexMask(@NativeType("GLuint") int mask) {
    }

    @Overwrite(remap = false)
    public static void glIndexPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glIndexPointer(@NativeType("GLenum") int type, @NativeType("GLsizei") int stride, @NativeType("void const *") long pointer) {
    }

    @Overwrite(remap = false)
    public static void glIndexPointer(@NativeType("GLsizei") int stride, @NativeType("void const *") ByteBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glIndexPointer(@NativeType("GLsizei") int stride, @NativeType("void const *") ShortBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glIndexPointer(@NativeType("GLsizei") int stride, @NativeType("void const *") IntBuffer pointer) {
    }

    @Overwrite(remap = false)
    public static void glInitNames() {
    }

    @Overwrite(remap = false)
    public static void glLoadName(@NativeType("GLuint") int name) {
    }

    @Overwrite(remap = false)
    public static void glPassThrough(@NativeType("GLfloat") float token) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapfv(@NativeType("GLenum") int map, @NativeType("GLsizei") int size, @NativeType("GLfloat const *") long values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapfv(@NativeType("GLenum") int map, @NativeType("GLfloat const *") FloatBuffer values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapfv(@NativeType("GLenum") int map, @NativeType("GLfloat const *") float[] values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapusv(@NativeType("GLenum") int map, @NativeType("GLsizei") int size, @NativeType("GLushort const *") long values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapusv(@NativeType("GLenum") int map, @NativeType("GLushort const *") ShortBuffer values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapusv(@NativeType("GLenum") int map, @NativeType("GLushort const *") short[] values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapuiv(@NativeType("GLenum") int map, @NativeType("GLsizei") int size, @NativeType("GLuint const *") long values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapuiv(@NativeType("GLenum") int map, @NativeType("GLuint const *") IntBuffer values) {
    }

    @Overwrite(remap = false)
    public static void glPixelMapuiv(@NativeType("GLenum") int map, @NativeType("GLuint const *") int[] values) {
    }

    @Overwrite(remap = false)
    public static void glPixelTransferi(@NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glPixelTransferf(@NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glPixelZoom(@NativeType("GLfloat") float xfactor, @NativeType("GLfloat") float yfactor) {
    }

    @Overwrite(remap = false)
    public static void glPopName() {
    }

    @Overwrite(remap = false)
    public static void glPushName(@NativeType("GLuint") int name) {
    }

    @Overwrite(remap = false)
    public static int glRenderMode(@NativeType("GLenum") int mode) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glSelectBuffer(@NativeType("GLuint *") IntBuffer buffer) {
        vulkanmod$zero(buffer);
    }

    @Overwrite(remap = false)
    public static void glSelectBuffer(@NativeType("GLuint *") int[] buffer) {
        vulkanmod$zero(buffer);
    }

    @Overwrite(remap = false)
    public static void glGenTextures(@NativeType("GLuint *") IntBuffer textures) {
        for (int i = textures.position(); i < textures.limit(); ++i) {
            textures.put(i, GL11M.glGenTextures());
        }
    }

    @Overwrite(remap = false)
    public static void glGenTextures(@NativeType("GLuint *") int[] textures) {
        for (int i = 0; i < textures.length; ++i) {
            textures[i] = GL11M.glGenTextures();
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteTextures(@NativeType("GLuint const *") IntBuffer textures) {
        GL11M.glDeleteTextures(textures);
    }

    @Overwrite(remap = false)
    public static void glDeleteTextures(@NativeType("GLuint const *") int[] textures) {
        for (int texture : textures) {
            GL11M.glDeleteTextures(texture);
        }
    }

    @Overwrite(remap = false)
    public static void glDrawElements(@NativeType("GLenum") int mode, @NativeType("GLsizei") int count, @NativeType("GLenum") int type, @NativeType("void const *") long indices) {
        GL11M.glDrawElements(mode, count, type, indices);
    }

    @Overwrite(remap = false)
    public static void glDrawElements(@NativeType("GLenum") int mode, @NativeType("GLenum") int type, @NativeType("void const *") ByteBuffer indices) {
        GL11M.glDrawElements(mode, indices != null ? indices.remaining() : 0, type, 0L);
    }

    @Overwrite(remap = false)
    public static void glDrawElements(@NativeType("GLenum") int mode, @NativeType("GLubyte const *") ByteBuffer indices) {
        GL11M.glDrawElements(mode, indices != null ? indices.remaining() : 0, GL11.GL_UNSIGNED_BYTE, 0L);
    }

    @Overwrite(remap = false)
    public static void glDrawElements(@NativeType("GLenum") int mode, @NativeType("GLushort const *") ShortBuffer indices) {
        GL11M.glDrawElements(mode, indices != null ? indices.remaining() : 0, GL11.GL_UNSIGNED_SHORT, 0L);
    }

    @Overwrite(remap = false)
    public static void glDrawElements(@NativeType("GLenum") int mode, @NativeType("GLuint const *") IntBuffer indices) {
        GL11M.glDrawElements(mode, indices != null ? indices.remaining() : 0, GL11.GL_UNSIGNED_INT, 0L);
    }

    @Overwrite(remap = false)
    public static void glAccum(@NativeType("GLenum") int op, @NativeType("GLfloat") float value) {
    }

    @Overwrite(remap = false)
    public static void glClearAccum(@NativeType("GLfloat") float red, @NativeType("GLfloat") float green, @NativeType("GLfloat") float blue, @NativeType("GLfloat") float alpha) {
    }

    @Overwrite(remap = false)
    public static void glArrayElement(@NativeType("GLint") int i) {
    }

    @Overwrite(remap = false)
    public static void glColorMaterial(@NativeType("GLenum") int face, @NativeType("GLenum") int mode) {
    }

    @Overwrite(remap = false)
    public static void glLineStipple(@NativeType("GLint") int factor, @NativeType("GLushort") short pattern) {
    }

    @Overwrite(remap = false)
    public static void glMatrixMode(@NativeType("GLenum") int mode) {
    }

    @Overwrite(remap = false)
    public static void glPushMatrix() {
    }

    @Overwrite(remap = false)
    public static void glPopMatrix() {
    }

    @Overwrite(remap = false)
    public static void glLoadIdentity() {
    }

    @Overwrite(remap = false)
    public static void glLoadMatrixf(@NativeType("GLfloat const *") FloatBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glLoadMatrixf(@NativeType("GLfloat const *") float[] m) {
    }

    @Overwrite(remap = false)
    public static void glLoadMatrixd(@NativeType("GLdouble const *") DoubleBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glLoadMatrixd(@NativeType("GLdouble const *") double[] m) {
    }

    @Overwrite(remap = false)
    public static void glMultMatrixf(@NativeType("GLfloat const *") FloatBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glMultMatrixf(@NativeType("GLfloat const *") float[] m) {
    }

    @Overwrite(remap = false)
    public static void glMultMatrixd(@NativeType("GLdouble const *") DoubleBuffer m) {
    }

    @Overwrite(remap = false)
    public static void glMultMatrixd(@NativeType("GLdouble const *") double[] m) {
    }

    @Overwrite(remap = false)
    public static void glTranslatef(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z) {
    }

    @Overwrite(remap = false)
    public static void glTranslated(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glRotatef(@NativeType("GLfloat") float angle, @NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z) {
    }

    @Overwrite(remap = false)
    public static void glRotated(@NativeType("GLdouble") double angle, @NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glScalef(@NativeType("GLfloat") float x, @NativeType("GLfloat") float y, @NativeType("GLfloat") float z) {
    }

    @Overwrite(remap = false)
    public static void glScaled(@NativeType("GLdouble") double x, @NativeType("GLdouble") double y, @NativeType("GLdouble") double z) {
    }

    @Overwrite(remap = false)
    public static void glOrtho(@NativeType("GLdouble") double left, @NativeType("GLdouble") double right, @NativeType("GLdouble") double bottom, @NativeType("GLdouble") double top, @NativeType("GLdouble") double zNear, @NativeType("GLdouble") double zFar) {
    }

    @Overwrite(remap = false)
    public static void glFrustum(@NativeType("GLdouble") double left, @NativeType("GLdouble") double right, @NativeType("GLdouble") double bottom, @NativeType("GLdouble") double top, @NativeType("GLdouble") double zNear, @NativeType("GLdouble") double zFar) {
    }

    @Overwrite(remap = false)
    public static void glPushAttrib(@NativeType("GLbitfield") int mask) {
    }

    @Overwrite(remap = false)
    public static void glPopAttrib() {
    }

    @Overwrite(remap = false)
    public static void glPushClientAttrib(@NativeType("GLbitfield") int mask) {
    }

    @Overwrite(remap = false)
    public static void glPopClientAttrib() {
    }

    @Overwrite(remap = false)
    public static void glEnableClientState(@NativeType("GLenum") int cap) {
    }

    @Overwrite(remap = false)
    public static void glDisableClientState(@NativeType("GLenum") int cap) {
    }

    @Overwrite(remap = false)
    public static void glShadeModel(@NativeType("GLenum") int mode) {
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") ShortBuffer pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") FloatBuffer pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") DoubleBuffer pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") short[] pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") int[] pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") float[] pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glGetTexImage(@NativeType("GLenum") int tex, @NativeType("GLint") int level, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") double[] pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") ShortBuffer pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") short[] pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") int[] pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glReadPixels(@NativeType("GLint") int x, @NativeType("GLint") int y, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void *") float[] pixels) {
        vulkanmod$zero(pixels);
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") ByteBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") long pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") ShortBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") IntBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") FloatBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") DoubleBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") short[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") int[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") float[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") double[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") ShortBuffer pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") IntBuffer pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") FloatBuffer pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") DoubleBuffer pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") short[] pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") int[] pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") float[] pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int internalformat, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLint") int border, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") double[] pixels) {
        GL11M.glTexImage2D(target, level, internalformat, width, height, border, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") ByteBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @NativeType("void const *") long pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") ShortBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") IntBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") FloatBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") DoubleBuffer pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") short[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") int[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") float[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage1D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLsizei") int width, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") double[] pixels) {
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") ShortBuffer pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") IntBuffer pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") FloatBuffer pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") DoubleBuffer pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") short[] pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") int[] pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") float[] pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexSubImage2D(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLint") int xoffset, @NativeType("GLint") int yoffset, @NativeType("GLsizei") int width, @NativeType("GLsizei") int height, @NativeType("GLenum") int format, @NativeType("GLenum") int type, @Nullable @NativeType("void const *") double[] pixels) {
        GL11M.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, vulkanmod$copyBytes(pixels));
    }

    @Overwrite(remap = false)
    public static void glTexParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
        if (params != null && params.length > 0) {
            GL11M.glTexParameteri(target, pname, params[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glTexParameterfv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
        if (params != null && params.length > 0) {
            GL11M.glTexParameterf(target, pname, params[0]);
        }
    }

    @Overwrite(remap = false)
    public static void glGetTexParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GL11M.glGetTexParameteri(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetTexParameterfv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] params) {
        if (params != null && params.length > 0) {
            params[0] = GL11M.glGetTexParameterf(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glPolygonStipple(@NativeType("GLubyte const *") ByteBuffer pattern) {
    }

    @Overwrite(remap = false)
    public static void glPolygonStipple(@NativeType("GLubyte const *") long pattern) {
    }

    @Overwrite(remap = false)
    public static void glGetPolygonStipple(@NativeType("void *") ByteBuffer pattern) {
        vulkanmod$zero(pattern);
    }

    @Overwrite(remap = false)
    public static void glGetPolygonStipple(@NativeType("void *") long pattern) {
    }

    @Overwrite(remap = false)
    public static void glGetPointerv(@NativeType("GLenum") int pname, @NativeType("void **") PointerBuffer params) {
        vulkanmod$zero(params);
    }

    @Overwrite(remap = false)
    public static void glTexEnvi(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint") int param) {
    }

    @Overwrite(remap = false)
    public static void glTexEnviv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint const *") IntBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glTexEnviv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint const *") int[] params) {
    }

    @Overwrite(remap = false)
    public static void glTexEnvf(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat") float param) {
    }

    @Overwrite(remap = false)
    public static void glTexEnvfv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") FloatBuffer params) {
    }

    @Overwrite(remap = false)
    public static void glTexEnvfv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLfloat const *") float[] params) {
    }

    @Overwrite(remap = false)
    public static void glGetTexEnviv(@NativeType("GLenum") int env, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetTexEnviv(@NativeType("GLenum") int env, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static int glGetTexEnvi(@NativeType("GLenum") int env, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetTexEnvfv(@NativeType("GLenum") int env, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static void glGetTexEnvfv(@NativeType("GLenum") int env, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] data) {
        vulkanmod$zero(data);
    }

    @Overwrite(remap = false)
    public static float glGetTexEnvf(@NativeType("GLenum") int env, @NativeType("GLenum") int pname) {
        return 0.0F;
    }

    @Overwrite(remap = false)
    public static void glGetTexLevelParameteriv(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        vulkanmod$zero(params);
    }

    @Overwrite(remap = false)
    public static void glGetTexLevelParameteriv(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        vulkanmod$zero(params);
    }

    @Overwrite(remap = false)
    public static int glGetTexLevelParameteri(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int pname) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glGetTexLevelParameterfv(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int pname, @NativeType("GLfloat *") FloatBuffer params) {
        vulkanmod$zero(params);
    }

    @Overwrite(remap = false)
    public static void glGetTexLevelParameterfv(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int pname, @NativeType("GLfloat *") float[] params) {
        vulkanmod$zero(params);
    }

    @Overwrite(remap = false)
    public static float glGetTexLevelParameterf(@NativeType("GLenum") int target, @NativeType("GLint") int level, @NativeType("GLenum") int pname) {
        return 0.0F;
    }

    @Overwrite(remap = false)
    public static void glPrioritizeTextures(@NativeType("GLuint const *") IntBuffer textures, @NativeType("GLfloat const *") FloatBuffer priorities) {
    }

    @Overwrite(remap = false)
    public static void glPrioritizeTextures(@NativeType("GLuint const *") int[] textures, @NativeType("GLfloat const *") float[] priorities) {
    }

    @Overwrite(remap = false)
    public static boolean glAreTexturesResident(@NativeType("GLuint const *") IntBuffer textures, @NativeType("GLboolean *") ByteBuffer residences) {
        vulkanmod$zero(residences);
        return false;
    }

    @Overwrite(remap = false)
    public static boolean glAreTexturesResident(@NativeType("GLuint const *") int texture, @NativeType("GLboolean *") ByteBuffer residences) {
        vulkanmod$zero(residences);
        return false;
    }

    @Overwrite(remap = false)
    public static boolean glAreTexturesResident(@NativeType("GLuint const *") int[] textures, @NativeType("GLboolean *") ByteBuffer residences) {
        vulkanmod$zero(residences);
        return false;
    }

    @Overwrite(remap = false)
    public static boolean glIsEnabled(@NativeType("GLenum") int target) {
        return GlCapabilityState.isEnabled(target);
    }

    @Overwrite(remap = false)
    public static int glGetInteger(@NativeType("GLenum") int pname) {
        return GL11C.glGetInteger(pname);
    }

    @Overwrite(remap = false)
    public static void glGetIntegerv(@NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        GL11C.glGetIntegerv(pname, params);
    }

    @Overwrite(remap = false)
    public static void glGetIntegerv(@NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        GL11C.glGetIntegerv(pname, params);
    }
}
