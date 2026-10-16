package net.vulkanmod.mixin.compatibility.gl;

import net.vulkanmod.compat.opengl.EmulatedGlFunctionProvider;
import net.vulkanmod.compat.opengl.GlCapabilitiesFallback;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.FunctionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.IntFunction;

                                                                          
@Mixin(value = GL.class, remap = false)
public abstract class GLProviderMixin {
    @Inject(method = "getFunctionProvider", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$verifiedProvider(CallbackInfoReturnable<FunctionProvider> cir) {
        if (GlCapabilitiesFallback.shouldEmulate()) cir.setReturnValue(EmulatedGlFunctionProvider.INSTANCE);
    }

    @Inject(method = "createCapabilities(ZLjava/util/function/IntFunction;)Lorg/lwjgl/opengl/GLCapabilities;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$verifiedCapabilities(boolean forwardCompatible,
                                                       IntFunction<PointerBuffer> bufferFactory,
                                                       CallbackInfoReturnable<GLCapabilities> cir) {
        if (GlCapabilitiesFallback.shouldEmulate()) cir.setReturnValue(GlCapabilitiesFallback.forCurrentThread());
    }

    @Inject(method = {"getCapabilities", "getICD"}, at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$verifiedCurrentCapabilities(CallbackInfoReturnable<GLCapabilities> cir) {
        if (GlCapabilitiesFallback.shouldEmulate()) cir.setReturnValue(GlCapabilitiesFallback.forCurrentThread());
    }
}
