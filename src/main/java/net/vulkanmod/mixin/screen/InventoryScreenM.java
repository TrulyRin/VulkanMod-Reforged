package net.vulkanmod.mixin.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.vulkanmod.compat.render.RenderStateSnapshot;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenM {
    @Unique
    private static final ThreadLocal<RenderStateSnapshot> vulkanMod$inventoryEntityStateSnapshot = new ThreadLocal<>();

    @Inject(method = "renderEntityInInventory", at = @At("HEAD"))
    private static void vulkanMod$beginInventoryEntityRenderStateBoundary(GuiGraphics guiGraphics, float x, float y, float scale,
                                                                          Vector3f translate, Quaternionf pose,
                                                                          @Nullable Quaternionf cameraOrientation,
                                                                          LivingEntity entity, CallbackInfo ci) {
        vulkanMod$inventoryEntityStateSnapshot.set(new RenderStateSnapshot());
        vulkanMod$prepareInventoryEntityState();
    }

    @Inject(method = "renderEntityInInventory", at = @At("RETURN"))
    private static void vulkanMod$endInventoryEntityRenderStateBoundary(GuiGraphics guiGraphics, float x, float y, float scale,
                                                                        Vector3f translate, Quaternionf pose,
                                                                        @Nullable Quaternionf cameraOrientation,
                                                                        LivingEntity entity, CallbackInfo ci) {
        RenderStateSnapshot snapshot = vulkanMod$inventoryEntityStateSnapshot.get();
        vulkanMod$inventoryEntityStateSnapshot.remove();

        if (snapshot != null) {
            snapshot.restore();
        }
    }

    @Unique
    private static void vulkanMod$prepareInventoryEntityState() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        VRenderSystem.cullFace(GL11.GL_BACK);
        VRenderSystem.frontFace(GL11.GL_CCW);
        VRenderSystem.setPolygonModeGL(GL11.GL_FILL);
    }
}
