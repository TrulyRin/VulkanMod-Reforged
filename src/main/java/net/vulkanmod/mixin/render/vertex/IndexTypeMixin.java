package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VertexFormat.IndexType.class)
public class IndexTypeMixin {

       
              
       
    @Overwrite(remap = false)
    public static VertexFormat.IndexType m_166933_(int number) {
        return VertexFormat.IndexType.SHORT;
    }
}
