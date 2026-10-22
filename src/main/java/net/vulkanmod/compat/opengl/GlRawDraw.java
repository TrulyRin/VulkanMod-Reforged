package net.vulkanmod.compat.opengl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.PreparedDraw;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.vulkan.VK10.*;


public final class GlRawDraw {
    private GlRawDraw() {}

    public static boolean drawArrays(int mode, int first, int count) {
        if (!supportedMode(mode)) return false;
        if (first < 0 || count < 0) throw new IllegalArgumentException("Negative raw vertex range");
        if (count == 0) return true;
        var shader = RenderSystem.getShader();
        if (!(shader instanceof ShaderMixed owner)) return false;
        var plan = GlRawDrawPlan.arrays(shader.getVertexFormat(), first, count);
        return plan != null && submit(mode, owner, plan);
    }

    public static boolean drawElements(int mode, int count, int type, long indexByteOffset) {
        if (!supportedMode(mode)) return false;
        if (count < 0 || indexByteOffset < 0) throw new IllegalArgumentException("Negative raw index range");
        if (type != GL_UNSIGNED_SHORT && type != GL_UNSIGNED_INT) return false;
        if (indexByteOffset % (type == GL_UNSIGNED_SHORT ? Short.BYTES : Integer.BYTES) != 0)
            throw new IllegalArgumentException("Invalid raw index byte offset");
        if (count == 0) return true;
        var shader = RenderSystem.getShader();
        if (!(shader instanceof ShaderMixed owner)) return false;
        var plan = GlRawDrawPlan.elements(shader.getVertexFormat(), count, type, indexByteOffset);
        return plan != null && submit(mode, owner, plan);
    }

    private static boolean supportedMode(int mode) { return mode == GL_TRIANGLES || mode == GL_TRIANGLE_STRIP || mode == GL_TRIANGLE_FAN; }

    private static boolean submit(int mode, ShaderMixed owner, GlRawDrawPlan plan) {
        if (Renderer.skipRendering || !Renderer.isRecording()) return true;
        GraphicsPipeline pipeline = owner.getRawPipeline(plan.state.format());
        if (pipeline == null || pipeline.hasTessellation()) return false;
        plan.requireCurrent();
        Renderer renderer = Renderer.getInstance();
        var previous = renderer.getBoundPipeline();
        int previousTopology = VRenderSystem.topology;
        long units = 0;
        for (var descriptor : pipeline.getImageDescriptors()) {
            if (!descriptor.usesVanillaTextureBinding()) continue;
            int unit = descriptor.getTextureUnit();
            if (unit < 0 || unit >= VTextureSelector.SIZE) throw new IllegalArgumentException("Raw shader texture unit is out of range");
            units |= 1L << unit;
        }
        var textures = VTextureSelector.captureBindings(units);
        try {
            VRenderSystem.topology = switch (mode) {
                case GL_TRIANGLE_STRIP -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
                case GL_TRIANGLE_FAN -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN;
                default -> VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            };
            GlFramebuffer.ensureDrawFramebufferRendering();
            var drawer = Renderer.getDrawer();
            var range = new PreparedDraw();
            drawer.prepare(range, plan.vertices, plan.state, plan.indices);
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline, owner);
            renderer.uploadAndBindUBOs(pipeline);
            drawer.drawPrepared(range, false);
            return true;
        } finally {
            VRenderSystem.topology = previousTopology;
            textures.restore();
            renderer.restoreGraphicsPipeline(previous);
        }
    }
}
