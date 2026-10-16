package net.vulkanmod.mixin.profiling;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.vulkanmod.compat.observer.CompatBenchmarkShortcut;
import net.vulkanmod.compat.observer.CompatProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(KeyboardHandler.class)
public abstract class CompatBenchmarkKeyboardMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V", at = @At("TAIL"))
    private void vulkanmod$startCompatibilityBenchmark(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (event.key() != GLFW_KEY_F10 || action != GLFW_PRESS
                || window != minecraft.getWindow().handle() || minecraft.level == null
                || minecraft.screen != null || CompatProfiler.isBenchmarking()) return;
        CompatBenchmarkShortcut.Phase phase = CompatBenchmarkShortcut.selectPhase(event.key(), action,
                InputConstants.isKeyDown(minecraft.getWindow(), GLFW_KEY_LEFT_ALT),
                true, true, false, false, CompatProfiler.hasBaseline());
        if (phase != CompatBenchmarkShortcut.Phase.NONE) {
            CompatProfiler.startBenchmark(phase == CompatBenchmarkShortcut.Phase.OBSERVE);
        }
    }
}
