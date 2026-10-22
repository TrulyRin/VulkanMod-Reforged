package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import net.vulkanmod.vulkan.shader.parser.CanonicalSamplerOrigins;


public final class RawShaderBytecode {
    private final byte[] vertex, fragment;
    private final CanonicalSamplerOrigins.Bindings origins;

    private RawShaderBytecode(byte[] vertex, byte[] fragment) { this(vertex, fragment, null); }

    private RawShaderBytecode(byte[] vertex, byte[] fragment, CanonicalSamplerOrigins.Bindings origins) {
        this.vertex = vertex; this.fragment = fragment; this.origins = origins;
    }

    static RawShaderBytecode capture(SPIRVUtils.SPIRV vertex, SPIRVUtils.SPIRV fragment) {
        return new RawShaderBytecode(copy(vertex), copy(fragment));
    }

    private static byte[] copy(SPIRVUtils.SPIRV source) {
        ByteBuffer bytes = Objects.requireNonNull(source, "Compiled shader is missing").bytecode().duplicate();
        byte[] result = new byte[bytes.remaining()]; bytes.get(result); return result;
    }


    public RawShaderBytecode withSamplerOrigins(Pipeline.Builder builder, Collection<String> reservedNames) {
        if (builder.imageDescriptors.isEmpty()) return this;
        String path = "/assets/vulkanmod/shaders/" + builder.shaderPath;
        var program = CanonicalSamplerOrigins.lower(builder.shaderPath, read(path + ".vsh"), read(path + ".fsh"),
                builder.imageDescriptors, builder.nextBinding, reservedNames);
        if (program.bindings().empty()) return this;
        try (var v = SPIRVUtils.compileShader(builder.shaderPath + "-raw.vsh", program.vertex(), SPIRVUtils.ShaderKind.VERTEX_SHADER);
             var f = SPIRVUtils.compileShader(builder.shaderPath + "-raw.fsh", program.fragment(), SPIRVUtils.ShaderKind.FRAGMENT_SHADER)) {
            return new RawShaderBytecode(copy(v), copy(f), program.bindings());
        }
    }

    private static String read(String path) {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        InputStream selected = context == null ? null : context.getResourceAsStream(path.substring(1));
        try (InputStream source = selected != null ? selected : Pipeline.class.getResourceAsStream(path)) {
            if (source == null) throw new IllegalStateException("Canonical raw shader is unavailable: " + path);
            return new String(source.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) { throw new IllegalStateException("Unable to read canonical raw shader " + path, failure); }
    }

    public GraphicsPipeline create(Pipeline.Builder builder) {
        if (this.origins != null) {
            this.origins.append(builder.UBOs, builder.imageDescriptors);
            builder.nextBinding++;
        }
        try (var v = nativeCopy(vertex); var f = nativeCopy(fragment)) {
            builder.setSPIRVs(v, f);
            return builder.createGraphicsPipeline();
        }
    }

    private static SPIRVUtils.SPIRV nativeCopy(byte[] bytes) {
        ByteBuffer nativeBytes = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        return SPIRVUtils.SPIRV.ownedBuffer(nativeBytes, () -> MemoryUtil.memFree(nativeBytes));
    }
}
