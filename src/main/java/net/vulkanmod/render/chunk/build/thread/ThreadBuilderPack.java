package net.vulkanmod.render.chunk.build.thread;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class ThreadBuilderPack {
    private static Function<TerrainRenderType, TerrainBufferBuilder> terrainBuilderConstructor;

    public static void defaultTerrainBuilderConstructor() {
        terrainBuilderConstructor = renderType -> new TerrainBufferBuilder(TerrainRenderType.getRenderType(renderType).bufferSize());
    }

    public static void setTerrainBuilderConstructor(Function<TerrainRenderType, TerrainBufferBuilder> constructor) {
        terrainBuilderConstructor = constructor;
    }

    private final Map<TerrainRenderType, TerrainBufferBuilder> builders;
    private TerrainBufferBuilder blockLayerBuilder;
    private boolean closed;

    public ThreadBuilderPack() {
        var map = new EnumMap<TerrainRenderType, TerrainBufferBuilder>(TerrainRenderType.class);
        Arrays.stream(TerrainRenderType.values()).forEach(
                terrainRenderType -> map.put(terrainRenderType,
                        terrainBuilderConstructor.apply(terrainRenderType))
        );
        builders = map;
    }

    public TerrainBufferBuilder builder(TerrainRenderType renderType) {
        return this.builders.get(renderType);
    }

                                                                                            
    public TerrainBufferBuilder blockLayerBuilder() {
        if (this.closed) {
            throw new IllegalStateException("Builder pack has been closed");
        }
        if (this.blockLayerBuilder == null) {
            this.blockLayerBuilder = new TerrainBufferBuilder(4096, DefaultVertexFormat.BLOCK);
        }
        return this.blockLayerBuilder;
    }

    public void clearAll() {
        Throwable failure = null;
        for (TerrainBufferBuilder builder : this.builders.values()) {
            try {
                builder.clear();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = collectFailure(failure, cleanupFailure);
            }
        }
        if (this.blockLayerBuilder != null) {
            try {
                this.blockLayerBuilder.clear();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = collectFailure(failure, cleanupFailure);
            }
        }
        rethrowFailure(failure);
    }

    public void closeAll() {
        if (this.closed) return;
        this.closed = true;
        TerrainBufferBuilder blockLayerBuilder = this.blockLayerBuilder;
        this.blockLayerBuilder = null;
        Throwable failure = null;
        for (TerrainBufferBuilder builder : this.builders.values()) {
            try {
                builder.close();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = collectFailure(failure, cleanupFailure);
            }
        }
        this.builders.clear();
        if (blockLayerBuilder != null) {
            try {
                blockLayerBuilder.close();
            } catch (RuntimeException | Error cleanupFailure) {
                failure = collectFailure(failure, cleanupFailure);
            }
        }
        rethrowFailure(failure);
    }

    private static Throwable collectFailure(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void rethrowFailure(Throwable failure) {
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }
}
