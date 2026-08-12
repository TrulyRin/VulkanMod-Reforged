package net.vulkanmod.compat.opengl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlFunctionRegistryTest {
    @Test
    void framebufferBlitFunctionsAreRoutedToFramebufferEmulation() throws IOException {
        String source = Files.readString(Path.of("src/main/java/net/vulkanmod/compat/opengl/GlFunctionRegistry.java"));

        assertTrue(GlFunctionRegistry.isRegistered("glBlitFramebuffer"));
        assertTrue(GlFunctionRegistry.isRegistered("glBlitFramebufferEXT"));
        assertTrue(GlFunctionRegistry.isRegistered("glBlitNamedFramebuffer"));
        assertFalse(source.contains("noop(\"glBlitFramebuffer\", \"V:iiiiiiiiii\")"));
        assertFalse(source.contains("noop(\"glBlitFramebufferEXT\", \"V:iiiiiiiiii\")"));
        assertFalse(source.contains("noop(\"glBlitNamedFramebuffer\", \"V:iiiiiiiiiiii\")"));
        assertTrue(source.contains("GlFramebuffer.blitFramebuffer"));
        assertTrue(source.contains("GlFramebuffer.blitNamedFramebuffer"));
    }
}
