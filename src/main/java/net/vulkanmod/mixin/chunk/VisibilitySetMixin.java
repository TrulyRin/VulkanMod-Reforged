package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.Direction;
import net.vulkanmod.interfaces.VisibilitySetExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VisibilitySet.class)
public class VisibilitySetMixin implements VisibilitySetExtended {


    private long vis = 0;





    @Overwrite
    public void set(Direction dir1, Direction dir2, boolean p_112989_) {

        this.vis |= 1L << ((dir1.ordinal() << 3) + dir2.ordinal()) | 1L << ((dir2.ordinal() << 3) + dir1.ordinal());

    }





    @Overwrite
    public void setAll(boolean bl) {
        if(bl) this.vis = 0xFFFFFFFFFFFFFFFFL;
    }





    @Overwrite
    public boolean visibilityBetween(Direction dir1, Direction dir2) {

        return (this.vis & (1L << ((dir1.ordinal() << 3) + dir2.ordinal()))) != 0;
    }

    @Override
    public long getVisibility() {
        return vis;
    }
}
