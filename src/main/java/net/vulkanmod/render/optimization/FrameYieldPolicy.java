package net.vulkanmod.render.optimization;

import com.mojang.blaze3d.systems.GpuDevice;
import net.vulkanmod.render.engine.VkGpuDevice;

                                                                                   
public final class FrameYieldPolicy {
    private FrameYieldPolicy() {}

    public static void atFrameEnd(GpuDevice device, Runnable yieldAction) {
                                                                               
                                                                                
        if (!(device instanceof VkGpuDevice)) {
            yieldAction.run();
        }
    }
}
