package net.vulkanmod.mixin.compatibility.essential;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.compat.render.GuiEntityRenderState;
import net.vulkanmod.compat.render.RenderStateSnapshot;
import net.vulkanmod.mixin.render.entity.EntityRenderDispatcherAccessor;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@Mixin(targets = "gg.essential.gui.common.UI3DPlayer", remap = false)
public class EssentialUI3DPlayerMixin {
    @Unique
    private static final ThreadLocal<RenderStateSnapshot> vulkanMod$essentialPlayerStateSnapshot = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<Boolean> vulkanMod$essentialPlayerShadowState = new ThreadLocal<>();
    @Unique
    private static final ThreadLocal<ScissorState> vulkanMod$essentialPlayerScissorState = new ThreadLocal<>();

    @Inject(method = "drawPlayer()V", at = @At("HEAD"), require = 0)
    private void vulkanMod$beginEssentialPlayerPreview(CallbackInfo ci) {
        vulkanMod$essentialPlayerStateSnapshot.set(new RenderStateSnapshot());
        GuiEntityRenderState.beginHudEntityPreview();
        GuiEntityRenderState.prepare();
        vulkanMod$beginComponentScissor();

        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        boolean shouldRenderShadow = ((EntityRenderDispatcherAccessor) entityRenderDispatcher).vulkanMod$shouldRenderShadow();
        vulkanMod$essentialPlayerShadowState.set(shouldRenderShadow);
        entityRenderDispatcher.setRenderShadow(false);
    }

    @Inject(method = "drawPlayer()V", at = @At("RETURN"), require = 0)
    private void vulkanMod$endEssentialPlayerPreview(CallbackInfo ci) {
        GuiEntityRenderState.endHudEntityPreview();

        Boolean shouldRenderShadow = vulkanMod$essentialPlayerShadowState.get();
        vulkanMod$essentialPlayerShadowState.remove();
        if (shouldRenderShadow != null) {
            Minecraft.getInstance().getEntityRenderDispatcher().setRenderShadow(shouldRenderShadow);
        }

        vulkanMod$restoreComponentScissor();

        RenderStateSnapshot snapshot = vulkanMod$essentialPlayerStateSnapshot.get();
        vulkanMod$essentialPlayerStateSnapshot.remove();

        if (snapshot != null) {
            snapshot.restore();
        }
    }

    @Unique
    private void vulkanMod$beginComponentScissor() {
        ScissorState parent = new ScissorState(
                Renderer.isScissorEnabled(),
                Renderer.getScissorBox(0),
                Renderer.getScissorBox(1),
                Renderer.getScissorBox(2),
                Renderer.getScissorBox(3));
        vulkanMod$essentialPlayerScissorState.set(parent);



        if (vulkanMod$usesLocalPreviewTarget()) return;

        Window window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        float left = vulkanMod$componentFloat("getLeft");
        float top = vulkanMod$componentFloat("getTop");
        float widthGui = vulkanMod$componentFloat("getWidth");
        float heightGui = vulkanMod$componentFloat("getHeight");
        int x = (int) Math.floor(left * scale);
        int y = (int) Math.floor(window.getHeight() - (top + heightGui) * scale);
        int width = Math.max(0, (int) Math.ceil(widthGui * scale));
        int height = Math.max(0, (int) Math.ceil(heightGui * scale));
        if (parent.enabled()) {
            long right = Math.min((long) x + width, (long) parent.x() + Math.max(0, parent.width()));
            long topEdge = Math.min((long) y + height, (long) parent.y() + Math.max(0, parent.height()));
            x = Math.max(x, parent.x());
            y = Math.max(y, parent.y());
            width = (int) Math.max(0L, right - x);
            height = (int) Math.max(0L, topEdge - y);
        }
        RenderSystem.enableScissor(x, y, width, height);
    }

    @Unique
    private static boolean vulkanMod$usesLocalPreviewTarget() {
        Renderer renderer = Renderer.getInstance();
        if (renderer == null || renderer.getMainPass() == null)
            return false;
        if (renderer.getBoundRenderPass() == null) {

            GlFramebuffer selected = GlFramebuffer.getBoundFramebuffer();
            return selected != null && !selected.usesMainTargetOrientation();
        }
        var target = renderer.getBoundRenderPass().getFramebuffer();
        var mainColor = renderer.getMainPass().getColorAttachment();

        return target != null && mainColor != null && target.getColorAttachment() != mainColor;
    }

    @Unique
    private float vulkanMod$componentFloat(String methodName) {
        try {
            Method method = this.getClass().getMethod(methodName);
            Object value = method.invoke(this);
            return value instanceof Number number ? number.floatValue() : 0.0f;
        } catch (ReflectiveOperationException e) {
            return 0.0f;
        }
    }

    @Unique
    private static void vulkanMod$restoreComponentScissor() {
        ScissorState scissorState = vulkanMod$essentialPlayerScissorState.get();
        vulkanMod$essentialPlayerScissorState.remove();

        if (scissorState == null) {
            return;
        }

        if (scissorState.enabled()) {
            RenderSystem.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
            return;
        }

        Renderer.setScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
        Renderer.setScissorEnabled(false);
    }

    @Unique
    private record ScissorState(boolean enabled, int x, int y, int width, int height) {
    }
}
