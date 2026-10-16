package net.vulkanmod.mixin.compatibility;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.vulkanmod.compat.render.RenderStateSnapshot;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;

@Mixin(value = PostPass.class, priority = 900)
public class PostPassM {

    @Shadow(remap = false) @Final public RenderTarget f_110052_;

    @Shadow(remap = false) @Final public RenderTarget f_110053_;

    @Shadow(remap = false) @Final private EffectInstance f_110054_;

    @Shadow(remap = false) @Final private List<IntSupplier> f_110055_;

    @Shadow(remap = false) @Final private List<String> f_110056_;

    @Shadow(remap = false) @Final private List<Integer> f_110057_;

    @Shadow(remap = false) @Final private List<Integer> f_110058_;

    @Shadow(remap = false) private Matrix4f f_110059_;

                                                                                         
                                                                                             
                                                                                         
                                     
    @Inject(method = "m_110065_", at = @At("HEAD"), cancellable = true, remap = false)
    private void process(float f, CallbackInfo ci) {
        ci.cancel();

        RenderStateSnapshot renderStateSnapshot = new RenderStateSnapshot();

        try {
            this.f_110052_.unbindWrite();
            float g = (float)this.f_110053_.width;
            float h = (float)this.f_110053_.height;
            RenderSystem.viewport(0, 0, (int)g, (int)h);

            Objects.requireNonNull(this.f_110052_);
            this.f_110054_.setSampler("DiffuseSampler", this.f_110052_::getColorTextureId);

            if(this.f_110052_ instanceof MainTarget)
                this.f_110052_.bindRead();

            for(int i = 0; i < this.f_110055_.size(); ++i) {
                this.f_110054_.setSampler(this.f_110056_.get(i), this.f_110055_.get(i));
                this.f_110054_.safeGetUniform("AuxSize" + i).set((float) this.f_110057_.get(i), (float) this.f_110058_.get(i));
            }

            this.f_110054_.safeGetUniform("ProjMat").set(this.f_110059_);
            this.f_110054_.safeGetUniform("InSize").set((float)this.f_110052_.width, (float)this.f_110052_.height);
            this.f_110054_.safeGetUniform("OutSize").set(g, h);
            this.f_110054_.safeGetUniform("Time").set(f);
            Minecraft minecraft = Minecraft.getInstance();
            this.f_110054_.safeGetUniform("ScreenSize").set((float)minecraft.getWindow().getWidth(), (float)minecraft.getWindow().getHeight());

            this.f_110053_.clear(Minecraft.ON_OSX);
            this.f_110053_.bindWrite(false);

            VRenderSystem.disableCull();
            RenderSystem.depthFunc(519);

            Renderer.setViewport(0, this.f_110053_.height, this.f_110053_.width, -this.f_110053_.height);
            Renderer.resetScissor();

            this.f_110054_.apply();

            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bufferBuilder.vertex(0.0, 0.0, 500.0).endVertex();
            bufferBuilder.vertex(g, 0.0, 500.0).endVertex();
            bufferBuilder.vertex(g, h, 500.0).endVertex();
            bufferBuilder.vertex(0.0, h, 500.0).endVertex();
            BufferUploader.draw(bufferBuilder.end());
            RenderSystem.depthFunc(515);

            this.f_110054_.clear();
            this.f_110053_.unbindWrite();
            this.f_110052_.unbindRead();

            for (Object object : this.f_110055_) {
                if (object instanceof RenderTarget) {
                    ((RenderTarget) object).unbindRead();
                }
            }

            VRenderSystem.enableCull();
        } finally {
            renderStateSnapshot.restore();
            Renderer.resetScissor();
        }
    }
}
