package net.vulkanmod.vulkan.shader;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineTessellationStateCreateInfo;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.vulkan.VK10.*;

                                                                                
public final class GraphicsShaderStages implements AutoCloseable {
    public record Source(int stage, ByteBuffer bytecode) {}

    public record TessellationState(int patchControlPoints) {
        public TessellationState {
            if (patchControlPoints < 0) throw new IllegalArgumentException("Negative patch size");
        }

        public static TessellationState of(boolean stages, int vertices, boolean enabled, int limit) {
            if (!stages) {
                if (vertices != 0) throw new IllegalArgumentException("Patch size requires tessellation stages");
                return new TessellationState(0);
            }
            if (!enabled) throw new IllegalStateException("The tessellation shader feature is not enabled");
            if (vertices <= 0 || vertices > limit) {
                throw new IllegalArgumentException("Patch size exceeds the device tessellation limit");
            }
            return new TessellationState(vertices);
        }

        public int topology(int ordinaryTopology) {
            return patchControlPoints == 0 ? ordinaryTopology : VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;
        }

        public VkPipelineTessellationStateCreateInfo write(MemoryStack stack) {
            return patchControlPoints == 0 ? null : VkPipelineTessellationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO)
                    .patchControlPoints(patchControlPoints);
        }
    }

    public interface Backend {
        long create(ByteBuffer bytecode);
        void destroy(long module);
    }

    private record Entry(int stage, long module) {}

    private final List<Entry> entries;
    private final Backend backend;
    private boolean closed;

    private GraphicsShaderStages(List<Entry> entries, Backend backend) {
        this.entries = List.copyOf(entries);
        this.backend = backend;
    }

    public static GraphicsShaderStages create(List<Source> sources, Backend backend) {
        if (sources == null || backend == null) {
            throw new IllegalArgumentException("Graphics shader sources and backend are required");
        }
        Source[] ordered = new Source[4];
        for (Source source : sources) {
            if (source == null || source.bytecode() == null) {
                throw new IllegalArgumentException("Every graphics stage requires source bytecode");
            }
            int index = switch (source.stage()) {
                case VK_SHADER_STAGE_VERTEX_BIT -> 0;
                case VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT -> 1;
                case VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT -> 2;
                case VK_SHADER_STAGE_FRAGMENT_BIT -> 3;
                default -> throw new IllegalArgumentException("Unsupported graphics shader stage: " + source.stage());
            };
            if (ordered[index] != null) {
                throw new IllegalArgumentException("Duplicate graphics shader stage: " + source.stage());
            }
            ordered[index] = source;
        }
        if (ordered[0] == null || ordered[3] == null) {
            throw new IllegalArgumentException("Graphics programs require vertex and fragment stages");
        }
        if ((ordered[1] == null) != (ordered[2] == null)) {
            throw new IllegalArgumentException("Tessellation control and evaluation stages must be paired");
        }

        var entries = new ArrayList<Entry>(4);
        try {
            for (Source source : ordered) {
                if (source == null) continue;
                long module = backend.create(source.bytecode());
                if (module == VK_NULL_HANDLE) {
                    throw new IllegalStateException("Graphics shader module creation returned a zero handle");
                }
                entries.add(new Entry(source.stage(), module));
            }
            return new GraphicsShaderStages(entries, backend);
        } catch (RuntimeException | Error failure) {
            destroy(entries, backend, failure);
            throw failure;
        }
    }

    public VkPipelineShaderStageCreateInfo.Buffer write(MemoryStack stack, ByteBuffer entryPoint) {
        if (closed) throw new IllegalStateException("Graphics shader modules are closed");
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(entryPoint, "entryPoint");
        var stages = VkPipelineShaderStageCreateInfo.calloc(entries.size(), stack);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            stages.get(i)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(entry.stage())
                    .module(entry.module())
                    .pName(entryPoint);
        }
        return stages;
    }

    public int stageCount() {
        return entries.size();
    }

    public boolean hasTessellation() {
        return entries.size() == 4;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        Throwable failure = destroy(entries, backend, null);
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
    }

    private static Throwable destroy(List<Entry> entries, Backend backend, Throwable failure) {
        for (int i = entries.size() - 1; i >= 0; i--) {
            try {
                backend.destroy(entries.get(i).module());
            } catch (RuntimeException | Error cleanupFailure) {
                if (failure == null) failure = cleanupFailure;
                else if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
            }
        }
        return failure;
    }
}
