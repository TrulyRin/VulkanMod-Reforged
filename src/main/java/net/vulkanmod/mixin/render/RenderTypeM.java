package net.vulkanmod.mixin.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.compat.render.MaterialCullState;
import net.vulkanmod.interfaces.ExtendedRenderType;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderType.class)
public class RenderTypeM implements ExtendedRenderType {
    TerrainRenderType terrainRenderType;

    @WrapMethod(method = "draw")
    private void vulkanMod$materialCullScope(MeshData mesh, Operation<Void> original) {
        int previous = MaterialCullState.beginDraw(mesh);
        try {
            original.call(mesh);
        } finally {
            MaterialCullState.endDraw(previous);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void inj(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2, CallbackInfo ci) {
        terrainRenderType = switch (string) {
            case "solid" -> TerrainRenderType.SOLID;
            case "cutout" -> TerrainRenderType.CUTOUT;
            case "cutout_mipped" -> TerrainRenderType.CUTOUT_MIPPED;
            case "translucent" -> TerrainRenderType.TRANSLUCENT;
            case "tripwire" -> TerrainRenderType.TRIPWIRE;
            default -> null;
        };
    }

    @Override
    public TerrainRenderType getTerrainRenderType() {
        return terrainRenderType;
    }
}
