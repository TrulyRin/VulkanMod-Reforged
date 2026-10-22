package net.vulkanmod.render.chunk.build.light.smooth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.vulkanmod.render.chunk.util.SimpleDirection;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.chunk.build.light.data.LightDataAccess;
import net.vulkanmod.render.chunk.build.light.LightPipeline;
import net.vulkanmod.render.chunk.build.light.data.QuadLightData;
import net.vulkanmod.render.model.quad.ModelQuadFlags;




public class NewSmoothLightPipeline implements LightPipeline {
    private final LightDataAccess lightCache;




    private final SubBlockAoFace[] cachedFaceData = new SubBlockAoFace[6 * 2];




    private final SubBlockAoFace self = new SubBlockAoFace();

    private long cachedPos = Long.MIN_VALUE;




    private final float[] weights = new float[4];

    public NewSmoothLightPipeline(LightDataAccess cache) {
        this.lightCache = cache;

        for (int i = 0; i < this.cachedFaceData.length; i++) {
            this.cachedFaceData[i] = new SubBlockAoFace();
        }
    }

    @Override
    public void calculate(QuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFaceO, boolean shade) {
        this.updateCachedData(pos.asLong());

        int flags = quad.getFlags();

        SimpleDirection lightFace = SimpleDirection.of(lightFaceO);

        final AoNeighborInfo neighborInfo = AoNeighborInfo.get(lightFace);





        if ((flags & ModelQuadFlags.IS_ALIGNED) != 0 || ((flags & ModelQuadFlags.IS_PARALLEL) != 0 && LightDataAccess.unpackFC(this.lightCache.get(pos)))) {
            if ((flags & ModelQuadFlags.IS_PARTIAL) == 0) {
                this.applyAlignedFullFace(neighborInfo, pos, lightFace, out);
            } else {
                this.applyAlignedPartialFace(neighborInfo, quad, pos, lightFace, out);
            }
        } else if ((flags & ModelQuadFlags.IS_PARALLEL) != 0) {
            this.applyParallelFace(neighborInfo, quad, pos, lightFace, out);
        } else {
            this.applyNonParallelFace(neighborInfo, quad, pos, lightFace, out);
        }

        this.applySidedBrightness(out, lightFaceO, shade);
    }







    private void applyAlignedFullFace(AoNeighborInfo neighborInfo, BlockPos pos, SimpleDirection dir, QuadLightData out) {
        SubBlockAoFace faceData = this.getCachedFaceData(pos, dir, true);
        neighborInfo.copyLightValues(faceData.lm, faceData.ao, out.lm, out.br);
    }





    private void applyAlignedPartialFace(AoNeighborInfo neighborInfo, QuadView quad, BlockPos pos, SimpleDirection dir, QuadLightData out) {




        for (int i = 0; i < 4; i++) {

            float cx = clamp(quad.getX(i));
            float cy = clamp(quad.getY(i));
            float cz = clamp(quad.getZ(i));

            float[] weights = this.weights;
            neighborInfo.calculateCornerWeights(cx, cy, cz, weights);
            this.applyAlignedPartialFaceVertex(pos, dir, weights, i, out, true);
        }

    }








    private void applyParallelFace(AoNeighborInfo neighborInfo, QuadView quad, BlockPos pos, SimpleDirection dir, QuadLightData out) {
        this.self.calculateSelfOcclusion(this.lightCache, pos, dir);

        for (int i = 0; i < 4; i++) {

            float cx = clamp(quad.getX(i));
            float cy = clamp(quad.getY(i));
            float cz = clamp(quad.getZ(i));

            float[] weights = this.weights;
            neighborInfo.calculateCornerWeights(cx, cy, cz, weights);

            float depth = neighborInfo.getDepth(cx, cy, cz);



            if (Mth.equal(depth, 1.0F)) {
                this.applyAlignedPartialFaceVertex(pos, dir, weights, i, out, false);
            } else {


                this.applyInsetPartialFaceVertexSO(pos, dir, depth, 1.0f - depth, weights, i, out);
            }
        }
    }




    private void applyNonParallelFace(AoNeighborInfo neighborInfo, QuadView quad, BlockPos pos, SimpleDirection dir, QuadLightData out) {
        for (int i = 0; i < 4; i++) {

            float cx = clamp(quad.getX(i));
            float cy = clamp(quad.getY(i));
            float cz = clamp(quad.getZ(i));

            float[] weights = this.weights;
            neighborInfo.calculateCornerWeights(cx, cy, cz, weights);

            float depth = neighborInfo.getDepth(cx, cy, cz);


            if (Mth.equal(depth, 0.0F)) {
                this.applyAlignedPartialFaceVertex(pos, dir, weights, i, out, true);
            } else if (Mth.equal(depth, 1.0F)) {
                this.applyAlignedPartialFaceVertex(pos, dir, weights, i, out, false);
            } else {


                this.applyInsetPartialFaceVertex(pos, dir, depth, 1.0f - depth, weights, i, out);
            }
        }
    }

    private void applyAlignedPartialFaceVertex(BlockPos pos, SimpleDirection dir, float[] w, int i, QuadLightData out, boolean offset) {
        SubBlockAoFace faceData = this.getCachedFaceData(pos, dir, offset);

        if (!faceData.hasUnpackedLightData()) {
            faceData.unpackLightData();
        }

        float sl = faceData.getBlendedSkyLight(w);
        float bl = faceData.getBlendedBlockLight(w);
        float ao = faceData.getBlendedShade(w);

        out.br[i] = ao;
        out.lm[i] = packLightMap(sl, bl);
    }

    private void applyInsetPartialFaceVertex(BlockPos pos, SimpleDirection dir, float n1d, float n2d, float[] w, int i, QuadLightData out) {
        SubBlockAoFace n1 = this.getCachedFaceData(pos, dir, false);

        if (!n1.hasUnpackedLightData()) {
            n1.unpackLightData();
        }

        SubBlockAoFace n2 = this.getCachedFaceData(pos, dir, true);

        if (!n2.hasUnpackedLightData()) {
            n2.unpackLightData();
        }


        float ao = (n1.getBlendedShade(w) * n1d) + (n2.getBlendedShade(w) * n2d);
        float sl = (n1.getBlendedSkyLight(w) * n1d) + (n2.getBlendedSkyLight(w) * n2d);
        float bl = (n1.getBlendedBlockLight(w) * n1d) + (n2.getBlendedBlockLight(w) * n2d);

        out.br[i] = ao;
        out.lm[i] = packLightMap(sl, bl);
    }




    private void applyInsetPartialFaceVertexSO(BlockPos pos, SimpleDirection dir, float n1d, float n2d, float[] w, int i, QuadLightData out) {
        SubBlockAoFace n1 = this.getCachedFaceData(pos, dir, false);

        if (!n1.hasUnpackedLightData()) {
            n1.unpackLightData();
        }

        SubBlockAoFace n2 = this.getCachedFaceData(pos, dir, true);

        if (!n2.hasUnpackedLightData()) {
            n2.unpackLightData();
        }


        float ao = (n1.getBlendedShade(w) * n1d) + (n2.getBlendedShade(w) * n2d);
        float sl = (n1.getBlendedSkyLight(w) * n1d) + (n2.getBlendedSkyLight(w) * n2d);
        float bl = (n1.getBlendedBlockLight(w) * n1d) + (n2.getBlendedBlockLight(w) * n2d);

        ao = Math.min(ao, (this.self.getBlendedShade(w)));

        out.br[i] = ao;
        out.lm[i] = packLightMap(sl, bl);
    }

    private void applySidedBrightness(QuadLightData out, Direction face, boolean shade) {
        float brightness = this.lightCache.getWorld().getShade(face, shade);
        float[] br = out.br;

        for (int i = 0; i < br.length; i++) {
            br[i] *= brightness;
        }
    }




    private SubBlockAoFace getCachedFaceData(BlockPos pos, SimpleDirection face, boolean offset) {
        SubBlockAoFace data = this.cachedFaceData[offset ? face.ordinal() : face.ordinal() + 6];

        if (!data.hasLightData()) {
            data.initLightData(this.lightCache, pos, face, offset);
        }

        return data;
    }

    private void updateCachedData(long key) {
        if (this.cachedPos != key) {
            for (SubBlockAoFace data : this.cachedFaceData) {
                data.reset();
            }

            this.cachedPos = key;
        }
    }




    private static float clamp(float v) {
        if (v < 0.0f) {
            return 0.0f;
        } else if (v > 1.0f) {
            return 1.0f;
        }

        return v;
    }




    private static int packLightMap(float sl, float bl) {
        return (((int) sl & 0xFF) << 16) | ((int) bl & 0xFF);
    }

}
