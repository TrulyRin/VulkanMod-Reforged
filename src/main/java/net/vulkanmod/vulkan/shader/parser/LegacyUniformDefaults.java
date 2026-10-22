package net.vulkanmod.vulkan.shader.parser;

import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public final class LegacyUniformDefaults {
    private static final Pattern DECIMAL_INT = Pattern.compile("[+-]?(?:0|[1-9][0-9]*)");
    private static final Pattern DECIMAL_FLOAT = Pattern.compile(
            "[+-]?(?:(?:[0-9]+\\.[0-9]*|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?|[0-9]+[eE][+-]?[0-9]+)[fF]?");

    private LegacyUniformDefaults() {}

    public static void bind(UBO ubo, Map<String, UniformParser.Declaration> declarations, Set<String> jsonOwners) {

        for (var declaration : declarations.values()) validateAgreement(declaration);

        for (var field : ubo.getUniforms()) {
            if (jsonOwners.contains(field.getName()) || field.hasExplicitSupplier()) continue;
            var declaration = declarations.get(field.getName());
            if (declaration == null) throw new IllegalArgumentException("Missing legacy uniform declaration for " + field.getName());
            if (declaration.initializers().isEmpty()) {
                field.setZeroDefault();
            } else {
                Integer word = literalWord(declaration.type(), declaration.initializers().getFirst());
                for (String initializer : declaration.initializers()) {
                    if (literalWord(declaration.type(), initializer) == null) {
                        throw new IllegalArgumentException("Unsupported initializer for legacy uniform "
                                + field.getName() + ": " + initializer);
                    }
                }
                field.setDefaultWords(new int[]{word});
            }
        }
    }

    private static void validateAgreement(UniformParser.Declaration declaration) {
        Integer known = null;
        for (String initializer : declaration.initializers()) {
            Integer next = literalWord(declaration.type(), initializer);

            if (next == null) continue;
            boolean different = known != null && ("float".equals(declaration.type())
                    ? Float.intBitsToFloat(known) != Float.intBitsToFloat(next) : !known.equals(next));
            if (different) {
                throw new IllegalArgumentException("Conflicting linked initializers for legacy uniform " + declaration.name());
            }
            known = next;
        }
    }

    private static Integer literalWord(String type, String expression) {
        String value = expression.trim();
        try {
            if ("bool".equals(type)) return switch (value) {
                case "true" -> 1;
                case "false" -> 0;
                default -> null;
            };
            if ("int".equals(type) && DECIMAL_INT.matcher(value).matches()) return Integer.parseInt(value);
            if ("float".equals(type)) {
                float result;
                if (DECIMAL_FLOAT.matcher(value).matches()) result = Float.parseFloat(value);
                else if (DECIMAL_INT.matcher(value).matches()) result = Integer.parseInt(value);
                else return null;
                return Float.isFinite(result) ? Float.floatToRawIntBits(result) : null;
            }
        } catch (NumberFormatException outsideAdmittedRange) {
            return null;
        }
        return null;
    }
}
