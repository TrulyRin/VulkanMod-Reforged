package net.vulkanmod.mixin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinStaticMethodVisibilityTest {

    private static final Pattern STATIC_METHOD = Pattern.compile("\\bstatic\\b[^=(){};]*\\b\\w+\\s*\\(");

    @Test
    void unannotatedStaticHelpersInMixinsArePrivate() throws IOException {
        Path mixinRoot = Paths.get("src/main/java/net/vulkanmod/mixin");
        assertTrue(Files.isDirectory(mixinRoot),
                "mixin source dir not found at " + mixinRoot.toAbsolutePath());

        List<String> violations = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(mixinRoot)) {
            List<Path> javaFiles = stream.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path file : javaFiles) {
                scanFile(file, violations);
            }
        }

        assertTrue(violations.isEmpty(),
                "Non-private unannotated static method(s) in a @Mixin class WILL crash Mixin at APPLY "
                        + "(bootstrap crash). Make them `private static`:\n  " + String.join("\n  ", violations));
    }

    private static void scanFile(Path file, List<String> violations) throws IOException {
        boolean precededByAnnotation = false;
        for (String raw : Files.readAllLines(file)) {
            String trimmed = raw.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("*")
                    || trimmed.startsWith("/*")) {
                continue;
            }
            if (trimmed.startsWith("@")) {
                precededByAnnotation = true;
                continue;
            }

            String code = raw.replaceAll("/\\*.*?\\*/", "").replaceAll("//.*$", "");
            boolean isStaticMethod = STATIC_METHOD.matcher(code).find()
                    && !code.trim().matches("^static\\s*\\{.*")      // not a static initializer
                    && !code.contains("class ") && !code.contains("interface ")
                    && !code.contains("enum ");

            if (isStaticMethod && !code.contains("private") && !precededByAnnotation) {
                violations.add(file.getFileName() + ": " + trimmed);
            }
            precededByAnnotation = false;
        }
    }
}
