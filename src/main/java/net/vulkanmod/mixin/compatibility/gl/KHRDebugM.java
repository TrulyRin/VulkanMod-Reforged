package net.vulkanmod.mixin.compatibility.gl;

import org.lwjgl.opengl.GLDebugMessageCallbackI;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(KHRDebug.class)
public class KHRDebugM {
    @Overwrite(remap = false)
    public static void glDebugMessageControl(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLenum") int severity, @NativeType("GLuint const *") IntBuffer ids, @NativeType("GLboolean") boolean enabled) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageControl(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLenum") int severity, @NativeType("GLuint const *") int id, @NativeType("GLboolean") boolean enabled) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageInsert(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLuint") int id, @NativeType("GLenum") int severity, @NativeType("GLchar const *") ByteBuffer message) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageInsert(@NativeType("GLenum") int source, @NativeType("GLenum") int type, @NativeType("GLuint") int id, @NativeType("GLenum") int severity, @NativeType("GLchar const *") CharSequence message) {
    }

    @Overwrite(remap = false)
    public static void glDebugMessageCallback(@NativeType("GLDEBUGPROC") GLDebugMessageCallbackI callback, @NativeType("void const *") long userParam) {
    }

    @Overwrite(remap = false)
    public static int glGetDebugMessageLog(@NativeType("GLuint") int count, @NativeType("GLenum *") IntBuffer sources, @NativeType("GLenum *") IntBuffer types, @NativeType("GLuint *") IntBuffer ids, @NativeType("GLenum *") IntBuffer severities, @NativeType("GLsizei *") IntBuffer lengths, @NativeType("GLchar *") ByteBuffer messageLog) {
        return 0;
    }

    @Overwrite(remap = false)
    public static int glGetDebugMessageLog(@NativeType("GLuint") int count, @NativeType("GLenum *") int[] sources, @NativeType("GLenum *") int[] types, @NativeType("GLuint *") int[] ids, @NativeType("GLenum *") int[] severities, @NativeType("GLsizei *") int[] lengths, @NativeType("GLchar *") ByteBuffer messageLog) {
        return 0;
    }

    @Overwrite(remap = false)
    public static void glPushDebugGroup(@NativeType("GLenum") int source, @NativeType("GLuint") int id, @NativeType("GLchar const *") ByteBuffer message) {
    }

    @Overwrite(remap = false)
    public static void glPushDebugGroup(@NativeType("GLenum") int source, @NativeType("GLuint") int id, @NativeType("GLchar const *") CharSequence message) {
    }

    @Overwrite(remap = false)
    public static void glPopDebugGroup() {
    }

    @Overwrite(remap = false)
    public static void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLchar const *") ByteBuffer label) {
    }

    @Overwrite(remap = false)
    public static void glObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLchar const *") CharSequence label) {
    }

    @Overwrite(remap = false)
    public static void glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer label) {
        if (length != null && length.remaining() > 0) {
            length.put(length.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer label) {
        if (length != null && length.length > 0) {
            length[0] = 0;
        }
    }

    @Overwrite(remap = false)
    public static String glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetObjectLabel(@NativeType("GLenum") int identifier, @NativeType("GLuint") int name) {
        return "";
    }

    @Overwrite(remap = false)
    public static void glObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLchar const *") ByteBuffer label) {
    }

    @Overwrite(remap = false)
    public static void glObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLchar const *") CharSequence label) {
    }

    @Overwrite(remap = false)
    public static void glGetObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLsizei *") IntBuffer length, @NativeType("GLchar *") ByteBuffer label) {
        if (length != null && length.remaining() > 0) {
            length.put(length.position(), 0);
        }
    }

    @Overwrite(remap = false)
    public static void glGetObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLsizei *") int[] length, @NativeType("GLchar *") ByteBuffer label) {
        if (length != null && length.length > 0) {
            length[0] = 0;
        }
    }

    @Overwrite(remap = false)
    public static String glGetObjectPtrLabel(@NativeType("void *") long ptr, @NativeType("GLsizei") int bufSize) {
        return "";
    }

    @Overwrite(remap = false)
    public static String glGetObjectPtrLabel(@NativeType("void *") long ptr) {
        return "";
    }
}
