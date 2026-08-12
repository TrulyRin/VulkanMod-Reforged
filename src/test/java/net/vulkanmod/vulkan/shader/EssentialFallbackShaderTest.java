package net.vulkanmod.vulkan.shader;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EssentialFallbackShaderTest {
    @Test
    void universalCraftPositionColorFallbackCompilesToSpirv() {
        assertDoesNotThrow(() -> SPIRVUtils.compileShaderAbsoluteFile(
                "/assets/vulkanmod/shaders/essential/core/uc_position_color/uc_position_color.vsh",
                SPIRVUtils.ShaderKind.VERTEX_SHADER));
        assertDoesNotThrow(() -> SPIRVUtils.compileShaderAbsoluteFile(
                "/assets/vulkanmod/shaders/essential/core/uc_position_color/uc_position_color.fsh",
                SPIRVUtils.ShaderKind.FRAGMENT_SHADER));
    }
}
