package net.vulkanmod.render.vertex;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.vulkanmod.vulkan.VRenderSystem;

import java.util.EnumSet;

public enum TerrainRenderType {
    SOLID(0.0f),
    CUTOUT_MIPPED(0.5f),
    CUTOUT(0.1f),
    TRANSLUCENT(0.0f),
    TRIPWIRE(0.1f);

    public static final TerrainRenderType[] VALUES = TerrainRenderType.values();

    public static final EnumSet<TerrainRenderType> COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, TRANSLUCENT);
    public static final EnumSet<TerrainRenderType> SEMI_COMPACT_RENDER_TYPES = EnumSet.of(CUTOUT_MIPPED, CUTOUT, TRANSLUCENT);

    static {
        SEMI_COMPACT_RENDER_TYPES.add(CUTOUT);
        SEMI_COMPACT_RENDER_TYPES.add(CUTOUT_MIPPED);
        SEMI_COMPACT_RENDER_TYPES.add(TRANSLUCENT);

        COMPACT_RENDER_TYPES.add(CUTOUT_MIPPED);
        COMPACT_RENDER_TYPES.add(TRANSLUCENT);
    }

    public final float alphaCutout;

    TerrainRenderType(float alphaCutout) {
        this.alphaCutout = alphaCutout;
    }

    public void setCutoutUniform() {
        VRenderSystem.alphaCutout = this.alphaCutout;
    }

       
                                                                                                 
                                                                                             
                                                                            
       
    public static TerrainRenderType get(ChunkSectionLayer renderType) {
        return switch (renderType) {
            case SOLID -> TerrainRenderType.SOLID;
            case CUTOUT -> TerrainRenderType.CUTOUT;
            case TRANSLUCENT -> TerrainRenderType.TRANSLUCENT;
            case TRIPWIRE -> TerrainRenderType.TRIPWIRE;
        };
    }

    public static TerrainRenderType get(String name) {
        return switch (name) {
            case "solid" -> TerrainRenderType.SOLID;
            case "cutout" -> TerrainRenderType.CUTOUT;
            case "cutout_mipped" -> TerrainRenderType.CUTOUT_MIPPED;
            case "translucent" -> TerrainRenderType.TRANSLUCENT;
            case "tripwire" -> TerrainRenderType.TRIPWIRE;
            default -> null;
        };
    }

    public static ChunkSectionLayer getRenderType(TerrainRenderType renderType) {
        return switch (renderType) {
            case SOLID -> ChunkSectionLayer.SOLID;
            case CUTOUT -> ChunkSectionLayer.CUTOUT;
            case CUTOUT_MIPPED -> ChunkSectionLayer.CUTOUT;
            case TRANSLUCENT -> ChunkSectionLayer.TRANSLUCENT;
            case TRIPWIRE -> ChunkSectionLayer.TRIPWIRE;
        };
    }
}
