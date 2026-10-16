package net.vulkanmod.vulkan.device;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.GLFW_PLATFORM_WIN32;
import static org.lwjgl.glfw.GLFW.glfwGetPlatform;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkEnumerateInstanceVersion;
import static org.lwjgl.vulkan.VK11.vkGetPhysicalDeviceFeatures2;

public class Device {
    final VkPhysicalDevice physicalDevice;
    final VkPhysicalDeviceProperties properties;

    private final int vendorId;
    public final String vendorIdString;
    public final String deviceName;
    public final String driverVersion;
    public final String vkVersion;

    public final VkPhysicalDeviceFeatures2 availableFeatures;
    public final VkPhysicalDeviceVulkan11Features availableFeatures11;

    private boolean drawIndirectSupported;
    private final boolean portabilitySubset;

    public Device(VkPhysicalDevice device) {
        this.physicalDevice = device;

        properties = VkPhysicalDeviceProperties.malloc();
        vkGetPhysicalDeviceProperties(physicalDevice, properties);

        this.vendorId = properties.vendorID();
        this.vendorIdString = decodeVendor(properties.vendorID());
        this.deviceName = properties.deviceNameString();
        this.driverVersion = decodeDvrVersion(properties.driverVersion(), properties.vendorID());
        this.vkVersion = decDefVersion(getVkVer());

        this.availableFeatures = VkPhysicalDeviceFeatures2.calloc();
        this.availableFeatures.sType$Default();

        this.availableFeatures11 = VkPhysicalDeviceVulkan11Features.malloc();
        this.availableFeatures11.sType$Default();
        this.availableFeatures.pNext(this.availableFeatures11);

        vkGetPhysicalDeviceFeatures2(this.physicalDevice, this.availableFeatures);

        if (this.availableFeatures.features().multiDrawIndirect() && this.availableFeatures11.shaderDrawParameters())
            this.drawIndirectSupported = true;

                                                                                                         
        this.portabilitySubset = getUnsupportedExtensions(
                Set.of(KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME)).isEmpty();
    }

    private static String decodeVendor(int i) {
        return switch (i) {
            case (0x10DE) -> "Nvidia";
            case (0x1022) -> "AMD";
            case (0x8086) -> "Intel";
            default -> "undef";
        };
    }

    static String decDefVersion(int v) {
        return VK_VERSION_MAJOR(v) + "." + VK_VERSION_MINOR(v) + "." + VK_VERSION_PATCH(v);
    }

    private static String decodeDvrVersion(int v, int i) {
        return switch (i) {
            case (0x10DE) -> decodeNvidia(v);
            case (0x1022) -> decDefVersion(v);
            case (0x8086) -> decIntelVersion(v);
            default -> decDefVersion(v);
        };
    }

    private static String decIntelVersion(int v) {
        return (glfwGetPlatform() == GLFW_PLATFORM_WIN32) ? (v >>> 14) + "." + (v & 0x3fff) : decDefVersion(v);
    }

    private static String decodeNvidia(int v) {
        return (v >>> 22 & 0x3FF) + "." + (v >>> 14 & 0xff) + "." + (v >>> 6 & 0xff) + "." + (v & 0xff);
    }

    static int getVkVer() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var a = stack.mallocInt(1);
            vkEnumerateInstanceVersion(a);
            int vkVer1 = a.get(0);
            if (VK_VERSION_MINOR(vkVer1) < 2) {
                throw new RuntimeException("Vulkan 1.2 not supported: Only Has: %s".formatted(decDefVersion(vkVer1)));
            }
            return vkVer1;
        }
    }

    public Set<String> getUnsupportedExtensions(Set<String> requiredExtensions) {
        Set<String> unsupportedExtensions = new HashSet<>(requiredExtensions);
        unsupportedExtensions.removeAll(getAvailableExtensions(physicalDevice));
        return unsupportedExtensions;
    }

                                                                                                     
                                                                                                    
    static Set<String> getAvailableExtensions(VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0));
            try {
                vkEnumerateDeviceExtensionProperties(physicalDevice, (String) null, extensionCount, availableExtensions);

                return availableExtensions.stream()
                        .map(VkExtensionProperties::extensionNameString)
                        .collect(toSet());
            } finally {
                availableExtensions.free();
            }
        }
    }

    public boolean isDrawIndirectSupported() {
        return drawIndirectSupported;
    }

    public boolean isAMD() {
        return vendorId == 0x1022;
    }

    public boolean isNvidia() {
        return vendorId == 0x10DE;
    }

    public boolean isIntel() {
        return vendorId == 0x8086;
    }

                                                                                                                      
    public boolean isPortabilitySubset() {
        return portabilitySubset;
    }

    public void free() {
        this.properties.free();
        this.availableFeatures11.free();
        this.availableFeatures.free();
    }
}

