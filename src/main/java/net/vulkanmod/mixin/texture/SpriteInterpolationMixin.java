package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.vulkanmod.render.texture.SpriteInterpolation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$InterpolationData")
public abstract class SpriteInterpolationMixin {
    @Shadow @Final private NativeImage[] activeFrame;
    @Shadow @Final private SpriteContents this$0;

    @Inject(method = "uploadInterpolatedFrame(IILnet/minecraft/client/renderer/texture/SpriteContents$Ticker;)V",
            at = @At("HEAD"), cancellable = true)
    private void vulkanmod$interpolate(int x, int y, SpriteContents.Ticker ticker, CallbackInfo callback) {
        var state = (SpriteTickerAccessor) ticker;
        SpriteContents.AnimatedTexture animation = state.vulkanmod$getAnimationInfo();
        var frames = animation.frames;
        int frame = state.vulkanmod$getFrame();
        SpriteContents.FrameInfo current = frames.get(frame);
        double delta = 1.0 - (double) state.vulkanmod$getSubFrame() / (double) current.time;
        int first = ((SpriteFrameInfoAccessor) current).vulkanmod$getIndex();
        int next = ((SpriteFrameInfoAccessor) frames.get((frame + 1) % frames.size())).vulkanmod$getIndex();
        if (first == next) {
            callback.cancel();
            return;
        }
        var layout = (SpriteAnimatedTextureAccessor) animation;
        if (SpriteInterpolation.interpolate(this.this$0.byMipLevel, this.activeFrame, this.this$0.width(), this.this$0.height(),
                layout.vulkanmod$getFrameX(first), layout.vulkanmod$getFrameY(first),
                layout.vulkanmod$getFrameX(next), layout.vulkanmod$getFrameY(next), delta)) {

            ((SpriteContentsAccessor) this.this$0).vulkanmod$upload(x, y, 0, 0, this.activeFrame);
            callback.cancel();
        }
    }
}
