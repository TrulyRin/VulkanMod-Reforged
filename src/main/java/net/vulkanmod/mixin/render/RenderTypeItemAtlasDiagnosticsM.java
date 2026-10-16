package net.vulkanmod.mixin.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.vulkanmod.render.gui.InventoryAtlasDiagnostics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderType.class)
public abstract class RenderTypeItemAtlasDiagnosticsM {
    @Shadow public abstract RenderPipeline pipeline();

    @WrapMethod(method = "draw")
    private void vulkanmod$observeItemAtlasDraw(MeshData mesh, Operation<Void> original) {
        if (!InventoryAtlasDiagnostics.shouldObserveDraw()) {
            original.call(mesh);
            return;
        }
        InventoryAtlasDiagnostics.DrawObservation observation = InventoryAtlasDiagnostics.describeDraw(this.pipeline(), mesh);
        try {
            original.call(mesh);
            InventoryAtlasDiagnostics.finishDraw(observation, true);
        } catch (RuntimeException | Error failure) {
            InventoryAtlasDiagnostics.finishDraw(observation, false);
            throw failure;
        }
    }
}
