package net.vulkanmod.vulkan.shader.parser;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static net.vulkanmod.vulkan.shader.parser.UniformParser.removeSemicolon;

public class InputOutputParser {
    private final GlslConverter converterInstance;
    private VertexFormat vertexFormat;

    private final AttributeSet vertInAttributes = new AttributeSet();
    private final AttributeSet vertOutAttributes = new AttributeSet();
    private final AttributeSet fragOutAttributes = new AttributeSet();
    private final Map<Attribute, String> unsignedShortInputs = new LinkedHashMap<>();

    private GlslConverter.ShaderStage shaderStage;

    private int currentLocation = 0;
    private String ioType;
    private String type;
    private String name;

    public InputOutputParser(GlslConverter converterInstance) {
        this.converterInstance = converterInstance;
        this.vertInAttributes.parser = this;
        this.vertOutAttributes.parser = this;
    }

    public void setFormat(VertexFormat format) {
        this.vertexFormat = format;
    }

    public boolean parseToken(String token) {

        if (this.ioType == null)
            this.ioType = token;
        else if (this.type == null)
            this.type = token;
        else if (this.name == null) {
            token = removeSemicolon(token);

            this.name = token;

            if(this.shaderStage == GlslConverter.ShaderStage.Vertex) {
                switch (this.ioType) {
                    case "in" -> this.vertInAttributes.add(this.type, this.name);
                    case "out" -> this.vertOutAttributes.add(this.type, this.name);
                }
            }
            else {
                switch (this.ioType) {
                    case "in" -> {

                    }
                    case "out" -> {
                                                                                                    
                        String outputName = switch (this.name) {
                            case "gl_FragColor", "uc_FragColor" -> "fragColor";
                            default -> this.name;
                        };
                        if (!this.fragOutAttributes.contains(this.type, outputName))
                            this.fragOutAttributes.add(this.type, outputName);
                    }
                }
            }
            this.resetState();
            return true;
        }

        return false;
    }

    private void resetState() {
        this.ioType = null;
        this.type = null;
        this.name = null;
    }

    public String createInOutCode() {

        StringBuilder builder = new StringBuilder();

        if(this.shaderStage == GlslConverter.ShaderStage.Vertex) {

            for(Attribute attribute : this.vertInAttributes.attributes) {
                String raw = unsignedShortInputs.get(attribute);
                if (raw == null)
                    builder.append(String.format("layout(location = %d) in %s %s;\n", attribute.location, attribute.type, attribute.name));
                else
                    builder.append(String.format("layout(location = %d) in uint %s;\nint %s;\n", attribute.location, raw, attribute.name));
            }
            builder.append("\n");

            for(Attribute attribute : this.vertOutAttributes.attributes) {
                builder.append(String.format("layout(location = %d) out %s %s;\n", attribute.location, attribute.type, attribute.name));
            }
            builder.append("\n");
        }
        else {

            for(Attribute attribute : this.vertOutAttributes.attributes) {
                builder.append(String.format("layout(location = %d) in %s %s;\n", attribute.location, attribute.type, attribute.name));
            }
            builder.append("\n");

            if (this.fragOutAttributes.attributes.isEmpty()) {
                                                                                   
                builder.append("layout(location = 0) out vec4 fragColor;\n\n");
            } else {
                for (Attribute attribute : this.fragOutAttributes.attributes) {
                    builder.append(String.format("layout(location = %d) out %s %s;\n", attribute.location, attribute.type, attribute.name));
                }
                builder.append("\n");
            }
        }

        return builder.toString();
    }

                                                                                                                          
    String lowerUnsignedShortInputs(String code, String originalSource) {
        if (vertexFormat == null) return code;
        String occupied = originalSource + code;
        for (Attribute attribute : vertInAttributes.attributes) {
            int index = vertexFormat.getElementAttributeNames().indexOf(attribute.name);
            if (!attribute.type.equals("int") || index < 0) continue;
            VertexFormatElement element = vertexFormat.getElements().get(index);
            boolean custom = switch (element.usage()) {
                case POSITION, COLOR, UV, NORMAL -> false;
                default -> true;
            };
            if (!custom || element.type() != VertexFormatElement.Type.USHORT || element.count() != 1) continue;
            String name = uniqueName("vulkanmod_unsigned_" + attribute.name, occupied);
            unsignedShortInputs.put(attribute, name);
            occupied += " " + name;
        }
        if (unsignedShortInputs.isEmpty()) return code;
                                                                                                        
        var main = Pattern.compile("\\bvoid\\s+(main)\\s*\\(\\s*(?:void\\s*)?\\)\\s*\\{").matcher(withoutDirectivesAndQuotes(code));
        if (!main.find()) throw new IllegalArgumentException("Unsigned legacy vertex input requires a literal main definition");
        int start = main.start(1), end = main.end(1);
        if (main.find()) throw new IllegalArgumentException("Multiple legacy vertex main definitions");
        String entry = uniqueName("vulkanmod_legacy_main", occupied);
        StringBuilder result = new StringBuilder(code.substring(0, start)).append(entry).append(code.substring(end));
        result.append("\nvoid main() {\n");
        unsignedShortInputs.forEach((attribute, raw) -> result.append(attribute.name).append(" = int(").append(raw).append(");\n"));
        return result.append(entry).append("();\n}\n").toString();
    }

    private static String uniqueName(String base, String source) {
        String name = base;
        for (int suffix = 1; source.contains(name); suffix++) name = base + suffix;
        return name;
    }

    private static String withoutDirectivesAndQuotes(String source) {
        char[] masked = source.toCharArray();
        boolean lineStart = true, directive = false, escaped = false;
        char quote = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\n') {
                boolean continued = i > 0 && source.charAt(i - 1) == '\\'
                        || i > 1 && source.charAt(i - 1) == '\r' && source.charAt(i - 2) == '\\';
                directive = directive && continued;
                lineStart = true;
                continue;
            }
            if (lineStart && (c == ' ' || c == '\t' || c == '\r')) continue;
            if (lineStart && c == '#') directive = true;
            lineStart = false;
            if (directive) { masked[i] = ' '; continue; }
            if (quote != 0) {
                masked[i] = ' ';
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') { quote = c; masked[i] = ' '; }
        }
        return new String(masked);
    }

    public void setShaderStage(GlslConverter.ShaderStage shaderStage) {
        this.shaderStage = shaderStage;
    }

    public record Attribute(int location, String type, String name) {}

    static class AttributeSet {
        List<Attribute> attributes = new ObjectArrayList<>();
        int currentLocation = 0;
        InputOutputParser parser;

        void add(String type, String name) {
            int loc = this.currentLocation;
            if (parser != null && parser.shaderStage == GlslConverter.ShaderStage.Vertex && parser.vertexFormat != null) {
                int idx = parser.vertexFormat.getElementAttributeNames().indexOf(name);
                if (idx != -1) {
                    loc = idx;
                }
            }
            this.attributes.add(new Attribute(loc, type, name));
            this.currentLocation++;
        }

        boolean contains(String type, String name) {
            return this.attributes.stream().anyMatch(attribute -> Objects.equals(attribute.name, name) && Objects.equals(attribute.type, type));
        }
    }
}
