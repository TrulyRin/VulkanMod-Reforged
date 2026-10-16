package net.vulkanmod.render.chunk.build;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.vulkanmod.render.chunk.WorldRenderer;

import java.util.IdentityHashMap;
import java.util.Map;

public class TintCache {
    private static final int SECTION_WIDTH = 16;

    private final Layer[] layers = new Layer[SECTION_WIDTH];

    private int blendRadius, totalWidth;
    private int secY;
    private int minX, minZ;
    private int maxX, maxZ;

    private int dataSize;
    private long[] temp;

    public TintCache() {
        for (int y = 0; y < layers.length; y++) {
            layers[y] = new Layer();
        }
    }

    public void init(int blendRadius, int secX, int secY, int secZ) {
        this.blendRadius = blendRadius;
        this.totalWidth = (blendRadius * 2) + 16;

        this.secY = secY;

        minX = (secX << 4) - blendRadius;
        minZ = (secZ << 4) - blendRadius;
        maxX = (secX << 4) + 16 + blendRadius;
        maxZ = (secZ << 4) + 16 + blendRadius;

        int size = totalWidth * totalWidth;

        if(size != dataSize) {
            this.dataSize = size;
            for (Layer layer : layers) {
                layer.allocate(size);
            }
            temp = new long[size];
        } else {
            for (Layer layer : layers) {
                layer.invalidate();
            }
        }
    }

    public int getColor(BlockPos blockPos, ColorResolver colorResolver) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        int minY = secY << 4;
        if (x < minX + blendRadius || x >= maxX - blendRadius
                || y < minY || y >= minY + SECTION_WIDTH
                || z < minZ + blendRadius || z >= maxZ - blendRadius) {
            return calculateUncachedColor(blockPos, colorResolver);
        }

        int relY = blockPos.getY() & 15;
        Layer layer = layers[relY];
        if(layer.invalidated)
            calculateLayer(relY);

        ResolverColors colors = layer.colors.get(colorResolver);
        if (colors == null) {
            colors = new ResolverColors(dataSize);
            layer.colors.put(colorResolver, colors);
        }
        if (!colors.valid) {
            calculateColors(layer, colorResolver, colors.values);
            colors.valid = true;
        }
        int relX = blockPos.getX() & 15;
        int relZ = blockPos.getZ() & 15;
        int idx = totalWidth * (relZ + blendRadius) + (relX + blendRadius);
        return colors.values[idx];
    }

    private int calculateUncachedColor(BlockPos blockPos, ColorResolver resolver) {
        Level level = WorldRenderer.getLevel();
        if (blendRadius == 0) {
            return resolver.getColor(level.getBiome(blockPos).value(), blockPos.getX(), blockPos.getZ());
        }

                                                                                                
                                                                                               
        int red = 0, green = 0, blue = 0;
        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
        for (int z = blockPos.getZ() - blendRadius; z <= blockPos.getZ() + blendRadius; z++) {
            for (int x = blockPos.getX() - blendRadius; x <= blockPos.getX() + blendRadius; x++) {
                samplePos.set(x, blockPos.getY(), z);
                int color = resolver.getColor(level.getBiome(samplePos).value(), x, z);
                red += (color >>> 16) & 255;
                green += (color >>> 8) & 255;
                blue += color & 255;
            }
        }
        int diameter = blendRadius * 2 + 1;
        int sampleCount = diameter * diameter;
        return (red / sampleCount << 16) | (green / sampleCount << 8) | blue / sampleCount;
    }

    public void calculateLayer(int y) {
        Level level = WorldRenderer.getLevel();
        Layer layer = layers[y];
        layer.invalidate();

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        int absY = (secY << 4) + y;

        for (int absZ = minZ; absZ < maxZ ; absZ++) {
            for (int absX = minX; absX < maxX ; absX++) {
                blockPos.set(absX, absY, absZ);
                final int idx = (absX - minX) + (absZ - minZ) * totalWidth;
                layer.biomes[idx] = level.getBiome(blockPos).value();
            }
        }

        layer.invalidated = false;
    }

    private void calculateColors(Layer layer, ColorResolver resolver, int[] values) {
        for (int z = 0; z < totalWidth; z++) {
            for (int x = 0; x < totalWidth; x++) {
                int index = x + z * totalWidth;
                values[index] = resolver.getColor(layer.biomes[index], minX + x, minZ + z);
            }
        }

        if (blendRadius > 0) {
            blur(values);
        }
    }

    private void blur(int[] values) {
        int diameter = blendRadius * 2 + 1;
        int sampleCount = diameter * diameter;
        int end = blendRadius + SECTION_WIDTH;

                                                                                                    
        for (int z = 0; z < totalWidth; z++) {
            int row = z * totalWidth;
            int red = 0, green = 0, blue = 0;
            for (int x = 0; x < diameter; x++) {
                int color = values[row + x];
                red += (color >>> 16) & 255;
                green += (color >>> 8) & 255;
                blue += color & 255;
            }
            for (int x = blendRadius; x < end; x++) {
                temp[row + x] = ((long) red << 32) | ((long) green << 16) | blue;
                if (x + 1 < end) {
                    int leaving = values[row + x - blendRadius];
                    int entering = values[row + x + blendRadius + 1];
                    red += ((entering >>> 16) & 255) - ((leaving >>> 16) & 255);
                    green += ((entering >>> 8) & 255) - ((leaving >>> 8) & 255);
                    blue += (entering & 255) - (leaving & 255);
                }
            }
        }

        for (int x = blendRadius; x < end; x++) {
            int red = 0, green = 0, blue = 0;
            for (int z = 0; z < diameter; z++) {
                long sum = temp[z * totalWidth + x];
                red += (int) (sum >>> 32);
                green += (int) ((sum >>> 16) & 65535);
                blue += (int) (sum & 65535);
            }
            for (int z = blendRadius; z < end; z++) {
                values[z * totalWidth + x] = (red / sampleCount << 16) | (green / sampleCount << 8) | blue / sampleCount;
                if (z + 1 < end) {
                    long leaving = temp[(z - blendRadius) * totalWidth + x];
                    long entering = temp[(z + blendRadius + 1) * totalWidth + x];
                    red += (int) (entering >>> 32) - (int) (leaving >>> 32);
                    green += (int) ((entering >>> 16) & 65535) - (int) ((leaving >>> 16) & 65535);
                    blue += (int) (entering & 65535) - (int) (leaving & 65535);
                }
            }
        }
    }

    static class Layer {
        private boolean invalidated = true;
        private Biome[] biomes;
        private final Map<ColorResolver, ResolverColors> colors = new IdentityHashMap<>();

        void allocate(int size) {
            biomes = new Biome[size];
            for (ResolverColors colors : this.colors.values()) {
                colors.values = new int[size];
            }
            invalidate();
        }

        void invalidate() {
            invalidated = true;
            for (ResolverColors colors : this.colors.values()) {
                colors.valid = false;
            }
        }
    }

    private static class ResolverColors {
        private int[] values;
        private boolean valid;

        ResolverColors(int size) {
            values = new int[size];
        }
    }
}
