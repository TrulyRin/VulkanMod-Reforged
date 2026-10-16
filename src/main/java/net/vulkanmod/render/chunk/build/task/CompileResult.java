package net.vulkanmod.render.chunk.build.task;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.interfaces.VisibilitySetExtended;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.vertex.QuadSorter;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class CompileResult {
    public final RenderSection renderSection;
    public final boolean fullUpdate;
    private final ChunkArea chunkArea;
    private final int xOffset;
    private final int yOffset;
    private final int zOffset;

    final List<BlockEntity> globalBlockEntities = new ArrayList<>();
    final List<BlockEntity> blockEntities = new ArrayList<>();
    public final EnumMap<TerrainRenderType, UploadBuffer> renderedLayers = new EnumMap<>(TerrainRenderType.class);

    VisibilitySet visibilitySet;
    QuadSorter.SortState transparencyState;
    CompiledSection compiledSection;

    CompileResult(RenderSection renderSection, boolean fullUpdate) {
        this.renderSection = renderSection;
        this.fullUpdate = fullUpdate;
        this.chunkArea = renderSection.getChunkArea();
        this.xOffset = renderSection.xOffset();
        this.yOffset = renderSection.yOffset();
        this.zOffset = renderSection.zOffset();
    }

    public boolean matchesCurrentSection() {
        return this.renderSection.getChunkArea() == this.chunkArea
                && this.renderSection.xOffset() == this.xOffset
                && this.renderSection.yOffset() == this.yOffset
                && this.renderSection.zOffset() == this.zOffset;
    }

    public void releaseBuffers() {
        this.renderedLayers.values().forEach(UploadBuffer::release);
        this.renderedLayers.clear();
    }

    public void updateSection() {
        this.renderSection.updateGlobalBlockEntities(globalBlockEntities);
        this.renderSection.setCompiledSection(compiledSection);
        this.renderSection.setVisibility(((VisibilitySetExtended)visibilitySet).getVisibility());
        this.renderSection.setCompletelyEmpty(compiledSection.isCompletelyEmpty);
        this.renderSection.setContainsBlockEntities(!blockEntities.isEmpty());
    }
}

