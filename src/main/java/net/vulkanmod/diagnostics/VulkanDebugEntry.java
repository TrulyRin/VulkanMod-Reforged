package net.vulkanmod.diagnostics;

import net.minecraft.client.gui.components.debug.DebugEntrySystemSpecs;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.gui.components.debug.DebugScreenProfile;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.client.event.RegisterDebugEntriesEvent;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

                                                                                   
public final class VulkanDebugEntry implements DebugScreenEntry {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("vulkanmod", "renderer");
    private final Supplier<List<String>> rendererLines;

    public VulkanDebugEntry(Supplier<List<String>> rendererLines) {
        this.rendererLines = Objects.requireNonNull(rendererLines);
    }

    public static void register(RegisterDebugEntriesEvent event) {
        event.register(ID, new VulkanDebugEntry(VulkanDebugEntry::collectRendererLines));
        event.includeInProfile(ID, DebugScreenProfile.DEFAULT, DebugScreenEntryStatus.IN_OVERLAY);
        event.includeInProfile(ID, DebugScreenProfile.PERFORMANCE, DebugScreenEntryStatus.IN_OVERLAY);
    }

    @Override
    public void display(DebugScreenDisplayer display, @Nullable Level level,
                        @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
        display.addToGroup(DebugEntrySystemSpecs.GROUP, rendererLines.get());
    }

    private static List<String> collectRendererLines() {
        var lines = new ArrayList<String>();
        lines.add("VulkanMod Reforged " + Initializer.getVersion());
        Device device = Vulkan.getDevice();
        if (device != null) {
            lines.add("GPU: " + device.deviceName);
            lines.add("Driver: " + device.driverVersion);
            lines.add("Vulkan: " + device.vkVersion);
            if (!net.vulkanmod.render.engine.VkGpuTimestampQuery.isSupported()) {
                lines.add("GPU timing: unavailable on this graphics queue");
            }
        }
        lines.add("CPU: %s, %d logical processors".formatted(
                System.getProperty("os.arch", "unknown"), Runtime.getRuntime().availableProcessors()));
        long nonHeapMiB = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
        lines.add("Off-heap: " + nonHeapMiB + "MB");
        MemoryManager memory = MemoryManager.getInstance();
        if (memory != null) {
            lines.add("NativeMemory: " + memory.getNativeMemoryMB() + "MB");
            lines.add("DeviceMemory: " + memory.getAllocatedDeviceMemoryMB() + "MB");
        }
        WorldRenderer renderer = WorldRenderer.getInstance();
        if (renderer != null && renderer.getSectionGrid() != null) {
            lines.add(renderer.getChunkStatistics());
            Collections.addAll(lines, renderer.getChunkAreaManager().getStats());
        }
        return lines;
    }
}
