package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.Ticker.class)
public interface SpriteTickerAccessor {
    @Accessor("animationInfo")
    SpriteContents.AnimatedTexture vulkanmod$getAnimationInfo();
    @Accessor("frame")
    int vulkanmod$getFrame();
    @Accessor("subFrame")
    int vulkanmod$getSubFrame();
}
