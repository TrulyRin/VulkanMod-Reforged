package net.vulkanmod.mixin.chunk;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.vulkanmod.render.chunk.VanillaSectionInvalidation;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ViewArea.class)
public abstract class ViewAreaM {

	@Shadow public SectionRenderDispatcher.RenderSection[] sections;

	@Shadow protected abstract void setViewDistance(int i);

	@Inject(method = "createSections", at = @At("HEAD"))
	private void skipAllocation(SectionRenderDispatcher sectionRenderDispatcher, CallbackInfo ci) {

		this.setViewDistance(0);
	}

	@WrapOperation(method = "setDirty(IIIZ)V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection;setDirty(Z)V"), require = 1)
	private void forwardSectionDirty(SectionRenderDispatcher.RenderSection section, boolean playerChanged,
			Operation<Void> original, int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread) {
		VanillaSectionInvalidation bridge = VanillaSectionInvalidation.INSTANCE;
		bridge.beginInternal();
		try {
			if (bridge.isActiveOwner(this)) {
				WorldRenderer renderer = WorldRenderer.getInstance();
				if (renderer != null) renderer.setSectionDirty(sectionX, sectionY, sectionZ, reRenderOnMainThread);
			}
			original.call(section, playerChanged);
		} finally {
			bridge.endInternal();
		}
	}
}
