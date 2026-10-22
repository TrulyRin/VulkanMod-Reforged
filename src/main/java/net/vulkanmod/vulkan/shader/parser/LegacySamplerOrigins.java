package net.vulkanmod.vulkan.shader.parser;

import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.regex.Pattern;


final class LegacySamplerOrigins {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_]\\w*");
    private static final Map<String, Set<Integer>> OPERATIONS = Map.of(
            "texture", Set.of(2, 3), "textureLod", Set.of(3),
            "textureGrad", Set.of(4), "texelFetch", Set.of(3));

    private LegacySamplerOrigins() {}

    static Lowered lower(String vertex, String fragment, Map<String, UniformParser.Declaration> declarations,
                         String occupied) {
        return lower(vertex, fragment, declarations, occupied, false);
    }

    static Lowered lowerPreprocessed(String vertex, String fragment, Map<String, UniformParser.Declaration> declarations,
                                     String occupied) {
        return lower(vertex, fragment, declarations, occupied, true);
    }

    private static Lowered lower(String vertex, String fragment, Map<String, UniformParser.Declaration> declarations,
                                 String occupied, boolean preprocessed) {
        Set<String> samplers = new TreeSet<>();
        declarations.forEach((name, declaration) -> {
            if ("sampler2D".equals(declaration.type())) samplers.add(name);
        });
        if (samplers.isEmpty()) return new Lowered(vertex, fragment, List.of());

        String preparedVertex = preprocessed ? vertex : preprocessVertex(vertex);
        Body v = new Body(preparedVertex, samplers), f = new Body(fragment, samplers);
        if (!v.balanced || !f.balanced) return new Lowered(vertex, fragment, List.of());
        Set<String> blocked = new HashSet<>(v.blocked);
        blocked.addAll(f.blocked);
        var names = new Names(occupied + '\n' + preparedVertex + '\n' + fragment);
        Map<String, Binding> bindings = new LinkedHashMap<>();
        for (Body body : List.of(v, f)) for (Call call : body.calls) {
            if (call.sampler == null || blocked.contains(call.sampler) || !supported(call)) continue;
            bindings.computeIfAbsent(call.sampler,
                    sampler -> new Binding(sampler, names.fresh("vulkanModEffectSamplerFlip")));
        }
        if (bindings.isEmpty()) return new Lowered(vertex, fragment, List.of());
        return new Lowered(v.rewrite(bindings, names), f.rewrite(bindings, names), List.copyOf(bindings.values()));
    }

    private static String preprocessVertex(String code) {
        if (LegacyFragmentCoordinates.hasOnlyHarmlessDirectives(code)) return code;
        String processed = SPIRVUtils.preprocessShaderWithMetadata("legacy-effect-sampler-origins", "#version 450\n" + code,
                SPIRVUtils.ShaderKind.VERTEX_SHADER).source();
        return processed.replaceFirst("(?m)^[\\t ]*#[\\t ]*version\\b[^\\r\\n]*", "");
    }

    private static boolean supported(Call call) {
        return OPERATIONS.getOrDefault(call.operation, Set.of()).contains(call.arguments.size());
    }

    record Binding(String sampler, String uniform) {}

    record Lowered(String vertex, String fragment, List<Binding> bindings) {
        void bind(UBO ubo, List<ImageDescriptor> descriptors) {
            for (Binding binding : bindings) {
                ImageDescriptor descriptor = descriptors.stream().filter(d -> binding.sampler.equals(d.name))
                        .findFirst().orElseThrow();
                int unit = descriptor.getTextureUnit();

                MappedBuffer value = MappedBuffer.createFromBuffer(ByteBuffer.allocateDirect(Integer.BYTES)
                        .order(ByteOrder.nativeOrder()));
                ubo.getUniforms().stream().filter(u -> binding.uniform.equals(u.getName())).findFirst().orElseThrow()
                        .setSupplier(() -> {
                            VulkanImage image = VTextureSelector.getImage(unit);
                            value.buffer.putInt(0, uniformlyRequiresFlip(image) ? 1 : 0);
                            return value;
                        });
            }
        }
    }

    private static boolean uniformlyRequiresFlip(VulkanImage image) {
        if (image == null || !image.requiresGlDownloadFlip(0)) return false;


        for (int mip = 1; mip < image.mipLevels; mip++) if (!image.requiresGlDownloadFlip(mip)) return false;
        return true;
    }

    private record Token(String text, int start, int end) {}
    private record Argument(int first, int last, int start, int end) {}
    private record Call(String operation, int start, int end, List<Argument> arguments, String sampler) {}
    private record Signature(String operation, int count, String sampler) {}

    private static final class Names {
        private final Set<String> occupied = new HashSet<>();
        Names(String code) { var matcher = IDENTIFIER.matcher(code); while (matcher.find()) occupied.add(matcher.group()); }
        String fresh(String stem) {
            String candidate = stem;
            for (int i = 1; !occupied.add(candidate); i++) candidate = stem + i;
            return candidate;
        }
    }


    private static final class Body {
        final String code;
        final List<Token> tokens;
        final int[] closing;
        final boolean balanced;
        final List<Call> calls = new ArrayList<>();
        final Set<String> blocked = new HashSet<>();

        Body(String code, Set<String> samplers) {
            this.code = code;
            this.tokens = tokenize(code);
            this.closing = new int[tokens.size()];
            Arrays.fill(closing, -1);
            ArrayDeque<Integer> opens = new ArrayDeque<>();
            boolean valid = true;
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i).text.equals("(")) opens.push(i);
                else if (tokens.get(i).text.equals(")")) {
                    if (opens.isEmpty()) { valid = false; break; }
                    closing[opens.pop()] = i;
                }
            }
            this.balanced = valid && opens.isEmpty();
            if (!balanced) return;

            Set<Integer> admittedReferences = new HashSet<>();
            for (int i = 0; i + 1 < tokens.size(); i++) {
                if (!IDENTIFIER.matcher(tokens.get(i).text).matches() || !tokens.get(i + 1).text.equals("(")) continue;
                int close = closing[i + 1];
                List<Argument> arguments = arguments(i + 1, close);
                int reference = arguments.isEmpty() ? -1 : directReference(arguments.getFirst());
                String sampler = reference >= 0 && samplers.contains(tokens.get(reference).text)
                        ? tokens.get(reference).text : null;
                Call call = new Call(tokens.get(i).text, tokens.get(i).start, tokens.get(close).end, arguments, sampler);
                calls.add(call);
                if (sampler != null && (supported(call) || call.operation.equals("textureSize") && arguments.size() == 2))
                    admittedReferences.add(reference);
            }

            for (int i = 0; i < tokens.size(); i++) {
                String name = tokens.get(i).text;
                if (samplers.contains(name) && !admittedReferences.contains(i)) blocked.add(name);
            }
        }

        private List<Argument> arguments(int open, int close) {
            List<Argument> result = new ArrayList<>();
            if (open + 1 == close) return result;
            int first = open + 1, depth = 0;
            for (int i = first; i < close; i++) {
                String token = tokens.get(i).text;
                if (Set.of("(", "[", "{").contains(token)) depth++;
                else if (Set.of(")", "]", "}").contains(token)) depth--;
                else if (token.equals(",") && depth == 0) {
                    result.add(argument(first, i)); first = i + 1;
                }
            }
            result.add(argument(first, close));
            return result;
        }

        private Argument argument(int first, int last) {
            return new Argument(first, last, tokens.get(first).start, tokens.get(last - 1).end);
        }

        private int directReference(Argument argument) {
            int first = argument.first, last = argument.last;
            while (last - first >= 2 && tokens.get(first).text.equals("(") && closing[first] == last - 1) {
                first++; last--;
            }
            return last - first == 1 ? first : -1;
        }

        String rewrite(Map<String, Binding> bindings, Names names) {
            Map<Signature, String> helpers = new LinkedHashMap<>();
            for (Call call : calls) if (bindings.containsKey(call.sampler) && supported(call))
                helpers.computeIfAbsent(signature(call), unused -> names.fresh("vulkanModEffectSample"));
            if (helpers.isEmpty()) return code;
            StringBuilder functions = new StringBuilder();
            helpers.forEach((signature, name) -> functions.append(helper(name, signature, bindings.get(signature.sampler).uniform, names)));
            return functions + rewrite(0, code.length(), helpers);
        }

        private String rewrite(int start, int end, Map<Signature, String> helpers) {
            StringBuilder result = new StringBuilder();
            int cursor = start;
            for (Call call : calls) {
                if (call.start < cursor || call.end > end) continue;
                String helper = helpers.get(signature(call));
                if (helper == null) continue;
                result.append(code, cursor, call.start).append(helper).append('(');
                for (int i = 1; i < call.arguments.size(); i++) {
                    if (i > 1) result.append(',');
                    Argument argument = call.arguments.get(i);
                    result.append(rewrite(argument.start, argument.end, helpers));
                }
                result.append(')');
                cursor = call.end;
            }
            return result.append(code, cursor, end).toString();
        }
    }

    private static Signature signature(Call call) { return new Signature(call.operation, call.arguments.size(), call.sampler); }

    private static String helper(String name, Signature signature, String flip, Names names) {
        String sample = signature.sampler;
        String condition = flip + " != 0";
        String p = names.fresh("vulkanModEffectCoordinate"), bias = names.fresh("vulkanModEffectBias");
        String lod = names.fresh("vulkanModEffectLod"), dx = names.fresh("vulkanModEffectDx"), dy = names.fresh("vulkanModEffectDy");
        String coordinate = "vec2(" + p + ".x, " + condition + " ? 1.0 - " + p + ".y : " + p + ".y)";
        return switch (signature.operation) {
            case "texture" -> signature.count == 2
                    ? "vec4 " + name + "(vec2 " + p + "){return texture(" + sample + "," + coordinate + ");}\n"
                    : "vec4 " + name + "(vec2 " + p + ",float " + bias + "){return texture(" + sample + "," + coordinate + "," + bias + ");}\n";
            case "textureLod" -> "vec4 " + name + "(vec2 " + p + ",float " + lod + "){return textureLod(" + sample + "," + coordinate + "," + lod + ");}\n";
            case "textureGrad" -> "vec4 " + name + "(vec2 " + p + ",vec2 " + dx + ",vec2 " + dy + "){return textureGrad(" + sample + "," + coordinate
                    + ",vec2(" + dx + ".x," + condition + " ? -" + dx + ".y : " + dx + ".y),vec2(" + dy + ".x," + condition + " ? -" + dy + ".y : " + dy + ".y));}\n";
            case "texelFetch" -> "vec4 " + name + "(ivec2 " + p + ",int " + lod + "){return texelFetch(" + sample + ",ivec2(" + p + ".x,"
                    + condition + " ? textureSize(" + sample + "," + lod + ").y - 1 - " + p + ".y : " + p + ".y)," + lod + ");}\n";
            default -> throw new IllegalStateException("Unsupported effect sampling helper");
        };
    }

    private static List<Token> tokenize(String code) {
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < code.length();) {
            char c = code.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            int start = i++;
            if (Character.isJavaIdentifierStart(c)) {
                while (i < code.length() && Character.isJavaIdentifierPart(code.charAt(i))) i++;
            } else if (c == '"' || c == '\'') {
                while (i < code.length()) {
                    char next = code.charAt(i++);
                    if (next == '\\' && i < code.length()) i++;
                    else if (next == c) break;
                }
            }
            tokens.add(new Token(code.substring(start, i), start, i));
        }
        return tokens;
    }
}
