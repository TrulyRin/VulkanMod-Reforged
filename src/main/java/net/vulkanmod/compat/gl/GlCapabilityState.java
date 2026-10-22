package net.vulkanmod.compat.gl;

import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.opengl.GL11;

public final class GlCapabilityState {
    private GlCapabilityState() {
    }

    public static boolean isEnabled(int cap) {
        return switch (cap) {
            case GL11.GL_DEPTH_TEST -> VRenderSystem.depthTest;
            case GL11.GL_STENCIL_TEST -> VRenderSystem.stencilTest;
            case GL11.GL_CULL_FACE -> VRenderSystem.cull;
            case GL11.GL_BLEND -> net.vulkanmod.vulkan.shader.PipelineState.blendInfo.enabled;
            case GL11.GL_SCISSOR_TEST -> Renderer.isScissorEnabled();
            default -> false;
        };
    }
}
