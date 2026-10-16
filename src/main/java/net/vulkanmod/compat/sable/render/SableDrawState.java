package net.vulkanmod.compat.sable.render;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;

public final class SableDrawState {
    private SableDrawState() {
    }

    public static boolean applyMatrices(ScopedUniformResolver.UniformSource source) {
        MappedBuffer modelView = source.uniformBuffer("ModelViewMat");
        MappedBuffer projection = source.uniformBuffer("ProjMat");
        if (modelView == null || projection == null) {
            return false;
        }
        Matrix4f modelViewMatrix = new Matrix4f().set(modelView.buffer.asFloatBuffer());
        Matrix4f projectionMatrix = new Matrix4f().set(projection.buffer.asFloatBuffer());
        VRenderSystem.applyMVP(modelViewMatrix, projectionMatrix);
        return true;
    }
}
