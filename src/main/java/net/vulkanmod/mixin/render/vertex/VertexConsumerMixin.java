package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.vulkanmod.render.vertex.BulkQuadWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {
       
                        
                                                                                      
       
    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad,
                             float red, float green, float blue, float alpha, int light, int overlay) {
        BulkQuadWriter.putBulkData((VertexConsumer) (Object) this, pose, quad, red, green, blue, alpha, light, overlay);
    }

       
                        
                                                                                      
       
    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness,
                             float red, float green, float blue, float alpha, int[] lights, int overlay) {
        BulkQuadWriter.putBulkData((VertexConsumer) (Object) this, pose, quad, brightness,
                red, green, blue, alpha, lights, overlay);
    }
}
