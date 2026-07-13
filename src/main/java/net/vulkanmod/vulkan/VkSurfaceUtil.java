package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.Platform;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.libffi.LibFFI.*;

import static org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTMetalSurface.*;
import static org.lwjgl.vulkan.KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRWaylandSurface.*;
import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.KHRXlibSurface.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Boot-safe replacements for the LWJGL {@code GLFWVulkan} helpers.
 * <p>
 * On NeoForge, {@code org.lwjgl.glfw.GLFWVulkan} is loaded in the BOOT module layer and its
 * static initializer references {@code org.lwjgl.vulkan.VK}. Our JiJ'd lwjgl-vulkan lives in the
 * GAME layer, which the BOOT layer cannot read, so any reference to {@code GLFWVulkan} throws
 * {@code NoClassDefFoundError: org/lwjgl/vulkan/VK}. We therefore build the required instance
 * extensions and create the window surface ourselves, from GAME-layer code that CAN see vulkan.
 * The GLFW native window-handle getters ({@code GLFWNative*}) live in BOOT lwjgl-glfw but contain
 * no vulkan references, so they are safe to call.
 */
public final class VkSurfaceUtil {
    private VkSurfaceUtil() {}

    /** Replacement for glfwGetRequiredInstanceExtensions(): VK_KHR_surface + the platform surface extension. */
    public static PointerBuffer getRequiredInstanceExtensions(MemoryStack stack, boolean withDebugUtils) {
        String platformSurface;
        if (Platform.isWindows())      platformSurface = VK_KHR_WIN32_SURFACE_EXTENSION_NAME;
        else if (Platform.isMacOS())   platformSurface = VK_EXT_METAL_SURFACE_EXTENSION_NAME;
        else if (Platform.isWayLand()) platformSurface = VK_KHR_WAYLAND_SURFACE_EXTENSION_NAME;
        else                           platformSurface = VK_KHR_XLIB_SURFACE_EXTENSION_NAME; // X11 (or unknown)

        int count = withDebugUtils ? 3 : 2;
        PointerBuffer extensions = stack.mallocPointer(count);
        extensions.put(stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME));
        extensions.put(stack.UTF8(platformSurface));
        if (withDebugUtils) {
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
        }
        return extensions.rewind();
    }

    /** Replacement for glfwCreateWindowSurface(): create a VkSurfaceKHR for the given GLFW window. */
    public static long createSurface(VkInstance instance, long window, MemoryStack stack) {
        if (Platform.isMacOS())        return createMetalSurface(instance, window, stack);
        else if (Platform.isWindows()) return createWin32Surface(instance, window, stack);
        else if (Platform.isWayLand()) return createWaylandSurface(instance, window, stack);
        else                           return createXlibSurface(instance, window, stack);
    }

    // ---- macOS / MoltenVK -----------------------------------------------------------------

    private static long createMetalSurface(VkInstance instance, long window, MemoryStack stack) {
        long nsWindow = org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow(window);
        if (nsWindow == 0L) throw new RuntimeException("glfwGetCocoaWindow returned NULL");

        // Attach a CAMetalLayer to the window's contentView, then create a Metal surface from it.
        // Objective-C runtime glue (no LWJGL wrapper for objc_msgSend, so we invoke it directly).
        long objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
        long contentView = JNI.invokePPP(nsWindow, ObjCRuntime.sel_getUid("contentView"), objc_msgSend);
        long metalLayer  = JNI.invokePPP(ObjCRuntime.objc_getClass("CAMetalLayer"), ObjCRuntime.sel_getUid("layer"), objc_msgSend);
        if (contentView == 0L || metalLayer == 0L) throw new RuntimeException("Failed to obtain contentView/CAMetalLayer");
        JNI.invokePPPP(contentView, ObjCRuntime.sel_getUid("setWantsLayer:"), 1L, objc_msgSend); // setWantsLayer:YES
        JNI.invokePPPP(contentView, ObjCRuntime.sel_getUid("setLayer:"), metalLayer, objc_msgSend);

        // Retina: match the layer's contentsScale to the window's backing scale, otherwise MoltenVK
        // renders into a 1x drawable while the framebuffer is 2x -> blurry / low-res / offset image.
        float[] sx = { 1.0f };
        float[] sy = { 1.0f };
        org.lwjgl.glfw.GLFW.glfwGetWindowContentScale(window, sx, sy);
        setContentsScale(objc_msgSend, metalLayer, sx[0] > 0 ? sx[0] : 1.0, stack);

        VkMetalSurfaceCreateInfoEXT createInfo = VkMetalSurfaceCreateInfoEXT.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_METAL_SURFACE_CREATE_INFO_EXT);
        // pLayer is `const CAMetalLayer*`; write the layer id straight into the field.
        org.lwjgl.system.MemoryUtil.memPutAddress(createInfo.address() + VkMetalSurfaceCreateInfoEXT.PLAYER, metalLayer);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateMetalSurfaceEXT(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Metal surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_EXT_metal_surface (MoltenVK)");
        return pSurface.get(0);
    }

    // ---- Windows --------------------------------------------------------------------------

    private static long createWin32Surface(VkInstance instance, long window, MemoryStack stack) {
        long hwnd = org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(window);
        // HINSTANCE from the window (GWLP_HINSTANCE = -6)
        long hinstance = org.lwjgl.system.windows.User32.GetWindowLongPtr(hwnd, -6);

        VkWin32SurfaceCreateInfoKHR createInfo = VkWin32SurfaceCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR)
                .hinstance(hinstance)
                .hwnd(hwnd);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateWin32SurfaceKHR(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Win32 surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_KHR_win32_surface");
        return pSurface.get(0);
    }

    // ---- Linux X11 ------------------------------------------------------------------------

    private static long createXlibSurface(VkInstance instance, long window, MemoryStack stack) {
        long display = org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display();
        long x11Window = org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window(window);

        VkXlibSurfaceCreateInfoKHR createInfo = VkXlibSurfaceCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_XLIB_SURFACE_CREATE_INFO_KHR)
                .dpy(display)
                .window(x11Window);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateXlibSurfaceKHR(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Xlib surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_KHR_xlib_surface");
        return pSurface.get(0);
    }

    // ---- Linux Wayland --------------------------------------------------------------------

    private static long createWaylandSurface(VkInstance instance, long window, MemoryStack stack) {
        long display = org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandDisplay();
        long waylandSurface = org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandWindow(window);

        VkWaylandSurfaceCreateInfoKHR createInfo = VkWaylandSurfaceCreateInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_WAYLAND_SURFACE_CREATE_INFO_KHR)
                .display(display)
                .surface(waylandSurface);

        LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);
        int r = vkCreateWaylandSurfaceKHR(instance, createInfo, null, pSurface);
        Vulkan.checkResult(r, "Failed to create Wayland surface");
        Initializer.LOGGER.info("VulkanMod: created VkSurfaceKHR via VK_KHR_wayland_surface");
        return pSurface.get(0);
    }

    /**
     * Calls {@code [metalLayer setContentsScale:scale]} via libffi. LWJGL 3.3.3 has no
     * {@code objc_msgSend} trampoline that takes a {@code double} argument, so we build the call
     * interface ourselves (receiver, selector, CGFloat) -> void.
     */
    private static void setContentsScale(long objc_msgSend, long metalLayer, double scale, MemoryStack stack) {
        long sel = ObjCRuntime.sel_getUid("setContentsScale:");
        FFICIF cif = FFICIF.malloc(stack);
        PointerBuffer argTypes = stack.mallocPointer(3);
        argTypes.put(0, ffi_type_pointer);
        argTypes.put(1, ffi_type_pointer);
        argTypes.put(2, ffi_type_double);
        if (ffi_prep_cif(cif, FFI_DEFAULT_ABI, ffi_type_void, argTypes) != FFI_OK) {
            Initializer.LOGGER.warn("VulkanMod: ffi_prep_cif failed; Retina contentsScale not applied");
            return;
        }
        PointerBuffer receiver = stack.pointers(metalLayer);
        PointerBuffer selector = stack.pointers(sel);
        DoubleBuffer scaleValue = stack.doubles(scale);
        PointerBuffer argValues = stack.mallocPointer(3);
        argValues.put(0, MemoryUtil.memAddress(receiver));
        argValues.put(1, MemoryUtil.memAddress(selector));
        argValues.put(2, MemoryUtil.memAddress(scaleValue));
        ffi_call(cif, objc_msgSend, (ByteBuffer) null, argValues);
    }
}
