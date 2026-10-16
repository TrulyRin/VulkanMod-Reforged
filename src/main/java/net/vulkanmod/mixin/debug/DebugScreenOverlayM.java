package net.vulkanmod.mixin.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayM {

    @Shadow(remap = false)
    @Final
    private Minecraft f_94030_;

    @Shadow(remap = false)
    private static long m_94050_(long bytes) {
        return 0;
    }

    @Shadow(remap = false)
    @Final
    private Font f_94031_;

    @Shadow(remap = false)
    protected abstract List<String> m_94075_();

    @Shadow(remap = false)
    protected abstract List<String> m_94078_();

    @Redirect(method = "m_94078_", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false), remap = false)
    private ArrayList<String> redirectList(Object[] elements) {
        ArrayList<String> strings = new ArrayList<>();

        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        Device device = Vulkan.getDevice();

        strings.add(String.format("Java: %s", System.getProperty("java.version")));
        strings.add(String.format("Mem: % 2d%% %03d/%03dMB", usedMemory * 100L / maxMemory, m_94050_(usedMemory), m_94050_(maxMemory)));
        strings.add(String.format("Allocated: % 2d%% %03dMB", totalMemory * 100L / maxMemory, m_94050_(totalMemory)));
        strings.add(String.format("Off-heap: " + getOffHeapMemory() + "MB"));
        strings.add("NativeMemory: %dMB".formatted(MemoryManager.getInstance().getNativeMemoryMB()));
        strings.add("DeviceMemory: %dMB".formatted(MemoryManager.getInstance().getAllocatedDeviceMemoryMB()));
        strings.add("");
        strings.add("VulkanMod " + getVersion());
        strings.add("CPU: " + vulkanMod$getCpuInfo());
        strings.add("GPU: " + device.deviceName);
        strings.add("Driver: " + device.driverVersion);
        strings.add("Vulkan: " + device.vkVersion);
        strings.add("");
        strings.add("");

        Collections.addAll(strings, WorldRenderer.getInstance().getChunkAreaManager().getStats());

        return strings;
    }

    private long getOffHeapMemory() {
        return m_94050_(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }

    @Unique
    private static String vulkanMod$getCpuInfo() {
        return "%s, %d logical processors".formatted(
                System.getProperty("os.arch", "unknown"),
                Runtime.getRuntime().availableProcessors());
    }
}

