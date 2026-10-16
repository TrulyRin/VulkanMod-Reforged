package net.vulkanmod.render.gui;

import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.engine.VkGpuBuffer;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.texture.ImageUtil;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

                                                                                                        
public final class InventoryAtlasDiagnostics {
    public static final String PROPERTY = "vulkanmod.debug.inventoryAtlas";
    public static final String OUTPUT_PROPERTY = "vulkanmod.debug.inventoryAtlas.outputDirectory";
    public static final boolean ENABLED = Boolean.getBoolean(PROPERTY);

    private static final class Session {
        static final Path DIRECTORY = Path.of(System.getProperty(OUTPUT_PROPERTY, "vulkanmod-diagnostics"));
        static final InventoryAtlasCapture<GpuTexture> CAPTURE =
                new InventoryAtlasCapture<>(true, DIRECTORY, InventoryAtlasDiagnostics::readPixels);
    }

    private InventoryAtlasDiagnostics() {}

    public static void beginPass(int itemModelCount) {
        if (ENABLED) Session.CAPTURE.beginPass(itemModelCount);
    }

    public static boolean shouldObserveDraw() {
        if (!ENABLED || !Session.CAPTURE.isCollecting()) return false;
        var view = RenderSystem.outputColorTextureOverride;
        return view != null && "UI items atlas".equals(view.texture().getLabel());
    }

    public static DrawObservation describeDraw(RenderPipeline pipeline, MeshData mesh) {
        var scissor = RenderSystem.getScissorStateForRenderTypeDraws();
        int[] rectangle = scissor.enabled()
                ? new int[]{scissor.x(), scissor.y(), scissor.width(), scissor.height()} : new int[0];
        var projection = RenderSystem.getProjectionMatrixBuffer();
        String projectionDescription = "unbound";
        if (projection != null && projection.buffer() instanceof VkGpuBuffer buffer) {
            projectionDescription = buffer.getBuffer().name + " offset=" + projection.offset() + " bytes=" + projection.length();
        }
        return new DrawObservation(pipeline.getLocation().toString(), mesh.drawState().vertexCount(),
                mesh.drawState().indexCount(), rectangle, vertexBounds(pipeline.getVertexFormat(), mesh),
                projectionDescription);
    }

    public static void finishDraw(DrawObservation draw, boolean completed) {
        if (!ENABLED) return;
        Session.CAPTURE.recordDraw(draw.pipeline, draw.vertices, draw.indices, draw.scissor,
                draw.vertexBounds, draw.projectionBuffer, completed);
    }

    public static void completePass(GpuTexture atlas) {
        if (!ENABLED || Session.CAPTURE.hasAttempted()) return;
        boolean success = Session.CAPTURE.completePass(atlas,
                atlas == null ? 0 : atlas.getWidth(0), atlas == null ? 0 : atlas.getHeight(0));
        if (!Session.CAPTURE.hasAttempted()) return;
        if (success) {
            Initializer.LOGGER.info("Inventory atlas diagnostic saved {} ({} nontransparent pixels)",
                    Session.DIRECTORY.toAbsolutePath(), Session.CAPTURE.nonTransparentPixels());
        } else {
            Initializer.LOGGER.warn("Inventory atlas diagnostic stopped after one attempt: {}", Session.CAPTURE.failure());
        }
    }

    static byte[] readPixels(GpuTexture texture, int width, int height) {
        if (texture.isClosed()) throw new IllegalStateException("Inventory atlas is closed");
        if (!(texture instanceof VkGpuTexture atlas) || texture.getFormat() != TextureFormat.RGBA8
                || atlas.getVulkanImage() == null) {
            throw new IllegalStateException("Inventory atlas is not a Vulkan RGBA texture");
        }
        int bytes = Math.multiplyExact(Math.multiplyExact(width, height), 4);
                                                                                        
                                                                                           
        try (GpuFence fence = RenderSystem.getDevice().createCommandEncoder().createFence()) {
            if (!fence.awaitCompletion(Long.MAX_VALUE)) {
                throw new IllegalStateException("Inventory atlas draw fence did not complete");
            }
        }
        ByteBuffer pixels = MemoryUtil.memAlloc(bytes);
        try {
            ImageUtil.downloadTexture(atlas.getVulkanImage(), MemoryUtil.memAddress(pixels));
            byte[] result = new byte[bytes];
            pixels.get(result);
            return result;
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    private static float[] vertexBounds(VertexFormat format, MeshData mesh) {
        if (!format.contains(VertexFormatElement.POSITION)
                || VertexFormatElement.POSITION.type() != VertexFormatElement.Type.FLOAT) return new float[0];
        ByteBuffer vertices = mesh.vertexBuffer().duplicate().order(ByteOrder.nativeOrder());
        int stride = format.getVertexSize();
        int positionOffset = format.getOffset(VertexFormatElement.POSITION);
        int count = Math.min(mesh.drawState().vertexCount(), 256);
        float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (int i = 0; i < count; i++) {
            int offset = vertices.position() + i * stride + positionOffset;
            if (offset < 0 || offset > vertices.limit() - 12) return new float[0];
            for (int axis = 0; axis < 3; axis++) {
                float value = vertices.getFloat(offset + axis * Float.BYTES);
                if (!Float.isFinite(value)) return new float[0];
                bounds[axis] = Math.min(bounds[axis], value);
                bounds[axis + 3] = Math.max(bounds[axis + 3], value);
            }
        }
        return count == 0 ? new float[0] : bounds;
    }

    public record DrawObservation(String pipeline, int vertices, int indices, int[] scissor,
                                  float[] vertexBounds, String projectionBuffer) {}
}
