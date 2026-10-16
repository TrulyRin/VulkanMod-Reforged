package net.vulkanmod.vulkan.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.texture.VTextureSelector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class PipelineConfig {
    final public EnumMap<SPIRVUtils.ShaderKind, String> shaderPaths;
    final public List<UB> ubs;
    final public List<ImageDescriptorInfo> imageDescriptors;
    final public UB pushConstantsInfo;

    public PipelineConfig(EnumMap<SPIRVUtils.ShaderKind, String> shaderPaths, List<UB> ubs, List<ImageDescriptorInfo> imageDescriptors, UB pushConstantsInfo) {
        this.shaderPaths = shaderPaths;
        this.ubs = ubs;
        this.imageDescriptors = imageDescriptors;
        this.pushConstantsInfo = pushConstantsInfo;
    }

    public static PipelineConfig fromJson(String configName, JsonObject config) {
        var builder = builder();

        String defaultPath = String.format("%s", configName, configName);
        String vertexShader = config.has("vertex") ? config.get("vertex").getAsString() : defaultPath;
        String fragmentShader = config.has("fragment") ? config.get("fragment").getAsString() : defaultPath;

        builder.withShader(SPIRVUtils.ShaderKind.VERTEX_SHADER, vertexShader);
        builder.withShader(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, fragmentShader);

        JsonArray jsonUbos = GsonHelper.getAsJsonArray(config, "UBOs", null);
        JsonArray jsonManualUbos = GsonHelper.getAsJsonArray(config, "ManualUBOs", null);
        JsonArray jsonSamplers = GsonHelper.getAsJsonArray(config, "samplers", null);
        JsonArray jsonPushConstants = GsonHelper.getAsJsonArray(config, "PushConstants", null);

        int nextBinding = 0;
        if (jsonUbos != null) {
            for (JsonElement jsonelement : jsonUbos) {
                UB ub = parseUboNode(jsonelement);
                builder.addUB(ub);
                nextBinding = ub.binding + 1;
            }
        }

        if (jsonManualUbos != null) {
            UB ub = parseManualUboNode(jsonManualUbos.get(0));
            builder.addUB(ub);
        }

        if (jsonSamplers != null) {
            for (JsonElement jsonelement : jsonSamplers) {
                ImageDescriptorInfo imageDescriptor = parseSamplerNode(jsonelement, nextBinding);
                builder.addImageDescriptor(imageDescriptor);
                nextBinding++;
            }
        }

        if (jsonPushConstants != null) {
            UB ub = parsePushConstantNode(jsonPushConstants);
            builder.setPushConstants(ub);
        }

        return builder.build();
    }

    private static UB parseUboNode(JsonElement jsonelement) {
        JsonObject uboJson = GsonHelper.convertToJsonObject(jsonelement, "UBO");
        int binding = GsonHelper.getAsInt(uboJson, "binding");
        int stages = getStageFromString(GsonHelper.getAsString(uboJson, "type"));

        var builder = UB.builder(binding, stages);
        if (GsonHelper.isArrayNode(uboJson, "fields")) {
            JsonArray fields = GsonHelper.getAsJsonArray(uboJson, "fields");

            for (JsonElement field : fields) {
                JsonObject fieldObject = GsonHelper.convertToJsonObject(field, "uniform");
                String name = GsonHelper.getAsString(fieldObject, "name");
                String type2 = GsonHelper.getAsString(fieldObject, "type");
                int count = GsonHelper.getAsInt(fieldObject, "count");


                type2 = convertType(type2, count);
                builder.addUniform(type2, name);
            }
        }
        else {
            int size = GsonHelper.getAsInt(uboJson, "size");

            builder.setSize(size);
        }

        return builder.build();
    }

    private static UB parseManualUboNode(JsonElement jsonelement) {
        JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "ManualUBO");
        int binding = GsonHelper.getAsInt(jsonobject, "binding");
        int stages = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
        int size = GsonHelper.getAsInt(jsonobject, "size");

        var builder = UB.builder(binding, stages);
        builder.setSize(size);

        return builder.build();
    }

    private static ImageDescriptorInfo parseSamplerNode(JsonElement jsonelement, int binding) {
        JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "Sampler");
        String name = GsonHelper.getAsString(jsonobject, "name");

        int imageIdx = VTextureSelector.getTextureIdx(name);
        return new ImageDescriptorInfo(binding, "sampler2D", name, imageIdx);
    }

    private static UB parsePushConstantNode(JsonArray jsonArray) {
        var builder = UB.builder(0, VK_SHADER_STAGE_VERTEX_BIT);

        for (JsonElement field : jsonArray) {
            JsonObject fieldObject = GsonHelper.convertToJsonObject(field, "PushConstants");
            String name = GsonHelper.getAsString(fieldObject, "name");
            String type2 = GsonHelper.getAsString(fieldObject, "type");
            int count = GsonHelper.getAsInt(fieldObject, "count");


            type2 = convertType(type2, count);
            builder.addUniform(type2, name);
        }

        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        EnumMap<SPIRVUtils.ShaderKind, String> shaderPaths = new EnumMap<>(SPIRVUtils.ShaderKind.class);
        public List<UB> ubs = new ArrayList<>();
        public List<ImageDescriptorInfo> imageDescriptors = new ArrayList<>();
        public UB pushConstantsInfo;

        public Builder withShader(SPIRVUtils.ShaderKind shaderKind, String path) {
            this.shaderPaths.put(shaderKind, path);

            return this;
        }

        public Builder addUB(UB ub) {
            this.ubs.add(ub);

            return this;
        }

        public Builder addImageDescriptor(int binding, String type, String name, int imageIdx) {
            this.imageDescriptors.add(new ImageDescriptorInfo(binding, type, name, imageIdx));

            return this;
        }

        public Builder addImageDescriptor(ImageDescriptorInfo info) {
            this.imageDescriptors.add(info);

            return this;
        }

        public Builder setPushConstants(UB pc) {
            this.pushConstantsInfo = pc;

            return this;
        }

        public PipelineConfig build() {
            return new PipelineConfig(this.shaderPaths, this.ubs, this.imageDescriptors, this.pushConstantsInfo);
        }
    }

    public interface DescriptorBinding {
        int getBinding();
    }

    public static class UB {
        public final int binding;
        public final int stage;
        public final int size;
        public final List<Uniform> uniforms;

        public static Builder builder(int binding, int stage) {
            return new Builder(binding, stage);
        }

        public UB(int binding, int stage, int size, List<Uniform> uniforms) {
            this.binding = binding;
            this.stage = stage;
            this.size = size;
            this.uniforms = uniforms;
        }

        public static class Builder {
            int binding;
            int stage;
            int size;
            List<Uniform> uniforms = new ArrayList<>();

            public Builder(int binding, int stage) {
                this.binding = binding;
                this.stage = stage;
            }

            public Builder addUniform(String type, String name) {
                this.uniforms.add(new Uniform(type, name));

                return this;
            }

            public Builder setSize(int size) {
                this.size = size;

                return this;
            }

            public UB build() {
                return new UB(this.binding, this.stage, this.size, this.uniforms);
            }
        }
    }

    public record Uniform(String type, String name) {}

    public record ImageDescriptorInfo(int binding, String type, String name, int imageIdx) {}

    public static int getStageFromString(String s) {
        return switch (s) {
            case "vertex" -> VK_SHADER_STAGE_VERTEX_BIT;
            case "fragment" -> VK_SHADER_STAGE_FRAGMENT_BIT;
            case "all" -> VK_SHADER_STAGE_ALL_GRAPHICS;
            case "compute" -> VK_SHADER_STAGE_COMPUTE_BIT;

            default -> throw new RuntimeException("cannot identify type..");
        };
    }

    public static String convertType(String type, int count) {
        return switch (type) {
            case "matrix4x4" -> "mat4";
            case "float" -> switch (count) {
                case 4 -> "vec4";
                case 3 -> "vec3";
                case 2 -> "vec2";
                case 1 -> "float";

                default -> throw new IllegalStateException("Unexpected value: " + count);
            };
            case "int" -> switch (count) {
                case 4 -> "ivec4";
                case 3 -> "ivec3";
                case 2 -> "ivec2";
                case 1 -> "int";

                default -> throw new IllegalStateException("Unexpected value: " + count);
            };
            default -> throw new RuntimeException("not admitted type..");
        };
    }
}
