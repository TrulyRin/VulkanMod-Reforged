package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.Platform;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.lang.reflect.Field;

public final class VkInstanceFactory {

    static final int INIT_THREAD_STACK_BYTES = 4 * 1024 * 1024;

    private static final String THREAD_NAME = "VulkanMod-InstanceInit";
    private static final String TLS_FIELD = "TLS";

    private VkInstanceFactory() {
    }

    public static VkInstance create(long handle, VkInstanceCreateInfo createInfo) {
        final VkInstance[] result = new VkInstance[1];
        final Throwable[] error = new Throwable[1];

        Thread initThread = new Thread(() -> {
            installLargeMemoryStack();
            try {
                result[0] = new VkInstance(handle, createInfo);
            } catch (Throwable t) {
                error[0] = t;
            }
        }, THREAD_NAME);

        try {
            initThread.start();
            initThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while creating Vulkan instance", e);
        }

        if (error[0] != null) {
            throw new RuntimeException("Failed to initialize Vulkan instance.\n"
                    + Platform.vulkanDriverHint(), error[0]);
        }
        return result[0];
    }

    private static void installLargeMemoryStack() {
        try {
            memoryStackTls().set(MemoryStack.create(INIT_THREAD_STACK_BYTES));
            Initializer.LOGGER.info("VulkanMod: instance init stack = {} KB.",
                    MemoryStack.stackGet().getSize() / 1024);
        } catch (Throwable t) {
            Initializer.LOGGER.warn("VulkanMod: could not resize the instance-init MemoryStack ({}); "
                    + "falling back to LWJGL's default size.", t.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<MemoryStack> memoryStackTls() throws ReflectiveOperationException {
        Field tls = MemoryStack.class.getDeclaredField(TLS_FIELD);
        tls.setAccessible(true);
        return (ThreadLocal<MemoryStack>) tls.get(null);
    }
}
