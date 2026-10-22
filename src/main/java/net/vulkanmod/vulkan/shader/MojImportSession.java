package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


final class MojImportSession {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z_0-9]*");

    interface Resolver {
        String resolve(boolean relative, String requested, String sourceName) throws IOException;
        String read(String resourcePath) throws IOException;
    }

    private final Resolver resolver;
    private final Set<String> importedPaths = new HashSet<>();
    private final Set<String> importedIdentifiers = new HashSet<>();

    MojImportSession(Resolver resolver) {
        this.resolver = resolver;
    }

    synchronized String expand(String source, String sourceName) throws IOException {
        if (!source.contains("moj_import")) return source;
        try {


            return String.join("\n", new GlslPreprocessor() {
                @Override public String applyImport(boolean relative, String requested) {
                    try {
                        String path = resolver.resolve(relative, requested, sourceName);
                        if (!importedPaths.add(path)) return null;
                        String included = resolver.read(path);
                        recordIncludedSource(included);
                        return included;
                    } catch (IOException failure) {
                        throw new UncheckedIOException("Unable to import " + requested + ": " + failure.getMessage(), failure);
                    }
                }
            }.process(source));
        } catch (UncheckedIOException failure) {
            throw new IOException(failure.getMessage(), failure.getCause());
        }
    }

    synchronized void recordIncludedSource(String source) {


        var identifiers = IDENTIFIER.matcher(source);
        while (identifiers.find()) this.importedIdentifiers.add(identifiers.group());
    }

    synchronized Set<String> importedIdentifiers() {
        return Set.copyOf(this.importedIdentifiers);
    }
}
