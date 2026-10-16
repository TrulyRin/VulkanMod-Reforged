package net.vulkanmod.vulkan;

import org.lwjgl.system.Configuration;

   
                                                                             
                                                                           
                                                                          
                                                             
  
                                                                                          
                                                                                               
                                                                                                
              
   
public final class LwjglStackSizeConfigurator {
    private static final String STACK_SIZE_PROPERTY = "org.lwjgl.system.stackSize";
    private static final int VULKAN_MINIMUM_STACK_KB = 4096;

    private LwjglStackSizeConfigurator() {
    }

    public static void configureSystemPropertyForVulkan() {
        Integer configured = parseStackSize(System.getProperty(STACK_SIZE_PROPERTY));
        System.setProperty(STACK_SIZE_PROPERTY, Integer.toString(stackSizeAtLeastVulkanMinimum(configured)));
    }

    public static void configureForVulkan() {
        Object current = Configuration.STACK_SIZE.get();
        Integer configured = current instanceof Integer value ? value : null;
        Configuration.STACK_SIZE.set(stackSizeAtLeastVulkanMinimum(configured));
    }

    private static int stackSizeAtLeastVulkanMinimum(Integer configured) {
        return configured == null ? VULKAN_MINIMUM_STACK_KB : Math.max(configured, VULKAN_MINIMUM_STACK_KB);
    }

    private static Integer parseStackSize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
