package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.gl.GlVertexArray;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.system.NativeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.nio.IntBuffer;

@Mixin(ARBVertexArrayObject.class)
public class ARBVertexArrayObjectM {
    @Overwrite(remap = false)
    public static void glBindVertexArray(@NativeType("GLuint") int array) {
        GlVertexArray.bindVertexArray(array);
    }

    @Overwrite(remap = false)
    public static void glDeleteVertexArrays(@NativeType("GLuint const *") IntBuffer arrays) {
        GlVertexArray.deleteVertexArrays(arrays);
    }

    @Overwrite(remap = false)
    public static void glDeleteVertexArrays(@NativeType("GLuint const *") int array) {
        GlVertexArray.deleteVertexArray(array);
    }

    @Overwrite(remap = false)
    public static void glDeleteVertexArrays(@NativeType("GLuint const *") int[] arrays) {
        if (arrays == null) {
            return;
        }
        for (int array : arrays) {
            GlVertexArray.deleteVertexArray(array);
        }
    }

    @Overwrite(remap = false)
    public static void glGenVertexArrays(@NativeType("GLuint *") IntBuffer arrays) {
        GlVertexArray.genVertexArrays(arrays);
    }

    @Overwrite(remap = false)
    @NativeType("void")
    public static int glGenVertexArrays() {
        return GlVertexArray.genVertexArray();
    }

    @Overwrite(remap = false)
    public static void glGenVertexArrays(@NativeType("GLuint *") int[] arrays) {
        if (arrays == null) {
            return;
        }
        for (int i = 0; i < arrays.length; i++) {
            arrays[i] = GlVertexArray.genVertexArray();
        }
    }

    @Overwrite(remap = false)
    @NativeType("GLboolean")
    public static boolean glIsVertexArray(@NativeType("GLuint") int array) {
        return GlVertexArray.isVertexArray(array);
    }
}
