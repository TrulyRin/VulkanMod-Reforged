package net.vulkanmod.gl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.GL_VERTEX_ATTRIB_ARRAY_DIVISOR;

public final class GlVertexArray {

    private static final int MAX_ATTRIBUTES = 16;
    private static int nextId = 1;
    private static int boundId;
    private static final IntOpenHashSet ARRAYS = new IntOpenHashSet();
    private static final Int2ObjectOpenHashMap<ArrayState> STATES = new Int2ObjectOpenHashMap<>();

    public record Attribute(int location, int size, int type, boolean normalized, boolean integer,
                            int stride, long offset, GlBuffer buffer, int divisor) {
    }

    private static final class ArrayState {
        final Int2ObjectOpenHashMap<AttributeState> attributes = new Int2ObjectOpenHashMap<>();
        GlBuffer elementBuffer;
    }

    private static final class AttributeState {
        boolean enabled;
        int size = 4;
        int type = GL_FLOAT;
        boolean normalized;
        boolean integer;
        int stride;
        long offset;
        GlBuffer buffer;
        int divisor;

        Attribute snapshot(int location) {
            return new Attribute(location, size, type, normalized, integer, stride, offset, buffer, divisor);
        }
    }

    private GlVertexArray() {
    }

    public static int genVertexArray() {
        while (ARRAYS.contains(nextId)) nextId++;
        int id = nextId++;
        ARRAYS.add(id);
        return id;
    }

    public static void genVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            arrays.put(i, genVertexArray());
        }
    }

    public static void bindVertexArray(int id) {
        if (id != 0 && !ARRAYS.contains(id)) {
            ARRAYS.add(id);
        }
        boundId = id;
    }

    public static void deleteVertexArray(int id) {
        if (id == 0) return;
        ARRAYS.remove(id);
        STATES.remove(id);
        if (boundId == id) {
            boundId = 0;
        }
    }

    public static void deleteVertexArrays(IntBuffer arrays) {
        for (int i = arrays.position(); i < arrays.limit(); i++) {
            deleteVertexArray(arrays.get(i));
        }
    }

    public static boolean isVertexArray(int id) {
        return id == 0 || ARRAYS.contains(id);
    }

    public static int boundId() {
        return boundId;
    }


    public static List<Attribute> attributes() {
        ArrayState state = STATES.get(boundId);
        if (state == null) return List.of();
        List<Attribute> attributes = new ArrayList<>();
        for (int index = 0; index < MAX_ATTRIBUTES; index++) {
            AttributeState attribute = state.attributes.get(index);
            if (attribute != null && attribute.enabled) attributes.add(attribute.snapshot(index));
        }
        return List.copyOf(attributes);
    }

    public static GlBuffer elementBuffer() {
        ArrayState state = STATES.get(boundId);
        return state == null ? null : state.elementBuffer;
    }

    static void bindElementBuffer(GlBuffer buffer) {
        state(boundId).elementBuffer = buffer;
    }

    static void onBufferDeleted(GlBuffer buffer) {
        for (ArrayState state : STATES.values()) {
            if (state.elementBuffer == buffer) state.elementBuffer = null;
            for (AttributeState attribute : state.attributes.values()) {
                if (attribute.buffer == buffer) attribute.buffer = null;
            }
        }
    }

    public static void enableVertexAttribArray(int index) {
        if (validIndex(index)) attribute(boundId, index).enabled = true;
    }

    public static void disableVertexAttribArray(int index) {
        if (validIndex(index)) attribute(boundId, index).enabled = false;
    }

    public static void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long offset) {
        definePointer(index, size, type, normalized, false, stride, offset);
    }

    public static void vertexAttribIPointer(int index, int size, int type, int stride, long offset) {
        definePointer(index, size, type, false, true, stride, offset);
    }

    private static void definePointer(int index, int size, int type, boolean normalized, boolean integer,
                                      int stride, long offset) {
        if (!validIndex(index)) return;
        if (size < 1 || size > 4 || stride < 0 || offset < 0 || !validType(type, integer)) {
            GlEmulationLog.warnContractGap("draw_path", "vertex_attribute_format",
                    "Invalid or unsupported vertex attribute format; pointer definition skipped");
            return;
        }
        AttributeState attribute = attribute(boundId, index);
        attribute.size = size;
        attribute.type = type;
        attribute.normalized = normalized;
        attribute.integer = integer;
        attribute.stride = stride;
        attribute.offset = offset;

        attribute.buffer = GlBuffer.getArrayBufferBound();
    }

    public static void unsupportedClientPointer() {
        GlEmulationLog.warnContractGap("draw_path", "client_vertex_attribute_pointer",
                "Client-memory vertex attribute pointers are unsupported; pointer definition skipped");
    }

    private static boolean validType(int type, boolean integer) {
        return switch (type) {
            case GL_BYTE, GL_UNSIGNED_BYTE, GL_SHORT, GL_UNSIGNED_SHORT, GL_INT, GL_UNSIGNED_INT -> true;
            case GL_FLOAT, GL_DOUBLE, GL_HALF_FLOAT, 0x140C                -> !integer;
            default -> false;
        };
    }

    private static boolean validIndex(int index) {
        if (index >= 0 && index < MAX_ATTRIBUTES) return true;
        GlEmulationLog.warnContractGap("draw_path", "vertex_attribute_index",
                "Vertex attribute index {} is outside the supported range 0..{}; operation skipped",
                index, MAX_ATTRIBUTES - 1);
        return false;
    }

    private static ArrayState state(int array) {
        return STATES.computeIfAbsent(array, ignored -> new ArrayState());
    }

    private static AttributeState attribute(int array, int index) {
        return state(array).attributes.computeIfAbsent(index, ignored -> new AttributeState());
    }

    public static void vertexAttribDivisor(int index, int divisor) {
        vertexArrayVertexAttribDivisor(boundId, index, divisor);
    }

    public static void vertexArrayVertexAttribDivisor(int array, int index, int divisor) {
        if (array != 0 && !ARRAYS.contains(array)) return;
        if (!validIndex(index)) return;
        if (divisor < 0) {
            GlEmulationLog.warnContractGap("draw_path", "vertex_attribute_divisor",
                    "Vertex attribute divisor exceeds the supported signed range; operation skipped");
            return;
        }
        attribute(array, index).divisor = divisor;
    }

    public static int getVertexAttribDivisor(int index) {
        return getVertexAttribi(index, GL_VERTEX_ATTRIB_ARRAY_DIVISOR);
    }

    public static int getVertexAttribi(int index, int pname) {
        if (!validIndex(index)) return 0;
        ArrayState state = STATES.get(boundId);
        AttributeState attribute = state == null ? null : state.attributes.get(index);
        return switch (pname) {
            case GL_VERTEX_ATTRIB_ARRAY_ENABLED -> attribute != null && attribute.enabled ? 1 : 0;
            case GL_VERTEX_ATTRIB_ARRAY_SIZE -> attribute == null ? 4 : attribute.size;
            case GL_VERTEX_ATTRIB_ARRAY_TYPE -> attribute == null ? GL_FLOAT : attribute.type;
            case GL_VERTEX_ATTRIB_ARRAY_NORMALIZED -> attribute != null && attribute.normalized ? 1 : 0;
            case GL_VERTEX_ATTRIB_ARRAY_INTEGER -> attribute != null && attribute.integer ? 1 : 0;
            case GL_VERTEX_ATTRIB_ARRAY_STRIDE -> attribute == null ? 0 : attribute.stride;
            case GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING -> attribute == null || attribute.buffer == null ? 0 : attribute.buffer.getId();
            case GL_VERTEX_ATTRIB_ARRAY_DIVISOR -> attribute == null ? 0 : attribute.divisor;
            default -> 0;
        };
    }

    public static long getVertexAttribPointer(int index, int pname) {
        if (!validIndex(index) || pname != GL_VERTEX_ATTRIB_ARRAY_POINTER) return 0L;
        ArrayState state = STATES.get(boundId);
        AttributeState attribute = state == null ? null : state.attributes.get(index);
        return attribute == null ? 0L : attribute.offset;
    }
}
