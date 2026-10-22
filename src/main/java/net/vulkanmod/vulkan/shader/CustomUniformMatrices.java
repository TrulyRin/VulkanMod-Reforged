package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.shaders.Uniform;
import net.vulkanmod.gl.GlProgram;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;


public final class CustomUniformMatrices {
    private CustomUniformMatrices() {}

    public static void copy(Uniform owner, Matrix4f destination, String name) {
        if (!(owner instanceof CustomUniformSource source)) {
            FloatBuffer values = owner.getFloatBuffer();
            if (values == null || values.limit() < 16)
                throw new IllegalArgumentException("Uniform " + name + " has no readable mat4 storage");

            destination.set(values.get(0), values.get(1), values.get(2), values.get(3),
                    values.get(4), values.get(5), values.get(6), values.get(7),
                    values.get(8), values.get(9), values.get(10), values.get(11),
                    values.get(12), values.get(13), values.get(14), values.get(15));
            return;
        }
        LegacyUniformSupplier.requireMatches(source.descriptor(), "mat4", 0, name);
        destination.set(source.previewFloat(0), source.previewFloat(1), source.previewFloat(2), source.previewFloat(3),
                source.previewFloat(4), source.previewFloat(5), source.previewFloat(6), source.previewFloat(7),
                source.previewFloat(8), source.previewFloat(9), source.previewFloat(10), source.previewFloat(11),
                source.previewFloat(12), source.previewFloat(13), source.previewFloat(14), source.previewFloat(15));
    }


    public static void copyProgram(int owner, long generation, Matrix4f destination, String name) {
        requireProgramGeneration(owner, generation);
        var values = GlProgram.uniformBuffer(owner, name);
        if (values == null || values.buffer == null || !values.buffer.isDirect()
                || values.ptr == 0 || values.buffer.capacity() < 16 * Float.BYTES)
            throw new IllegalArgumentException("Program uniform " + name + " has no readable mat4 storage");
        destination.set(values.getFloat(0), values.getFloat(4), values.getFloat(8), values.getFloat(12),
                values.getFloat(16), values.getFloat(20), values.getFloat(24), values.getFloat(28),
                values.getFloat(32), values.getFloat(36), values.getFloat(40), values.getFloat(44),
                values.getFloat(48), values.getFloat(52), values.getFloat(56), values.getFloat(60));
        requireProgramGeneration(owner, generation);
    }

    public static void requireProgramGeneration(int owner, long generation) {
        var linked = GlProgram.linkedProgram(owner);
        if (owner <= 0 || linked == null || linked.generation() != generation)
            throw new IllegalStateException("Matrix snapshot program owner was removed or relinked");
    }
}
