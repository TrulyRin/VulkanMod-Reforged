package net.vulkanmod.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteContents.AnimatedTexture.class)
public interface SpriteAnimatedTextureAccessor {
    @Invoker("getFrameX")
    int vulkanmod$getFrameX(int frame);
    @Invoker("getFrameY")
    int vulkanmod$getFrameY(int frame);
}
