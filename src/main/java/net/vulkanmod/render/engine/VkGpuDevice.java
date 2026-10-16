package net.vulkanmod.render.engine;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.compat.observer.CompatProfiler;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.converter.GLSLParser;
import net.vulkanmod.vulkan.shader.converter.Lexer;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.vulkan.VK10;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings("NullableProblems")
public class VkGpuDevice implements GpuDevice {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final VkCommandEncoder encoder;
    private final VkDebugLabel debugLabels;
    private final int maxSupportedTextureSize;
    private final int uniformOffsetAlignment;
    private final ShaderSource defaultShaderSource;
    private final Set<String> enabledExtensions = new HashSet<>();

    private final Map<ShaderCompilationKey, String> shaderSrcCache = new HashMap<>();

    public VkGpuDevice(long l, int i, boolean bl, ShaderSource shaderSource, boolean bl2) {
        this.debugLabels = VkDebugLabel.create(bl2, this.enabledExtensions);
        this.maxSupportedTextureSize = VRenderSystem.maxSupportedTextureSize();
        this.uniformOffsetAlignment = (int) DeviceManager.deviceProperties.limits().minUniformBufferOffsetAlignment();
        this.defaultShaderSource = shaderSource;

        this.encoder = new VkCommandEncoder(this);
    }

    public VkDebugLabel debugLabels() {
        return this.debugLabels;
    }

    @Override
    public CommandEncoder createCommandEncoder() {
        return this.encoder;
    }

    @Override
    public GpuSampler createSampler(AddressMode addressMode, AddressMode addressMode2, FilterMode filterMode,
                                    FilterMode filterMode2, int maxAnisotropy, OptionalDouble maxLod) {
        return new VkSampler(addressMode, addressMode2, filterMode, filterMode2, maxAnisotropy, maxLod);
    }

    @Override
    public GpuTexture createTexture(@Nullable Supplier<String> supplier, int usage, TextureFormat textureFormat, int width, int height, int layers, int mipLevels) {
        return this.createTexture(this.debugLabels.exists() && supplier != null ? supplier.get() : null, usage, textureFormat, width, height, layers, mipLevels);
    }

    @Override
    public GpuTexture createTexture(@Nullable String string, int usage, TextureFormat textureFormat, int width, int height, int layers, int mipLevels) {
        if (mipLevels < 1) {
            throw new IllegalArgumentException("mipLevels must be at least 1");
        } else {
            int id = VkGlTexture.genTextureId();
            if (string == null) {
                string = String.valueOf(id);
            }

            int format = VkGpuTexture.vkFormat(textureFormat);
            int viewType = VkGpuTexture.vkImageViewType(usage);
            boolean depthFormat = VulkanImage.isDepthFormat(format);
            int attachmentUsage = depthFormat ? VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT : VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

            VulkanImage texture = VulkanImage.builder(width, height)
                                             .setName(string)
                                             .setFormat(format)
                                             .setArrayLayers(layers)
                                             .setMipLevels(mipLevels)
                                             .addUsage(attachmentUsage)
                                             .setViewType(viewType)
                                             .createVulkanImage();

            VkGlTexture vGlTexture = VkGlTexture.getTexture(id);
            vGlTexture.setVulkanImage(texture);
            VkGlTexture.bindTexture(id);

            VkGpuTexture glTexture = new VkGpuTexture(usage, string, textureFormat, width, height, layers, mipLevels, id, vGlTexture);
            this.debugLabels.applyLabel(glTexture);
            return glTexture;
        }
    }

    public VkGpuTexture gpuTextureFromVulkanImage(VulkanImage image) {
        int id = VkGlTexture.genTextureId();
        VkGlTexture glTexture = VkGlTexture.getTexture(id);
        glTexture.setVulkanImage(image);
        TextureFormat textureFormat = VkGpuTexture.textureFormat(image.format);
        VkGpuTexture gpuTexture = new VkGpuTexture(0, image.name, textureFormat, image.width, image.height, 1, image.mipLevels, id, glTexture);
        this.debugLabels.applyLabel(gpuTexture);
        return gpuTexture;
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture gpuTexture) {
        return this.createTextureView(gpuTexture, 0, gpuTexture.getMipLevels());
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture gpuTexture, int startLevel, int levels) {
        if (gpuTexture.isClosed()) {
            throw new IllegalArgumentException("Can't create texture view with closed texture");
        } else if (startLevel >= 0 && startLevel + levels <= gpuTexture.getMipLevels()) {

                                                                         
            if (gpuTexture.getClass() != VkGpuTexture.class) {
                gpuTexture = VkGpuTexture.fromGlTexture((GlTexture) gpuTexture);
            }

            return new VkTextureView((VkGpuTexture) gpuTexture, startLevel, levels);
        } else {
            throw new IllegalArgumentException(
                    levels + " mip levels starting from " + startLevel + " would be out of range for texture with only " + gpuTexture.getMipLevels() + " mip levels"
            );
        }
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> supplier, @GpuBuffer.Usage int usage, long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Buffer size must be greater than zero");
        } else {
            return new VkGpuBuffer(this.debugLabels, supplier, usage, size);
        }
    }

    @Override
    public GpuBuffer createBuffer(@Nullable Supplier<String> supplier, int usage, ByteBuffer byteBuffer) {
        if (!byteBuffer.hasRemaining()) {
            throw new IllegalArgumentException("Buffer source must not be empty");
        } else {
            VkGpuBuffer glBuffer = new VkGpuBuffer(this.debugLabels, supplier, usage, byteBuffer.remaining());
            this.encoder.initializeBuffer(glBuffer, byteBuffer);
            return glBuffer;
        }
    }

    @Override
    public String getImplementationInformation() {
        return "Vulkan " + Vulkan.getDevice().vkVersion + ", " + Vulkan.getDevice().vendorIdString;
    }

    @Override
    public List<String> getLastDebugMessages() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public String getRenderer() {
        return DeviceManager.device.deviceName;
    }

    @Override
    public String getVendor() {
        return Vulkan.getDevice().vendorIdString;
    }

    @Override
    public String getBackendName() {
        return "Vulkan";
    }

    @Override
    public String getVersion() {
        return Vulkan.getDevice().vkVersion;
    }

    @Override
    public int getMaxTextureSize() {
        return this.maxSupportedTextureSize;
    }

    @Override
    public int getUniformOffsetAlignment() {
        return this.uniformOffsetAlignment;
    }

    @Override
    public void clearPipelineCache() {
                                                                              
                                                                                
                                                                        
        this.shaderSrcCache.clear();
    }

    @Override
    public List<String> getEnabledExtensions() {
        return new ArrayList(this.enabledExtensions);
    }

    @Override
    public int getMaxSupportedAnisotropy() {
        return DeviceManager.getMaxSupportedAnisotropy();
    }

    @Override
    public void close() {
        this.clearPipelineCache();
    }

    protected String getCachedShaderSrc(Identifier resourceLocation, ShaderType shaderType, ShaderDefines shaderDefines, ShaderSource shaderSourceGetter) {
        ShaderCompilationKey shaderCompilationKey = new ShaderCompilationKey(resourceLocation, shaderType, shaderDefines);
        if (CompatProfiler.ENABLED) {
            if (this.shaderSrcCache.containsKey(shaderCompilationKey)) CompatProfiler.shaderCacheHits++;
            else CompatProfiler.shaderCacheMisses++;
        }

        return this.shaderSrcCache.computeIfAbsent(shaderCompilationKey, compilationKey -> {
            String shaderExtension = switch (shaderType) {
                case VERTEX -> ".vsh";
                case FRAGMENT -> ".fsh";
            };

            String shaderName = resourceLocation.getPath() + shaderExtension;

            if (ShaderLoadUtil.REMAPPED_SHADERS.contains(shaderName)) {
                String src = ShaderLoadUtil.getShaderSource(resourceLocation, shaderType);

                if (src == null) {
                    throw new RuntimeException("shader: (%s) not found.".formatted(src));
                }

                return src;
            }

            return shaderSourceGetter.get(compilationKey.id, compilationKey.type);
        });
    }

    public int getCachedShaderSourceCount() {
        return this.shaderSrcCache.size();
    }

    public CompiledRenderPipeline precompilePipeline(RenderPipeline renderPipeline, @Nullable ShaderSource shaderSourceGetter) {
        shaderSourceGetter = shaderSourceGetter == null ? this.defaultShaderSource : shaderSourceGetter;

        try {
            compilePipeline(renderPipeline, shaderSourceGetter);
        } catch (Exception e) {
            throw new RuntimeException("Caught exception compiling pipeline: %s".formatted(renderPipeline.toString()), e);
        }


        return new VkRenderPipeline(renderPipeline);
    }

    public void compilePipeline(RenderPipeline renderPipeline) {
        this.compilePipeline(renderPipeline, this.defaultShaderSource);
    }

    private void compilePipeline(RenderPipeline renderPipeline, ShaderSource shaderSrcGetter) {
        String locationPath = renderPipeline.getLocation().getPath();

        String configName;
        if (locationPath.contains("core")) {
            configName = locationPath.split("/")[1];
        } else {
            configName = locationPath;
        }

        Pipeline.Builder builder = new Pipeline.Builder(renderPipeline.getVertexFormat(), configName);
        GraphicsPipeline pipeline;
        ExtendedRenderPipeline extPipeline = ExtendedRenderPipeline.of(renderPipeline);

        Identifier vertexShaderLocation = renderPipeline.getVertexShader();
        Identifier fragmentShaderLocation = renderPipeline.getFragmentShader();

        ShaderDefines shaderDefines = renderPipeline.getShaderDefines();

        String vshSrc = this.getCachedShaderSrc(vertexShaderLocation, ShaderType.VERTEX, shaderDefines, shaderSrcGetter);
        String fshSrc = this.getCachedShaderSrc(fragmentShaderLocation, ShaderType.FRAGMENT, shaderDefines, shaderSrcGetter);

        vshSrc = GlslPreprocessor.injectDefines(vshSrc, shaderDefines);
        fshSrc = GlslPreprocessor.injectDefines(fshSrc, shaderDefines);

        Lexer lexer = new Lexer(vshSrc);
        GLSLParser parser = new GLSLParser();
        parser.setVertexFormat(renderPipeline.getVertexFormat());

        try {
            parser.parse(lexer, GLSLParser.Stage.VERTEX);

            lexer = new Lexer(fshSrc);
            parser.parse(lexer, GLSLParser.Stage.FRAGMENT);
        } catch (Exception e) {
            throw new RuntimeException("Caught exception while parsing: %s".formatted(renderPipeline.toString()), e);
        }

        UBO[] ubos = parser.createUBOs();
        List<ImageDescriptor> samplers = parser.getSamplerList();

        String vshProcessed = parser.getOutput(GLSLParser.Stage.VERTEX);
        String fshProcessed = parser.getOutput(GLSLParser.Stage.FRAGMENT);

        builder.setUniforms(List.of(ubos), samplers, parser.getTexelBufferList());

        builder.setShaderSrc(SPIRVUtils.ShaderKind.VERTEX_SHADER, vshProcessed);
        builder.setShaderSrc(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, fshProcessed);

        try {
            pipeline = builder.createGraphicsPipeline();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Exception while compiling pipeline %s".formatted(renderPipeline));
        }

        EGlProgram eGlProgram = new EGlProgram(1, configName);
        eGlProgram.setupUniforms(pipeline, renderPipeline.getUniforms(), renderPipeline.getSamplers());
        extPipeline.setProgram(eGlProgram);

        extPipeline.setPipeline(pipeline);
    }

    
    record ShaderCompilationKey(Identifier id, ShaderType type, ShaderDefines defines) {

        public String toString() {
            String string = this.id + " (" + this.type + ")";
            return !this.defines.isEmpty() ? string + " with " + this.defines : string;
        }
    }

    private static class VkRenderPipeline implements CompiledRenderPipeline {
        final RenderPipeline renderPipeline;

        public VkRenderPipeline(RenderPipeline renderPipeline) {
            this.renderPipeline = renderPipeline;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    @Override
    public net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties deviceProperties() {
        return new net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties() {
            @Override
            public String backendName() {
                return "VulkanMod";
            }

            @Override
            public String apiName() {
                return "Vulkan";
            }
        };
    }

    @Override
    public net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures enabledFeatures() {
        return () -> true;
    }
}
