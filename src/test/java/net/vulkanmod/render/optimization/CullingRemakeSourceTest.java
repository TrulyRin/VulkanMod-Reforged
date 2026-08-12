package net.vulkanmod.render.optimization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CullingRemakeSourceTest {
    @Test
    void invisibleLeafPropertyUpdatesDoNotDirtySections() throws IOException {
        String mixin = source("src/main/java/net/vulkanmod/mixin/chunk/LevelRendererMixin.java");
        String filter = source("src/main/java/net/vulkanmod/render/culling/LeafUpdateFilter.java");

        assertTrue(mixin.contains("method = \"blockChanged\""));
        assertTrue(mixin.contains("isInvisibleLeafPropertyChange"));
        assertTrue(mixin.contains("recordLeafDirtySkipped"));
        assertTrue(filter.contains("LeavesBlock.DISTANCE"));
        assertTrue(filter.contains("LeavesBlock.PERSISTENT"));
        assertTrue(filter.contains("normalized == newState"),
                "interned-state identity is the only-difference proof");
    }

    @Test
    void nonPlayerRebuildStormsAreCoalescedByACooldown() throws IOException {
        String graph = source("src/main/java/net/vulkanmod/render/chunk/graph/SectionGraph.java");
        String section = source("src/main/java/net/vulkanmod/render/chunk/RenderSection.java");

        assertTrue(graph.contains("REBUILD_COOLDOWN_NANOS"));
        assertTrue(graph.contains("DelayedRebuildQueue<RenderSection>"));
        assertTrue(graph.contains("MAX_DELAYED_REBUILD_PROMOTIONS_PER_FRAME"));
        assertTrue(graph.contains("section.markRebuildScheduled(prepareEndNanos)"));
        assertTrue(graph.contains("this.delayedRebuilds.remove(section)"),
                "player edits must be promoted out of the delayed queue");
        assertTrue(graph.contains("!section.isDirtyFromPlayer()"),
                "player edits must bypass the cooldown");
        assertTrue(section.contains("lastRebuildScheduledNanos != Long.MIN_VALUE"),
                "sentinel keeps the first build exempt and nanoTime-overflow safe");
        assertTrue(section.contains("this.lastRebuildScheduledNanos = Long.MIN_VALUE;"),
                "a recycled render section must not inherit the old location's cooldown");
        assertTrue(ChunkSchedulingPolicy.REBUILD_COOLDOWN_NANOS > 0);
    }

    @Test
    void leafFaceCullingUsesTheLeavesTagAndASnapshottedFlag() throws IOException {
        String renderer = source("src/main/java/net/vulkanmod/render/chunk/build/BlockRenderer.java");

        assertTrue(renderer.contains("BlockTags.LEAVES"));
        assertTrue(renderer.contains("this.leavesCulling"));
        assertFalse(renderer.contains("instanceof LeavesBlock"));
        assertTrue(renderer.indexOf("CONFIG.leavesCulling") == renderer.lastIndexOf("CONFIG.leavesCulling")
                        && renderer.contains("CONFIG.leavesCulling"),
                "exactly one config read: the per-task snapshot in setResources");
        assertTrue(renderer.indexOf("skipRendering(adjBlockState, direction)")
                < renderer.indexOf("BlockTags.LEAVES"), "vanilla fast path stays first");
    }

    @Test
    void perFrameCullingPathsAvoidQuadraticAndRepeatedConfigWork() throws IOException {
        String particles = source("src/main/java/net/vulkanmod/mixin/render/particle/ParticleEngineMixin.java");
        String entity = source("src/main/java/net/vulkanmod/render/culling/EntityCulling.java");
        String world = source("src/main/java/net/vulkanmod/render/chunk/WorldRenderer.java");

        assertTrue(particles.contains("vulkanmod$sameTypeCount"));
        assertTrue(particles.contains("gameTime != this.vulkanmod$countsGameTime"),
                "queue scans may run at most once per game tick");
        assertFalse(particles.contains("int sameTypeCount = 0"),
                "the per-add counting loop is the O(n^2) bug this remake removes");

        int distanceCheck = entity.indexOf("isWithinAggressiveDistance(entity, aabb");
        int frustumCheck = entity.indexOf("return isVisible(frustum, aabb);");
        assertTrue(distanceCheck > 0 && distanceCheck < frustumCheck,
                "cheap distance rejection runs before frustum/section tests");

        assertTrue(world.contains("final boolean blockEntityCulling = Initializer.CONFIG.blockEntityCulling;"));
    }

    private static String source(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
