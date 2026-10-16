package net.vulkanmod.mixin.compatibility.tdp;

import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.compat.tdp.TdpFastDrawBridge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="org.mesdag.thr_dim_particle.client.TDPRenderType", remap=false)
public abstract class TdpFastDrawMixin {
    @Shadow @Final private RenderType.CompositeState state;

    @Inject(method="draw(JIILjava/lang/Runnable;)V", at=@At("HEAD"), cancellable=true)
    private void vulkanmod$drawBorrowedMesh(long address, int capacity, int vertices, Runnable close, CallbackInfo ci) {
        if (!TdpFastDrawBridge.enabled()) return;
        TdpFastDrawBridge.draw((RenderType) (Object) this, state, address, capacity, vertices, close);
        ci.cancel();
    }
}
