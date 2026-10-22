package net.vulkanmod.vulkan.shader.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


final class LegacyArrayExtent {

    static final int MAX_EXTENT = Integer.MAX_VALUE / 16;
    private static final int MAX_EXPRESSION_CHARACTERS = 16_384;
    private static final int MAX_EXPANDED_TOKENS = 4096;
    private static final int MAX_MACRO_DEPTH = 64;
    private static final int MAX_EXPRESSION_DEPTH = 128;

    private final Map<String, Macro> macros = new HashMap<>();
    private final ArrayDeque<Boolean> conditionalElseSeen = new ArrayDeque<>();
    private boolean continuedDirective;
    private boolean currentDirectiveContinuation;
    private String invalidState;


    void observeLine(String line) {
        String trimmed = line.stripLeading();
        boolean wasContinued = this.continuedDirective;
        this.currentDirectiveContinuation = wasContinued;
        boolean directive = wasContinued || trimmed.startsWith("#");
        this.continuedDirective = directive && line.stripTrailing().endsWith("\\");
        if (wasContinued || !directive) return;

        String text = trimmed.substring(1).stripLeading();
        if (text.isEmpty()) return;
        int end = identifierEnd(text, 0);
        if (end == 0) {
            this.invalidState = "unrecognized preprocessor directive";
            return;
        }
        String operation = text.substring(0, end);
        String arguments = text.substring(end).stripLeading();
        switch (operation) {
            case "define" -> define(arguments);
            case "undef" -> undefine(arguments);
            case "if", "ifdef", "ifndef" -> this.conditionalElseSeen.push(false);
            case "else", "elif" -> {
                if (this.conditionalElseSeen.isEmpty() || this.conditionalElseSeen.peek()) {
                    this.invalidState = "unbalanced conditional directive";
                } else if (operation.equals("else")) {
                    this.conditionalElseSeen.pop();
                    this.conditionalElseSeen.push(true);
                }
            }
            case "endif" -> {
                if (this.conditionalElseSeen.isEmpty()) this.invalidState = "unbalanced conditional directive";
                else this.conditionalElseSeen.pop();
            }
            case "include", "moj_import", "pragma" -> invalidateDefinitions("uninspected #" + operation);
            case "version", "extension", "line" -> {

            }
            default -> this.invalidState = "unsupported preprocessor directive #" + operation;
        }
    }

    int resolve(String expression) {
        if (this.invalidState != null) throw unsupported(this.invalidState);
        if (this.currentDirectiveContinuation || this.continuedDirective) {
            throw unsupported("array declaration in a continued preprocessor directive");
        }
        if (!this.conditionalElseSeen.isEmpty()) {
            throw unsupported("conditional array declaration");
        }

        List<String> expanded = new ArrayList<>();
        expand(tokenize(expression), new HashSet<>(), expanded);
        final int extent;
        try {
            extent = new Expression(expanded).parse();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Invalid legacy uniform array extent arithmetic: " + expression, exception);
        }
        if (extent <= 0 || extent > MAX_EXTENT) {
            throw unsupported("extent must be positive and at most " + MAX_EXTENT + ": " + expression);
        }
        return extent;
    }

    private void define(String arguments) {
        int end = identifierEnd(arguments, 0);
        if (end == 0) {
            this.invalidState = "unrecognized #define";
            return;
        }
        String name = arguments.substring(0, end);
        if (!this.conditionalElseSeen.isEmpty()) {
            this.macros.put(name, Macro.unavailable("conditional definition of " + name));
        } else if (this.continuedDirective) {
            this.macros.put(name, Macro.unavailable("continued definition of " + name));
        } else if (end < arguments.length() && arguments.charAt(end) == '(') {
            this.macros.put(name, new Macro(MacroKind.FUNCTION, null, null));
        } else {
            this.macros.put(name, new Macro(MacroKind.OBJECT, arguments.substring(end).stripLeading(), null));
        }
    }

    private void undefine(String arguments) {
        int end = identifierEnd(arguments, 0);
        if (end == 0 || !arguments.substring(end).isBlank()) {
            this.invalidState = "unsupported #undef";
            return;
        }
        String name = arguments.substring(0, end);
        if (!this.conditionalElseSeen.isEmpty()) {
            this.macros.put(name, Macro.unavailable("conditional undefinition of " + name));
        } else {
            this.macros.remove(name);
        }
    }

    private void invalidateDefinitions(String reason) {
        this.macros.replaceAll((name, macro) -> Macro.unavailable(reason + " may change " + name));
    }

    private void expand(List<String> tokens, Set<String> active, List<String> result) {
        for (String token : tokens) {
            if (identifierStart(token.charAt(0))) {
                Macro macro = this.macros.get(token);
                if (macro == null) throw unsupported("undefined integer macro " + token);
                if (macro.kind == MacroKind.FUNCTION) throw unsupported("function-like macro " + token);
                if (macro.kind == MacroKind.UNAVAILABLE) throw unsupported(macro.reason);
                if (!active.add(token)) throw unsupported("cyclic integer macro " + token);
                if (active.size() > MAX_MACRO_DEPTH) throw unsupported("integer macro expansion depth exceeded");
                try {

                    expand(tokenize(macro.replacement), active, result);
                } finally {
                    active.remove(token);
                }
            } else {
                if (result.size() >= MAX_EXPANDED_TOKENS) throw unsupported("integer macro token budget exceeded");
                result.add(token);
            }
        }
    }

    private static List<String> tokenize(String expression) {
        if (expression == null || expression.isBlank()) throw unsupported("empty array extent or macro replacement");
        if (expression.length() > MAX_EXPRESSION_CHARACTERS) throw unsupported("integer expression length exceeded");
        List<String> tokens = new ArrayList<>();
        for (int index = 0; index < expression.length();) {
            char current = expression.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            int end = index + 1;
            if (identifierStart(current)) {
                end = identifierEnd(expression, index);
            } else if (digit(current)) {
                while (end < expression.length() && (identifierPart(expression.charAt(end)) || expression.charAt(end) == '.')) end++;
            } else if ("+-*/%()".indexOf(current) >= 0) {
                if (end < expression.length() && (expression.charAt(end) == '='
                        || (current == '+' || current == '-') && expression.charAt(end) == current)) {
                    throw unsupported("integer operator " + expression.substring(index, end + 1));
                }
            } else {
                throw unsupported("integer expression token " + current);
            }
            if (tokens.size() >= MAX_EXPANDED_TOKENS) throw unsupported("integer expression token budget exceeded");
            tokens.add(expression.substring(index, end));
            index = end;
        }
        return tokens;
    }

    private static int integerLiteral(String token) {
        int radix = 10;
        String digits = token;
        if (token.startsWith("0x") || token.startsWith("0X")) {
            radix = 16;
            digits = token.substring(2);
        } else if (token.length() > 1 && token.charAt(0) == '0') {
            radix = 8;
        }
        if (digits.isEmpty()) throw unsupported("signed integer literal " + token);
        for (int index = 0; index < digits.length(); index++) {
            char current = digits.charAt(index);
            boolean valid = digit(current) && current - '0' < radix
                    || radix == 16 && (current >= 'a' && current <= 'f' || current >= 'A' && current <= 'F');
            if (!valid) throw unsupported("signed integer literal " + token);
        }
        try {
            return Integer.parseInt(digits, radix);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Unsupported signed integer literal in legacy uniform array extent: " + token, exception);
        }
    }

    private static int identifierEnd(String text, int start) {
        if (start >= text.length() || !identifierStart(text.charAt(start))) return start;
        int end = start + 1;
        while (end < text.length() && identifierPart(text.charAt(end))) end++;
        return end;
    }

    private static boolean identifierStart(char current) {
        return current == '_' || current >= 'a' && current <= 'z' || current >= 'A' && current <= 'Z';
    }

    private static boolean identifierPart(char current) {
        return identifierStart(current) || digit(current);
    }

    private static boolean digit(char current) {
        return current >= '0' && current <= '9';
    }

    private static IllegalArgumentException unsupported(String detail) {
        return new IllegalArgumentException("Unsupported legacy uniform array extent: " + detail);
    }

    private enum MacroKind { OBJECT, FUNCTION, UNAVAILABLE }

    private record Macro(MacroKind kind, String replacement, String reason) {
        private static Macro unavailable(String reason) {
            return new Macro(MacroKind.UNAVAILABLE, null, reason);
        }
    }

    private static final class Expression {
        private final List<String> tokens;
        private int index;
        private int depth;

        private Expression(List<String> tokens) {
            this.tokens = tokens;
        }

        private int parse() {
            int result = additive();
            if (this.index != this.tokens.size()) throw unsupported("unexpected integer expression token " + this.tokens.get(this.index));
            return result;
        }

        private int additive() {
            int result = multiplicative();
            while (this.index < this.tokens.size()) {
                if (take("+")) result = Math.addExact(result, multiplicative());
                else if (take("-")) result = Math.subtractExact(result, multiplicative());
                else break;
            }
            return result;
        }

        private int multiplicative() {
            int result = unary();
            while (this.index < this.tokens.size()) {
                if (take("*")) result = Math.multiplyExact(result, unary());
                else if (take("/")) {
                    int divisor = unary();
                    if (result == Integer.MIN_VALUE && divisor == -1) throw new ArithmeticException("signed division overflow");
                    result /= divisor;
                } else if (take("%")) {
                    int divisor = unary();
                    if (result == Integer.MIN_VALUE && divisor == -1) throw new ArithmeticException("signed remainder overflow");
                    result %= divisor;
                }
                else break;
            }
            return result;
        }

        private int unary() {
            if (++this.depth > MAX_EXPRESSION_DEPTH) throw unsupported("integer expression nesting exceeded");
            try {
                if (take("+")) return unary();
                if (take("-")) return Math.negateExact(unary());
                if (take("(")) {
                    int result = additive();
                    if (!take(")")) throw unsupported("unbalanced integer expression parentheses");
                    return result;
                }
                if (this.index >= this.tokens.size()) throw unsupported("missing integer expression operand");
                return integerLiteral(this.tokens.get(this.index++));
            } finally {
                this.depth--;
            }
        }

        private boolean take(String token) {
            if (this.index < this.tokens.size() && this.tokens.get(this.index).equals(token)) {
                this.index++;
                return true;
            }
            return false;
        }
    }
}
