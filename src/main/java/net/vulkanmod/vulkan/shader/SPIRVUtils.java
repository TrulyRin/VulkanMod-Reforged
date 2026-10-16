package net.vulkanmod.vulkan.shader;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.ShadercIncludeResolveI;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultReleaseI;
import org.lwjgl.vulkan.VK12;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.memUTF8;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class SPIRVUtils {
    private static final boolean DEBUG = false;
    private static final boolean OPTIMIZATIONS = true;
    private static final int MAX_INCLUDE_DEPTH = 32;

    private static long compiler;
    private static long options;

    private static final ShaderIncluder SHADER_INCLUDER = new ShaderIncluder();
    private static final ShaderReleaser SHADER_RELEASER = new ShaderReleaser();
    private static final long pUserData = 0;

    private static ObjectArrayList<String> includePaths;
    private static final Pattern RESERVED_SAMPLER_IDENTIFIER = Pattern.compile("\\bsampler\\b");
    private static final Pattern INCLUDE_VERSION_DIRECTIVE = Pattern.compile("(?m)^[\\t ]*#[\\t ]*version\\b[^\\r\\n]*");

    private static float time = 0.0f;

    static {
        initCompiler();
    }

    private static void initCompiler() {
        compiler = shaderc_compiler_initialize();

        if(compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        options = shaderc_compile_options_initialize();

        if(options == NULL) {
            throw new RuntimeException("Failed to create compiler options");
        }

        if(OPTIMIZATIONS)
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

        if(DEBUG)
            shaderc_compile_options_set_generate_debug_info(options);

        shaderc_compile_options_set_target_env(options, shaderc_env_version_vulkan_1_2, VK12.VK_API_VERSION_1_2);
        shaderc_compile_options_set_include_callbacks(options, SHADER_INCLUDER, SHADER_RELEASER, pUserData);

        includePaths = new ObjectArrayList<>();
        addIncludePath("/assets/vulkanmod/shaders/include/");
        addIncludePath("/assets/minecraft/shaders/include/");
    }

    public static void addIncludePath(String path) {
        if(!path.endsWith("/")) path = path + "/";
        includePaths.add(path);
    }

    public static SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind) {
        try (InputStream is = openResource(shaderFile)) {
            if (is == null) throw new RuntimeException("Shader not found in classpath: " + shaderFile);
            String source = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

                                                                                                                     
    public static String preprocessShader(String filename, String source, ShaderKind shaderKind) {
        long result = shaderc_compile_into_preprocessed_text(compiler, source, shaderKind.kind, filename, "main", options);
        if (result == NULL) throw new RuntimeException("Failed to preprocess shader " + filename);
        try (var resultOwner = new SPIRV.ResultOwner(result,
                org.lwjgl.util.shaderc.Shaderc::shaderc_result_release)) {
            if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                throw new RuntimeException("Failed to preprocess shader " + filename + ":\n"
                        + shaderc_result_get_error_message(result));
            }
                                                                            
            String text = StandardCharsets.UTF_8.decode(Objects.requireNonNull(shaderc_result_get_bytes(result))).toString();
            return text.endsWith("\0") ? text.substring(0, text.length() - 1) : text;
        }
    }

    public static SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {
        long startTime = System.nanoTime();

        source = rewriteLegacyReservedIdentifiers(source);
        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

        if(result == NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }

        try (var resultOwner = new SPIRV.ResultOwner(result,
                org.lwjgl.util.shaderc.Shaderc::shaderc_result_release)) {
            if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
                                                                                   
                String message = shaderc_result_get_error_message(result);
                throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + message);
            }

            long elapsed = System.nanoTime() - startTime;
            time += elapsed / 1000000.0f;

            recordCompileForProfiler(elapsed);

            return resultOwner.transfer(shaderc_result_get_bytes(result));
        }
    }

    private static void recordCompileForProfiler(long elapsed) {
        try {
            if (net.vulkanmod.compat.observer.CompatProfiler.ENABLED) {
                net.vulkanmod.compat.observer.CompatProfiler.shaderCompileCount++;
                net.vulkanmod.compat.observer.CompatProfiler.spirvCompileTimeNanos += elapsed;
            }
        } catch (LinkageError ignored) {

        }
    }

    private static SPIRV readFromStream(InputStream inputStream) {
        ByteBuffer buffer = null;
        try {
            byte[] bytes = inputStream.readAllBytes();
            buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.position(0);

            ByteBuffer owned = buffer;
            SPIRV spirv = SPIRV.ownedBuffer(buffer, () -> memFree(owned));
            buffer = null;
            return spirv;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (buffer != null) memFree(buffer);
        }
        throw new RuntimeException("unable to read inputStream");
    }

    private static InputStream openResource(String resourcePath) {
        String classLoaderPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream stream = contextClassLoader.getResourceAsStream(classLoaderPath);
            if (stream != null) {
                return stream;
            }
        }
        return SPIRVUtils.class.getResourceAsStream(resourcePath);
    }

    private static InputStream openNamespacedShaderInclude(String requested) {
        String resourcePath = toNamespacedIncludePath(requested);
        if (resourcePath == null) {
            return null;
        }

        return openResource(resourcePath);
    }

    private static String toNamespacedIncludePath(String requested) {
        int namespaceSeparator = requested.indexOf(':');
        if (namespaceSeparator <= 0 || namespaceSeparator == requested.length() - 1) {
            return null;
        }

        String namespace = requested.substring(0, namespaceSeparator);
        String path = requested.substring(namespaceSeparator + 1);
        return "/assets/" + namespace + "/shaders/include/" + path;
    }

    private static InputStream openShaderInclude(String requested) {
        InputStream namespacedInclude = openNamespacedShaderInclude(requested);
        if (namespacedInclude != null) {
            return namespacedInclude;
        }

        for(String includePath : includePaths) {
            String fullPath = includePath + requested;
            InputStream include = openResource(fullPath);
            if(include != null) {
                return include;
            }
        }

        return null;
    }

    private static byte[] readIncludeBytes(InputStream inputStream) throws IOException {
        String source = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return rewriteLegacyReservedIdentifiers(withoutIncludeVersions(expandMojImports(source, 0)))
                .getBytes(StandardCharsets.UTF_8);
    }

                                                                                                         
    private static String withoutIncludeVersions(String source) {
        char[] code = source.toCharArray();
        boolean blockComment = false, lineComment = false;
        char quote = 0;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : 0;
            if (c == '\n' || c == '\r') {
                int previous = i - 1;
                if (c == '\n' && previous >= 0 && source.charAt(previous) == '\r') previous--;
                lineComment = lineComment && previous >= 0 && source.charAt(previous) == '\\';
                continue;
            }
            if (lineComment) { code[i] = ' '; continue; }
            if (blockComment) {
                code[i] = ' ';
                if (c == '*' && next == '/') { blockComment = false; code[++i] = ' '; }
                continue;
            }
            if (quote != 0) {
                code[i] = ' ';
                if (c == '\\' && next != 0 && next != '\n' && next != '\r') code[++i] = ' ';
                else if (c == quote) quote = 0;
                continue;
            }
            if (c == '/' && (next == '*' || next == '/')) {
                blockComment = next == '*'; lineComment = next == '/';
                code[i] = ' '; code[++i] = ' '; continue;
            }
            if (c == '"' || c == '\'') { quote = c; code[i] = ' '; }
        }
        StringBuilder result = new StringBuilder(source);
        var directives = INCLUDE_VERSION_DIRECTIVE.matcher(new String(code));
        while (directives.find()) {
            for (int i = directives.start(); i < directives.end(); i++) {
                if (code[i] != ' ' && code[i] != '\t') result.setCharAt(i, ' ');
            }
        }
        return result.toString();
    }

    static String rewriteLegacyReservedIdentifiers(String source) {
        return RESERVED_SAMPLER_IDENTIFIER.matcher(source).replaceAll("samplerTexture");
    }

    private static String expandMojImports(String source, int depth) throws IOException {
        if (depth > MAX_INCLUDE_DEPTH) {
            throw new IOException("Shader include depth exceeded " + MAX_INCLUDE_DEPTH);
        }

        String[] lines = source.split("\\R", -1);
        StringBuilder builder = new StringBuilder(source.length());

        for (int i = 0; i < lines.length; i++) {
            String importPath = parseMojImportPath(lines[i]);
            if (importPath != null) {
                builder.append(readShaderInclude(importPath, depth + 1));
            } else {
                builder.append(lines[i]);
            }

            if (i < lines.length - 1) {
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    private static String readShaderInclude(String requested, int depth) throws IOException {
        try (InputStream inputStream = openShaderInclude(requested)) {
            if (inputStream == null) {
                throw new IOException("Unable to find nested include " + requested);
            }

            String source = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return expandMojImports(source, depth);
        }
    }

    private static String parseMojImportPath(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        if (!tokenizer.hasMoreTokens()) {
            return null;
        }

        if (!"#moj_import".equals(tokenizer.nextToken())) {
            return null;
        }

        if (tokenizer.countTokens() != 1) {
            return null;
        }

        String importPath = tokenizer.nextToken();
        if (importPath.startsWith("<") && importPath.endsWith(">")) {
            return importPath.substring(1, importPath.length() - 1);
        }

        if (importPath.startsWith("\"") && importPath.endsWith("\"")) {
            return importPath.substring(1, importPath.length() - 1);
        }

        return importPath;
    }

    public enum ShaderKind {
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        TESSELLATION_CONTROL_SHADER(shaderc_glsl_tess_control_shader),
        TESSELLATION_EVALUATION_SHADER(shaderc_glsl_tess_evaluation_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader),
        COMPUTE_SHADER(shaderc_glsl_compute_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }

    static ByteBuffer encodeIncludeSourceName(String sourceName) {
        return memUTF8(sourceName);
    }

    private static class ShaderIncluder implements ShadercIncludeResolveI {
        @Override
        public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
            var requesting = memASCII(requesting_source);
            var requested = memASCII(requested_source);

            try {
                try (InputStream is = openShaderInclude(requested)) {
                    if(is != null) {
                        return createIncludeResult(requested, readIncludeBytes(is), user_data);
                    }
                }
            } catch (IOException e) {
                return createIncludeError(requesting, requested, e.getMessage(), user_data);
            }

            return createIncludeError(requesting, requested, "Unable to find include in registered include paths", user_data);
        }

        private static long createIncludeResult(String sourceName, byte[] content, long userData) {
            ByteBuffer sourceNameBuffer = encodeIncludeSourceName(sourceName);
            ByteBuffer contentBuffer = memAlloc(content.length);
            contentBuffer.put(content).flip();

            return ShadercIncludeResult.malloc()
                    .source_name(sourceNameBuffer)
                    .content(contentBuffer)
                    .user_data(userData)
                    .address();
        }

        private static long createIncludeError(String requesting, String requested, String reason, long userData) {
            String message = String.format("%s: Unable to include %s: %s", requesting, requested, reason);
            return createIncludeResult("", message.getBytes(StandardCharsets.UTF_8), userData);
        }
    }

    private static class ShaderReleaser implements ShadercIncludeResultReleaseI {

        @Override
        public void invoke(long user_data, long include_result) {
            if (include_result == NULL) {
                return;
            }

            ShadercIncludeResult result = ShadercIncludeResult.create(include_result);
            memFree(result.source_name());
            memFree(result.content());
            result.free();
        }
    }

    public static final class SPIRV implements NativeResource {

        private static final Runnable BORROWED_RELEASE = () -> {};
        private final long handle;
        private ByteBuffer bytecode;
        private Runnable release;
        private boolean freed;

                                                                                           
        public SPIRV(long handle, ByteBuffer bytecode) {
            this(handle, bytecode, BORROWED_RELEASE);
        }

        private SPIRV(long handle, ByteBuffer bytecode, Runnable release) {
            this.handle = handle;
            this.bytecode = bytecode;
            this.release = release;
        }

                                                                                  
        public static SPIRV borrowed(ByteBuffer bytecode) {
            return new SPIRV(0, Objects.requireNonNull(bytecode, "bytecode"));
        }

                                                                                    
        public static SPIRV compilerResult(long handle, ByteBuffer bytecode, LongConsumer releaseResult) {
            Objects.requireNonNull(bytecode, "bytecode");
            Objects.requireNonNull(releaseResult, "releaseResult");
            return new SPIRV(handle, bytecode, () -> releaseResult.accept(handle));
        }

                                                                                               
        public static SPIRV ownedBuffer(ByteBuffer bytecode, Runnable releaseBuffer) {
            return new SPIRV(0, Objects.requireNonNull(bytecode, "bytecode"),
                    Objects.requireNonNull(releaseBuffer, "releaseBuffer"));
        }

                                                                                      
        static final class ResultOwner implements AutoCloseable {
            private final long handle;
            private final LongConsumer releaseResult;
            private boolean transferred;
            private boolean closed;

            ResultOwner(long handle, LongConsumer releaseResult) {
                this.handle = handle;
                this.releaseResult = Objects.requireNonNull(releaseResult, "releaseResult");
            }

            synchronized SPIRV transfer(ByteBuffer bytecode) {
                if (closed || transferred) throw new IllegalStateException("Compiler result ownership is already consumed");
                SPIRV spirv = compilerResult(handle, bytecode, releaseResult);
                transferred = true;
                return spirv;
            }

            @Override
            public synchronized void close() {
                if (closed) return;
                closed = true;
                if (!transferred) releaseResult.accept(handle);
            }
        }

        public synchronized ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public synchronized void free() {
            if (freed) return;
            freed = true;
            bytecode = null;
            Runnable releaser = release;
            release = null;
            releaser.run();
        }
    }
}
