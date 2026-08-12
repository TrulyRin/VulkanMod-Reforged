package net.vulkanmod.render.culling;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityCullingHotPathTest {
    @Test
    void distanceBoundariesPreserveMobAndMiscRanges() {
        assertTrue(EntityCulling.isWithinAggressiveDistanceSquared(96.0D * 96.0D, false));
        assertFalse(EntityCulling.isWithinAggressiveDistanceSquared(Math.nextUp(96.0D * 96.0D), false));
        assertTrue(EntityCulling.isWithinAggressiveDistanceSquared(128.0D * 128.0D, true));
        assertFalse(EntityCulling.isWithinAggressiveDistanceSquared(Math.nextUp(128.0D * 128.0D), true));
    }

    @Test
    void cheapDistanceDecisionPrecedesUncommonEntityChecks() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/render/culling/EntityCulling.java"));
        int method = source.indexOf("private static boolean isWithinAggressiveDistance(");
        int distance = source.indexOf("isWithinAggressiveDistanceSquared(distanceSquared", method);
        int category = source.indexOf("entity.getType().getCategory()", method);
        int important = source.indexOf("isImportantEntity(entity)", method);
        int minecraft = source.indexOf("Minecraft.getInstance()", method);

        assertTrue(method >= 0);
        assertTrue(distance > method);
        assertTrue(category > distance);
        assertTrue(important > distance);
        assertTrue(minecraft > distance);
    }
}
