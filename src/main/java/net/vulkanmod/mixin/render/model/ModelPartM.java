package net.vulkanmod.mixin.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.vulkanmod.compat.render.GuiEntityRenderState;
import net.vulkanmod.interfaces.ExtendedVertexBuilder;
import net.vulkanmod.interfaces.ModelPartCubeMixed;
import net.vulkanmod.render.model.CubeModel;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

                                                                                                 
                                                                                             
@Mixin(value = ModelPart.class, priority = 900)
public class ModelPartM {

    @Shadow(remap = false) @Final private List<ModelPart.Cube> f_104212_;

    @Unique
    private final Vector3f vulkanMod$normal = new Vector3f();

              
    @Inject(method = "m_104290_", at = @At("HEAD"), cancellable = true, remap = false)
    private void compile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, float r, float g, float b, float a, CallbackInfo ci) {
        if (GuiEntityRenderState.isGuiEntityPreview(light)) {
            GuiEntityRenderState.prepareDeferredDraw();
            return;                                                                    
        }

        Matrix4f matrix4f = pose.pose();
        Matrix3f matrix3f = pose.normal();

        ExtendedVertexBuilder vertexBuilder = ExtendedVertexBuilder.of(vertexConsumer);

        if (vertexBuilder != null && vertexBuilder.canUseFastVertex()) {
            int packedColor = ColorUtil.RGBA.pack(r, g, b, a);

            for (ModelPart.Cube cube : this.f_104212_) {
                CubeModel cubeModel = ((ModelPartCubeMixed) (Object) cube).getCubeModel();

                ModelPart.Polygon[] polygons = cubeModel.getPolygons();

                cubeModel.transformVertices(matrix4f);

                for (ModelPart.Polygon polygon : polygons) {
                    matrix3f.transform(this.vulkanMod$normal.set(polygon.normal));
                    this.vulkanMod$normal.normalize();

                    int packedNormal = VertexUtil.packNormal(vulkanMod$normal.x(), vulkanMod$normal.y(), vulkanMod$normal.z());

                    for (ModelPart.Vertex vertex : polygon.vertices) {
                        Vector3f pos = vertex.pos;
                        vertexBuilder.vertex(pos.x(), pos.y(), pos.z(), packedColor, vertex.u, vertex.v, overlay, light, packedNormal);
                    }
                }
            }
        } else {
            for (ModelPart.Cube cube : this.f_104212_) {
                CubeModel cubeModel = ((ModelPartCubeMixed) (Object) cube).getCubeModel();

                ModelPart.Polygon[] polygons = cubeModel.getPolygons();

                cubeModel.transformVertices(matrix4f);

                for (ModelPart.Polygon polygon : polygons) {
                    matrix3f.transform(this.vulkanMod$normal.set(polygon.normal));
                    this.vulkanMod$normal.normalize();

                    for (ModelPart.Vertex vertex : polygon.vertices) {
                        Vector3f pos = vertex.pos;
                        vertexConsumer.vertex(pos.x(), pos.y(), pos.z(), r, g, b, a, vertex.u, vertex.v, overlay, light,
                                vulkanMod$normal.x(), vulkanMod$normal.y(), vulkanMod$normal.z());
                    }
                }
            }
        }

        ci.cancel();
    }
}
