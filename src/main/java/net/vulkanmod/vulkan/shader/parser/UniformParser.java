package net.vulkanmod.vulkan.shader.parser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SamplerTextureSlot;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class UniformParser {

    private final GlslConverter converterInstance;
    private final StageUniforms[] stageUniforms = new StageUniforms[GlslConverter.ShaderStage.values().length];
    private StageUniforms currentUniforms;
    List<Uniform> globalUniforms = new ArrayList<>();
    private final Map<String, Declaration> declarations = new LinkedHashMap<>();

    private String type;
    private String name;

    private UBO ubo;
    private List<ImageDescriptor> imageDescriptors;

    public UniformParser(GlslConverter converterInstance) {
        this.converterInstance = converterInstance;

        for (int i = 0; i < this.stageUniforms.length; ++i) {
            this.stageUniforms[i] = new StageUniforms();
        }
    }

    public boolean parseToken(String token) {
        if (token.matches("uniform"))
            return false;

        if (this.type == null) {
            this.type = token;

        } else if (this.name == null) {
            token = removeSemicolon(token);

            this.name = token;

            parseDeclaration(this.type, this.name, null);

            this.resetSate();
            return true;
        }

        return false;
    }

    public void parseDeclaration(String type, String name, String initializer) {
        parseDeclaration(type, name, initializer, 0);
    }

    public void parseDeclaration(String type, String name, String initializer, int arrayLength) {
        if (arrayLength < 0 || arrayLength > LegacyArrayExtent.MAX_EXTENT) {
            throw new IllegalArgumentException("Invalid legacy uniform array extent for " + name + ": " + arrayLength);
        }
        if (arrayLength > 0 && !"float".equals(type)) {
            throw new IllegalArgumentException("Unsupported legacy uniform array type: " + type + " " + name);
        }
        if (arrayLength > 0 && initializer != null) {
            throw new IllegalArgumentException("Unsupported legacy uniform array initializer for " + name);
        }
        Declaration previous = this.declarations.get(name);
        if (previous != null && !previous.type().equals(type)) {
            throw new IllegalArgumentException("Conflicting legacy uniform types for " + name);
        }
        if (previous != null && previous.arrayLength() != arrayLength) {
            throw new IllegalArgumentException("Conflicting legacy uniform array extents for " + name);
        }
        List<String> initializers = new ArrayList<>(previous == null ? List.of() : previous.initializers());
        if (initializer != null) initializers.add(initializer);
        this.declarations.put(name, new Declaration(type, name, List.copyOf(initializers), arrayLength));

        Uniform uniform = new Uniform(type, name, arrayLength);
        if (isSamplerType(type)) {
            if (!this.currentUniforms.samplers.contains(uniform)) this.currentUniforms.samplers.add(uniform);
        } else if (!this.globalUniforms.contains(uniform)) {
            this.globalUniforms.add(uniform);
        }
    }

    public Map<String, Declaration> getDeclarations() {
        return Map.copyOf(this.declarations);
    }

    public void setCurrentUniforms(GlslConverter.ShaderStage shaderStage) {
        this.currentUniforms = stageUniforms[shaderStage.ordinal()];
    }

    private void resetSate() {
        this.type = null;
        this.name = null;

    }

    public String createUniformsCode() {
        this.ubo = this.createUBO();

        if (this.globalUniforms.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("layout(binding = %d) uniform UniformBufferObject {\n", 0));
        for (Uniform uniform : this.globalUniforms) {
            builder.append(uniform.type).append(' ').append(uniform.name);
            if (uniform.arrayLength > 0) builder.append('[').append(uniform.arrayLength).append(']');
            builder.append(";\n");
        }
        builder.append("};\n\n");

        return builder.toString();
    }

    public String createSamplersCode(GlslConverter.ShaderStage shaderStage) {
        StringBuilder builder = new StringBuilder();

        this.imageDescriptors = createSamplerList();

        for (ImageDescriptor imageDescriptor : this.imageDescriptors) {
            builder.append(String.format("layout(binding = %d) uniform %s %s;\n", imageDescriptor.getBinding(),
                    imageDescriptor.qualifier, imageDescriptor.name));
        }
        builder.append("\n");

        return builder.toString();
    }

    private UBO createUBO() {
        AlignedStruct.Builder builder = new AlignedStruct.Builder();

        for (Uniform uniform : this.globalUniforms) {
            if (uniform.arrayLength > 0) builder.addUniformArrayInfo(uniform.type, uniform.name, uniform.arrayLength);
            else builder.addUniformInfo(uniform.type, uniform.name);
        }

        return builder.buildUBO(0, Pipeline.Builder.getStageFromString("all"));
    }

    private List<ImageDescriptor> createSamplerList() {
        int currentLocation = 1;

        List<ImageDescriptor> imageDescriptors = new ObjectArrayList<>();

        for (StageUniforms stageUniforms : this.stageUniforms) {
            for (Uniform uniform : stageUniforms.samplers) {
                int imageIdx = SamplerTextureSlot.getTextureIdxOrDefault(uniform.name, currentLocation - 1);
                imageDescriptors.add(new ImageDescriptor(currentLocation, uniform.type, uniform.name, imageIdx));
                currentLocation++;
            }
        }

        return imageDescriptors;
    }

    public static String removeSemicolon(String s) {
        int last = s.length() - 1;
        if (last >= 0 && s.charAt(last) == ';') {
            return s.substring(0, last);
        }
        return s;
    }

    private static boolean isSamplerType(String type) {
        return type != null && type.startsWith("sampler");
    }

    public UBO getUbo() {
        return this.ubo;
    }

    public List<ImageDescriptor> getSamplers() {
        return this.imageDescriptors;
    }

    public record Uniform(String type, String name, int arrayLength) {
        public Uniform(String type, String name) {
            this(type, name, 0);
        }
    }

    public record Declaration(String type, String name, List<String> initializers, int arrayLength) {
        public Declaration(String type, String name, List<String> initializers) {
            this(type, name, initializers, 0);
        }
    }

    private static class StageUniforms {
        List<Uniform> samplers = new ArrayList<>();
    }

    enum State {
        Uniform,
        Sampler,
        None
    }
}
