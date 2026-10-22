package net.vulkanmod.render.chunk.build.task;

import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.vertex.QuadSorter;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

public class SortTransparencyTask extends ChunkTask {
    private final long transparencyRevision;
    private final QuadSorter.SortState transparencyState;

    public SortTransparencyTask(RenderSection renderSection, QuadSorter.SortState transparencyState,
                                long buildRevision, long transparencyRevision) {
        super(renderSection, buildRevision);
        this.transparencyState = transparencyState;
        this.transparencyRevision = transparencyRevision;
    }

    public String name() {
        return "rend_chk_sort";
    }

    public Result runTask(BuilderResources context) {
        ThreadBuilderPack builderPack = context.builderPack;

        if (this.cancelled.get()
                || this.section.getBuildRevision() != this.buildRevision
                || this.section.getTransparencyRevision() != this.transparencyRevision) {
            return Result.CANCELLED;
        }

        Vec3 vec3 = WorldRenderer.getCameraPos();
        float x = (float) vec3.x;
        float y = (float) vec3.y;
        float z = (float) vec3.z;

        TerrainBufferBuilder bufferBuilder = builderPack.builder(TerrainRenderType.TRANSLUCENT);
        bufferBuilder.begin();
        bufferBuilder.restoreSortState(this.transparencyState);

        bufferBuilder.setupQuadSorting(x - (float) this.section.xOffset(), y - (float) this.section.yOffset(), z - (float) this.section.zOffset());
        TerrainBufferBuilder.RenderedBuffer renderedBuffer = bufferBuilder.end();

        CompileResult compileResult = new CompileResult(
                this.section, false, this.buildRevision, this.transparencyRevision);
        UploadBuffer uploadBuffer = new UploadBuffer(renderedBuffer);
        compileResult.renderedLayers.put(TerrainRenderType.TRANSLUCENT, uploadBuffer);
        renderedBuffer.release();

        if (this.cancelled.get()
                || this.section.getBuildRevision() != this.buildRevision
                || this.section.getTransparencyRevision() != this.transparencyRevision) {
            uploadBuffer.release();
            return Result.CANCELLED;
        }
        taskDispatcher.scheduleSectionUpdate(compileResult, this);
        return Result.SUCCESSFUL;
    }
}
