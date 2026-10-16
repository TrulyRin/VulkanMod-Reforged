package net.vulkanmod.mixin.compatibility;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = PostChain.class, priority = 900)
public abstract class PostChainM {

    @Shadow(remap = false) @Final private List<PostPass> f_110009_;

    @Shadow(remap = false) private float f_110016_;
    @Shadow(remap = false) private float f_110015_;

                                                                                         
                                                                                        
    @Inject(method = "m_110023_", at = @At("HEAD"), cancellable = true, remap = false)
    private void process(float f, CallbackInfo ci) {
        ci.cancel();

        if (f < this.f_110016_) {
            this.f_110015_ += 1.0F - this.f_110016_;
            this.f_110015_ += f;
        } else {
            this.f_110015_ += f - this.f_110016_;
        }

        this.f_110016_ = f;

        while (this.f_110015_ > 20.0F) {
            this.f_110015_ -= 20.0F;
        }

        for (PostPass postPass : this.f_110009_) {
            postPass.process(this.f_110015_ / 20.0F);
        }

        Renderer.resetViewport();
    }

}
