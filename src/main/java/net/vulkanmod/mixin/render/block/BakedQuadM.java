package net.vulkanmod.mixin.render.block;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.vulkanmod.render.model.quad.ModelQuadFlags;
import net.vulkanmod.render.model.quad.QuadView;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

   
                                                                                             
              
  
                                                                                                  
                                                                                                  
                                                                                                  
                                              
  
                                                                                                   
                                                                                                  
   
@Mixin(BakedQuad.class)
public abstract class BakedQuadM implements QuadView {

    @Shadow public abstract Vector3fc position(int index);

    @Shadow public abstract long packedUV(int index);

    @Shadow public abstract int tintIndex();

    @Shadow public abstract Direction direction();

    @Shadow public abstract BakedColors bakedColors();

    @Unique private int vulkanmod$flags = -1;

    @Override
    public int getFlags() {
        int flags = this.vulkanmod$flags;

        if (flags == -1) {
            flags = ModelQuadFlags.getQuadFlags(this, this.direction());
            this.vulkanmod$flags = flags;
        }

        return flags;
    }

    @Override
    public float getX(int idx) {
        return this.position(idx).x();
    }

    @Override
    public float getY(int idx) {
        return this.position(idx).y();
    }

    @Override
    public float getZ(int idx) {
        return this.position(idx).z();
    }

    @Override
    public int getColor(int idx) {
        return this.bakedColors().color(idx);
    }

    @Override
    public float getU(int idx) {
        return UVPair.unpackU(this.packedUV(idx));
    }

    @Override
    public float getV(int idx) {
        return UVPair.unpackV(this.packedUV(idx));
    }

    @Override
    public int getColorIndex() {
        return this.tintIndex();
    }

    @Override
    public Direction getFacingDirection() {
        return this.direction();
    }

    @Override
    public boolean isTinted() {
        return this.tintIndex() != -1;
    }
}
