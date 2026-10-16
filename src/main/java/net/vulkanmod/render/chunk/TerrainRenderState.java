package net.vulkanmod.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL11;

public final class TerrainRenderState {
    private TerrainRenderState() {
    }

    public static void prepareWorldTerrainState() {
        VRenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        VRenderSystem.colorMask(true, true, true, true);
        VRenderSystem.enableDepthTest();
        VRenderSystem.depthMask(true);
        VRenderSystem.enableCull();
        VRenderSystem.disableBlend();
        VRenderSystem.depthFunc(GL11.GL_LEQUAL);
        VRenderSystem.setPolygonModeGL(GL11.GL_FILL);
                                                                                             
                                                                                   
        Renderer.resetScissor();
    }
}
