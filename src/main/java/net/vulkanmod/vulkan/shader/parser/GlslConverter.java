package net.vulkanmod.vulkan.shader.parser;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.*;

public class GlslConverter {

    ShaderStage shaderStage;
    private State state;

    private UniformParser uniformParser;
    private InputOutputParser inOutParser;

    private String vshConverted;
    private String fshConverted;

    private VertexFormat format;
    private Set<String> reservedUniformNames = Set.of();

    public void setFormat(VertexFormat format) {
        this.format = format;
    }

                                                                                                 
    public void reserveUniformNames(Collection<String> names) {
        this.reservedUniformNames = Set.copyOf(names);
    }

    public void process(String vertShader, String fragShader) {
        this.uniformParser = new UniformParser(this);
        this.inOutParser = new InputOutputParser(this);
        this.inOutParser.setFormat(this.format);

        StringBuilder vshOut = new StringBuilder();
        StringBuilder fshOut = new StringBuilder();

                                                                                   
        vertShader = withoutComments(vertShader);
        fragShader = withoutComments(fragShader);
        vertShader = prepareLegacyVertexShader(vertShader, fragShader);

        this.setShaderStage(ShaderStage.Vertex);

        String[] lines = vertShader.split("\n");

        var iterator = Arrays.stream(lines).iterator();

        while (iterator.hasNext()) {
            String line = iterator.next();

            String parsedLine = this.parseLine(line);
            if (parsedLine != null) {
                vshOut.append(parsedLine);
                vshOut.append("\n");
            }

        }

        vshOut = new StringBuilder(this.inOutParser.lowerUnsignedShortInputs(vshOut.toString(), vertShader));
        vshOut.insert(0, this.inOutParser.createInOutCode());

        this.setShaderStage(ShaderStage.Fragment);

        lines = fragShader.split("\n");

        iterator = Arrays.stream(lines).iterator();

        while (iterator.hasNext()) {
            String line = iterator.next();

            String parsedLine = this.parseLine(line);
            if (parsedLine != null) {
                fshOut.append(parsedLine);
                fshOut.append("\n");
            }
        }

        LegacyFragmentCoordinates.Lowered coordinates = LegacyFragmentCoordinates.lower(
                fshOut.toString(), vertShader + fragShader + vshOut + String.join(" ", this.reservedUniformNames));
        fshOut = new StringBuilder(coordinates.code());
        if (coordinates.heightName() != null) {
            parseUniformLine("uniform float " + coordinates.heightName() + ";");
        }
        fshOut.insert(0, this.inOutParser.createInOutCode());

        String uniformBlock = this.uniformParser.createUniformsCode();
        coordinates.bindHeight(this.uniformParser.getUbo());
        vshOut.insert(0, uniformBlock);
        fshOut.insert(0, uniformBlock);

        String samplersVertCode = this.uniformParser.createSamplersCode(ShaderStage.Vertex);
        String samplersFragCode = this.uniformParser.createSamplersCode(ShaderStage.Fragment);

        vshOut.insert(0, samplersVertCode);
        fshOut.insert(0, samplersFragCode);

        vshOut.insert(0, "#version 450\n\n");
        fshOut.insert(0, "#version 450\n\n");

        this.vshConverted = vshOut.toString();
        this.fshConverted = fshOut.toString();

    }

    private String parseLine(String line) {

        StringTokenizer tokenizer = new StringTokenizer(line);

        if (!tokenizer.hasMoreTokens())
            return "\n";

        String token = tokenizer.nextToken();

        switch (token) {
            case "uniform" -> {
                return parseUniformLine(line);
            }
            case "in", "out" -> this.state = State.MATCHING_IN_OUT;
            case "attribute" -> {
                this.state = State.MATCHING_IN_OUT;
                token = "in";
            }
            case "varying" -> {
                this.state = State.MATCHING_IN_OUT;
                token = this.shaderStage == ShaderStage.Vertex ? "out" : "in";
            }
            case "#version" -> {
                return null;
            }
            case "#moj_import" -> {
                if (tokenizer.countTokens() != 1) {
                    throw new IllegalArgumentException("Token count != 1");
                }

                String importPath = tokenizer.nextToken();
                if (importPath.startsWith("<") && importPath.endsWith(">")) {
                    importPath = "\"" + importPath.substring(1, importPath.length() - 1) + "\"";
                }

                return String.format("#include %s", importPath);
            }

            default -> {
                return CodeParser.parseCodeLine(line, this.shaderStage);
            }
        }

        if (tokenizer.countTokens() < 2) {
            throw new IllegalArgumentException("Less than 3 tokens present");
        }

        feedToken(token);

        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();

            feedToken(token);
        }

        return null;
    }

    private String parseUniformLine(String line) {
        int end=line.indexOf(';');
        if(end<0) throw new IllegalArgumentException("Unterminated legacy uniform declaration");
        String declaration=line.substring(0,end);
        int initializer=declaration.indexOf('=');
        if(initializer>=0) declaration=declaration.substring(0,initializer);
        StringTokenizer tokens=new StringTokenizer(declaration.stripTrailing()+";");
        if(tokens.countTokens()!=3)
            throw new IllegalArgumentException("Unsupported legacy uniform declaration: "+declaration.trim());
        while(tokens.hasMoreTokens()) this.uniformParser.parseToken(tokens.nextToken());
                                                                                     
        String tail=line.substring(end+1);
        return tail.isBlank()?null:parseLine(tail);
    }

    private static String withoutComments(String source) {
        StringBuilder result=new StringBuilder(source.length());
        boolean lineComment=false,blockComment=false,escaped=false;
        char quote=0;
        for(int i=0;i<source.length();i++) {
            char c=source.charAt(i),next=i+1<source.length()?source.charAt(i+1):0;
            if(lineComment) {
                if(c=='\n') {
                    boolean continued=i>0 && source.charAt(i-1)=='\\'
                            || i>1 && source.charAt(i-1)=='\r' && source.charAt(i-2)=='\\';
                    lineComment=continued; result.append(c);
                } else result.append(c=='\r'?c:' ');
            } else if(blockComment) {
                if(c=='*' && next=='/') { result.append("  "); i++; blockComment=false; }
                else result.append(c=='\n' || c=='\r'?c:' ');
            } else if(quote!=0) {
                result.append(c);
                if(escaped) escaped=false;
                else if(c=='\\') escaped=true;
                else if(c==quote) quote=0;
            } else if(c=='"' || c=='\'') { quote=c; result.append(c); }
            else if(c=='/' && next=='/') { result.append("  "); i++; lineComment=true; }
            else if(c=='/' && next=='*') { result.append("  "); i++; blockComment=true; }
            else result.append(c);
        }
        if(blockComment) throw new IllegalArgumentException("Unterminated shader block comment");
        return result.toString();
    }

    private void feedToken(String token) {
        switch (this.state) {
            case MATCHING_UNIFORM -> this.uniformParser.parseToken(token);
            case MATCHING_IN_OUT -> this.inOutParser.parseToken(token);
        }
    }

    private void setShaderStage(ShaderStage shaderStage) {
        this.shaderStage = shaderStage;
        this.uniformParser.setCurrentUniforms(this.shaderStage);
        this.inOutParser.setShaderStage(this.shaderStage);
    }

    private static String prepareLegacyVertexShader(String vertShader, String fragShader) {
        StringBuilder prefix = new StringBuilder();

        if (vertShader.contains("gl_ModelViewProjectionMatrix") && !vertShader.contains("uniform mat4 MVP")) {
            prefix.append("uniform mat4 MVP;\n");
        }

        if (vertShader.contains("gl_Vertex") && !hasDeclaration(vertShader, "Position")) {
            prefix.append("in vec3 Position;\n");
        }

        if (vertShader.contains("gl_MultiTexCoord0") && !hasDeclaration(vertShader, "UV0")) {
            prefix.append("in vec2 UV0;\n");
        }

        if ((vertShader.contains("gl_Color") || vertShader.contains("gl_FrontColor") || fragShader.contains("gl_Color"))
                && !hasDeclaration(vertShader, "Color")) {
            prefix.append("in vec4 Color;\n");
            prefix.append("out vec4 vertexColor;\n");
        }

        if (prefix.isEmpty()) {
            return vertShader;
        }

        return prefix.append(vertShader).toString();
    }

    private static boolean hasDeclaration(String shader, String name) {
        return shader.matches("(?s).*\\b(in|attribute|out|varying)\\s+\\w+\\s+" + java.util.regex.Pattern.quote(name) + "\\s*;.*");
    }

    public UBO getUBO() {
        return this.uniformParser.getUbo();
    }

    public List<ImageDescriptor> getSamplerList() {
        return this.uniformParser.getSamplers();
    }

    public String getVshConverted() {
        return vshConverted;
    }

    public String getFshConverted() {
        return fshConverted;
    }

    enum ShaderStage {
        Vertex,
        Fragment
    }

    enum State {
        MATCHING_UNIFORM,
        MATCHING_IN_OUT
    }
}
