package net.vulkanmod.compat.opengl;

import org.lwjgl.system.APIUtil;
import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.FFIType;
import org.lwjgl.system.libffi.LibFFI;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class GlTrampolines {
    private static final int DIAGNOSTIC_KEY_LIMIT = 256;

    @FunctionalInterface
    interface Handler {
        void invoke(long ret, long args);
    }

    private static final class Log {
        static final org.apache.logging.log4j.Logger IMPL =
                org.apache.logging.log4j.LogManager.getLogger("VulkanMod-GlEmulation");
    }

    static void logInfo(String message, Object... args) {
        try {
            Log.IMPL.info(message, args);
        } catch (Throwable ignored) {
        }
    }

    static void logWarn(String message, Object... args) {
        try {
            Log.IMPL.warn(message, args);
        } catch (Throwable ignored) {
        }
    }

    static void logError(String message, Object... args) {
        try {
            Log.IMPL.error(message, args);
        } catch (Throwable ignored) {
        }
    }

    private static final Map<String, FFICIF> CIF_CACHE = new HashMap<>();
    private static final List<GlTrampoline> LIVE_TRAMPOLINES = new ArrayList<>();
    private static final Map<String, ByteBuffer> PINNED_STRINGS = new HashMap<>();
    private static final Set<String> FAULTED = ConcurrentHashMap.newKeySet();

    private GlTrampolines() {
    }

    static Set<String> faultedFunctionsSnapshot() {
        return Set.copyOf(new java.util.TreeSet<>(FAULTED));
    }

    static synchronized long create(String name, String shape, Handler handler) {
        GlTrampoline trampoline = new GlTrampoline(cifFor(shape), shape.charAt(0), name, handler);
        LIVE_TRAMPOLINES.add(trampoline);
        return trampoline.address();
    }

    private static FFICIF cifFor(String shape) {
        FFICIF cached = CIF_CACHE.get(shape);
        if (cached != null) {
            return cached;
        }

        int sep = shape.indexOf(':');
        FFIType ret = ffiType(shape.charAt(0));
        String args = sep >= 0 ? shape.substring(sep + 1) : "";
        FFIType[] argTypes = new FFIType[args.length()];
        for (int i = 0; i < args.length(); i++) {
            argTypes[i] = ffiType(args.charAt(i));
        }

        FFICIF cif = APIUtil.apiCreateCIF(LibFFI.FFI_DEFAULT_ABI, ret, argTypes);
        CIF_CACHE.put(shape, cif);
        return cif;
    }

    private static FFIType ffiType(char kind) {
        return switch (kind) {
            case 'V' -> LibFFI.ffi_type_void;
            case 'I', 'i' -> LibFFI.ffi_type_sint32;
            case 'L', 'l' -> LibFFI.ffi_type_sint64;
            case 'P', 'p' -> LibFFI.ffi_type_pointer;
            case 'F', 'f' -> LibFFI.ffi_type_float;
            case 's' -> LibFFI.ffi_type_sint16;
            case 'b' -> LibFFI.ffi_type_uint8;
            case 'd' -> LibFFI.ffi_type_double;
            default -> throw new IllegalArgumentException("unknown shape kind: " + kind);
        };
    }

    private static long slot(long args, int index) {
        return MemoryUtil.memGetAddress(args + (long) index * Long.BYTES);
    }

    static int argI(long args, int index) {
        return MemoryUtil.memGetInt(slot(args, index));
    }

    static long argL(long args, int index) {
        return MemoryUtil.memGetLong(slot(args, index));
    }

    static long argP(long args, int index) {
        return MemoryUtil.memGetAddress(slot(args, index));
    }

    static float argF(long args, int index) {
        return MemoryUtil.memGetFloat(slot(args, index));
    }

    static double argD(long args, int index) {
        return MemoryUtil.memGetDouble(slot(args, index));
    }

    static boolean argB(long args, int index) {
        return MemoryUtil.memGetByte(slot(args, index)) != 0;
    }

    static void retI(long ret, int value) {
        APIUtil.apiClosureRet(ret, value);
    }

    static void retL(long ret, long value) {
        APIUtil.apiClosureRetL(ret, value);
    }

    static void retP(long ret, long value) {
        APIUtil.apiClosureRetP(ret, value);
    }

    static void retF(long ret, float value) {
        APIUtil.apiClosureRet(ret, value);
    }

    static void retB(long ret, boolean value) {
        MemoryUtil.memPutByte(ret, (byte) (value ? 1 : 0));
    }

    static void retD(long ret, double value) {
        APIUtil.apiClosureRet(ret, value);
    }

    static synchronized long pinnedString(String value) {
        ByteBuffer pinned = PINNED_STRINGS.computeIfAbsent(value, v -> MemoryUtil.memUTF8(v, true));
        return MemoryUtil.memAddress(pinned);
    }

    private static final class GlTrampoline extends Callback implements CallbackI {
        private final FFICIF cif;
        private final char retKind;
        private final String name;
        private final Handler handler;

        GlTrampoline(FFICIF cif, char retKind, String name, Handler handler) {
            super(cif);
            this.cif = cif;
            this.retKind = retKind;
            this.name = name;
            this.handler = handler;
        }

        @Override
        public FFICIF getCallInterface() {
            return cif;
        }

        @Override
        public void callback(long ret, long args) {
            try {
                handler.invoke(ret, args);
            } catch (Throwable t) {

                if (!FAULTED.contains(name) && FAULTED.size() < DIAGNOSTIC_KEY_LIMIT && FAULTED.add(name)) {
                    logError("Emulated GL function {} failed; returning default", name, t);
                }
                switch (retKind) {
                    case 'I' -> retI(ret, 0);
                    case 'L' -> retL(ret, 0L);
                    case 'P' -> retP(ret, 0L);
                    case 'F' -> retF(ret, 0.0f);
                    case 'b' -> retB(ret, false);
                    case 'd' -> retD(ret, 0.0);
                    default -> { }
                }
            }
        }
    }
}
