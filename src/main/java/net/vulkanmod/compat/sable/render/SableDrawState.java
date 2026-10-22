package net.vulkanmod.compat.sable.render;

import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class SableDrawState {
    private static final ThreadLocal<Matrices> MATRICES = ThreadLocal.withInitial(Matrices::new);

    private static final class Matrices {
        final Matrix4f modelView = new Matrix4f();
        final Matrix4f projection = new Matrix4f();
        final Matrix4f combined = new Matrix4f();
        final ByteBuffer bytes = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
        final FloatBuffer floats = bytes.asFloatBuffer();
        final MappedBuffer view = MappedBuffer.createFromBuffer(bytes.duplicate().order(ByteOrder.nativeOrder()));
    }

    private SableDrawState() {
    }

    public static MappedBuffer mvp(ScopedUniformResolver.UniformSource source) {
        if (source.uniformBuffer("ModelViewMat") == null || source.uniformBuffer("ProjMat") == null) return null;
        MappedBuffer modelView = source.uploadUniformBuffer("ModelViewMat", "mat4");
        MappedBuffer projection = source.uploadUniformBuffer("ProjMat", "mat4");
        if (modelView == null || projection == null) {
            return null;
        }
        Matrices matrices = MATRICES.get();
        readMatrix(matrices.modelView, modelView);
        readMatrix(matrices.projection, projection);
        Matrix4f projectionMatrix = matrices.projection;
        projectionMatrix.m02((projectionMatrix.m02() + projectionMatrix.m03()) * .5f);
        projectionMatrix.m12((projectionMatrix.m12() + projectionMatrix.m13()) * .5f);
        projectionMatrix.m22((projectionMatrix.m22() + projectionMatrix.m23()) * .5f);
        projectionMatrix.m32((projectionMatrix.m32() + projectionMatrix.m33()) * .5f);
        projectionMatrix.mul(matrices.modelView, matrices.combined);
        matrices.floats.clear();
        matrices.combined.get(matrices.floats);
        return matrices.view;
    }

    private static void readMatrix(Matrix4f matrix, MappedBuffer source) {
        if (!source.buffer.isDirect() || source.buffer.capacity() < 16 * Float.BYTES)
            throw new IllegalArgumentException("Sable matrix source requires 64 direct packed bytes");
        matrix.set(source.getFloat(0), source.getFloat(4), source.getFloat(8), source.getFloat(12),
                source.getFloat(16), source.getFloat(20), source.getFloat(24), source.getFloat(28),
                source.getFloat(32), source.getFloat(36), source.getFloat(40), source.getFloat(44),
                source.getFloat(48), source.getFloat(52), source.getFloat(56), source.getFloat(60));
    }
}
