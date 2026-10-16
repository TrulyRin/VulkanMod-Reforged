package net.vulkanmod.render.engine;

import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.Queue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

                                                                                      
public final class VkGpuTimestampQuery implements GpuQuery {
                                                                                       
    public interface Backend {
        int validBits();
        double timestampPeriodNanos();
        long createPool();
        void begin(long pool);
        void end(long pool);
                                                                                            
        long[] poll(long pool);
                                                                                 
        void retire(long pool);
    }

    private record Hardware(VkDevice device, int validBits, double period) {
        boolean supported() { return validBits > 0 && validBits <= 64 && period > 0 && Double.isFinite(period); }
    }

    private static final Set<VkGpuTimestampQuery> LIVE = ConcurrentHashMap.newKeySet();
    private static volatile Hardware hardware;
    private final Backend backend;
    private final long pool;
    private final int validBits;
    private final double period;
    private boolean ended;
    private boolean closed;
    private OptionalLong result = OptionalLong.empty();

    public VkGpuTimestampQuery(Backend backend) {
        this.backend = backend;
        this.validBits = backend.validBits();
        this.period = backend.timestampPeriodNanos();
        if (validBits <= 0 || validBits > 64 || period <= 0 || !Double.isFinite(period)) {
            throw new UnsupportedOperationException("The graphics queue does not support valid GPU timestamps");
        }
        this.pool = backend.createPool();
        try {
            backend.begin(pool);
            LIVE.add(this);
        } catch (RuntimeException | Error failure) {
            backend.retire(pool);
            throw failure;
        }
    }

    public static boolean isSupported() {
        return DeviceManager.vkDevice != null && currentHardware().supported();
    }

    public static VkGpuTimestampQuery begin() {
        RenderSystem.assertOnRenderThread();
        Hardware capabilities = currentHardware();
        return new VkGpuTimestampQuery(new NativeBackend(capabilities));
    }

    public void end() {
        requireOpen();
        if (ended) throw new IllegalStateException("GPU timestamp query has already ended");
        backend.end(pool);
        ended = true;
    }

    @Override
    public OptionalLong getValue() {
        requireOpen();
        if (!ended || result.isPresent()) return result;
        long[] counters = backend.poll(pool);
        if (counters == null) return OptionalLong.empty();
        if (counters.length != 2) throw new IllegalStateException("GPU timestamp backend returned an invalid counter pair");
        long ticks = counters[1] - counters[0];
        if (validBits < 64) ticks &= (1L << validBits) - 1;
        double unsignedTicks = ticks >= 0 ? ticks : (double) (ticks & Long.MAX_VALUE) + 0x1.0p63;
                                                                           
        result = OptionalLong.of(Math.max(1L, Math.round(unsignedTicks * period)));
        return result;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            LIVE.remove(this);
            backend.retire(pool);
        }
    }

                                                                                            
    public static void closeAll() {
        for (VkGpuTimestampQuery query : Set.copyOf(LIVE)) query.close();
        hardware = null;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("GPU timestamp query is closed");
    }

    private static Hardware currentHardware() {
        VkDevice device = DeviceManager.vkDevice;
        Hardware cached = hardware;
        if (cached != null && cached.device() == device) return cached;
        if (device == null || DeviceManager.physicalDevice == null || DeviceManager.deviceProperties == null) {
            return new Hardware(device, 0, 0);
        }
        try (MemoryStack stack = stackPush()) {
            IntBuffer count = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(DeviceManager.physicalDevice, count, null);
            VkQueueFamilyProperties.Buffer families = VkQueueFamilyProperties.calloc(count.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(DeviceManager.physicalDevice, count, families);
            int graphicsFamily = Queue.getQueueFamilies().graphicsFamily;
            int validBits = graphicsFamily >= 0 && graphicsFamily < families.capacity()
                    ? families.get(graphicsFamily).timestampValidBits() : 0;
            cached = new Hardware(device, validBits, DeviceManager.deviceProperties.limits().timestampPeriod());
            hardware = cached;
            return cached;
        }
    }

    private static final class NativeBackend implements Backend {
        private final Hardware capabilities;

        private NativeBackend(Hardware capabilities) { this.capabilities = capabilities; }
        @Override public int validBits() { return capabilities.validBits(); }
        @Override public double timestampPeriodNanos() { return capabilities.period(); }

        @Override
        public long createPool() {
            requireCurrentDevice();
            try (MemoryStack stack = stackPush()) {
                VkQueryPoolCreateInfo info = VkQueryPoolCreateInfo.calloc(stack).sType$Default()
                        .queryType(VK_QUERY_TYPE_TIMESTAMP).queryCount(2);
                LongBuffer handle = stack.longs(VK_NULL_HANDLE);
                int status = vkCreateQueryPool(capabilities.device(), info, null, handle);
                if (status != VK_SUCCESS) throw new IllegalStateException("Could not create GPU timestamp pool: " + status);
                return handle.get(0);
            }
        }

        @Override
        public void begin(long pool) {
            requireCurrentDevice();
            Renderer renderer = Renderer.getInstance();
            if (!Renderer.isRecording()) renderer.beginFrame();
            Framebuffer previousFramebuffer = renderer.getBoundFramebuffer();
            RenderPass previousPass = renderer.getBoundRenderPass();
                                                                                
                                                                              
            renderer.endRenderPass();
            VkCommandBuffer commands = Renderer.getCommandBuffer();
            vkCmdResetQueryPool(commands, pool, 0, 2);
            vkCmdWriteTimestamp(commands, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, pool, 0);
            if (previousPass != null && previousFramebuffer != null) renderer.beginRenderPass(previousPass, previousFramebuffer);
        }

        @Override
        public void end(long pool) {
            requireCurrentDevice();
            if (!Renderer.isRecording()) throw new IllegalStateException("GPU timestamp query ended outside command recording");
            vkCmdWriteTimestamp(Renderer.getCommandBuffer(), VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, pool, 1);
        }

        @Override
        public long[] poll(long pool) {
            requireCurrentDevice();
            try (MemoryStack stack = stackPush()) {
                LongBuffer values = stack.callocLong(4);
                int status = vkGetQueryPoolResults(capabilities.device(), pool, 0, 2, values, 2L * Long.BYTES,
                        VK_QUERY_RESULT_64_BIT | VK_QUERY_RESULT_WITH_AVAILABILITY_BIT);
                if (status == VK_NOT_READY) return null;
                if (status != VK_SUCCESS) throw new IllegalStateException("Could not read GPU timestamp query: " + status);
                return values.get(1) != 0L && values.get(3) != 0L ? new long[]{values.get(0), values.get(2)} : null;
            }
        }

        @Override
        public void retire(long pool) {
            RenderSystem.assertOnRenderThread();
            MemoryManager manager = MemoryManager.getInstance();
            if (manager == null) throw new IllegalStateException("GPU query retired without the frame memory manager");
                                                                              
                                                                                  
            manager.addFrameOp(() -> vkDestroyQueryPool(capabilities.device(), pool, null));
        }

        private void requireCurrentDevice() {
            RenderSystem.assertOnRenderThread();
            if (capabilities.device() != DeviceManager.vkDevice) throw new IllegalStateException("GPU timestamp query belongs to a retired device");
        }
    }
}
