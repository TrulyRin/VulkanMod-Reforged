package net.vulkanmod.render.chunk.build.task;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.vulkanmod.interfaces.VisibilitySetExtended;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.build.BlockLayerMesh;
import net.vulkanmod.render.VBO;
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
    private final long buildRevision;
    private final long transparencyRevision;

    final List<BlockEntity> globalBlockEntities = new ArrayList<>();
    final List<BlockEntity> blockEntities = new ArrayList<>();
    public final EnumMap<TerrainRenderType, UploadBuffer> renderedLayers = new EnumMap<>(TerrainRenderType.class);
    public final EnumMap<TerrainRenderType, DrawBuffers.PendingUpload> stagedLayers =
            new EnumMap<>(TerrainRenderType.class);

    public BlockLayerMesh blockLayerMesh;
    private VBO stagedBlockLayer;

    VisibilitySet visibilitySet;
    QuadSorter.SortState transparencyState;
    CompiledSection compiledSection;

    CompileResult(RenderSection renderSection, boolean fullUpdate,
                  long buildRevision, long transparencyRevision) {
        this.renderSection = renderSection;
        this.fullUpdate = fullUpdate;
        this.chunkArea = renderSection.getChunkArea();
        this.xOffset = renderSection.xOffset();
        this.yOffset = renderSection.yOffset();
        this.zOffset = renderSection.zOffset();
        this.buildRevision = buildRevision;
        this.transparencyRevision = transparencyRevision;
    }

    public boolean matchesCurrentSection() {
        return this.matchesCurrentLocation()
                && this.renderSection.getBuildRevision() == this.buildRevision
                && (this.fullUpdate
                || this.renderSection.getTransparencyRevision() == this.transparencyRevision);
    }

    public boolean matchesCurrentLocation() {
        return this.renderSection.getChunkArea() == this.chunkArea
                && this.renderSection.xOffset() == this.xOffset
                && this.renderSection.yOffset() == this.yOffset
                && this.renderSection.zOffset() == this.zOffset;
    }


    public boolean stageBlockLayer() {
        if (!this.fullUpdate || this.blockLayerMesh == null) return false;
        BlockLayerMesh cpu = this.blockLayerMesh;
        this.blockLayerMesh = null;
        VBO candidate = null;
        try (cpu) {
            candidate = new VBO();
            candidate.upload(cpu.mesh());
        } catch (RuntimeException | Error failure) {
            if (candidate != null) {
                try { candidate.close(); }
                catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
        this.stagedBlockLayer = candidate;
        return true;
    }


    public boolean commitBlockLayer() {
        if (!this.matchesCurrentSection()) {
            this.releaseBuffers();
            return false;
        }
        if (!this.fullUpdate) return true;
        VBO replacement = this.stagedBlockLayer;
        this.stagedBlockLayer = null;
        this.renderSection.replaceBlockLayerVbo(replacement);
        return true;
    }

    public void releaseBuffers() {
        Throwable failure = null;
        for (UploadBuffer buffer : this.renderedLayers.values()) {
            try { buffer.release(); }
            catch (RuntimeException | Error cleanup) { failure = accumulate(failure, cleanup); }
        }
        this.renderedLayers.clear();
        for (DrawBuffers.PendingUpload buffer : this.stagedLayers.values()) {
            try { buffer.discard(); }
            catch (RuntimeException | Error cleanup) { failure = accumulate(failure, cleanup); }
        }
        this.stagedLayers.clear();
        BlockLayerMesh cpu = this.blockLayerMesh;
        this.blockLayerMesh = null;
        if (cpu != null) {
            try { cpu.close(); }
            catch (RuntimeException | Error cleanup) { failure = accumulate(failure, cleanup); }
        }
        VBO candidate = this.stagedBlockLayer;
        this.stagedBlockLayer = null;
        if (candidate != null) {
            try { candidate.close(); }
            catch (RuntimeException | Error cleanup) { failure = accumulate(failure, cleanup); }
        }
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

    private static Throwable accumulate(Throwable failure, Throwable cleanup) {
        if (failure == null) return cleanup;
        if (failure != cleanup) failure.addSuppressed(cleanup);
        return failure;
    }

    public void updateSection() {
        this.renderSection.updateGlobalBlockEntities(globalBlockEntities);
        this.renderSection.setCompiledSection(compiledSection, this.buildRevision);
        this.renderSection.setVisibility(((VisibilitySetExtended)visibilitySet).getVisibility());
        this.renderSection.setCompletelyEmpty(compiledSection.isCompletelyEmpty);
        this.renderSection.setContainsBlockEntities(!blockEntities.isEmpty());
    }
}
