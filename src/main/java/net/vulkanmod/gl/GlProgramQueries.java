package net.vulkanmod.gl;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;


public final class GlProgramQueries {
    private GlProgramQueries() {}

    public static String decodeName(ByteBuffer source) {
        if (source == null) return null;
        int start = source.position(), end = start;
        while (end < source.limit() && source.get(end) != 0) end++;
        byte[] bytes = new byte[end - start];
        for (int i = 0; i < bytes.length; i++) bytes[i] = source.get(start + i);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String activeName(GlProgram.ActiveVariable variable, int maxLength, IntBuffer size, IntBuffer type) {
        if (variable == null || maxLength < 0) return "";
        put(size, variable.size());
        put(type, variable.type());
        byte[] bytes = variable.name().getBytes(StandardCharsets.UTF_8);
        int count = maxLength == 0 ? 0 : Math.min(bytes.length, maxLength - 1);
        return new String(bytes, 0, count, StandardCharsets.UTF_8);
    }

    public static void activeVariable(GlProgram.ActiveVariable variable, IntBuffer length, IntBuffer size,
                                      IntBuffer type, ByteBuffer name) {
        if (variable == null) return;
        int count = writeName(variable.name(), name);
        put(length, count);
        put(size, variable.size());
        put(type, variable.type());
    }

    public static void activeVariable(GlProgram.ActiveVariable variable, int[] length, int[] size,
                                      int[] type, ByteBuffer name) {
        if (variable == null) return;
        int count = writeName(variable.name(), name);
        put(length, count);
        put(size, variable.size());
        put(type, variable.type());
    }

    public static void activeVariable(GlProgram.ActiveVariable variable, int maxLength,
                                      long length, long size, long type, long name) {
        if (variable == null || maxLength < 0) return;
        int count = 0;
        if (name != 0 && maxLength > 0) {
            byte[] bytes = variable.name().getBytes(StandardCharsets.UTF_8);
            count = Math.min(bytes.length, maxLength - 1);
            for (int i = 0; i < count; i++) MemoryUtil.memPutByte(name + i, bytes[i]);
            MemoryUtil.memPutByte(name + count, (byte) 0);
        }
        if (length != 0) MemoryUtil.memPutInt(length, count);
        if (size != 0) MemoryUtil.memPutInt(size, variable.size());
        if (type != 0) MemoryUtil.memPutInt(type, variable.type());
    }

    private static int writeName(String value, ByteBuffer output) {
        if (output == null || !output.hasRemaining()) return 0;
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        int start = output.position(), count = Math.min(bytes.length, output.remaining() - 1);
        for (int i = 0; i < count; i++) output.put(start + i, bytes[i]);
        output.put(start + count, (byte) 0);
        return count;
    }

    private static void put(IntBuffer output, int value) {
        if (output != null && output.hasRemaining()) output.put(output.position(), value);
    }

    private static void put(int[] output, int value) {
        if (output != null && output.length > 0) output[0] = value;
    }
}
