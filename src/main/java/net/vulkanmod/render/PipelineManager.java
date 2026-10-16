package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.vulkanmod.compat.external.ExternalRenderPathSupport;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.vulkan.shader.PipelineConfig;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

import java.util.function.Function;


public abstract class PipelineManager {
    private static final String shaderPath = "/assets/vulkanmod/shaders/";
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
        net.vulkanmod.render.vertex.CustomVertexFormat.TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainShaderEarlyZ, terrainShader, fastBlitPipeline, renderScaleBlitPipeline, externalLodPipeline;

    private static Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

    public static void init() {
        if (terrainShader != null) return;
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        try {
            createBasicPipelines();
        } catch (RuntimeException | Error failure) {
            destroyPipelines();
            throw failure;
        }
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    public static void setDefaultShader() {
        setShaderGetter(renderType -> terrainShader);
    }

    private static void createBasicPipelines() {
        terrainShaderEarlyZ = createPipeline("terrain","terrain", "terrain_z", TERRAIN_VERTEX_FORMAT);
        terrainShader = createPipeline("terrain", "terrain", "terrain", TERRAIN_VERTEX_FORMAT);
        fastBlitPipeline = createPipeline("blit", "blit", "blit", CustomVertexFormat.NONE);
        renderScaleBlitPipeline = createPipeline("render_scale_blit", "render_scale_blit", "render_scale_blit", CustomVertexFormat.NONE);
        if (ExternalRenderPathSupport.shouldCreateExternalLodPipeline()) {
            externalLodPipeline = createPipeline("external_lod", "lod", "lod", CustomVertexFormat.EXTERNAL_LOD);
        }
    }

    private static GraphicsPipeline createPipeline(String baseName, String vertName, String fragName, VertexFormat vertexFormat) {
                                                                                                
                                                                                                 
        String base = "%sbasic".formatted(ShaderLoadUtil.SHADERS_PATH);
        String dir = "%s/%s".formatted(base, baseName);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(vertexFormat, "basic/%s/%s".formatted(baseName, baseName));
        pipelineBuilder.applyConfig(PipelineConfig.fromJson(baseName, ShaderLoadUtil.getJsonConfig(base, baseName)));

        pipelineBuilder.setShaderSrc(SPIRVUtils.ShaderKind.VERTEX_SHADER,
                                     ShaderLoadUtil.getShaderSource(dir, "%s.vsh".formatted(vertName)));
        pipelineBuilder.setShaderSrc(SPIRVUtils.ShaderKind.FRAGMENT_SHADER,
                                     ShaderLoadUtil.getShaderSource(dir, "%s.fsh".formatted(fragName)));

        return pipelineBuilder.createGraphicsPipeline();
    }

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        init();
        return shaderGetter.apply(renderType);
    }

    public static void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static GraphicsPipeline getTerrainDirectShader(ChunkSectionLayer renderType) {
        return terrainShader;
    }

    public static GraphicsPipeline getTerrainIndirectShader(ChunkSectionLayer renderType) {
        return terrainShaderEarlyZ;
    }

    public static GraphicsPipeline getFastBlitPipeline() { init(); return fastBlitPipeline; }

    public static GraphicsPipeline getRenderScaleBlitPipeline() { init(); return renderScaleBlitPipeline; }

    public static GraphicsPipeline getExternalLodPipeline() { init(); return externalLodPipeline; }

    public static void reload() {
        if (terrainShader == null) return;
        destroyPipelines();
        init();
    }

    public static void destroyPipelines() {
        for (GraphicsPipeline pipeline : new GraphicsPipeline[]{terrainShaderEarlyZ, terrainShader,
                fastBlitPipeline, renderScaleBlitPipeline, externalLodPipeline}) {
            if (pipeline != null) pipeline.scheduleCleanUp();
        }
        terrainShaderEarlyZ = null;
        terrainShader = null;
        fastBlitPipeline = null;
        renderScaleBlitPipeline = null;
        externalLodPipeline = null;
        shaderGetter = null;
    }
}
