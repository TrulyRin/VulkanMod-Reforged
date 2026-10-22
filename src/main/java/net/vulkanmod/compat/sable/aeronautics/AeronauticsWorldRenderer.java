package net.vulkanmod.compat.sable.aeronautics;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.gl.GlPatchState;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.util.StaticQueue;
import org.joml.Matrix4f;


public final class AeronauticsWorldRenderer {
    private AeronauticsWorldRenderer() {}

    public static void renderBase(RenderType layer, StaticQueue<ChunkArea> areas,
            double cameraX, double cameraY, double cameraZ, Matrix4f modelView, Matrix4f projection) {
        if (!AeronauticsWorldLayers.isEnabled() || !AeronauticsWorldLayers.isBase(layer)
                || !hasGeometry(areas)) return;
        RenderSystem.assertOnRenderThread();
        ShaderInstance shader = AeronauticsWorldLayers.shader();
        if (shader == null) return;
        int owner = VeilShaderProgramAccess.programId(shader);
        var conversion = GlProgram.convertedProgram(owner);
        if (conversion == null) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        AeronauticsMaterialState before = AeronauticsMaterialState.capture(conversion.samplers().size());
        Uniform offset = null;
        boolean layerEntered = false;
        Throwable failure = null;
        try {
            shader.apply();
            AeronauticsWorldLayers.prepare(shader, cameraX, cameraY, cameraZ);
            layerEntered = true;
            layer.setupRenderState();
            GlPatchState.patchParameteri(0x8E72, 4);
            if (RenderSystem.getShader() != shader || GlProgram.currentProgramId() != owner) {
                throw new IllegalStateException("Aeronautics base layer selected a different material owner");
            }


            shader.setDefaultUniforms(VertexFormat.Mode.QUADS, modelView, projection, client.getWindow());
            shader.safeGetUniform("layerIndex").set(0);
            shader.safeGetUniform("time").set((client.level.getGameTime() % 100000)
                    + client.getTimer().getGameTimeDeltaPartialTick(true));
            shader.safeGetUniform("SableEnableNormalLighting").set(0.0f);
            shader.safeGetUniform("SableSkyLightScale").set(1.0f);
            offset = shader.getUniform("ChunkOffset");
            if (offset == null) throw new IllegalStateException("Aeronautics material has no ChunkOffset uniform");
            for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
                var sections = areas.get(areaIndex).sectionQueue;
                for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                    var section = sections.get(sectionIndex);
                    var vbo = section.getBlockLayerVbo();
                    if (vbo == null) continue;
                    offset.set((float) (section.xOffset() - cameraX),
                            (float) (section.yOffset() - cameraY), (float) (section.zOffset() - cameraZ));
                    vbo.drawWithShader(modelView, projection, shader);
                }
            }
        } catch (RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            Throwable cleanup = failure;
            try { if (offset != null) offset.set(0.0f, 0.0f, 0.0f); }
            catch (RuntimeException | Error error) { cleanup = append(cleanup, error); }
            try { shader.clear(); }
            catch (RuntimeException | Error error) { cleanup = append(cleanup, error); }
            try { if (layerEntered) layer.clearRenderState(); }
            catch (RuntimeException | Error error) { cleanup = append(cleanup, error); }
            cleanup = before.restore(cleanup);
            if (failure == null && cleanup != null) rethrow(cleanup);
        }
    }

    private static boolean hasGeometry(StaticQueue<ChunkArea> areas) {
        if (areas == null) return false;
        for (int areaIndex = 0; areaIndex < areas.size(); areaIndex++) {
            var sections = areas.get(areaIndex).sectionQueue;
            for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                if (sections.get(sectionIndex).getBlockLayerVbo() != null) return true;
            }
        }
        return false;
    }

    private static Throwable append(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) throw error;
        throw (RuntimeException) failure;
    }

}
