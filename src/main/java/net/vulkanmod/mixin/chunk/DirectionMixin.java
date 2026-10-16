package net.vulkanmod.mixin.chunk;

import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Direction.class)
public class DirectionMixin {

    @Shadow(remap = false) @Final private static Direction[] f_122348_;

    @Shadow(remap = false) @Final private int f_122340_;

       
              
              
       
    @Overwrite(remap = false)
    public Direction m_122424_() {
        return f_122348_[this.f_122340_];
    }
}

