package net.vulkanmod.render.chunk.build.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;





public interface LightPipeline {









    void calculate(QuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade);
}
