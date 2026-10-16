package net.vulkanmod.interfaces;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

public interface ShaderMixed {

    GraphicsPipeline getPipeline();

    GraphicsPipeline getPipeline(VertexFormat drawFormat);
}

