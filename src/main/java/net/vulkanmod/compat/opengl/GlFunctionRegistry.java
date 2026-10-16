package net.vulkanmod.compat.opengl;

import net.vulkanmod.gl.VkGlBuffer;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.compat.observer.GLCallObserver;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static net.vulkanmod.compat.opengl.GlTrampolines.*;

                                                                                                
public final class GlFunctionRegistry {
    public static final String REPORTED_GL_VERSION = "0.0 VulkanMod verified compatibility";
    private record Spec(String shape, GlTrampolines.Handler handler) { }
    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();
    private static final Map<String, Long> ADDRESSES = new LinkedHashMap<>();
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();

    static {
        fn("glGetError", "I:", (ret, args) -> retI(ret, GlErrorState.drain()));
                                                                                          
                                                                                           
        fn("glDrawElements", "V:iiip", (ret, args) -> {
            if (!ExternalTerrainRenderBridge.drawElements(argI(args, 0), argI(args, 1),
                    argI(args, 2), argP(args, 3))) {
                GlErrorState.record(0x0502);
            }
        });
        fn("glGetString", "P:i", (ret, args) -> {
            String value = switch (argI(args, 0)) {
                case 0x1F00 -> "VulkanMod";
                case 0x1F01 -> "Verified contextless compatibility functions";
                case 0x1F02 -> REPORTED_GL_VERSION;
                case 0x1F03 -> "";
                default -> null;
            };
            if (value == null) GlErrorState.record(0x0500);
            retP(ret, value == null ? 0L : pinnedString(value));
        });
        fn("glGetIntegerv", "V:ip", (ret, args) -> {
            int pname = argI(args, 0);
            int value = switch (pname) {
                case 0x821B, 0x821C, 0x821D, 0x821E, 0x9126 -> 0;
                case 0x8894, 0x8895, 0x88ED, 0x88EF, 0x8F36, 0x8F37 -> VkGlBuffer.getBoundId(pname);
                default -> { GlErrorState.record(0x0500); yield 0; }
            };
            putInt(argP(args, 1), value);
        });
        fn("glGenBuffers", "V:ip", (ret, args) -> generate(args, VkGlBuffer::glGenBuffers));
        fn("glCreateBuffers", "V:ip", (ret, args) -> generate(args, VkGlBuffer::glGenBuffers));
        fn("glDeleteBuffers", "V:ip", (ret, args) -> delete(args, VkGlBuffer::glDeleteBuffers));
        fn("glBindBuffer", "V:ii", (ret, args) -> VkGlBuffer.glBindBuffer(argI(args, 0), argI(args, 1)));
        fn("glIsBuffer", "b:i", (ret, args) -> retB(ret, VkGlBuffer.glIsBuffer(argI(args, 0))));
        fn("glBufferData", "V:ippi", (ret, args) -> {
            int target = argI(args, 0);
            long size = argP(args, 1);
            long data = argP(args, 2);
            if (data == 0L) VkGlBuffer.glBufferData(target, checkedSize(size), argI(args, 3));
            else VkGlBuffer.glBufferData(target, bytes(data, size), argI(args, 3));
        });
        fn("glBufferSubData", "V:ippp", (ret, args) ->
                VkGlBuffer.glBufferSubData(argI(args, 0), argP(args, 1), bytes(argP(args, 3), argP(args, 2))));
        fn("glGetBufferSubData", "V:ippp", (ret, args) ->
                VkGlBuffer.glGetBufferSubData(argI(args, 0), argP(args, 1), bytes(argP(args, 3), argP(args, 2))));
        fn("glGetBufferParameteriv", "V:iip", (ret, args) ->
                putInt(argP(args, 2), VkGlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1))));
        fn("glGetBufferParameteri64v", "V:iip", (ret, args) ->
                putLong(argP(args, 2), VkGlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1))));
        fn("glMapBuffer", "P:ii", (ret, args) ->
                retP(ret, addressOf(VkGlBuffer.glMapBuffer(argI(args, 0), argI(args, 1)))));
        fn("glUnmapBuffer", "b:i", (ret, args) -> retB(ret, VkGlBuffer.glUnmapBuffer(argI(args, 0))));
        fn("glMapBufferRange", "P:ippi", (ret, args) ->
                retP(ret, addressOf(VkGlBuffer.glMapBufferRange(argI(args, 0), argP(args, 1), argP(args, 2), argI(args, 3)))));
        fn("glCopyBufferSubData", "V:iippp", (ret, args) ->
                VkGlBuffer.glCopyBufferSubData(argI(args, 0), argI(args, 1), argP(args, 2), argP(args, 3), argP(args, 4)));
        for (String name : new String[]{"glGenBuffers", "glDeleteBuffers", "glBindBuffer", "glIsBuffer", "glBufferData",
                "glBufferSubData", "glGetBufferSubData", "glGetBufferParameteriv", "glMapBuffer", "glUnmapBuffer"}) {
            SPECS.put(name + "ARB", SPECS.get(name));
        }
    }

    private GlFunctionRegistry() { }

    public static synchronized long address(String name) {
        Spec spec = SPECS.get(name);
        if (spec == null) {
            if (name != null && name.startsWith("gl") && MISSING.size() < 256) MISSING.add(name);
            return 0L;
        }
        return ADDRESSES.computeIfAbsent(name, ignored -> GlTrampolines.create(name, spec.shape(), (ret, args) -> {
            GLCallObserver.observeCall(name, "");
            spec.handler().invoke(ret, args);
        }));
    }

    public static boolean isRegistered(String name) { return SPECS.containsKey(name); }
    public static int registeredCount() { return SPECS.size(); }
    public static Set<String> reportedMissingFunctions() { return Set.copyOf(MISSING); }
    public static Set<String> reportedStubbedFunctions() { return Set.of(); }
    public static Set<String> reportedFaultedFunctions() { return GlTrampolines.faultedFunctionsSnapshot(); }

    private static void fn(String name, String shape, GlTrampolines.Handler handler) { SPECS.put(name, new Spec(shape, handler)); }
    private static int checkedSize(long size) {
        if (size < 0 || size > Integer.MAX_VALUE) throw new IllegalArgumentException("Buffer size is outside the Java-backed data-store range: " + size);
        return (int) size;
    }
    private static ByteBuffer bytes(long pointer, long size) {
        if (pointer == 0L && size != 0L) throw new IllegalArgumentException("Buffer data pointer is null");
        return size == 0L ? ByteBuffer.allocateDirect(0) : MemoryUtil.memByteBuffer(pointer, checkedSize(size));
    }
    private static long addressOf(ByteBuffer buffer) { return buffer == null ? 0L : MemoryUtil.memAddress(buffer); }
    private static void putInt(long pointer, int value) {
        if (pointer == 0L) throw new IllegalArgumentException("Result pointer is null");
        MemoryUtil.memPutInt(pointer, value);
    }
    private static void putLong(long pointer, long value) {
        if (pointer == 0L) throw new IllegalArgumentException("Result pointer is null");
        MemoryUtil.memPutLong(pointer, value);
    }
    private static void generate(long args, IntSupplier generator) {
        int count = argI(args, 0);
        long pointer = argP(args, 1);
        if (count < 0 || (count > 0 && pointer == 0L)) throw new IllegalArgumentException("Invalid object-generation arguments");
        for (int i = 0; i < count; i++) MemoryUtil.memPutInt(pointer + (long) i * Integer.BYTES, generator.getAsInt());
    }
    private static void delete(long args, IntConsumer deleter) {
        int count = argI(args, 0);
        long pointer = argP(args, 1);
        if (count < 0 || (count > 0 && pointer == 0L)) throw new IllegalArgumentException("Invalid object-deletion arguments");
        for (int i = 0; i < count; i++) deleter.accept(MemoryUtil.memGetInt(pointer + (long) i * Integer.BYTES));
    }
}
