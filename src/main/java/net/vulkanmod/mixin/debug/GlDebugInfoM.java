package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.platform.GlUtil;
import net.vulkanmod.vulkan.Vulkan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GlUtil.class)
public class GlDebugInfoM {

    @Overwrite(remap = false)
    public static String m_84818_() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().vendorIdString : "n/a";
    }

    @Overwrite(remap = false)
    public static String m_84820_() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().deviceName : "n/a";
    }

    @Overwrite(remap = false)
    public static String m_84821_() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().driverVersion : "n/a";
    }

    @Overwrite(remap = false)
    public static String m_84819_() {
        return vulkanMod$getCpuInfo();
    }

    @Unique
    private static String vulkanMod$getCpuInfo() {
        return "%s, %d logical processors".formatted(
                System.getProperty("os.arch", "unknown"),
                Runtime.getRuntime().availableProcessors());
    }
}

