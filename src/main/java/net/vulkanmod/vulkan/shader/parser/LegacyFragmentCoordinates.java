package net.vulkanmod.vulkan.shader.parser;

import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.util.MappedBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import java.util.regex.Pattern;


final class LegacyFragmentCoordinates {
    private static final String HEIGHT_NAME = "vulkanModLegacyFramebufferHeight";
    private static final Pattern UPPER_LEFT = Pattern.compile(
            "\\blayout\\s*\\([^)]*\\borigin_upper_left\\b[^)]*\\)\\s*in\\s+(?:(?:lowp|mediump|highp)\\s+)?vec4\\s+gl_FragCoord\\b");
    private static final Pattern HARMLESS_DIRECTIVE = Pattern.compile(
            "[\\t ]*#[\\t ]*(?:version[\\t ]+[0-9]+(?:[\\t ]+(?:core|compatibility|es))?"
                    + "|line[\\t ]+[0-9]+(?:[\\t ]+(?:[0-9]+|\"(?:[^\"\\\\]|\\\\.)*\"))?)[\\t \\r]*");

    private LegacyFragmentCoordinates() {}

    static Lowered lower(String code, String occupied) {
        boolean fullyPreprocessed = false;
        Set<String> importedIdentifiers = Set.of();
        if (Pattern.compile("(?m)^[\\t ]*#").matcher(code).find()) {
            var preprocessed = SPIRVUtils.preprocessShaderWithMetadata("legacy-fragment-coordinates", "#version 450\n" + code,
                    SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
            code = preprocessed.source();
            importedIdentifiers = preprocessed.importedIdentifiers();
            code = code.replaceFirst("(?m)^[\\t ]*#[\\t ]*version\\b[^\\r\\n]*", "");
            fullyPreprocessed = hasOnlyHarmlessDirectives(code);
        }
        if (UPPER_LEFT.matcher(withoutQuotedTextAndDirectives(code)).find())
            return new Lowered(code, null, fullyPreprocessed, importedIdentifiers);
        String height = HEIGHT_NAME;
        for (int suffix = 1; containsIdentifier(occupied + "\n" + code, height); suffix++) height = HEIGHT_NAME + suffix;

        StringBuilder result = new StringBuilder(code.length());
        boolean changed = false;
        for (int i = 0; i < code.length();) {
            char c = code.charAt(i);
            if (c == '"' || c == '\'') {
                int end = quotedEnd(code, i);
                result.append(code, i, end);
                i = end;
            } else if (identifierStart(c)) {
                int end = i + 1;
                while (end < code.length() && identifierPart(code.charAt(end))) end++;
                if (code.substring(i, end).equals("gl_FragCoord") && readsY(code, end)) {
                    result.append("(vec4(gl_FragCoord.x, ").append(height)
                            .append(" - gl_FragCoord.y, gl_FragCoord.zw))");
                    changed = true;
                } else result.append(code, i, end);
                i = end;
            } else {
                result.append(c);
                i++;
            }
        }
        return new Lowered(result.toString(), changed ? height : null, fullyPreprocessed, importedIdentifiers);
    }


    static boolean hasOnlyHarmlessDirectives(String code) {
        for (String line : code.split("\\n", -1)) {
            if (line.stripTrailing().endsWith("\\")) return false;
            if (line.indexOf('#') >= 0 && !HARMLESS_DIRECTIVE.matcher(line).matches()) return false;
        }
        return true;
    }

    private static int quotedEnd(String source, int start) {
        char quote = source.charAt(start);
        int end = start + 1;
        while (end < source.length()) {
            char c = source.charAt(end++);
            if (c == '\\' && end < source.length()) end++;
            else if (c == quote) break;
        }
        return end;
    }

    private static String withoutQuotedTextAndDirectives(String source) {
        StringBuilder masked = new StringBuilder(source);
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c != '"' && c != '\'') continue;
            int end = quotedEnd(source, i);
            for (int j = i; j < end; j++) if (source.charAt(j) != '\n' && source.charAt(j) != '\r') masked.setCharAt(j, ' ');
            i = end - 1;
        }
        boolean continuedDirective = false;
        for (int start = 0; start < source.length();) {
            int newline = source.indexOf('\n', start);
            int end = newline >= 0 ? newline : source.length();
            int first = start;
            while (first < end && Character.isWhitespace(masked.charAt(first))) first++;
            boolean directive = continuedDirective || first < end && masked.charAt(first) == '#';
            if (directive) {
                for (int j = start; j < end; j++) if (source.charAt(j) != '\r') masked.setCharAt(j, ' ');
            }
            int last = end - 1;
            if (last >= start && source.charAt(last) == '\r') last--;
            continuedDirective = directive && last >= start && source.charAt(last) == '\\';
            start = end + 1;
        }
        return masked.toString();
    }

    private static boolean readsY(String source, int end) {
        while (end < source.length() && Character.isWhitespace(source.charAt(end))) end++;
        if (end == source.length() || source.charAt(end) != '.') return true;
        end++;
        while (end < source.length() && Character.isWhitespace(source.charAt(end))) end++;
        int start = end;
        while (end < source.length() && identifierPart(source.charAt(end))) end++;
        String swizzle = source.substring(start, end);
        if (swizzle.isEmpty()) return true;

        return swizzle.indexOf('y') >= 0 || swizzle.indexOf('g') >= 0 || swizzle.indexOf('t') >= 0;
    }

    private static boolean containsIdentifier(String code, String name) {
        return Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(code).find();
    }

    private static boolean identifierStart(char c) {
        return c == '_' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static boolean identifierPart(char c) {
        return identifierStart(c) || c >= '0' && c <= '9';
    }

    private static MappedBuffer currentHeight() {
        HeightBuffer.VALUE.putFloat(0, Renderer.getBoundFramebufferHeight());
        return HeightBuffer.VALUE;
    }

    private static final class HeightBuffer {

        private static final MappedBuffer VALUE = MappedBuffer.createFromBuffer(
                ByteBuffer.allocateDirect(Float.BYTES).order(ByteOrder.nativeOrder()));
    }

    record Lowered(String code, String heightName, boolean fullyPreprocessed, Set<String> importedIdentifiers) {
        Lowered {
            importedIdentifiers = Set.copyOf(importedIdentifiers);
        }

        Lowered(String code, String heightName) {
            this(code, heightName, false, Set.of());
        }

        Lowered(String code, String heightName, boolean fullyPreprocessed) {
            this(code, heightName, fullyPreprocessed, Set.of());
        }

        void bindHeight(UBO ubo) {
            if (heightName == null) return;
            ubo.getUniforms().stream().filter(field -> field.getName().equals(heightName))
                    .findFirst().orElseThrow().setSupplier(LegacyFragmentCoordinates::currentHeight);
        }
    }
}
