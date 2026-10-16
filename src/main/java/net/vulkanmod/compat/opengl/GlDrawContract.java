package net.vulkanmod.compat.opengl;

import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.gl.GlEmulationLog;

public final class GlDrawContract {
    private GlDrawContract() {
    }

    public static void drawArrays(int mode, int first, int count) {
        if (GlDrawOptions.shouldPreserveLegacyBridge()) {
            ExternalTerrainRenderBridge.drawArrays(mode, first, count);
            return;
        }

        warnUnsupported("glDrawArrays", mode, count);
    }

    public static void drawElements(int mode, int count, int type, long indices) {
        if (GlDrawOptions.shouldPreserveLegacyBridge()) {
            ExternalTerrainRenderBridge.drawElements(mode, count, type, indices);
            return;
        }

        warnUnsupported("glDrawElements", mode, count);
    }

    public static void onBufferDeleted(int id) {
        if (GlDrawOptions.shouldPreserveLegacyBridge()) {
            ExternalTerrainRenderBridge.onBufferDeleted(id);
        }
    }

    private static void warnUnsupported(String function, int mode, int count) {
        GlEmulationLog.warnContractGap("draw_path", function,
                "{} generic Vulkan submission is not implemented for mode 0x{} count {}; dropping draw safely",
                function, Integer.toHexString(mode), count);
    }
}
