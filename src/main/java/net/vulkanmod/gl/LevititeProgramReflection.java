package net.vulkanmod.gl;

import net.vulkanmod.vulkan.shader.parser.LevititeShaderConverter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;






public final class LevititeProgramReflection {

    public record Variable(String name, int size, int type, int components, boolean integer, int location) {}

    private final LevititeShaderConverter.Result conversion;
    private final List<Variable> uniforms;
    private final List<Variable> attributes;
    private final int uniformMaxNameLength;
    private final int attributeMaxNameLength;

    private LevititeProgramReflection(LevititeShaderConverter.Result conversion) {
        this.conversion = conversion;
        List<Variable> uniforms = new ArrayList<>();
        for (LevititeShaderConverter.UniformField field : conversion.uniforms()) {
            boolean array = field.arrayLength() != 0;
            uniforms.add(variable(field.name() + (array ? "[0]" : ""),
                    array ? field.arrayLength() : 1, field.type(), -1));
        }
        for (LevititeShaderConverter.Sampler sampler : conversion.samplers()) {
            uniforms.add(variable(sampler.name(), 1, sampler.type(), -1));
        }
        this.uniforms = List.copyOf(uniforms);
        this.attributes = conversion.attributes().stream()
                .map(attribute -> variable(attribute.name(), 1, attribute.type(), attribute.location())).toList();
        this.uniformMaxNameLength = maximumNameLength(this.uniforms);
        this.attributeMaxNameLength = maximumNameLength(this.attributes);
    }


    public static LevititeProgramReflection tryCreate(GlProgram.LinkedProgram linked) {
        final LevititeShaderConverter.Result conversion;
        try {
            conversion = LevititeShaderConverter.convert(linked);
        } catch (IllegalArgumentException unsupported) {
            return null;
        }
        return new LevititeProgramReflection(conversion);
    }

    public LevititeShaderConverter.Result conversion() { return conversion; }
    public List<Variable> uniforms() { return uniforms; }
    public List<Variable> attributes() { return attributes; }
    public int uniformMaxNameLength() { return uniformMaxNameLength; }
    public int attributeMaxNameLength() { return attributeMaxNameLength; }

    private static Variable variable(String name, int size, String type, int location) {
        return switch (type) {
            case "float" -> new Variable(name, size, 0x1406, 1, false, location);
            case "int" -> new Variable(name, size, 0x1404, 1, true, location);
            case "vec2" -> new Variable(name, size, 0x8B50, 2, false, location);
            case "vec3" -> new Variable(name, size, 0x8B51, 3, false, location);
            case "vec4" -> new Variable(name, size, 0x8B52, 4, false, location);
            case "ivec2" -> new Variable(name, size, 0x8B53, 2, true, location);
            case "mat3" -> new Variable(name, size, 0x8B5B, 9, false, location);
            case "mat4" -> new Variable(name, size, 0x8B5C, 16, false, location);
            case "sampler2D" -> new Variable(name, size, 0x8B5E, 1, true, location);
            default -> throw new IllegalStateException("Accepted converter metadata has an unsupported GL type: " + type);
        };
    }

    private static int maximumNameLength(List<Variable> variables) {
        return variables.stream().mapToInt(variable -> variable.name().getBytes(StandardCharsets.UTF_8).length + 1)
                .max().orElse(0);
    }
}
