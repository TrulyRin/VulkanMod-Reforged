package net.vulkanmod.mixin.compatibility.sable;

import net.vulkanmod.compat.sable.veil.VeilFirstPersonTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer",remap=false)
public abstract class VeilFirstPersonRendererMixin {
    @Redirect(method="bind(I)V",at=@At(value="INVOKE",
            target="Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo;getWidth()I",ordinal=0),require=1)
    private static int vulkanmod$mainWidth(@Coerce Object ignoredMainWrapper) {
        return VeilFirstPersonTarget.width();
    }

    @Redirect(method="bind(I)V",at=@At(value="INVOKE",
            target="Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo;getHeight()I",ordinal=0),require=1)
    private static int vulkanmod$mainHeight(@Coerce Object ignoredMainWrapper) {
        return VeilFirstPersonTarget.height();
    }

    @Inject(method="bind(I)V",at=@At(value="INVOKE",
            target="Lfoundry/veil/impl/client/render/dynamicbuffer/DynamicBufferManager;getDynamicFbo(Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo;)Lfoundry/veil/api/client/render/framebuffer/AdvancedFbo;",
            shift=At.Shift.BEFORE),require=1)
    private static void vulkanmod$refreshBorrowedColor(int mask,CallbackInfo ci) {
        VeilFirstPersonTarget.refresh();
    }
}
