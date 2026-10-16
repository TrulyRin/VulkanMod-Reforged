package net.vulkanmod.mixin.compatibility.sable;

import net.vulkanmod.gl.GlVertexArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = {
        "foundry.veil.impl.client.render.vertex.LegacyVertexAttribBindingBuilder$FloatAttribute",
        "foundry.veil.impl.client.render.vertex.LegacyVertexAttribBindingBuilder$IntAttribute",
        "foundry.veil.impl.client.render.vertex.LegacyVertexAttribBindingBuilder$LongAttribute"
}, remap = false)
public abstract class VeilVertexAttribDivisorMixin {
    @Redirect(
            method = "apply(ILfoundry/veil/impl/client/render/vertex/LegacyVertexAttribBindingBuilder$VertexBufferRegion;)V",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glVertexAttribDivisor(II)V"),
            remap = false,
            require = 1)
    private static void vulkanmod$vertexAttribDivisor(int index, int divisor) {
        GlVertexArray.vertexAttribDivisor(index, divisor);
    }
}
