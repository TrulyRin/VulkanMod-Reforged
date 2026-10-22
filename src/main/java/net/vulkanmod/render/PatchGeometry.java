package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;


public final class PatchGeometry {
    private PatchGeometry() {}
    public static void requireQuads(VertexFormat.Mode mode, int vertices, int triangleIndices, int patchVertices) {
        if (mode != VertexFormat.Mode.QUADS || vertices < 0 || vertices % 4 != 0
                || patchVertices != 4 || triangleIndices < 0 || (long) vertices / 4 * 6 != triangleIndices) {
            throw new IllegalArgumentException("Four-vertex patches require complete original QUADS and matching triangle counts");
        }
    }
    public static QuadPatchIndices.IndexType indexType(VertexFormat.IndexType type) {
        if (type == null) throw new IllegalArgumentException("Declared index type is required");
        return switch (type) {
            case SHORT -> QuadPatchIndices.IndexType.U16;
            case INT -> QuadPatchIndices.IndexType.U32;
        };
    }
}
