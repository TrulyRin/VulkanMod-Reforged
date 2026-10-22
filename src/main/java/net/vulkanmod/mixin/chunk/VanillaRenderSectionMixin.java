package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.vulkanmod.render.chunk.VanillaSectionInvalidation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class VanillaRenderSectionMixin {
    @Inject(method = "setDirty(Z)V", at = @At("RETURN"))
    private void forwardExternalDirty(boolean playerChanged, CallbackInfo ci) {
        VanillaSectionInvalidation.INSTANCE.request(this);
    }
}
