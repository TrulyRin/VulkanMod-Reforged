package net.vulkanmod.vulkan.shader.parser;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;

import java.util.*;
import java.util.regex.Pattern;

import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_ALL_GRAPHICS;


public final class CanonicalSamplerOrigins {
    private static final Pattern SAMPLER = Pattern.compile(
            "(?:layout\\s*\\([^)]*\\)\\s*)?uniform\\s+(?:(?:lowp|mediump|highp)\\s+)?sampler2D\\s+([A-Za-z_]\\w*)\\s*;");
    private static final Pattern HEADER = Pattern.compile("(?m)^[\\t ]*#[\\t ]*(version|extension)\\b[^\\r\\n]*");

    private CanonicalSamplerOrigins() {}

    public record Program(String vertex, String fragment, Bindings bindings) {}


    public static final class Bindings {
        private final int binding;
        private final List<LegacySamplerOrigins.Binding> fields;

        private Bindings(int binding, List<LegacySamplerOrigins.Binding> fields) {
            this.binding = binding;
            this.fields = List.copyOf(fields);
        }

        public boolean empty() { return this.fields.isEmpty(); }

        public void append(List<UBO> buffers, List<ImageDescriptor> descriptors) {
            if (empty()) return;
            var builder = new AlignedStruct.Builder();
            for (var field : this.fields) builder.addUniformInfo("int", field.uniform());
            UBO buffer = builder.buildUBO(this.binding, VK_SHADER_STAGE_ALL_GRAPHICS);
            new LegacySamplerOrigins.Lowered("", "", this.fields).bind(buffer, descriptors);
            buffers.add(buffer);
        }
    }

    public static Program lower(String name, String vertex, String fragment, List<ImageDescriptor> descriptors,
                                int nextBinding, Collection<String> reservedNames) {
        var declarations = new LinkedHashMap<String, UniformParser.Declaration>();
        for (var descriptor : descriptors) if ("sampler2D".equals(descriptor.qualifier))
            declarations.put(descriptor.name, new UniformParser.Declaration("sampler2D", descriptor.name, List.of()));
        if (declarations.isEmpty()) return new Program(vertex, fragment, new Bindings(nextBinding, List.of()));
        String occupied = vertex + '\n' + fragment + '\n' + String.join(" ", reservedNames);
        Stage v = prepare(name + ".vsh", vertex, SPIRVUtils.ShaderKind.VERTEX_SHADER, declarations.keySet());
        Stage f = prepare(name + ".fsh", fragment, SPIRVUtils.ShaderKind.FRAGMENT_SHADER, declarations.keySet());
        var lowered = LegacySamplerOrigins.lowerPreprocessed(v.body, f.body, declarations, occupied);
        var bindings = new Bindings(nextBinding, lowered.bindings());
        if (bindings.empty()) return new Program(vertex, fragment, bindings);
        String blockName = "VulkanModRawSamplerOrigins";
        int suffix = 0;
        while (Pattern.compile("\\b" + blockName + "\\b").matcher(occupied + lowered.vertex() + lowered.fragment()).find())
            blockName = "VulkanModRawSamplerOrigins" + ++suffix;
        var block = new StringBuilder("layout(binding = ").append(nextBinding).append(") uniform ").append(blockName).append(" {\n");
        for (var binding : lowered.bindings()) block.append("int ").append(binding.uniform()).append(";\n");
        block.append("};\n");
        return new Program(v.header + v.samplers + block + lowered.vertex(),
                f.header + f.samplers + block + lowered.fragment(), bindings);
    }

    private record Stage(String header, String samplers, String body) {}

    private static Stage prepare(String name, String source, SPIRVUtils.ShaderKind kind, Set<String> samplers) {
        String preprocessed = SPIRVUtils.preprocessShaderWithMetadata(name, source, kind).source();
        var header = new StringBuilder("#version 450\n");
        var directives = HEADER.matcher(preprocessed);
        while (directives.find()) if (directives.group(1).equals("extension")) header.append(directives.group()).append('\n');
        String body = directives.replaceAll("");
        var declarations = new StringBuilder();
        var matcher = SAMPLER.matcher(body);
        var stripped = new StringBuilder();
        while (matcher.find()) {
            if (samplers.contains(matcher.group(1))) {
                declarations.append(matcher.group()).append('\n');
                matcher.appendReplacement(stripped, "");
            }
        }
        matcher.appendTail(stripped);
        return new Stage(header.toString(), declarations.toString(), stripped.toString());
    }
}
