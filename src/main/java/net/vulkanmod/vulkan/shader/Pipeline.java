package net.vulkanmod.vulkan.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.UniformBuffer;
import net.vulkanmod.vulkan.shader.SPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.shader.SPIRVUtils.ShaderKind;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.ManualUBO;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.layout.PushConstants;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.gl.GlSampler;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShader;
import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Pipeline {

    private static final VkDevice DEVICE = Vulkan.getVkDevice();
    private static final java.nio.file.Path CACHE_PATH = java.nio.file.Path.of("config", "vulkanmod_pipeline_cache.bin");
    protected static final long PIPELINE_CACHE = createPipelineCache();
    public static final List<Pipeline> PIPELINES = new LinkedList<>();

    private static long createPipelineCache() {
        try (MemoryStack stack = stackPush()) {
            VkPipelineCacheCreateInfo cacheCreateInfo = VkPipelineCacheCreateInfo.calloc(stack);
            cacheCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            ByteBuffer initialData = null;
            if (java.nio.file.Files.exists(CACHE_PATH)) {
                try {
                    byte[] bytes = java.nio.file.Files.readAllBytes(CACHE_PATH);
                    if (bytes.length > 0) {
                        initialData = MemoryUtil.memAlloc(bytes.length);
                        initialData.put(bytes);
                        initialData.flip();

                        cacheCreateInfo.pInitialData(initialData);
                    }
                } catch (Exception e) {
                    System.err.println("[VulkanMod] Failed to load pipeline cache: " + e.getMessage());
                }
            }

            LongBuffer pPipelineCache = stack.mallocLong(1);
            int result = vkCreatePipelineCache(DEVICE, cacheCreateInfo, null, pPipelineCache);

            if (initialData != null) {
                MemoryUtil.memFree(initialData);
            }

            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline cache. Result: " + result);
            }

            return pPipelineCache.get(0);
        }
    }

    public static void destroyPipelineCache() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pDataSize = stack.mallocPointer(1);
            int result = vkGetPipelineCacheData(DEVICE, PIPELINE_CACHE, pDataSize, null);

            if (result == VK_SUCCESS && pDataSize.get(0) > 0) {
                long dataSize = pDataSize.get(0);
                ByteBuffer dataBuffer = MemoryUtil.memAlloc((int) dataSize);

                result = vkGetPipelineCacheData(DEVICE, PIPELINE_CACHE, pDataSize, dataBuffer);
                if (result == VK_SUCCESS) {
                    byte[] bytes = new byte[(int) dataSize];
                    dataBuffer.get(bytes);

                    try {
                        if (!java.nio.file.Files.exists(CACHE_PATH.getParent())) {
                            java.nio.file.Files.createDirectories(CACHE_PATH.getParent());
                        }
                        java.nio.file.Files.write(CACHE_PATH, bytes);
                    } catch (Exception e) {
                        System.err.println("[VulkanMod] Failed to write pipeline cache: " + e.getMessage());
                    }
                }
                MemoryUtil.memFree(dataBuffer);
            }
        } catch (Exception e) {
            System.err.println("[VulkanMod] Error destroying pipeline cache: " + e.getMessage());
        }

        vkDestroyPipelineCache(DEVICE, PIPELINE_CACHE, null);
    }

    public static void recreateDescriptorSets(int frames) {
        List<Pipeline> pipelines = new ArrayList<>(PIPELINES);
        DescriptorSets[][] previous = new DescriptorSets[pipelines.size()][];
                                                                              
                                                                          
        DescriptorSets[][] replacements = DescriptorResources.createFrames(pipelines.size(), DescriptorSets[][]::new,
                index -> pipelines.get(index).buildDescriptorSets(frames),
                sets -> DescriptorResources.closeFrames(sets, DescriptorSets::cleanUp));
        for (int i = 0; i < pipelines.size(); i++) {
            Pipeline pipeline = pipelines.get(i);
            previous[i] = pipeline.descriptorSets;
            pipeline.descriptorSets = replacements[i];
        }
        DescriptorResources.closeFrames(previous,
                sets -> DescriptorResources.closeFrames(sets, DescriptorSets::cleanUp));
    }

    public final String name;

    protected long descriptorSetLayout;
    protected long pipelineLayout;

    protected DescriptorSets[] descriptorSets;
    protected List<UBO> buffers;
    protected ManualUBO manualUBO;
    protected List<ImageDescriptor> imageDescriptors;
    protected PushConstants pushConstants;

    public List<UBO> getBuffers() {
        return buffers;
    }

    public Pipeline(String name) {
        this.name = name;
    }

    protected void createDescriptorSetLayout() {
        try (MemoryStack stack = stackPush()) {
            int bindingsSize = this.buffers.size() + imageDescriptors.size();

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindingsSize, stack);

            for (UBO ubo : this.buffers) {
                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(ubo.getBinding());
                uboLayoutBinding.binding(ubo.getBinding());
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(ubo.getType());
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(ubo.getStages());
            }

            for (ImageDescriptor imageDescriptor : this.imageDescriptors) {
                VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(imageDescriptor.getBinding());
                samplerLayoutBinding.binding(imageDescriptor.getBinding());
                samplerLayoutBinding.descriptorCount(1);
                samplerLayoutBinding.descriptorType(imageDescriptor.getType());
                samplerLayoutBinding.pImmutableSamplers(null);
                samplerLayoutBinding.stageFlags(imageDescriptor.getStages());
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(DeviceManager.vkDevice, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }

            this.descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    protected void createPipelineLayout() {
        try (MemoryStack stack = stackPush()) {

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(this.descriptorSetLayout));

            if (this.pushConstants != null) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
                pushConstantRange.size(this.pushConstants.getSize());
                pushConstantRange.offset(0);
                pushConstantRange.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

                pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
            }

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if (vkCreatePipelineLayout(DEVICE, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);
        }
    }

    protected void createDescriptorSets(int frames) {
        DescriptorSets[] replacement = buildDescriptorSets(frames);
        DescriptorSets[] previous = descriptorSets;
        descriptorSets = replacement;
        DescriptorResources.closeFrames(previous, DescriptorSets::cleanUp);
    }

    private DescriptorSets[] buildDescriptorSets(int frames) {
        return DescriptorResources.createFrames(frames, DescriptorSets[]::new,
                frame -> new DescriptorSets(this), DescriptorSets::cleanUp);
    }

    public void scheduleCleanUp() {
        MemoryManager.getInstance().addFrameOp(this::cleanUp);
    }

    public abstract void cleanUp();

    void destroyDescriptorSets() {
        DescriptorSets[] previous = this.descriptorSets;
        this.descriptorSets = null;
        DescriptorResources.closeFrames(previous, DescriptorSets::cleanUp);
    }

    public ManualUBO getManualUBO() {
        return this.manualUBO;
    }

    public void resetDescriptorPool(int i) {
        if (this.descriptorSets != null)
            this.descriptorSets[i].resetIdx();

    }

    public PushConstants getPushConstants() {
        return this.pushConstants;
    }

    public long getLayout() {
        return pipelineLayout;
    }

    public List<ImageDescriptor> getImageDescriptors() {
        return imageDescriptors;
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, int frame) {
        UniformBuffer uniformBuffer = Renderer.getDrawer().getUniformBuffer();
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS);
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int frame) {
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS);
    }

    static long createShaderModule(ByteBuffer spirvCode) {

        try (MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if (vkCreateShaderModule(DEVICE, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }

    protected static class DescriptorSets {
        private final Pipeline pipeline;
        private final DescriptorResources resources;
        private long currentSet;
        private int currentIdx = -1;

        private final long[] boundUBs;
        private final ImageDescriptor.State[] boundTextures;
        private final IntBuffer dynamicOffsets;

        DescriptorSets(Pipeline pipeline) {
            this.pipeline = pipeline;
            this.boundTextures = new ImageDescriptor.State[pipeline.imageDescriptors.size()];
            this.boundUBs = new long[pipeline.buffers.size()];

            Arrays.setAll(boundTextures, i -> new ImageDescriptor.State(0, 0));
            this.resources = DescriptorResources.create(new DescriptorResources.Backend() {
                public IntBuffer allocateOffsets(int count) { return MemoryUtil.memAllocInt(count); }
                public void freeOffsets(IntBuffer offsets) { MemoryUtil.memFree(offsets); }
                public long createPool(int capacity) { return DescriptorSets.this.createDescriptorPool(capacity); }
                public LongBuffer allocateSetStorage(int capacity) { return MemoryUtil.memAllocLong(capacity); }
                public void allocateSets(long pool, int capacity, LongBuffer storage) {
                    DescriptorSets.this.allocateDescriptorSets(pool, capacity, storage);
                }
                public void freeSetStorage(LongBuffer storage) { MemoryUtil.memFree(storage); }
                public void destroyPool(long pool) { vkDestroyDescriptorPool(DEVICE, pool, null); }
                public void retirePool(long pool) {
                    MemoryManager.getInstance().addFrameOp(() -> vkDestroyDescriptorPool(DEVICE, pool, null));
                }
            }, 10, pipeline.buffers.size());
            this.dynamicOffsets = this.resources.offsets();
        }

        protected void bindSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int bindPoint) {
            try (MemoryStack stack = stackPush()) {

                this.updateUniforms(uniformBuffer);
                this.updateDescriptorSet(stack, uniformBuffer);

                vkCmdBindDescriptorSets(commandBuffer, bindPoint, pipeline.pipelineLayout,
                        0, stack.longs(currentSet), dynamicOffsets);
            }
        }

        private void updateUniforms(UniformBuffer globalUB) {
            int i = 0;
            for (UBO ubo : pipeline.buffers) {
                boolean useOwnUB = ubo.getUniformBuffer() != null;
                UniformBuffer ub = useOwnUB ? ubo.getUniformBuffer() : globalUB;

                int currentOffset = (int) ub.getUsedBytes();
                this.dynamicOffsets.put(i, currentOffset);

                int alignedSize = UniformBuffer.getAlignedSize(ubo.getSize());
                ub.checkCapacity(alignedSize);

                if (!useOwnUB) {
                    ubo.update(ub.getPointer());
                    ub.updateOffset(alignedSize);
                }

                ++i;
            }
        }

        private boolean needsUpdate(UniformBuffer uniformBuffer) {
            if (currentIdx == -1)
                return true;

            for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = imageDescriptor.getSampler(image);

                if (imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                if (!this.boundTextures[j].isCurrentState(view, sampler)) {
                    return true;
                }
            }

            for (int j = 0; j < pipeline.buffers.size(); ++j) {
                UBO ubo = pipeline.buffers.get(j);
                UniformBuffer uniformBufferI = ubo.getUniformBuffer();

                if (uniformBufferI == null)
                    uniformBufferI = uniformBuffer;

                if (this.boundUBs[j] != uniformBufferI.getId()) {
                    return true;
                }
            }

            return false;
        }

        private void updateDescriptorSet(MemoryStack stack, UniformBuffer uniformBuffer) {

            if (!needsUpdate(uniformBuffer))
                return;

            int nextIndex = Math.addExact(this.currentIdx, 1);
            if (nextIndex >= this.resources.capacity()) {
                this.resources.grow(Math.multiplyExact(this.resources.capacity(), 2));
                nextIndex = 0;
            }
            this.currentIdx = nextIndex;
            this.currentSet = this.resources.sets().get(nextIndex);

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(pipeline.buffers.size() + pipeline.imageDescriptors.size(), stack);
            VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[pipeline.buffers.size()];

            int i = 0;
            for (UBO ubo : pipeline.buffers) {
                UniformBuffer ub = ubo.getUniformBuffer();
                if (ub == null)
                    ub = uniformBuffer;
                boundUBs[i] = ub.getId();

                bufferInfos[i] = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfos[i].buffer(boundUBs[i]);
                bufferInfos[i].range(ubo.getSize());

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                uboDescriptorWrite.sType$Default();
                uboDescriptorWrite.dstBinding(ubo.getBinding());
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(ubo.getType());
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfos[i]);
                uboDescriptorWrite.dstSet(currentSet);

                ++i;
            }

            VkDescriptorImageInfo.Buffer[] imageInfo = new VkDescriptorImageInfo.Buffer[pipeline.imageDescriptors.size()];

            for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = imageDescriptor.getSampler(image);
                int layout = imageDescriptor.getLayout();

                if (imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                imageInfo[j] = VkDescriptorImageInfo.calloc(1, stack);
                imageInfo[j].imageLayout(layout);
                imageInfo[j].imageView(view);

                if (imageDescriptor.useSampler)
                    imageInfo[j].sampler(sampler);

                VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(imageDescriptor.getBinding());
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(imageDescriptor.getType());
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo[j]);
                samplerDescriptorWrite.dstSet(currentSet);

                this.boundTextures[j].set(view, sampler);
                ++i;
            }

            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
        }

        private void allocateDescriptorSets(long pool, int capacity, LongBuffer storage) {
            try (MemoryStack stack = stackPush()) {
                LongBuffer layout = stack.mallocLong(capacity);
                for (int i = 0; i < capacity; ++i) layout.put(i, pipeline.descriptorSetLayout);

                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                allocInfo.sType$Default();
                allocInfo.descriptorPool(pool);
                allocInfo.pSetLayouts(layout);

                int result = vkAllocateDescriptorSets(DEVICE, allocInfo, storage);
                if (result != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
                }
            }
        }

        private long createDescriptorPool(int capacity) {
            try (MemoryStack stack = stackPush()) {
                int size = pipeline.buffers.size() + pipeline.imageDescriptors.size();
                VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(size, stack);

                int i;
                for (i = 0; i < pipeline.buffers.size(); ++i) {
                    VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(i);
                    uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
                    uniformBufferPoolSize.descriptorCount(capacity);
                }

                for (; i < pipeline.buffers.size() + pipeline.imageDescriptors.size(); ++i) {
                    VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(i);
                    textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                    textureSamplerPoolSize.descriptorCount(capacity);
                }

                VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
                poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
                poolInfo.pPoolSizes(poolSizes);
                poolInfo.maxSets(capacity);

                LongBuffer pool = stack.callocLong(1);
                if (vkCreateDescriptorPool(DEVICE, poolInfo, null, pool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor pool");
                }
                return pool.get(0);
            }
        }

        public void resetIdx() {
            this.currentIdx = -1;
        }

        private void cleanUp() {
            this.resources.close();
        }

    }

    public static class Builder {

        public static GraphicsPipeline createGraphicsPipeline(VertexFormat format, String path) {
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            return pipelineBuilder.createGraphicsPipeline();
        }

        final VertexFormat vertexFormat;
        final String shaderPath;
        List<UBO> UBOs;
        ManualUBO manualUBO;
        PushConstants pushConstants;
        List<ImageDescriptor> imageDescriptors;
        int nextBinding;

        SPIRV vertShaderSPIRV;
        SPIRV fragShaderSPIRV;
        SPIRV tescShaderSPIRV;
        SPIRV teseShaderSPIRV;
        int patchControlPoints;

        RenderPass renderPass;

        public Builder(VertexFormat vertexFormat, String path) {
            this.vertexFormat = vertexFormat;
            this.shaderPath = path;
        }

        public Builder(VertexFormat vertexFormat) {
            this(vertexFormat, null);
        }

        public GraphicsPipeline createGraphicsPipeline() {
            Validate.isTrue(this.imageDescriptors != null && this.UBOs != null
                            && this.vertShaderSPIRV != null && this.fragShaderSPIRV != null,
                    "Cannot create Pipeline: resources missing");
            Validate.isTrue((this.tescShaderSPIRV == null) == (this.teseShaderSPIRV == null)
                            && (this.tescShaderSPIRV != null) == (this.patchControlPoints > 0),
                    "Cannot create Pipeline: incomplete tessellation state");

            if (this.manualUBO != null)
                this.UBOs.add(this.manualUBO);

            return new GraphicsPipeline(this);
        }

        public void setUniforms(List<UBO> UBOs, List<ImageDescriptor> imageDescriptors) {
            this.UBOs = UBOs;
            this.imageDescriptors = imageDescriptors;
        }

        public void setSPIRVs(SPIRV vertShaderSPIRV, SPIRV fragShaderSPIRV) {
            this.vertShaderSPIRV = vertShaderSPIRV;
            this.fragShaderSPIRV = fragShaderSPIRV;
            this.tescShaderSPIRV = null;
            this.teseShaderSPIRV = null;
            this.patchControlPoints = 0;
        }

        public void setTessellationSPIRVs(SPIRV control, SPIRV evaluation, int patchControlPoints) {
            if (control == null || evaluation == null || patchControlPoints <= 0) {
                throw new IllegalArgumentException("Tessellation requires both stages and a positive patch size");
            }
            this.tescShaderSPIRV = control;
            this.teseShaderSPIRV = evaluation;
            this.patchControlPoints = patchControlPoints;
        }

        public void compileShaders() {
            String resourcePath = "/assets/vulkanmod/shaders/";

            setSPIRVs(compileShaderAbsoluteFile(String.format("%s%s.vsh", resourcePath, this.shaderPath), ShaderKind.VERTEX_SHADER),
                    compileShaderAbsoluteFile(String.format("%s%s.fsh", resourcePath, this.shaderPath), ShaderKind.FRAGMENT_SHADER));
        }

        public void compileShaders(String name, String vsh, String fsh) {
            setSPIRVs(compileShader(String.format("%s.vsh", name), vsh, ShaderKind.VERTEX_SHADER),
                    compileShader(String.format("%s.fsh", name), fsh, ShaderKind.FRAGMENT_SHADER));
        }

        public void parseBindingsJSON() {
            Validate.notNull(this.shaderPath, "Cannot parse bindings: shaderPath is null");

            this.UBOs = new ArrayList<>();
            this.imageDescriptors = new ArrayList<>();

            JsonObject jsonObject;

            String resourcePath = String.format("/assets/vulkanmod/shaders/%s.json", this.shaderPath);
            InputStream stream = openResource(resourcePath);

            if (stream == null)
                throw new NullPointerException(String.format("Failed to load: %s", resourcePath));

            jsonObject = GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));

            JsonArray jsonUbos = GsonHelper.getAsJsonArray(jsonObject, "UBOs", null);
            JsonArray jsonManualUbos = GsonHelper.getAsJsonArray(jsonObject, "ManualUBOs", null);
            JsonArray jsonSamplers = GsonHelper.getAsJsonArray(jsonObject, "samplers", null);
            JsonArray jsonPushConstants = GsonHelper.getAsJsonArray(jsonObject, "PushConstants", null);

            if (jsonUbos != null) {
                for (JsonElement jsonelement : jsonUbos) {
                    this.parseUboNode(jsonelement);
                }
            }

            if (jsonManualUbos != null) {
                this.parseManualUboNode(jsonManualUbos.get(0));
            }

            if (jsonSamplers != null) {
                for (JsonElement jsonelement : jsonSamplers) {
                    this.parseSamplerNode(jsonelement);
                }
            }

            if (jsonPushConstants != null) {
                this.parsePushConstantNode(jsonPushConstants);
            }
        }

        private static InputStream openResource(String resourcePath) {
            String classLoaderPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null) {
                InputStream stream = contextClassLoader.getResourceAsStream(classLoaderPath);
                if (stream != null) {
                    return stream;
                }
            }
            return Pipeline.class.getResourceAsStream(resourcePath);
        }

        private void parseUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "UBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int type = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            JsonArray fields = GsonHelper.getAsJsonArray(jsonobject, "fields");

            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for (JsonElement jsonelement2 : fields) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement2, "uniform");

                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addUniformInfo(type2, name, j);

            }
            UBO ubo = builder.buildUBO(binding, type);

            if (binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.UBOs.add(ubo);
        }

        private void parseManualUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "ManualUBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int stage = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            int size = GsonHelper.getAsInt(jsonobject, "size");

            if (binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.manualUBO = new ManualUBO(binding, stage, size);
        }

        private void parseSamplerNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "Sampler");
            String name = GsonHelper.getAsString(jsonobject, "name");

            int imageIdx = VTextureSelector.getTextureIdx(name);
            this.imageDescriptors.add(new ImageDescriptor(this.nextBinding, "sampler2D", name, imageIdx));
            this.nextBinding++;
        }

        private void parsePushConstantNode(JsonArray jsonArray) {
            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for (JsonElement jsonelement : jsonArray) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement, "PC");

                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addUniformInfo(type2, name, j);
            }

            this.pushConstants = builder.buildPushConstant();
        }

        public static int getStageFromString(String s) {
            return switch (s) {
                case "vertex" -> VK_SHADER_STAGE_VERTEX_BIT;
                case "tessellation_control" -> VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
                case "tessellation_evaluation" -> VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
                case "fragment" -> VK_SHADER_STAGE_FRAGMENT_BIT;
                case "all" -> VK_SHADER_STAGE_ALL_GRAPHICS;
                case "compute" -> VK_SHADER_STAGE_COMPUTE_BIT;

                default -> throw new RuntimeException("cannot identify type..");
            };
        }
    }
}
