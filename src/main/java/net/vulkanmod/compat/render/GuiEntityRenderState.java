package net.vulkanmod.compat.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL11;

public final class GuiEntityRenderState {
    public static final int FULL_BRIGHT_LIGHT = 15728880;
    private static final ThreadLocal<RenderStateSnapshot> DEFERRED_DRAW_STATE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> HUD_ENTITY_PREVIEW_DEPTH = ThreadLocal.withInitial(() -> 0);

    private GuiEntityRenderState() {
    }

    public static boolean isGuiEntityPreview() {
        return Minecraft.getInstance().screen != null || isHudEntityPreviewActive();
    }

    public static boolean isGuiEntityPreview(int packedLight) {
        return isGuiEntityPreview() && packedLight == 15728880;
    }

    public static void beginHudEntityPreview() {
        HUD_ENTITY_PREVIEW_DEPTH.set(HUD_ENTITY_PREVIEW_DEPTH.get() + 1);
    }

    public static void endHudEntityPreview() {
        int depth = HUD_ENTITY_PREVIEW_DEPTH.get();
        if (depth <= 1) {
            HUD_ENTITY_PREVIEW_DEPTH.remove();
            return;
        }

        HUD_ENTITY_PREVIEW_DEPTH.set(depth - 1);
    }

    public static boolean isHudEntityPreviewActive() {
        return HUD_ENTITY_PREVIEW_DEPTH.get() > 0;
    }

    public static void prepare() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.disableCull();
        VRenderSystem.setPolygonModeGL(GL11.GL_FILL);
    }

    public static void prepareDeferredDraw() {
        if (DEFERRED_DRAW_STATE.get() == null) {
            DEFERRED_DRAW_STATE.set(new RenderStateSnapshot());
        }

        prepare();
    }

    public static boolean hasDeferredDrawState() {
        return DEFERRED_DRAW_STATE.get() != null;
    }

    public static void restoreDeferredDrawState() {
        RenderStateSnapshot snapshot = DEFERRED_DRAW_STATE.get();
        DEFERRED_DRAW_STATE.remove();

        if (snapshot != null) {
            snapshot.restore();
        }
    }
}
