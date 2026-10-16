package net.vulkanmod.compat.sable.veil;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.vulkan.shader.SPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.shader.SPIRVUtils.ShaderKind;
import net.vulkanmod.vulkan.shader.parser.LevititeShaderConverter.Stage;

import java.util.Objects;
import java.util.function.BiFunction;

                                                                                            
final class VeilProgramPipelineFactory<P> {
    interface Compiler { SPIRV compile(String name, String source, ShaderKind kind); }
    interface Backend<P> {
        P create(VeilProgramInputs inputs, SPIRV vertex, SPIRV control, SPIRV evaluation, SPIRV fragment);
        void discard(P pipeline);
    }
    private final Compiler compiler;
    private final Backend<P> backend;
    private final BiFunction<Integer, VertexFormat, VeilProgramInputs> inputs;

    VeilProgramPipelineFactory(Compiler compiler, Backend<P> backend) {
        this(compiler, backend, VeilProgramInputs::capture);
    }

    VeilProgramPipelineFactory(Compiler compiler, Backend<P> backend,
                               BiFunction<Integer, VertexFormat, VeilProgramInputs> inputs) {
        this.compiler = Objects.requireNonNull(compiler);
        this.backend = Objects.requireNonNull(backend);
        this.inputs = Objects.requireNonNull(inputs);
    }

    P create(int owner, long generation, VertexFormat format) {
        VeilProgramInputs captured = inputs.apply(owner, format);
        if (captured.generation() != generation || !captured.isCurrent()) {
            throw new IllegalStateException("Program generation changed before compilation");
        }
        String name = "veil/program/" + owner + "/" + generation;
        var sources = captured.conversion().sources();
        P created = null;
        try (SPIRV vertex = compile(name + ".vsh", sources.get(Stage.VERTEX), ShaderKind.VERTEX_SHADER);
             SPIRV control = compile(name + ".tcs", sources.get(Stage.TESSELLATION_CONTROL), ShaderKind.TESSELLATION_CONTROL_SHADER);
             SPIRV evaluation = compile(name + ".tes", sources.get(Stage.TESSELLATION_EVALUATION), ShaderKind.TESSELLATION_EVALUATION_SHADER);
             SPIRV fragment = compile(name + ".fsh", sources.get(Stage.FRAGMENT), ShaderKind.FRAGMENT_SHADER)) {
            if (!captured.isCurrent()) throw new IllegalStateException("Program relinked during compilation");
            created = Objects.requireNonNull(backend.create(captured, vertex, control, evaluation, fragment),
                    "Native pipeline backend returned null");
        } catch (RuntimeException | Error failure) {
            if (created != null) {
                try { backend.discard(created); }
                catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
        return created;
    }

    private SPIRV compile(String name, String source, ShaderKind kind) {
        return Objects.requireNonNull(compiler.compile(name, source, kind), "Compiler returned no result");
    }
}
