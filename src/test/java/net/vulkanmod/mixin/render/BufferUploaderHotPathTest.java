package net.vulkanmod.mixin.render;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BufferUploaderHotPathTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/net/vulkanmod/mixin/render/BufferUploaderM.java");

    @Test
    void ordinaryDrawPathHasNoTemporaryLoggingOrComparisonListAllocation() throws Exception {
        String source = Files.readString(SOURCE);

        assertFalse(source.contains("[ESS-DBG]"));
        assertFalse(source.contains("List.of(\"uc_Position\""));
        assertTrue(source.contains("attributes.size()"));
        assertTrue(source.contains("attributes.get(0)"));
    }

    @Test
    void ordinaryGuiShadersShortCircuitBeforeUniversalCraftClassification() throws Exception {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains(
                "isFlatGuiShader(shaderName) || isUniversalCraftGuiFormat(parameters.format())"));
    }
}
