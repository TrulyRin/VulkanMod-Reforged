package net.vulkanmod.vulkan.shader.parser;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.*;
import java.util.regex.Pattern;

import static net.vulkanmod.vulkan.shader.parser.UniformParser.removeSemicolon;

public class InputOutputParser {
    private static final Set<String> QUALIFIERS = Set.of("flat", "smooth", "noperspective", "centroid", "sample",
            "invariant", "precise", "highp", "mediump", "lowp");
    private static final Set<String> DIRECTIONS = Set.of("in", "out", "attribute", "varying");
    private static final Pattern TOKENS = Pattern.compile("[A-Za-z_]\\w*|[0-9]+|\\S");
    private static final Pattern IDENTIFIERS = Pattern.compile("[A-Za-z_]\\w*");
    private static final Pattern IMPORT_DIRECTIVE = Pattern.compile("#\\s*(?:include|moj_import)\\b.*");
    private static final Pattern INTERFACE_TYPE = Pattern.compile("(?:[diub]?vec[234]|d?mat[234](?:x[234])?|float|double|int|uint|bool)");
    private VertexFormat vertexFormat;

    private final AttributeSet vertInAttributes = new AttributeSet();
    private final AttributeSet vertOutAttributes = new AttributeSet();
    private final AttributeSet fragInAttributes = new AttributeSet();
    private final AttributeSet fragOutAttributes = new AttributeSet();
    private final Map<Attribute, String> unsignedShortInputs = new LinkedHashMap<>();
    private final Map<Attribute, Attribute> varyingConsumers = new LinkedHashMap<>();
    private final Set<String> fragmentCodeIdentifiers = new HashSet<>();
    private boolean fragmentPreprocessingUnknown;
    private boolean fragmentRequiresImportMetadata;

    private GlslConverter.ShaderStage shaderStage;
    private boolean locationsResolved;
    private int braceDepth, parenthesisDepth;
    private boolean continuedDirective;
    private String ioType;
    private String type;
    private String name;

    public InputOutputParser(GlslConverter converterInstance) {
    }

    public void setFormat(VertexFormat format) {
        this.vertexFormat = format;
        this.locationsResolved = false;
    }


    public boolean tryParseDeclaration(String line) {
        String stripped = line.stripLeading();
        if (continuedDirective || stripped.startsWith("#")) {
            if (shaderStage == GlslConverter.ShaderStage.Fragment
                    && (continuedDirective || !stripped.stripTrailing().matches("#\\s*version\\b.*"))) {

                fragmentPreprocessingUnknown = true;


                observeFragmentIdentifiers(line);
                fragmentRequiresImportMetadata |= continuedDirective || IMPORT_DIRECTIVE.matcher(stripped.stripTrailing()).matches();
                locationsResolved = false;
            }
            continuedDirective = line.stripTrailing().endsWith("\\");
            return false;
        }
        if (braceDepth != 0 || parenthesisDepth != 0 || stripped.isBlank()) {
            observeScope(line);
            return false;
        }

        var matcher = TOKENS.matcher(line);
        List<String> tokens = new ArrayList<>();
        while (matcher.find()) tokens.add(matcher.group());
        List<ParsedDeclaration> declarations = new ArrayList<>();
        int cursor = 0;
        while (cursor < tokens.size()) {
            ParsedDeclaration declaration = parseDeclaration(tokens, cursor, line);
            if (declaration == null) {
                if (!declarations.isEmpty())
                    throw new IllegalArgumentException("Code after legacy interface declaration must start on a new line: " + line.trim());
                observeScope(line);
                return false;
            }
            declarations.add(declaration);
            cursor = declaration.end;
        }
        if (declarations.isEmpty()) return false;
        for (var declaration : declarations)
            for (var attribute : declaration.attributes) add(declaration.direction, attribute);
        return true;
    }


    public void acceptFullyPreprocessedFragmentBody(String code) {
        if (fragmentRequiresImportMetadata) {
            Objects.requireNonNull(code, "code");
            fragmentPreprocessingUnknown = true;
            locationsResolved = false;
            return;
        }
        acceptFullyPreprocessedFragmentBody(code, Set.of());
    }


    public void acceptFullyPreprocessedFragmentBody(String code, Set<String> importedIdentifiers) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(importedIdentifiers, "importedIdentifiers");
        if (!LegacyFragmentCoordinates.hasOnlyHarmlessDirectives(code)) {
            fragmentPreprocessingUnknown = true;
            locationsResolved = false;
            return;
        }
        fragmentCodeIdentifiers.addAll(importedIdentifiers);
        observeFragmentIdentifiers(code);
        fragmentPreprocessingUnknown = false;
        locationsResolved = false;
    }

    private void observeFragmentIdentifiers(String code) {
        var identifiers = IDENTIFIERS.matcher(code);
        while (identifiers.find()) fragmentCodeIdentifiers.add(identifiers.group());
    }

    private ParsedDeclaration parseDeclaration(List<String> tokens, int start, String source) {
        List<String> qualifiers = new ArrayList<>();
        List<String> layout = new ArrayList<>();
        String direction = null;
        int cursor = start;
        while (cursor < tokens.size()) {
            String token = tokens.get(cursor);
            if (token.equals("layout") && cursor + 1 < tokens.size() && tokens.get(cursor + 1).equals("(")) {
                cursor += 2;
                while (cursor < tokens.size() && !tokens.get(cursor).equals(")")) layout.add(tokens.get(cursor++));
                if (cursor == tokens.size()) throw unsupported(source);
                cursor++;
            } else if (QUALIFIERS.contains(token)) {
                if (!qualifiers.contains(token)) qualifiers.add(token);
                cursor++;
            } else if (DIRECTIONS.contains(token) && direction == null) {
                direction = token;
                cursor++;
            } else break;
        }
        if (direction == null) return null;

        if (cursor >= tokens.size() || tokens.get(cursor).equals(";")) return null;
        String type = tokens.get(cursor++);
        if (cursor < tokens.size() && tokens.get(cursor).startsWith("gl_")
                && !tokens.get(cursor).equals("gl_FragColor")) return null;
        if (!INTERFACE_TYPE.matcher(type).matches()) throw unsupported(source);
        long interpolationCount = qualifiers.stream().filter(q -> q.equals("flat") || q.equals("smooth") || q.equals("noperspective")).count();
        if (interpolationCount > 1) throw unsupported(source);

        Integer location = parseLocation(layout, source);
        List<Attribute> attributes = new ArrayList<>();
        while (cursor < tokens.size()) {
            String name = tokens.get(cursor++);
            if (!name.matches("[A-Za-z_]\\w*")) throw unsupported(source);
            StringBuilder arrays = new StringBuilder();
            int span = typeLocationSpan(type);
            while (cursor < tokens.size() && tokens.get(cursor).equals("[")) {
                if (cursor + 2 >= tokens.size() || !tokens.get(cursor + 1).matches("[0-9]+")
                        || !tokens.get(cursor + 2).equals("]")) throw unsupported(source);
                try {
                    int length = Integer.parseInt(tokens.get(cursor + 1));
                    if (length == 0) throw unsupported(source);
                    span = Math.multiplyExact(span, length);
                    arrays.append('[').append(length).append(']');
                } catch (NumberFormatException | ArithmeticException e) {
                    throw unsupported(source);
                }
                cursor += 3;
            }
            attributes.add(new Attribute(type, name + arrays, List.copyOf(qualifiers), location, span));
            if (cursor >= tokens.size()) throw unsupported(source);
            String delimiter = tokens.get(cursor++);
            if (delimiter.equals(";")) return new ParsedDeclaration(direction, attributes, cursor);
            if (!delimiter.equals(",") || location != null) throw unsupported(source);
        }
        throw unsupported(source);
    }

    private static Integer parseLocation(List<String> layout, String source) {
        if (layout.isEmpty()) return null;
        if (layout.size() != 3 || !layout.get(0).equals("location") || !layout.get(1).equals("=")
                || !layout.get(2).matches("[0-9]+")) throw unsupported(source);
        try {
            return Integer.parseInt(layout.get(2));
        } catch (NumberFormatException e) {
            throw unsupported(source);
        }
    }

    private static int typeLocationSpan(String type) {
        if (type.startsWith("mat") || type.startsWith("dmat")) {
            int offset = type.startsWith("dmat") ? 4 : 3;
            int columns = type.charAt(offset) - '0';
            int rows = type.length() > offset + 1 ? type.charAt(offset + 2) - '0' : columns;
            return columns * (type.startsWith("dmat") && rows > 2 ? 2 : 1);
        }
        return type.equals("dvec3") || type.equals("dvec4") ? 2 : 1;
    }

    private static IllegalArgumentException unsupported(String source) {
        return new IllegalArgumentException("Unsupported legacy interface declaration: " + source.trim());
    }

    private void observeScope(String line) {
        if (shaderStage == GlslConverter.ShaderStage.Fragment) {

            observeFragmentIdentifiers(line + "\n" + CodeParser.parseCodeLine(line, shaderStage));
            locationsResolved = false;
        }
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') quote = c;
            else if (c == '{') braceDepth++;
            else if (c == '}') braceDepth = Math.max(0, braceDepth - 1);
            else if (c == '(') parenthesisDepth++;
            else if (c == ')') parenthesisDepth = Math.max(0, parenthesisDepth - 1);
        }
    }

    public boolean parseToken(String token) {

        if (this.ioType == null)
            this.ioType = token;
        else if (this.type == null)
            this.type = token;
        else if (this.name == null) {
            token = removeSemicolon(token);

            this.name = token;

            add(this.ioType, new Attribute(this.type, this.name, List.of(), null, typeLocationSpan(this.type)));
            this.resetState();
            return true;
        }

        return false;
    }

    private void add(String direction, Attribute attribute) {
        direction = switch (direction) {
            case "attribute" -> "in";
            case "varying" -> shaderStage == GlslConverter.ShaderStage.Vertex ? "out" : "in";
            default -> direction;
        };
        AttributeSet target;
        if (shaderStage == GlslConverter.ShaderStage.Vertex)
            target = direction.equals("in") ? vertInAttributes : vertOutAttributes;
        else target = direction.equals("in") ? fragInAttributes : fragOutAttributes;
        if (target == fragOutAttributes) {
            String outputName = switch (attribute.name) {
                case "gl_FragColor", "uc_FragColor" -> "fragColor";
                default -> attribute.name;
            };
            attribute = new Attribute(attribute.type, outputName, attribute.qualifiers, attribute.explicitLocation, attribute.span);
            if (target.contains(attribute.type, outputName)) return;
        }
        target.attributes.add(attribute);
        locationsResolved = false;
    }

    private void resetState() {
        this.ioType = null;
        this.type = null;
        this.name = null;
    }

    public String createInOutCode() {
        resolveLocations();

        StringBuilder builder = new StringBuilder();

        if(this.shaderStage == GlslConverter.ShaderStage.Vertex) {

            for(Attribute attribute : this.vertInAttributes.attributes) {
                String raw = unsignedShortInputs.get(attribute);
                if (raw == null)
                    appendDeclaration(builder, "in", attribute, vertInAttributes.location(attribute));
                else
                    builder.append(String.format("layout(location = %d) %sin uint %s;\nint %s;\n",
                            vertInAttributes.location(attribute), attribute.qualifierCode(), raw, attribute.name));
            }
            builder.append("\n");

            for(Attribute attribute : this.vertOutAttributes.attributes) {
                appendDeclaration(builder, "out", attribute, vertOutAttributes.location(attribute));
            }
            builder.append("\n");
        }
        else {

            for(Attribute attribute : this.vertOutAttributes.attributes) {
                Attribute consumer = varyingConsumers.get(attribute);
                if (consumer != null)
                    appendDeclaration(builder, "in", consumer, vertOutAttributes.location(attribute));
                else if (fragInAttributes.find(attribute.baseName()) == null)
                    appendDeclaration(builder, "in", attribute.synthesizedInput(), vertOutAttributes.location(attribute));
            }
            builder.append("\n");

            if (this.fragOutAttributes.attributes.isEmpty()) {

                builder.append("layout(location = 0) out vec4 fragColor;\n\n");
            } else {
                for (Attribute attribute : this.fragOutAttributes.attributes) {
                    appendDeclaration(builder, "out", attribute, fragOutAttributes.location(attribute));
                }
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private static void appendDeclaration(StringBuilder builder, String direction, Attribute attribute, int location) {
        builder.append(String.format("layout(location = %d) %s%s %s %s;\n",
                location, attribute.qualifierCode(), direction, attribute.type, attribute.name));
    }

    private void resolveLocations() {
        if (locationsResolved) return;
        Map<Attribute, Integer> varyingLocations = new HashMap<>();
        varyingConsumers.clear();
        for (Attribute consumer : fragInAttributes.attributes) {
            Attribute producer = consumer.explicitLocation == null ? null
                    : vertOutAttributes.findExplicitLocation(consumer.explicitLocation);
            if (producer == null) producer = vertOutAttributes.find(consumer.baseName());
            if (producer == null && !fragmentPreprocessingUnknown
                    && !fragmentCodeIdentifiers.contains(consumer.baseName())) continue;
            if (producer == null)
                throw new IllegalArgumentException("Missing vertex producer for legacy fragment input " + consumer.baseName());
            if (!producer.semanticType().equals(consumer.semanticType()) || !producer.arraySuffix().equals(consumer.arraySuffix())
                    || !producer.interpolation().equals(consumer.interpolation()))
                throw new IllegalArgumentException("Mismatched legacy shader interface " + consumer.baseName());
            if (consumer.explicitLocation != null) {
                if (producer.explicitLocation != null && !producer.explicitLocation.equals(consumer.explicitLocation))
                    throw new IllegalArgumentException("Mismatched explicit interface location for " + consumer.baseName());
                varyingLocations.put(producer, consumer.explicitLocation);
            }
            if (varyingConsumers.putIfAbsent(producer, consumer) != null)
                throw new IllegalArgumentException("Overlapping legacy fragment input location for " + consumer.baseName());
        }
        vertInAttributes.resolve(Map.of(), true);
        vertOutAttributes.resolve(varyingLocations, false);
        fragOutAttributes.resolve(Map.of(), false);
        locationsResolved = true;
    }


    String lowerUnsignedShortInputs(String code, String originalSource) {
        if (vertexFormat == null) return code;
        String occupied = originalSource + code;
        for (Attribute attribute : vertInAttributes.attributes) {
            int index = vertexFormat.getElementAttributeNames().indexOf(attribute.baseName());
            if (!attribute.type.equals("int") || !attribute.arraySuffix().isEmpty() || index < 0) continue;
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
        if (this.shaderStage != shaderStage) {
            braceDepth = parenthesisDepth = 0;
            continuedDirective = false;
        }
        this.shaderStage = shaderStage;
    }

    private record ParsedDeclaration(String direction, List<Attribute> attributes, int end) {}

    private record Attribute(String type, String name, List<String> qualifiers, Integer explicitLocation, int span) {
        String baseName() { int array = name.indexOf('['); return array < 0 ? name : name.substring(0, array); }
        String arraySuffix() { int array = name.indexOf('['); return array < 0 ? "" : name.substring(array); }
        String qualifierCode() { return qualifiers.isEmpty() ? "" : String.join(" ", qualifiers) + " "; }
        String semanticType() {
            return switch (type) {
                case "mat2x2" -> "mat2";
                case "mat3x3" -> "mat3";
                case "mat4x4" -> "mat4";
                case "dmat2x2" -> "dmat2";
                case "dmat3x3" -> "dmat3";
                case "dmat4x4" -> "dmat4";
                default -> type;
            };
        }
        String interpolation() {
            return qualifiers.stream().filter(q -> q.equals("flat") || q.equals("noperspective")).findFirst().orElse("smooth");
        }
        Attribute synthesizedInput() {
            if (!qualifiers.contains("invariant")) return this;
            return new Attribute(type, name, qualifiers.stream().filter(q -> !q.equals("invariant")).toList(), explicitLocation, span);
        }
    }

    private final class AttributeSet {
        final List<Attribute> attributes = new ObjectArrayList<>();
        final Map<Attribute, Integer> locations = new HashMap<>();

        void resolve(Map<Attribute, Integer> overrides, boolean vertexInputs) {
            locations.clear();
            BitSet occupied = new BitSet();
            for (Attribute attribute : attributes) {
                Integer location = overrides.getOrDefault(attribute, attribute.explicitLocation);
                if (vertexInputs && vertexFormat != null) {
                    int index = vertexFormat.getElementAttributeNames().indexOf(attribute.baseName());
                    if (index >= 0) location = index;
                }
                if (location != null) reserve(attribute, location, occupied);
            }
            int cursor = 0;
            for (Attribute attribute : attributes) {
                Integer location = locations.get(attribute);
                if (location == null) {
                    location = cursor;
                    for (int used = occupied.nextSetBit(location); used >= 0 && used < end(attribute, location);
                         used = occupied.nextSetBit(location)) location = used + 1;
                    reserve(attribute, location, occupied);
                }
                cursor = Math.max(cursor, end(attribute, location));
            }
        }

        private void reserve(Attribute attribute, int location, BitSet occupied) {
            int end = end(attribute, location);
            int used = occupied.nextSetBit(location);
            if (used >= 0 && used < end)
                throw new IllegalArgumentException("Overlapping legacy interface location " + used + " for " + attribute.baseName());
            occupied.set(location, end);
            locations.put(attribute, location);
        }

        private int end(Attribute attribute, int location) {
            if (location < 0 || location > Integer.MAX_VALUE - attribute.span)
                throw new IllegalArgumentException("Invalid legacy interface location for " + attribute.baseName());
            return location + attribute.span;
        }

        int location(Attribute attribute) { return locations.get(attribute); }

        Attribute find(String name) {
            return attributes.stream().filter(attribute -> attribute.baseName().equals(name)).findFirst().orElse(null);
        }

        Attribute findExplicitLocation(int location) {
            return attributes.stream().filter(attribute -> Objects.equals(attribute.explicitLocation, location)).findFirst().orElse(null);
        }

        boolean contains(String type, String name) {
            return this.attributes.stream().anyMatch(attribute -> Objects.equals(attribute.name, name) && Objects.equals(attribute.type, type));
        }
    }
}
