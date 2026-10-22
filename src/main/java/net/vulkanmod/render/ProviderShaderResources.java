package net.vulkanmod.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public record ProviderShaderResources(String vertexSource, String fragmentSource, Map<ResourceLocation, String> digests) {
    private static final int MAX_RESOURCE_BYTES = 65536;
    private static final int MAX_TOTAL_BYTES = 262144;
    private static final int MAX_RESOURCES = 64;
    private static final int MAX_INCLUDE_DEPTH = 16;
    private static final int MAX_EXPANDED_CHARS = 1048576;

    private static final Pattern MOJ_IMPORT = Pattern.compile(
            "(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*moj_import(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(?:\"(.*)\"|<(.*)>))");
    private static final Pattern ENDS_WITH_WHITESPACE = Pattern.compile(
            "(?:^|\\v)(?:\\s|/\\*(?:[^*]|\\*+[^*/])*\\*+/|(//[^\\v]*))*\\z");
    private static final Pattern UNRESOLVED_INCLUDE = Pattern.compile("(?m)^\\s*#\\s*(?:moj_import|include)\\b");

    public ProviderShaderResources {
        digests = Map.copyOf(digests);
    }

    public boolean matches(Map<ResourceLocation, String> canonical) {
        return this.digests.equals(canonical);
    }

    public static ProviderShaderResources load(ResourceProvider provider, ResourceLocation jsonLocation) throws IOException {
        Loader loader = new Loader(provider);
        var json = GsonHelper.parse(new StringReader(loader.read(jsonLocation)));
        ResourceLocation vertex = stageLocation(GsonHelper.getAsString(json, "vertex"), "vsh");
        ResourceLocation fragment = stageLocation(GsonHelper.getAsString(json, "fragment"), "fsh");

        loader.read(vertex);
        loader.read(fragment);
        String vertexSource = loader.expand(vertex, directory(vertex.getPath()), "", 0, new HashSet<>(), new HashSet<>());
        String fragmentSource = loader.expand(fragment, directory(fragment.getPath()), "", 0, new HashSet<>(), new HashSet<>());
        return new ProviderShaderResources(vertexSource, fragmentSource, loader.digests);
    }

    private static ResourceLocation stageLocation(String name, String extension) throws IOException {
        ResourceLocation stage = checkedLocation(name);
        return ResourceLocation.fromNamespaceAndPath(stage.getNamespace(), "shaders/core/" + stage.getPath() + "." + extension);
    }

    private static ResourceLocation checkedLocation(String name) throws IOException {
        ResourceLocation location = ResourceLocation.tryParse(name);
        if (location == null || name.length() > 512) throw new IOException("Invalid shader resource path: " + name);
        for (String segment : location.getPath().split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Unsupported shader resource path: " + name);
            }
        }
        return location;
    }

    private static String directory(String path) {
        return path.substring(0, path.lastIndexOf('/') + 1);
    }

    private static ResourceLocation importLocation(String stageBase, String name, boolean relative) throws IOException {
        ResourceLocation target = checkedLocation(name);


        return ResourceLocation.fromNamespaceAndPath(target.getNamespace(),
                (relative ? stageBase : "shaders/include/") + target.getPath());
    }

    private static final class Loader {
        private final ResourceProvider provider;
        private final Map<ResourceLocation, String> sources = new LinkedHashMap<>();
        private final Map<ResourceLocation, String> digests = new LinkedHashMap<>();
        private int totalBytes;

        private Loader(ResourceProvider provider) { this.provider = provider; }

        private String read(ResourceLocation location) throws IOException {
            String cached = this.sources.get(location);
            if (cached != null) return cached;
            if (this.sources.size() >= MAX_RESOURCES) throw new IOException("Shader include resource count exceeded " + MAX_RESOURCES);
            byte[] bytes;
            try (var input = this.provider.getResourceOrThrow(location).open()) {
                bytes = input.readNBytes(MAX_RESOURCE_BYTES + 1);
            }
            if (bytes.length > MAX_RESOURCE_BYTES) throw new IOException("Shader resource exceeds byte limit: " + location);
            this.totalBytes += bytes.length;
            if (this.totalBytes > MAX_TOTAL_BYTES) throw new IOException("Shader include closure exceeds byte limit");
            String source = new String(bytes, StandardCharsets.UTF_8);
            this.sources.put(location, source);
            try {
                this.digests.put(location, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            } catch (NoSuchAlgorithmException impossible) {
                throw new IllegalStateException(impossible);
            }
            return source;
        }

        private String expand(ResourceLocation location, String stageBase, String includeDirectory,
                              int depth, Set<ResourceLocation> active,
                              Set<ResourceLocation> imported) throws IOException {
            if (depth > MAX_INCLUDE_DEPTH) throw new IOException("Shader include depth exceeded " + MAX_INCLUDE_DEPTH);
            if (!active.add(location)) throw new IOException("Shader include cycle at " + location);
            try {
                if (!imported.add(location)) return "";
                String source = read(location);
                String code = maskComments(source);
                StringBuilder output = new StringBuilder(source.length());
                Matcher directive = MOJ_IMPORT.matcher(source);
                int offset = 0;
                while (directive.find()) {
                    if (code.charAt(directive.start()) != '#' || isDirectiveDisabled(source, directive, offset)) continue;
                    appendBounded(output, code.substring(offset, directive.start(1)));
                    boolean relative = directive.group(2) != null;
                    String importPath = includeDirectory + directive.group(relative ? 2 : 3);
                    ResourceLocation include = importLocation(stageBase, importPath, relative);
                    String expanded = expand(include, stageBase, relative ? directory(importPath) : "", depth + 1, active, imported);
                    appendBounded(output, expanded);
                    if (!expanded.endsWith("\n") && !expanded.endsWith("\r")) appendBounded(output, "\n");
                    offset = directive.end(1);
                }
                appendBounded(output, code.substring(offset));
                String expanded = output.toString();
                if (UNRESOLVED_INCLUDE.matcher(expanded).find()) {
                    throw new IOException("Invalid or unsupported shader include in selected resource: " + location);
                }
                return expanded;
            } finally {
                active.remove(location);
            }
        }
    }

    private static void appendBounded(StringBuilder output, String source) throws IOException {
        if ((long) output.length() + source.length() > MAX_EXPANDED_CHARS) {
            throw new IOException("Expanded shader include source exceeds character limit");
        }
        output.append(source);
    }

    private static boolean isDirectiveDisabled(String source, Matcher directive, int offset) {
        if (directive.start() == offset) return false;
        Matcher prefix = ENDS_WITH_WHITESPACE.matcher(source.substring(offset, directive.start()));
        return !prefix.find() || prefix.end(1) == directive.start();
    }


    private static String maskComments(String source) throws IOException {
        StringBuilder result = new StringBuilder(source.length());
        boolean lineComment = false, blockComment = false, escaped = false;
        char quote = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i), next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (lineComment) {
                if (c == '\n') {
                    lineComment = i > 0 && source.charAt(i - 1) == '\\'
                            || i > 1 && source.charAt(i - 1) == '\r' && source.charAt(i - 2) == '\\';
                    result.append(c);
                } else result.append(c == '\r' ? c : ' ');
            } else if (blockComment) {
                if (c == '*' && next == '/') { result.append("  "); i++; blockComment = false; }
                else result.append(c == '\n' || c == '\r' ? c : ' ');
            } else if (quote != 0) {
                result.append(c);
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') { quote = c; result.append(c); }
            else if (c == '/' && next == '/') { result.append("  "); i++; lineComment = true; }
            else if (c == '/' && next == '*') { result.append("  "); i++; blockComment = true; }
            else result.append(c);
        }
        if (blockComment) throw new IOException("Unterminated shader include comment");
        return result.toString();
    }
}
