package net.vulkanmod.vulkan.shader.parser;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlslConverterTest {
    @Test
    void rewritesUniversalCraftFragmentOutputToVulkanOutput() {
        GlslConverter converter = convertFragmentOutput("uc_FragColor");

        String fragmentShader = converter.getFshConverted();

        assertTrue(fragmentShader.contains("layout(location = 0) out vec4 fragColor;"));
        assertTrue(fragmentShader.contains("fragColor = vec4"));
        assertFalse(fragmentShader.contains("uc_FragColor"));
    }

    @Test
    void rewritesLegacyFragmentOutputToVulkanOutput() {
        GlslConverter converter = convertFragmentOutput("gl_FragColor");

        String fragmentShader = converter.getFshConverted();

        assertTrue(fragmentShader.contains("layout(location = 0) out vec4 fragColor;"));
        assertTrue(fragmentShader.contains("fragColor = vec4"));
        assertFalse(fragmentShader.contains("gl_FragColor"));
    }

    @Test
    void convertedUniversalCraftFragmentOutputCompilesToSpirv() {
        GlslConverter converter = convertFragmentOutput("uc_FragColor");

        assertDoesNotThrow(() -> SPIRVUtils.compileShader(
                "universalcraft-output.fsh",
                converter.getFshConverted(),
                SPIRVUtils.ShaderKind.FRAGMENT_SHADER));
    }

    @Test
    void convertsEssentialFixedFunctionShapeShaderToSpirv() {
        GlslConverter converter = new GlslConverter();

        converter.process("""
                #version 110

                varying vec2 f_Position;

                void main() {
                    f_Position = gl_Vertex.xy;

                    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
                    gl_FrontColor = gl_Color;
                }
                """, """
                #version 110

                uniform float u_Radius;
                uniform vec4 u_InnerRect;

                varying vec2 f_Position;

                void main() {
                    vec2 tl = u_InnerRect.xy - f_Position;
                    vec2 br = f_Position - u_InnerRect.zw;
                    vec2 dis = max(br, tl);

                    float v = length(max(vec2(0.0), dis)) - u_Radius;
                    float a = 1.0 - smoothstep(0.0, 1.0, v);
                    gl_FragColor = gl_Color * vec4(1.0, 1.0, 1.0, a);
                }
                """);

        assertCompiles("essential-shape", converter);
    }

    @Test
    void convertsEssentialFontShaderToSpirv() {
        GlslConverter converter = new GlslConverter();

        converter.process("""
                #version 110

                varying vec2 pos;

                void main() {
                    pos = gl_MultiTexCoord0.st;

                    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
                }
                """, """
                #version 110

                varying vec2 pos;

                uniform sampler2D msdf;
                uniform vec4 fgColor;

                float median(float r, float g, float b) {
                    return max(min(r, g), min(max(r, g), b));
                }

                void main() {
                    vec3 raw = texture2D(msdf, pos).rgb;
                    float alpha = median(raw.r, raw.g, raw.b);
                    if (alpha < 20.0 / 256.0) discard;
                    gl_FragColor = vec4(fgColor.rgb, alpha);
                }
                """);

        assertCompiles("essential-font", converter);
    }

    private static void assertCompiles(String name, GlslConverter converter) {
        assertDoesNotThrow(() -> SPIRVUtils.compileShader(
                name + ".vsh",
                converter.getVshConverted(),
                SPIRVUtils.ShaderKind.VERTEX_SHADER));
        assertDoesNotThrow(() -> SPIRVUtils.compileShader(
                name + ".fsh",
                converter.getFshConverted(),
                SPIRVUtils.ShaderKind.FRAGMENT_SHADER));
    }

    private static GlslConverter convertFragmentOutput(String outputName) {
        GlslConverter converter = new GlslConverter();

        converter.process("""
                in vec3 Position;

                void main() {
                    gl_Position = vec4(Position, 1.0);
                }
                """, """
                out vec4 %s;

                void main() {
                    %s = vec4(1.0);
                }
                """.formatted(outputName, outputName));
        return converter;
    }
}
