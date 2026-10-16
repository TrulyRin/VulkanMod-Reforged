package net.vulkanmod.mixin.render;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

   
                                                                                          
                                                                                  
   
@Mixin(Camera.class)
public interface CameraAccessor {

    @Accessor("xRot")
    float getXRot();

    @Accessor("yRot")
    float getYRot();
}
