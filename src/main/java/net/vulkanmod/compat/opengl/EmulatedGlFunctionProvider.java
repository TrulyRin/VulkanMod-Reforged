package net.vulkanmod.compat.opengl;

import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public final class EmulatedGlFunctionProvider implements FunctionProvider {
    public static final EmulatedGlFunctionProvider INSTANCE = new EmulatedGlFunctionProvider();

    private EmulatedGlFunctionProvider() {
    }

    @Override
    public long getFunctionAddress(ByteBuffer functionName) {
        return GlFunctionRegistry.address(MemoryUtil.memASCII(MemoryUtil.memAddress(functionName)));
    }

    @Override
    public long getFunctionAddress(CharSequence functionName) {
        return GlFunctionRegistry.address(functionName.toString());
    }
}
