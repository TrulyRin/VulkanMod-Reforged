package net.vulkanmod.vulkan.shader.parser;

import net.vulkanmod.gl.GlProgram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;






public final class LevititeShaderConverter {
    private LevititeShaderConverter() {}

    public enum Stage { VERTEX, TESSELLATION_CONTROL, TESSELLATION_EVALUATION, FRAGMENT }

    public record UniformField(String type, String name, int arrayLength) {}


    public record Attribute(String type, String name, int location) {}

    public record Sampler(String name, String type, int binding, Set<Stage> stages) {
        public Sampler { stages = Set.copyOf(stages); }
    }

    public record Result(Map<Stage, String> sources, List<UniformField> uniforms, List<Sampler> samplers,
                         List<Attribute> attributes) {
        public Result {
            EnumMap<Stage, String> copy = new EnumMap<>(Stage.class);
            copy.putAll(sources);
            sources = Collections.unmodifiableMap(copy);
            uniforms = List.copyOf(uniforms);
            samplers = List.copyOf(samplers);
            attributes = List.copyOf(attributes);
        }
    }

    private static final Set<String> UNIFORM_TYPES = Set.of("float", "int", "vec2", "vec3", "vec4", "mat3", "mat4");
    private static final Set<String> VARYING_TYPES = Set.of("float", "vec2", "vec3", "vec4");
    private static final Pattern NUMBER = Pattern.compile("(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?[fF]?");
    private static final String REMAP = "\n    gl_Position.z = (gl_Position.z + gl_Position.w) * 0.5;\n";

    public static Result convert(GlProgram.LinkedProgram linked) {
        if (linked == null) throw failure("A captured linked program is required");
        EnumMap<Stage, Source> stages = new EnumMap<>(Stage.class);
        for (GlProgram.ShaderSource shader : linked.shaders()) {
            Stage stage = switch (shader.type()) {
                case 0x8B31 -> Stage.VERTEX;
                case 0x8E88 -> Stage.TESSELLATION_CONTROL;
                case 0x8E87 -> Stage.TESSELLATION_EVALUATION;
                case 0x8B30 -> Stage.FRAGMENT;
                default -> throw failure("Unsupported captured shader stage: " + shader.type());
            };
            if (!shader.compiled() || shader.source() == null || shader.source().isBlank()) {
                throw failure(stage + " requires nonempty compile-acknowledged source");
            }
            if (stages.containsKey(stage)) throw failure("Multiple compilation units for " + stage + " are unsupported");
            stages.put(stage, new Source(stage, shader.source()));
        }
        boolean tessellated = stages.keySet().equals(EnumSet.allOf(Stage.class));
        if (!tessellated && !stages.keySet().equals(EnumSet.of(Stage.VERTEX, Stage.FRAGMENT))) {
            throw failure("Exactly VERTEX and FRAGMENT, or VERTEX, TCS, TES and FRAGMENT are required");
        }

        LinkedHashMap<String, UniformField> uniforms = new LinkedHashMap<>();
        LinkedHashMap<String, SamplerBuilder> samplers = new LinkedHashMap<>();
        for (Source source : stages.values()) {
            source.scan();
            for (Declaration declaration : source.uniforms.values()) {
                if (declaration.type.equals("sampler2D")) {
                    if (uniforms.containsKey(declaration.name)) throw failure("Conflicting sampler/uniform: " + declaration.name);
                    SamplerBuilder sampler = samplers.computeIfAbsent(declaration.name,
                            name -> new SamplerBuilder(name, samplers.size() + 1));
                    sampler.stages.add(source.stage);
                } else {
                    if (samplers.containsKey(declaration.name)) throw failure("Conflicting uniform/sampler: " + declaration.name);
                    UniformField field = new UniformField(declaration.type, declaration.name, declaration.arrayLength);
                    UniformField previous = uniforms.putIfAbsent(field.name(), field);
                    if (previous != null && !previous.equals(field)) throw failure("Conflicting uniform shape: " + field.name());
                }
            }
        }

        Source vertex = stages.get(Stage.VERTEX), control = stages.get(Stage.TESSELLATION_CONTROL);
        Source evaluation = stages.get(Stage.TESSELLATION_EVALUATION), fragment = stages.get(Stage.FRAGMENT);
        List<Attribute> attributes = bindVertexInputs(vertex, linked.attributeBindings());
        if (tessellated) {
            matchBoundary(vertex, control);
            matchBoundary(control, evaluation);
            matchBoundary(evaluation, fragment);
        } else {
            matchBoundary(vertex, fragment);
        }
        if (fragment.outputs.size() != 1 || !fragment.outputs.containsKey("fragColor")
                || !fragment.outputs.get("fragColor").type.equals("vec4")) {
            throw failure("The fragment stage requires one vec4 fragColor output");
        }
        fragment.locate(fragment.outputs.get("fragColor"), 0);
        if (tessellated) evaluation.remapFinalClip();
        else vertex.remapVertexClip();

        StringBuilder block = new StringBuilder("layout(std140, set = 0, binding = 0) uniform UniformBufferObject {\n");
        for (UniformField field : uniforms.values()) {
            block.append("    ").append(field.type()).append(' ').append(field.name());
            if (field.arrayLength() != 0) block.append('[').append(field.arrayLength()).append(']');
            block.append(";\n");
        }
        block.append("};\n");
        EnumMap<Stage, String> converted = new EnumMap<>(Stage.class);
        for (Source source : stages.values()) {
            for (Declaration declaration : source.uniforms.values()) {
                SamplerBuilder sampler = samplers.get(declaration.name);
                String replacement = sampler == null ? "" : "layout(set = 0, binding = " + sampler.binding
                        + ") uniform sampler2D " + declaration.name + ";";
                source.edits.add(new Edit(declaration.start, declaration.end, replacement));
            }
            converted.put(source.stage, "#version 450\n" + source.extensions + block + "\n" + source.rewrite());
        }
        return new Result(converted, new ArrayList<>(uniforms.values()),
                samplers.values().stream().map(SamplerBuilder::build).toList(), attributes);
    }

    private static List<Attribute> bindVertexInputs(Source vertex, Map<String, Integer> bindings) {
        Set<Integer> used = new HashSet<>();
        List<Attribute> attributes = new ArrayList<>();
        for (Declaration input : vertex.inputs.values()) {
            Integer captured = bindings.get(input.name);
            Integer location = input.location != null ? input.location : captured;
            if (location == null || location < 0) throw failure("Missing explicit attribute location: " + input.name);
            if (captured != null && !captured.equals(location)) throw failure("Conflicting attribute binding: " + input.name);
            if (!used.add(location)) throw failure("Overlapping vertex attribute location: " + location);
            vertex.locate(input, location);
            attributes.add(new Attribute(input.type, input.name, location));
        }
        return List.copyOf(attributes);
    }

    private static void matchBoundary(Source producer, Source consumer) {
        if (!producer.outputs.keySet().equals(consumer.inputs.keySet())) {
            throw failure("Unmatched interface names between " + producer.stage + " and " + consumer.stage);
        }
        int location = 0;
        for (Declaration output : producer.outputs.values()) {
            Declaration input = consumer.inputs.get(output.name);
            if (!output.type.equals(input.type)) throw failure("Interface type mismatch: " + output.name);
            producer.locate(output, location);
            consumer.locate(input, location++);
        }
    }

    private static final class SamplerBuilder {
        final String name;
        final int binding;
        final EnumSet<Stage> stages = EnumSet.noneOf(Stage.class);
        SamplerBuilder(String name, int binding) { this.name = name; this.binding = binding; }
        Sampler build() { return new Sampler(name, "sampler2D", binding, stages); }
    }

    private record Token(String text, int start, int end) {}
    private record Edit(int start, int end, String replacement) {}
    private record Declaration(int start, int end, String storage, String type, String name,
                               int arrayLength, Integer location) {}


    private static final class Source {
        final Stage stage;
        final String text;
        final List<Token> tokens;
        final List<Edit> edits = new ArrayList<>();
        final LinkedHashMap<String, Declaration> uniforms = new LinkedHashMap<>();
        final LinkedHashMap<String, Declaration> inputs = new LinkedHashMap<>();
        final LinkedHashMap<String, Declaration> outputs = new LinkedHashMap<>();
        final StringBuilder extensions = new StringBuilder();
        int mainOpen = -1, mainClose = -1, tessLayouts;

        Source(Stage stage, String text) { this.stage = stage; this.text = text; this.tokens = lex(text); }

        void scan() {
            int start = 0;
            while (start < tokens.size()) {
                if (at(start).startsWith("#")) { directive(tokens.get(start++)); continue; }
                int parentheses = 0, brackets = 0;
                boolean ended = false;
                for (int i = start; i < tokens.size(); i++) {
                    String token = at(i);
                    if (token.startsWith("#")) throw failure(stage + ": directive inside a declaration");
                    if (token.equals("(")) parentheses++;
                    if (token.equals(")") && --parentheses < 0) throw failure(stage + ": unbalanced parentheses");
                    if (token.equals("[")) brackets++;
                    if (token.equals("]") && --brackets < 0) throw failure(stage + ": unbalanced brackets");
                    if (parentheses != 0 || brackets != 0) continue;
                    if (token.equals(";")) {
                        declaration(start, i);
                        start = i + 1;
                        ended = true;
                        break;
                    }
                    if (token.equals("{")) {
                        int close = closingBrace(i);
                        if (i > start && at(i - 1).equals(")") && isFunctionHeader(start, i)) {
                            int nameIndex = start + 1;
                            if (at(nameIndex).equals("main")) {
                                List<String> signature = words(start, i);
                                if (!(signature.equals(List.of("void", "main", "(", ")"))
                                        || signature.equals(List.of("void", "main", "(", "void", ")")))) {
                                    throw failure(stage + ": unsupported main signature");
                                }
                                if (mainOpen >= 0) throw failure(stage + ": duplicate main");
                                mainOpen = i;
                                mainClose = close;
                            }
                            start = close + 1;
                        } else {

                            if (!words(start, i).equals(List.of("in", "gl_PerVertex"))
                                    && !words(start, i).equals(List.of("out", "gl_PerVertex"))) {
                                throw failure(stage + ": unsupported user interface/uniform block");
                            }
                            int semicolon = close + 1;
                            while (semicolon < tokens.size() && !at(semicolon).equals(";")) semicolon++;
                            if (semicolon == tokens.size()) throw failure(stage + ": unterminated built-in block");
                            start = semicolon + 1;
                        }
                        ended = true;
                        break;
                    }
                    if (token.equals("}")) throw failure(stage + ": unexpected closing brace");
                }
                if (!ended) throw failure(stage + ": unterminated top-level statement");
            }
            if (mainOpen < 0) throw failure(stage + ": missing main function");
            boolean tess = stage == Stage.TESSELLATION_CONTROL || stage == Stage.TESSELLATION_EVALUATION;
            if (tessLayouts != (tess ? 1 : 0)) throw failure(stage + ": missing or duplicate tessellation layout");
        }

        private boolean isFunctionHeader(int start, int brace) {
            return brace > start + 3 && identifier(at(start)) && identifier(at(start + 1)) && at(start + 2).equals("(");
        }

        private int closingBrace(int open) {
            int braces = 1, parentheses = 0, brackets = 0;
            for (int i = open + 1; i < tokens.size(); i++) {
                switch (at(i)) {
                    case "{" -> braces++;
                    case "}" -> { if (--braces == 0) {
                        if (parentheses != 0 || brackets != 0) throw failure(stage + ": unbalanced function body");
                        return i;
                    } }
                    case "(" -> parentheses++;
                    case ")" -> { if (--parentheses < 0) throw failure(stage + ": unbalanced function parentheses"); }
                    case "[" -> brackets++;
                    case "]" -> { if (--brackets < 0) throw failure(stage + ": unbalanced function brackets"); }
                    default -> { if (at(i).startsWith("#")) directiveInBody(tokens.get(i)); }
                }
            }
            throw failure(stage + ": unterminated function/block");
        }

        private void directiveInBody(Token token) {
            String command = directiveCommand(token.text);
            if (!command.equals("line")) throw failure(stage + ": only #line is supported inside function bodies");
        }

        private void directive(Token token) {
            String command = directiveCommand(token.text);
            switch (command) {
                case "version" -> edits.add(new Edit(token.start, token.end, ""));
                case "extension" -> {
                    extensions.append(token.text).append('\n');
                    edits.add(new Edit(token.start, token.end, ""));
                }
                case "define", "line", "pragma" -> {                                                         }
                default -> throw failure(stage + ": unsupported or unresolved directive #" + command);
            }
        }

        private void declaration(int start, int end) {
            int cursor = start;
            List<String> layout = List.of();
            if (at(cursor).equals("layout")) {
                if (++cursor >= end || !at(cursor).equals("(")) throw failure(stage + ": malformed layout");
                int first = ++cursor;
                while (cursor < end && !at(cursor).equals(")")) cursor++;
                if (cursor >= end) throw failure(stage + ": malformed layout");
                layout = words(first, cursor);
                cursor++;
            }
            if (cursor >= end) throw failure(stage + ": empty declaration");
            String storage = at(cursor++);
            if (storage.equals("patch")) throw failure(stage + ": patch-qualified payloads are unsupported");
            if (!Set.of("uniform", "in", "out").contains(storage)) {
                for (int i = start; i < end; i++) {
                    if (Set.of("uniform", "in", "out", "buffer", "attribute", "varying", "patch").contains(at(i))) {
                        throw failure(stage + ": unsupported storage declaration");
                    }
                }
                if (!layout.isEmpty()) throw failure(stage + ": unsupported layout declaration");
                return;
            }
            if (cursor == end) {
                boolean control = stage == Stage.TESSELLATION_CONTROL && storage.equals("out")
                        && layout.equals(List.of("vertices", "=", "4"));
                boolean evaluation = stage == Stage.TESSELLATION_EVALUATION && storage.equals("in")
                        && layout.equals(List.of("quads"));
                if (!control && !evaluation) throw failure(stage + ": unsupported standalone stage layout");
                tessLayouts++;
                return;
            }
            Integer location = null;
            if (!layout.isEmpty()) {
                if (storage.equals("uniform") || layout.size() != 3 || !layout.getFirst().equals("location")
                        || !layout.get(1).equals("=")) throw failure(stage + ": unsupported declaration layout");
                location = positiveInteger(layout.get(2), true);
            }
            String type = at(cursor++);
            int arrayLength = 0;
            if (cursor < end && at(cursor).equals("[")) {
                int close = arrayClose(cursor, end);
                arrayLength = arrayLength(cursor, close);
                cursor = close + 1;
            }
            if (cursor >= end || !identifier(at(cursor))) throw failure(stage + ": missing declaration name");
            String name = at(cursor++);
            if (name.startsWith("gl_") || name.equals("UniformBufferObject")) throw failure(stage + ": reserved declaration name " + name);
            if (cursor < end && at(cursor).equals("[")) {
                if (arrayLength != 0) throw failure(stage + ": multidimensional arrays are unsupported");
                int close = arrayClose(cursor, end);
                arrayLength = arrayLength(cursor, close);
                cursor = close + 1;
            }
            if (cursor != end) throw failure(stage + ": unsupported declaration shape for " + name);
            if (storage.equals("uniform")) {
                if (!(UNIFORM_TYPES.contains(type) || type.equals("sampler2D"))) throw failure("Unsupported uniform type: " + type);
                if (arrayLength != 0 && !(type.equals("float") && arrayLength == 6)) throw failure("Unsupported uniform array: " + name);
            } else {
                if (!(VARYING_TYPES.contains(type) || stage == Stage.VERTEX && storage.equals("in") && type.equals("ivec2"))) {
                    throw failure("Unsupported interface type: " + type);
                }
                boolean perVertex = stage == Stage.TESSELLATION_CONTROL
                        || stage == Stage.TESSELLATION_EVALUATION && storage.equals("in");
                if (arrayLength != (perVertex ? -1 : 0)) throw failure(stage + ": unsupported per-vertex interface shape: " + name);
            }
            Declaration declaration = new Declaration(tokens.get(start).start, tokens.get(end).end,
                    storage, type, name, arrayLength, location);
            Map<String, Declaration> destination = storage.equals("uniform") ? uniforms : storage.equals("in") ? inputs : outputs;
            if (uniforms.containsKey(name) || inputs.containsKey(name) || outputs.containsKey(name)) throw failure(stage + ": duplicate declaration: " + name);
            destination.put(name, declaration);
        }

        private int arrayClose(int start, int end) {
            int close = start + 1;
            while (close < end && !at(close).equals("]")) close++;
            if (close >= end || close > start + 2) throw failure(stage + ": unsupported array extent");
            return close;
        }

        private int arrayLength(int open, int close) {
            return close == open + 1 ? -1 : positiveInteger(at(open + 1), false);
        }

        void locate(Declaration declaration, int location) {
            if (declaration.location != null && declaration.location != location) throw failure("Conflicting interface location: " + declaration.name);
            edits.add(new Edit(declaration.start, declaration.end, "layout(location = " + location + ") "
                    + declaration.storage + " " + declaration.type + " " + declaration.name
                    + (declaration.arrayLength == -1 ? "[]" : "") + ";"));
        }

        void remapVertexClip() {
            int position = -1, braces = 0;
            for (int i = mainOpen + 1; i < mainClose; i++) {
                String token = at(i);
                if (token.equals("return")) throw failure("Vertex early return prevents final clip conversion");
                if (token.equals("{")) braces++;
                if (token.equals("}")) braces--;
                if (!token.equals("gl_Position")) continue;
                String preceding = at(i - 1);
                if (position >= 0 || braces != 0 || !at(i + 1).equals("=")
                        || !(preceding.equals(";") || preceding.equals("}") || preceding.equals("{"))) {
                    throw failure("Vertex projection requires one unconditional gl_Position assignment");
                }
                position = i;
            }
            if (position < 0) throw failure("Vertex main lacks a gl_Position assignment");

            edits.add(new Edit(tokens.get(mainClose).start, tokens.get(mainClose).start, REMAP));
        }

        void remapFinalClip() {


            List<List<String>> expectedForms = List.of(
                    List.of("gl_Position", "=", "ProjMat", "*", "ModelViewMat", "*", "vec4", "(",
                            "pos", ",", "1", ")", ";", "gl_Position", ".", "z", "-", "=", "0.00001", ";",
                            "depth", "=", "gl_Position", ".", "z", ";"),
                    List.of("gl_Position", "=", "(", "(", "ProjMat", "*", "ModelViewMat", ")", "*", "vec4", "(",
                            "pos", ",", "1", ")", ")", ";", "gl_Position", ".", "z", "-", "=", "0.00001", ";",
                            "depth", "=", "gl_Position", ".", "z", ";"));
            if (mainClose - expectedForms.getFirst().size() <= mainOpen)
                throw failure("TES main lacks the final projection/bias/depth contract");
            int positionReferences = 0;
            for (int i = mainOpen + 1; i < mainClose; i++) {
                if (at(i).equals("return")) throw failure("TES early return prevents final clip conversion");


                if (at(i).equals("gl_Position") && !at(i - 1).equals(".")) positionReferences++;
            }
            if (positionReferences != 3) throw failure("TES has unsupported extra gl_Position operations");
            int first = -1;
            for (List<String> expected : expectedForms) {
                int candidate = mainClose - expected.size();
                if (candidate <= mainOpen) continue;
                boolean matches = true;
                for (int i = 0; i < expected.size(); i++) {
                    String wanted = expected.get(i), actual = at(candidate + i);
                    if (wanted.equals("1") || wanted.equals("0.00001")) {
                        if (!sameNumber(wanted, actual)) { matches = false; break; }
                    } else if (!wanted.equals(actual)) { matches = false; break; }
                }
                if (matches) { first = candidate; break; }
            }
            if (first < 0) throw failure("TES final projection/bias/depth contract changed");

            int depth = 0;
            for (int i = mainOpen + 1; i < first; i++) {
                if (at(i).equals("{")) depth++;
                if (at(i).equals("}")) depth--;
            }
            String preceding = at(first - 1);
            if (depth != 0 || !(preceding.equals(";") || preceding.equals("}") || preceding.equals("{"))) {
                throw failure("TES projection must be an unconditional final main statement");
            }
            edits.add(new Edit(tokens.get(mainClose).start, tokens.get(mainClose).start, REMAP));
        }

        String rewrite() {
            edits.sort((left, right) -> Integer.compare(right.start, left.start));
            StringBuilder result = new StringBuilder(text);
            int boundary = text.length();
            for (Edit edit : edits) {
                if (edit.end > boundary) throw failure("Overlapping source rewrites");
                result.replace(edit.start, edit.end, edit.replacement);
                boundary = edit.start;
            }
            return result.toString();
        }

        private String at(int index) { return tokens.get(index).text; }
        private List<String> words(int start, int end) { return tokens.subList(start, end).stream().map(Token::text).toList(); }
    }

    private static String directiveCommand(String text) {
        String rest = text.substring(1).stripLeading();
        int end = 0;
        while (end < rest.length() && Character.isLetter(rest.charAt(end))) end++;
        return rest.substring(0, end);
    }

    private static boolean identifier(String text) { return text.matches("[A-Za-z_][A-Za-z_0-9]*"); }

    private static int positiveInteger(String text, boolean allowZero) {
        if (!text.matches("[0-9]+")) throw failure("Expected a literal integer extent/location: " + text);
        try {
            int value = Integer.parseInt(text);
            if (value < (allowZero ? 0 : 1)) throw failure("Invalid integer extent/location: " + text);
            return value;
        } catch (NumberFormatException failure) {
            throw failure("Integer extent/location is too large: " + text);
        }
    }

    private static boolean sameNumber(String expected, String actual) {
        try { return Double.parseDouble(expected) == Double.parseDouble(actual); }
        catch (NumberFormatException ignored) { return false; }
    }

    private static List<Token> lex(String source) {
        List<Token> tokens = new ArrayList<>();
        boolean lineStart = true;
        for (int i = 0; i < source.length();) {
            char character = source.charAt(i);
            if (Character.isWhitespace(character)) {
                if (character == '\n' || character == '\r') lineStart = true;
                i++;
                continue;
            }
            if (character == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                while (i < source.length() && source.charAt(i) != '\n') i++;
                continue;
            }
            if (character == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                int close = source.indexOf("*/", i + 2);
                if (close < 0) throw failure("Unterminated source comment");
                for (int j = i; j < close + 2; j++) if (source.charAt(j) == '\n') lineStart = true;
                i = close + 2;
                continue;
            }
            int start = i;
            if (character == '#') {
                if (!lineStart) throw failure("Preprocessor directive must start a line");
                do {
                    int newline = source.indexOf('\n', i);
                    i = newline < 0 ? source.length() : newline;
                    int tail = i - 1;
                    if (tail >= start && source.charAt(tail) == '\r') tail--;
                    if (tail < start || source.charAt(tail) != '\\' || i == source.length()) break;
                    i++;
                } while (i < source.length());
            } else if (Character.isLetter(character) || character == '_') {
                i++;
                while (i < source.length() && (Character.isLetterOrDigit(source.charAt(i)) || source.charAt(i) == '_')) i++;
            } else if (Character.isDigit(character) || character == '.' && i + 1 < source.length() && Character.isDigit(source.charAt(i + 1))) {
                Matcher number = NUMBER.matcher(source);
                number.region(i, source.length());
                if (!number.lookingAt()) throw failure("Malformed source number");
                i = number.end();
            } else {
                if (character == '"' || character == '\'') throw failure("Unexpected quoted source outside a directive");
                i++;
            }
            tokens.add(new Token(source.substring(start, i), start, i));
            lineStart = false;
        }
        return tokens;
    }

    private static IllegalArgumentException failure(String message) {
        return new IllegalArgumentException("Levitite conversion: " + message);
    }
}
