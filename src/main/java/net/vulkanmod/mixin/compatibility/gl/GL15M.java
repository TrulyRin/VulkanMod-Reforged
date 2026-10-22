package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlQuery;
import org.lwjgl.PointerBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

@Mixin(GL15C.class)
public class GL15M {
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
    private static ByteBuffer vulkanmod$copyBytes(@Nullable LongBuffer data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Long.BYTES).order(ByteOrder.nativeOrder());
        LongBuffer view = bytes.asLongBuffer();
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
    private static ByteBuffer vulkanmod$copyBytes(@Nullable long[] data) {
        if (data == null) {
            return null;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Long.BYTES).order(ByteOrder.nativeOrder());
        bytes.asLongBuffer().put(data);
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

    private static void vulkanmod$getBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, ShortBuffer data) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Short.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        ShortBuffer view = bytes.asShortBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, view.get(i - data.position()));
        }
    }

    private static void vulkanmod$getBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, IntBuffer data) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Integer.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        IntBuffer view = bytes.asIntBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, view.get(i - data.position()));
        }
    }

    private static void vulkanmod$getBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, LongBuffer data) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Long.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        LongBuffer view = bytes.asLongBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, view.get(i - data.position()));
        }
    }

    private static void vulkanmod$getBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, FloatBuffer data) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Float.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        FloatBuffer view = bytes.asFloatBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, view.get(i - data.position()));
        }
    }

    private static void vulkanmod$getBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, DoubleBuffer data) {
        ByteBuffer bytes = ByteBuffer.allocateDirect(data.remaining() * Double.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        DoubleBuffer view = bytes.asDoubleBuffer();
        for (int i = data.position(); i < data.limit(); ++i) {
            data.put(i, view.get(i - data.position()));
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenBuffers() {
        return GlBuffer.glGenBuffers();
    }

    @Overwrite(remap = false)
    public static void glGenBuffers(@NativeType("GLuint *") IntBuffer buffers) {
        for (int i = buffers.position(); i < buffers.limit(); i++) {
            buffers.put(i, GlBuffer.glGenBuffers());
        }
    }

    @Overwrite(remap = false)
    public static void glGenBuffers(@NativeType("GLuint *") int[] buffers) {
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = GlBuffer.glGenBuffers();
        }
    }

    @Overwrite(remap = false)
    public static void glBindBuffer(@NativeType("GLenum") int target, @NativeType("GLuint") int buffer) {
        GlBuffer.glBindBuffer(target, buffer);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") ByteBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, data, usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") ShortBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") IntBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") LongBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") FloatBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") DoubleBuffer data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") short[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") int[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") long[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") float[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(@NativeType("GLenum") int target, @NativeType("void const *") double[] data, @NativeType("GLenum") int usage) {
        GlBuffer.glBufferData(target, vulkanmod$copyBytes(data), usage);
    }

    @Overwrite(remap = false)
    public static void glBufferData(int i, long l, int j) {
        GlBuffer.glBufferData(i, l, j);
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") ByteBuffer data) {
        GlBuffer.glBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") ShortBuffer data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") IntBuffer data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") LongBuffer data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") FloatBuffer data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") DoubleBuffer data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") short[] data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") int[] data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") long[] data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") float[] data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static void glBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void const *") double[] data) {
        GlBuffer.glBufferSubData(target, offset, vulkanmod$copyBytes(data));
    }

    @Overwrite(remap = false)
    public static boolean glIsBuffer(@NativeType("GLuint") int buffer) {
        return GlBuffer.glIsBuffer(buffer);
    }

    @Overwrite(remap = false)
    @NativeType("void *")
    public static ByteBuffer glMapBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int access) {
        return GlBuffer.glMapBuffer(target, access);
    }

    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int access, @Nullable ByteBuffer old_buffer) {
        return GlBuffer.glMapBuffer(target, access);
    }

    @Overwrite(remap = false)
    @Nullable
    @NativeType("void *")
    public static ByteBuffer glMapBuffer(@NativeType("GLenum") int target, @NativeType("GLenum") int access, long length, @Nullable ByteBuffer old_buffer) {
        return GlBuffer.glMapBuffer(target, access);
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glUnmapBuffer(@NativeType("GLenum") int target) {
        return GlBuffer.glUnmapBuffer(target);
    }

    @Overwrite(remap = false)
    public static void glDeleteBuffers(int i) {
        GlBuffer.glDeleteBuffers(i);
    }

    @Overwrite(remap = false)
    public static void glDeleteBuffers(@NativeType("GLuint const *") IntBuffer buffers) {
        GlBuffer.glDeleteBuffers(buffers);
    }

    @Overwrite(remap = false)
    public static void glDeleteBuffers(@NativeType("GLuint const *") int[] buffers) {
        for (int buffer : buffers) {
            GlBuffer.glDeleteBuffers(buffer);
        }
    }

    @Overwrite(remap = false)
    public static int glGetBufferParameteri(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlBuffer.glGetBufferParameteri(target, pname);
    }

    @Overwrite(remap = false)
    public static void glGetBufferParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlBuffer.glGetBufferParameteri(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetBufferParameteriv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlBuffer.glGetBufferParameteri(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") ByteBuffer data) {
        GlBuffer.glGetBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") ShortBuffer data) {
        vulkanmod$getBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") IntBuffer data) {
        vulkanmod$getBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") LongBuffer data) {
        vulkanmod$getBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") FloatBuffer data) {
        vulkanmod$getBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") DoubleBuffer data) {
        vulkanmod$getBufferSubData(target, offset, data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") short[] data) {
        if (data == null) {
            return;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Short.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        bytes.asShortBuffer().get(data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") int[] data) {
        if (data == null) {
            return;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Integer.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        bytes.asIntBuffer().get(data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") long[] data) {
        if (data == null) {
            return;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Long.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        bytes.asLongBuffer().get(data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") float[] data) {
        if (data == null) {
            return;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Float.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        bytes.asFloatBuffer().get(data);
    }

    @Overwrite(remap = false)
    public static void glGetBufferSubData(@NativeType("GLenum") int target, @NativeType("GLintptr") long offset, @NativeType("void *") double[] data) {
        if (data == null) {
            return;
        }

        ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * Double.BYTES).order(ByteOrder.nativeOrder());
        GlBuffer.glGetBufferSubData(target, offset, bytes);
        bytes.asDoubleBuffer().get(data);
    }

    @Overwrite(remap = false)
    public static long glGetBufferPointer(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return 0L;
    }

    @Overwrite(remap = false)
    public static void glGetBufferPointerv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("void **") PointerBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), 0L);
        }
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenQueries() {
        return GlQuery.genQueries();
    }

    @Overwrite(remap = false)
    public static void glGenQueries(@NativeType("GLuint *") IntBuffer ids) {
        GlQuery.genQueries(ids);
    }

    @Overwrite(remap = false)
    public static void glGenQueries(@NativeType("GLuint *") int[] ids) {
        for (int i = 0; i < ids.length; i++) {
            ids[i] = GlQuery.genQueries();
        }
    }

    @Overwrite(remap = false)
    public static void glDeleteQueries(@NativeType("GLuint const *") int id) {
        GlQuery.deleteQueries(id);
    }

    @Overwrite(remap = false)
    public static void glDeleteQueries(@NativeType("GLuint const *") IntBuffer ids) {
        GlQuery.deleteQueries(ids);
    }

    @Overwrite(remap = false)
    public static void glDeleteQueries(@NativeType("GLuint const *") int[] ids) {
        for (int id : ids) {
            GlQuery.deleteQueries(id);
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsQuery(@NativeType("GLuint") int id) {
        return GlQuery.isQuery(id);
    }

    @Overwrite(remap = false)
    public static void glBeginQuery(@NativeType("GLenum") int target, @NativeType("GLuint") int id) {
        GlQuery.beginQuery(target, id);
    }

    @Overwrite(remap = false)
    public static void glEndQuery(@NativeType("GLenum") int target) {
        GlQuery.endQuery(target);
    }

    @Overwrite(remap = false)
    public static int glGetQueryi(@NativeType("GLenum") int target, @NativeType("GLenum") int pname) {
        return GlQuery.getQueryi(target, pname);
    }

    @Overwrite(remap = false)
    public static void glGetQueryiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), GlQuery.getQueryi(target, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetQueryiv(@NativeType("GLenum") int target, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = GlQuery.getQueryi(target, pname);
        }
    }

    @Overwrite(remap = false)
    public static int glGetQueryObjecti(@NativeType("GLuint") int id, @NativeType("GLenum") int pname) {
        return (int) GlQuery.getQueryObject(id, pname);
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectiv(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), (int) GlQuery.getQueryObject(id, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectiv(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLint *") long params) {
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectiv(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = (int) GlQuery.getQueryObject(id, pname);
        }
    }

    @Overwrite(remap = false)
    public static int glGetQueryObjectui(@NativeType("GLuint") int id, @NativeType("GLenum") int pname) {
        return (int) GlQuery.getQueryObject(id, pname);
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectuiv(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLuint *") IntBuffer params) {
        if (params != null && params.remaining() > 0) {
            params.put(params.position(), (int) GlQuery.getQueryObject(id, pname));
        }
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectuiv(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLuint *") long params) {
    }

    @Overwrite(remap = false)
    public static void glGetQueryObjectuiv(@NativeType("GLuint") int id, @NativeType("GLenum") int pname, @NativeType("GLuint *") int[] params) {
        if (params != null && params.length > 0) {
            params[0] = (int) GlQuery.getQueryObject(id, pname);
        }
    }
}
