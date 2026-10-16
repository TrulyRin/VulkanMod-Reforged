package net.vulkanmod.compat.external;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.VkGlBuffer;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.IndexBuffer;
import net.vulkanmod.vulkan.memory.buffer.VertexBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

                                                                                                     
public final class ExternalTerrainRenderBridge {
    private static final ExternalTerrainSubmission<Buffer> SUBMISSION = new ExternalTerrainSubmission<>(
            new ExternalTerrainSubmission.Backend<>() {
                @Override
                public Buffer uploadVertices(ByteBuffer data) {
                    VertexBuffer buffer = new VertexBuffer(data.remaining(), MemoryTypes.HOST_MEM);
                    return upload(buffer, data);
                }

                @Override
                public Buffer uploadIndices(ByteBuffer data, int indexBytes) {
                    IndexBuffer buffer = new IndexBuffer(data.remaining(), MemoryTypes.HOST_MEM,
                            indexBytes == 2 ? IndexBuffer.IndexType.UINT16 : IndexBuffer.IndexType.UINT32);
                    return upload(buffer, data);
                }

                @Override
                public void submit(Buffer vertices, Buffer indices, int indexBytes, int count) {
                    GraphicsPipeline pipeline = PipelineManager.getExternalLodPipeline();
                    if (pipeline == null) throw new IllegalStateException("External LOD pipeline is unavailable");
                    Minecraft minecraft = Minecraft.getInstance();
                    RenderTarget target = minecraft.getMainRenderTarget();
                    GpuTextureView previousTexture = VRenderSystem.getShaderTexture(0);
                    VulkanImage previousImage = VTextureSelector.getImage(0);
                    int previousTopology = VRenderSystem.topology;
                                                                                                      
                                                                                                          
                    try (RenderPass ignored = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                            () -> "Vulkan external LOD", target.getColorTextureView(), OptionalInt.empty(),
                            target.getDepthTextureView(), OptionalDouble.empty())) {
                        VRenderSystem.setPrimitiveTopologyGL(4);
                        VRenderSystem.setShaderTexture(0, minecraft.gameRenderer.lightTexture().getTextureView());
                        VTextureSelector.bindShaderTextures(pipeline);
                        Renderer renderer = Renderer.getInstance();
                        renderer.bindGraphicsPipeline(pipeline);
                        renderer.uploadAndBindUBOs(pipeline);
                        Renderer.getDrawer().drawIndexed(vertices, (IndexBuffer) indices, count);
                    } finally {
                        VRenderSystem.topology = previousTopology;
                        VRenderSystem.setShaderTexture(0, previousTexture);
                        VTextureSelector.bindTexture(0, previousImage);
                    }
                }

                @Override
                public void release(Buffer buffer) {
                    buffer.scheduleFree();
                }
            });

    private ExternalTerrainRenderBridge() {}

                                                                                                  
    private static final class UniformStorage {
        static final MappedBuffer MATRIX = new MappedBuffer(16 * Float.BYTES);
        static final MappedBuffer MODEL_OFFSET = new MappedBuffer(4 * Float.BYTES);
        static final MappedBuffer PARAMS = new MappedBuffer(4 * Float.BYTES);
    }

    public static MappedBuffer getCombinedMatrix() { return UniformStorage.MATRIX; }
    public static MappedBuffer getModelOffsetAndYOffset() { return UniformStorage.MODEL_OFFSET; }
    public static MappedBuffer getRenderParams() { return UniformStorage.PARAMS; }

    public static boolean drawElements(int mode, int count, int type, long offset) {
        if (mode == 4 && count == 0) return true;
        if (!ExternalRenderPathSupport.shouldDrawExternalLodBridge() || !isLegacyTerrainProgram()) return false;
        if (Renderer.getInstance() == null || !Renderer.isRecording()) return false;
        VkGlBuffer vertices = VkGlBuffer.getArrayBufferBound();
        VkGlBuffer indices = VkGlBuffer.getElementArrayBufferBound();
        if (vertices == null || indices == null) return false;
        syncUniforms();
        return SUBMISSION.draw(true, true, mode, count, type, offset,
                vertices.getId(), vertices.getVersion(), vertices.copyData(),
                indices.getId(), indices.getVersion(), indices.copyData()) == ExternalTerrainSubmission.Outcome.SUBMITTED;
    }

    private static boolean isLegacyTerrainProgram() {
        float[] matrix = GlProgram.getFloatUniform("uCombinedMatrix");
        float[] model = GlProgram.getFloatUniform("uModelOffset");
        return matrix != null && matrix.length >= 16 && model != null && model.length >= 3;
    }

    private static Buffer upload(Buffer buffer, ByteBuffer data) {
        try {
            buffer.copyBuffer(data, data.remaining(), 0);
            return buffer;
        } catch (RuntimeException | Error failure) {
            buffer.scheduleFree();
            throw failure;
        }
    }

    private static void syncUniforms() {
        float[] matrix = GlProgram.getFloatUniform("uCombinedMatrix");
        getCombinedMatrix().buffer.asFloatBuffer().put(matrix, 0, 16);
        float[] model = GlProgram.getFloatUniform("uModelOffset");
        float[] camera = GlProgram.getFloatUniform("uCameraPos");
        float[] yOffset = GlProgram.getFloatUniform("uWorldYOffset");
        for (int i = 0; i < 3; i++) {
            getModelOffsetAndYOffset().putFloat(i * Float.BYTES,
                    value(model, i, 0.0f) - value(camera, i, 0.0f));
        }
        getModelOffsetAndYOffset().putFloat(12, value(yOffset, 0, 0.0f));
        getRenderParams().putFloat(0, value(GlProgram.getFloatUniform("uMircoOffset"), 0, 0.01f));
        getRenderParams().putFloat(4, value(GlProgram.getFloatUniform("uClipDistance"), 0, 0.0f));
        getRenderParams().putFloat(8, flag("uIsWhiteWorld") ? 1.0f : 0.0f);
        getRenderParams().putFloat(12, flag("uDitherExternalLodRendering") ? 1.0f : 0.0f);
    }

    private static float value(float[] values, int index, float fallback) {
        return values != null && values.length > index ? values[index] : fallback;
    }

    private static boolean flag(String name) {
        int[] values = GlProgram.getIntUniform(name);
        return values != null && values.length > 0 ? values[0] != 0
                : value(GlProgram.getFloatUniform(name), 0, 0.0f) != 0.0f;
    }

    public static void onBufferDeleted(int id) { SUBMISSION.onBufferDeleted(id); }
    public static void close() { SUBMISSION.close(); }
}
