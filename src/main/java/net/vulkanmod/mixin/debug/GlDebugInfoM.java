package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.platform.GlUtil;
import net.vulkanmod.vulkan.Vulkan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GlUtil.class)
public class GlDebugInfoM {

    @Overwrite
    public static String getVendor() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().vendorIdString : "n/a";
    }

    @Overwrite
    public static String getRenderer() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().deviceName : "n/a";
    }

    @Overwrite
    public static String getOpenGLVersion() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().driverVersion : "n/a";
    }

    @Overwrite
    public static String getCpuInfo() {
        return vulkanMod$getCpuInfo();
    }

    @Unique
    private static String vulkanMod$getCpuInfo() {
        return "%s, %d logical processors".formatted(
                System.getProperty("os.arch", "unknown"),
                Runtime.getRuntime().availableProcessors());
    }
}
