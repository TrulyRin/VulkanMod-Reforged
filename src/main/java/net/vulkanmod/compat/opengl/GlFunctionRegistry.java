package net.vulkanmod.compat.opengl;

import net.minecraft.client.Minecraft;
import net.vulkanmod.compat.opengl.GlDrawContract;
import net.vulkanmod.compat.gl.GlCapabilityState;
import net.vulkanmod.compat.gl.GlIntegerState;
import net.vulkanmod.compat.gl.GlPixelStore;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlQuery;
import net.vulkanmod.gl.GlRenderbuffer;
import net.vulkanmod.gl.GlSampler;
import net.vulkanmod.gl.GlSync;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.gl.GlVertexArray;
import net.vulkanmod.mixin.compatibility.gl.GL40M;
import net.vulkanmod.mixin.compatibility.gl.GL41M;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import static net.vulkanmod.compat.opengl.GlTrampolines.argB;
import static net.vulkanmod.compat.opengl.GlTrampolines.argD;
import static net.vulkanmod.compat.opengl.GlTrampolines.argF;
import static net.vulkanmod.compat.opengl.GlTrampolines.argI;
import static net.vulkanmod.compat.opengl.GlTrampolines.argL;
import static net.vulkanmod.compat.opengl.GlTrampolines.argP;
import static net.vulkanmod.compat.opengl.GlTrampolines.pinnedString;
import static net.vulkanmod.compat.opengl.GlTrampolines.retB;
import static net.vulkanmod.compat.opengl.GlTrampolines.retD;
import static net.vulkanmod.compat.opengl.GlTrampolines.retF;
import static net.vulkanmod.compat.opengl.GlTrampolines.retI;
import static net.vulkanmod.compat.opengl.GlTrampolines.retL;
import static net.vulkanmod.compat.opengl.GlTrampolines.retP;

public final class GlFunctionRegistry {

    private record Spec(String shape, GlTrampolines.Handler handler) {
    }

    private static final Map<String, Spec> SPECS = new LinkedHashMap<>();
    private static final Map<String, Long> ADDRESSES = new HashMap<>();
    private static final Set<String> REPORTED_MISSING = ConcurrentHashMap.newKeySet();
    private static final Set<String> REPORTED_STUBS = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, Integer> ARB_OBJECT_TO_CORE = new HashMap<>();
    private static final Map<Integer, Integer> ARB_OBJECT_TYPE = new HashMap<>();
    private static final Map<Integer, Integer> ARB_OBJECT_SUBTYPE = new HashMap<>();
    private static final Map<Integer, Integer> ARB_CORE_PROGRAM_TO_HANDLE = new HashMap<>();
    private static final Set<Integer> ARB_VERTEX_PROGRAMS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> EXT_SEMAPHORES = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> EXT_MEMORY_OBJECTS = ConcurrentHashMap.newKeySet();
    private static final Set<Long> ARB_RESIDENT_TEXTURE_HANDLES = ConcurrentHashMap.newKeySet();
    private static final Set<Long> ARB_RESIDENT_IMAGE_HANDLES = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> NV_PATHS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> NV_FENCES = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> NV_QUERY_RESOURCE_TAGS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> NV_COMMAND_STATES = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> NV_COMMAND_LISTS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> NV_TRANSFORM_FEEDBACKS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> AMD_PERF_MONITORS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> INTEL_PERF_QUERIES = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> LEGACY_DISPLAY_LISTS = ConcurrentHashMap.newKeySet();

    private static final int[] VIEWPORT = new int[4];
    private static int nextArbObjectHandle = 0x10000000;
    private static int nextArbVertexProgramId = 1;
    private static int nextExtSemaphoreId = 1;
    private static int nextExtMemoryObjectId = 1;
    private static int nextNvPathId = 1;
    private static int nextNvFenceId = 1;
    private static int nextNvQueryResourceTagId = 1;
    private static int nextNvCommandStateId = 1;
    private static int nextNvCommandListId = 1;
    private static int nextNvTransformFeedbackId = 1;
    private static int nextAmdPerfMonitorId = 1;
    private static int nextIntelPerfQueryId = 1;
    private static int nextLegacyDisplayListId = 1;

    private static final int GL_VIEWPORT = 0x0BA2;
    private static final int GL_COLOR_CLEAR_VALUE = 0x0C22;
    private static final int GL_MAJOR_VERSION = 0x821B;
    private static final int GL_MINOR_VERSION = 0x821C;
    private static final int GL_NUM_EXTENSIONS = 0x821D;
    private static final int GL_CONTEXT_FLAGS = 0x821E;
    private static final int GL_CONTEXT_PROFILE_MASK = 0x9126;
    private static final int GL_VENDOR = 0x1F00;
    private static final int GL_RENDERER = 0x1F01;
    private static final int GL_VERSION = 0x1F02;
    private static final int GL_EXTENSIONS = 0x1F03;
    private static final int GL_SHADING_LANGUAGE_VERSION = 0x8B8C;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_TEXTURE0 = 0x84C0;
    private static final int GL_DEPTH_TEST = 0x0B71;
    private static final int GL_STENCIL_TEST = 0x0B90;
    private static final int GL_CULL_FACE = 0x0B44;
    private static final int GL_BLEND = 0x0BE2;
    private static final int GL_FRAMEBUFFER_COMPLETE = 0x8CD5;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_STATIC_DRAW = 0x88E4;
    private static final int GL_LINE_WIDTH = 0x0B21;
    private static final int GL_INVALID_INDEX = 0xFFFFFFFF;
    private static final int GL_PROGRAM_OBJECT_ARB = 0x8B40;
    private static final int GL_SHADER_OBJECT_ARB = 0x8B48;
    private static final int GL_OBJECT_TYPE_ARB = 0x8B4E;
    private static final int GL_OBJECT_SUBTYPE_ARB = 0x8B4F;
    private static final int GL_OBJECT_DELETE_STATUS_ARB = 0x8B80;
    private static final int GL_OBJECT_COMPILE_STATUS_ARB = 0x8B81;
    private static final int GL_OBJECT_LINK_STATUS_ARB = 0x8B82;
    private static final int GL_OBJECT_VALIDATE_STATUS_ARB = 0x8B83;
    private static final int GL_OBJECT_INFO_LOG_LENGTH_ARB = 0x8B84;
    private static final int GL_OBJECT_ATTACHED_OBJECTS_ARB = 0x8B85;
    private static final int GL_OBJECT_ACTIVE_UNIFORMS_ARB = 0x8B86;
    private static final int GL_OBJECT_ACTIVE_UNIFORM_MAX_LENGTH_ARB = 0x8B87;
    private static final int GL_OBJECT_SHADER_SOURCE_LENGTH_ARB = 0x8B88;

    public static final String REPORTED_GL_VERSION = "3.2.0 VulkanMod Compatibility";

    static {
        registerAll();
    }

    private GlFunctionRegistry() {
    }

    public static synchronized long address(String name) {
        Spec spec = SPECS.get(name);
        if (spec == null) {
            if (name.startsWith("gl") && REPORTED_MISSING.add(name)) {
                GlTrampolines.logWarn("Unimplemented GL function requested: {} (reported absent)", name);
            }
            return 0L;
        }

        Long cached = ADDRESSES.get(name);
        if (cached != null) {
            return cached;
        }

        long address = GlTrampolines.create(name, spec.shape(), spec.handler());
        ADDRESSES.put(name, address);
        return address;
    }

    public static boolean isRegistered(String name) {
        return SPECS.containsKey(name);
    }

    public static int registeredCount() {
        return SPECS.size();
    }

    private static String contractFamily(String functionName) {
        if (functionName == null || functionName.isBlank()) {
            return "provider";
        }
        String name = functionName.toLowerCase(java.util.Locale.ROOT);
        if (name.contains("shader") || name.contains("program") || name.contains("uniform") || name.contains("attrib")) {
            return "shader_conversion";
        }
        if (name.contains("tex") || name.contains("sampler") || name.contains("image") || name.contains("mipmap")) {
            return "texture_image";
        }
        if (name.contains("framebuffer") || name.contains("renderbuffer") || name.contains("readpixels")
                || name.contains("blit") || name.contains("clear")) {
            return "framebuffer_readback";
        }
        if (name.contains("draw") || name.contains("begin") || name.contains("end") || name.contains("calllist")
                || name.contains("arrayelement")) {
            return "draw_path";
        }
        if (name.contains("buffer") || name.contains("vertexarray") || name.contains("query") || name.contains("sync")
                || name.contains("fence") || name.contains("list")) {
            return "object_lifetime";
        }
        if (name.startsWith("glget") || name.startsWith("glis") || name.contains("enable") || name.contains("disable")
                || name.contains("blend") || name.contains("depth") || name.contains("stencil") || name.contains("scissor")
                || name.contains("viewport") || name.contains("cull") || name.contains("polygon")) {
            return "state_query";
        }
        return "provider";
    }

    private static void fn(String name, String shape, GlTrampolines.Handler handler) {
        SPECS.put(name, new Spec(shape, handler));
    }

    private static void stub(String... names) {
        for (String name : names) {
            SPECS.put(name, new Spec("L:", (ret, args) -> {
                if (REPORTED_STUBS.add(name)) {
                    GlTrampolines.logWarn("Stubbed GL function called: {} (no-op, returns 0)", name);
                }
                GlTrampolines.retL(ret, 0L);
            }));
        }
    }

    private static void noop(String name, String shape) {
        fn(name, shape, (ret, args) -> {
        });
    }

    private static void genLoop(long args, IntSupplier generator) {
        int n = argI(args, 0);
        long ptr = argP(args, 1);
        for (int i = 0; i < n && ptr != 0; i++) {
            MemoryUtil.memPutInt(ptr + 4L * i, generator.getAsInt());
        }
    }

    private static void deleteLoop(long args, IntConsumer deleter) {
        int n = argI(args, 0);
        long ptr = argP(args, 1);
        for (int i = 0; i < n && ptr != 0; i++) {
            deleter.accept(MemoryUtil.memGetInt(ptr + 4L * i));
        }
    }

    private static void zeroInt(long ptr) {
        if (ptr != 0) {
            MemoryUtil.memPutInt(ptr, 0);
        }
    }

    private static void zeroLong(long ptr) {
        if (ptr != 0) {
            MemoryUtil.memPutLong(ptr, 0L);
        }
    }

    private static void zeroBytes(long ptr, int byteCount) {
        if (ptr != 0 && byteCount > 0) {
            MemoryUtil.memSet(ptr, 0, byteCount);
        }
    }

    private static int newArbObject(int coreId, int objectType, int subtype) {
        int handle = nextArbObjectHandle++;
        ARB_OBJECT_TO_CORE.put(handle, coreId);
        ARB_OBJECT_TYPE.put(handle, objectType);
        ARB_OBJECT_SUBTYPE.put(handle, subtype);
        if (objectType == GL_PROGRAM_OBJECT_ARB) {
            ARB_CORE_PROGRAM_TO_HANDLE.put(coreId, handle);
        }
        return handle;
    }

    private static int arbCoreObject(int handle) {
        return ARB_OBJECT_TO_CORE.getOrDefault(handle, 0);
    }

    private static void deleteArbObject(int handle) {
        int core = arbCoreObject(handle);
        int type = ARB_OBJECT_TYPE.getOrDefault(handle, 0);
        if (type == GL_PROGRAM_OBJECT_ARB) {
            GlProgram.deleteProgram(core);
            ARB_CORE_PROGRAM_TO_HANDLE.remove(core);
        } else if (type == GL_SHADER_OBJECT_ARB) {
            GlProgram.deleteShader(core);
        }
        ARB_OBJECT_TO_CORE.remove(handle);
        ARB_OBJECT_TYPE.remove(handle);
        ARB_OBJECT_SUBTYPE.remove(handle);
    }

    private static int getArbObjectParameter(int handle, int pname) {
        return switch (pname) {
            case GL_OBJECT_TYPE_ARB -> ARB_OBJECT_TYPE.getOrDefault(handle, 0);
            case GL_OBJECT_SUBTYPE_ARB -> ARB_OBJECT_SUBTYPE.getOrDefault(handle, 0);
            case GL_OBJECT_DELETE_STATUS_ARB -> 0;
            case GL_OBJECT_INFO_LOG_LENGTH_ARB, GL_OBJECT_ATTACHED_OBJECTS_ARB,
                    GL_OBJECT_ACTIVE_UNIFORMS_ARB, GL_OBJECT_ACTIVE_UNIFORM_MAX_LENGTH_ARB,
                    GL_OBJECT_SHADER_SOURCE_LENGTH_ARB -> 0;
            case GL_OBJECT_COMPILE_STATUS_ARB, GL_OBJECT_LINK_STATUS_ARB,
                    GL_OBJECT_VALIDATE_STATUS_ARB -> 1;
            default -> 0;
        };
    }

    private static int genArbVertexProgram() {
        int id = nextArbVertexProgramId++;
        ARB_VERTEX_PROGRAMS.add(id);
        return id;
    }

    private static int genExtSemaphore() {
        int id = nextExtSemaphoreId++;
        EXT_SEMAPHORES.add(id);
        return id;
    }

    private static int genExtMemoryObject() {
        int id = nextExtMemoryObjectId++;
        EXT_MEMORY_OBJECTS.add(id);
        return id;
    }

    private static int genNvFence() {
        int id = nextNvFenceId++;
        NV_FENCES.add(id);
        return id;
    }

    private static int genNvQueryResourceTag() {
        int id = nextNvQueryResourceTagId++;
        NV_QUERY_RESOURCE_TAGS.add(id);
        return id;
    }

    private static int genNvCommandState() {
        int id = nextNvCommandStateId++;
        NV_COMMAND_STATES.add(id);
        return id;
    }

    private static int genNvCommandList() {
        int id = nextNvCommandListId++;
        NV_COMMAND_LISTS.add(id);
        return id;
    }

    private static int genNvTransformFeedback() {
        int id = nextNvTransformFeedbackId++;
        NV_TRANSFORM_FEEDBACKS.add(id);
        return id;
    }

    private static int genAmdPerfMonitor() {
        int id = nextAmdPerfMonitorId++;
        AMD_PERF_MONITORS.add(id);
        return id;
    }

    private static int genIntelPerfQuery() {
        int id = nextIntelPerfQueryId++;
        INTEL_PERF_QUERIES.add(id);
        return id;
    }

    private static int genLegacyDisplayLists(int range) {
        if (range <= 0) {
            return 0;
        }
        int first = nextLegacyDisplayListId;
        nextLegacyDisplayListId += range;
        for (int i = 0; i < range && first + i > 0; i++) {
            LEGACY_DISPLAY_LISTS.add(first + i);
        }
        return first;
    }

    private static void deleteLegacyDisplayLists(int first, int range) {
        for (int i = 0; i < range && first + i > 0; i++) {
            LEGACY_DISPLAY_LISTS.remove(first + i);
        }
    }

    private static int genNvPaths(int range) {
        if (range <= 0) {
            return 0;
        }
        int first = nextNvPathId;
        nextNvPathId += range;
        addNvPathRange(first, range);
        return first;
    }

    private static void addNvPath(int path) {
        if (path > 0) {
            NV_PATHS.add(path);
        }
    }

    private static void addNvPathRange(int first, int range) {
        for (int i = 0; i < range && first + i > 0; i++) {
            NV_PATHS.add(first + i);
        }
    }

    private static void deleteNvPathRange(int first, int range) {
        for (int i = 0; i < range && first + i > 0; i++) {
            NV_PATHS.remove(first + i);
        }
    }

    private static long fakeBindlessHandle(long namespace, long a, long b, long c, long d, long e) {
        long x = 0x56554C4B00000000L ^ namespace;
        x ^= Long.rotateLeft(a * 0x9E3779B97F4A7C15L, 7);
        x ^= Long.rotateLeft(b * 0xC2B2AE3D27D4EB4FL, 17);
        x ^= Long.rotateLeft(c * 0x165667B19E3779F9L, 29);
        x ^= Long.rotateLeft(d * 0x85EBCA77C2B2AE63L, 41);
        x ^= Long.rotateLeft(e * 0x27D4EB2F165667C5L, 53);
        return x == 0L ? 1L : x;
    }

    private static long fakeTextureHandle(int texture, int sampler) {
        return fakeBindlessHandle(0x54455854L, Integer.toUnsignedLong(texture),
                Integer.toUnsignedLong(sampler), 0L, 0L, 0L);
    }

    private static long fakeImageHandle(int texture, int level, boolean layered, int layer, int format) {
        return fakeBindlessHandle(0x494D4147L, Integer.toUnsignedLong(texture),
                Integer.toUnsignedLong(level), layered ? 1L : 0L,
                Integer.toUnsignedLong(layer), Integer.toUnsignedLong(format));
    }

    private static String shaderSourceFromPointers(int count, long strings, long lengths) {
        StringBuilder source = new StringBuilder();
        for (int i = 0; i < count && strings != 0; i++) {
            long string = MemoryUtil.memGetAddress(strings + (long) i * Long.BYTES);
            if (string == 0) {
                continue;
            }
            int length = lengths == 0 ? -1 : MemoryUtil.memGetInt(lengths + 4L * i);
            source.append(length < 0
                    ? MemoryUtil.memUTF8(string)
                    : MemoryUtil.memUTF8(MemoryUtil.memByteBuffer(string, length)));
        }
        return source.toString();
    }

    private static void writeOutString(String value, int bufSize, long lengthPtr, long stringPtr) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int n = Math.max(0, Math.min(bytes.length, bufSize - 1));
        if (stringPtr != 0 && bufSize > 0) {
            for (int i = 0; i < n; i++) {
                MemoryUtil.memPutByte(stringPtr + i, bytes[i]);
            }
            MemoryUtil.memPutByte(stringPtr + n, (byte) 0);
        }
        if (lengthPtr != 0) {
            MemoryUtil.memPutInt(lengthPtr, n);
        }
    }

    private static void withTexture(int texture, Runnable action) {
        if (GlTexture.getTexture(texture) == null) {
            return;
        }

        int previous = GlTexture.getBoundTextureId(GL_TEXTURE_2D);
        GlTexture.bindTexture(texture);
        try {
            action.run();
        } finally {
            GlTexture.bindTexture(previous);
        }
    }

    private static void withTextureUnit(int texunit, Runnable action) {
        int previous = GlTexture.getActiveTexture();
        GlTexture.activeTexture(texunit);
        try {
            action.run();
        } finally {
            GlTexture.activeTexture(previous);
        }
    }

    private static void ensureViewport() {
        if (VIEWPORT[2] == 0 || VIEWPORT[3] == 0) {
            try {
                var window = Minecraft.getInstance().getWindow();
                VIEWPORT[0] = 0;
                VIEWPORT[1] = 0;
                VIEWPORT[2] = window.getWidth();
                VIEWPORT[3] = window.getHeight();
            } catch (Throwable ignored) {
            }
        }
    }

    private static int integerValue(int pname, int index) {
        return switch (pname) {
            case GL_MAJOR_VERSION -> 3;
            case GL_MINOR_VERSION -> 2;
            case GL_NUM_EXTENSIONS -> 0;
            case GL_CONTEXT_PROFILE_MASK -> 1;
            case GL_CONTEXT_FLAGS -> 0;
            default -> GlPixelStore.isPixelStoreParameter(pname)
                    ? GlPixelStore.getInteger(pname)
                    : GlIntegerState.getInteger(pname, index);
        };
    }

    private static int componentCount(int pname) {
        return switch (pname) {
            case GL_MAJOR_VERSION, GL_MINOR_VERSION, GL_NUM_EXTENSIONS, GL_CONTEXT_PROFILE_MASK, GL_CONTEXT_FLAGS -> 1;
            default -> GlPixelStore.isPixelStoreParameter(pname) ? 1 : GlIntegerState.getComponentCount(pname);
        };
    }

    private static long glStringAddress(int name) {
        return switch (name) {
            case GL_VENDOR -> pinnedString("VulkanMod");
            case GL_RENDERER -> pinnedString("VulkanMod Vulkan Renderer");
            case GL_VERSION -> pinnedString(REPORTED_GL_VERSION);
            case GL_SHADING_LANGUAGE_VERSION -> pinnedString("1.50");
            case GL_EXTENSIONS -> pinnedString("");
            default -> pinnedString("");
        };
    }

    private static void registerAll() {
        registerGetters();
        registerLegacyFixedFunctionDirectProvider();
        registerGl11();
        registerGl12to14();
        registerGl15();
        registerGl20();
        registerGl21();
        registerGl30();
        registerGl31();
        registerGl32();
        registerGl33();
        registerGl4x();
        registerModernControlPlane();
        registerModernUtilitySurface();
        registerDirectStateAccess();
        registerExtensionAliases();
        registerStubs();
    }

    private static void registerLegacyFixedFunctionDirectProvider() {
        fn("glGenLists", "I:i", (ret, args) -> retI(ret, genLegacyDisplayLists(argI(args, 0))));
        fn("glDeleteLists", "V:ii", (ret, args) -> deleteLegacyDisplayLists(argI(args, 0), argI(args, 1)));
        fn("glIsList", "b:i", (ret, args) -> retB(ret, LEGACY_DISPLAY_LISTS.contains(argI(args, 0))));
        noop("glNewList", "V:ii");
        noop("glEndList", "V:");
        noop("glCallList", "V:i");
        noop("glCallLists", "V:iip");
        noop("glListBase", "V:i");
        fn("glRenderMode", "I:i", (ret, args) -> retI(ret, 0));

        noop("glBegin", "V:i");
        noop("glEnd", "V:");
        noop("glMatrixMode", "V:i");
        noop("glPushMatrix", "V:");
        noop("glPopMatrix", "V:");
        noop("glLoadIdentity", "V:");
        noop("glLoadMatrixf", "V:p");
        noop("glLoadMatrixd", "V:p");
        noop("glMultMatrixf", "V:p");
        noop("glMultMatrixd", "V:p");
        noop("glLoadTransposeMatrixf", "V:p");
        noop("glLoadTransposeMatrixd", "V:p");
        noop("glMultTransposeMatrixf", "V:p");
        noop("glMultTransposeMatrixd", "V:p");
        noop("glTranslatef", "V:fff");
        noop("glTranslated", "V:ddd");
        noop("glRotatef", "V:ffff");
        noop("glRotated", "V:dddd");
        noop("glScalef", "V:fff");
        noop("glScaled", "V:ddd");
        noop("glOrtho", "V:dddddd");
        noop("glFrustum", "V:dddddd");

        noop("glVertex2f", "V:ff");
        noop("glVertex2i", "V:ii");
        noop("glVertex2s", "V:ss");
        noop("glVertex2d", "V:dd");
        noop("glVertex3f", "V:fff");
        noop("glVertex3i", "V:iii");
        noop("glVertex3s", "V:sss");
        noop("glVertex3d", "V:ddd");
        noop("glVertex4f", "V:ffff");
        noop("glVertex4i", "V:iiii");
        noop("glVertex4s", "V:ssss");
        noop("glVertex4d", "V:dddd");
        for (String vector : new String[]{"2fv", "2iv", "2sv", "2dv", "3fv", "3iv", "3sv", "3dv", "4fv", "4iv", "4sv", "4dv"}) {
            noop("glVertex" + vector, "V:p");
        }

        noop("glColor3f", "V:fff");
        noop("glColor3i", "V:iii");
        noop("glColor3s", "V:sss");
        noop("glColor3b", "V:bbb");
        noop("glColor3d", "V:ddd");
        noop("glColor4f", "V:ffff");
        noop("glColor4i", "V:iiii");
        noop("glColor4s", "V:ssss");
        noop("glColor4b", "V:bbbb");
        noop("glColor4d", "V:dddd");
        for (String vector : new String[]{"3fv", "3iv", "3sv", "3bv", "3dv", "4fv", "4iv", "4sv", "4bv", "4dv"}) {
            noop("glColor" + vector, "V:p");
        }

        noop("glNormal3f", "V:fff");
        noop("glNormal3i", "V:iii");
        noop("glNormal3s", "V:sss");
        noop("glNormal3b", "V:bbb");
        noop("glNormal3d", "V:ddd");
        for (String vector : new String[]{"3fv", "3iv", "3sv", "3bv", "3dv"}) {
            noop("glNormal" + vector, "V:p");
        }

        for (String dim : new String[]{"1", "2", "3", "4"}) {
            String ints = "i".repeat(Integer.parseInt(dim));
            String shorts = "s".repeat(Integer.parseInt(dim));
            String floats = "f".repeat(Integer.parseInt(dim));
            String doubles = "d".repeat(Integer.parseInt(dim));
            noop("glTexCoord" + dim + "i", "V:" + ints);
            noop("glTexCoord" + dim + "s", "V:" + shorts);
            noop("glTexCoord" + dim + "f", "V:" + floats);
            noop("glTexCoord" + dim + "d", "V:" + doubles);
            noop("glTexCoord" + dim + "iv", "V:p");
            noop("glTexCoord" + dim + "sv", "V:p");
            noop("glTexCoord" + dim + "fv", "V:p");
            noop("glTexCoord" + dim + "dv", "V:p");
            noop("glMultiTexCoord" + dim + "i", "V:i" + ints);
            noop("glMultiTexCoord" + dim + "s", "V:i" + shorts);
            noop("glMultiTexCoord" + dim + "f", "V:i" + floats);
            noop("glMultiTexCoord" + dim + "d", "V:i" + doubles);
            noop("glMultiTexCoord" + dim + "iv", "V:ip");
            noop("glMultiTexCoord" + dim + "sv", "V:ip");
            noop("glMultiTexCoord" + dim + "fv", "V:ip");
            noop("glMultiTexCoord" + dim + "dv", "V:ip");
        }

        noop("glShadeModel", "V:i");
        noop("glAlphaFunc", "V:if");
        noop("glFogf", "V:if");
        noop("glFogi", "V:ii");
        noop("glFogfv", "V:ip");
        noop("glFogiv", "V:ip");
        noop("glLightf", "V:iif");
        noop("glLighti", "V:iii");
        noop("glLightfv", "V:iip");
        noop("glLightiv", "V:iip");
        noop("glLightModelf", "V:if");
        noop("glLightModeli", "V:ii");
        noop("glLightModelfv", "V:ip");
        noop("glLightModeliv", "V:ip");
        noop("glMaterialf", "V:iif");
        noop("glMateriali", "V:iii");
        noop("glMaterialfv", "V:iip");
        noop("glMaterialiv", "V:iip");
        noop("glTexEnvf", "V:iif");
        noop("glTexEnvi", "V:iii");
        noop("glTexEnvfv", "V:iip");
        noop("glTexEnviv", "V:iip");
        noop("glTexGenf", "V:iif");
        noop("glTexGeni", "V:iii");
        noop("glTexGenfv", "V:iip");
        noop("glTexGeniv", "V:iip");
        fn("glGetTexEnvfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTexEnviv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTexGenfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTexGeniv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));

        noop("glRasterPos2f", "V:ff");
        noop("glRasterPos2i", "V:ii");
        noop("glRasterPos3f", "V:fff");
        noop("glRasterPos3i", "V:iii");
        noop("glRasterPos4f", "V:ffff");
        noop("glRasterPos4i", "V:iiii");
        for (String vector : new String[]{"2fv", "2iv", "3fv", "3iv", "4fv", "4iv"}) {
            noop("glRasterPos" + vector, "V:p");
        }
        noop("glRectf", "V:ffff");
        noop("glRecti", "V:iiii");
        noop("glRectfv", "V:pp");
        noop("glRectiv", "V:pp");
        noop("glWindowPos2f", "V:ff");
        noop("glWindowPos2i", "V:ii");
        noop("glWindowPos3f", "V:fff");
        noop("glWindowPos3i", "V:iii");
        noop("glWindowPos2fv", "V:p");
        noop("glWindowPos2iv", "V:p");
        noop("glWindowPos3fv", "V:p");
        noop("glWindowPos3iv", "V:p");

        noop("glAccum", "V:if");
        fn("glAreTexturesResident", "b:ipp", (ret, args) -> {
            zeroBytes(argP(args, 2), Math.max(1, argI(args, 0)));
            retB(ret, false);
        });
        noop("glArrayElement", "V:i");
        noop("glBitmap", "V:iiffffp");
        noop("glClearAccum", "V:ffff");
        noop("glClearIndex", "V:f");
        noop("glClientActiveTexture", "V:i");
        noop("glClipPlane", "V:ip");
        fn("glGetClipPlane", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), Double.BYTES * 4));

        noop("glColor3ub", "V:bbb");
        noop("glColor3ui", "V:iii");
        noop("glColor3us", "V:sss");
        noop("glColor4ub", "V:bbbb");
        noop("glColor4ui", "V:iiii");
        noop("glColor4us", "V:ssss");
        for (String vector : new String[]{"3ubv", "3uiv", "3usv", "4ubv", "4uiv", "4usv"}) {
            noop("glColor" + vector, "V:p");
        }
        noop("glColorMaterial", "V:ii");
        noop("glColorPointer", "V:iiip");
        noop("glCopyPixels", "V:iiiii");
        noop("glDisableClientState", "V:i");
        noop("glDrawPixels", "V:iiiip");
        noop("glEdgeFlag", "V:b");
        noop("glEdgeFlagPointer", "V:ip");
        noop("glEdgeFlagv", "V:p");
        noop("glEnableClientState", "V:i");
        noop("glFeedbackBuffer", "V:iip");
        noop("glFogCoordPointer", "V:iip");
        noop("glNormalPointer", "V:iip");
        noop("glIndexPointer", "V:iip");
        noop("glInterleavedArrays", "V:iip");
        noop("glSecondaryColorPointer", "V:iiip");
        noop("glTexCoordPointer", "V:iiip");
        noop("glVertexPointer", "V:iiip");

        noop("glEvalCoord1d", "V:d");
        noop("glEvalCoord1f", "V:f");
        noop("glEvalCoord2d", "V:dd");
        noop("glEvalCoord2f", "V:ff");
        for (String vector : new String[]{"1dv", "1fv", "2dv", "2fv"}) {
            noop("glEvalCoord" + vector, "V:p");
        }
        noop("glEvalMesh1", "V:iii");
        noop("glEvalMesh2", "V:iiiii");
        noop("glEvalPoint1", "V:i");
        noop("glEvalPoint2", "V:ii");
        noop("glMap1d", "V:iddiip");
        noop("glMap1f", "V:iffiip");
        noop("glMap2d", "V:iddiiddiip");
        noop("glMap2f", "V:iffiiffiip");
        noop("glMapGrid1d", "V:idd");
        noop("glMapGrid1f", "V:iff");
        noop("glMapGrid2d", "V:iddidd");
        noop("glMapGrid2f", "V:iffiff");
        fn("glGetMapdv", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetMapfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetMapiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));

        noop("glFogCoordd", "V:d");
        noop("glFogCoordf", "V:f");
        noop("glFogCoorddv", "V:p");
        noop("glFogCoordfv", "V:p");
        fn("glGetLightfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetLightiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetMaterialfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetMaterialiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetPixelMapfv", "V:ip", (ret, args) -> zeroInt(argP(args, 1)));
        fn("glGetPixelMapuiv", "V:ip", (ret, args) -> zeroInt(argP(args, 1)));
        fn("glGetPixelMapusv", "V:ip", (ret, args) -> zeroInt(argP(args, 1)));
        fn("glGetPolygonStipple", "V:p", (ret, args) -> zeroBytes(argP(args, 0), 128));
        fn("glGetTexGendv", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));

        noop("glIndexMask", "V:i");
        noop("glIndexd", "V:d");
        noop("glIndexf", "V:f");
        noop("glIndexi", "V:i");
        noop("glIndexs", "V:s");
        noop("glIndexub", "V:b");
        for (String vector : new String[]{"dv", "fv", "iv", "sv", "ubv"}) {
            noop("glIndex" + vector, "V:p");
        }
        noop("glInitNames", "V:");
        noop("glLoadName", "V:i");
        noop("glPopName", "V:");
        noop("glPushName", "V:i");
        noop("glLineStipple", "V:is");
        noop("glPassThrough", "V:f");
        noop("glPixelMapfv", "V:iip");
        noop("glPixelMapuiv", "V:iip");
        noop("glPixelMapusv", "V:iip");
        noop("glPixelTransferf", "V:if");
        noop("glPixelTransferi", "V:ii");
        noop("glPixelZoom", "V:ff");
        noop("glPolygonStipple", "V:p");
        noop("glPopAttrib", "V:");
        noop("glPopClientAttrib", "V:");
        noop("glPrioritizeTextures", "V:ipp");
        noop("glPushAttrib", "V:i");
        noop("glPushClientAttrib", "V:i");
        noop("glSelectBuffer", "V:ip");

        noop("glRasterPos2d", "V:dd");
        noop("glRasterPos2s", "V:ss");
        noop("glRasterPos3d", "V:ddd");
        noop("glRasterPos3s", "V:sss");
        noop("glRasterPos4d", "V:dddd");
        noop("glRasterPos4s", "V:ssss");
        for (String vector : new String[]{"2dv", "2sv", "3dv", "3sv", "4dv", "4sv"}) {
            noop("glRasterPos" + vector, "V:p");
        }
        noop("glRectd", "V:dddd");
        noop("glRects", "V:ssss");
        noop("glRectdv", "V:pp");
        noop("glRectsv", "V:pp");
        for (String vector : new String[]{"3b", "3d", "3f", "3i", "3s", "3ub", "3ui", "3us"}) {
            String shape = switch (vector) {
                case "3b", "3ub" -> "V:bbb";
                case "3s", "3us" -> "V:sss";
                case "3f" -> "V:fff";
                case "3d" -> "V:ddd";
                default -> "V:iii";
            };
            noop("glSecondaryColor" + vector, shape);
        }
        for (String vector : new String[]{"3bv", "3dv", "3fv", "3iv", "3sv", "3ubv", "3uiv", "3usv"}) {
            noop("glSecondaryColor" + vector, "V:p");
        }
        noop("glTexGend", "V:iid");
        noop("glTexGendv", "V:iip");
        noop("glWindowPos2d", "V:dd");
        noop("glWindowPos2s", "V:ss");
        noop("glWindowPos3d", "V:ddd");
        noop("glWindowPos3s", "V:sss");
        noop("glWindowPos2dv", "V:p");
        noop("glWindowPos2sv", "V:p");
        noop("glWindowPos3dv", "V:p");
        noop("glWindowPos3sv", "V:p");
        noop("glMultiDrawArraysIndirectCount", "V:ippii");
        noop("glMultiDrawElementsIndirectCount", "V:iippii");
        noop("glSpecializeShader", "V:ipipp");
    }

    private static void registerGetters() {
        fn("glGetError", "I:", (ret, args) -> retI(ret, 0));
        fn("glGetString", "P:i", (ret, args) -> retP(ret, glStringAddress(argI(args, 0))));
        fn("glGetStringi", "P:ii", (ret, args) -> retP(ret, pinnedString("")));

        fn("glGetIntegerv", "V:ip", (ret, args) -> {
            int pname = argI(args, 0);
            long params = argP(args, 1);
            if (params == 0) {
                return;
            }
            if (pname == GL_VIEWPORT) {
                ensureViewport();
                for (int i = 0; i < 4; i++) {
                    MemoryUtil.memPutInt(params + 4L * i, VIEWPORT[i]);
                }
                return;
            }
            int count = componentCount(pname);
            for (int i = 0; i < count; i++) {
                MemoryUtil.memPutInt(params + 4L * i, integerValue(pname, i));
            }
        });

        fn("glGetFloatv", "V:ip", (ret, args) -> {
            int pname = argI(args, 0);
            long params = argP(args, 1);
            if (params == 0) {
                return;
            }
            if (pname == GL_VIEWPORT) {
                ensureViewport();
                for (int i = 0; i < 4; i++) {
                    MemoryUtil.memPutFloat(params + 4L * i, VIEWPORT[i]);
                }
            } else if (pname == GL_COLOR_CLEAR_VALUE) {
                for (int i = 0; i < 4; i++) {
                    MemoryUtil.memPutFloat(params + 4L * i, VRenderSystem.clearColor.get(i));
                }
            } else if (pname == GL_LINE_WIDTH) {
                MemoryUtil.memPutFloat(params, 1.0f);
            } else {
                int count = componentCount(pname);
                for (int i = 0; i < count; i++) {
                    MemoryUtil.memPutFloat(params + 4L * i, integerValue(pname, i));
                }
            }
        });

        fn("glGetDoublev", "V:ip", (ret, args) -> {
            int pname = argI(args, 0);
            long params = argP(args, 1);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, integerValue(pname, 0));
            }
        });

        fn("glGetBooleanv", "V:ip", (ret, args) -> {
            int pname = argI(args, 0);
            long params = argP(args, 1);
            if (params == 0) {
                return;
            }
            if (GlIntegerState.isBooleanIntegerState(pname)) {
                int count = componentCount(pname);
                for (int i = 0; i < count; i++) {
                    MemoryUtil.memPutByte(params + i, (byte) (integerValue(pname, i) != 0 ? 1 : 0));
                }
            } else {
                MemoryUtil.memPutByte(params, (byte) (GlCapabilityState.isEnabled(pname) ? 1 : 0));
            }
        });

        fn("glGetInteger64v", "V:ip", (ret, args) -> {
            int pname = argI(args, 0);
            long params = argP(args, 1);
            if (params != 0) {
                MemoryUtil.memPutLong(params, integerValue(pname, 0));
            }
        });

        fn("glGetPointerv", "V:ip", (ret, args) -> {
            long params = argP(args, 1);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });

        fn("glIsEnabled", "I:i", (ret, args) -> retI(ret, GlCapabilityState.isEnabled(argI(args, 0)) ? 1 : 0));
    }

    private static void registerGl11() {
        fn("glEnable", "V:i", (ret, args) -> {
            switch (argI(args, 0)) {
                case GL_DEPTH_TEST -> VRenderSystem.enableDepthTest();
                case GL_STENCIL_TEST -> VRenderSystem.enableStencilTest();
                case GL_CULL_FACE -> VRenderSystem.enableCull();
                case GL_BLEND -> VRenderSystem.enableBlend();
                default -> {
                }
            }
        });
        fn("glDisable", "V:i", (ret, args) -> {
            switch (argI(args, 0)) {
                case GL_DEPTH_TEST -> VRenderSystem.disableDepthTest();
                case GL_STENCIL_TEST -> VRenderSystem.disableStencilTest();
                case GL_CULL_FACE -> VRenderSystem.disableCull();
                case GL_BLEND -> VRenderSystem.disableBlend();
                default -> {
                }
            }
        });

        fn("glGenTextures", "V:ip", (ret, args) -> genLoop(args, GlTexture::genTextureId));
        fn("glDeleteTextures", "V:ip", (ret, args) -> deleteLoop(args, GlTexture::glDeleteTextures));
        fn("glBindTexture", "V:ii", (ret, args) -> GlTexture.bindTexture(argI(args, 1)));
        fn("glIsTexture", "I:i", (ret, args) -> {
            int texture = argI(args, 0);
            retI(ret, texture == 0 || GlTexture.getTexture(texture) != null ? 1 : 0);
        });

        fn("glTexImage2D", "V:iiiiiiiip", (ret, args) -> GlTexture.texImage2D(argI(args, 0), argI(args, 1), argI(args, 2),
                argI(args, 3), argI(args, 4), argI(args, 5), argI(args, 6), argI(args, 7), argP(args, 8)));
        fn("glTexSubImage2D", "V:iiiiiiiip", (ret, args) -> GlTexture.texSubImage2D(argI(args, 0), argI(args, 1), argI(args, 2),
                argI(args, 3), argI(args, 4), argI(args, 5), argI(args, 6), argI(args, 7), argP(args, 8)));
        noop("glTexImage1D", "V:iiiiiiip");
        noop("glTexSubImage1D", "V:iiiiiip");
        noop("glCopyTexImage1D", "V:iiiiiii");
        noop("glCopyTexImage2D", "V:iiiiiiii");
        noop("glCopyTexSubImage1D", "V:iiiiii");
        noop("glCopyTexSubImage2D", "V:iiiiiiii");
        fn("glGetTexImage", "V:iiiip", (ret, args) -> GlTexture.getTexImage(argI(args, 0), argI(args, 1), argI(args, 2),
                argI(args, 3), argP(args, 4)));

        fn("glTexParameteri", "V:iii", (ret, args) -> {
            if (argI(args, 0) == GL_TEXTURE_2D) {
                GlTexture.texParameteri(GL_TEXTURE_2D, argI(args, 1), argI(args, 2));
            }
        });
        noop("glTexParameterf", "V:iif");
        fn("glTexParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (argI(args, 0) == GL_TEXTURE_2D && params != 0) {
                GlTexture.texParameteri(GL_TEXTURE_2D, argI(args, 1), MemoryUtil.memGetInt(params));
            }
        });
        noop("glTexParameterfv", "V:iip");
        fn("glGetTexParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetTexParameterfv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetTexLevelParameteriv", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlTexture.getTexLevelParameter(argI(args, 0), argI(args, 1), argI(args, 2)));
            }
        });
        fn("glGetTexLevelParameterfv", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, GlTexture.getTexLevelParameter(argI(args, 0), argI(args, 1), argI(args, 2)));
            }
        });

        fn("glClear", "V:i", (ret, args) -> VRenderSystem.clear(argI(args, 0)));
        fn("glClearColor", "V:ffff", (ret, args) -> VRenderSystem.setClearColor(argF(args, 0), argF(args, 1), argF(args, 2), argF(args, 3)));
        fn("glClearDepth", "V:d", (ret, args) -> VRenderSystem.clearDepth(argD(args, 0)));
        fn("glClearStencil", "V:i", (ret, args) -> VRenderSystem.clearStencil(argI(args, 0)));
        fn("glColorMask", "V:bbbb", (ret, args) -> VRenderSystem.colorMask(argB(args, 0), argB(args, 1), argB(args, 2), argB(args, 3)));
        fn("glDepthMask", "V:b", (ret, args) -> VRenderSystem.depthMask(argB(args, 0)));
        fn("glDepthFunc", "V:i", (ret, args) -> VRenderSystem.depthFunc(argI(args, 0)));
        noop("glDepthRange", "V:dd");
        fn("glBlendFunc", "V:ii", (ret, args) -> VRenderSystem.blendFunc(argI(args, 0), argI(args, 1)));
        fn("glCullFace", "V:i", (ret, args) -> VRenderSystem.cullFace(argI(args, 0)));
        fn("glFrontFace", "V:i", (ret, args) -> VRenderSystem.frontFace(argI(args, 0)));
        fn("glStencilFunc", "V:iii", (ret, args) -> VRenderSystem.stencilFunc(argI(args, 0), argI(args, 1), argI(args, 2)));
        fn("glStencilMask", "V:i", (ret, args) -> VRenderSystem.stencilMask(argI(args, 0)));
        fn("glStencilOp", "V:iii", (ret, args) -> VRenderSystem.stencilOp(argI(args, 0), argI(args, 1), argI(args, 2)));
        fn("glViewport", "V:iiii", (ret, args) -> {
            VIEWPORT[0] = argI(args, 0);
            VIEWPORT[1] = argI(args, 1);
            VIEWPORT[2] = argI(args, 2);
            VIEWPORT[3] = argI(args, 3);
            Renderer.setViewport(VIEWPORT[0], VIEWPORT[1], VIEWPORT[2], VIEWPORT[3]);
        });
        noop("glScissor", "V:iiii");
        fn("glLineWidth", "V:f", (ret, args) -> VRenderSystem.setLineWidth(argF(args, 0)));
        noop("glPointSize", "V:f");
        fn("glPolygonMode", "V:ii", (ret, args) -> VRenderSystem.setPolygonModeGL(argI(args, 1)));
        noop("glPolygonOffset", "V:ff");
        noop("glLogicOp", "V:i");
        noop("glHint", "V:ii");
        noop("glFinish", "V:");
        noop("glFlush", "V:");
        fn("glPixelStorei", "V:ii", (ret, args) -> GlPixelStore.setInteger(argI(args, 0), argI(args, 1)));
        fn("glPixelStoref", "V:if", (ret, args) -> GlPixelStore.setInteger(argI(args, 0), (int) argF(args, 1)));
        noop("glDrawBuffer", "V:i");
        noop("glReadBuffer", "V:i");
        noop("glReadPixels", "V:iiiiiip");
        fn("glDrawArrays", "V:iii", (ret, args) -> GlDrawContract.drawArrays(argI(args, 0), argI(args, 1), argI(args, 2)));
        fn("glDrawElements", "V:iiip", (ret, args) -> GlDrawContract.drawElements(argI(args, 0), argI(args, 1), argI(args, 2), argP(args, 3)));
    }

    private static void registerGl12to14() {
        fn("glDrawRangeElements", "V:iiiiip", (ret, args) -> GlDrawContract.drawElements(argI(args, 0), argI(args, 3), argI(args, 4), argP(args, 5)));
        noop("glTexImage3D", "V:iiiiiiiiip");
        noop("glTexSubImage3D", "V:iiiiiiiiiip");
        noop("glCopyTexSubImage3D", "V:iiiiiiiii");

        fn("glActiveTexture", "V:i", (ret, args) -> GlTexture.activeTexture(argI(args, 0)));
        noop("glSampleCoverage", "V:fb");
        noop("glCompressedTexImage1D", "V:iiiiiip");
        fn("glCompressedTexImage2D", "V:iiiiiiip", (ret, args) -> {
            int target = argI(args, 0);
            int level = argI(args, 1);
            int internalFormat = argI(args, 2);
            int width = argI(args, 3);
            int height = argI(args, 4);
            int border = argI(args, 5);
            int imageSize = argI(args, 6);
            long data = argP(args, 7);
            java.nio.ByteBuffer dataBuffer = data != 0L && imageSize > 0 ? MemoryUtil.memByteBuffer(data, imageSize) : null;
            GlTexture.compressedTexImage2D(target, level, internalFormat, width, height, border, dataBuffer);
        });
        noop("glCompressedTexImage3D", "V:iiiiiiiip");
        noop("glCompressedTexSubImage1D", "V:iiiiiip");
        noop("glCompressedTexSubImage2D", "V:iiiiiiiip");
        noop("glCompressedTexSubImage3D", "V:iiiiiiiiiip");
        noop("glGetCompressedTexImage", "V:iip");

        fn("glBlendFuncSeparate", "V:iiii", (ret, args) -> VRenderSystem.blendFuncSeparate(argI(args, 0), argI(args, 1), argI(args, 2), argI(args, 3)));
        fn("glBlendEquation", "V:i", (ret, args) -> VRenderSystem.blendEquation(argI(args, 0)));
        fn("glBlendColor", "V:ffff", (ret, args) -> VRenderSystem.blendColor(argF(args, 0), argF(args, 1), argF(args, 2), argF(args, 3)));
        fn("glMultiDrawArrays", "V:ippi", (ret, args) -> {
            int mode = argI(args, 0);
            long first = argP(args, 1);
            long count = argP(args, 2);
            int drawCount = argI(args, 3);
            for (int i = 0; i < drawCount && first != 0 && count != 0; i++) {
                GlDrawContract.drawArrays(mode, MemoryUtil.memGetInt(first + 4L * i), MemoryUtil.memGetInt(count + 4L * i));
            }
        });
        fn("glMultiDrawElements", "V:ipipi", (ret, args) -> {
            int mode = argI(args, 0);
            long count = argP(args, 1);
            int type = argI(args, 2);
            long indices = argP(args, 3);
            int drawCount = argI(args, 4);
            for (int i = 0; i < drawCount && count != 0 && indices != 0; i++) {
                GlDrawContract.drawElements(mode, MemoryUtil.memGetInt(count + 4L * i), type,
                        MemoryUtil.memGetAddress(indices + (long) i * Long.BYTES));
            }
        });
        noop("glPointParameterf", "V:if");
        noop("glPointParameteri", "V:ii");
        noop("glPointParameterfv", "V:ip");
        noop("glPointParameteriv", "V:ip");
    }

    private static void registerGl15() {
        fn("glGenBuffers", "V:ip", (ret, args) -> genLoop(args, GlBuffer::glGenBuffers));
        fn("glDeleteBuffers", "V:ip", (ret, args) -> deleteLoop(args, GlBuffer::glDeleteBuffers));
        fn("glBindBuffer", "V:ii", (ret, args) -> GlBuffer.glBindBuffer(argI(args, 0), argI(args, 1)));
        fn("glIsBuffer", "I:i", (ret, args) -> retI(ret, GlBuffer.glIsBuffer(argI(args, 0)) ? 1 : 0));
        fn("glBufferData", "V:ilpi", (ret, args) -> {
            int target = argI(args, 0);
            long size = argL(args, 1);
            long data = argP(args, 2);
            int usage = argI(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.glBufferData(target, MemoryUtil.memByteBuffer(data, (int) size), usage);
            } else {
                GlBuffer.glBufferData(target, size, usage);
            }
        });
        fn("glBufferSubData", "V:illp", (ret, args) -> {
            long size = argL(args, 2);
            long data = argP(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.glBufferSubData(argI(args, 0), argL(args, 1), MemoryUtil.memByteBuffer(data, (int) size));
            }
        });
        fn("glGetBufferSubData", "V:illp", (ret, args) -> {
            long size = argL(args, 2);
            long data = argP(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.glGetBufferSubData(argI(args, 0), argL(args, 1), MemoryUtil.memByteBuffer(data, (int) size));
            }
        });
        fn("glMapBuffer", "P:ii", (ret, args) -> {
            var mapped = GlBuffer.glMapBuffer(argI(args, 0), argI(args, 1));
            retP(ret, mapped == null ? 0L : MemoryUtil.memAddress(mapped));
        });
        fn("glUnmapBuffer", "I:i", (ret, args) -> retI(ret, GlBuffer.glUnmapBuffer(argI(args, 0)) ? 1 : 0));
        fn("glGetBufferParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetBufferPointerv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });

        fn("glGenQueries", "V:ip", (ret, args) -> genLoop(args, GlQuery::genQueries));
        fn("glDeleteQueries", "V:ip", (ret, args) -> deleteLoop(args, GlQuery::deleteQueries));
        fn("glIsQuery", "I:i", (ret, args) -> retI(ret, GlQuery.isQuery(argI(args, 0)) ? 1 : 0));
        fn("glBeginQuery", "V:ii", (ret, args) -> GlQuery.beginQuery(argI(args, 0), argI(args, 1)));
        fn("glEndQuery", "V:i", (ret, args) -> GlQuery.endQuery(argI(args, 0)));
        fn("glGetQueryiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlQuery.getQueryi(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjectiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, (int) GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjectuiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, (int) GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
    }

    private static void registerGl20() {
        fn("glCreateProgram", "I:", (ret, args) -> retI(ret, GlProgram.createProgram()));
        fn("glDeleteProgram", "V:i", (ret, args) -> GlProgram.deleteProgram(argI(args, 0)));
        fn("glCreateShader", "I:i", (ret, args) -> retI(ret, GlProgram.createShader(argI(args, 0))));
        fn("glDeleteShader", "V:i", (ret, args) -> GlProgram.deleteShader(argI(args, 0)));
        fn("glAttachShader", "V:ii", (ret, args) -> GlProgram.attachShader(argI(args, 0), argI(args, 1)));
        noop("glDetachShader", "V:ii");
        fn("glShaderSource", "V:iipp", (ret, args) -> {
            int shader = argI(args, 0);
            int count = argI(args, 1);
            long strings = argP(args, 2);
            long lengths = argP(args, 3);
            StringBuilder source = new StringBuilder();
            for (int i = 0; i < count && strings != 0; i++) {
                long string = MemoryUtil.memGetAddress(strings + (long) i * Long.BYTES);
                if (string == 0) {
                    continue;
                }
                int length = lengths == 0 ? -1 : MemoryUtil.memGetInt(lengths + 4L * i);
                source.append(length < 0
                        ? MemoryUtil.memUTF8(string)
                        : MemoryUtil.memUTF8(MemoryUtil.memByteBuffer(string, length)));
            }
            GlProgram.shaderSource(shader, source);
        });
        fn("glCompileShader", "V:i", (ret, args) -> GlProgram.compileShader(argI(args, 0)));
        fn("glLinkProgram", "V:i", (ret, args) -> GlProgram.linkProgram(argI(args, 0)));
        noop("glValidateProgram", "V:i");
        fn("glUseProgram", "V:i", (ret, args) -> GlProgram.useProgram(argI(args, 0)));
        fn("glIsProgram", "I:i", (ret, args) -> retI(ret, GlProgram.isProgram(argI(args, 0)) ? 1 : 0));
        fn("glIsShader", "I:i", (ret, args) -> retI(ret, argI(args, 0) > 0 ? 1 : 0));
        fn("glGetShaderiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlProgram.getShaderi(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetProgramiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlProgram.getProgrami(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetShaderInfoLog", "V:iipp", (ret, args) -> {
            String log = GlProgram.getShaderInfoLog(argI(args, 0));
            writeOutString(log == null ? "" : log, argI(args, 1), argP(args, 2), argP(args, 3));
        });
        fn("glGetProgramInfoLog", "V:iipp", (ret, args) -> {
            String log = GlProgram.getProgramInfoLog(argI(args, 0));
            writeOutString(log == null ? "" : log, argI(args, 1), argP(args, 2), argP(args, 3));
        });
        fn("glGetShaderSource", "V:iipp", (ret, args) -> writeOutString("", argI(args, 1), argP(args, 2), argP(args, 3)));
        fn("glGetAttachedShaders", "V:iipp", (ret, args) -> {
            long count = argP(args, 2);
            if (count != 0) {
                MemoryUtil.memPutInt(count, 0);
            }
        });
        fn("glBindAttribLocation", "V:iip", (ret, args) -> {
            long name = argP(args, 2);
            if (name != 0) {
                GlProgram.bindAttribLocation(argI(args, 0), argI(args, 1), MemoryUtil.memUTF8(name));
            }
        });
        fn("glGetAttribLocation", "I:ip", (ret, args) -> {
            long name = argP(args, 1);
            retI(ret, name == 0 ? -1 : GlProgram.getAttribLocation(argI(args, 0), MemoryUtil.memUTF8(name)));
        });
        fn("glGetUniformLocation", "I:ip", (ret, args) -> {
            long name = argP(args, 1);
            retI(ret, name == 0 ? -1 : GlProgram.getUniformLocation(argI(args, 0), MemoryUtil.memUTF8(name)));
        });
        fn("glGetActiveUniform", "V:iiipppp", (ret, args) -> {
            long size = argP(args, 4);
            long type = argP(args, 5);
            if (size != 0) {
                MemoryUtil.memPutInt(size, 0);
            }
            if (type != 0) {
                MemoryUtil.memPutInt(type, 0);
            }
            writeOutString("", argI(args, 2), argP(args, 3), argP(args, 6));
        });
        fn("glGetActiveAttrib", "V:iiipppp", (ret, args) -> {
            long size = argP(args, 4);
            long type = argP(args, 5);
            if (size != 0) {
                MemoryUtil.memPutInt(size, 0);
            }
            if (type != 0) {
                MemoryUtil.memPutInt(type, 0);
            }
            writeOutString("", argI(args, 2), argP(args, 3), argP(args, 6));
        });

        fn("glUniform1i", "V:ii", (ret, args) -> GlProgram.uniform1i(argI(args, 0), argI(args, 1)));
        fn("glUniform1f", "V:if", (ret, args) -> GlProgram.uniform1f(argI(args, 0), argF(args, 1)));
        fn("glUniform2f", "V:iff", (ret, args) -> GlProgram.uniform2f(argI(args, 0), argF(args, 1), argF(args, 2)));
        fn("glUniform3f", "V:ifff", (ret, args) -> GlProgram.uniform3f(argI(args, 0), argF(args, 1), argF(args, 2), argF(args, 3)));
        fn("glUniform4f", "V:iffff", (ret, args) -> GlProgram.uniform4f(argI(args, 0), argF(args, 1), argF(args, 2), argF(args, 3), argF(args, 4)));
        noop("glUniform2i", "V:iii");
        fn("glUniform3i", "V:iiii", (ret, args) -> GlProgram.uniform3i(argI(args, 0), argI(args, 1), argI(args, 2), argI(args, 3)));
        noop("glUniform4i", "V:iiiii");
        fn("glUniform1fv", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform1f(argI(args, 0), MemoryUtil.memGetFloat(value));
            }
        });
        fn("glUniform2fv", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform2f(argI(args, 0), MemoryUtil.memGetFloat(value), MemoryUtil.memGetFloat(value + 4));
            }
        });
        fn("glUniform3fv", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform3f(argI(args, 0), MemoryUtil.memGetFloat(value), MemoryUtil.memGetFloat(value + 4),
                        MemoryUtil.memGetFloat(value + 8));
            }
        });
        fn("glUniform4fv", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform4f(argI(args, 0), MemoryUtil.memGetFloat(value), MemoryUtil.memGetFloat(value + 4),
                        MemoryUtil.memGetFloat(value + 8), MemoryUtil.memGetFloat(value + 12));
            }
        });
        fn("glUniform1iv", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform1i(argI(args, 0), MemoryUtil.memGetInt(value));
            }
        });
        noop("glUniform2iv", "V:iip");
        noop("glUniform3iv", "V:iip");
        noop("glUniform4iv", "V:iip");
        noop("glUniformMatrix2fv", "V:iibp");
        noop("glUniformMatrix3fv", "V:iibp");
        fn("glUniformMatrix4fv", "V:iibp", (ret, args) -> {
            long value = argP(args, 3);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniformMatrix4fv(argI(args, 0), argB(args, 2), MemoryUtil.memFloatBuffer(value, 16));
            }
        });
        fn("glGetUniformfv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetUniformiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });

        noop("glEnableVertexAttribArray", "V:i");
        noop("glDisableVertexAttribArray", "V:i");
        noop("glVertexAttribPointer", "V:iiibip");
        fn("glGetVertexAttribiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetVertexAttribfv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetVertexAttribdv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, 0.0);
            }
        });
        fn("glGetVertexAttribPointerv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });

        noop("glDrawBuffers", "V:ip");
        fn("glBlendEquationSeparate", "V:ii", (ret, args) -> VRenderSystem.blendEquationSeparate(argI(args, 0), argI(args, 1)));
        fn("glStencilOpSeparate", "V:iiii", (ret, args) -> VRenderSystem.stencilOp(argI(args, 1), argI(args, 2), argI(args, 3)));
        fn("glStencilFuncSeparate", "V:iiii", (ret, args) -> VRenderSystem.stencilFunc(argI(args, 1), argI(args, 2), argI(args, 3)));
        fn("glStencilMaskSeparate", "V:ii", (ret, args) -> VRenderSystem.stencilMask(argI(args, 1)));
    }

    private static void registerGl21() {
        noop("glUniformMatrix2x3fv", "V:iibp");
        noop("glUniformMatrix3x2fv", "V:iibp");
        noop("glUniformMatrix2x4fv", "V:iibp");
        noop("glUniformMatrix4x2fv", "V:iibp");
        noop("glUniformMatrix3x4fv", "V:iibp");
        noop("glUniformMatrix4x3fv", "V:iibp");
    }

    private static void registerGl30() {
        fn("glGenFramebuffers", "V:ip", (ret, args) -> genLoop(args, GlFramebuffer::genFramebufferId));
        fn("glDeleteFramebuffers", "V:ip", (ret, args) -> deleteLoop(args, GlFramebuffer::deleteFramebuffer));
        fn("glBindFramebuffer", "V:ii", (ret, args) -> GlFramebuffer.bindFramebuffer(argI(args, 0), argI(args, 1)));
        fn("glIsFramebuffer", "I:i", (ret, args) -> {
            int framebuffer = argI(args, 0);
            retI(ret, framebuffer == 0 || GlFramebuffer.getFramebuffer(framebuffer) != null ? 1 : 0);
        });
        fn("glCheckFramebufferStatus", "I:i", (ret, args) -> retI(ret, GlFramebuffer.glCheckFramebufferStatus(argI(args, 0))));
        fn("glFramebufferTexture2D", "V:iiiii", (ret, args) -> GlFramebuffer.framebufferTexture2D(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3), argI(args, 4)));
        fn("glFramebufferRenderbuffer", "V:iiii", (ret, args) -> GlFramebuffer.framebufferRenderbuffer(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glGetFramebufferAttachmentParameteriv", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlFramebuffer.getFramebufferAttachmentParameteri(argI(args, 0), argI(args, 1), argI(args, 2)));
            }
        });
        noop("glFramebufferTexture1D", "V:iiiii");
        noop("glFramebufferTexture3D", "V:iiiiii");
        noop("glFramebufferTextureLayer", "V:iiiii");
        noop("glBlitFramebuffer", "V:iiiiiiiiii");

        fn("glGenRenderbuffers", "V:ip", (ret, args) -> genLoop(args, GlRenderbuffer::genId));
        fn("glDeleteRenderbuffers", "V:ip", (ret, args) -> deleteLoop(args, GlRenderbuffer::deleteRenderbuffer));
        fn("glBindRenderbuffer", "V:ii", (ret, args) -> GlRenderbuffer.bindRenderbuffer(argI(args, 0), argI(args, 1)));
        fn("glIsRenderbuffer", "I:i", (ret, args) -> {
            int renderbuffer = argI(args, 0);
            retI(ret, renderbuffer == 0 || GlRenderbuffer.getRenderbuffer(renderbuffer) != null ? 1 : 0);
        });
        fn("glRenderbufferStorage", "V:iiii", (ret, args) -> GlRenderbuffer.renderbufferStorage(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glRenderbufferStorageMultisample", "V:iiiii", (ret, args) -> GlRenderbuffer.renderbufferStorage(argI(args, 0), argI(args, 2),
                argI(args, 3), argI(args, 4)));
        fn("glGetRenderbufferParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });

        fn("glGenVertexArrays", "V:ip", (ret, args) -> genLoop(args, GlVertexArray::genVertexArray));
        fn("glDeleteVertexArrays", "V:ip", (ret, args) -> deleteLoop(args, GlVertexArray::deleteVertexArray));
        fn("glBindVertexArray", "V:i", (ret, args) -> GlVertexArray.bindVertexArray(argI(args, 0)));
        fn("glIsVertexArray", "I:i", (ret, args) -> retI(ret, GlVertexArray.isVertexArray(argI(args, 0)) ? 1 : 0));

        fn("glGenerateMipmap", "V:i", (ret, args) -> {
            if (argI(args, 0) == GL_TEXTURE_2D) {
                GlTexture.generateMipmap(GL_TEXTURE_2D);
            }
        });

        fn("glMapBufferRange", "P:illi", (ret, args) -> {
            var mapped = GlBuffer.glMapBufferRange(argI(args, 0), argL(args, 1), argL(args, 2), argI(args, 3));
            retP(ret, mapped == null ? 0L : MemoryUtil.memAddress(mapped));
        });
        noop("glFlushMappedBufferRange", "V:ill");
        fn("glBindBufferBase", "V:iii", (ret, args) -> GlBuffer.glBindBuffer(argI(args, 0), argI(args, 2)));
        fn("glBindBufferRange", "V:iiill", (ret, args) -> GlBuffer.glBindBuffer(argI(args, 0), argI(args, 2)));

        noop("glClearBufferiv", "V:iip");
        noop("glClearBufferuiv", "V:iip");
        noop("glClearBufferfv", "V:iip");
        noop("glClearBufferfi", "V:iifi");
        noop("glColorMaski", "V:ibbbb");
        noop("glEnablei", "V:ii");
        noop("glDisablei", "V:ii");
        fn("glIsEnabledi", "I:ii", (ret, args) -> retI(ret, 0));
        noop("glBindFragDataLocation", "V:iip");
        fn("glGetFragDataLocation", "I:ip", (ret, args) -> retI(ret, 0));
        fn("glGetIntegeri_v", "V:iip", (ret, args) -> {
            long data = argP(args, 2);
            if (data != 0) {
                MemoryUtil.memPutInt(data, 0);
            }
        });
        fn("glGetBooleani_v", "V:iip", (ret, args) -> {
            long data = argP(args, 2);
            if (data != 0) {
                MemoryUtil.memPutByte(data, (byte) 0);
            }
        });
        noop("glBeginConditionalRender", "V:ii");
        noop("glEndConditionalRender", "V:");
        noop("glClampColor", "V:ii");
        noop("glVertexAttribIPointer", "V:iiiip");
        fn("glGetVertexAttribIiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetVertexAttribIuiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetUniformuiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        noop("glTexParameterIiv", "V:iip");
        noop("glTexParameterIuiv", "V:iip");
        fn("glGetTexParameterIiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetTexParameterIuiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        noop("glUniform1ui", "V:ii");
        noop("glUniform2ui", "V:iii");
        noop("glUniform3ui", "V:iiii");
        noop("glUniform4ui", "V:iiiii");
        noop("glUniform1uiv", "V:iip");
        noop("glUniform2uiv", "V:iip");
        noop("glUniform3uiv", "V:iip");
        noop("glUniform4uiv", "V:iip");
        noop("glBeginTransformFeedback", "V:i");
        noop("glEndTransformFeedback", "V:");
        noop("glTransformFeedbackVaryings", "V:iipi");
    }

    private static void registerGl31() {
        noop("glDrawArraysInstanced", "V:iiii");
        noop("glDrawElementsInstanced", "V:iiipi");
        fn("glCopyBufferSubData", "V:iilll", (ret, args) -> GlBuffer.glCopyBufferSubData(argI(args, 0), argI(args, 1),
                argL(args, 2), argL(args, 3), argL(args, 4)));
        fn("glGetUniformBlockIndex", "I:ip", (ret, args) -> retI(ret, GL_INVALID_INDEX));
        noop("glUniformBlockBinding", "V:iii");
        fn("glGetActiveUniformBlockiv", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetActiveUniformBlockName", "V:iiipp", (ret, args) -> writeOutString("", argI(args, 2), argP(args, 3), argP(args, 4)));
        fn("glGetActiveUniformName", "V:iiipp", (ret, args) -> writeOutString("", argI(args, 2), argP(args, 3), argP(args, 4)));
        fn("glGetUniformIndices", "V:iipp", (ret, args) -> {
            int count = argI(args, 1);
            long indices = argP(args, 3);
            for (int i = 0; i < count && indices != 0; i++) {
                MemoryUtil.memPutInt(indices + 4L * i, GL_INVALID_INDEX);
            }
        });
        fn("glGetActiveUniformsiv", "V:iipip", (ret, args) -> {
            int count = argI(args, 1);
            long params = argP(args, 4);
            for (int i = 0; i < count && params != 0; i++) {
                MemoryUtil.memPutInt(params + 4L * i, 0);
            }
        });
        noop("glPrimitiveRestartIndex", "V:i");
        noop("glTexBuffer", "V:iii");
    }

    private static void registerGl32() {
        fn("glFramebufferTexture", "V:iiii", (ret, args) -> GlFramebuffer.framebufferTexture2D(argI(args, 0), argI(args, 1),
                GL_TEXTURE_2D, argI(args, 2), argI(args, 3)));
        fn("glFenceSync", "P:ii", (ret, args) -> retP(ret, GlSync.fenceSync(argI(args, 0), argI(args, 1))));
        fn("glIsSync", "I:p", (ret, args) -> retI(ret, GlSync.isSync(argP(args, 0)) ? 1 : 0));
        fn("glDeleteSync", "V:p", (ret, args) -> GlSync.deleteSync(argP(args, 0)));
        fn("glClientWaitSync", "I:pil", (ret, args) -> retI(ret, GlSync.clientWaitSync(argP(args, 0), argI(args, 1), argL(args, 2))));
        fn("glWaitSync", "V:pil", (ret, args) -> GlSync.waitSync(argP(args, 0), argI(args, 1), argL(args, 2)));
        fn("glGetSynciv", "V:piipp", (ret, args) -> {
            long length = argP(args, 3);
            long values = argP(args, 4);
            if (values != 0) {
                MemoryUtil.memPutInt(values, GlSync.getSynci(argP(args, 0), argI(args, 1)));
            }
            if (length != 0) {
                MemoryUtil.memPutInt(length, 1);
            }
        });
        fn("glDrawElementsBaseVertex", "V:iiipi", (ret, args) -> {
            if (argI(args, 4) == 0) {
                GlDrawContract.drawElements(argI(args, 0), argI(args, 1), argI(args, 2), argP(args, 3));
            }
        });
        noop("glDrawRangeElementsBaseVertex", "V:iiiiipi");
        noop("glDrawElementsInstancedBaseVertex", "V:iiipii");
        noop("glMultiDrawElementsBaseVertex", "V:ipipip");
        fn("glGetBufferParameteri64v", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutLong(params, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetInteger64i_v", "V:iip", (ret, args) -> {
            long data = argP(args, 2);
            if (data != 0) {
                MemoryUtil.memPutLong(data, 0L);
            }
        });
        fn("glGetMultisamplefv", "V:iip", (ret, args) -> {
            long val = argP(args, 2);
            if (val != 0) {
                MemoryUtil.memPutFloat(val, 0.0f);
                MemoryUtil.memPutFloat(val + 4, 0.0f);
            }
        });
        noop("glSampleMaski", "V:ii");
        noop("glProvokingVertex", "V:i");
        noop("glTexImage2DMultisample", "V:iiiiib");
        noop("glTexImage3DMultisample", "V:iiiiiib");
    }

    private static void registerGl33() {
        fn("glGenSamplers", "V:ip", (ret, args) -> genLoop(args, GlSampler::genSamplers));
        fn("glDeleteSamplers", "V:ip", (ret, args) -> deleteLoop(args, GlSampler::deleteSamplers));
        fn("glIsSampler", "I:i", (ret, args) -> retI(ret, GlSampler.isSampler(argI(args, 0)) ? 1 : 0));
        fn("glBindSampler", "V:ii", (ret, args) -> GlSampler.bindSampler(argI(args, 0), argI(args, 1)));
        fn("glSamplerParameteri", "V:iii", (ret, args) -> GlSampler.samplerParameteri(argI(args, 0), argI(args, 1), argI(args, 2)));
        fn("glSamplerParameterf", "V:iif", (ret, args) -> GlSampler.samplerParameterf(argI(args, 0), argI(args, 1), argF(args, 2)));
        fn("glSamplerParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                GlSampler.samplerParameteri(argI(args, 0), argI(args, 1), MemoryUtil.memGetInt(params));
            }
        });
        fn("glSamplerParameterfv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                GlSampler.samplerParameterf(argI(args, 0), argI(args, 1), MemoryUtil.memGetFloat(params));
            }
        });
        noop("glSamplerParameterIiv", "V:iip");
        noop("glSamplerParameterIuiv", "V:iip");
        fn("glGetSamplerParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlSampler.getSamplerParameteri(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetSamplerParameteri", "I:ii", (ret, args) ->
                retI(ret, GlSampler.getSamplerParameteri(argI(args, 0), argI(args, 1))));
        fn("glGetSamplerParameterfv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, GlSampler.getSamplerParameterf(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetSamplerParameterf", "F:ii", (ret, args) ->
                retF(ret, GlSampler.getSamplerParameterf(argI(args, 0), argI(args, 1))));
        fn("glGetSamplerParameterIiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetSamplerParameterIi", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetSamplerParameterIuiv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glGetSamplerParameterIui", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glQueryCounter", "V:ii", (ret, args) -> GlQuery.queryCounter(argI(args, 0), argI(args, 1)));
        fn("glGetQueryObjecti64v", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutLong(params, GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjecti64", "L:ii", (ret, args) ->
                retL(ret, GlQuery.getQueryObject(argI(args, 0), argI(args, 1))));
        fn("glGetQueryObjectui64v", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutLong(params, GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjectui64", "L:ii", (ret, args) ->
                retL(ret, GlQuery.getQueryObject(argI(args, 0), argI(args, 1))));
        for (String name : new String[]{"glColorP3ui", "glColorP4ui", "glNormalP3ui",
                "glSecondaryColorP3ui", "glTexCoordP1ui", "glTexCoordP2ui", "glTexCoordP3ui",
                "glTexCoordP4ui", "glVertexP2ui", "glVertexP3ui", "glVertexP4ui"}) {
            noop(name, "V:ii");
        }
        for (String name : new String[]{"glColorP3uiv", "glColorP4uiv", "glNormalP3uiv",
                "glSecondaryColorP3uiv", "glTexCoordP1uiv", "glTexCoordP2uiv", "glTexCoordP3uiv",
                "glTexCoordP4uiv", "glVertexP2uiv", "glVertexP3uiv", "glVertexP4uiv"}) {
            noop(name, "V:ip");
        }
        for (String name : new String[]{"glMultiTexCoordP1ui", "glMultiTexCoordP2ui",
                "glMultiTexCoordP3ui", "glMultiTexCoordP4ui"}) {
            noop(name, "V:iii");
        }
        for (String name : new String[]{"glMultiTexCoordP1uiv", "glMultiTexCoordP2uiv",
                "glMultiTexCoordP3uiv", "glMultiTexCoordP4uiv"}) {
            noop(name, "V:iip");
        }

        noop("glVertexAttribDivisor", "V:ii");

        noop("glVertexAttribLPointer", "V:iiiip");
        noop("glBindFragDataLocationIndexed", "V:iiip");
        fn("glGetFragDataIndex", "I:ip", (ret, args) -> retI(ret, -1));
    }

    private static void registerGl4x() {
        fn("glBlendFunci", "V:iii", (ret, args) -> {
            if (argI(args, 0) == 0) {
                VRenderSystem.blendFunc(argI(args, 1), argI(args, 2));
            }
        });
        fn("glBlendFuncSeparatei", "V:iiiii", (ret, args) -> {
            if (argI(args, 0) == 0) {
                VRenderSystem.blendFuncSeparate(argI(args, 1), argI(args, 2), argI(args, 3), argI(args, 4));
            }
        });
        noop("glBlendEquationi", "V:ii");
        noop("glBlendEquationSeparatei", "V:iii");
        noop("glMinSampleShading", "V:f");
        noop("glPatchParameteri", "V:ii");
        noop("glPatchParameterfv", "V:ip");
        noop("glDrawArraysIndirect", "V:ip");
        noop("glDrawElementsIndirect", "V:iip");

        fn("glClearDepthf", "V:f", (ret, args) -> VRenderSystem.clearDepth(argF(args, 0)));
        noop("glDepthRangef", "V:ff");
        noop("glProgramParameteri", "V:iii");
        noop("glReleaseShaderCompiler", "V:");
        fn("glGetShaderPrecisionFormat", "V:iipp", (ret, args) -> {
            long range = argP(args, 2);
            if (range != 0) {
                MemoryUtil.memPutInt(range, 0);
                MemoryUtil.memPutInt(range + Integer.BYTES, 0);
            }
            zeroInt(argP(args, 3));
        });

        noop("glUniform1d", "V:id");
        noop("glUniform2d", "V:idd");
        noop("glUniform3d", "V:iddd");
        noop("glUniform4d", "V:idddd");
        for (String vector : new String[]{"1dv", "2dv", "3dv", "4dv"}) {
            noop("glUniform" + vector, "V:iip");
        }
        for (String matrix : new String[]{"2dv", "3dv", "4dv", "2x3dv", "3x2dv", "2x4dv", "4x2dv", "3x4dv", "4x3dv"}) {
            noop("glUniformMatrix" + matrix, "V:iibp");
        }

        noop("glProgramUniform1i", "V:iii");
        noop("glProgramUniform2i", "V:iiii");
        noop("glProgramUniform3i", "V:iiiii");
        noop("glProgramUniform4i", "V:iiiiii");
        noop("glProgramUniform1f", "V:iif");
        noop("glProgramUniform2f", "V:iiff");
        noop("glProgramUniform3f", "V:iifff");
        noop("glProgramUniform4f", "V:iiffff");
        noop("glProgramUniform1ui", "V:iii");
        noop("glProgramUniform2ui", "V:iiii");
        noop("glProgramUniform3ui", "V:iiiii");
        noop("glProgramUniform4ui", "V:iiiiii");
        noop("glProgramUniform1d", "V:iid");
        noop("glProgramUniform2d", "V:iidd");
        noop("glProgramUniform3d", "V:iiddd");
        noop("glProgramUniform4d", "V:iidddd");
        for (String vector : new String[]{"1iv", "2iv", "3iv", "4iv", "1fv", "2fv", "3fv", "4fv",
                "1uiv", "2uiv", "3uiv", "4uiv", "1dv", "2dv", "3dv", "4dv"}) {
            noop("glProgramUniform" + vector, "V:iiip");
        }
        for (String matrix : new String[]{"2fv", "3fv", "4fv", "2x3fv", "3x2fv", "2x4fv", "4x2fv", "3x4fv", "4x3fv",
                "2dv", "3dv", "4dv", "2x3dv", "3x2dv", "2x4dv", "4x2dv", "3x4dv", "4x3dv"}) {
            noop("glProgramUniformMatrix" + matrix, "V:iiibp");
        }
        fn("glGetUniformd", "d:ii", (ret, args) -> retD(ret, 0.0));
        fn("glGetUniformdv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, 0.0);
            }
        });
        noop("glVertexAttribL1d", "V:id");
        noop("glVertexAttribL2d", "V:idd");
        noop("glVertexAttribL3d", "V:iddd");
        noop("glVertexAttribL4d", "V:idddd");
        noop("glVertexAttribL1dv", "V:ip");
        noop("glVertexAttribL2dv", "V:ip");
        noop("glVertexAttribL3dv", "V:ip");
        noop("glVertexAttribL4dv", "V:ip");
        fn("glGetVertexAttribLdv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, 0.0);
            }
        });
        noop("glShaderStorageBlockBinding", "V:iii");
        noop("glMultiDrawElementsIndirect", "V:iipii");

        noop("glTexStorage1D", "V:iiii");
        fn("glTexStorage2D", "V:iiiii", (ret, args) -> {
            if (argI(args, 0) == GL_TEXTURE_2D) {
                GlTexture.texImage2D(GL_TEXTURE_2D, 0, argI(args, 2), argI(args, 3), argI(args, 4), 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
            }
        });
        noop("glTexStorage3D", "V:iiiiii");
        noop("glBindImageTexture", "V:iiibiii");
        noop("glMemoryBarrier", "V:i");
        noop("glDrawArraysInstancedBaseInstance", "V:iiiii");

        noop("glDebugMessageCallback", "V:pp");
        noop("glDebugMessageControl", "V:iiiipb");
        noop("glDebugMessageInsert", "V:iiiiip");
        noop("glObjectLabel", "V:iiip");
        fn("glGetObjectLabel", "V:iiipp", (ret, args) -> writeOutString("", argI(args, 2), argP(args, 3), argP(args, 4)));
        noop("glPushDebugGroup", "V:iiip");
        noop("glPopDebugGroup", "V:");
        noop("glInvalidateFramebuffer", "V:iip");
        noop("glCopyImageSubData", "V:iiiiiiiiiiiiiii");
        noop("glDispatchCompute", "V:iii");
        noop("glFramebufferParameteri", "V:iii");
        fn("glGetFramebufferParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        noop("glTexStorage2DMultisample", "V:iiiiib");

        fn("glBufferStorage", "V:ilpi", (ret, args) -> {
            int target = argI(args, 0);
            long size = argL(args, 1);
            long data = argP(args, 2);
            if (data != 0 && size > 0) {
                GlBuffer.glBufferData(target, MemoryUtil.memByteBuffer(data, (int) size), GL_STATIC_DRAW);
            } else {
                GlBuffer.glBufferData(target, size, GL_STATIC_DRAW);
            }
        });
        noop("glBindBuffersBase", "V:iiip");
        noop("glBindTextures", "V:iip");
        noop("glBindSamplers", "V:iip");
        noop("glBindImageTextures", "V:iip");
        noop("glClearTexImage", "V:iiiip");
    }

    private static void registerModernControlPlane() {
        fn("glGenTransformFeedbacks", "V:ip", (ret, args) -> genLoop(args, GL40M::glGenTransformFeedbacks));
        fn("glDeleteTransformFeedbacks", "V:ip", (ret, args) -> deleteLoop(args, GL40M::glDeleteTransformFeedbacks));
        fn("glBindTransformFeedback", "V:ii", (ret, args) -> GL40M.glBindTransformFeedback(argI(args, 0), argI(args, 1)));
        fn("glIsTransformFeedback", "I:i", (ret, args) -> retI(ret, GL40M.glIsTransformFeedback(argI(args, 0)) ? 1 : 0));
        noop("glPauseTransformFeedback", "V:");
        noop("glResumeTransformFeedback", "V:");
        noop("glDrawTransformFeedback", "V:ii");
        noop("glDrawTransformFeedbackStream", "V:iii");

        noop("glBeginQueryIndexed", "V:iii");
        noop("glEndQueryIndexed", "V:ii");
        fn("glGetQueryIndexediv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetQueryIndexedi", "I:iii", (ret, args) -> retI(ret, 0));

        fn("glGetProgramStageiv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetProgramStagei", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetSubroutineIndex", "I:iip", (ret, args) -> retI(ret, GL_INVALID_INDEX));
        fn("glGetSubroutineUniformLocation", "I:iip", (ret, args) -> retI(ret, -1));
        noop("glUniformSubroutinesuiv", "V:iip");
        noop("glUniformSubroutinesui", "V:ii");
        fn("glGetUniformSubroutineuiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetUniformSubroutineui", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetActiveSubroutineUniformiv", "V:iiiip", (ret, args) -> zeroInt(argP(args, 4)));
        fn("glGetActiveSubroutineUniformi", "I:iiii", (ret, args) -> retI(ret, 0));
        fn("glGetActiveSubroutineUniformName", "V:iiiipp", (ret, args) ->
                writeOutString("", argI(args, 3), argP(args, 4), argP(args, 5)));
        fn("glGetActiveSubroutineName", "V:iiiipp", (ret, args) ->
                writeOutString("", argI(args, 3), argP(args, 4), argP(args, 5)));

        fn("glGenProgramPipelines", "V:ip", (ret, args) -> genLoop(args, GL41M::glGenProgramPipelines));
        fn("glDeleteProgramPipelines", "V:ip", (ret, args) -> deleteLoop(args, GL41M::glDeleteProgramPipelines));
        noop("glBindProgramPipeline", "V:i");
        fn("glIsProgramPipeline", "I:i", (ret, args) -> retI(ret, GL41M.glIsProgramPipeline(argI(args, 0)) ? 1 : 0));
        noop("glUseProgramStages", "V:iii");
        noop("glActiveShaderProgram", "V:ii");
        noop("glValidateProgramPipeline", "V:i");
        fn("glGetProgramPipelineiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetProgramPipelinei", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetProgramPipelineInfoLog", "V:iipp", (ret, args) ->
                writeOutString("", argI(args, 1), argP(args, 2), argP(args, 3)));
        fn("glCreateShaderProgramv", "I:iip", (ret, args) -> retI(ret, GlProgram.createProgram()));
        noop("glProgramBinary", "V:iipi");
        fn("glGetProgramBinary", "V:iippp", (ret, args) -> {
            zeroInt(argP(args, 2));
            zeroInt(argP(args, 3));
        });
        noop("glShaderBinary", "V:ipipi");

        noop("glViewportArrayv", "V:iip");
        noop("glViewportIndexedf", "V:iffff");
        noop("glViewportIndexedfv", "V:ip");
        noop("glScissorArrayv", "V:iip");
        noop("glScissorIndexed", "V:iiiii");
        noop("glScissorIndexedv", "V:ip");
        noop("glDepthRangeArrayv", "V:iip");
        noop("glDepthRangeIndexed", "V:idd");
        fn("glGetFloati_v", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetFloati", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetDoublei_v", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, 0.0);
            }
        });
        fn("glGetDoublei", "d:ii", (ret, args) -> retD(ret, 0.0));
    }

    private static void registerModernUtilitySurface() {
        noop("glDrawElementsInstancedBaseInstance", "V:iiipii");
        noop("glDrawElementsInstancedBaseVertexBaseInstance", "V:iiipiii");
        noop("glDrawTransformFeedbackInstanced", "V:iii");
        noop("glDrawTransformFeedbackStreamInstanced", "V:iiii");
        fn("glGetActiveAtomicCounterBufferiv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetActiveAtomicCounterBufferi", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetInternalformativ", "V:iiiip", (ret, args) -> zeroInt(argP(args, 4)));
        fn("glGetInternalformati", "I:iii", (ret, args) -> retI(ret, 0));

        noop("glBindVertexBuffer", "V:iili");
        noop("glClearBufferData", "V:iiiip");
        noop("glClearBufferSubData", "V:iilliip");
        noop("glDispatchComputeIndirect", "V:l");
        fn("glGetDebugMessageLog", "I:iipppppp", (ret, args) -> retI(ret, 0));
        fn("glGetFramebufferParameteri", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetInternalformati64", "L:iii", (ret, args) -> retL(ret, 0L));
        fn("glGetInternalformati64v", "V:iiiip", (ret, args) -> zeroLong(argP(args, 4)));
        noop("glObjectPtrLabel", "V:pip");
        fn("glGetObjectPtrLabel", "V:pipp", (ret, args) ->
                writeOutString("", argI(args, 1), argP(args, 2), argP(args, 3)));
        fn("glGetProgramInterfacei", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetProgramInterfaceiv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetProgramResourceIndex", "I:iip", (ret, args) -> retI(ret, GL_INVALID_INDEX));
        fn("glGetProgramResourceLocation", "I:iip", (ret, args) -> retI(ret, -1));
        fn("glGetProgramResourceLocationIndex", "I:iip", (ret, args) -> retI(ret, -1));
        fn("glGetProgramResourceName", "V:iiiipp", (ret, args) ->
                writeOutString("", argI(args, 3), argP(args, 4), argP(args, 5)));
        fn("glGetProgramResourceiv", "V:iiiipipp", (ret, args) -> {
            zeroInt(argP(args, 6));
            zeroInt(argP(args, 7));
        });
        noop("glInvalidateBufferData", "V:i");
        noop("glInvalidateBufferSubData", "V:ill");
        noop("glInvalidateSubFramebuffer", "V:iipiiii");
        noop("glInvalidateTexImage", "V:ii");
        noop("glInvalidateTexSubImage", "V:iiiiiiii");
        noop("glMultiDrawArraysIndirect", "V:ipii");
        noop("glMultiDrawElementsIndirect", "V:iipii");
        noop("glTexBufferRange", "V:iiill");
        noop("glTexStorage3DMultisample", "V:iiiiiib");
        noop("glTextureView", "V:iiiiiiii");
        noop("glVertexAttribBinding", "V:ii");
        noop("glVertexAttribFormat", "V:iiibi");
        noop("glVertexAttribIFormat", "V:iiii");
        noop("glVertexAttribLFormat", "V:iiii");
        noop("glVertexBindingDivisor", "V:ii");

        noop("glBindBuffersRange", "V:iiippp");
        noop("glBindVertexBuffers", "V:iippp");
        noop("glClearTexSubImage", "V:iiiiiiiiiip");
    }

    private static void registerDirectStateAccess() {
        fn("glCreateBuffers", "V:ip", (ret, args) -> genLoop(args, GlBuffer::glGenBuffers));
        fn("glCreateTextures", "V:iip", (ret, args) -> {
            int n = argI(args, 1);
            long ptr = argP(args, 2);
            for (int i = 0; i < n && ptr != 0; i++) {
                MemoryUtil.memPutInt(ptr + 4L * i, GlTexture.genTextureId());
            }
        });
        fn("glCreateFramebuffers", "V:ip", (ret, args) -> genLoop(args, GlFramebuffer::genFramebufferId));
        fn("glCreateRenderbuffers", "V:ip", (ret, args) -> genLoop(args, GlRenderbuffer::genId));
        fn("glCreateVertexArrays", "V:ip", (ret, args) -> genLoop(args, GlVertexArray::genVertexArray));
        fn("glCreateSamplers", "V:ip", (ret, args) -> genLoop(args, GlSampler::genSamplers));
        fn("glCreateQueries", "V:iip", (ret, args) -> {
            int n = argI(args, 1);
            long ptr = argP(args, 2);
            for (int i = 0; i < n && ptr != 0; i++) {
                MemoryUtil.memPutInt(ptr + 4L * i, GlQuery.genQueries());
            }
        });
        fn("glCreateProgramPipelines", "V:ip", (ret, args) -> genLoop(args, GL41M::glGenProgramPipelines));
        fn("glCreateTransformFeedbacks", "V:ip", (ret, args) -> genLoop(args, GL40M::glGenTransformFeedbacks));

        fn("glNamedBufferData", "V:ilpi", (ret, args) -> {
            int buffer = argI(args, 0);
            long size = argL(args, 1);
            long data = argP(args, 2);
            int usage = argI(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.namedBufferData(buffer, MemoryUtil.memByteBuffer(data, (int) size), usage);
            } else {
                GlBuffer.namedBufferData(buffer, size, usage);
            }
        });
        fn("glNamedBufferStorage", "V:ilpi", (ret, args) -> {
            int buffer = argI(args, 0);
            long size = argL(args, 1);
            long data = argP(args, 2);
            if (data != 0 && size > 0) {
                GlBuffer.namedBufferData(buffer, MemoryUtil.memByteBuffer(data, (int) size), GL_STATIC_DRAW);
            } else {
                GlBuffer.namedBufferData(buffer, size, GL_STATIC_DRAW);
            }
        });
        fn("glNamedBufferSubData", "V:illp", (ret, args) -> {
            long size = argL(args, 2);
            long data = argP(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.namedBufferSubData(argI(args, 0), argL(args, 1), MemoryUtil.memByteBuffer(data, (int) size));
            }
        });
        fn("glMapNamedBuffer", "P:ii", (ret, args) -> {
            var mapped = GlBuffer.mapNamedBuffer(argI(args, 0));
            retP(ret, mapped == null ? 0L : MemoryUtil.memAddress(mapped));
        });
        fn("glUnmapNamedBuffer", "I:i", (ret, args) -> retI(ret, GlBuffer.unmapNamedBuffer(argI(args, 0)) ? 1 : 0));
        fn("glGetTextureParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, 0);
            }
        });
        fn("glMapNamedBufferRange", "P:illi", (ret, args) -> {
            var mapped = GlBuffer.mapNamedBufferRange(argI(args, 0), argL(args, 1), argL(args, 2));
            retP(ret, mapped == null ? 0L : MemoryUtil.memAddress(mapped));
        });
        noop("glFlushMappedNamedBufferRange", "V:ill");
        fn("glCopyNamedBufferSubData", "V:iilll", (ret, args) -> GlBuffer.copyNamedBufferSubData(argI(args, 0),
                argI(args, 1), argL(args, 2), argL(args, 3), argL(args, 4)));
        noop("glClearNamedBufferData", "V:iiiip");
        noop("glClearNamedBufferSubData", "V:iilliip");
        fn("glGetNamedBufferSubData", "V:illp", (ret, args) -> {
            var mapped = GlBuffer.mapNamedBuffer(argI(args, 0));
            long ptr = argP(args, 3);
            long size = argL(args, 2);
            if (mapped == null || ptr == 0 || size <= 0) {
                return;
            }

            int offset = (int) Math.max(0L, argL(args, 1));
            int length = (int) Math.min(size, Math.max(mapped.capacity() - offset, 0));
            if (length <= 0) {
                return;
            }

            ByteBuffer src = mapped.duplicate();
            src.position(offset);
            src.limit(offset + length);
            MemoryUtil.memCopy(MemoryUtil.memAddress(src), ptr, length);
        });
        fn("glGetNamedBufferParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetNamedBufferParameteri", "I:ii", (ret, args) -> retI(ret, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1))));
        fn("glGetNamedBufferParameteri64v", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutLong(params, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetNamedBufferParameteri64", "L:ii", (ret, args) -> retL(ret, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1))));
        fn("glGetNamedBufferPointerv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });
        fn("glGetNamedBufferPointer", "P:ii", (ret, args) -> retP(ret, 0L));

        fn("glBindTextureUnit", "V:ii", (ret, args) -> {
            int previous = GlTexture.getActiveTexture();
            GlTexture.activeTexture(GL_TEXTURE0 + argI(args, 0));
            GlTexture.bindTexture(argI(args, 1));
            GlTexture.activeTexture(previous);
        });
        fn("glTextureParameteri", "V:iii", (ret, args) -> {
            int pname = argI(args, 1);
            int param = argI(args, 2);
            withTexture(argI(args, 0), () -> GlTexture.texParameteri(GL_TEXTURE_2D, pname, param));
        });
        noop("glTextureParameterf", "V:iif");
        fn("glTextureParameteriv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                int texture = argI(args, 0);
                int pname = argI(args, 1);
                int param = MemoryUtil.memGetInt(params);
                withTexture(texture, () -> GlTexture.texParameteri(GL_TEXTURE_2D, pname, param));
            }
        });
        noop("glTextureParameterfv", "V:iip");
        noop("glTextureParameterIi", "V:iii");
        noop("glTextureParameterIiv", "V:iip");
        noop("glTextureParameterIui", "V:iii");
        noop("glTextureParameterIuiv", "V:iip");
        fn("glGenerateTextureMipmap", "V:i", (ret, args) -> withTexture(argI(args, 0), () -> GlTexture.generateMipmap(GL_TEXTURE_2D)));
        noop("glTextureStorage1D", "V:iiii");
        fn("glTextureStorage2D", "V:iiiii", (ret, args) -> {
            int internalformat = argI(args, 2);
            int width = argI(args, 3);
            int height = argI(args, 4);
            withTexture(argI(args, 0), () -> GlTexture.texImage2D(GL_TEXTURE_2D, 0, internalformat, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L));
        });
        noop("glTextureStorage3D", "V:iiiiii");
        noop("glTextureStorage2DMultisample", "V:iiiiib");
        noop("glTextureStorage3DMultisample", "V:iiiiiib");
        noop("glTextureSubImage1D", "V:iiiiiip");
        fn("glTextureSubImage2D", "V:iiiiiiiip", (ret, args) -> {
            int level = argI(args, 1);
            int xoffset = argI(args, 2);
            int yoffset = argI(args, 3);
            int width = argI(args, 4);
            int height = argI(args, 5);
            int format = argI(args, 6);
            int type = argI(args, 7);
            long pixels = argP(args, 8);
            withTexture(argI(args, 0), () -> GlTexture.texSubImage2D(GL_TEXTURE_2D, level, xoffset, yoffset, width, height, format, type, pixels));
        });
        noop("glTextureSubImage3D", "V:iiiiiiiiiip");
        noop("glCompressedTextureSubImage1D", "V:iiiiiip");
        noop("glCompressedTextureSubImage2D", "V:iiiiiiiip");
        noop("glCompressedTextureSubImage3D", "V:iiiiiiiiiip");
        noop("glCopyTextureSubImage1D", "V:iiiiii");
        noop("glCopyTextureSubImage2D", "V:iiiiiiii");
        noop("glCopyTextureSubImage3D", "V:iiiiiiiii");
        noop("glTextureBuffer", "V:iii");
        noop("glTextureBufferRange", "V:iiill");
        noop("glGetTextureImage", "V:iiiiip");
        noop("glGetCompressedTextureImage", "V:iiip");
        noop("glGetTextureSubImage", "V:iiiiiiiiiiip");
        noop("glGetCompressedTextureSubImage", "V:iiiiiiiiiip");
        fn("glGetTextureParameteriv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTextureParameteri", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureParameterfv", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetTextureParameterf", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetTextureParameterIiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTextureParameterIi", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureParameterIuiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTextureParameterIui", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureLevelParameteriv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetTextureLevelParameteri", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureLevelParameterfv", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetTextureLevelParameterf", "F:iii", (ret, args) -> retF(ret, 0.0f));

        fn("glTextureParameteriEXT", "V:iiii", (ret, args) -> {
            int texture = argI(args, 0);
            int target = argI(args, 1);
            int pname = argI(args, 2);
            int param = argI(args, 3);
            withTexture(texture, () -> GlTexture.texParameteri(target, pname, param));
        });
        noop("glTextureParameterfEXT", "V:iiif");
        fn("glTextureParameterivEXT", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                int texture = argI(args, 0);
                int target = argI(args, 1);
                int pname = argI(args, 2);
                int param = MemoryUtil.memGetInt(params);
                withTexture(texture, () -> GlTexture.texParameteri(target, pname, param));
            }
        });
        noop("glTextureParameterfvEXT", "V:iiip");
        noop("glTextureParameterIivEXT", "V:iiip");
        noop("glTextureParameterIuivEXT", "V:iiip");
        noop("glTextureImage1DEXT", "V:iiiiiiiip");
        fn("glTextureImage2DEXT", "V:iiiiiiiiip", (ret, args) -> {
            int texture = argI(args, 0);
            int target = argI(args, 1);
            int level = argI(args, 2);
            int internalformat = argI(args, 3);
            int width = argI(args, 4);
            int height = argI(args, 5);
            int border = argI(args, 6);
            int format = argI(args, 7);
            int type = argI(args, 8);
            long pixels = argP(args, 9);
            withTexture(texture, () -> GlTexture.texImage2D(target, level, internalformat, width, height, border, format, type, pixels));
        });
        noop("glTextureImage3DEXT", "V:iiiiiiiiiip");
        noop("glTextureSubImage1DEXT", "V:iiiiiiip");
        fn("glTextureSubImage2DEXT", "V:iiiiiiiiip", (ret, args) -> {
            int texture = argI(args, 0);
            int target = argI(args, 1);
            int level = argI(args, 2);
            int xoffset = argI(args, 3);
            int yoffset = argI(args, 4);
            int width = argI(args, 5);
            int height = argI(args, 6);
            int format = argI(args, 7);
            int type = argI(args, 8);
            long pixels = argP(args, 9);
            withTexture(texture, () -> GlTexture.texSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
        });
        noop("glTextureSubImage3DEXT", "V:iiiiiiiiiip");
        noop("glCopyTextureImage1DEXT", "V:iiiiiiii");
        noop("glCopyTextureImage2DEXT", "V:iiiiiiiii");
        noop("glCopyTextureSubImage1DEXT", "V:iiiiiii");
        noop("glCopyTextureSubImage2DEXT", "V:iiiiiiiii");
        noop("glCopyTextureSubImage3DEXT", "V:iiiiiiiiii");
        noop("glCompressedTextureImage1DEXT", "V:iiiiiiip");
        noop("glCompressedTextureImage2DEXT", "V:iiiiiiiip");
        noop("glCompressedTextureImage3DEXT", "V:iiiiiiiiip");
        noop("glCompressedTextureSubImage1DEXT", "V:iiiiiiip");
        noop("glCompressedTextureSubImage2DEXT", "V:iiiiiiiip");
        noop("glCompressedTextureSubImage3DEXT", "V:iiiiiiiiiip");
        noop("glTextureBufferEXT", "V:iiii");
        noop("glTextureRenderbufferEXT", "V:iii");
        fn("glGenerateTextureMipmapEXT", "V:ii", (ret, args) ->
                withTexture(argI(args, 0), () -> GlTexture.generateMipmap(argI(args, 1))));
        fn("glGetTextureImageEXT", "V:iiiiip", (ret, args) -> zeroBytes(argP(args, 5), 1));
        fn("glGetTextureParameterivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetTextureParameteriEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureParameterfvEXT", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetTextureParameterfEXT", "F:iii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetTextureParameterIivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetTextureParameterIiEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureParameterIuivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetTextureParameterIuiEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureLevelParameterivEXT", "V:iiiip", (ret, args) -> zeroInt(argP(args, 4)));
        fn("glGetTextureLevelParameteriEXT", "I:iiii", (ret, args) -> retI(ret, 0));
        fn("glGetTextureLevelParameterfvEXT", "V:iiiip", (ret, args) -> {
            long params = argP(args, 4);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetTextureLevelParameterfEXT", "F:iiii", (ret, args) -> retF(ret, 0.0f));

        noop("glMultiTexCoordPointerEXT", "V:iiiip");
        noop("glMultiTexEnvfEXT", "V:iiif");
        noop("glMultiTexEnvfvEXT", "V:iiip");
        noop("glMultiTexEnviEXT", "V:iiii");
        noop("glMultiTexEnvivEXT", "V:iiip");
        noop("glMultiTexGendEXT", "V:iiid");
        noop("glMultiTexGendvEXT", "V:iiip");
        noop("glMultiTexGenfEXT", "V:iiif");
        noop("glMultiTexGenfvEXT", "V:iiip");
        noop("glMultiTexGeniEXT", "V:iiii");
        noop("glMultiTexGenivEXT", "V:iiip");
        fn("glGetMultiTexEnvfvEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetMultiTexEnvfEXT", "F:iii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetMultiTexEnvivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetMultiTexEnviEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetMultiTexGendvEXT", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));
        fn("glGetMultiTexGendEXT", "d:iii", (ret, args) -> retD(ret, 0.0d));
        fn("glGetMultiTexGenfvEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetMultiTexGenfEXT", "F:iii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetMultiTexGenivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetMultiTexGeniEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glMultiTexParameteriEXT", "V:iiii", (ret, args) -> withTextureUnit(argI(args, 0),
                () -> GlTexture.texParameteri(argI(args, 1), argI(args, 2), argI(args, 3))));
        noop("glMultiTexParameterfEXT", "V:iiif");
        fn("glMultiTexParameterivEXT", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                withTextureUnit(argI(args, 0), () -> GlTexture.texParameteri(argI(args, 1), argI(args, 2), MemoryUtil.memGetInt(params)));
            }
        });
        noop("glMultiTexParameterfvEXT", "V:iiip");
        noop("glMultiTexParameterIivEXT", "V:iiip");
        noop("glMultiTexParameterIuivEXT", "V:iiip");
        noop("glMultiTexImage1DEXT", "V:iiiiiiiip");
        fn("glMultiTexImage2DEXT", "V:iiiiiiiiip", (ret, args) -> withTextureUnit(argI(args, 0),
                () -> GlTexture.texImage2D(argI(args, 1), argI(args, 2), argI(args, 3), argI(args, 4),
                        argI(args, 5), argI(args, 6), argI(args, 7), argI(args, 8), argP(args, 9))));
        noop("glMultiTexImage3DEXT", "V:iiiiiiiiiip");
        noop("glMultiTexSubImage1DEXT", "V:iiiiiiip");
        fn("glMultiTexSubImage2DEXT", "V:iiiiiiiiip", (ret, args) -> withTextureUnit(argI(args, 0),
                () -> GlTexture.texSubImage2D(argI(args, 1), argI(args, 2), argI(args, 3), argI(args, 4),
                        argI(args, 5), argI(args, 6), argI(args, 7), argI(args, 8), argP(args, 9))));
        noop("glMultiTexSubImage3DEXT", "V:iiiiiiiiiip");
        noop("glCopyMultiTexImage1DEXT", "V:iiiiiiii");
        noop("glCopyMultiTexImage2DEXT", "V:iiiiiiiii");
        noop("glCopyMultiTexSubImage1DEXT", "V:iiiiiii");
        noop("glCopyMultiTexSubImage2DEXT", "V:iiiiiiiii");
        noop("glCopyMultiTexSubImage3DEXT", "V:iiiiiiiiii");
        noop("glCompressedMultiTexImage1DEXT", "V:iiiiiiip");
        noop("glCompressedMultiTexImage2DEXT", "V:iiiiiiiip");
        noop("glCompressedMultiTexImage3DEXT", "V:iiiiiiiiip");
        noop("glCompressedMultiTexSubImage1DEXT", "V:iiiiiiip");
        noop("glCompressedMultiTexSubImage2DEXT", "V:iiiiiiiiip");
        noop("glCompressedMultiTexSubImage3DEXT", "V:iiiiiiiiiip");
        noop("glMultiTexBufferEXT", "V:iiii");
        noop("glMultiTexRenderbufferEXT", "V:iii");
        fn("glGenerateMultiTexMipmapEXT", "V:ii", (ret, args) ->
                withTextureUnit(argI(args, 0), () -> GlTexture.generateMipmap(argI(args, 1))));
        fn("glGetMultiTexImageEXT", "V:iiiiip", (ret, args) -> zeroBytes(argP(args, 5), 1));
        fn("glGetCompressedMultiTexImageEXT", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), 1));
        fn("glGetMultiTexParameterivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetMultiTexParameteriEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetMultiTexParameterfvEXT", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetMultiTexParameterfEXT", "F:iii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetMultiTexParameterIivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetMultiTexParameterIiEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetMultiTexParameterIuivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetMultiTexParameterIuiEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetMultiTexLevelParameterivEXT", "V:iiiip", (ret, args) -> zeroInt(argP(args, 4)));
        fn("glGetMultiTexLevelParameteriEXT", "I:iiii", (ret, args) -> retI(ret, 0));
        fn("glGetMultiTexLevelParameterfvEXT", "V:iiiip", (ret, args) -> {
            long params = argP(args, 4);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetMultiTexLevelParameterfEXT", "F:iiii", (ret, args) -> retF(ret, 0.0f));

        fn("glBindMultiTextureEXT", "V:iii", (ret, args) ->
                withTextureUnit(argI(args, 0), () -> GlTexture.bindTexture(argI(args, 2))));
        noop("glClientAttribDefaultEXT", "V:i");
        noop("glPushClientAttribDefaultEXT", "V:i");
        noop("glEnableClientStateIndexedEXT", "V:ii");
        noop("glDisableClientStateIndexedEXT", "V:ii");
        noop("glEnableClientStateiEXT", "V:ii");
        noop("glDisableClientStateiEXT", "V:ii");
        noop("glMatrixLoadfEXT", "V:ip");
        noop("glMatrixLoaddEXT", "V:ip");
        noop("glMatrixMultfEXT", "V:ip");
        noop("glMatrixMultdEXT", "V:ip");
        noop("glMatrixLoadIdentityEXT", "V:i");
        noop("glMatrixRotatefEXT", "V:iffff");
        noop("glMatrixRotatedEXT", "V:idddd");
        noop("glMatrixScalefEXT", "V:ifff");
        noop("glMatrixScaledEXT", "V:iddd");
        noop("glMatrixTranslatefEXT", "V:ifff");
        noop("glMatrixTranslatedEXT", "V:iddd");
        noop("glMatrixOrthoEXT", "V:idddddd");
        noop("glMatrixFrustumEXT", "V:idddddd");
        noop("glMatrixPushEXT", "V:i");
        noop("glMatrixPopEXT", "V:i");
        noop("glMatrixLoadTransposefEXT", "V:ip");
        noop("glMatrixLoadTransposedEXT", "V:ip");
        noop("glMatrixMultTransposefEXT", "V:ip");
        noop("glMatrixMultTransposedEXT", "V:ip");
        fn("glGetFloatIndexedvEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetFloatIndexedEXT", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetDoubleIndexedvEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, 0.0);
            }
        });
        fn("glGetDoubleIndexedEXT", "d:ii", (ret, args) -> retD(ret, 0.0));
        fn("glGetPointerIndexedvEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });
        fn("glGetPointerIndexedEXT", "P:ii", (ret, args) -> retP(ret, 0L));
        fn("glGetFloati_vEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetFloatiEXT", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetDoublei_vEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, 0.0);
            }
        });
        fn("glGetDoubleiEXT", "d:ii", (ret, args) -> retD(ret, 0.0));
        fn("glGetPointeri_vEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });
        fn("glGetPointeriEXT", "P:ii", (ret, args) -> retP(ret, 0L));

        noop("glNamedProgramStringEXT", "V:iiiip");
        noop("glNamedProgramLocalParameter4dEXT", "V:iiidddd");
        noop("glNamedProgramLocalParameter4dvEXT", "V:iiip");
        noop("glNamedProgramLocalParameter4fEXT", "V:iiiffff");
        noop("glNamedProgramLocalParameter4fvEXT", "V:iiip");
        noop("glNamedProgramLocalParameters4fvEXT", "V:iiiip");
        noop("glNamedProgramLocalParameterI4iEXT", "V:iiiiiii");
        noop("glNamedProgramLocalParameterI4ivEXT", "V:iiip");
        noop("glNamedProgramLocalParametersI4ivEXT", "V:iiiip");
        noop("glNamedProgramLocalParameterI4uiEXT", "V:iiiiiii");
        noop("glNamedProgramLocalParameterI4uivEXT", "V:iiip");
        noop("glNamedProgramLocalParametersI4uivEXT", "V:iiiip");
        fn("glGetNamedProgramLocalParameterdvEXT", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));
        fn("glGetNamedProgramLocalParameterfvEXT", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetNamedProgramLocalParameterIivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetNamedProgramLocalParameterIuivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetNamedProgramivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetNamedProgramiEXT", "I:iii", (ret, args) -> retI(ret, 0));
        noop("glGetNamedProgramStringEXT", "V:iiip");
        noop("glProgramUniform1fEXT", "V:iif");
        noop("glProgramUniform2fEXT", "V:iiff");
        noop("glProgramUniform3fEXT", "V:iifff");
        noop("glProgramUniform4fEXT", "V:iiffff");
        noop("glProgramUniform1iEXT", "V:iii");
        noop("glProgramUniform2iEXT", "V:iiii");
        noop("glProgramUniform3iEXT", "V:iiiii");
        noop("glProgramUniform4iEXT", "V:iiiiii");
        noop("glProgramUniform1uiEXT", "V:iii");
        noop("glProgramUniform2uiEXT", "V:iiii");
        noop("glProgramUniform3uiEXT", "V:iiiii");
        noop("glProgramUniform4uiEXT", "V:iiiiii");
        for (String vector : new String[]{"1fv", "2fv", "3fv", "4fv", "1iv", "2iv", "3iv", "4iv",
                "1uiv", "2uiv", "3uiv", "4uiv"}) {
            noop("glProgramUniform" + vector + "EXT", "V:iiip");
        }
        for (String matrix : new String[]{"2fv", "3fv", "4fv", "2x3fv", "3x2fv", "2x4fv",
                "4x2fv", "3x4fv", "4x3fv"}) {
            noop("glProgramUniformMatrix" + matrix + "EXT", "V:iiibp");
        }
        fn("glGetCompressedTextureImageEXT", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), 1));
        noop("glVertexArrayVertexOffsetEXT", "V:iiiiil");
        noop("glVertexArrayColorOffsetEXT", "V:iiiiil");
        noop("glVertexArrayEdgeFlagOffsetEXT", "V:iiil");
        noop("glVertexArrayIndexOffsetEXT", "V:iiiil");
        noop("glVertexArrayNormalOffsetEXT", "V:iiiil");
        noop("glVertexArrayTexCoordOffsetEXT", "V:iiiiil");
        noop("glVertexArrayMultiTexCoordOffsetEXT", "V:iiiiiil");
        noop("glVertexArrayFogCoordOffsetEXT", "V:iiiil");
        noop("glVertexArraySecondaryColorOffsetEXT", "V:iiiiil");
        noop("glVertexArrayVertexAttribOffsetEXT", "V:iiiiibil");
        noop("glVertexArrayVertexAttribIOffsetEXT", "V:iiiiiil");
        noop("glEnableVertexArrayEXT", "V:ii");
        noop("glDisableVertexArrayEXT", "V:ii");
        noop("glEnableVertexArrayAttribEXT", "V:ii");
        noop("glDisableVertexArrayAttribEXT", "V:ii");
        fn("glGetVertexArrayIntegervEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetVertexArrayIntegerEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetVertexArrayPointervEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });
        fn("glGetVertexArrayPointerEXT", "P:ii", (ret, args) -> retP(ret, 0L));
        fn("glGetVertexArrayIntegeri_vEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetVertexArrayIntegeriEXT", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetVertexArrayPointeri_vEXT", "V:iiip", (ret, args) -> {
            long params = argP(args, 3);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });
        fn("glGetVertexArrayPointeriEXT", "P:iii", (ret, args) -> retP(ret, 0L));

        fn("glGetGraphicsResetStatus", "I:", (ret, args) -> retI(ret, 0));

        fn("glCheckNamedFramebufferStatus", "I:ii", (ret, args) -> retI(ret, GL_FRAMEBUFFER_COMPLETE));
        noop("glNamedFramebufferParameteri", "V:iii");
        fn("glNamedFramebufferTexture", "V:iiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glNamedFramebufferTextureLayer", "V:iiiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glNamedFramebufferRenderbuffer", "V:iiii", (ret, args) -> GlFramebuffer.namedFramebufferRenderbuffer(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glNamedRenderbufferStorage", "V:iiii", (ret, args) -> GlRenderbuffer.namedRenderbufferStorage(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glNamedRenderbufferStorageMultisample", "V:iiiii", (ret, args) -> GlRenderbuffer.namedRenderbufferStorageMultisample(argI(args, 0),
                argI(args, 1), argI(args, 2), argI(args, 3), argI(args, 4)));
        noop("glNamedFramebufferDrawBuffers", "V:iip");
        noop("glNamedFramebufferDrawBuffer", "V:ii");
        noop("glNamedFramebufferReadBuffer", "V:ii");
        noop("glBlitNamedFramebuffer", "V:iiiiiiiiiiii");
        noop("glClearNamedFramebufferiv", "V:iiip");
        noop("glClearNamedFramebufferuiv", "V:iiip");
        noop("glClearNamedFramebufferfv", "V:iiip");
        noop("glClearNamedFramebufferfi", "V:iiifi");
        noop("glInvalidateNamedFramebufferData", "V:iip");
        noop("glInvalidateNamedFramebufferSubData", "V:iipiiii");
        fn("glGetNamedFramebufferParameteriv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetNamedFramebufferParameteri", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetNamedFramebufferAttachmentParameteriv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetNamedFramebufferAttachmentParameteri", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetNamedRenderbufferParameteriv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetNamedRenderbufferParameteri", "I:ii", (ret, args) -> retI(ret, 0));

        noop("glTransformFeedbackBufferBase", "V:iii");
        noop("glTransformFeedbackBufferRange", "V:iiill");
        fn("glGetTransformFeedbackiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTransformFeedbacki", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetTransformFeedbacki_v", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetTransformFeedbacki64", "L:iii", (ret, args) -> retL(ret, 0L));
        fn("glGetTransformFeedbacki64_v", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));

        noop("glDisableVertexArrayAttrib", "V:ii");
        noop("glEnableVertexArrayAttrib", "V:ii");
        noop("glVertexArrayElementBuffer", "V:ii");
        noop("glVertexArrayVertexBuffer", "V:iiili");
        noop("glVertexArrayVertexBuffers", "V:iiippp");
        noop("glVertexArrayAttribFormat", "V:iiiibi");
        noop("glVertexArrayAttribIFormat", "V:iiiii");
        noop("glVertexArrayAttribLFormat", "V:iiiii");
        noop("glVertexArrayAttribBinding", "V:iii");
        noop("glVertexArrayBindingDivisor", "V:iii");
        fn("glGetVertexArrayiv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetVertexArrayi", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetVertexArrayIndexediv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetVertexArrayIndexedi", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetVertexArrayIndexed64iv", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));
        fn("glGetVertexArrayIndexed64i", "L:iii", (ret, args) -> retL(ret, 0L));

        fn("glGetQueryBufferObjectiv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetQueryBufferObjectuiv", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetQueryBufferObjecti64v", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));
        fn("glGetQueryBufferObjectui64v", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));

        noop("glMemoryBarrierByRegion", "V:i");
        noop("glTextureBarrier", "V:");
        fn("glGetnMapdv", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnMapd", "d:ii", (ret, args) -> retD(ret, 0.0));
        fn("glGetnMapfv", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnMapf", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetnMapiv", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnMapi", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetnPixelMapfv", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), argI(args, 1)));
        fn("glGetnPixelMapuiv", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), argI(args, 1)));
        fn("glGetnPixelMapusv", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), argI(args, 1)));
        fn("glGetnPolygonStipple", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), argI(args, 0)));
        fn("glGetnTexImage", "V:iiiiip", (ret, args) -> zeroBytes(argP(args, 5), argI(args, 4)));
        fn("glReadnPixels", "V:iiiiiiip", (ret, args) -> zeroBytes(argP(args, 7), argI(args, 6)));
        fn("glGetnColorTable", "V:iiiip", (ret, args) -> zeroBytes(argP(args, 4), argI(args, 3)));
        fn("glGetnConvolutionFilter", "V:iiiip", (ret, args) -> zeroBytes(argP(args, 4), argI(args, 3)));
        fn("glGetnSeparableFilter", "V:iiiipipp", (ret, args) -> {
            zeroBytes(argP(args, 4), argI(args, 3));
            zeroBytes(argP(args, 6), argI(args, 5));
        });
        fn("glGetnHistogram", "V:ibiiip", (ret, args) -> zeroBytes(argP(args, 5), argI(args, 4)));
        fn("glGetnMinmax", "V:ibiiip", (ret, args) -> zeroBytes(argP(args, 5), argI(args, 4)));
        fn("glGetnCompressedTexImage", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformfv", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformf", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetnUniformdv", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformd", "d:ii", (ret, args) -> retD(ret, 0.0));
        fn("glGetnUniformiv", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformi", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetnUniformuiv", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformui", "I:ii", (ret, args) -> retI(ret, 0));
        noop("glClipControl", "V:ii");
    }

    private static void registerExtensionAliases() {
        noop("glDebugMessageCallbackARB", "V:pp");
        noop("glDebugMessageControlARB", "V:iiiipb");
        noop("glDebugMessageInsertARB", "V:iiiiip");
        fn("glGetDebugMessageLogARB", "I:iipppppp", (ret, args) -> retI(ret, 0));

        fn("glActiveTextureARB", "V:i", (ret, args) -> GlTexture.activeTexture(argI(args, 0)));
        noop("glClientActiveTextureARB", "V:i");
        for (int dimension = 1; dimension <= 4; dimension++) {
            noop("glMultiTexCoord" + dimension + "fvARB", "V:ip");
            noop("glMultiTexCoord" + dimension + "svARB", "V:ip");
            noop("glMultiTexCoord" + dimension + "ivARB", "V:ip");
            noop("glMultiTexCoord" + dimension + "dvARB", "V:ip");
        }
        noop("glMultiTexCoord1fARB", "V:if");
        noop("glMultiTexCoord2fARB", "V:iff");
        noop("glMultiTexCoord3fARB", "V:ifff");
        noop("glMultiTexCoord4fARB", "V:iffff");
        noop("glMultiTexCoord1sARB", "V:is");
        noop("glMultiTexCoord2sARB", "V:iss");
        noop("glMultiTexCoord3sARB", "V:isss");
        noop("glMultiTexCoord4sARB", "V:issss");
        noop("glMultiTexCoord1iARB", "V:ii");
        noop("glMultiTexCoord2iARB", "V:iii");
        noop("glMultiTexCoord3iARB", "V:iiii");
        noop("glMultiTexCoord4iARB", "V:iiiii");
        noop("glMultiTexCoord1dARB", "V:id");
        noop("glMultiTexCoord2dARB", "V:idd");
        noop("glMultiTexCoord3dARB", "V:iddd");
        noop("glMultiTexCoord4dARB", "V:idddd");

        fn("glBindBufferARB", "V:ii", (ret, args) -> GlBuffer.glBindBuffer(argI(args, 0), argI(args, 1)));
        fn("glBufferDataARB", "V:ilpi", (ret, args) -> {
            int target = argI(args, 0);
            long size = argL(args, 1);
            long data = argP(args, 2);
            int usage = argI(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.glBufferData(target, MemoryUtil.memByteBuffer(data, (int) size), usage);
            } else {
                GlBuffer.glBufferData(target, size, usage);
            }
        });
        fn("glBufferSubDataARB", "V:illp", (ret, args) -> {
            long size = argL(args, 2);
            long data = argP(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.glBufferSubData(argI(args, 0), argL(args, 1), MemoryUtil.memByteBuffer(data, (int) size));
            }
        });
        fn("glGetBufferSubDataARB", "V:illp", (ret, args) -> {
            long size = argL(args, 2);
            long data = argP(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.glGetBufferSubData(argI(args, 0), argL(args, 1), MemoryUtil.memByteBuffer(data, (int) size));
            }
        });
        fn("glDeleteBuffersARB", "V:ip", (ret, args) -> deleteLoop(args, GlBuffer::glDeleteBuffers));
        fn("glGenBuffersARB", "V:ip", (ret, args) -> genLoop(args, GlBuffer::glGenBuffers));
        fn("glIsBufferARB", "I:i", (ret, args) -> retI(ret, GlBuffer.glIsBuffer(argI(args, 0)) ? 1 : 0));
        fn("glMapBufferARB", "P:ii", (ret, args) -> {
            var mapped = GlBuffer.glMapBuffer(argI(args, 0), argI(args, 1));
            retP(ret, mapped == null ? 0L : MemoryUtil.memAddress(mapped));
        });
        fn("glUnmapBufferARB", "I:i", (ret, args) -> retI(ret, GlBuffer.glUnmapBuffer(argI(args, 0)) ? 1 : 0));
        fn("glGetBufferParameterivARB", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetBufferParameteriARB", "I:ii", (ret, args) -> retI(ret, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1))));
        fn("glGetBufferPointervARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });
        fn("glGetBufferPointerARB", "P:ii", (ret, args) -> retP(ret, 0L));

        fn("glBindFramebufferEXT", "V:ii", (ret, args) -> GlFramebuffer.bindFramebuffer(argI(args, 0), argI(args, 1)));
        fn("glBindRenderbufferEXT", "V:ii", (ret, args) -> GlRenderbuffer.bindRenderbuffer(argI(args, 0), argI(args, 1)));
        fn("glCheckFramebufferStatusEXT", "I:i", (ret, args) -> retI(ret, GlFramebuffer.glCheckFramebufferStatus(argI(args, 0))));
        fn("glDeleteFramebuffersEXT", "V:ip", (ret, args) -> deleteLoop(args, GlFramebuffer::deleteFramebuffer));
        fn("glDeleteRenderbuffersEXT", "V:ip", (ret, args) -> deleteLoop(args, GlRenderbuffer::deleteRenderbuffer));
        fn("glGenFramebuffersEXT", "V:ip", (ret, args) -> genLoop(args, GlFramebuffer::genFramebufferId));
        fn("glGenRenderbuffersEXT", "V:ip", (ret, args) -> genLoop(args, GlRenderbuffer::genId));
        fn("glIsFramebufferEXT", "I:i", (ret, args) -> retI(ret, GlFramebuffer.getFramebuffer(argI(args, 0)) != null ? 1 : 0));
        fn("glIsRenderbufferEXT", "I:i", (ret, args) -> retI(ret, GlRenderbuffer.getRenderbuffer(argI(args, 0)) != null ? 1 : 0));
        fn("glFramebufferRenderbufferEXT", "V:iiii", (ret, args) -> GlFramebuffer.framebufferRenderbuffer(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        noop("glFramebufferTexture1DEXT", "V:iiiii");
        fn("glFramebufferTexture2DEXT", "V:iiiii", (ret, args) -> GlFramebuffer.framebufferTexture2D(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3), argI(args, 4)));
        noop("glFramebufferTexture3DEXT", "V:iiiiii");
        fn("glRenderbufferStorageEXT", "V:iiii", (ret, args) -> GlRenderbuffer.renderbufferStorage(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glGenerateMipmapEXT", "V:i", (ret, args) -> GlTexture.generateMipmap(argI(args, 0)));
        fn("glGetFramebufferAttachmentParameterivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetFramebufferAttachmentParameteriEXT", "I:iii", (ret, args) -> retI(ret, GlFramebuffer.getFramebufferAttachmentParameteri(argI(args, 0),
                argI(args, 1), argI(args, 2))));
        fn("glGetFramebufferAttachmentParameteri", "I:iii", (ret, args) -> retI(ret, GlFramebuffer.getFramebufferAttachmentParameteri(argI(args, 0),
                argI(args, 1), argI(args, 2))));
        fn("glGetRenderbufferParameterivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetRenderbufferParameteriEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetRenderbufferParameteri", "I:ii", (ret, args) -> retI(ret, 0));

        noop("glTextureStorage1DEXT", "V:iiiii");
        noop("glTextureStorage2DEXT", "V:iiiiii");
        noop("glTextureStorage3DEXT", "V:iiiiiii");
        noop("glTextureStorage2DMultisampleEXT", "V:iiiiiib");
        noop("glTextureStorage3DMultisampleEXT", "V:iiiiiiib");

        fn("glCreateShaderObjectARB", "I:i", (ret, args) -> {
            int type = argI(args, 0);
            retI(ret, newArbObject(GlProgram.createShader(type), GL_SHADER_OBJECT_ARB, type));
        });
        fn("glCreateProgramObjectARB", "I:", (ret, args) ->
                retI(ret, newArbObject(GlProgram.createProgram(), GL_PROGRAM_OBJECT_ARB, 0)));
        fn("glDeleteObjectARB", "V:i", (ret, args) -> deleteArbObject(argI(args, 0)));
        fn("glAttachObjectARB", "V:ii", (ret, args) -> GlProgram.attachShader(arbCoreObject(argI(args, 0)), arbCoreObject(argI(args, 1))));
        noop("glDetachObjectARB", "V:ii");
        fn("glShaderSourceARB", "V:iipp", (ret, args) -> GlProgram.shaderSource(arbCoreObject(argI(args, 0)),
                shaderSourceFromPointers(argI(args, 1), argP(args, 2), argP(args, 3))));
        fn("glCompileShaderARB", "V:i", (ret, args) -> GlProgram.compileShader(arbCoreObject(argI(args, 0))));
        fn("glLinkProgramARB", "V:i", (ret, args) -> GlProgram.linkProgram(arbCoreObject(argI(args, 0))));
        fn("glUseProgramObjectARB", "V:i", (ret, args) -> GlProgram.useProgram(arbCoreObject(argI(args, 0))));
        noop("glValidateProgramARB", "V:i");
        fn("glGetHandleARB", "I:i", (ret, args) -> {
            int pname = argI(args, 0);
            int coreProgram = GlProgram.currentProgramId();
            retI(ret, pname == GL_PROGRAM_OBJECT_ARB ? ARB_CORE_PROGRAM_TO_HANDLE.getOrDefault(coreProgram, 0) : 0);
        });
        fn("glGetObjectParameterivARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, getArbObjectParameter(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetObjectParameterfvARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, getArbObjectParameter(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetObjectParameteriARB", "I:ii", (ret, args) -> retI(ret, getArbObjectParameter(argI(args, 0), argI(args, 1))));
        fn("glGetInfoLogARB", "V:iipp", (ret, args) -> writeOutString("", argI(args, 1), argP(args, 2), argP(args, 3)));
        fn("glGetAttachedObjectsARB", "V:iipp", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetUniformLocationARB", "I:ip", (ret, args) -> {
            long name = argP(args, 1);
            retI(ret, name == 0 ? -1 : GlProgram.getUniformLocation(arbCoreObject(argI(args, 0)), MemoryUtil.memUTF8(name)));
        });
        fn("glGetActiveUniformARB", "V:iiipppp", (ret, args) -> {
            zeroInt(argP(args, 4));
            zeroInt(argP(args, 5));
            writeOutString("", argI(args, 2), argP(args, 3), argP(args, 6));
        });
        fn("glGetShaderSourceARB", "V:iipp", (ret, args) -> writeOutString("", argI(args, 1), argP(args, 2), argP(args, 3)));
        fn("glUniform1fARB", "V:if", (ret, args) -> GlProgram.uniform1f(argI(args, 0), argF(args, 1)));
        fn("glUniform2fARB", "V:iff", (ret, args) -> GlProgram.uniform2f(argI(args, 0), argF(args, 1), argF(args, 2)));
        fn("glUniform3fARB", "V:ifff", (ret, args) -> GlProgram.uniform3f(argI(args, 0), argF(args, 1), argF(args, 2), argF(args, 3)));
        fn("glUniform4fARB", "V:iffff", (ret, args) -> GlProgram.uniform4f(argI(args, 0), argF(args, 1), argF(args, 2), argF(args, 3), argF(args, 4)));
        fn("glUniform1iARB", "V:ii", (ret, args) -> GlProgram.uniform1i(argI(args, 0), argI(args, 1)));
        noop("glUniform2iARB", "V:iii");
        fn("glUniform3iARB", "V:iiii", (ret, args) -> GlProgram.uniform3i(argI(args, 0), argI(args, 1), argI(args, 2), argI(args, 3)));
        noop("glUniform4iARB", "V:iiiii");
        fn("glUniform1fvARB", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform1f(argI(args, 0), MemoryUtil.memGetFloat(value));
            }
        });
        fn("glUniform2fvARB", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform2f(argI(args, 0), MemoryUtil.memGetFloat(value), MemoryUtil.memGetFloat(value + 4));
            }
        });
        fn("glUniform3fvARB", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform3f(argI(args, 0), MemoryUtil.memGetFloat(value), MemoryUtil.memGetFloat(value + 4),
                        MemoryUtil.memGetFloat(value + 8));
            }
        });
        fn("glUniform4fvARB", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform4f(argI(args, 0), MemoryUtil.memGetFloat(value), MemoryUtil.memGetFloat(value + 4),
                        MemoryUtil.memGetFloat(value + 8), MemoryUtil.memGetFloat(value + 12));
            }
        });
        fn("glUniform1ivARB", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniform1i(argI(args, 0), MemoryUtil.memGetInt(value));
            }
        });
        noop("glUniform2ivARB", "V:iip");
        noop("glUniform3ivARB", "V:iip");
        noop("glUniform4ivARB", "V:iip");
        noop("glUniformMatrix2fvARB", "V:iibp");
        noop("glUniformMatrix3fvARB", "V:iibp");
        fn("glUniformMatrix4fvARB", "V:iibp", (ret, args) -> {
            long value = argP(args, 3);
            if (argI(args, 1) >= 1 && value != 0) {
                GlProgram.uniformMatrix4fv(argI(args, 0), argB(args, 2), MemoryUtil.memFloatBuffer(value, 16));
            }
        });
        fn("glGetUniformfvARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetUniformfARB", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetUniformivARB", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetUniformiARB", "I:ii", (ret, args) -> retI(ret, 0));

        fn("glGenQueriesARB", "V:ip", (ret, args) -> genLoop(args, GlQuery::genQueries));
        fn("glDeleteQueriesARB", "V:ip", (ret, args) -> deleteLoop(args, GlQuery::deleteQueries));
        fn("glIsQueryARB", "I:i", (ret, args) -> retI(ret, GlQuery.isQuery(argI(args, 0)) ? 1 : 0));
        fn("glBeginQueryARB", "V:ii", (ret, args) -> GlQuery.beginQuery(argI(args, 0), argI(args, 1)));
        fn("glEndQueryARB", "V:i", (ret, args) -> GlQuery.endQuery(argI(args, 0)));
        fn("glGetQueryivARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlQuery.getQueryi(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryiARB", "I:ii", (ret, args) -> retI(ret, GlQuery.getQueryi(argI(args, 0), argI(args, 1))));
        fn("glGetQueryObjectivARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, (int) GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjectiARB", "I:ii", (ret, args) -> retI(ret, (int) GlQuery.getQueryObject(argI(args, 0), argI(args, 1))));
        fn("glGetQueryObjectuivARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, (int) GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjectuiARB", "I:ii", (ret, args) -> retI(ret, (int) GlQuery.getQueryObject(argI(args, 0), argI(args, 1))));
        fn("glGetQueryObjecti64vEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutLong(params, GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjecti64EXT", "L:ii", (ret, args) -> retL(ret, GlQuery.getQueryObject(argI(args, 0), argI(args, 1))));
        fn("glGetQueryObjectui64vEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutLong(params, GlQuery.getQueryObject(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetQueryObjectui64EXT", "L:ii", (ret, args) -> retL(ret, GlQuery.getQueryObject(argI(args, 0), argI(args, 1))));

        fn("glGetGraphicsResetStatusARB", "I:", (ret, args) -> retI(ret, 0));
        fn("glGetnMapdvARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnMapdARB", "d:ii", (ret, args) -> retD(ret, 0.0));
        fn("glGetnMapfvARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnMapfARB", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetnMapivARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnMapiARB", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetnPixelMapfvARB", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), argI(args, 1)));
        fn("glGetnPixelMapuivARB", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), argI(args, 1)));
        fn("glGetnPixelMapusvARB", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), argI(args, 1)));
        fn("glGetnPolygonStippleARB", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), argI(args, 0)));
        fn("glGetnTexImageARB", "V:iiiiip", (ret, args) -> zeroBytes(argP(args, 5), argI(args, 4)));
        fn("glReadnPixelsARB", "V:iiiiiiip", (ret, args) -> zeroBytes(argP(args, 7), argI(args, 6)));
        fn("glGetnColorTableARB", "V:iiiip", (ret, args) -> zeroBytes(argP(args, 4), argI(args, 3)));
        fn("glGetnConvolutionFilterARB", "V:iiiip", (ret, args) -> zeroBytes(argP(args, 4), argI(args, 3)));
        fn("glGetnSeparableFilterARB", "V:iiiipipp", (ret, args) -> {
            zeroBytes(argP(args, 4), argI(args, 3));
            zeroBytes(argP(args, 6), argI(args, 5));
        });
        fn("glGetnHistogramARB", "V:ibiiip", (ret, args) -> zeroBytes(argP(args, 5), argI(args, 4)));
        fn("glGetnMinmaxARB", "V:ibiiip", (ret, args) -> zeroBytes(argP(args, 5), argI(args, 4)));
        fn("glGetnCompressedTexImageARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformfvARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformfARB", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetnUniformdvARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformdARB", "d:ii", (ret, args) -> retD(ret, 0.0));
        fn("glGetnUniformivARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformiARB", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetnUniformuivARB", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), argI(args, 2)));
        fn("glGetnUniformuiARB", "I:ii", (ret, args) -> retI(ret, 0));

        noop("glBlendEquationiARB", "V:ii");
        noop("glBlendEquationSeparateiARB", "V:iii");
        fn("glBlendFunciARB", "V:iii", (ret, args) -> {
            if (argI(args, 0) == 0) {
                VRenderSystem.blendFunc(argI(args, 1), argI(args, 2));
            }
        });
        fn("glBlendFuncSeparateiARB", "V:iiiii", (ret, args) -> {
            if (argI(args, 0) == 0) {
                VRenderSystem.blendFuncSeparate(argI(args, 1), argI(args, 2), argI(args, 3), argI(args, 4));
            }
        });

        noop("glProgramParameteriARB", "V:iii");
        fn("glFramebufferTextureARB", "V:iiii", (ret, args) -> GlFramebuffer.framebufferTexture2D(argI(args, 0), argI(args, 1),
                GL_TEXTURE_2D, argI(args, 2), argI(args, 3)));
        fn("glFramebufferTextureLayerARB", "V:iiiii", (ret, args) -> GlFramebuffer.framebufferTexture2D(argI(args, 0), argI(args, 1),
                GL_TEXTURE_2D, argI(args, 2), argI(args, 3)));
        noop("glFramebufferTextureFaceARB", "V:iiiii");
        noop("glProgramParameteriEXT", "V:iii");
        fn("glFramebufferTextureEXT", "V:iiii", (ret, args) -> GlFramebuffer.framebufferTexture2D(argI(args, 0), argI(args, 1),
                GL_TEXTURE_2D, argI(args, 2), argI(args, 3)));
        fn("glFramebufferTextureLayerEXT", "V:iiiii", (ret, args) -> GlFramebuffer.framebufferTexture2D(argI(args, 0), argI(args, 1),
                GL_TEXTURE_2D, argI(args, 2), argI(args, 3)));
        noop("glFramebufferTextureFaceEXT", "V:iiiii");
        noop("glNamedFramebufferParameteriEXT", "V:iii");
        fn("glGetNamedFramebufferParameterivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetNamedFramebufferParameteriEXT", "I:ii", (ret, args) -> retI(ret, 0));

        fn("glNamedBufferDataEXT", "V:ilpi", (ret, args) -> {
            long size = argL(args, 1);
            long data = argP(args, 2);
            if (data != 0 && size > 0) {
                GlBuffer.namedBufferData(argI(args, 0), MemoryUtil.memByteBuffer(data, (int) size), argI(args, 3));
            } else {
                GlBuffer.namedBufferData(argI(args, 0), size, argI(args, 3));
            }
        });
        fn("glNamedBufferSubDataEXT", "V:illp", (ret, args) -> {
            long size = argL(args, 2);
            long data = argP(args, 3);
            if (data != 0 && size > 0) {
                GlBuffer.namedBufferSubData(argI(args, 0), argL(args, 1), MemoryUtil.memByteBuffer(data, (int) size));
            }
        });
        fn("glMapNamedBufferEXT", "P:ii", (ret, args) -> {
            var mapped = GlBuffer.mapNamedBuffer(argI(args, 0));
            retP(ret, mapped == null ? 0L : MemoryUtil.memAddress(mapped));
        });
        fn("glUnmapNamedBufferEXT", "b:i", (ret, args) -> retB(ret, GlBuffer.unmapNamedBuffer(argI(args, 0))));
        fn("glGetNamedBufferParameterivEXT", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1)));
            }
        });
        fn("glGetNamedBufferParameteriEXT", "I:ii", (ret, args) -> retI(ret, GlBuffer.glGetBufferParameteri(argI(args, 0), argI(args, 1))));
        fn("glGetNamedBufferSubDataEXT", "V:illp", (ret, args) -> {
            var mapped = GlBuffer.mapNamedBuffer(argI(args, 0));
            long ptr = argP(args, 3);
            long size = argL(args, 2);
            if (mapped == null || ptr == 0 || size <= 0) {
                return;
            }

            int offset = (int) Math.max(0L, argL(args, 1));
            int length = (int) Math.min(size, Math.max(mapped.capacity() - offset, 0));
            if (length <= 0) {
                return;
            }

            ByteBuffer src = mapped.duplicate();
            src.position(offset);
            src.limit(offset + length);
            MemoryUtil.memCopy(MemoryUtil.memAddress(src), ptr, length);
        });
        fn("glMapNamedBufferRangeEXT", "P:illi", (ret, args) -> {
            var mapped = GlBuffer.mapNamedBufferRange(argI(args, 0), argL(args, 1), argL(args, 2));
            retP(ret, mapped == null ? 0L : MemoryUtil.memAddress(mapped));
        });
        noop("glFlushMappedNamedBufferRangeEXT", "V:ill");
        fn("glNamedCopyBufferSubDataEXT", "V:iilll", (ret, args) -> GlBuffer.copyNamedBufferSubData(argI(args, 0),
                argI(args, 1), argL(args, 2), argL(args, 3), argL(args, 4)));

        fn("glNamedRenderbufferStorageEXT", "V:iiii", (ret, args) -> GlRenderbuffer.namedRenderbufferStorage(argI(args, 0),
                argI(args, 1), argI(args, 2), argI(args, 3)));
        fn("glGetNamedRenderbufferParameterivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetNamedRenderbufferParameteriEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glNamedRenderbufferStorageMultisampleEXT", "V:iiiii", (ret, args) -> GlRenderbuffer.namedRenderbufferStorageMultisample(argI(args, 0),
                argI(args, 1), argI(args, 2), argI(args, 3), argI(args, 4)));
        noop("glNamedRenderbufferStorageMultisampleCoverageEXT", "V:iiiiii");
        fn("glCheckNamedFramebufferStatusEXT", "I:ii", (ret, args) -> retI(ret, GL_FRAMEBUFFER_COMPLETE));
        fn("glNamedFramebufferTexture1DEXT", "V:iiiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0),
                argI(args, 1), argI(args, 3), argI(args, 4)));
        fn("glNamedFramebufferTexture2DEXT", "V:iiiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0),
                argI(args, 1), argI(args, 3), argI(args, 4)));
        fn("glNamedFramebufferTexture3DEXT", "V:iiiiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0),
                argI(args, 1), argI(args, 3), argI(args, 4)));
        fn("glNamedFramebufferRenderbufferEXT", "V:iiii", (ret, args) -> GlFramebuffer.namedFramebufferRenderbuffer(argI(args, 0),
                argI(args, 1), argI(args, 2), argI(args, 3)));
        fn("glGetNamedFramebufferAttachmentParameterivEXT", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetNamedFramebufferAttachmentParameteriEXT", "I:iii", (ret, args) -> retI(ret, 0));
        noop("glFramebufferDrawBufferEXT", "V:ii");
        noop("glFramebufferDrawBuffersEXT", "V:iip");
        noop("glFramebufferReadBufferEXT", "V:ii");
        fn("glGetFramebufferParameterivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetFramebufferParameteriEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glNamedFramebufferTextureEXT", "V:iiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0),
                argI(args, 1), argI(args, 2), argI(args, 3)));
        fn("glNamedFramebufferTextureLayerEXT", "V:iiiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0),
                argI(args, 1), argI(args, 2), argI(args, 3)));
        fn("glNamedFramebufferTextureFaceEXT", "V:iiiii", (ret, args) -> GlFramebuffer.namedFramebufferTexture(argI(args, 0),
                argI(args, 1), argI(args, 2), argI(args, 3)));

        fn("glGetActiveUniformBlocki", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetActiveUniformsi", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glGetIntegeri", "I:ii", (ret, args) -> retI(ret, 0));

        fn("glBindAttribLocationARB", "V:iip", (ret, args) -> {
            long name = argP(args, 2);
            if (name != 0) {
                GlProgram.bindAttribLocation(argI(args, 0), argI(args, 1), MemoryUtil.memUTF8(name));
            }
        });
        fn("glGetAttribLocationARB", "I:ip", (ret, args) -> {
            long name = argP(args, 1);
            retI(ret, name == 0 ? -1 : GlProgram.getAttribLocation(argI(args, 0), MemoryUtil.memUTF8(name)));
        });
        fn("glGetActiveAttribARB", "V:iiipppp", (ret, args) -> {
            zeroInt(argP(args, 4));
            zeroInt(argP(args, 5));
            writeOutString("", argI(args, 2), argP(args, 3), argP(args, 6));
        });
        noop("glEnableVertexAttribArrayARB", "V:i");
        noop("glDisableVertexAttribArrayARB", "V:i");
        noop("glVertexAttribPointerARB", "V:iiibip");
        fn("glGetVertexAttribivARB", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetVertexAttribiARB", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetVertexAttribfvARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetVertexAttribdvARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutDouble(params, 0.0);
            }
        });
        fn("glGetVertexAttribPointervARB", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutAddress(params, 0L);
            }
        });
        fn("glGetVertexAttribPointerARB", "P:ii", (ret, args) -> retP(ret, 0L));
        noop("glVertexAttrib1fARB", "V:if");
        noop("glVertexAttrib2fARB", "V:iff");
        noop("glVertexAttrib3fARB", "V:ifff");
        noop("glVertexAttrib4fARB", "V:iffff");
        noop("glVertexAttrib1sARB", "V:is");
        noop("glVertexAttrib2sARB", "V:iss");
        noop("glVertexAttrib3sARB", "V:isss");
        noop("glVertexAttrib4sARB", "V:issss");
        noop("glVertexAttrib1dARB", "V:id");
        noop("glVertexAttrib2dARB", "V:idd");
        noop("glVertexAttrib3dARB", "V:iddd");
        noop("glVertexAttrib4dARB", "V:idddd");
        noop("glVertexAttrib4NubARB", "V:ibbbb");
        for (String vector : new String[]{"1fv", "1sv", "1dv", "2fv", "2sv", "2dv", "3fv", "3sv", "3dv",
                "4fv", "4sv", "4dv", "4iv", "4bv", "4ubv", "4usv", "4uiv", "4Nbv", "4Nsv",
                "4Niv", "4Nubv", "4Nusv", "4Nuiv"}) {
            noop("glVertexAttrib" + vector + "ARB", "V:ip");
        }

        noop("glBindProgramARB", "V:ii");
        fn("glGenProgramsARB", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genArbVertexProgram));
        fn("glDeleteProgramsARB", "V:ip", (ret, args) -> {
            int n = argI(args, 0);
            long ptr = argP(args, 1);
            for (int i = 0; i < n && ptr != 0; i++) {
                ARB_VERTEX_PROGRAMS.remove(MemoryUtil.memGetInt(ptr + 4L * i));
            }
        });
        fn("glIsProgramARB", "I:i", (ret, args) -> retI(ret, ARB_VERTEX_PROGRAMS.contains(argI(args, 0)) ? 1 : 0));
        noop("glProgramStringARB", "V:iiip");
        noop("glProgramEnvParameter4dARB", "V:iidddd");
        noop("glProgramLocalParameter4dARB", "V:iidddd");
        noop("glProgramEnvParameter4fARB", "V:iiffff");
        noop("glProgramLocalParameter4fARB", "V:iiffff");
        noop("glProgramEnvParameter4dvARB", "V:iip");
        noop("glProgramLocalParameter4dvARB", "V:iip");
        noop("glProgramEnvParameter4fvARB", "V:iip");
        noop("glProgramLocalParameter4fvARB", "V:iip");
        fn("glGetProgramEnvParameterdvARB", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetProgramLocalParameterdvARB", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetProgramEnvParameterfvARB", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetProgramLocalParameterfvARB", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetProgramivARB", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetProgramiARB", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetProgramStringARB", "V:iip", (ret, args) -> {
        });

        noop("glBindFragDataLocationEXT", "V:iip");
        fn("glGetFragDataLocationEXT", "I:ip", (ret, args) -> retI(ret, 0));
        fn("glGetUniformuivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetUniformuiEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetVertexAttribIivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetVertexAttribIiEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetVertexAttribIuivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetVertexAttribIuiEXT", "I:ii", (ret, args) -> retI(ret, 0));
        noop("glUniform1uiEXT", "V:ii");
        noop("glUniform2uiEXT", "V:iii");
        noop("glUniform3uiEXT", "V:iiii");
        noop("glUniform4uiEXT", "V:iiiii");
        noop("glUniform1uivEXT", "V:iip");
        noop("glUniform2uivEXT", "V:iip");
        noop("glUniform3uivEXT", "V:iip");
        noop("glUniform4uivEXT", "V:iip");
        noop("glVertexAttribI1iEXT", "V:ii");
        noop("glVertexAttribI2iEXT", "V:iii");
        noop("glVertexAttribI3iEXT", "V:iiii");
        noop("glVertexAttribI4iEXT", "V:iiiii");
        noop("glVertexAttribI1uiEXT", "V:ii");
        noop("glVertexAttribI2uiEXT", "V:iii");
        noop("glVertexAttribI3uiEXT", "V:iiii");
        noop("glVertexAttribI4uiEXT", "V:iiiii");
        for (String vector : new String[]{"1iv", "2iv", "3iv", "4iv", "1uiv", "2uiv", "3uiv", "4uiv",
                "4bv", "4sv", "4ubv", "4usv"}) {
            noop("glVertexAttribI" + vector + "EXT", "V:ip");
        }
        noop("glVertexAttribIPointerEXT", "V:iiiip");

        noop("glProgramUniform1dEXT", "V:iid");
        noop("glProgramUniform2dEXT", "V:iidd");
        noop("glProgramUniform3dEXT", "V:iiddd");
        noop("glProgramUniform4dEXT", "V:iidddd");
        for (String vector : new String[]{"1dv", "2dv", "3dv", "4dv"}) {
            noop("glProgramUniform" + vector + "EXT", "V:iiip");
        }
        for (String matrix : new String[]{"2dv", "3dv", "4dv", "2x3dv", "3x2dv", "2x4dv", "4x2dv", "3x4dv", "4x3dv"}) {
            noop("glProgramUniformMatrix" + matrix + "EXT", "V:iiibp");
        }

        noop("glClearColorIiEXT", "V:iiii");
        noop("glClearColorIuiEXT", "V:iiii");
        fn("glGetTexParameterIivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTexParameterIiEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetTexParameterIuivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetTexParameterIuiEXT", "I:ii", (ret, args) -> retI(ret, 0));
        noop("glTexParameterIiEXT", "V:iii");
        noop("glTexParameterIivEXT", "V:iip");
        noop("glTexParameterIuiEXT", "V:iii");
        noop("glTexParameterIuivEXT", "V:iip");

        noop("glColorMaskIndexedEXT", "V:ibbbb");
        noop("glDisableIndexedEXT", "V:ii");
        noop("glEnableIndexedEXT", "V:ii");
        fn("glGetBooleanIndexedvEXT", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), 1));
        fn("glGetBooleanIndexedEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetIntegerIndexedvEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetIntegerIndexedEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glIsEnabledIndexedEXT", "I:ii", (ret, args) -> retI(ret, 0));

        fn("glGetVertexAttribLdvEXT", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        noop("glVertexArrayVertexAttribLOffsetEXT", "V:iiiiiil");
        noop("glVertexAttribL1dEXT", "V:id");
        noop("glVertexAttribL2dEXT", "V:idd");
        noop("glVertexAttribL3dEXT", "V:iddd");
        noop("glVertexAttribL4dEXT", "V:idddd");
        noop("glVertexAttribL1dvEXT", "V:ip");
        noop("glVertexAttribL2dvEXT", "V:ip");
        noop("glVertexAttribL3dvEXT", "V:ip");
        noop("glVertexAttribL4dvEXT", "V:ip");
        noop("glVertexAttribLPointerEXT", "V:iiiip");

        noop("glSecondaryColor3bEXT", "V:bbb");
        noop("glSecondaryColor3ubEXT", "V:bbb");
        noop("glSecondaryColor3sEXT", "V:sss");
        noop("glSecondaryColor3usEXT", "V:sss");
        noop("glSecondaryColor3iEXT", "V:iii");
        noop("glSecondaryColor3uiEXT", "V:iii");
        noop("glSecondaryColor3fEXT", "V:fff");
        noop("glSecondaryColor3dEXT", "V:ddd");
        for (String vector : new String[]{"3bv", "3ubv", "3sv", "3usv", "3iv", "3uiv", "3fv", "3dv"}) {
            noop("glSecondaryColor" + vector + "EXT", "V:p");
        }
        noop("glSecondaryColorPointerEXT", "V:iiip");

        noop("glWindowPos2dARB", "V:dd");
        noop("glWindowPos3dARB", "V:ddd");
        noop("glWindowPos2fARB", "V:ff");
        noop("glWindowPos3fARB", "V:fff");
        noop("glWindowPos2iARB", "V:ii");
        noop("glWindowPos3iARB", "V:iii");
        noop("glWindowPos2sARB", "V:ss");
        noop("glWindowPos3sARB", "V:sss");
        for (String vector : new String[]{"2dv", "3dv", "2fv", "3fv", "2iv", "3iv", "2sv", "3sv"}) {
            noop("glWindowPos" + vector + "ARB", "V:p");
        }

        noop("glCompressedTexImage1DARB", "V:iiiiiip");
        noop("glCompressedTexImage2DARB", "V:iiiiiiip");
        noop("glCompressedTexImage3DARB", "V:iiiiiiiip");
        noop("glCompressedTexSubImage1DARB", "V:iiiiiip");
        noop("glCompressedTexSubImage2DARB", "V:iiiiiiiip");
        noop("glCompressedTexSubImage3DARB", "V:iiiiiiiiiip");
        noop("glGetCompressedTexImageARB", "V:iip");

        noop("glBeginTransformFeedbackEXT", "V:i");
        noop("glBindBufferBaseEXT", "V:iii");
        noop("glBindBufferOffsetEXT", "V:iiil");
        noop("glBindBufferRangeEXT", "V:iiill");
        noop("glEndTransformFeedbackEXT", "V:");
        fn("glGetTransformFeedbackVaryingEXT", "V:iiipppp", (ret, args) -> {
            zeroInt(argP(args, 4));
            zeroInt(argP(args, 5));
            writeOutString("", argI(args, 2), argP(args, 3), argP(args, 6));
        });
        noop("glTransformFeedbackVaryingsEXT", "V:iipi");

        noop("glVertexArrayBindVertexBufferEXT", "V:iiili");
        noop("glVertexArrayVertexAttribBindingEXT", "V:iii");
        noop("glVertexArrayVertexAttribFormatEXT", "V:iiiibi");
        noop("glVertexArrayVertexAttribIFormatEXT", "V:iiiii");
        noop("glVertexArrayVertexAttribLFormatEXT", "V:iiiii");
        noop("glVertexArrayVertexBindingDivisorEXT", "V:iii");

        noop("glCurrentPaletteMatrixARB", "V:i");
        noop("glMatrixIndexPointerARB", "V:iiip");
        noop("glMatrixIndexubvARB", "V:ip");
        noop("glMatrixIndexuivARB", "V:ip");
        noop("glMatrixIndexusvARB", "V:ip");

        noop("glCompileShaderIncludeARB", "V:iipp");
        noop("glDeleteNamedStringARB", "V:ip");
        fn("glGetNamedStringARB", "V:ipipp", (ret, args) ->
                writeOutString("", argI(args, 2), argP(args, 3), argP(args, 4)));
        fn("glGetNamedStringivARB", "V:ipip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGetNamedStringiARB", "I:ipi", (ret, args) -> retI(ret, 0));
        fn("glIsNamedStringARB", "I:ip", (ret, args) -> retI(ret, 0));
        noop("glNamedStringARB", "V:iipip");

        noop("glInsertEventMarkerEXT", "V:ip");
        noop("glPopGroupMarkerEXT", "V:");
        noop("glPushGroupMarkerEXT", "V:ip");

        noop("glLoadTransposeMatrixdARB", "V:p");
        noop("glLoadTransposeMatrixfARB", "V:p");
        noop("glMultTransposeMatrixdARB", "V:p");
        noop("glMultTransposeMatrixfARB", "V:p");

        noop("glEvaluateDepthValuesARB", "V:");
        noop("glFramebufferSampleLocationsfvARB", "V:iiip");
        noop("glNamedFramebufferSampleLocationsfvARB", "V:iiip");

        noop("glBufferPageCommitmentARB", "V:illb");
        noop("glNamedBufferPageCommitmentARB", "V:illb");
        noop("glNamedBufferPageCommitmentEXT", "V:illb");

        fn("glGetUniformBufferSizeEXT", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetUniformOffsetEXT", "L:ii", (ret, args) -> retL(ret, 0L));
        noop("glUniformBufferEXT", "V:iii");

        noop("glVertexBlendARB", "V:i");
        noop("glWeightPointerARB", "V:iiip");
        for (String vector : new String[]{"bv", "dv", "fv", "iv", "sv", "ubv", "uiv", "usv"}) {
            noop("glWeight" + vector + "ARB", "V:ip");
        }

        fn("glGetUniformi64vARB", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetUniformui64vARB", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetnUniformi64vARB", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));
        fn("glGetnUniformui64vARB", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));

        noop("glUniform1i64ARB", "V:il");
        noop("glUniform2i64ARB", "V:ill");
        noop("glUniform3i64ARB", "V:illl");
        noop("glUniform4i64ARB", "V:illll");
        noop("glUniform1ui64ARB", "V:il");
        noop("glUniform2ui64ARB", "V:ill");
        noop("glUniform3ui64ARB", "V:illl");
        noop("glUniform4ui64ARB", "V:illll");
        for (String vector : new String[]{"1i64v", "2i64v", "3i64v", "4i64v",
                "1ui64v", "2ui64v", "3ui64v", "4ui64v"}) {
            noop("glUniform" + vector + "ARB", "V:iip");
        }

        noop("glProgramUniform1i64ARB", "V:iil");
        noop("glProgramUniform2i64ARB", "V:iill");
        noop("glProgramUniform3i64ARB", "V:iilll");
        noop("glProgramUniform4i64ARB", "V:iillll");
        noop("glProgramUniform1ui64ARB", "V:iil");
        noop("glProgramUniform2ui64ARB", "V:iill");
        noop("glProgramUniform3ui64ARB", "V:iilll");
        noop("glProgramUniform4ui64ARB", "V:iillll");
        for (String vector : new String[]{"1i64v", "2i64v", "3i64v", "4i64v",
                "1ui64v", "2ui64v", "3ui64v", "4ui64v"}) {
            noop("glProgramUniform" + vector + "ARB", "V:iiip");
        }

        noop("glActiveProgramEXT", "V:i");
        fn("glCreateShaderProgramEXT", "I:ip", (ret, args) -> retI(ret, GlProgram.createProgram()));
        noop("glUseShaderProgramEXT", "V:ii");

        noop("glTexStorage1DEXT", "V:iiii");
        noop("glTexStorage2DEXT", "V:iiiii");
        noop("glTexStorage3DEXT", "V:iiiiii");

        noop("glClearNamedBufferDataEXT", "V:iiiip");
        noop("glClearNamedBufferSubDataEXT", "V:iilliip");

        noop("glDrawArraysInstancedARB", "V:iiii");
        noop("glDrawElementsInstancedARB", "V:iiipi");

        noop("glMultiDrawArraysIndirectCountARB", "V:iplii");
        noop("glMultiDrawElementsIndirectCountARB", "V:iiplii");
        noop("glVertexArrayVertexAttribDivisorEXT", "V:iii");
        noop("glVertexAttribDivisorARB", "V:ii");
        noop("glPointParameterfARB", "V:if");
        noop("glPointParameterfvARB", "V:ip");

        noop("glTexPageCommitmentARB", "V:iiiiiiiib");
        noop("glTexturePageCommitmentEXT", "V:iiiiiiiib");
        noop("glLockArraysEXT", "V:ii");
        noop("glUnlockArraysEXT", "V:");
        fn("glGetInteger64", "L:i", (ret, args) -> retL(ret, 0L));
        fn("glGetSynci", "I:pi", (ret, args) -> retI(ret, 0));

        fn("glGetObjectLabelEXT", "V:iiipp", (ret, args) ->
                writeOutString("", argI(args, 2), argP(args, 3), argP(args, 4)));
        noop("glLabelObjectEXT", "V:iiip");
        noop("glDrawArraysInstancedEXT", "V:iiii");
        noop("glDrawElementsInstancedEXT", "V:iiipi");
        noop("glEGLImageTargetTexStorageEXT", "V:ilp");
        noop("glEGLImageTargetTextureStorageEXT", "V:ilp");
        noop("glDrawTextureNV", "V:iifffffffff");
        noop("glTextureBarrierNV", "V:");
        noop("glVertex2hNV", "V:ss");
        noop("glVertex2hvNV", "V:p");
        noop("glVertex3hNV", "V:sss");
        noop("glVertex3hvNV", "V:p");
        noop("glVertex4hNV", "V:ssss");
        noop("glVertex4hvNV", "V:p");
        noop("glNormal3hNV", "V:sss");
        noop("glNormal3hvNV", "V:p");
        noop("glColor3hNV", "V:sss");
        noop("glColor3hvNV", "V:p");
        noop("glColor4hNV", "V:ssss");
        noop("glColor4hvNV", "V:p");
        noop("glTexCoord1hNV", "V:s");
        noop("glTexCoord1hvNV", "V:p");
        noop("glTexCoord2hNV", "V:ss");
        noop("glTexCoord2hvNV", "V:p");
        noop("glTexCoord3hNV", "V:sss");
        noop("glTexCoord3hvNV", "V:p");
        noop("glTexCoord4hNV", "V:ssss");
        noop("glTexCoord4hvNV", "V:p");
        noop("glMultiTexCoord1hNV", "V:is");
        noop("glMultiTexCoord1hvNV", "V:ip");
        noop("glMultiTexCoord2hNV", "V:iss");
        noop("glMultiTexCoord2hvNV", "V:ip");
        noop("glMultiTexCoord3hNV", "V:isss");
        noop("glMultiTexCoord3hvNV", "V:ip");
        noop("glMultiTexCoord4hNV", "V:issss");
        noop("glMultiTexCoord4hvNV", "V:ip");
        noop("glFogCoordhNV", "V:s");
        noop("glFogCoordhvNV", "V:p");
        noop("glSecondaryColor3hNV", "V:sss");
        noop("glSecondaryColor3hvNV", "V:p");
        noop("glVertexWeighthNV", "V:s");
        noop("glVertexWeighthvNV", "V:p");
        noop("glVertexAttrib1hNV", "V:is");
        noop("glVertexAttrib1hvNV", "V:ip");
        noop("glVertexAttrib2hNV", "V:iss");
        noop("glVertexAttrib2hvNV", "V:ip");
        noop("glVertexAttrib3hNV", "V:isss");
        noop("glVertexAttrib3hvNV", "V:ip");
        noop("glVertexAttrib4hNV", "V:issss");
        noop("glVertexAttrib4hvNV", "V:ip");
        noop("glVertexAttribs1hvNV", "V:iip");
        noop("glVertexAttribs2hvNV", "V:iip");
        noop("glVertexAttribs3hvNV", "V:iip");
        noop("glVertexAttribs4hvNV", "V:iip");
        noop("glUniform1i64NV", "V:il");
        noop("glUniform2i64NV", "V:ill");
        noop("glUniform3i64NV", "V:illl");
        noop("glUniform4i64NV", "V:illll");
        noop("glUniform1ui64NV", "V:il");
        noop("glUniform2ui64NV", "V:ill");
        noop("glUniform3ui64NV", "V:illl");
        noop("glUniform4ui64NV", "V:illll");
        for (String vector : new String[]{"1i64v", "2i64v", "3i64v", "4i64v",
                "1ui64v", "2ui64v", "3ui64v", "4ui64v"}) {
            noop("glUniform" + vector + "NV", "V:iip");
        }
        fn("glGetUniformi64vNV", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetUniformi64NV", "L:ii", (ret, args) -> retL(ret, 0L));
        fn("glGetUniformui64vNV", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetUniformui64NV", "L:ii", (ret, args) -> retL(ret, 0L));
        noop("glProgramUniform1i64NV", "V:iil");
        noop("glProgramUniform2i64NV", "V:iill");
        noop("glProgramUniform3i64NV", "V:iilll");
        noop("glProgramUniform4i64NV", "V:iillll");
        noop("glProgramUniform1ui64NV", "V:iil");
        noop("glProgramUniform2ui64NV", "V:iill");
        noop("glProgramUniform3ui64NV", "V:iilll");
        noop("glProgramUniform4ui64NV", "V:iillll");
        for (String vector : new String[]{"1i64v", "2i64v", "3i64v", "4i64v",
                "1ui64v", "2ui64v", "3ui64v", "4ui64v"}) {
            noop("glProgramUniform" + vector + "NV", "V:iiip");
        }
        noop("glMakeBufferResidentNV", "V:ii");
        noop("glMakeBufferNonResidentNV", "V:i");
        fn("glIsBufferResidentNV", "b:i", (ret, args) -> retB(ret, false));
        noop("glMakeNamedBufferResidentNV", "V:ii");
        noop("glMakeNamedBufferNonResidentNV", "V:i");
        fn("glIsNamedBufferResidentNV", "b:i", (ret, args) -> retB(ret, false));
        fn("glGetBufferParameterui64vNV", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetBufferParameterui64NV", "L:ii", (ret, args) -> retL(ret, 0L));
        fn("glGetNamedBufferParameterui64vNV", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetNamedBufferParameterui64NV", "L:ii", (ret, args) -> retL(ret, 0L));
        fn("glGetIntegerui64vNV", "V:ip", (ret, args) -> zeroLong(argP(args, 1)));
        fn("glGetIntegerui64NV", "L:i", (ret, args) -> retL(ret, 0L));
        noop("glUniformui64NV", "V:il");
        noop("glUniformui64vNV", "V:iip");
        noop("glProgramUniformui64NV", "V:iil");
        noop("glProgramUniformui64vNV", "V:iiip");
        noop("glBlendParameteriNV", "V:ii");
        noop("glBlendBarrierNV", "V:");
        noop("glBeginConditionalRenderNV", "V:ii");
        noop("glEndConditionalRenderNV", "V:");
        noop("glCopyImageSubDataNV", "V:iiiiiiiiiiiiiii");
        noop("glDepthRangedNV", "V:dd");
        noop("glClearDepthdNV", "V:d");
        noop("glDepthBoundsdNV", "V:dd");
        noop("glPixelDataRangeNV", "V:iip");
        noop("glFlushPixelDataRangeNV", "V:i");
        noop("glPrimitiveRestartNV", "V:");
        noop("glPrimitiveRestartIndexNV", "V:i");
        noop("glFramebufferSampleLocationsfvNV", "V:iiip");
        noop("glNamedFramebufferSampleLocationsfvNV", "V:iiip");
        noop("glResolveDepthValuesNV", "V:");
        noop("glTexImage2DMultisampleCoverageNV", "V:iiiiiib");
        noop("glTexImage3DMultisampleCoverageNV", "V:iiiiiiib");
        noop("glTextureImage2DMultisampleNV", "V:iiiiiib");
        noop("glTextureImage3DMultisampleNV", "V:iiiiiiib");
        noop("glTextureImage2DMultisampleCoverageNV", "V:iiiiiiib");
        noop("glTextureImage3DMultisampleCoverageNV", "V:iiiiiiiib");
        noop("glBeginTransformFeedbackNV", "V:i");
        noop("glEndTransformFeedbackNV", "V:");
        noop("glTransformFeedbackAttribsNV", "V:ipi");
        noop("glBindBufferRangeNV", "V:iiill");
        noop("glBindBufferOffsetNV", "V:iiil");
        noop("glBindBufferBaseNV", "V:iii");
        noop("glTransformFeedbackVaryingsNV", "V:iipi");
        noop("glActiveVaryingNV", "V:ip");
        fn("glGetVaryingLocationNV", "I:ip", (ret, args) -> retI(ret, -1));
        fn("glGetActiveVaryingNV", "V:iiipppp", (ret, args) -> {
            zeroInt(argP(args, 4));
            zeroInt(argP(args, 5));
            writeOutString("", argI(args, 2), argP(args, 3), argP(args, 6));
        });
        fn("glGetTransformFeedbackVaryingNV", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        noop("glTransformFeedbackStreamAttribsNV", "V:ipipi");
        noop("glVertexArrayRangeNV", "V:ip");
        noop("glFlushVertexArrayRangeNV", "V:");
        fn("glPathCommandsNV", "V:iipiip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathCoordsNV", "V:iiip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathSubCommandsNV", "V:iiiipiip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathSubCoordsNV", "V:iiiip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathStringNV", "V:iiip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathGlyphsNV", "V:iipiiipiif", (ret, args) -> addNvPathRange(argI(args, 0), argI(args, 4)));
        fn("glPathGlyphRangeNV", "V:iipiiiiif", (ret, args) -> addNvPathRange(argI(args, 0), argI(args, 5)));
        fn("glPathGlyphIndexArrayNV", "I:iipiiiif", (ret, args) -> {
            addNvPathRange(argI(args, 0), argI(args, 5));
            retI(ret, 0);
        });
        fn("glPathMemoryGlyphIndexArrayNV", "I:iipliiiif", (ret, args) -> {
            addNvPathRange(argI(args, 0), argI(args, 6));
            retI(ret, 0);
        });
        fn("glCopyPathNV", "V:ii", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glWeightPathsNV", "V:iipp", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glInterpolatePathsNV", "V:iiif", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glTransformPathNV", "V:iiip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathParameterivNV", "V:iip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathParameteriNV", "V:iii", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathParameterfvNV", "V:iip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathParameterfNV", "V:iif", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glPathDashArrayNV", "V:iip", (ret, args) -> addNvPath(argI(args, 0)));
        fn("glGenPathsNV", "I:i", (ret, args) -> retI(ret, genNvPaths(argI(args, 0))));
        fn("glDeletePathsNV", "V:ii", (ret, args) -> deleteNvPathRange(argI(args, 0), argI(args, 1)));
        fn("glIsPathNV", "b:i", (ret, args) -> retB(ret, NV_PATHS.contains(argI(args, 0))));
        noop("glPathStencilFuncNV", "V:iii");
        noop("glPathStencilDepthOffsetNV", "V:ff");
        noop("glStencilFillPathNV", "V:iii");
        noop("glStencilStrokePathNV", "V:iii");
        noop("glStencilFillPathInstancedNV", "V:iipiiiip");
        noop("glStencilStrokePathInstancedNV", "V:iipiiiip");
        noop("glPathCoverDepthFuncNV", "V:i");
        noop("glPathColorGenNV", "V:iiip");
        noop("glPathTexGenNV", "V:iiip");
        noop("glPathFogGenNV", "V:i");
        noop("glCoverFillPathNV", "V:ii");
        noop("glCoverStrokePathNV", "V:ii");
        noop("glCoverFillPathInstancedNV", "V:iipiiip");
        noop("glCoverStrokePathInstancedNV", "V:iipiiip");
        noop("glStencilThenCoverFillPathNV", "V:iiii");
        noop("glStencilThenCoverStrokePathNV", "V:iiii");
        noop("glStencilThenCoverFillPathInstancedNV", "V:iipiiiiip");
        noop("glStencilThenCoverStrokePathInstancedNV", "V:iipiiiiip");
        fn("glPathGlyphIndexRangeNV", "I:ipiifp", (ret, args) -> {
            zeroInt(argP(args, 5));
            retI(ret, 0);
        });
        noop("glProgramPathFragmentInputGenNV", "V:iiiip");
        fn("glGetPathParameterivNV", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetPathParameteriNV", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetPathParameterfvNV", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetPathParameterfNV", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetPathCommandsNV", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), 1));
        fn("glGetPathCoordsNV", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), 1));
        fn("glGetPathDashArrayNV", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), 1));
        fn("glGetPathMetricsNV", "V:iiipiip", (ret, args) -> zeroBytes(argP(args, 6), 4));
        fn("glGetPathMetricRangeNV", "V:iiiip", (ret, args) -> zeroBytes(argP(args, 4), 4));
        fn("glGetPathSpacingNV", "V:iiipiffip", (ret, args) -> zeroBytes(argP(args, 8), 4));
        fn("glGetPathColorGenivNV", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetPathColorGeniNV", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetPathColorGenfvNV", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetPathColorGenfNV", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glGetPathTexGenivNV", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetPathTexGeniNV", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetPathTexGenfvNV", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        fn("glGetPathTexGenfNV", "F:ii", (ret, args) -> retF(ret, 0.0f));
        fn("glIsPointInFillPathNV", "b:iiff", (ret, args) -> retB(ret, false));
        fn("glIsPointInStrokePathNV", "b:iff", (ret, args) -> retB(ret, false));
        fn("glGetPathLengthNV", "F:iii", (ret, args) -> retF(ret, 0.0f));
        fn("glPointAlongPathNV", "b:iiifpppp", (ret, args) -> {
            zeroInt(argP(args, 4));
            zeroInt(argP(args, 5));
            zeroInt(argP(args, 6));
            zeroInt(argP(args, 7));
            retB(ret, false);
        });
        noop("glMatrixLoad3x2fNV", "V:ip");
        noop("glMatrixLoad3x3fNV", "V:ip");
        noop("glMatrixLoadTranspose3x3fNV", "V:ip");
        noop("glMatrixMult3x2fNV", "V:ip");
        noop("glMatrixMult3x3fNV", "V:ip");
        noop("glMatrixMultTranspose3x3fNV", "V:ip");
        fn("glGetProgramResourcefvNV", "V:iiiipipp", (ret, args) -> {
            zeroInt(argP(args, 6));
            long params = argP(args, 7);
            if (params != 0) {
                MemoryUtil.memPutFloat(params, 0.0f);
            }
        });
        noop("glVertexAttribL1i64NV", "V:il");
        noop("glVertexAttribL2i64NV", "V:ill");
        noop("glVertexAttribL3i64NV", "V:illl");
        noop("glVertexAttribL4i64NV", "V:illll");
        noop("glVertexAttribL1ui64NV", "V:il");
        noop("glVertexAttribL2ui64NV", "V:ill");
        noop("glVertexAttribL3ui64NV", "V:illl");
        noop("glVertexAttribL4ui64NV", "V:illll");
        for (String vector : new String[]{"1i64v", "2i64v", "3i64v", "4i64v",
                "1ui64v", "2ui64v", "3ui64v", "4ui64v"}) {
            noop("glVertexAttribL" + vector + "NV", "V:ip");
        }
        fn("glGetVertexAttribLi64vNV", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetVertexAttribLi64NV", "L:ii", (ret, args) -> retL(ret, 0L));
        fn("glGetVertexAttribLui64vNV", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetVertexAttribLui64NV", "L:ii", (ret, args) -> retL(ret, 0L));
        noop("glVertexAttribLFormatNV", "V:iiii");
        noop("glBufferAddressRangeNV", "V:iill");
        noop("glVertexFormatNV", "V:iii");
        noop("glNormalFormatNV", "V:ii");
        noop("glColorFormatNV", "V:iii");
        noop("glIndexFormatNV", "V:ii");
        noop("glTexCoordFormatNV", "V:iii");
        noop("glEdgeFlagFormatNV", "V:i");
        noop("glSecondaryColorFormatNV", "V:iii");
        noop("glFogCoordFormatNV", "V:ii");
        noop("glVertexAttribFormatNV", "V:iiibi");
        noop("glVertexAttribIFormatNV", "V:iiii");
        fn("glGetIntegerui64i_vNV", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetIntegerui64iNV", "L:ii", (ret, args) -> retL(ret, 0L));
        fn("glGetTextureHandleNV", "L:i", (ret, args) -> retL(ret, fakeTextureHandle(argI(args, 0), 0)));
        fn("glGetTextureSamplerHandleNV", "L:ii", (ret, args) -> retL(ret, fakeTextureHandle(argI(args, 0), argI(args, 1))));
        fn("glMakeTextureHandleResidentNV", "V:l", (ret, args) -> ARB_RESIDENT_TEXTURE_HANDLES.add(argL(args, 0)));
        fn("glMakeTextureHandleNonResidentNV", "V:l", (ret, args) -> ARB_RESIDENT_TEXTURE_HANDLES.remove(argL(args, 0)));
        fn("glGetImageHandleNV", "L:iibii", (ret, args) -> retL(ret, fakeImageHandle(argI(args, 0), argI(args, 1),
                argB(args, 2), argI(args, 3), argI(args, 4))));
        fn("glMakeImageHandleResidentNV", "V:li", (ret, args) -> ARB_RESIDENT_IMAGE_HANDLES.add(argL(args, 0)));
        fn("glMakeImageHandleNonResidentNV", "V:l", (ret, args) -> ARB_RESIDENT_IMAGE_HANDLES.remove(argL(args, 0)));
        noop("glUniformHandleui64NV", "V:il");
        noop("glUniformHandleui64vNV", "V:iip");
        noop("glProgramUniformHandleui64NV", "V:iil");
        noop("glProgramUniformHandleui64vNV", "V:iiip");
        fn("glIsTextureHandleResidentNV", "b:l", (ret, args) -> retB(ret, ARB_RESIDENT_TEXTURE_HANDLES.contains(argL(args, 0))));
        fn("glIsImageHandleResidentNV", "b:l", (ret, args) -> retB(ret, ARB_RESIDENT_IMAGE_HANDLES.contains(argL(args, 0))));
        fn("glDeleteFencesNV", "V:ip", (ret, args) -> {
            int n = argI(args, 0);
            long ptr = argP(args, 1);
            for (int i = 0; i < n && ptr != 0; i++) {
                NV_FENCES.remove(MemoryUtil.memGetInt(ptr + 4L * i));
            }
        });
        fn("glGenFencesNV", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genNvFence));
        fn("glIsFenceNV", "b:i", (ret, args) -> retB(ret, NV_FENCES.contains(argI(args, 0))));
        fn("glTestFenceNV", "b:i", (ret, args) -> retB(ret, NV_FENCES.contains(argI(args, 0))));
        fn("glGetFenceivNV", "V:iip", (ret, args) -> {
            long params = argP(args, 2);
            if (params != 0) {
                MemoryUtil.memPutInt(params, NV_FENCES.contains(argI(args, 0)) ? 1 : 0);
            }
        });
        fn("glGetFenceiNV", "I:ii", (ret, args) -> retI(ret, NV_FENCES.contains(argI(args, 0)) ? 1 : 0));
        noop("glFinishFenceNV", "V:i");
        fn("glSetFenceNV", "V:ii", (ret, args) -> NV_FENCES.add(argI(args, 0)));
        noop("glAlphaToCoverageDitherControlNV", "V:i");
        noop("glViewportPositionWScaleNV", "V:iff");
        noop("glSubpixelPrecisionBiasNV", "V:ii");
        noop("glConservativeRasterParameterfNV", "V:if");
        noop("glConservativeRasterParameteriNV", "V:ii");
        fn("glGetMultisamplefvNV", "V:iip", (ret, args) -> {
            long value = argP(args, 2);
            if (value != 0) {
                MemoryUtil.memPutFloat(value, 0.0f);
            }
        });
        noop("glSampleMaskIndexedNV", "V:ii");
        noop("glTexRenderbufferNV", "V:ii");
        noop("glFragmentCoverageColorNV", "V:i");
        noop("glCoverageModulationTableNV", "V:ip");
        fn("glGetCoverageModulationTableNV", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), Math.max(4, argI(args, 0) * Float.BYTES)));
        noop("glCoverageModulationNV", "V:i");
        noop("glRenderbufferStorageMultisampleCoverageNV", "V:iiiiii");
        noop("glPointParameteriNV", "V:ii");
        noop("glPointParameterivNV", "V:ip");
        fn("glQueryResourceNV", "I:iiip", (ret, args) -> {
            zeroBytes(argP(args, 3), Math.max(0, argI(args, 2)) * Integer.BYTES);
            retI(ret, 0);
        });
        fn("glGenQueryResourceTagNV", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genNvQueryResourceTag));
        fn("glDeleteQueryResourceTagNV", "V:ip", (ret, args) -> {
            int n = argI(args, 0);
            long ptr = argP(args, 1);
            for (int i = 0; i < n && ptr != 0; i++) {
                NV_QUERY_RESOURCE_TAGS.remove(MemoryUtil.memGetInt(ptr + 4L * i));
            }
        });
        noop("glQueryResourceTagNV", "V:ip");
        noop("glScissorExclusiveArrayvNV", "V:iip");
        noop("glScissorExclusiveNV", "V:iiii");
        fn("glCreateSemaphoresNV", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genExtSemaphore));
        noop("glSemaphoreParameterivNV", "V:iip");
        fn("glGetSemaphoreParameterivNV", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        noop("glViewportSwizzleNV", "V:iiiii");
        noop("glBeginConditionalRenderNVX", "V:i");
        noop("glEndConditionalRenderNVX", "V:");
        fn("glCreateProgressFenceNVX", "I:", (ret, args) -> retI(ret, genExtSemaphore()));
        noop("glSignalSemaphoreui64NVX", "V:iipp");
        noop("glWaitSemaphoreui64NVX", "V:iipp");
        noop("glClientWaitSemaphoreui64NVX", "V:ipp");
        fn("glCreateStatesNV", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genNvCommandState));
        fn("glDeleteStatesNV", "V:ip", (ret, args) ->
                deleteLoop(args, id -> NV_COMMAND_STATES.remove(id)));
        fn("glIsStateNV", "b:i", (ret, args) -> retB(ret, NV_COMMAND_STATES.contains(argI(args, 0))));
        noop("glStateCaptureNV", "V:ii");
        fn("glGetCommandHeaderNV", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetStageIndexNV", "I:i", (ret, args) -> retI(ret, 0));
        noop("glDrawCommandsNV", "V:iippi");
        noop("glDrawCommandsAddressNV", "V:ippi");
        noop("glDrawCommandsStatesNV", "V:ippppi");
        noop("glDrawCommandsStatesAddressNV", "V:ppppi");
        fn("glCreateCommandListsNV", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genNvCommandList));
        fn("glDeleteCommandListsNV", "V:ip", (ret, args) ->
                deleteLoop(args, id -> NV_COMMAND_LISTS.remove(id)));
        fn("glIsCommandListNV", "b:i", (ret, args) -> retB(ret, NV_COMMAND_LISTS.contains(argI(args, 0))));
        noop("glListDrawCommandsStatesClientNV", "V:iippppi");
        noop("glCommandListSegmentsNV", "V:ii");
        noop("glCompileCommandListNV", "V:i");
        noop("glCallCommandListNV", "V:i");
        noop("glRenderGpuMaskNV", "V:i");
        noop("glMulticastBufferSubDataNV", "V:iillp");
        noop("glMulticastCopyBufferSubDataNV", "V:iiiilll");
        noop("glMulticastCopyImageSubDataNV", "V:iiiiiiiiiiiiiiiii");
        noop("glMulticastBlitFramebufferNV", "V:iiiiiiiiiiii");
        noop("glMulticastFramebufferSampleLocationsfvNV", "V:iiiip");
        noop("glMulticastBarrierNV", "V:");
        noop("glMulticastWaitSyncNV", "V:ii");
        fn("glMulticastGetQueryObjectivNV", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glMulticastGetQueryObjectiNV", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glMulticastGetQueryObjectuivNV", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glMulticastGetQueryObjectuiNV", "I:iii", (ret, args) -> retI(ret, 0));
        fn("glMulticastGetQueryObjecti64vNV", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));
        fn("glMulticastGetQueryObjecti64NV", "L:iii", (ret, args) -> retL(ret, 0L));
        fn("glMulticastGetQueryObjectui64vNV", "V:iiip", (ret, args) -> zeroLong(argP(args, 3)));
        fn("glMulticastGetQueryObjectui64NV", "L:iii", (ret, args) -> retL(ret, 0L));
        noop("glBindTransformFeedbackNV", "V:ii");
        fn("glDeleteTransformFeedbacksNV", "V:ip", (ret, args) ->
                deleteLoop(args, id -> NV_TRANSFORM_FEEDBACKS.remove(id)));
        fn("glGenTransformFeedbacksNV", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genNvTransformFeedback));
        fn("glIsTransformFeedbackNV", "b:i", (ret, args) ->
                retB(ret, NV_TRANSFORM_FEEDBACKS.contains(argI(args, 0))));
        noop("glPauseTransformFeedbackNV", "V:");
        noop("glResumeTransformFeedbackNV", "V:");
        noop("glDrawTransformFeedbackNV", "V:ii");
        noop("glMultiDrawArraysIndirectBindlessNV", "V:ipiii");
        noop("glMultiDrawElementsIndirectBindlessNV", "V:iipiii");
        noop("glMultiDrawArraysIndirectBindlessCountNV", "V:ipliii");
        noop("glMultiDrawElementsIndirectBindlessCountNV", "V:iipliii");
        noop("glDrawMeshTasksNV", "V:ii");
        noop("glDrawMeshTasksIndirectNV", "V:p");
        noop("glMultiDrawMeshTasksIndirectNV", "V:pii");
        noop("glMultiDrawMeshTasksIndirectCountNV", "V:ppii");
        noop("glDrawVkImageNV", "V:lifffffffff");
        fn("glGetVkProcAddrNV", "P:p", (ret, args) -> retP(ret, 0L));
        noop("glWaitVkSemaphoreNV", "V:l");
        noop("glSignalVkSemaphoreNV", "V:l");
        noop("glSignalVkFenceNV", "V:l");
        fn("glAsyncCopyImageSubDataNVX", "I:ippiiiiiiiiiiiiiiiiiipp", (ret, args) -> retI(ret, 0));
        fn("glAsyncCopyBufferSubDataNVX", "L:ippiiiilllipp", (ret, args) -> retL(ret, 0L));
        noop("glUploadGpuMaskNVX", "V:i");
        noop("glMulticastViewportArrayvNVX", "V:iiip");
        noop("glMulticastScissorArrayvNVX", "V:iiip");
        noop("glMulticastViewportPositionWScaleNVX", "V:iiff");
        noop("glResetMemoryObjectParameterNV", "V:ii");
        noop("glTexAttachMemoryNV", "V:iil");
        noop("glBufferAttachMemoryNV", "V:iil");
        noop("glTextureAttachMemoryNV", "V:iil");
        noop("glNamedBufferAttachMemoryNV", "V:iil");
        fn("glGetMemoryObjectDetachedResourcesuivNV", "V:iiiip", (ret, args) -> zeroInt(argP(args, 4)));
        noop("glBufferPageCommitmentMemNV", "V:illilb");
        noop("glNamedBufferPageCommitmentMemNV", "V:illilb");
        noop("glTexPageCommitmentMemNV", "V:iiiiiiiiilb");
        noop("glTexturePageCommitmentMemNV", "V:iiiiiiiiilb");
        fn("glGetInternalformatSampleivNV", "V:iiiiip", (ret, args) -> zeroInt(argP(args, 5)));
        noop("glBindShadingRateImageNV", "V:i");
        noop("glShadingRateImagePaletteNV", "V:iiip");
        fn("glGetShadingRateImagePaletteNV", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        noop("glShadingRateImageBarrierNV", "V:b");
        noop("glShadingRateSampleOrderNV", "V:i");
        noop("glShadingRateSampleOrderCustomNV", "V:iip");
        fn("glGetShadingRateSampleLocationivNV", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        noop("glBlendFuncIndexedAMD", "V:iii");
        noop("glBlendFuncSeparateIndexedAMD", "V:iiiii");
        noop("glBlendEquationIndexedAMD", "V:ii");
        noop("glBlendEquationSeparateIndexedAMD", "V:iii");
        noop("glDebugMessageEnableAMD", "V:iiipb");
        noop("glDebugMessageInsertAMD", "V:iiiip");
        noop("glDebugMessageCallbackAMD", "V:pp");
        fn("glGetDebugMessageLogAMD", "I:iippppp", (ret, args) -> {
            zeroInt(argP(args, 2));
            zeroInt(argP(args, 3));
            zeroInt(argP(args, 4));
            zeroInt(argP(args, 5));
            zeroBytes(argP(args, 6), Math.max(1, argI(args, 1)));
            retI(ret, 0);
        });
        fn("glGetPerfMonitorGroupsAMD", "V:pip", (ret, args) -> {
            zeroInt(argP(args, 0));
            zeroInt(argP(args, 2));
        });
        fn("glGetPerfMonitorCountersAMD", "V:ippip", (ret, args) -> {
            zeroInt(argP(args, 1));
            zeroInt(argP(args, 2));
            zeroInt(argP(args, 4));
        });
        fn("glGetPerfMonitorGroupStringAMD", "V:iipp", (ret, args) -> {
            zeroInt(argP(args, 2));
            zeroBytes(argP(args, 3), Math.max(1, argI(args, 1)));
        });
        fn("glGetPerfMonitorCounterStringAMD", "V:iiipp", (ret, args) -> {
            zeroInt(argP(args, 3));
            zeroBytes(argP(args, 4), Math.max(1, argI(args, 2)));
        });
        fn("glGetPerfMonitorCounterInfoAMD", "V:iiip", (ret, args) -> zeroInt(argP(args, 3)));
        fn("glGenPerfMonitorsAMD", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genAmdPerfMonitor));
        fn("glDeletePerfMonitorsAMD", "V:ip", (ret, args) ->
                deleteLoop(args, id -> AMD_PERF_MONITORS.remove(id)));
        noop("glSelectPerfMonitorCountersAMD", "V:ibiip");
        noop("glBeginPerfMonitorAMD", "V:i");
        noop("glEndPerfMonitorAMD", "V:i");
        fn("glGetPerfMonitorCounterDataAMD", "V:iiipp", (ret, args) -> {
            zeroInt(argP(args, 3));
            zeroInt(argP(args, 4));
        });
        noop("glRenderbufferStorageMultisampleAdvancedAMD", "V:iiiiii");
        noop("glNamedRenderbufferStorageMultisampleAdvancedAMD", "V:iiiiii");
        noop("glVertexAttribParameteriAMD", "V:iii");
        noop("glQueryObjectParameteruiAMD", "V:iiii");
        noop("glSetMultisamplefvAMD", "V:iip");
        noop("glTexStorageSparseAMD", "V:iiiiiii");
        noop("glTextureStorageSparseAMD", "V:iiiiiiii");
        noop("glStencilOpValueAMD", "V:ii");
        noop("glTessellationFactorAMD", "V:f");
        noop("glTessellationModeAMD", "V:i");
        noop("glApplyFramebufferAttachmentCMAAINTEL", "V:");
        noop("glSyncTextureINTEL", "V:i");
        noop("glUnmapTexture2DINTEL", "V:ii");
        fn("glMapTexture2DINTEL", "P:iiipp", (ret, args) -> {
            zeroInt(argP(args, 3));
            zeroInt(argP(args, 4));
            retP(ret, 0L);
        });
        noop("glBeginPerfQueryINTEL", "V:i");
        fn("glCreatePerfQueryINTEL", "V:ip", (ret, args) -> {
            long queryHandle = argP(args, 1);
            if (queryHandle != 0) {
                MemoryUtil.memPutInt(queryHandle, genIntelPerfQuery());
            }
        });
        fn("glDeletePerfQueryINTEL", "V:i", (ret, args) -> INTEL_PERF_QUERIES.remove(argI(args, 0)));
        noop("glEndPerfQueryINTEL", "V:i");
        fn("glGetFirstPerfQueryIdINTEL", "V:p", (ret, args) -> zeroInt(argP(args, 0)));
        fn("glGetNextPerfQueryIdINTEL", "V:ip", (ret, args) -> zeroInt(argP(args, 1)));
        fn("glGetPerfCounterInfoINTEL", "V:iiipipppppp", (ret, args) -> {
            zeroBytes(argP(args, 3), Math.max(1, argI(args, 2)));
            zeroBytes(argP(args, 5), Math.max(1, argI(args, 4)));
            zeroInt(argP(args, 6));
            zeroInt(argP(args, 7));
            zeroInt(argP(args, 8));
            zeroInt(argP(args, 9));
            zeroLong(argP(args, 10));
        });
        fn("glGetPerfQueryDataINTEL", "V:iiipp", (ret, args) -> {
            zeroBytes(argP(args, 3), Math.max(1, argI(args, 2)));
            zeroInt(argP(args, 4));
        });
        fn("glGetPerfQueryIdByNameINTEL", "V:pp", (ret, args) -> zeroInt(argP(args, 1)));
        fn("glGetPerfQueryInfoINTEL", "V:iippppp", (ret, args) -> {
            zeroBytes(argP(args, 2), Math.max(1, argI(args, 1)));
            zeroInt(argP(args, 3));
            zeroInt(argP(args, 4));
            zeroInt(argP(args, 5));
            zeroInt(argP(args, 6));
        });
        noop("glFramebufferParameteriMESA", "V:iii");
        fn("glGetFramebufferParameterivMESA", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        noop("glFramebufferTextureMultiviewOVR", "V:iiiiii");
        noop("glNamedFramebufferTextureMultiviewOVR", "V:iiiiii");
        noop("glFrameTerminatorGREMEDY", "V:");
        noop("glStringMarkerGREMEDY", "V:ip");

        noop("glBufferStorageExternalEXT", "V:illli");
        noop("glNamedBufferStorageExternalEXT", "V:illli");
        noop("glBlitFramebufferLayerEXT", "V:iiiiiiiiiiii");
        noop("glBlitFramebufferLayersEXT", "V:iiiiiiiiii");
        noop("glProgramEnvParameters4fvEXT", "V:iip");
        noop("glProgramLocalParameters4fvEXT", "V:iip");

        fn("glAcquireKeyedMutexWin32EXT", "b:ili", (ret, args) -> retB(ret, true));
        fn("glReleaseKeyedMutexWin32EXT", "b:il", (ret, args) -> retB(ret, true));
        noop("glBindImageTextureEXT", "V:iiibiii");
        noop("glMemoryBarrierEXT", "V:i");
        noop("glImportSemaphoreWin32HandleEXT", "V:iil");
        noop("glImportSemaphoreWin32NameEXT", "V:iil");
        noop("glPointParameterfEXT", "V:if");
        noop("glPointParameterfvEXT", "V:ip");
        noop("glImportMemoryWin32HandleEXT", "V:ilil");
        noop("glImportMemoryWin32NameEXT", "V:ilil");

        fn("glGetMultisamplef", "F:ii", (ret, args) -> retF(ret, 0.0f));
        noop("glTextureBufferRangeEXT", "V:iiiill");
        fn("glNamedBufferStorageEXT", "V:ilpi", (ret, args) -> {
            long size = argL(args, 1);
            long data = argP(args, 2);
            if (data != 0 && size > 0) {
                GlBuffer.glBufferData(argI(args, 0), MemoryUtil.memByteBuffer(data, (int) size), GL_STATIC_DRAW);
            } else {
                GlBuffer.glBufferData(argI(args, 0), size, GL_STATIC_DRAW);
            }
        });
        noop("glMaxShaderCompilerThreadsKHR", "V:i");
        noop("glBlendBarrierKHR", "V:");
        fn("glBlendEquationEXT", "V:i", (ret, args) -> VRenderSystem.blendEquation(argI(args, 0)));
        fn("glBlendFuncSeparateEXT", "V:iiii", (ret, args) -> VRenderSystem.blendFuncSeparate(argI(args, 0), argI(args, 1),
                argI(args, 2), argI(args, 3)));
        fn("glBlendColorEXT", "V:ffff", (ret, args) -> VRenderSystem.blendColor(argF(args, 0), argF(args, 1),
                argF(args, 2), argF(args, 3)));
        fn("glBlendEquationSeparateEXT", "V:ii", (ret, args) -> VRenderSystem.blendEquationSeparate(argI(args, 0), argI(args, 1)));
        noop("glTexBufferEXT", "V:iii");

        fn("glImportSyncEXT", "P:ili", (ret, args) -> retP(ret, GlSync.fenceSync(argI(args, 0), argI(args, 2))));
        noop("glWindowRectanglesEXT", "V:iip");
        noop("glActiveStencilFaceEXT", "V:i");
        noop("glStencilClearTagEXT", "V:ii");
        noop("glFramebufferFetchBarrierEXT", "V:");
        noop("glProvokingVertexEXT", "V:i");
        noop("glImportMemoryFdEXT", "V:ilii");
        noop("glImportSemaphoreFdEXT", "V:iii");

        noop("glRasterSamplesEXT", "V:ib");
        noop("glPolygonOffsetClampEXT", "V:fff");
        fn("glRenderbufferStorageMultisampleEXT", "V:iiiii", (ret, args) ->
                GlRenderbuffer.renderbufferStorage(argI(args, 0), argI(args, 2), argI(args, 3), argI(args, 4)));
        noop("glBlitFramebufferEXT", "V:iiiiiiiiii");
        noop("glDepthBoundsEXT", "V:dd");
        noop("glClampColorARB", "V:ii");
        noop("glDispatchComputeGroupSizeARB", "V:iiiiii");

        noop("glTexBufferARB", "V:iii");
        noop("glMinSampleShadingARB", "V:f");
        noop("glPolygonOffsetClamp", "V:fff");
        noop("glMaxShaderCompilerThreadsARB", "V:i");
        noop("glSampleCoverageARB", "V:fb");
        noop("glPrimitiveBoundingBoxARB", "V:ffffffff");

        noop("glSpecializeShaderARB", "V:ipipp");
        noop("glDrawBuffersARB", "V:ip");
        fn("glCreateSyncFromCLeventARB", "P:ppi", (ret, args) -> retP(ret, GlSync.fenceSync(0, argI(args, 2))));

        fn("glGetUnsignedBytevEXT", "V:ip", (ret, args) -> zeroBytes(argP(args, 1), 16));
        fn("glGetUnsignedBytei_vEXT", "V:iip", (ret, args) -> zeroBytes(argP(args, 2), 16));
        fn("glGenSemaphoresEXT", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genExtSemaphore));
        fn("glDeleteSemaphoresEXT", "V:ip", (ret, args) -> {
            int n = argI(args, 0);
            long ptr = argP(args, 1);
            for (int i = 0; i < n && ptr != 0; i++) {
                EXT_SEMAPHORES.remove(MemoryUtil.memGetInt(ptr + 4L * i));
            }
        });
        fn("glIsSemaphoreEXT", "b:i", (ret, args) -> retB(ret, EXT_SEMAPHORES.contains(argI(args, 0))));
        noop("glSemaphoreParameterui64vEXT", "V:iip");
        noop("glSemaphoreParameterui64EXT", "V:iil");
        fn("glGetSemaphoreParameterui64vEXT", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetSemaphoreParameterui64EXT", "L:ii", (ret, args) -> retL(ret, 0L));
        noop("glWaitSemaphoreEXT", "V:iipipp");
        noop("glSignalSemaphoreEXT", "V:iipipp");

        fn("glDeleteMemoryObjectsEXT", "V:ip", (ret, args) -> {
            int n = argI(args, 0);
            long ptr = argP(args, 1);
            for (int i = 0; i < n && ptr != 0; i++) {
                EXT_MEMORY_OBJECTS.remove(MemoryUtil.memGetInt(ptr + 4L * i));
            }
        });
        fn("glIsMemoryObjectEXT", "b:i", (ret, args) -> retB(ret, EXT_MEMORY_OBJECTS.contains(argI(args, 0))));
        fn("glCreateMemoryObjectsEXT", "V:ip", (ret, args) -> genLoop(args, GlFunctionRegistry::genExtMemoryObject));
        noop("glMemoryObjectParameterivEXT", "V:iip");
        noop("glMemoryObjectParameteriEXT", "V:iii");
        fn("glGetMemoryObjectParameterivEXT", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetMemoryObjectParameteriEXT", "I:ii", (ret, args) -> retI(ret, 0));
        noop("glTexStorageMem1DEXT", "V:iiiiil");
        noop("glTexStorageMem2DEXT", "V:iiiiiil");
        noop("glTexStorageMem2DMultisampleEXT", "V:iiiiibil");
        noop("glTexStorageMem3DEXT", "V:iiiiiiil");
        noop("glTexStorageMem3DMultisampleEXT", "V:iiiiiibil");
        noop("glBufferStorageMemEXT", "V:ilil");
        noop("glTextureStorageMem1DEXT", "V:iiiiil");
        noop("glTextureStorageMem2DEXT", "V:iiiiiil");
        noop("glTextureStorageMem2DMultisampleEXT", "V:iiiiibil");
        noop("glTextureStorageMem3DEXT", "V:iiiiiiil");
        noop("glTextureStorageMem3DMultisampleEXT", "V:iiiiiibil");
        noop("glNamedBufferStorageMemEXT", "V:ilil");

        fn("glGetTextureHandleARB", "L:i", (ret, args) ->
                retL(ret, fakeTextureHandle(argI(args, 0), 0)));
        fn("glGetTextureSamplerHandleARB", "L:ii", (ret, args) ->
                retL(ret, fakeTextureHandle(argI(args, 0), argI(args, 1))));
        fn("glMakeTextureHandleResidentARB", "V:l", (ret, args) ->
                ARB_RESIDENT_TEXTURE_HANDLES.add(argL(args, 0)));
        fn("glMakeTextureHandleNonResidentARB", "V:l", (ret, args) ->
                ARB_RESIDENT_TEXTURE_HANDLES.remove(argL(args, 0)));
        fn("glGetImageHandleARB", "L:iibii", (ret, args) ->
                retL(ret, fakeImageHandle(argI(args, 0), argI(args, 1), argB(args, 2),
                        argI(args, 3), argI(args, 4))));
        fn("glMakeImageHandleResidentARB", "V:li", (ret, args) ->
                ARB_RESIDENT_IMAGE_HANDLES.add(argL(args, 0)));
        fn("glMakeImageHandleNonResidentARB", "V:l", (ret, args) ->
                ARB_RESIDENT_IMAGE_HANDLES.remove(argL(args, 0)));
        noop("glUniformHandleui64ARB", "V:il");
        noop("glUniformHandleui64vARB", "V:iip");
        noop("glProgramUniformHandleui64ARB", "V:iil");
        noop("glProgramUniformHandleui64vARB", "V:iiip");
        fn("glIsTextureHandleResidentARB", "b:l", (ret, args) ->
                retB(ret, ARB_RESIDENT_TEXTURE_HANDLES.contains(argL(args, 0))));
        fn("glIsImageHandleResidentARB", "b:l", (ret, args) ->
                retB(ret, ARB_RESIDENT_IMAGE_HANDLES.contains(argL(args, 0))));
        noop("glVertexAttribL1ui64ARB", "V:il");
        noop("glVertexAttribL1ui64vARB", "V:ip");
        fn("glGetVertexAttribLui64vARB", "V:iip", (ret, args) -> zeroLong(argP(args, 2)));
        fn("glGetVertexAttribLui64ARB", "L:ii", (ret, args) -> retL(ret, 0L));

        noop("glColorTable", "V:iiiiip");
        noop("glCopyColorTable", "V:iiiii");
        noop("glColorTableParameteriv", "V:iip");
        noop("glColorTableParameterfv", "V:iip");
        fn("glGetColorTable", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), 1));
        fn("glGetColorTableParameteriv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetColorTableParameteri", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetColorTableParameterfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetColorTableParameterf", "F:ii", (ret, args) -> retF(ret, 0.0f));
        noop("glColorSubTable", "V:iiiiip");
        noop("glCopyColorSubTable", "V:iiiii");

        noop("glConvolutionFilter1D", "V:iiiiip");
        noop("glConvolutionFilter2D", "V:iiiiiip");
        noop("glCopyConvolutionFilter1D", "V:iiiii");
        noop("glCopyConvolutionFilter2D", "V:iiiiii");
        fn("glGetConvolutionFilter", "V:iiip", (ret, args) -> zeroBytes(argP(args, 3), 1));
        noop("glSeparableFilter2D", "V:iiiiiipp");
        fn("glGetSeparableFilter", "V:iiippp", (ret, args) -> {
            zeroBytes(argP(args, 3), 1);
            zeroBytes(argP(args, 4), 1);
            zeroBytes(argP(args, 5), 1);
        });
        noop("glConvolutionParameteri", "V:iii");
        noop("glConvolutionParameteriv", "V:iip");
        noop("glConvolutionParameterf", "V:iif");
        noop("glConvolutionParameterfv", "V:iip");
        fn("glGetConvolutionParameteriv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetConvolutionParameteri", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetConvolutionParameterfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetConvolutionParameterf", "F:ii", (ret, args) -> retF(ret, 0.0f));

        noop("glHistogram", "V:iiib");
        noop("glResetHistogram", "V:i");
        fn("glGetHistogram", "V:ibiip", (ret, args) -> zeroBytes(argP(args, 4), 1));
        fn("glGetHistogramParameteriv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetHistogramParameteri", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetHistogramParameterfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetHistogramParameterf", "F:ii", (ret, args) -> retF(ret, 0.0f));

        noop("glMinmax", "V:iib");
        noop("glResetMinmax", "V:i");
        fn("glGetMinmax", "V:ibiip", (ret, args) -> zeroBytes(argP(args, 4), 1));
        fn("glGetMinmaxParameteriv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetMinmaxParameteri", "I:ii", (ret, args) -> retI(ret, 0));
        fn("glGetMinmaxParameterfv", "V:iip", (ret, args) -> zeroInt(argP(args, 2)));
        fn("glGetMinmaxParameterf", "F:ii", (ret, args) -> retF(ret, 0.0f));
    }

    private static void registerStubs() {
        stub("glVertexAttrib1f", "glVertexAttrib1s", "glVertexAttrib1d",
                "glVertexAttrib2f", "glVertexAttrib2s", "glVertexAttrib2d",
                "glVertexAttrib3f", "glVertexAttrib3s", "glVertexAttrib3d",
                "glVertexAttrib4f", "glVertexAttrib4s", "glVertexAttrib4d", "glVertexAttrib4Nub",
                "glVertexAttrib1fv", "glVertexAttrib1sv", "glVertexAttrib1dv",
                "glVertexAttrib2fv", "glVertexAttrib2sv", "glVertexAttrib2dv",
                "glVertexAttrib3fv", "glVertexAttrib3sv", "glVertexAttrib3dv",
                "glVertexAttrib4fv", "glVertexAttrib4sv", "glVertexAttrib4dv",
                "glVertexAttrib4iv", "glVertexAttrib4bv", "glVertexAttrib4ubv",
                "glVertexAttrib4usv", "glVertexAttrib4uiv",
                "glVertexAttrib4Nbv", "glVertexAttrib4Nsv", "glVertexAttrib4Niv",
                "glVertexAttrib4Nubv", "glVertexAttrib4Nusv", "glVertexAttrib4Nuiv");

        stub("glVertexAttribI1i", "glVertexAttribI2i", "glVertexAttribI3i", "glVertexAttribI4i",
                "glVertexAttribI1ui", "glVertexAttribI2ui", "glVertexAttribI3ui", "glVertexAttribI4ui",
                "glVertexAttribI1iv", "glVertexAttribI2iv", "glVertexAttribI3iv", "glVertexAttribI4iv",
                "glVertexAttribI1uiv", "glVertexAttribI2uiv", "glVertexAttribI3uiv", "glVertexAttribI4uiv",
                "glVertexAttribI4bv", "glVertexAttribI4sv", "glVertexAttribI4ubv", "glVertexAttribI4usv",
                "glGetTransformFeedbackVarying");

        stub("glVertexAttribP1ui", "glVertexAttribP2ui", "glVertexAttribP3ui", "glVertexAttribP4ui",
                "glVertexAttribP1uiv", "glVertexAttribP2uiv", "glVertexAttribP3uiv", "glVertexAttribP4uiv");
    }
}

