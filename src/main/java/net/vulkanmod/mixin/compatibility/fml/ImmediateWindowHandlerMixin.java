package net.vulkanmod.mixin.compatibility.fml;

import net.neoforged.fml.loading.ImmediateWindowHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ImmediateWindowHandler.class, remap = false)
public class ImmediateWindowHandlerMixin {

    @Overwrite
    public static void renderTick() {
        if (net.vulkanmod.compat.EarlyWindowCompat.isHandoffComplete()) {
            return;
        }

        try {
            java.lang.reflect.Field providerField = ImmediateWindowHandler.class.getDeclaredField("provider");
            providerField.setAccessible(true);
            Object provider = providerField.get(null);
            if (provider != null) {
                provider.getClass().getMethod("renderTick").invoke(provider);
            }
        } catch (Throwable t) {

        }
    }
}
