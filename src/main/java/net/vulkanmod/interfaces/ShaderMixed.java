package net.vulkanmod.interfaces;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.compat.sable.render.ScopedUniformResolver;

public interface ShaderMixed extends ScopedUniformResolver.UniformSource {

    enum BlitContract { CANONICAL_COPY, LEGACY, UNSUPPORTED }

    BlitContract blitContract();

    GraphicsPipeline getPipeline();

    GraphicsPipeline getPipeline(VertexFormat drawFormat);


    default GraphicsPipeline getRawPipeline(VertexFormat drawFormat) { return getPipeline(drawFormat); }


    int samplerTextureId(Pipeline drawPipeline, String samplerName);

    GraphicsPipeline getSablePipeline(VertexFormat drawFormat);
}
