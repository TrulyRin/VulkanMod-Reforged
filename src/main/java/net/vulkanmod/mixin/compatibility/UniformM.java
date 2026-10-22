package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Uniform.class, priority = 900)
public class UniformM {

    @Inject(method = "glGetUniformLocation", at = @At("HEAD"), cancellable = true)
    private static void glGetUniformLocation(int i, CharSequence charSequence, CallbackInfoReturnable<Integer> cir) {

        cir.setReturnValue(1);
    }

    @Inject(method = "glGetAttribLocation", at = @At("HEAD"), cancellable = true)
    private static void glGetAttribLocation(int i, CharSequence charSequence, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(0);
    }

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    public void cancelUpload(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "uploadInteger", at = @At("HEAD"), cancellable = true)
    private static void cancelUploadInteger(int i, int j, CallbackInfo ci) {
        ci.cancel();
    }
}
