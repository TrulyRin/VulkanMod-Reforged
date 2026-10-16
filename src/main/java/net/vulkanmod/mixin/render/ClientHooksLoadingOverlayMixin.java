package net.vulkanmod.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.neoforged.neoforge.client.ClientHooks;
import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.function.Consumer;

   
                                                                              
                                                                         
   
@Mixin(ClientHooks.class)
public abstract class ClientHooksLoadingOverlayMixin {

    @Inject(method = "createLoadingOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$createLoadingOverlay(Minecraft minecraft, ReloadInstance reloadInstance,
                                                       Consumer<Optional<Throwable>> onFinish, boolean fadeIn,
                                                       CallbackInfoReturnable<Overlay> cir) {
        Initializer.LOGGER.info("VulkanMod: using vanilla loading overlay");
        cir.setReturnValue(new LoadingOverlay(minecraft, reloadInstance, onFinish, fadeIn));
    }
}
