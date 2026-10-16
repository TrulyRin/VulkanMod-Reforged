package net.vulkanmod.vulkan.shader.parser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SamplerTextureSlot;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.ArrayList;
import java.util.List;

public class UniformParser {

    private final GlslConverter converterInstance;
    private final StageUniforms[] stageUniforms = new StageUniforms[GlslConverter.ShaderStage.values().length];
    private StageUniforms currentUniforms;
    List<Uniform> globalUniforms = new ArrayList<>();

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

            Uniform uniform = new Uniform(this.type, this.name);
            if (isSamplerType(this.type)) {
                if (!this.currentUniforms.samplers.contains(uniform))
                    this.currentUniforms.samplers.add(uniform);
            } else {
                if (!this.globalUniforms.contains(uniform))
                    this.globalUniforms.add(uniform);
            }

            this.resetSate();
            return true;
        }

        return false;
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
            builder.append(String.format("%s %s;\n", uniform.type, uniform.name));
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
            builder.addUniformInfo(uniform.type, uniform.name);
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

    public record Uniform(String type, String name) {
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

