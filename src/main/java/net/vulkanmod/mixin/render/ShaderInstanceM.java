package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.gl.GlEmulationLog;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.shader.parser.GlslConverter;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(value = ShaderInstance.class, priority = 900)
public class ShaderInstanceM implements ShaderMixed {

    @Shadow @Final private Map<String, com.mojang.blaze3d.shaders.Uniform> uniformMap;
    @Shadow @Final private String name;

    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform MODEL_VIEW_MATRIX;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform PROJECTION_MATRIX;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform COLOR_MODULATOR;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform LINE_WIDTH;

    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform GLINT_ALPHA;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_START;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_END;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_COLOR;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform FOG_SHAPE;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform TEXTURE_MATRIX;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform GAME_TIME;
    @Shadow @Final @Nullable public com.mojang.blaze3d.shaders.Uniform SCREEN_SIZE;

    private String vsPath;
    private String fsName;
    private String vulkanBindPath;
    private VertexFormat pipelineFormat;

    private GraphicsPipeline pipeline;
    private final Map<VertexFormat, GraphicsPipeline> variantPipelines = new HashMap<>();
    boolean isLegacy = false;

    public GraphicsPipeline getPipeline() {
        return pipeline;
    }

    public GraphicsPipeline getPipeline(VertexFormat drawFormat) {
        if (this.pipeline == null || drawFormat == null || drawFormat.equals(this.pipelineFormat)) {
            return this.pipeline;
        }

        if (this.isLegacy || this.vulkanBindPath == null) {
            return this.pipeline;
        }

        return this.variantPipelines.computeIfAbsent(drawFormat, this::createPipelineVariant);
    }

    @Inject(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At("RETURN"))
    private void create_loc(ResourceProvider resourceProvider, ResourceLocation location, VertexFormat format, CallbackInfo ci) {
        this.create(resourceProvider, location.toString(), format);
    }

    @Inject(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At("RETURN"))
    private void create_str(ResourceProvider resourceProvider, String name, VertexFormat format, CallbackInfo ci) {
        this.create(resourceProvider, name, format);
    }

    private void create(ResourceProvider resourceProvider, String name, VertexFormat format) {
        String path;
        String namespace = "minecraft";
        String namePath = name;

        if (name.contains(":")) {
            String[] split = name.split(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR), 2);
            namespace = split[0];
            namePath = split.length > 1 ? split[1] : "";
        }

        if (!isUsableShaderPath(namePath)) {
            Initializer.LOGGER.warn("Skipping unsupported shader {}: empty shader path", name);
            return;
        }

        if (name.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
            path = ResourceLocation.fromNamespaceAndPath(namespace, "shaders/core/%s".formatted(namePath)).toString();
        } else {
            path = "shaders/core/%s".formatted(name);
        }
        this.vsPath = path;
        this.fsName = path;
        this.pipelineFormat = format;

        try {
            String bindPath = String.format("%s/core/%s/%s", namespace, namePath, namePath);
            if (!hasVulkanShader(bindPath)) {
                createLegacyShader(resourceProvider, format);
                return;
            }

            this.vulkanBindPath = bindPath;
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, bindPath);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            this.pipeline = pipelineBuilder.createGraphicsPipeline();
        } catch (Exception e) {

            Initializer.LOGGER.error("Error on shader {} creation, attempting conversion fallback", name, e);
            createLegacyShader(resourceProvider, format);
        }
    }

    private static boolean hasVulkanShader(String bindPath) {
        String resourcePath = "/assets/vulkanmod/shaders/%s.json".formatted(bindPath);
        try (InputStream stream = Pipeline.class.getResourceAsStream(resourcePath)) {
            return stream != null;
        } catch (IOException exception) {
            throw new RuntimeException("Failed to inspect Vulkan shader resource: " + resourcePath, exception);
        }
    }

    private GraphicsPipeline createPipelineVariant(VertexFormat drawFormat) {
        try {
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(drawFormat, this.vulkanBindPath);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            return pipelineBuilder.createGraphicsPipeline();
        } catch (Exception e) {
            Initializer.LOGGER.error("Error creating shader {} pipeline variant for draw format {}", this.name, drawFormat, e);
            return this.pipeline;
        }
    }

    private static boolean isUsableShaderPath(String namePath) {
        return namePath != null && !namePath.isBlank();
    }

    @Redirect(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glBindAttribLocation(IILjava/lang/CharSequence;)V"))
    private void bindAttr(int program, int index, CharSequence name) {}

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    public void close(CallbackInfo ci) {
        if (this.pipeline != null)
            this.pipeline.cleanUp();
        this.variantPipelines.values().forEach(variantPipeline -> {
            if (variantPipeline != null && variantPipeline != this.pipeline) {
                variantPipeline.cleanUp();
            }
        });
        this.variantPipelines.clear();
        ci.cancel();
    }

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    public void apply(CallbackInfo ci) {
        ci.cancel();
        if (!this.isLegacy)
            return;

        if (this.MODEL_VIEW_MATRIX != null) {
            this.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        }

        if (this.PROJECTION_MATRIX != null) {
            this.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        }

        if (this.COLOR_MODULATOR != null) {
            this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (this.GLINT_ALPHA != null) {
            this.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        if (this.FOG_START != null) {
            this.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (this.FOG_END != null) {
            this.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (this.FOG_COLOR != null) {
            this.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (this.FOG_SHAPE != null) {
            this.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }

        if (this.TEXTURE_MATRIX != null) {
            this.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (this.GAME_TIME != null) {
            this.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (this.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            this.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }

        if (this.LINE_WIDTH != null) {
            this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    public void clear(CallbackInfo ci) {
        ci.cancel();
    }

    private void setUniformSuppliers(UBO ubo) {

        for (Uniform vUniform : ubo.getUniforms()) {
            com.mojang.blaze3d.shaders.Uniform uniform = this.uniformMap.get(vUniform.getName());

            if (uniform == null) {
                if (vUniform.hasSupplier()) {
                    Initializer.LOGGER.debug("Continuing with global shader uniform supplier for {}", vUniform.getName());
                    continue;
                }
                Initializer.LOGGER.error(String.format("Error: field %s not present in uniform map", vUniform.getName()));
                continue;
            }

            Supplier<MappedBuffer> supplier;
            ByteBuffer byteBuffer;

            if (uniform.getType() <= 3) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
            } else if (uniform.getType() <= 10) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
            } else {
                throw new RuntimeException("out of bounds value for uniform " + uniform);
            }

            MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
            supplier = () -> mappedBuffer;

            vUniform.setSupplier(supplier);
        }

    }

    private void createLegacyShader(ResourceProvider resourceProvider, VertexFormat format) {
        try {
            ResourceLocation jsonLocation = ResourceLocation.tryParse(this.vsPath + ".json");
            Resource resource = resourceProvider.getResourceOrThrow(jsonLocation);
            JsonObject jsonObject;
            try (InputStream inputStream = resource.open()) {
                jsonObject = GsonHelper.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }

            ResourceLocation vertLocation = resolveShaderStageLocation(GsonHelper.getAsString(jsonObject, "vertex"), "vsh");
            ResourceLocation fragLocation = resolveShaderStageLocation(GsonHelper.getAsString(jsonObject, "fragment"), "fsh");

            resource = resourceProvider.getResourceOrThrow(vertLocation);
            InputStream inputStream = resource.open();
            String vshSrc = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            resource = resourceProvider.getResourceOrThrow(fragLocation);
            inputStream = resource.open();
            String fshSrc = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            GlslConverter converter = new GlslConverter();
            converter.setFormat(format);
            Pipeline.Builder builder = new Pipeline.Builder(format, this.name);

            converter.process(vshSrc, fshSrc);
            UBO ubo = converter.getUBO();
            this.setUniformSuppliers(ubo);

            builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());
            builder.compileShaders(this.name, converter.getVshConverted(), converter.getFshConverted());

            this.pipeline = builder.createGraphicsPipeline();
            this.isLegacy = true;

        } catch (Exception e) {
            Initializer.LOGGER.error("Error on shader {} conversion/compilation", this.name, e);
            GlEmulationLog.warnContractGap("shader_conversion", "fallbackShader",
                    "Using generic fallback shader for unsupported GLSL contract {}; shader={}",
                    classifyShaderFailure(e), this.name);
            createExternalFallbackShader(format);
        }
    }

    private static String classifyShaderFailure(Throwable throwable) {
        String message = throwable != null && throwable.getMessage() != null ? throwable.getMessage().toLowerCase(java.util.Locale.ROOT) : "";
        if (message.contains("include")) return "include";
        if (message.contains("uniform")) return "uniform";
        if (message.contains("sampler")) return "sampler";
        if (message.contains("attribute") || message.contains("attrib")) return "attribute";
        if (message.contains("syntax")) return "syntax";
        return "unknown";
    }

    private void createExternalFallbackShader(VertexFormat format) {
        String fallbackPath = fallbackShaderPath(format);
        if (fallbackPath == null) {
            Initializer.LOGGER.warn("No safe Vulkan fallback shader for {} with vertex format {}", this.name, format);
            return;
        }

        try {
            Pipeline.Builder builder = new Pipeline.Builder(format, fallbackPath);
            builder.parseBindingsJSON();
            builder.compileShaders();
            this.pipeline = builder.createGraphicsPipeline();
            this.vulkanBindPath = fallbackPath;
            this.pipelineFormat = format;
            this.isLegacy = false;
            Initializer.LOGGER.warn("Using Vulkan fallback shader {} for external shader {}", fallbackPath, this.name);
        } catch (Exception fallbackException) {
            Initializer.LOGGER.error("Error creating Vulkan fallback shader {} for {}", fallbackPath, this.name, fallbackException);
        }
    }

    private static String fallbackShaderPath(VertexFormat format) {
        if (DefaultVertexFormat.BLIT_SCREEN.equals(format)) {
            return "minecraft/core/blit_screen/blit_screen";
        }

        if (DefaultVertexFormat.PARTICLE.equals(format)) {
            return "minecraft/core/particle/particle";
        }

        if (DefaultVertexFormat.POSITION_TEX_COLOR.equals(format)) {
            return "minecraft/core/position_tex_color/position_tex_color";
        }

        if (DefaultVertexFormat.POSITION_TEX.equals(format)) {
            return "minecraft/core/position_tex/position_tex";
        }

        if (hasAttributes(format, "uc_Position", "uc_UV0")) {
            return "essential/core/uc_position_tex/uc_position_tex";
        }

        if (hasAttributes(format, "uc_Position", "uc_Color")) {
            return "essential/core/uc_position_color/uc_position_color";
        }

        if (hasAttributes(format, "uc_Position")) {
            return "essential/core/uc_position/uc_position";
        }

        if (hasAttributes(format, "Position", "Color", "UV0")) {
            return "minecraft/core/position_color_tex/position_color_tex";
        }

        if (DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP.equals(format)) {
            return "minecraft/core/position_color_tex_lightmap/position_color_tex_lightmap";
        }

        if (hasAttributes(format, "Position", "UV0", "Color", "UV2")) {
            return "minecraft/core/particle/particle";
        }

        if (hasAttributes(format, "Position", "UV0", "Color")) {
            return "minecraft/core/position_tex_color/position_tex_color";
        }

        if (DefaultVertexFormat.POSITION_COLOR_LIGHTMAP.equals(format)) {
            return "minecraft/core/position_color_lightmap/position_color_lightmap";
        }

        if (DefaultVertexFormat.POSITION_COLOR_NORMAL.equals(format)) {
            return "minecraft/core/position_color_normal/position_color_normal";
        }

        if (DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL.equals(format)) {
            return "minecraft/core/position_tex_color_normal/position_tex_color_normal";
        }

        if (DefaultVertexFormat.POSITION_COLOR.equals(format)) {
            return "minecraft/core/position_color/position_color";
        }

        if (DefaultVertexFormat.POSITION.equals(format)) {
            return "minecraft/core/position/position";
        }

        return null;
    }

    private static boolean hasAttributes(VertexFormat format, String... attributes) {
        return format != null && format.getElementAttributeNames().equals(List.of(attributes));
    }

    private static ResourceLocation resolveShaderStageLocation(String stageName, String extension) {
        ResourceLocation stage = ResourceLocation.tryParse(stageName);
        if (stage == null) {
            throw new IllegalArgumentException("Invalid shader stage: " + stageName);
        }

        return ResourceLocation.fromNamespaceAndPath(stage.getNamespace(), "shaders/core/%s.%s".formatted(stage.getPath(), extension));
    }
}

