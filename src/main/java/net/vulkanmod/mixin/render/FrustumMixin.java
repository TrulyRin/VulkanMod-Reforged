package net.vulkanmod.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.vulkanmod.interfaces.FrustumMixed;
import net.vulkanmod.render.chunk.frustum.VFrustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class FrustumMixin implements FrustumMixed {

    @Shadow private double camX;
    @Shadow private double camY;
    @Shadow private double camZ;
    @Shadow @Final private Matrix4f matrix;
    @Unique private final VFrustum vFrustum = new VFrustum();

    @Inject(method = "calculateFrustum(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", at = @At("HEAD"))
    private void calculateFrustum(Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {

        this.vFrustum.calculateFrustum(modelView, projection);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/culling/Frustum;)V", at = @At("RETURN"))
    private void copyCustomFrustum(Frustum original, CallbackInfo ci) {
        this.vFrustum.calculateFrustum(new Matrix4f(), this.matrix);
        this.vFrustum.setCamOffset(this.camX, this.camY, this.camZ);
    }

    @Inject(method = "prepare(DDD)V", at = @At("RETURN"))
    public void prepare(double d, double e, double f, CallbackInfo ci) {
        this.vFrustum.setCamOffset(this.camX, this.camY, this.camZ);
    }

    @Inject(method = "offset(F)Lnet/minecraft/client/renderer/culling/Frustum;", at = @At("RETURN"))
    private void updateOffset(float offset, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Frustum> cir) {
        this.vFrustum.setCamOffset(this.camX, this.camY, this.camZ);
    }

    @Override
    public VFrustum customFrustum() {
        return vFrustum;
    }
}
