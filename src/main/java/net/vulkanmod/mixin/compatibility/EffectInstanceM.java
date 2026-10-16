package net.vulkanmod.mixin.compatibility;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.shaders.EffectProgram;
import com.mojang.blaze3d.shaders.Program;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.layout.Uniform;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.parser.GlslConverter;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@Mixin(value = EffectInstance.class, priority = 900)
public class EffectInstanceM {

    @Shadow(remap = false) @Final private Map<String, com.mojang.blaze3d.shaders.Uniform> f_108930_;
    @Shadow(remap = false) @Final private List<com.mojang.blaze3d.shaders.Uniform> f_108928_;

    @Shadow(remap = false) private boolean f_108933_;
    @Shadow(remap = false) private static EffectInstance f_108923_;
    @Shadow(remap = false) @Final private BlendMode f_108934_;
    @Shadow(remap = false) private static int f_108924_;
    @Shadow(remap = false) @Final private int f_108931_;
    @Shadow(remap = false) @Final private List<Integer> f_108927_;
    @Shadow(remap = false) @Final private List<String> f_108926_;
    @Shadow(remap = false) @Final private Map<String, IntSupplier> f_108925_;

    @Shadow(remap = false) @Final private String f_108932_;
    private static GraphicsPipeline lastPipeline;

    private GraphicsPipeline pipeline;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void inj(ResourceManager resourceManager, String string, CallbackInfo ci) {
                                                                                                     
                                                                                                         
                                                                                                    
        String[] programPathInfo = this.decompose(string, ':');
        ResourceLocation resourceLocation = new ResourceLocation(programPathInfo[0], "shaders/program/" + programPathInfo[1] + ".json");

        try {
            Resource resource = resourceManager.getResourceOrThrow(resourceLocation);
            try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonObject jsonObject = GsonHelper.parse(reader);
                String vertexShader = GsonHelper.getAsString(jsonObject, "vertex");
                String fragmentShader = GsonHelper.getAsString(jsonObject, "fragment");
                createShaders(resourceManager, vertexShader, fragmentShader);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EffectInstance;m_172566_(Lnet/minecraft/server/packs/resources/ResourceManager;Lcom/mojang/blaze3d/shaders/Program$Type;Ljava/lang/String;)Lcom/mojang/blaze3d/shaders/EffectProgram;",
            remap = false), require = 0)
    private EffectProgram redirectShader(ResourceManager resourceManager, Program.Type type, String string) {
        return null;
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void close(CallbackInfo ci) {
        ci.cancel();

        for (com.mojang.blaze3d.shaders.Uniform uniform : this.f_108928_) {
            uniform.close();
        }

    }

    private void createShaders(ResourceProvider resourceManager, String vertexShader, String fragShader) {

        try {
            String[] vshPathInfo = this.decompose(vertexShader, ':');
            ResourceLocation vshLocation = new ResourceLocation(vshPathInfo[0], "shaders/program/" + vshPathInfo[1] + ".vsh");
            Resource resource = resourceManager.getResourceOrThrow(vshLocation);
            InputStream inputStream = resource.open();
            String vshSrc = modernizeLegacyGlsl(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), false);

            String[] fshPathInfo = this.decompose(fragShader, ':');
            ResourceLocation fshLocation = new ResourceLocation(fshPathInfo[0], "shaders/program/" + fshPathInfo[1] + ".fsh");
            resource = resourceManager.getResourceOrThrow(fshLocation);
            inputStream = resource.open();
            String fshSrc = modernizeLegacyGlsl(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), true);

            GlslConverter converter = new GlslConverter();

            converter.process(vshSrc, fshSrc);
            UBO ubo = converter.getUBO();
            this.setUniformSuppliers(ubo);

            Pipeline.Builder builder = new Pipeline.Builder(DefaultVertexFormat.POSITION);
            builder.setUniforms(Collections.singletonList(ubo), converter.getSamplerList());
            builder.compileShaders(this.f_108932_, converter.getVshConverted(), converter.getFshConverted());

            this.pipeline = builder.createGraphicsPipeline();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void setUniformSuppliers(UBO ubo) {

        for(Uniform v_uniform : ubo.getUniforms()) {
            com.mojang.blaze3d.shaders.Uniform uniform = this.f_108930_.get(v_uniform.getName());

            if (uniform == null) {

                continue;
            }

            Supplier<MappedBuffer> supplier;
            ByteBuffer byteBuffer;

            if (uniform.getType() <= 3) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getIntBuffer());
            }
            else if (uniform.getType() <= 10) {
                byteBuffer = MemoryUtil.memByteBuffer(uniform.getFloatBuffer());
            }
            else {
                throw new RuntimeException("out of bounds value for uniform " + uniform);
            }

            MappedBuffer mappedBuffer = MappedBuffer.createFromBuffer(byteBuffer);
            supplier = () -> mappedBuffer;

            v_uniform.setSupplier(supplier);
        }

    }

    private String[] decompose(String string, char c) {
        String[] strings = new String[]{"minecraft", string};
        int i = string.indexOf(c);
        if (i >= 0) {
            strings[1] = string.substring(i + 1);
            if (i >= 1) {
                strings[0] = string.substring(0, i);
            }
        }

        return strings;
    }

                                                                                                      
                                                                                                     
                                                                                                           
                                                                                                               
    private static String modernizeLegacyGlsl(String src, boolean fragment) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("#version\\s+(\\d{3})").matcher(src);
        if (!m.find() || Integer.parseInt(m.group(1)) >= 150) {
            return src;
        }
        src = src.replaceFirst("#version\\s+\\d{3}[^\\n]*", "#version 150");
        src = src.replaceAll("\\battribute\\b", "in");
        src = src.replaceAll("\\bvarying\\b", fragment ? "in" : "out");
        src = src.replaceAll("\\btexture(2D|3D|Cube)\\b", "texture");
        if (fragment && src.contains("gl_FragColor")) {
            src = src.replaceFirst("(#version[^\\n]*\\n)", "$1out vec4 fragColor;\n");
            src = src.replace("gl_FragColor", "fragColor");
        }
        return src;
    }

    @Inject(method = "m_108966_", at = @At("HEAD"), cancellable = true, remap = false)
    private void apply(CallbackInfo ci) {
        ci.cancel();

        this.f_108933_ = false;
        this.f_108934_.apply();

        Renderer renderer = Renderer.getInstance();

        if (this.pipeline != lastPipeline) {
            renderer.bindGraphicsPipeline(pipeline);
            lastPipeline = this.pipeline;
        }

        for(int i = 0; i < this.f_108927_.size(); ++i) {
            String string = this.f_108926_.get(i);
            IntSupplier intSupplier = this.f_108925_.get(string);
            if (intSupplier != null) {
                RenderSystem.activeTexture(GL30.GL_TEXTURE0 + i);
                int j = intSupplier.getAsInt();
                if (j != -1) {
                    RenderSystem.bindTexture(j);
                    com.mojang.blaze3d.shaders.Uniform.uploadInteger(this.f_108927_.get(i), i);
                }
            }
        }

        for (com.mojang.blaze3d.shaders.Uniform uniform : this.f_108928_) {
            uniform.upload();
        }

        renderer.uploadAndBindUBOs(pipeline);

    }

    @Inject(method = "m_108965_", at = @At("HEAD"), cancellable = true, remap = false)
    private void clear(CallbackInfo ci) {
        ci.cancel();

        RenderSystem.assertOnRenderThread();
        ProgramManager.glUseProgram(0);
        f_108924_ = -1;
        f_108923_ = null;
        lastPipeline = null;

        for(int i = 0; i < this.f_108927_.size(); ++i) {
            if (this.f_108925_.get(this.f_108926_.get(i)) != null) {
                GlStateManager._activeTexture(GL30.GL_TEXTURE0 + i);
                GlStateManager._bindTexture(0);
            }
        }

    }
}

