package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

                                                                                        
@Mixin(TextureAtlas.class)
public abstract class TextureAtlasAnimationMixin {
    @Inject(method = "cycleAnimationFrames", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$applyAnimationSetting(CallbackInfo ci) {
        if (Initializer.CONFIG != null && !Initializer.CONFIG.textureAnimations) {
            ci.cancel();
        }
    }
}
