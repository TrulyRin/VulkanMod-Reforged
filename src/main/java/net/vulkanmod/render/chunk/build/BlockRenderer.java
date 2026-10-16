package net.vulkanmod.render.chunk.build;

import net.minecraft.client.renderer.block.model.BlockModelPart;
import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.model.data.ModelData;
import net.minecraft.util.TriState;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.chunk.build.thread.BuilderResources;
import net.vulkanmod.render.chunk.build.task.BuildTask;
import net.vulkanmod.render.model.quad.QuadUtils;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.render.vertex.VertexUtil;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.joml.Vector3f;

import java.util.List;

public class BlockRenderer {

    static final Direction[] DIRECTIONS = Direction.values();
    private static BlockColors blockColors;

    RandomSource randomSource = RandomSource.createNewThreadLocalInstance();

    Vector3f pos;
    BlockPos blockPos;
    BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

    BuilderResources resources;

    BlockState blockState;

                                                                          
    private boolean leavesCulling;
    private boolean backFaceCulling;
    private boolean uniqueOpaqueLayer;

    public void setResources(BuilderResources resources) {
        this.resources = resources;
        this.leavesCulling = net.vulkanmod.Initializer.CONFIG.leavesCulling;
        this.backFaceCulling = net.vulkanmod.Initializer.CONFIG.backFaceCulling;
        this.uniqueOpaqueLayer = net.vulkanmod.Initializer.CONFIG.uniqueOpaqueLayer;
    }

    final Object2ByteLinkedOpenHashMap<OcclusionKey> occlusionCache = new Object2ByteLinkedOpenHashMap<>(2048, 0.25F) {
        protected void rehash(int i) {
        }
    };

       
                                                                                           
                                                       
       
    private record OcclusionKey(BlockState first, BlockState second, Direction direction) {
    }

    public BlockRenderer() {
        occlusionCache.defaultReturnValue((byte) 127);
    }

    public static void setBlockColors(BlockColors blockColors) {
        BlockRenderer.blockColors = blockColors;
    }

    public void renderBlock(BlockState blockState, BlockPos blockPos, Vector3f pos, TerrainBufferBuilder bufferBuilder,
                            BlockStateModel model, ModelData modelData, ChunkSectionLayer renderType) {
        this.pos = pos;
        this.blockPos = blockPos;
        this.blockState = blockState;

        long seed = blockState.getSeed(blockPos);

        tessellateBlock(model, modelData, renderType, bufferBuilder, seed);
    }

    public void tessellateBlock(BlockStateModel bakedModel, ModelData modelData, ChunkSectionLayer renderType,
                                TerrainBufferBuilder bufferBuilder, long seed) {
        Vec3 offset = blockState.getOffset(blockPos);

        pos.add((float) offset.x, (float) offset.y, (float) offset.z);

                                                                                          
                                                                                                    
        randomSource.setSeed(seed);
        List<BlockModelPart> parts = bakedModel.collectParts(resources.region, blockPos, blockState, randomSource);

        for (int p = 0; p < parts.size(); ++p) {
            BlockModelPart part = parts.get(p);
            ChunkSectionLayer material = part.getRenderType(blockState);
            TerrainBufferBuilder partBuffer = resources.builderPack.builder(BuildTask.compactRenderType(
                    TerrainRenderType.get(material), this.uniqueOpaqueLayer));
            TriState ambientOcclusion = part.ambientOcclusion();

            boolean useAO = Minecraft.useAmbientOcclusion()
                            && ambientOcclusion != TriState.FALSE
                            && (ambientOcclusion == TriState.TRUE || blockState.getLightEmission(resources.region, blockPos) == 0);
            LightPipeline lightPipeline = useAO ? resources.smoothLightPipeline : resources.flatLightPipeline;

            for (int i = 0; i < DIRECTIONS.length; ++i) {
                Direction direction = DIRECTIONS[i];

                List<BakedQuad> quads = part.getQuads(direction);

                if (!quads.isEmpty()) {
                    mutableBlockPos.setWithOffset(blockPos, direction);
                    if (shouldRenderFace(blockState, direction, mutableBlockPos)) {
                        renderModelFace(partBuffer, quads, lightPipeline, direction);
                    }
                }
            }

            List<BakedQuad> unculled = part.getQuads(null);
            if (!unculled.isEmpty()) {
                renderModelFace(partBuffer, unculled, lightPipeline, null);
            }
        }
    }

    private void renderModelFace(TerrainBufferBuilder bufferBuilder, List<BakedQuad> quads, LightPipeline lightPipeline, Direction cullFace) {
        QuadLightData quadLightData = resources.quadLightData;

        for (int i = 0; i < quads.size(); ++i) {
            BakedQuad bakedQuad = quads.get(i);
            QuadView quadView = (QuadView) (Object) bakedQuad;
            LightPipeline quadPipeline = bakedQuad.hasAmbientOcclusion() ? lightPipeline : resources.flatLightPipeline;
            quadPipeline.calculate(quadView, blockPos, quadLightData, cullFace, bakedQuad.direction(), bakedQuad.shade());
            putQuadData(bufferBuilder, quadView, quadLightData);
        }
    }

    private void putQuadData(TerrainBufferBuilder bufferBuilder, QuadView quadView, QuadLightData quadLightData) {
        float r, g, b;
        if (quadView.isTinted()) {
            int color = blockColors.getColor(blockState, resources.region, blockPos, quadView.getColorIndex());
            r = ColorUtil.ARGB.unpackR(color);
            g = ColorUtil.ARGB.unpackG(color);
            b = ColorUtil.ARGB.unpackB(color);
        } else {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        putQuadData(bufferBuilder, pos, quadView, quadLightData, r, g, b);
    }

    public static void putQuadData(TerrainBufferBuilder bufferBuilder, Vector3f pos, QuadView quad, QuadLightData quadLightData, float red, float green, float blue) {
        Vec3i normal = quad.getFacingDirection().getUnitVec3i();
        int packedNormal = VertexUtil.packNormal(normal.getX(), normal.getY(), normal.getZ());

        float[] brightnessArr = quadLightData.br;
        int[] lights = quadLightData.lm;

        int idx = QuadUtils.getIterationStartIdx(brightnessArr, lights);

        bufferBuilder.ensureCapacity();

        for (byte i = 0; i < 4; ++i) {
            final float x = pos.x() + quad.getX(idx);
            final float y = pos.y() + quad.getY(idx);
            final float z = pos.z() + quad.getZ(idx);

            final float r, g, b;
            final float quadR, quadG, quadB;

            final int quadColor = quad.getColor(idx);
            quadR = ColorUtil.RGBA.unpackR(quadColor);
            quadG = ColorUtil.RGBA.unpackG(quadColor);
            quadB = ColorUtil.RGBA.unpackB(quadColor);

            final float brightness = brightnessArr[idx];
            r = quadR * brightness * red;
            g = quadG * brightness * green;
            b = quadB * brightness * blue;

            final int color = ColorUtil.RGBA.pack(r, g, b, 1.0f);
            final int light = lights[idx];
            final float u = quad.getU(idx);
            final float v = quad.getV(idx);

            bufferBuilder.vertex(x, y, z, color, u, v, light, packedNormal);

            idx = (idx + 1) & 0b11;
        }

    }

    public boolean shouldRenderFace(BlockState blockState, Direction direction, BlockPos adjPos) {
        if (!this.backFaceCulling) return true;
        BlockGetter blockGetter = resources.region;
        BlockState adjBlockState = blockGetter.getBlockState(adjPos);

        if (blockState.skipRendering(adjBlockState, direction)) {
            return false;
        }

                                                                       
                                         
        if (this.leavesCulling
                && blockState.is(BlockTags.LEAVES)
                && adjBlockState.is(BlockTags.LEAVES)) {
            return false;
        }

        if (adjBlockState.canOcclude()) {
            VoxelShape shape = blockState.getFaceOcclusionShape(direction);

            if (shape.isEmpty())
                return true;

            VoxelShape adjShape = adjBlockState.getFaceOcclusionShape(direction.getOpposite());

            if (adjShape.isEmpty())
                return true;

            if (shape == Shapes.block() && adjShape == Shapes.block()) {
                return false;
            }

            OcclusionKey blockStatePairKey = new OcclusionKey(blockState, adjBlockState, direction);

            byte b = occlusionCache.getAndMoveToFirst(blockStatePairKey);
            if (b != 127) {
                return b != 0;
            } else {
                boolean bl = Shapes.joinIsNotEmpty(shape, adjShape, BooleanOp.ONLY_FIRST);

                if (occlusionCache.size() == 2048) {
                    occlusionCache.removeLastByte();
                }

                occlusionCache.putAndMoveToFirst(blockStatePairKey, (byte) (bl ? 1 : 0));
                return bl;
            }
        }

        return true;
    }
}

