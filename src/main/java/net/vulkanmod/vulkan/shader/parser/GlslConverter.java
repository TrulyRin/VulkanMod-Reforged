package net.vulkanmod.vulkan.shader.parser;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.descriptor.ImageDescriptor;
import net.vulkanmod.vulkan.shader.descriptor.UBO;

import java.util.*;
import java.util.regex.Pattern;

public class GlslConverter {

    ShaderStage shaderStage;
    private static final Pattern UNIFORM_DECLARATION = Pattern.compile(
            "\\s*uniform\\s+(?:(?:lowp|mediump|highp)\\s+)?([A-Za-z_]\\w*)\\s+(.+)", Pattern.DOTALL);
    private static final Pattern UNIFORM_DECLARATOR = Pattern.compile(
            "([A-Za-z_]\\w*)(?:\\s*\\[([^\\[\\]]*)\\])?(?:\\s*=\\s*(.+))?", Pattern.DOTALL);

    private UniformParser uniformParser;
    private InputOutputParser inOutParser;
    private LegacyArrayExtent arrayExtents;

    private String vshConverted;
    private String fshConverted;

    private VertexFormat format;
    private Set<String> reservedUniformNames = Set.of();
    private boolean effectSamplerOrigins;
    private LegacySamplerOrigins.Lowered samplerOrigins;

    public void setFormat(VertexFormat format) {
        this.format = format;
    }


    public void reserveUniformNames(Collection<String> names) {
        this.reservedUniformNames = Set.copyOf(names);
    }

    public void enableEffectSamplerOrigins() { this.effectSamplerOrigins = true; }


    public void bindEffectSamplerOrigins() {
        if (this.samplerOrigins != null) this.samplerOrigins.bind(getUBO(), getSamplerList());
    }

    public void process(String vertShader, String fragShader) {
        this.samplerOrigins = null;
        this.uniformParser = new UniformParser(this);
        this.inOutParser = new InputOutputParser(this);
        this.inOutParser.setFormat(this.format);

        StringBuilder vshOut = new StringBuilder();
        StringBuilder fshOut = new StringBuilder();


        vertShader = coalesceUniformDeclarations(withoutComments(vertShader));
        fragShader = coalesceUniformDeclarations(withoutComments(fragShader));
        vertShader = prepareLegacyVertexShader(vertShader, fragShader);

        this.setShaderStage(ShaderStage.Vertex);
        this.arrayExtents = new LegacyArrayExtent();

        String[] lines = vertShader.split("\n");

        var iterator = Arrays.stream(lines).iterator();

        while (iterator.hasNext()) {
            String line = iterator.next();
            this.arrayExtents.observeLine(line);

            String parsedLine = this.parseLine(line);
            if (parsedLine != null) {
                vshOut.append(parsedLine);
                vshOut.append("\n");
            }

        }

        vshOut = new StringBuilder(this.inOutParser.lowerUnsignedShortInputs(vshOut.toString(), vertShader));

        this.setShaderStage(ShaderStage.Fragment);
        this.arrayExtents = new LegacyArrayExtent();

        lines = fragShader.split("\n");

        iterator = Arrays.stream(lines).iterator();

        while (iterator.hasNext()) {
            String line = iterator.next();
            this.arrayExtents.observeLine(line);

            String parsedLine = this.parseLine(line);
            if (parsedLine != null) {
                fshOut.append(parsedLine);
                fshOut.append("\n");
            }
        }

        LegacyFragmentCoordinates.Lowered coordinates = LegacyFragmentCoordinates.lower(
                fshOut.toString(), vertShader + fragShader + vshOut + String.join(" ", this.reservedUniformNames));
        fshOut = new StringBuilder(coordinates.code());
        if (coordinates.fullyPreprocessed()) {
            this.inOutParser.acceptFullyPreprocessedFragmentBody(coordinates.code(), coordinates.importedIdentifiers());
        }
        if (coordinates.heightName() != null) {
            parseUniformLine("uniform float " + coordinates.heightName() + ";");
        }
        if (this.effectSamplerOrigins) {
            this.samplerOrigins = LegacySamplerOrigins.lower(vshOut.toString(), fshOut.toString(),
                    this.uniformParser.getDeclarations(), vertShader + '\n' + fragShader + '\n'
                            + String.join(" ", this.reservedUniformNames));
            vshOut = new StringBuilder(this.samplerOrigins.vertex());
            fshOut = new StringBuilder(this.samplerOrigins.fragment());
            for (var binding : this.samplerOrigins.bindings()) parseUniformLine("uniform int " + binding.uniform() + ";");
        }
        this.setShaderStage(ShaderStage.Vertex);
        vshOut.insert(0, this.inOutParser.createInOutCode());
        this.setShaderStage(ShaderStage.Fragment);
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

        if (!token.equals("uniform") && this.inOutParser.tryParseDeclaration(line)) return null;

        switch (token) {
            case "uniform" -> {
                return parseUniformLine(line);
            }
            case "#version" -> {
                return null;
            }
            case "#moj_import" -> {
                return line;
            }

            default -> {
                return CodeParser.parseCodeLine(line, this.shaderStage);
            }
        }

    }

    private String parseUniformLine(String line) {
        int end=line.indexOf(';');
        if(end<0) throw new IllegalArgumentException("Unterminated legacy uniform declaration");
        String declaration=line.substring(0,end);
        var header=UNIFORM_DECLARATION.matcher(declaration);
        if(!header.matches())
            throw new IllegalArgumentException("Unsupported legacy uniform declaration: "+declaration.trim());
        String type=header.group(1);
        for(String declarator:splitUniformDeclarators(header.group(2))) {
            var variable=UNIFORM_DECLARATOR.matcher(declarator.trim());
            if(!variable.matches())
                throw new IllegalArgumentException("Unsupported legacy uniform declarator: "+declarator.trim());
            int arrayLength=variable.group(2)==null?0:this.arrayExtents.resolve(variable.group(2));
            this.uniformParser.parseDeclaration(type,variable.group(1),variable.group(3)==null?null:variable.group(3).trim(),arrayLength);
        }

        String tail=line.substring(end+1);
        return tail.isBlank()?null:parseLine(tail);
    }

    private static List<String> splitUniformDeclarators(String declaration) {
        List<String> result=new ArrayList<>();
        int parentheses=0,brackets=0,start=0;
        for(int i=0;i<declaration.length();i++) {
            switch(declaration.charAt(i)) {
                case '(' -> parentheses++;
                case ')' -> parentheses--;
                case '[' -> brackets++;
                case ']' -> brackets--;
                case '{','}' -> throw new IllegalArgumentException("Unsupported legacy uniform block or array initializer");
                case ',' -> {
                    if(parentheses==0 && brackets==0) {
                        result.add(declaration.substring(start,i));start=i+1;
                    }
                }
            }
            if(parentheses<0 || brackets<0) throw new IllegalArgumentException("Unbalanced legacy uniform initializer");
        }
        if(parentheses!=0 || brackets!=0) throw new IllegalArgumentException("Unbalanced legacy uniform initializer");
        result.add(declaration.substring(start));
        return result;
    }

    private static String coalesceUniformDeclarations(String source) {
        StringBuilder result=new StringBuilder(source.length());
        StringBuilder pending=null;
        for(String line:source.split("\\n",-1)) {
            String trimmed=line.stripLeading();
            if(pending!=null) {
                if(trimmed.startsWith("#")) throw new IllegalArgumentException("Directive inside legacy uniform declaration");
                pending.append(' ').append(line);
                if(line.indexOf(';')>=0) {result.append(pending).append('\n');pending=null;}
            } else if((trimmed.equals("uniform") || trimmed.matches("uniform\\s.*")) && line.indexOf(';')<0) {
                pending=new StringBuilder(line);
            } else result.append(line).append('\n');
        }
        if(pending!=null) throw new IllegalArgumentException("Unterminated legacy uniform declaration");
        return result.toString();
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

    public Map<String, UniformParser.Declaration> getUniformDeclarations() {
        return this.uniformParser.getDeclarations();
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

}
