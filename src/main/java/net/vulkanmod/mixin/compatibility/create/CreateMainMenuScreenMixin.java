package net.vulkanmod.mixin.compatibility.create;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.infrastructure.gui.CreateMainMenuScreen", remap = false)
public abstract class CreateMainMenuScreenMixin extends Screen {
    protected CreateMainMenuScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "lambda$addButtons$3(Lnet/minecraft/client/gui/components/Button;)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void vulkanmod$openNeoForgeConfigScreen(Button button, CallbackInfo ci) {
        ModList.get().getModContainerById("create").ifPresent(container -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new ConfigurationScreen(container, (Screen) (Object) this));
                ci.cancel();
            }
        });
    }
}
