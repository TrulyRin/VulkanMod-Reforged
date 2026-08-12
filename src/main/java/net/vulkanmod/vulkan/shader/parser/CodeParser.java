package net.vulkanmod.vulkan.shader.parser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class CodeParser {

    /* TODO: this is not a proper parser, it just serves the purpose of converting glsl shaders
        to solve some simple and common compatibility issues.
        Implementing an AST would be a better solution.
     */
    public static String parseCodeLine(String line) {
        return parseCodeLine(line, null);
    }

    public static String parseCodeLine(String line, GlslConverter.ShaderStage shaderStage) {
        line = rewriteLegacyFixedFunction(line, shaderStage);

        LinkedList<String> tokens = new LinkedList<>();
        StringTokenizer tokenizer = new StringTokenizer(line, " \t\n\r\f,(){}%", true);

        String delims = " \t\n\r\f";

        String token;
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();

            if (!delims.contains(token))
                tokens.add(token);
        }

        List<String> processed = new ObjectArrayList<>();
        boolean changed = false;

        int i = 0;
        while (i < tokens.size()) {
            token = tokens.get(i);

            if (token.equals("%")) {
                processed.removeLast();

                String prevToken = tokens.get(i - 1);
                String nextToken = tokens.get(i + 1);

                prevToken = checkTokenMapping(prevToken);
                nextToken = checkTokenMapping(nextToken);

                String newToken = "mod(%s, %s)".formatted(prevToken, nextToken);
                processed.add(newToken);

                changed = true;

                i += 2;
                continue;
            }

            String remappedToken = checkTokenMapping(token);

            if (!remappedToken.equals(token))
                changed = true;

            processed.add(remappedToken + " ");
            i++;
        }

        if (changed) {
            StringBuilder stringBuilder = new StringBuilder();

            for (String s : processed) {
                stringBuilder.append(s);
            }

            return stringBuilder.toString();
        } else {
            return line;
        }
    }

    private static String checkTokenMapping(String token) {
        return switch (token) {
            case "gl_VertexID" -> "gl_VertexIndex";
            case "gl_FragColor", "uc_FragColor" -> "fragColor";
            case "texture2D", "textureCube" -> "texture";
            default -> token;
        };
    }

    private static String rewriteLegacyFixedFunction(String line, GlslConverter.ShaderStage shaderStage) {
        line = line.replaceAll("\\bgl_ModelViewProjectionMatrix\\b", "MVP");
        line = line.replaceAll("\\bgl_MultiTexCoord0\\.st\\b", "UV0.xy");
        line = line.replaceAll("\\bgl_MultiTexCoord0\\.xy\\b", "UV0.xy");
        line = line.replaceAll("\\bgl_MultiTexCoord0\\b", "vec4(UV0, 0.0, 1.0)");
        line = line.replaceAll("\\bgl_Vertex\\.xy\\b", "Position.xy");
        line = line.replaceAll("\\bgl_Vertex\\b", "vec4(Position, 1.0)");
        line = line.replaceAll("\\bgl_FrontColor\\b", "vertexColor");

        if (shaderStage == GlslConverter.ShaderStage.Vertex) {
            line = line.replaceAll("\\bgl_Color\\b", "Color");
        } else {
            line = line.replaceAll("\\bgl_Color\\b", "vertexColor");
        }

        return line;
    }
}
