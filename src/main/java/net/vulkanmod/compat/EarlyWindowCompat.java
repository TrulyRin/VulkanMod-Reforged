package net.vulkanmod.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EarlyWindowCompat {
    private static boolean handoffComplete = false;

    public static boolean isHandoffComplete() {
        return handoffComplete;
    }

    public static void setHandoffComplete(boolean value) {
        handoffComplete = value;
    }

    public static void disableFmlEarlyWindowProvider() {
        try {
            Class<?> clazz = Class.forName("net.neoforged.fml.loading.ImmediateWindowHandler");
            Field providerField = clazz.getDeclaredField("provider");
            providerField.setAccessible(true);
            providerField.set(null, createNoOpProvider(providerField.getType()));
            disableProgressWindowTick();
        } catch (Throwable t) {
            System.err.println("[VulkanMod] Failed to replace FML provider field: " + t.getMessage());
        }
    }

    public static void clearFmlProvider() {
        disableFmlEarlyWindowProvider();
    }

    private static Object createNoOpProvider(Class<?> providerType) {
        return Proxy.newProxyInstance(providerType.getClassLoader(), new Class<?>[] { providerType }, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "name" -> "vulkanmod_noop_earlywindow";
                case "initialize" -> (Runnable) () -> { };
                case "setupMinecraftWindow" -> 0L;
                case "positionWindow" -> false;
                case "loadingOverlay" -> createLoadingOverlaySupplier(args);
                case "getGLVersion" -> "3.2";
                case "toString" -> "VulkanModNoOpEarlyWindowProvider";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            };
        });
    }

    @SuppressWarnings("unchecked")
    private static Supplier<LoadingOverlay> createLoadingOverlaySupplier(Object[] args) {
        Supplier<Minecraft> minecraft = (Supplier<Minecraft>) args[0];
        Supplier<ReloadInstance> reload = (Supplier<ReloadInstance>) args[1];
        Consumer<Optional<Throwable>> listener = (Consumer<Optional<Throwable>>) args[2];
        boolean fade = (Boolean) args[3];
        return () -> new LoadingOverlay(minecraft.get(), reload.get(), listener, fade);
    }

    private static void disableProgressWindowTick() {
        try {
            Class<?> loaderClass = Class.forName("net.neoforged.fml.loading.FMLLoader");
            Field tickField = loaderClass.getDeclaredField("progressWindowTick");
            tickField.setAccessible(true);
            tickField.set(null, (Runnable) () -> { });
        } catch (Throwable ignored) {
        }
    }
}
