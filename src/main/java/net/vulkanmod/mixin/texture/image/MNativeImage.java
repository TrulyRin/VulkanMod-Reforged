package net.vulkanmod.mixin.texture.image;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.texture.ImageUtil;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.VUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.util.Locale;

@Mixin(NativeImage.class)
public abstract class MNativeImage {

    @Shadow(remap = false) private long f_84964_;
    @Shadow(remap = false) private long f_84965_;

    @Shadow public abstract void close();

    @Shadow(remap = false) @Final private NativeImage.Format f_84960_;

    @Shadow(remap = false) public abstract int m_84982_();

    @Shadow(remap = false) @Final private int f_84961_;
    @Shadow(remap = false) @Final private int f_84962_;

    @Shadow(remap = false) public abstract int m_85084_();

    @Shadow(remap = false) public abstract void m_84988_(int i, int j, int k);

    @Shadow(remap = false) public abstract int m_84985_(int i, int j);

    @Shadow(remap = false) protected abstract void m_85124_();

    private ByteBuffer buffer;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage$Format;IIZ)V", at = @At("RETURN"))
    private void constr(NativeImage.Format format, int width, int height, boolean useStb, CallbackInfo ci) {
        if(this.f_84964_ != 0) {
            buffer = VUtil.getByteBuffer(this.f_84964_, (int)this.f_84965_);
        }
    }

    @Inject(method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage$Format;IIZJ)V", at = @At("RETURN"))
    private void constr(NativeImage.Format format, int width, int height, boolean useStb, long pixels, CallbackInfo ci) {
        if(this.f_84964_ != 0) {
            buffer = VUtil.getByteBuffer(this.f_84964_, (int)this.f_84965_);
        }
    }

    @Overwrite(remap = false)
    private void m_85090_(int level, int xOffset, int yOffset, int unpackSkipPixels, int unpackSkipRows, int widthIn, int heightIn, boolean blur, boolean clamp, boolean mipmap, boolean autoClose) {
        RenderSystem.assertOnRenderThreadOrInit();

        VTextureSelector.uploadSubTexture(level, widthIn, heightIn, xOffset, yOffset, unpackSkipRows, unpackSkipPixels, this.m_84982_(), this.buffer);

        if (autoClose) {
            this.close();
        }
    }

    @Overwrite(remap = false)
    public void m_85045_(int level, boolean removeAlpha) {
        RenderSystem.assertOnRenderThread();

        ImageUtil.downloadTexture(VTextureSelector.getBoundTexture(0), this.f_84964_);

        if (removeAlpha && this.f_84960_.hasAlpha()) {
            if (this.f_84960_ != NativeImage.Format.RGBA) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.f_84960_));
            }

            for (long l = 0; l < this.f_84961_ * this.f_84962_ * 4L; l+=4) {
                int v =  VUtil.getInt(this.f_84964_ + l);

                if(Vulkan.getSwapChain().isBGRAformat)
                    v = ColorUtil.BGRAtoRGBA(v);

                v = v | 255 << this.f_84960_.alphaOffset();
                VUtil.putInt(this.f_84964_ + l, v);
            }
        }

    }

}

