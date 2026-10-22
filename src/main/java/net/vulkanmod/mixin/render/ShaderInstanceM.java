package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.CanonicalBlitResources;
import net.vulkanmod.render.CanonicalParticleResources;
import net.vulkanmod.render.ProviderShaderResources;
import net.vulkanmod.gl.GlEmulationLog;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.compat.sable.veil.VeilProgramPipelines;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.LegacyProjectionSupplier;
import net.vulkanmod.vulkan.shader.CustomUniformSource;
import net.vulkanmod.vulkan.shader.LegacyUniformSupplier;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.shader.parser.GlslConverter;
import net.vulkanmod.vulkan.shader.parser.LegacyUniformDefaults;
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
    @Shadow @Final private Map<String, Object> samplerMap;
    @Shadow @Final private String name;
    @Shadow @Final private int programId;

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
    private String legacyVertexSource;
    private String legacyFragmentSource;

    private GraphicsPipeline pipeline;
    private final Map<VertexFormat, GraphicsPipeline> variantPipelines = new HashMap<>();
    private final Map<VertexFormat, GraphicsPipeline> rawPipelines = new HashMap<>();
    private net.vulkanmod.vulkan.shader.RawShaderBytecode rawBytecode;
    private String rawBindingPath;
    private boolean rawOriginsPrepared;
    private final Map<VertexFormat, GraphicsPipeline> sablePipelines = new HashMap<>();
    private final Map<String, UniformBufferView> uniformBuffers = new HashMap<>();

    private record UniformBufferView(com.mojang.blaze3d.shaders.Uniform owner, MappedBuffer buffer) {}
    boolean isLegacy = false;
    private GraphicsPipeline admittedBlitCopy;
    private GraphicsPipeline admittedBlitLegacy;
    private int admittedBlitProgram;
    private boolean blitClosed;

    @Override
    public BlitContract blitContract() {
        if (this.blitClosed || this.pipeline == null || VeilShaderProgramAccess.isWrapper(this)) return BlitContract.UNSUPPORTED;
        if (this.pipeline == this.admittedBlitCopy && this.programId == this.admittedBlitProgram) return BlitContract.CANONICAL_COPY;
        return this.isLegacy && this.pipeline == this.admittedBlitLegacy ? BlitContract.LEGACY : BlitContract.UNSUPPORTED;
    }

    public GraphicsPipeline getPipeline() {
        if (VeilShaderProgramAccess.isWrapper(this)) return VeilProgramPipelines.getPipeline(this, null);
        return pipeline;
    }

    public GraphicsPipeline getPipeline(VertexFormat drawFormat) {
        if (VeilShaderProgramAccess.isWrapper(this)) return VeilProgramPipelines.getPipeline(this, drawFormat);
        if (this.pipeline == null || drawFormat == null || drawFormat.equals(this.pipelineFormat)) {
            return this.pipeline;
        }

        if (!this.isLegacy && this.vulkanBindPath == null) {
            return this.pipeline;
        }

        return this.variantPipelines.computeIfAbsent(drawFormat, this::createPipelineVariant);
    }

    @Override
    public GraphicsPipeline getRawPipeline(VertexFormat drawFormat) {
        if (this.blitClosed || this.pipeline == null) return null;
        if (VeilShaderProgramAccess.isWrapper(this)) return getPipeline(drawFormat);
        if (this.isLegacy) {
            if (this.pipeline.getImageDescriptors().isEmpty()) return getPipeline(drawFormat);
            VertexFormat format = drawFormat == null ? this.pipelineFormat : drawFormat;
            if (format == null) return null;
            return this.rawPipelines.computeIfAbsent(format,
                    actual -> createLegacyPipeline(actual, this.legacyVertexSource, this.legacyFragmentSource, true));
        }
        VertexFormat format = drawFormat == null ? this.pipelineFormat : drawFormat;
        if (format == null || this.rawBytecode == null || this.rawBindingPath == null) return null;
        return this.rawPipelines.computeIfAbsent(format, actual -> {
            Pipeline.Builder builder = new Pipeline.Builder(actual, this.rawBindingPath);
            builder.parseBindingsJSON();
            net.vulkanmod.vulkan.shader.RawShaderUniforms.bind(builder.uniformBuffers(), this.uniformMap);
            if (!this.rawOriginsPrepared) {
                this.rawBytecode = this.rawBytecode.withSamplerOrigins(builder, this.uniformMap.keySet());
                this.rawOriginsPrepared = true;
            }
            return this.rawBytecode.create(builder);
        });
    }

    @Override
    public int samplerTextureId(Pipeline drawPipeline, String samplerName) {
        if (drawPipeline == null || (drawPipeline != this.pipeline
                && !this.variantPipelines.containsValue(drawPipeline)
                && !this.rawPipelines.containsValue(drawPipeline))) {
            return -1;
        }

        Object sampler = this.samplerMap.get(samplerName);
        if (sampler instanceof RenderTarget target) {
            return target.getColorTextureId();
        }
        if (sampler instanceof AbstractTexture texture) {
            return texture.getId();
        }
        if (sampler instanceof Integer textureId) {
            return textureId;
        }
        return -1;
    }

    @Override
    public MappedBuffer uniformBuffer(String uniformName) {
        if (VeilShaderProgramAccess.isWrapper(this)) {

            return GlProgram.uniformBuffer(VeilShaderProgramAccess.programId(this), uniformName);
        }
        com.mojang.blaze3d.shaders.Uniform uniform = this.uniformMap.get(uniformName);
        if (uniform == null) {
            this.uniformBuffers.remove(uniformName);
            return null;
        }
        if (uniform instanceof CustomUniformSource source) {
            this.uniformBuffers.remove(uniformName);
            return source.view();
        }
        UniformBufferView cached = this.uniformBuffers.get(uniformName);
        if (cached != null && cached.owner() == uniform) {
            return cached.buffer();
        }


        ByteBuffer buffer;
        if (uniform.getType() >= 0 && uniform.getType() <= 3) {
            buffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
        } else if (uniform.getType() >= 4 && uniform.getType() <= 10) {
            buffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
        } else {
            this.uniformBuffers.remove(uniformName);
            return null;
        }
        MappedBuffer mapped = MappedBuffer.createFromBuffer(buffer);
        this.uniformBuffers.put(uniformName, new UniformBufferView(uniform, mapped));
        return mapped;
    }

    @Override
    public MappedBuffer uploadUniformBuffer(String uniformName) {
        com.mojang.blaze3d.shaders.Uniform uniform = this.uniformMap.get(uniformName);
        if (uniform instanceof CustomUniformSource source) {
            LegacyUniformSupplier.requireSupported(source.descriptor(), uniformName);
            return source.uploadBuffer();
        }
        return uniformBuffer(uniformName);
    }

    @Override
    public MappedBuffer uploadUniformBuffer(String uniformName, String type) {
        com.mojang.blaze3d.shaders.Uniform uniform = this.uniformMap.get(uniformName);
        if (uniform instanceof CustomUniformSource source) {
            LegacyUniformSupplier.requireMatches(source.descriptor(), type, 0, uniformName);
            return source.uploadBuffer();
        }
        return uploadUniformBuffer(uniformName);
    }

    @Override
    public GraphicsPipeline getSablePipeline(VertexFormat drawFormat) {
        if (drawFormat == null) {
            return null;
        }
        return this.sablePipelines.computeIfAbsent(drawFormat, format -> {
            try {
                Pipeline.Builder builder = new Pipeline.Builder(format,
                        "vulkanmod/compat/sable/terrain/terrain");
                builder.parseBindingsJSON();
                builder.compileShaders();
                return builder.createGraphicsPipeline();
            } catch (Exception exception) {
                Initializer.LOGGER.error("Unable to create the Sable terrain pipeline for shader {}", this.name,
                        exception);
                return null;
            }
        });
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
        this.admittedBlitCopy = null;
        this.admittedBlitLegacy = null;
        this.blitClosed = false;
        this.rawBytecode = null;
        this.rawBindingPath = null;
        this.rawOriginsPrepared = false;
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

        ProviderShaderResources selectedParticle = null;
        if ("minecraft".equals(namespace) && "particle".equals(namePath)) {
            try {
                selectedParticle = ProviderShaderResources.load(resourceProvider, ResourceLocation.parse(path + ".json"));
            } catch (Exception failure) {
                throw new IllegalStateException("Unable to load selected particle shader resources", failure);
            }
            if (!CanonicalParticleResources.matches(selectedParticle)) {
                createLegacyShader(resourceProvider, format, selectedParticle);
                return;
            }
        }

        try {
            String bindPath = String.format("%s/core/%s/%s", namespace, namePath, namePath);
            boolean blitCandidate = "minecraft".equals(namespace) && "blit_screen".equals(namePath);
            boolean canonicalBlit = blitCandidate && DefaultVertexFormat.BLIT_SCREEN.equals(format)
                    && CanonicalBlitResources.matches(resourceProvider);
            if ((blitCandidate && !canonicalBlit) || !hasVulkanShader(bindPath)) {
                createLegacyShader(resourceProvider, format, selectedParticle);
                return;
            }

            this.vulkanBindPath = bindPath;
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, bindPath);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            var compiledRaw = pipelineBuilder.snapshotGraphicsBytecode();
            this.pipeline = pipelineBuilder.createGraphicsPipeline();
            this.rawBytecode = compiledRaw;
            this.rawBindingPath = bindPath;
            this.isLegacy = false;
            if (canonicalBlit && this.pipeline != null) {
                this.admittedBlitCopy = this.pipeline;
                this.admittedBlitProgram = this.programId;
            }
        } catch (Exception e) {

            Initializer.LOGGER.error("Error on shader {} creation, attempting conversion fallback", name, e);
            createLegacyShader(resourceProvider, format, selectedParticle);
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
        if (this.isLegacy) {
            try {
                return createLegacyPipeline(drawFormat, this.legacyVertexSource, this.legacyFragmentSource);
            } catch (Exception failure) {

                throw new IllegalStateException("Unable to create legacy shader " + this.name
                        + " for draw format " + drawFormat, failure);
            }
        }
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
        this.blitClosed = true;
        this.rawPipelines.values().forEach(raw -> { if (raw != null) raw.cleanUp(); });
        this.rawPipelines.clear();
        this.rawBytecode = null;
        this.rawBindingPath = null;
        this.rawOriginsPrepared = false;
        this.admittedBlitCopy = null;
        this.admittedBlitLegacy = null;
        if (this.pipeline != null)
            this.pipeline.cleanUp();
        this.variantPipelines.values().forEach(variantPipeline -> {
            if (variantPipeline != null && variantPipeline != this.pipeline) {
                variantPipeline.cleanUp();
            }
        });
        this.variantPipelines.clear();
        this.legacyVertexSource = null;
        this.legacyFragmentSource = null;
        this.sablePipelines.values().forEach(sablePipeline -> {
            if (sablePipeline != null && sablePipeline != this.pipeline) {
                sablePipeline.cleanUp();
            }
        });
        this.sablePipelines.clear();
        this.uniformBuffers.clear();
        ci.cancel();
    }

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    public void apply(CallbackInfo ci) {
        ci.cancel();

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
                    Initializer.LOGGER.debug("Continuing with resolved legacy shader uniform supplier for {}", vUniform.getName());
                    continue;
                }
                throw new IllegalArgumentException("No owner or default for legacy shader uniform " + vUniform.getName());
            }

            String uniformName = vUniform.getName();
            Supplier<MappedBuffer> supplier = new LegacyUniformSupplier(vUniform,
                    () -> this.uniformMap.get(uniformName), () -> uniformBuffer(uniformName));
            if ("ProjMat".equals(uniformName) && vUniform.getSize() == 16 && "mat4".equals(vUniform.getType())) {
                vUniform.setSupplier(LegacyProjectionSupplier.fromSource(supplier));
            } else {
                vUniform.setSupplier(supplier);
            }
        }

    }

    private void createLegacyShader(ResourceProvider resourceProvider, VertexFormat format, @Nullable ProviderShaderResources selected) {
        try {
            String vshSrc;
            String fshSrc;
            if (selected != null) {
                vshSrc = selected.vertexSource();
                fshSrc = selected.fragmentSource();
            } else {
                ResourceLocation jsonLocation = ResourceLocation.tryParse(this.vsPath + ".json");
                Resource resource = resourceProvider.getResourceOrThrow(jsonLocation);
                JsonObject jsonObject;
                try (InputStream inputStream = resource.open()) {
                    jsonObject = GsonHelper.parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                }

                ResourceLocation vertLocation = resolveShaderStageLocation(GsonHelper.getAsString(jsonObject, "vertex"), "vsh");
                ResourceLocation fragLocation = resolveShaderStageLocation(GsonHelper.getAsString(jsonObject, "fragment"), "fsh");

                try (InputStream input = resourceProvider.getResourceOrThrow(vertLocation).open()) {
                    vshSrc = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                try (InputStream input = resourceProvider.getResourceOrThrow(fragLocation).open()) {
                    fshSrc = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            this.pipeline = createLegacyPipeline(format, vshSrc, fshSrc);
            this.legacyVertexSource = vshSrc;
            this.legacyFragmentSource = fshSrc;
            this.isLegacy = true;
            this.admittedBlitLegacy = this.pipeline;

        } catch (Exception e) {
            if (selected != null) {
                throw new IllegalStateException("Unable to convert selected particle shader contract", e);
            }
            Initializer.LOGGER.error("Error on shader {} conversion/compilation", this.name, e);
            GlEmulationLog.warnContractGap("shader_conversion", "fallbackShader",
                    "Using generic fallback shader for unsupported GLSL contract {}; shader={}",
                    classifyShaderFailure(e), this.name);
            createExternalFallbackShader(format);
        }
    }

    private GraphicsPipeline createLegacyPipeline(VertexFormat format, String vertex, String fragment) {
        return createLegacyPipeline(format, vertex, fragment, false);
    }

    private GraphicsPipeline createLegacyPipeline(VertexFormat format, String vertex, String fragment, boolean raw) {
        if (vertex == null || fragment == null) throw new IllegalStateException("Legacy shader sources are unavailable");
        GlslConverter converter = new GlslConverter();
        converter.setFormat(format);
        converter.reserveUniformNames(this.uniformMap.keySet());
        if (raw) converter.enableEffectSamplerOrigins();
        converter.process(vertex, fragment);
        UBO ubo = converter.getUBO();
        LegacyUniformDefaults.bind(ubo, converter.getUniformDeclarations(), this.uniformMap.keySet());
        this.setUniformSuppliers(ubo);
        if (raw) converter.bindEffectSamplerOrigins();
        Pipeline.Builder builder = new Pipeline.Builder(format, this.name);
        builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());


        try (var vertexBinary = SPIRVUtils.compileShader(this.name + ".vsh", converter.getVshConverted(), SPIRVUtils.ShaderKind.VERTEX_SHADER);
             var fragmentBinary = SPIRVUtils.compileShader(this.name + ".fsh", converter.getFshConverted(), SPIRVUtils.ShaderKind.FRAGMENT_SHADER)) {
            builder.setSPIRVs(vertexBinary, fragmentBinary);
            return builder.createGraphicsPipeline();
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
            var compiledRaw = builder.snapshotGraphicsBytecode();
            this.pipeline = builder.createGraphicsPipeline();
            this.rawBytecode = compiledRaw;
            this.rawBindingPath = fallbackPath;
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

