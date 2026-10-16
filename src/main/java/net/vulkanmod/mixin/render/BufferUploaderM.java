package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BufferUploader.class)
public class BufferUploaderM {

    private static final java.util.Set<String> VM_DBG_LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();

       
              
       
    @Overwrite(remap = false)
    public static void m_166835_() {}

       
              
       
    @Overwrite(remap = false)
    public static void m_231202_(BufferBuilder.RenderedBuffer renderedBuffer) {
        RenderSystem.assertOnRenderThread();

        BufferBuilder.DrawState parameters = renderedBuffer.drawState();

        Renderer renderer = Renderer.getInstance();

        if (parameters.vertexCount() > 0) {
            ShaderInstance shaderInstance = RenderSystem.getShader();

            String vmDbgName = shaderInstance.getName();
            if (vmDbgName != null && vmDbgName.contains(":") && VM_DBG_LOGGED.add(vmDbgName)) {
                net.vulkanmod.Initializer.LOGGER.info("[VM-DBG] draw custom shader '{}' shaderFmt={} bufFmt={} fmtMatch={} verts={} pipeline={}",
                        vmDbgName, shaderInstance.getVertexFormat(), parameters.format(),
                        shaderInstance.getVertexFormat() == parameters.format(), parameters.vertexCount(),
                        ((ShaderMixed) shaderInstance).getPipeline() != null);
            }

                                                                                     
            if (shaderInstance.getVertexFormat() != renderedBuffer.drawState().format()) {
                renderedBuffer.release();
                return;
            }

                                                    
                                                                                         
            shaderInstance.apply();

            GraphicsPipeline pipeline = ((ShaderMixed)(shaderInstance)).getPipeline();
            if (pipeline == null) {
                renderedBuffer.release();
                return;
            }

            VRenderSystem.setPrimitiveTopologyGL(parameters.mode().asGLMode);
            renderer.bindGraphicsPipeline(pipeline);
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().draw(renderedBuffer.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        renderedBuffer.release();
    }

       
              
       
    @Overwrite(remap = false)
    public static void m_231209_(BufferBuilder.RenderedBuffer renderedBuffer) {
        BufferBuilder.DrawState parameters = renderedBuffer.drawState();

        if (parameters.vertexCount() > 0) {
            Renderer.getDrawer().draw(renderedBuffer.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        renderedBuffer.release();
    }

}

