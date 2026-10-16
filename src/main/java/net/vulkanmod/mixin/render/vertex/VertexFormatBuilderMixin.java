package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.vulkanmod.render.vertex.VertexFormatAlignment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VertexFormat.Builder.class)
public abstract class VertexFormatBuilderMixin {
    @Shadow
    private int offset;

    @Shadow
    public abstract VertexFormat.Builder padding(int bytes);

    @Unique
    private int vulkanmod$largestComponentSize = 1;

    @Inject(method = "add", at = @At("HEAD"))
    private void alignElement(String name, VertexFormatElement element,
                              CallbackInfoReturnable<VertexFormat.Builder> callback) {
        int componentSize = element.type().size();
        this.padding(VertexFormatAlignment.padding(this.offset, componentSize));
        this.vulkanmod$largestComponentSize = Math.max(this.vulkanmod$largestComponentSize, componentSize);
    }

    @Inject(method = "build", at = @At("HEAD"))
    private void alignStride(CallbackInfoReturnable<VertexFormat> callback) {
        this.padding(VertexFormatAlignment.padding(this.offset, this.vulkanmod$largestComponentSize));
    }
}
