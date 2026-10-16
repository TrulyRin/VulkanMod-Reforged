package net.vulkanmod.interfaces;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.compat.sable.render.ScopedUniformResolver;

public interface ShaderMixed extends ScopedUniformResolver.UniformSource {

    GraphicsPipeline getPipeline();

    GraphicsPipeline getPipeline(VertexFormat drawFormat);

    GraphicsPipeline getSablePipeline(VertexFormat drawFormat);
}
