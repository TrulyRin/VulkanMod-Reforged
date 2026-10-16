package net.vulkanmod.compat.sable.veil;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.vulkan.framebuffer.MainDepthSnapshot;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;
import net.vulkanmod.vulkan.shader.SPIRVUtils.SPIRV;
import net.vulkanmod.vulkan.texture.VTextureSelector;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

                                                                                           
public final class VeilProgramPipelines {
    private static Bridge active;

    private VeilProgramPipelines() {}

    public static GraphicsPipeline getPipeline(Object shader, VertexFormat actualFormat) {
        if (!VeilShaderProgramAccess.isWrapper(shader)) return null;
        RenderSystem.assertOnRenderThread();
        int owner = VeilShaderProgramAccess.programId(shader);
        var linked = GlProgram.linkedProgram(owner);
        if (linked == null || GlProgram.convertedProgram(owner) == null) return null;
        if (actualFormat == null) {
            Resource previous = active == null ? null : active.last.get(owner);
            if (previous == null || previous.inputs.generation() != linked.generation()) return null;
            actualFormat = previous.inputs.format();
        }
        if (active == null) active = new Bridge();
        var entry = active.cache.resolve(owner, linked.generation(), actualFormat);
        if (entry.shouldReportFailure()) {
            Initializer.LOGGER.error("Unable to build Veil program {} generation {} for uploaded format {}",
                    owner, linked.generation(), actualFormat, entry.failure());
        }
        if (entry.resource() != null) active.last.put(owner, entry.resource());
        return entry.resource() == null ? null : entry.resource().pipeline;
    }

                                                                                                                     
    public static boolean prepareForDraw(Object shader, GraphicsPipeline pipeline) {
        if (!VeilShaderProgramAccess.isWrapper(shader)) return true;
        RenderSystem.assertOnRenderThread();
        Resource resource = active == null ? null : active.resources.get(pipeline);
        if (resource == null || resource.inputs.owner() != VeilShaderProgramAccess.programId(shader)
                || !resource.inputs.isCurrent()) return false;
        try {
            for (var sampler : resource.inputs.samplers()) sampler.refreshForDraw();
            if (!resource.inputs.isCurrent()) return false;
            resource.resourceFailureReported = false;
            return true;
        } catch (RuntimeException unavailable) {
            if (!resource.resourceFailureReported) {
                resource.resourceFailureReported = true;
                Initializer.LOGGER.error("Veil program {} generation {} has unavailable draw resources",
                        resource.inputs.owner(), resource.inputs.generation(), unavailable);
            }
            return false;
        }
    }

                                                                                           
    public static void cleanUp() {
        Bridge previous = active;
        active = null;
        if (previous != null) previous.close();
    }

    private static long generation(int owner) {
        var linked = GlProgram.linkedProgram(owner);
        return linked == null ? 0 : linked.generation();
    }

    private static final class Resource {
        final VeilProgramInputs inputs;
        final GraphicsPipeline pipeline;
        boolean resourceFailureReported;
        Resource(VeilProgramInputs inputs, GraphicsPipeline pipeline) {
            this.inputs = inputs;
            this.pipeline = pipeline;
        }
    }

    private static final class Bridge implements AutoCloseable {
        final MainDepthSnapshot depth = new MainDepthSnapshot();
        final IdentityHashMap<GraphicsPipeline, Resource> resources = new IdentityHashMap<>();
        final Int2ReferenceOpenHashMap<Resource> last = new Int2ReferenceOpenHashMap<>();
        final ProgramPipelineCache<VertexFormat, Resource> cache;
        final AutoCloseable listener;
        boolean closing;

        Bridge() {
            var factory = new VeilProgramPipelineFactory<>(SPIRVUtils::compileShader,
                    new VeilProgramPipelineFactory.Backend<Resource>() {
                        @Override public Resource create(VeilProgramInputs inputs, SPIRV v, SPIRV c, SPIRV e, SPIRV f) {
                            Pipeline.Builder builder = new Pipeline.Builder(inputs.format(),
                                    "veil/program/" + inputs.owner() + "/" + inputs.generation());
                            builder.setUniforms(List.of(inputs.uniforms()), new ArrayList<>(inputs.samplers()));
                            builder.setSPIRVs(v, f);
                            builder.setTessellationSPIRVs(c, e, 4);
                            return new Resource(inputs, builder.createGraphicsPipeline());
                        }
                        @Override public void discard(Resource resource) { resource.pipeline.cleanUp(); }
                    }, (owner, format) -> VeilProgramInputs.capture(owner, format,
                            (unit, stages) -> depth.resolveForSampling(VTextureSelector.getImage(unit), stages)));
            cache = new ProgramPipelineCache<>(VeilProgramPipelines::generation, (owner, current, format) -> {
                Resource resource = factory.create(owner, current, format);
                resources.put(resource.pipeline, resource);
                return resource;
            }, this::retire);
            listener = GlProgram.addLifecycleListener(owner -> {
                if (RenderSystem.isOnRenderThread()) invalidate(owner);
                else RenderSystem.recordRenderCall(() -> invalidate(owner));
            });
        }

        private void invalidate(int owner) {
            if (!closing) cache.invalidate(owner);
        }

        private void retire(Resource resource) {
            resources.remove(resource.pipeline);
            if (last.get(resource.inputs.owner()) == resource) last.remove(resource.inputs.owner());
            if (closing) resource.pipeline.cleanUp();
            else resource.pipeline.scheduleCleanUp();
        }

        @Override public void close() {
            if (closing) return;
            closing = true;
            RuntimeException failure = null;
            try { listener.close(); }
            catch (Exception error) { failure = new IllegalStateException("Unable to detach Veil program listener", error); }
            try { cache.clear(); }
            catch (RuntimeException error) { if (failure == null) failure = error; else failure.addSuppressed(error); }
            try { depth.close(); }
            catch (RuntimeException error) { if (failure == null) failure = error; else failure.addSuppressed(error); }
            resources.clear(); last.clear();
            if (failure != null) throw failure;
        }
    }
}
