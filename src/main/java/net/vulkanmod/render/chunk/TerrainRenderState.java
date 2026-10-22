package net.vulkanmod.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL11;

public final class TerrainRenderState {
    private TerrainRenderState() {
    }

    public static void prepareWorldTerrainState() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        VRenderSystem.depthFunc(GL11.GL_LEQUAL);
        VRenderSystem.setPolygonModeGL(GL11.GL_FILL);
        VRenderSystem.frontFace(GL11.GL_CCW);
        Renderer.resetScissor();
    }
}
