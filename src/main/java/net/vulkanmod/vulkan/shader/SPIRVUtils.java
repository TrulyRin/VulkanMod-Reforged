package net.vulkanmod.vulkan.shader;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.compat.observer.CompatProfiler;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.ShadercIncludeResolveI;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultReleaseI;
import org.lwjgl.vulkan.VK12;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.system.MemoryUtil.nmemFree;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class SPIRVUtils {
    private static final boolean DEBUG = true;
    private static final boolean OPTIMIZATIONS = false;

    private static long compiler;
    private static long options;

                                                                                                       
    private static final ShaderIncluder SHADER_INCLUDER = new ShaderIncluder();
    private static final ShaderReleaser SHADER_RELEASER = new ShaderReleaser();
    private static final long pUserData = 0;

    private static ObjectArrayList<String> includePaths;

    static {
        initCompiler();
    }

    private static void initCompiler() {
        compiler = shaderc_compiler_initialize();

        if (compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        options = shaderc_compile_options_initialize();

        if (options == NULL) {
            throw new RuntimeException("Failed to create compiler options");
        }

        if (OPTIMIZATIONS)
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

        if (DEBUG)
            shaderc_compile_options_set_generate_debug_info(options);

        shaderc_compile_options_set_target_env(options, shaderc_env_version_vulkan_1_2, VK12.VK_API_VERSION_1_2);
        shaderc_compile_options_set_include_callbacks(options, SHADER_INCLUDER, SHADER_RELEASER, pUserData);

        includePaths = new ObjectArrayList<>();
        addIncludePath("/assets/vulkanmod/shaders/include/");
    }

       
                                                                                              
                                                                                      
                                     
       
    public static void addIncludePath(String path) {
        includePaths.add(path);
    }

    public static SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {
        if (source == null) {
            throw new NullPointerException("Source for %s.%s is null".formatted(filename, shaderKind));
        }

        long compileStartedAt = System.nanoTime();
        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

        if (result == NULL) {
            throw new RuntimeException("Failed to compile shader %s into SPRI-V".formatted(filename ));
        }

        if (shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            String errorMessage;
            try {
                errorMessage = shaderc_result_get_error_message(result);
            } finally {
                shaderc_result_release(result);
            }
            throw new RuntimeException("Failed to compile shader %s into SPIR-V:\n\t%s".formatted(filename, errorMessage));
        }

        if (CompatProfiler.ENABLED) {
            CompatProfiler.shaderCompileCount++;
            CompatProfiler.spirvCompileTimeNanos += System.nanoTime() - compileStartedAt;
        }
        return new SPIRV(result, shaderc_result_get_bytes(result));
    }

    public enum ShaderKind {
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader),
        COMPUTE_SHADER(shaderc_glsl_compute_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }

    private static class ShaderIncluder implements ShadercIncludeResolveI {

        @Override
        public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
            var requesting = memASCII(requesting_source);
            var requested = memASCII(requested_source);

            for (String includePath : includePaths) {
                byte[] bytes = read("%s%s".formatted(includePath, requested));

                if (bytes != null) {
                    return result(requested, bytes, user_data);
                }
            }

                                                                                           
                                                                                             
                                                                                          
            return result("", "%s: Unable to find %s in include paths".formatted(requesting, requested), user_data);
        }

        private static byte[] read(String resourcePath) {
            try (InputStream stream = ShaderLoadUtil.getInputStream(resourcePath)) {
                return stream != null ? stream.readAllBytes() : null;
            } catch (Throwable t) {
                return null;
            }
        }

        private static long result(String sourceName, String content, long userData) {
            return result(sourceName, content.getBytes(StandardCharsets.UTF_8), userData);
        }

           
                                                                                    
                                                                              
           
        private static long result(String sourceName, byte[] content, long userData) {
            return ShadercIncludeResult.malloc()
                                       .source_name(copy(sourceName.getBytes(StandardCharsets.UTF_8)))
                                       .content(copy(content))
                                       .user_data(userData).address();
        }

        private static ByteBuffer copy(byte[] bytes) {
            ByteBuffer buffer = memAlloc(Math.max(bytes.length, 1));
            return buffer.put(bytes).flip();
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
            nmemFree(include_result);
        }
    }

    public static final class SPIRV implements NativeResource {

        private long handle;
        private ByteBuffer bytecode;

        public SPIRV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public synchronized void free() {
            long releasedHandle = handle;
            handle = NULL;
            bytecode = null;
            if (releasedHandle != NULL) shaderc_result_release(releasedHandle);
        }
    }

}
