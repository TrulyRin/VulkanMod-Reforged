package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.texture.SpriteUtil;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(TextureManager.class)
public abstract class MTextureManager {

    @Shadow(remap = false) @Final private Set<Tickable> f_118469_;

    @Overwrite(remap = false)
    public void m_7673_() {
        if (Renderer.skipRendering || !Initializer.CONFIG.textureAnimations)
            return;

        if (SpriteUtil.shouldUpload())
            DeviceManager.getGraphicsQueue().startRecording();
        for (Tickable tickable : this.f_118469_) {
            tickable.tick();
        }
        if (SpriteUtil.shouldUpload()) {
            SpriteUtil.transitionLayouts(DeviceManager.getGraphicsQueue().getCommandBuffer().getHandle());
            DeviceManager.getGraphicsQueue().endRecordingAndSubmit();
        }
    }
}

