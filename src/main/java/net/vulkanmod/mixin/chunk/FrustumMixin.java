package net.vulkanmod.mixin.chunk;

import net.minecraft.client.renderer.culling.Frustum;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.render.chunk.frustum.VFrustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class FrustumMixin implements FrustumMixed {

    @Shadow(remap = false) private double f_112996_;
    @Shadow(remap = false) private double f_112997_;
    @Shadow(remap = false) private double f_112998_;
    private final VFrustum vFrustum = new VFrustum();

    @Inject(method = "m_253155_", at = @At("HEAD"), remap = false)
    private void calculateFrustum(Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {

        this.vFrustum.calculateFrustum(modelView, projection);
    }

    @Inject(method = "m_113002_", at = @At("RETURN"), remap = false)
    public void prepare(double d, double e, double f, CallbackInfo ci) {
        this.vFrustum.setCamOffset(this.f_112996_, this.f_112997_, this.f_112998_);
    }

    @Override
    public VFrustum customFrustum() {
        return vFrustum;
    }
}

