package net.vulkanmod.compat.sable.veil;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlSampler;
import net.vulkanmod.vulkan.shader.descriptor.ProgramImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.shader.parser.LevititeShaderConverter;
import net.vulkanmod.vulkan.shader.layout.AlignedStruct;
import net.vulkanmod.vulkan.util.MappedBuffer;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import static org.lwjgl.vulkan.VK10.*;

                                                                                                 
record VeilProgramInputs(int owner, long generation, VertexFormat format,
                         LevititeShaderConverter.Result conversion, UBO uniforms,
                         List<ProgramImageDescriptor> samplers) {
    VeilProgramInputs { samplers = List.copyOf(samplers); }

    @FunctionalInterface interface SamplerImages {
        VulkanImage resolve(int unit, int samplingStages);
    }

    static VeilProgramInputs capture(int owner, VertexFormat actualFormat) {
        return capture(owner, actualFormat, VTextureSelector::getImage);
    }

    static VeilProgramInputs capture(int owner, VertexFormat actualFormat, IntFunction<VulkanImage> images) {
        return capture(owner, actualFormat, (unit, stages) -> images.apply(unit));
    }

    static VeilProgramInputs capture(int owner, VertexFormat actualFormat, SamplerImages images) {
        GlProgram.LinkedProgram linked = GlProgram.linkedProgram(owner);
        LevititeShaderConverter.Result conversion = GlProgram.convertedProgram(owner);
        if (owner <= 0 || linked == null || conversion == null || actualFormat == null) {
            throw new IllegalArgumentException("A converted linked owner and actual uploaded format are required");
        }
        validateFormat(actualFormat, conversion.attributes());
        AlignedStruct.Builder builder = new AlignedStruct.Builder();
        Map<String, MappedBuffer> views = new HashMap<>();
        for (var field : conversion.uniforms()) {
            MappedBuffer source = GlProgram.uniformBuffer(owner, field.name());
            int bytes = field.arrayLength() > 0 ? Math.multiplyExact(field.arrayLength(), 4) : packedBytes(field.type());
            if (source == null || source.buffer == null || !source.buffer.isDirect()
                    || source.ptr == 0 || source.buffer.capacity() < bytes) {
                throw new IllegalArgumentException("Missing packed owner uniform: " + field.name());
            }
            views.put(field.name(), source);
            if (field.arrayLength() > 0) builder.addUniformArrayInfo(field.type(), field.name(), field.arrayLength());
            else builder.addUniformInfo(field.type(), field.name());
        }
        UBO ubo = builder.buildUBO(0, VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT
                | VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT | VK_SHADER_STAGE_FRAGMENT_BIT);
        for (var uniform : ubo.getUniforms()) {
            MappedBuffer captured = views.get(uniform.getName());
            uniform.setSupplier(() -> captured);
        }
        List<ProgramImageDescriptor> samplers = new ArrayList<>();
        for (var sampler : conversion.samplers()) {
            int mask = 0;
            for (var stage : sampler.stages()) {
                mask |= switch (stage) {
                    case VERTEX -> VK_SHADER_STAGE_VERTEX_BIT;
                    case TESSELLATION_CONTROL -> VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
                    case TESSELLATION_EVALUATION -> VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
                    case FRAGMENT -> VK_SHADER_STAGE_FRAGMENT_BIT;
                };
            }
            int samplingStages = mask;
            samplers.add(new ProgramImageDescriptor(sampler.binding(), sampler.type(), sampler.name(), owner,
                    linked.generation(), GlProgram.uniformBuffer(owner, sampler.name()), id -> {
                        var current = GlProgram.linkedProgram(id);
                        return current == null ? 0 : current.generation();
                    }, unit -> images.resolve(unit, samplingStages), GlSampler::resolveSampler));
        }
        if (GlProgram.linkedProgram(owner) != linked) {
            throw new IllegalStateException("Program relinked while capturing its uniform views");
        }
        return new VeilProgramInputs(owner, linked.generation(), actualFormat, conversion, ubo, samplers);
    }

    private static int packedBytes(String type) {
        return switch (type) {
            case "float", "int" -> 4;
            case "vec2" -> 8;
            case "vec3" -> 12;
            case "vec4" -> 16;
            case "mat3" -> 36;
            case "mat4" -> 64;
            default -> throw new IllegalArgumentException("Unsupported packed uniform type: " + type);
        };
    }

    private static void validateFormat(VertexFormat format, List<LevititeShaderConverter.Attribute> attributes) {
        List<VertexFormatElement> elements = format.getElements();
        List<String> names = format.getElementAttributeNames();
        for (var attribute : attributes) {
            int location = attribute.location();
            if (location < 0 || location >= elements.size() || location >= names.size()
                    || !attribute.name().equals(names.get(location))) {
                throw new IllegalArgumentException("Uploaded attribute location/name differs: " + attribute.name());
            }
            VertexFormatElement element = elements.get(location);
            if (!attribute.type().equals(shaderType(element))) {
                throw new IllegalArgumentException("Uploaded attribute type differs: " + attribute.name());
            }
            int fetchedBytes = element.usage() == VertexFormatElement.Usage.NORMAL ? 4 : element.byteSize();
            int offset = format.getOffset(element);
            if (offset < 0 || (long) offset + fetchedBytes > format.getVertexSize()) {
                throw new IllegalArgumentException("Uploaded attribute exceeds its vertex stride: " + attribute.name());
            }
        }
    }

    private static String shaderType(VertexFormatElement element) {
        var type = element.type(); int count = element.count();
        return switch (element.usage()) {
            case POSITION -> type == VertexFormatElement.Type.FLOAT && count == 3 ? "vec3" : "";
            case COLOR -> type == VertexFormatElement.Type.UBYTE && count == 4 ? "vec4" : "";
            case NORMAL -> type == VertexFormatElement.Type.BYTE && count == 3 ? "vec3" : "";
            case UV -> count != 2 ? "" : switch (type) {
                case FLOAT -> "vec2";
                case SHORT -> "ivec2";
                case USHORT -> "uvec2";
                default -> "";
            };
            case GENERIC -> count == 1 && (type == VertexFormatElement.Type.INT
                    || type == VertexFormatElement.Type.SHORT) ? "int" : "";
        };
    }

    boolean isCurrent() {
        GlProgram.LinkedProgram linked = GlProgram.linkedProgram(owner);
        return linked != null && linked.generation() == generation;
    }
}
