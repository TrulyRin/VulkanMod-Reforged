package net.vulkanmod.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;


public final class PreparedDraw {
    Drawer owner;
    long generation;
    boolean valid;
    VertexFormat.Mode mode;
    VertexFormat.IndexType indexType;
    int vertexCount, sourceIndexCount, indexCount;
    long vertexBuffer, vertexOffset, indexBuffer, indexOffset, indexAddress;
    int nativeIndexType;
    boolean customIndices, patchPrepared;
    long patchBuffer, patchOffset;
    int patchCount, patchType, patchSourceCount;

    public void invalidate() {
        valid = false;
        owner = null;
        vertexBuffer = vertexOffset = indexBuffer = indexOffset = indexAddress = 0;
        vertexCount = sourceIndexCount = indexCount = 0;
        customIndices = false;
        clearPatches();
    }

    void clearPatches() {
        patchPrepared = false;
        patchBuffer = patchOffset = 0;
        patchCount = patchSourceCount = 0;
    }
}
