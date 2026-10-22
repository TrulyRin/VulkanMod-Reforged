package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.vulkanmod.interfaces.ImmediateBuffer;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.sable.render.SublevelRenderScope;
import net.vulkanmod.compat.sable.veil.VeilProgramDraws;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;

import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BufferUploader.class, priority = 900)
public class BufferUploaderM {
    @Inject(method = "reset", at = @At("HEAD"), cancellable = true)
    private static void reset(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "upload", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;upload(Lcom/mojang/blaze3d/vertex/MeshData;)V"))
    private static void drawWithShader(VertexBuffer buffer, MeshData meshData) {
        ImmediateBuffer immediate = (ImmediateBuffer) buffer;
        boolean previous = immediate.setImmediateUpload(true);
        try {
            buffer.upload(meshData);
        } finally {
            immediate.setImmediateUpload(previous);
        }
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true, order = 2000)
    private static void draw(MeshData meshData, CallbackInfo ci) {
        ci.cancel();

        RenderSystem.assertOnRenderThread();
        try (meshData) {
            MeshData.DrawState parameters = meshData.drawState();
            if (parameters.vertexCount() <= 0) return;
            Renderer renderer = Renderer.getInstance();
            Pipeline pipeline = renderer.getBoundPipeline();
            var scope = SublevelRenderScope.current().orElse(null);
            Object activeShader = scope != null && scope.shader() != null ? scope.shader() : RenderSystem.getShader();
            if (activeShader instanceof ShaderInstance shader && VeilShaderProgramAccess.isWrapper(shader)) {
                GraphicsPipeline owned = VeilProgramDraws.prepare(shader, parameters.format(), true);
                if (owned == null) return;
                try {
                    drawPrepared(renderer, owned, meshData, true);
                } finally {
                    renderer.restoreGraphicsPipeline(pipeline);
                }
                return;
            }
            if (pipeline == null) return;
            if (pipeline instanceof GraphicsPipeline graphics) {
                if (graphics.hasTessellation()) {
                    throw new IllegalStateException("Raw patch draw requires its Veil shader owner");
                }

                VRenderSystem.setPrimitiveTopology(parameters.mode());
                renderer.bindGraphicsPipeline(graphics);
            }
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters, meshData.indexBuffer());
        }
    }

    private static void drawPrepared(Renderer renderer, GraphicsPipeline pipeline, MeshData meshData, boolean programOwned) {
        MeshData.DrawState state = meshData.drawState();
        boolean patches = pipeline.hasTessellation();
        if (patches) Renderer.getDrawer().preparePatches(state, meshData.indexBuffer());
        VRenderSystem.setPrimitiveTopology(state.mode());
        renderer.bindGraphicsPipeline(pipeline);
        if (!programOwned) VTextureSelector.bindShaderTextures(pipeline);
        renderer.uploadAndBindUBOs(pipeline);
        if (patches) {
            Renderer.getDrawer().drawPatches(meshData.vertexBuffer(), state, meshData.indexBuffer() != null);
        } else {
            Renderer.getDrawer().draw(meshData.vertexBuffer(), state, meshData.indexBuffer());
        }
    }

}
