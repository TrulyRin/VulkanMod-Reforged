package net.vulkanmod.mixin.compatibility.sable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.render.RenderStateSnapshot;
import net.vulkanmod.compat.sable.render.SublevelRenderScope;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher", remap = false)
public abstract class VanillaSubLevelRenderDispatcherMixin {
    @WrapMethod(method = "renderSectionLayer(Ljava/lang/Iterable;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/ShaderInstance;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;F)V", remap = false, require = 1)
    private void vulkanmod$sectionLayerScope(Iterable<?> levels, RenderType renderType, ShaderInstance shader,
                                            double cameraX, double cameraY, double cameraZ,
                                            Matrix4f modelView, Matrix4f projection, float partialTick,
                                            Operation<Void> original) {
        SublevelRenderScope.Pass pass = renderType == RenderType.translucent()
                ? SublevelRenderScope.Pass.TRANSLUCENT : SublevelRenderScope.Pass.TERRAIN;
        RenderStateSnapshot snapshot = new RenderStateSnapshot();
        try (var scope = SublevelRenderScope.enter(pass, shader, snapshot::restore)) {
            original.call(levels, renderType, shader, cameraX, cameraY, cameraZ, modelView, projection, partialTick);
        }
    }

    @WrapMethod(method = "renderAfterSections(Ljava/lang/Iterable;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;F)V", remap = false, require = 1)
    private void vulkanmod$afterSectionsScope(Iterable<?> levels, double cameraX, double cameraY, double cameraZ,
                                             Matrix4f modelView, Matrix4f projection, float partialTick,
                                             Operation<Void> original) {
        RenderStateSnapshot snapshot = new RenderStateSnapshot();
        try (var scope = SublevelRenderScope.enter(SublevelRenderScope.Pass.AFTER_SECTIONS, null, snapshot::restore)) {
            original.call(levels, cameraX, cameraY, cameraZ, modelView, projection, partialTick);
        }
    }

    @WrapMethod(method = "renderBlockEntities(Ljava/lang/Iterable;Ldev/ryanhcode/sable/sublevel/render/dispatcher/SubLevelRenderDispatcher$BlockEntityRenderer;DDDF)V", remap = false, require = 1)
    private void vulkanmod$blockEntitiesScope(Iterable<?> levels, @Coerce Object blockEntityRenderer,
                                             double cameraX, double cameraY, double cameraZ, float partialTick,
                                             Operation<Void> original) {
        RenderStateSnapshot snapshot = new RenderStateSnapshot();
        try (var scope = SublevelRenderScope.enter(SublevelRenderScope.Pass.BLOCK_ENTITIES, null, snapshot::restore)) {
            original.call(levels, blockEntityRenderer, cameraX, cameraY, cameraZ, partialTick);
        }
    }
}
