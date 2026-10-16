package net.vulkanmod.render.chunk.build.light.flat;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.vulkanmod.render.chunk.util.SimpleDirection;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.chunk.build.light.data.LightDataAccess;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.model.quad.ModelQuadFlags;

import java.util.Arrays;

import static net.vulkanmod.render.chunk.build.light.data.LightDataAccess.*;

   
                                                                                                                  
                   
   
public class FlatLightPipeline implements LightPipeline {
       
                                                        
       
    private final LightDataAccess lightCache;

    public FlatLightPipeline(LightDataAccess lightCache) {
        this.lightCache = lightCache;
    }

    @Override
    public void calculate(QuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade) {
        int lightmap;

                                                                                 
        if (cullFace != null) {
            lightmap = getLightmap(pos, cullFace);
        } else {
            int flags = quad.getFlags();
                                                                  
                                                                                                                             
            if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && unpackFC(this.lightCache.get(pos)))) {
                lightmap = getLightmap(pos, lightFace);
            } else {
                lightmap = getEmissiveLightmap(this.lightCache.get(pos));
            }
        }

        Arrays.fill(out.lm, lightmap);
        Arrays.fill(out.br, this.lightCache.getWorld().getShade(lightFace, shade));
    }

    private int getLightmap(BlockPos pos, Direction face) {
        int word = this.lightCache.get(pos);

                                               
        if (unpackEM(word)) {
            return LightTexture.FULL_BRIGHT;
        }

        int adjWord = this.lightCache.get(pos, SimpleDirection.of(face));
        return LightDataAccess.getLightmap(adjWord);
    }
}
