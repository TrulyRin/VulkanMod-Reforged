package net.vulkanmod.render.chunk.build.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.BlockRenderer;
import net.vulkanmod.render.chunk.build.LiquidRenderer;
import net.vulkanmod.render.chunk.build.RenderRegion;
import net.vulkanmod.render.chunk.build.UploadBuffer;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BuildTask extends ChunkTask {
    @Nullable
    protected RenderRegion region;

    public BuildTask(RenderSection renderSection, RenderRegion renderRegion, boolean highPriority,
                     long buildRevision) {
        super(renderSection, buildRevision);
        this.region = renderRegion;
        this.highPriority = highPriority;
    }

    public String name() {
        return "rend_chk_rebuild";
    }

    public Result runTask(BuilderResources builderResources) {
        long startTime = System.nanoTime();

        if (this.cancelled.get() || this.section.getBuildRevision() != this.buildRevision) {
            this.clearPayload();
            return Result.CANCELLED;
        }

        try {
            Vec3 vec3 = WorldRenderer.getCameraPos();
            float x = (float) vec3.x;
            float y = (float) vec3.y;
            float z = (float) vec3.z;
            CompileResult compileResult = this.compile(x, y, z, builderResources);
            if (compileResult == null) {
                return Result.CANCELLED;
            }

            CompiledSection compiledSection = new CompiledSection();
            compiledSection.blockEntities.addAll(compileResult.blockEntities);
            compiledSection.transparencyState = compileResult.transparencyState;
            compiledSection.isCompletelyEmpty = compileResult.renderedLayers.isEmpty();
            compileResult.compiledSection = compiledSection;

            if (this.cancelled.get() || this.section.getBuildRevision() != this.buildRevision) {
                compileResult.releaseBuffers();
                return Result.CANCELLED;
            }

            taskDispatcher.scheduleSectionUpdate(compileResult, this);

            float buildTime = (System.nanoTime() - startTime) * 0.000001f;
            if (BENCH) {
                builderResources.updateBuildStats((int) buildTime);
            }

            return Result.SUCCESSFUL;
        } finally {
            this.clearPayload();
            builderResources.clear();
        }
    }

    @Nullable
    private CompileResult compile(float camX, float camY, float camZ, BuilderResources builderResources) {
        CompileResult compileResult = new CompileResult(this.section, true, this.buildRevision, -1L);

        BlockPos startBlockPos = new BlockPos(section.xOffset(), section.yOffset(), section.zOffset()).immutable();
        VisGraph visGraph = new VisGraph();

        if (this.region == null) {
            compileResult.visibilitySet = visGraph.resolve();
            return compileResult;
        }

        Vector3f pos = new Vector3f();
        ThreadBuilderPack bufferBuilders = builderResources.builderPack;
        setupBufferBuilders(bufferBuilders);

        this.region.loadBlockStates(builderResources.blockStates);
        this.region.initTintCache(builderResources.tintCache);

        builderResources.update(this.region, this.section);

        BlockRenderer blockRenderer = builderResources.blockRenderer;

        LiquidRenderer liquidRenderer = builderResources.liquidRenderer;

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        RandomSource randomSource = builderResources.randomSource;

        for (int y = 0; y < 16; ++y) {
            if (this.cancelled.get() || this.section.getBuildRevision() != this.buildRevision) {
                return null;
            }
            for (int z = 0; z < 16; ++z) {
                for (int x = 0; x < 16; ++x) {
                    blockPos.set(section.xOffset() + x, section.yOffset() + y, section.zOffset() + z);

                    BlockState blockState = this.region.getBlockState(blockPos);
                    if (blockState.isSolidRender(this.region, blockPos)) {
                        visGraph.setOpaque(blockPos);
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity blockEntity = this.region.getBlockEntity(blockPos);
                        if (blockEntity != null) {
                            this.handleBlockEntity(compileResult, blockEntity);
                        }
                    }

                    FluidState fluidState = blockState.getFluidState();
                    TerrainRenderType renderType;
                    TerrainBufferBuilder bufferBuilder;
                    if (!fluidState.isEmpty()) {
                        renderType = TerrainRenderType.get(ItemBlockRenderTypes.getRenderLayer(fluidState));

                        bufferBuilder = getBufferBuilder(bufferBuilders, renderType);
                        bufferBuilder.setBlockAttributes(blockState);

                        liquidRenderer.renderLiquid(blockState, fluidState, blockPos, bufferBuilder);
                    }

                    if (blockState.getRenderShape() == RenderShape.MODEL) {
                        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
                        ModelData modelData = getModelData(blockPos, blockState, model);
                        randomSource.setSeed(blockState.getSeed(blockPos));
                        ChunkRenderTypeSet renderTypes = model.getRenderTypes(blockState, randomSource, modelData);

                        for (RenderType chunkRenderType : renderTypes) {
                            renderType = TerrainRenderType.get(chunkRenderType);

                            bufferBuilder = getBufferBuilder(bufferBuilders, renderType);
                            bufferBuilder.setBlockAttributes(blockState);

                            pos.set(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
                            blockRenderer.renderBlock(blockState, blockPos, pos, bufferBuilder, model, modelData, chunkRenderType);
                        }
                    }
                }
            }
        }

        TerrainBufferBuilder translucentBufferBuilder = bufferBuilders.builder(TerrainRenderType.TRANSLUCENT);
        if (!translucentBufferBuilder.isCurrentBatchEmpty()) {
            translucentBufferBuilder.setupQuadSortingPoints();
            translucentBufferBuilder.setupQuadSorting(camX - (float) startBlockPos.getX(), camY - (float) startBlockPos.getY(), camZ - (float) startBlockPos.getZ());
            compileResult.transparencyState = translucentBufferBuilder.getSortState();
        }

        for (TerrainRenderType renderType : TerrainRenderType.VALUES) {
            TerrainBufferBuilder.RenderedBuffer renderedBuffer = bufferBuilders.builder(renderType).end();
            if (renderedBuffer != null) {
                UploadBuffer uploadBuffer = new UploadBuffer(renderedBuffer);
                compileResult.renderedLayers.put(renderType, uploadBuffer);
                renderedBuffer.release();
            }
        }

        compileResult.visibilitySet = visGraph.resolve();
        return compileResult;
    }

    @Override
    protected void clearPayload() {
        this.region = null;
    }

    @Override
    public void recoverFromFailure() {
        this.section.setDirtyAfterBuildFailure(this.buildRevision);
    }

    private ModelData getModelData(BlockPos blockPos, BlockState blockState, BakedModel model) {
        ModelData modelData = ModelData.EMPTY;
        if (blockState.hasBlockEntity() && this.region != null) {
            BlockEntity blockEntity = this.region.getBlockEntity(blockPos);
            if (blockEntity != null) {
                modelData = blockEntity.getModelData();
            }
        }

        return model.getModelData(this.region, blockPos, blockState, modelData);
    }

    private void setupBufferBuilders(ThreadBuilderPack builderPack) {
        for (TerrainRenderType renderType : TerrainRenderType.VALUES) {
            TerrainBufferBuilder bufferBuilder = builderPack.builder(renderType);
            bufferBuilder.begin();
        }
    }

    private TerrainBufferBuilder getBufferBuilder(ThreadBuilderPack bufferBuilders, TerrainRenderType renderType) {
        renderType = compactRenderTypes(renderType);
        return bufferBuilders.builder(renderType);
    }

    private TerrainRenderType compactRenderTypes(TerrainRenderType renderType) {
        if (Initializer.CONFIG.uniqueOpaqueLayer) {
            renderType = switch (renderType) {
                case SOLID, CUTOUT, CUTOUT_MIPPED -> TerrainRenderType.CUTOUT_MIPPED;
                case TRANSLUCENT, TRIPWIRE -> TerrainRenderType.TRANSLUCENT;
            };
        } else {
            renderType = switch (renderType) {
                case SOLID, CUTOUT_MIPPED -> TerrainRenderType.CUTOUT_MIPPED;
                case CUTOUT -> TerrainRenderType.CUTOUT;
                case TRANSLUCENT, TRIPWIRE -> TerrainRenderType.TRANSLUCENT;
            };
        }

        return renderType;
    }

    private <E extends BlockEntity> void handleBlockEntity(CompileResult compileResult, E blockEntity) {
        BlockEntityRenderer<E> blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
        if (blockEntityRenderer != null) {
            compileResult.blockEntities.add(blockEntity);
            if (blockEntityRenderer.shouldRenderOffScreen(blockEntity)) {
                compileResult.globalBlockEntities.add(blockEntity);
            }
        }

    }
}
