package net.vulkanmod.mixin.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ModelPart.Cube.class)
public class ModelPartCubeM implements ModelPartCubeMixed {

    CubeModel cube;

                                                                                            
                                                                                                 
                                                                                               
                                                                                            
                                                                                              
                                                                                            
                                                                                               
                                                                                                
               
    @Inject(method = "<init>", at = @At("RETURN"))
    private void getVertices(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float q, float r, Set<Direction> set, CallbackInfo ci) {
        ModelPart.Cube self = (ModelPart.Cube) (Object) this;
        CubeModel cube = new CubeModel();
        cube.setVertices(i, j, self.minX, self.minY, self.minZ, k, l, m, n, o, p, bl, q, r, set);
        this.cube = cube;
    }


    @Override
    public CubeModel getCubeModel() {
        return this.cube;
    }
}
