package net.vulkanmod.mixin.chunk;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.vulkanmod.render.chunk.VanillaSectionInvalidation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask")
public abstract class VanillaRebuildTaskMixin {
    @WrapOperation(method = "cancel()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setDirty(Z)V"), require = 1)
    private void suppressInternalDirty(SectionRenderDispatcher.RenderSection section, boolean playerChanged,
                                       Operation<Void> original) {
        VanillaSectionInvalidation bridge = VanillaSectionInvalidation.INSTANCE;
        bridge.beginInternal();
        try {
            original.call(section, playerChanged);
        } finally {
            bridge.endInternal();
        }
    }
}
