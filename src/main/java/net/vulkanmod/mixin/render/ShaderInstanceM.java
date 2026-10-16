package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.parser.GlslConverter;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

@Mixin(value = ShaderInstance.class, priority = 900)
public class ShaderInstanceM implements ShaderMixed {

    @Shadow(remap = false) @Final private Map<String, com.mojang.blaze3d.shaders.Uniform> f_173333_;
    @Shadow(remap = false) @Final private String f_173300_;

    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173308_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173309_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_200956_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173312_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173318_;

    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_267422_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173315_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173316_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173317_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_202432_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173310_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173319_;
    @Shadow(remap = false) @Final @Nullable public com.mojang.blaze3d.shaders.Uniform f_173311_;

    private String vsPath;
    private String fsName;

    private GraphicsPipeline pipeline;
    boolean isLegacy = false;


    public GraphicsPipeline getPipeline() {
        return pipeline;
    }

    public GraphicsPipeline getPipeline(VertexFormat drawFormat) {
        return pipeline;
    }

                                                                                                   
                                                                                                    
                                                                                                 
                                                                                                
                   
    @Inject(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At("RETURN"))
    private void create(ResourceProvider resourceProvider, ResourceLocation shaderLocation, VertexFormat format, CallbackInfo ci) {
        String name = this.f_173300_;

        try {
            String path = getVulkanShaderPath(name);
            Initializer.LOGGER.info("[VM-DBG] create shader '{}' vulkanPath={}", name, path);
            if (path == null) {
                createLegacyShader(resourceProvider, format);
                return;
            }

            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            this.pipeline = pipelineBuilder.createGraphicsPipeline();
            wireUnresolvedUniforms();
        } catch (Exception e) {
            Initializer.LOGGER.error("Error on shader {} creation", name, e);
            createFallbackShader(format);
        }
    }

                                                                                                 
                                                                                           
                                                                                               
                                                                                                
                                                                                   
    private void wireUnresolvedUniforms() {
        if (this.pipeline == null) {
            return;
        }

        for (UBO ubo : this.pipeline.getBuffers()) {
            for (Uniform vUniform : ubo.getUniforms()) {
                if (vUniform.hasSupplier()) {
                    continue;
                }

                com.mojang.blaze3d.shaders.Uniform uniform = this.f_173333_.get(vUniform.getName());

                ByteBuffer byteBuffer;
                if (uniform != null && uniform.getType() <= 3) {
                    byteBuffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
                } else if (uniform != null && uniform.getType() <= 10) {
                    byteBuffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
                } else {
                    Initializer.LOGGER.warn("No supplier or ShaderInstance uniform for '{}' in shader {}; zero-filling",
                            vUniform.getName(), this.f_173300_);
                    byteBuffer = MemoryUtil.memCalloc(vUniform.getSize() * 4);
                }

                MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
                vUniform.setSupplier(() -> mappedBuffer);
            }
        }
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ShaderInstance;m_173340_(Lnet/minecraft/server/packs/resources/ResourceProvider;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/Program;", remap = false), require = 0)
    private Program loadNames(ResourceProvider resourceProvider, Program.Type type, String name) {
        if (this.f_173300_.contains(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR))) {
            ResourceLocation location = ResourceLocation.tryParse(name);
            String path = location.withPath("shaders/core/%s".formatted(location.getPath())).toString();

            switch (type) {
                case VERTEX -> this.vsPath = path;
                case FRAGMENT -> this.fsName = path;
            }
        }

        return null;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;m_166710_(IILjava/lang/CharSequence;)V", remap = false), require = 0)
    private void bindAttr(int program, int index, CharSequence name) {}

                                                                                                 
                                                                                       

    @Inject(method = "close", at = @At("HEAD"), cancellable = true, remap = false)
    private void onClose(CallbackInfo ci) {
        if (this.pipeline != null)
            this.pipeline.cleanUp();
        ci.cancel();
    }

            
    @Inject(method = "m_173363_", at = @At("HEAD"), cancellable = true, remap = false)
    private void apply(CallbackInfo ci) {
        ci.cancel();

        if (!this.isLegacy)
            return;

        if (this.f_173308_ != null) {
            this.f_173308_.set(RenderSystem.getModelViewMatrix());
        }

        if (this.f_173309_ != null) {
            this.f_173309_.set(RenderSystem.getProjectionMatrix());
        }

        if (this.f_200956_ != null) {
            this.f_200956_.set(RenderSystem.getInverseViewRotationMatrix());
        }

        if (this.f_173312_ != null) {
            this.f_173312_.set(RenderSystem.getShaderColor());
        }

        if (this.f_267422_ != null) {
            this.f_267422_.set(RenderSystem.getShaderGlintAlpha());
        }

        if (this.f_173315_ != null) {
            this.f_173315_.set(RenderSystem.getShaderFogStart());
        }

        if (this.f_173316_ != null) {
            this.f_173316_.set(RenderSystem.getShaderFogEnd());
        }

        if (this.f_173317_ != null) {
            this.f_173317_.set(RenderSystem.getShaderFogColor());
        }

        if (this.f_202432_ != null) {
            this.f_202432_.set(RenderSystem.getShaderFogShape().getIndex());
        }

        if (this.f_173310_ != null) {
            this.f_173310_.set(RenderSystem.getTextureMatrix());
        }

        if (this.f_173319_ != null) {
            this.f_173319_.set(RenderSystem.getShaderGameTime());
        }

        if (this.f_173311_ != null) {
            Window window = Minecraft.getInstance().getWindow();
            this.f_173311_.set((float)window.getWidth(), (float)window.getHeight());
        }

        if (this.f_173318_ != null) {
            this.f_173318_.set(RenderSystem.getShaderLineWidth());
        }

                                                                                                             
                                                                                                         
                                                                                                          
                                                                                                     
        RenderSystem.setupShaderLights((ShaderInstance) (Object) this);
    }

                                                                   
    @Inject(method = "m_173362_", at = @At("HEAD"), cancellable = true, remap = false)
    private void clear(CallbackInfo ci) {
        ci.cancel();
    }

    private void setUniformSuppliers(UBO ubo) {

        for(Uniform vUniform : ubo.getUniforms()) {
            com.mojang.blaze3d.shaders.Uniform uniform = this.f_173333_.get(vUniform.getName());

            if(uniform == null) {
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
            String vertPath = this.vsPath + ".vsh";
            Resource resource = resourceProvider.getResourceOrThrow(ResourceLocation.tryParse(vertPath));
            InputStream inputStream = resource.open();
            String vshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            String fragPath = this.fsName + ".fsh";
            resource = resourceProvider.getResourceOrThrow(ResourceLocation.tryParse(fragPath));
            inputStream = resource.open();
            String fshSrc = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

            GlslConverter converter = new GlslConverter();
            Pipeline.Builder builder = new Pipeline.Builder(format, this.f_173300_);

            converter.setFormat(format);
            converter.process(vshSrc, fshSrc);
            UBO ubo = converter.getUBO();
            this.setUniformSuppliers(ubo);

            builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());
            builder.compileShaders(this.f_173300_, converter.getVshConverted(), converter.getFshConverted());

            this.pipeline = builder.createGraphicsPipeline();
            this.isLegacy = true;

            Initializer.LOGGER.info("[VM-DBG] legacy shader '{}' fmt={} samplers={} uboUniforms={}",
                    this.f_173300_, format,
                    converter.getSamplerList(),
                    ubo.getUniforms().stream().map(net.vulkanmod.vulkan.shader.layout.Uniform::getName).toList());

        } catch (Throwable e) {
            Initializer.LOGGER.error("[VM-DBG] createLegacyShader FAILED for '{}' (vsPath={}): {}", this.f_173300_, this.vsPath, e.toString(), e);
            createFallbackShader(format);
        }
    }

                                                                                                   
                                                                                                 
                                                                                                
                                
    private void createFallbackShader(VertexFormat format) {
        String fallbackPath = fallbackShaderPath(format);
        if (fallbackPath == null) {
            Initializer.LOGGER.warn("No safe Vulkan fallback shader for {} with vertex format {}", this.f_173300_, format);
            return;
        }

        try {
            Pipeline.Builder builder = new Pipeline.Builder(format, fallbackPath);
            builder.parseBindingsJSON();
            builder.compileShaders();
            this.pipeline = builder.createGraphicsPipeline();
            this.isLegacy = false;
            wireUnresolvedUniforms();
            Initializer.LOGGER.warn("Using Vulkan fallback shader {} for external shader {}", fallbackPath, this.f_173300_);
        } catch (Exception fallbackException) {
            Initializer.LOGGER.error("Error creating Vulkan fallback shader {} for {}", fallbackPath, this.f_173300_, fallbackException);
        }
    }

    private static String fallbackShaderPath(VertexFormat format) {
        if (DefaultVertexFormat.BLIT_SCREEN.equals(format)) {
            return "minecraft/core/blit_screen/blit_screen";
        }

        if (DefaultVertexFormat.PARTICLE.equals(format)) {
            return "minecraft/core/particle/particle";
        }

        if (DefaultVertexFormat.NEW_ENTITY.equals(format)) {
            return "minecraft/core/rendertype_entity_cutout_no_cull/rendertype_entity_cutout_no_cull";
        }

        if (DefaultVertexFormat.POSITION_TEX_COLOR.equals(format)) {
            return "minecraft/core/position_tex_color/position_tex_color";
        }

        if (DefaultVertexFormat.POSITION_TEX.equals(format)) {
            return "minecraft/core/position_tex/position_tex";
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
        return format != null && format.getElementAttributeNames().equals(java.util.List.of(attributes));
    }

    private static String getVulkanShaderPath(String name) {
        ResourceLocation location = ResourceLocation.tryParse(name);
        if (location == null) {
            return null;
        }

        String path = location.getNamespace().equals("minecraft")
                ? String.format("minecraft/core/%s/%s", location.getPath(), location.getPath())
                : String.format("%s/core/%s/%s", location.getNamespace(), location.getPath(), location.getPath());
        String resourcePath = String.format("/assets/vulkanmod/shaders/%s.json", path);
        return Pipeline.class.getResourceAsStream(resourcePath) != null ? path : null;
    }
}


