package net.vulkanmod.render.chunk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainUboBindingTest {

    @Test
    void terrainLayerBindsUbosOncePerLayerNotPerChunkArea() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/net/vulkanmod/render/chunk/WorldRenderer.java"));

        assertTrue(source.contains("boolean ubosBound = false;"));
        assertTrue(source.contains("if (!ubosBound) {"));
        assertTrue(source.contains("ubosBound = true;"));

        Matcher matcher = Pattern.compile(Pattern.quote("renderer.uploadAndBindUBOs(pipeline);")).matcher(source);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertEquals(1, count, "uploadAndBindUBOs must appear exactly once in the terrain layer loop");
    }
}
