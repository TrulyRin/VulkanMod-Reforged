package net.vulkanmod.render.chunk.build.light.data;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.vulkanmod.Initializer;
import net.vulkanmod.interfaces.VoxelShapeExtended;
import net.vulkanmod.render.chunk.build.light.LightMode;
import net.vulkanmod.render.chunk.util.SimpleDirection;

public abstract class LightDataAccess {
    private static final int BL_OFFSET = 0;
    private static final int SL_OFFSET = 4;
    private static final int AO_OFFSET = 8;
    private static final int CO_OFFSET = 20;
    private static final int EM_OFFSET = 28;
    private static final int OP_OFFSET = 29;
    private static final int FO_OFFSET = 30;
    private static final int FC_OFFSET = 31;

    private static final float AO_INV = 1.0f / 2048.0f;

    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    protected BlockAndTintGetter world;

    final boolean subBlockLighting;

    protected LightDataAccess() {
        this.subBlockLighting = Initializer.CONFIG.ambientOcclusion == LightMode.SUB_BLOCK;
    }

    public int get(int x, int y, int z, SimpleDirection d1, SimpleDirection d2) {
        return this.get(x + d1.getStepX() + d2.getStepX(),
                y + d1.getStepY() + d2.getStepY(),
                z + d1.getStepZ() + d2.getStepZ());
    }

    public int get(int x, int y, int z, SimpleDirection dir) {
        return this.get(x + dir.getStepX(),
                y + dir.getStepY(),
                z + dir.getStepZ());
    }

    public int get(BlockPos pos, SimpleDirection dir) {
        return this.get(pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    public int get(BlockPos pos) {
        return this.get(pos.getX(), pos.getY(), pos.getZ());
    }

    public abstract int get(int x, int y, int z);

    protected int compute(int x, int y, int z) {
        BlockPos.MutableBlockPos pos = this.pos.set(x, y, z);
        BlockState state;
        int bl;
        int sl;

        if (this.world instanceof net.vulkanmod.render.chunk.build.RenderRegion region) {
            state = region.getBlockStateRaw(x, y, z);
        } else {
            state = world.getBlockState(pos);
        }

        boolean em = state.emissiveRendering(world, pos);

        boolean op;
        if(this.subBlockLighting)
            op = state.canOcclude();
        else
            op = state.isViewBlocking(world, pos) && state.getLightBlock(world, pos) != 0;

        boolean fo = state.isSolidRender(world, pos);
        boolean fc = state.isCollisionShapeFullBlock(world, pos);

        int lu = state.getLightEmission();

        if (fo && lu == 0) {
            bl = 0;
            sl = 0;
        } else {
            if (this.world instanceof net.vulkanmod.render.chunk.build.RenderRegion region) {
                bl = region.getBrightnessRaw(LightLayer.BLOCK, x, y, z);
                sl = region.getBrightnessRaw(LightLayer.SKY, x, y, z);
            } else {
                bl = world.getBrightness(LightLayer.BLOCK, pos);
                sl = world.getBrightness(LightLayer.SKY, pos);
            }
        }

        float ao;
        if (lu == 0) {
            ao = state.getShadeBrightness(world, pos);
        } else {
            ao = 1.0f;
        }

        boolean useAo = ao < 1.0f;

        bl = Math.max(bl, lu);

        int crs = (fo || fc) && lu == 0 && useAo ? 0xFF : 0;
        if(!fo && op) {
            VoxelShape shape = state.getShape(world, pos);
            crs = ((VoxelShapeExtended)(shape)).getCornerOcclusion();
        }

       return packFC(fc) | packFO(fo) | packOP(op) | packEM(em) | packCO(crs) | packAO(ao) | packSL(sl) | packBL(bl);
    }

    public static int packBL(int blockLight) {
        return (blockLight & 0xF) << BL_OFFSET;
    }

    public static int unpackBL(int word) {
        return (word >>> BL_OFFSET) & 0xF;
    }

    public static int packSL(int skyLight) {
        return (skyLight & 0xF) << SL_OFFSET;
    }

    public static int unpackSL(int word) {
        return (word >>> SL_OFFSET) & 0xF;
    }

    public static int packAO(float ao) {
        int aoi = (int) (ao * 2048.0f);
        return (aoi & 0xFFF) << AO_OFFSET;
    }

    public static float unpackAO(int word) {
        int aoi = (word >>> AO_OFFSET) & 0xFFF;
        return aoi * (AO_INV);
    }

    public static int packCO(int co) {
        return (co & 0xFF) << CO_OFFSET;
    }

    public static int unpackCO(int word) {
        return (word >>> CO_OFFSET) & 0xFF;
    }

    public static int packEM(boolean emissive) {
        return (emissive ? 1 : 0) << EM_OFFSET;
    }

    public static boolean unpackEM(int word) {
        return ((word >>> EM_OFFSET) & 0b1) != 0;
    }

    public static int packOP(boolean opaque) {
        return (opaque ? 1 : 0) << OP_OFFSET;
    }

    public static boolean unpackOP(int word) {
        return ((word >>> OP_OFFSET) & 0b1) != 0;
    }

    public static int packFO(boolean opaque) {
        return (opaque ? 1 : 0) << FO_OFFSET;
    }

    public static boolean unpackFO(int word) {
        return ((word >>> FO_OFFSET) & 0b1) != 0;
    }

    public static int packFC(boolean fullCube) {
        return (fullCube ? 1 : 0) << FC_OFFSET;
    }

    public static boolean unpackFC(int word) {
        return ((word >>> FC_OFFSET) & 0b1) != 0;
    }

    public static int getLightmap(int word) {

        return LightTexture.pack(unpackBL(word), unpackSL(word));
    }

    public static int getEmissiveLightmap(int word) {
        if (unpackEM(word)) {
            return LightTexture.FULL_BRIGHT;
        } else {
            return getLightmap(word);
        }
    }

    public BlockAndTintGetter getWorld() {
        return this.world;
    }

    public void clearWorld() {
        this.world = null;
    }
}
