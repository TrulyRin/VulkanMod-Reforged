package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.compat.external.ExternalTerrainRenderBridge;
import net.vulkanmod.compat.sable.render.ScopedUniformResolver;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.util.function.Supplier;

public class Uniforms {

    public static Object2ReferenceOpenHashMap<String, Supplier<Integer>> vec1i_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static Object2ReferenceOpenHashMap<String, Supplier<Float>> vec1f_uniformMap = new Object2ReferenceOpenHashMap<>();
    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec2f_uniformMap = new Object2ReferenceOpenHashMap<>();
    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec3f_uniformMap = new Object2ReferenceOpenHashMap<>();
    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> vec4f_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static Object2ReferenceOpenHashMap<String, Supplier<MappedBuffer>> mat4f_uniformMap = new Object2ReferenceOpenHashMap<>();

    public static void setupDefaultUniforms() {

        mat4f_uniformMap.put("ModelViewMat", () -> ScopedUniformResolver.buffer("ModelViewMat", VRenderSystem::getModelViewMatrix));
        mat4f_uniformMap.put("ProjMat", VRenderSystem::getProjectionMatrix);
        mat4f_uniformMap.put("MVP", VRenderSystem::getMVP);
        mat4f_uniformMap.put("TextureMat", VRenderSystem::getTextureMatrix);
        if (ExternalRenderPathSupport.isExternalLodBridgeEnabled()) {
            mat4f_uniformMap.put("ExternalLodCombinedMatrix", ExternalTerrainRenderBridge::getCombinedMatrix);
        }

        vec1i_uniformMap.put("EndPortalLayers", () -> 15);
        vec1i_uniformMap.put("FogShape", () -> ScopedUniformResolver.intValue("FogShape", () -> RenderSystem.getShaderFogShape().getIndex()));

        vec1f_uniformMap.put("FogStart", () -> ScopedUniformResolver.floatValue("FogStart", RenderSystem::getShaderFogStart));
        vec1f_uniformMap.put("FogEnd", () -> ScopedUniformResolver.floatValue("FogEnd", RenderSystem::getShaderFogEnd));
        vec1f_uniformMap.put("LineWidth", RenderSystem::getShaderLineWidth);
        vec1f_uniformMap.put("GameTime", RenderSystem::getShaderGameTime);
        vec1f_uniformMap.put("GlintAlpha", RenderSystem::getShaderGlintAlpha);
        vec1f_uniformMap.put("AlphaCutout", () -> VRenderSystem.alphaCutout);
        vec1f_uniformMap.put("SableSkyLightScale", () -> ScopedUniformResolver.floatValue("SableSkyLightScale", () -> 1.0f));
        vec1f_uniformMap.put("SableEnableNormalLighting", () -> ScopedUniformResolver.floatValue("SableEnableNormalLighting", () -> 0.0f));
        vec1f_uniformMap.put("SableShadowsEnabled", () -> ScopedUniformResolver.floatValue("SableShadowsEnabled", () -> 0.0f));

        vec2f_uniformMap.put("ScreenSize", VRenderSystem::getScreenSize);

        vec3f_uniformMap.put("Light0_Direction", () -> VRenderSystem.lightDirection0);
        vec3f_uniformMap.put("Light1_Direction", () -> VRenderSystem.lightDirection1);
        vec3f_uniformMap.put("ChunkOffset", () -> ScopedUniformResolver.buffer("ChunkOffset", () -> VRenderSystem.ChunkOffset));

        vec4f_uniformMap.put("ColorModulator", VRenderSystem::getShaderColor);
        vec4f_uniformMap.put("FogColor", () -> ScopedUniformResolver.buffer("FogColor", VRenderSystem::getShaderFogColor));
        if (ExternalRenderPathSupport.isExternalLodBridgeEnabled()) {
            vec4f_uniformMap.put("ExternalLodModelOffsetAndYOffset", ExternalTerrainRenderBridge::getModelOffsetAndYOffset);
            vec4f_uniformMap.put("ExternalLodRenderParams", ExternalTerrainRenderBridge::getRenderParams);
        }

    }
}
