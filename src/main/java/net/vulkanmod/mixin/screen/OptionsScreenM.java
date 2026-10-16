package net.vulkanmod.mixin.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.vulkanmod.config.gui.VOptionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenM extends Screen {
    protected OptionsScreenM(Component title) {
        super(title);
    }

    @Shadow
    private Button openScreenButton(Component caption, Supplier<Screen> screenSupplier) {
        throw new AssertionError();
    }

    @Redirect(method = "init", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/screens/options/OptionsScreen;openScreenButton(Lnet/minecraft/network/chat/Component;Ljava/util/function/Supplier;)Lnet/minecraft/client/gui/components/Button;"))
    private Button vulkanmod$openScreenButton(OptionsScreen screen, Component caption,
                                              Supplier<Screen> screenSupplier) {
        return this.openScreenButton(caption, vulkanmod$videoScreenSupplier(caption,
                screenSupplier, () -> new VOptionScreen(caption, this)));
    }

    @Unique
    private static Supplier<Screen> vulkanmod$videoScreenSupplier(Component caption,
                                                                 Supplier<Screen> original,
                                                                 Supplier<Screen> vulkan) {
        return caption.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("options.video") ? vulkan : original;
    }
}
