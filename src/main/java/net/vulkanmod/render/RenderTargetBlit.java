package net.vulkanmod.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.gl.GlPatchState;
import net.vulkanmod.compat.sable.veil.VeilProgramDraws;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.gl.GlEmulationLog;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.vulkan.PreparedDraw;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.DrawUtil;

public final class RenderTargetBlit {
    private RenderTargetBlit() {}

    public static final class Call implements AutoCloseable {
        private ShaderMixed owner;
        private ShaderInstance shader;
        private int sourceId;
        private boolean drawn;
        private boolean closed;

        public int source(RenderTarget target) {
            RenderSystem.assertOnRenderThread();
            Renderer renderer = Renderer.getInstance();
            if (!Renderer.isRecording() || Renderer.skipRendering || renderer == null) {
                throw new IllegalStateException("Render-target blit requires active rendering");
            }
            var bindings = GlFramebuffer.captureBindings();
            var destinationPass = renderer.getBoundRenderPass();
            var destination = destinationPass != null ? destinationPass.getFramebuffer() : null;
            int width = renderer.getViewportLogicalWidth(), height = renderer.getViewportLogicalHeight();
            int x = Renderer.getViewportBox(0), y = Renderer.getViewportBox(1);
            int viewportWidth = Renderer.getViewportBox(2), viewportHeight = Renderer.getViewportBox(3);
            boolean viewportInitialized = Renderer.isViewportInitialized();
            long viewportRevision = Renderer.getViewportRevision();
            boolean glViewportSelection = Renderer.isGlViewportSelection();
            Throwable failure = null;
            int id;
            try {
                renderer.endRenderPass();
                bindings.restore();
                Renderer.setViewportScale(width, height);
                if (renderer.getBoundRenderPass() != null) throw new IllegalStateException("Blit source is still rendering");
                id = target.getColorTextureId();
                if (id <= 0) throw new IllegalStateException("Render-target blit source has no live texture");
            } catch (RuntimeException | Error error) {
                failure = error;
                throw error;
            } finally {
                try {
                    if (renderer.getBoundRenderPass() != destinationPass && renderer.getBoundRenderPass() != null) {
                        renderer.endRenderPass();
                    }
                    Renderer.setViewportScale(width, height);
                    if (viewportInitialized) Renderer.setViewport(x, y, viewportWidth, viewportHeight);
                    if (destinationPass != null && renderer.getBoundRenderPass() != destinationPass) {
                        var resumePass = renderer.getMainPass().resumeRenderPass(destinationPass);
                        if (resumePass == null || !renderer.beginRenderingPreservingRasterState(resumePass, destination)) {
                            throw new IllegalStateException("Unable to resume render-target blit destination");
                        }
                    }
                } catch (RuntimeException | Error cleanup) {
                    if (failure == null) throw cleanup;
                    if (failure != cleanup) failure.addSuppressed(cleanup);
                } finally {
                    bindings.restore();
                    Renderer.setViewportScale(width, height);
                    Renderer.restoreViewportRevision(viewportRevision, glViewportSelection);
                }
            }
            this.sourceId = id;
            return id;
        }

        public void apply(ShaderInstance shader, Runnable actualApply) {
            if (this.owner != null || this.closed) throw new IllegalStateException("Blit owner is already selected");
            if (!(shader instanceof ShaderMixed selected)) throw new IllegalStateException("Blit shader has no supported owner");
            this.owner = selected;
            this.shader = shader;
            actualApply.run();
        }

        public void draw(MeshData mesh) {
            try (mesh) {
                if (this.owner == null || this.drawn || this.closed) throw new IllegalStateException("Blit draw has no unique applied owner");
                this.drawn = true;
                var state = mesh.drawState();
                if (!DefaultVertexFormat.BLIT_SCREEN.equals(state.format()) || state.mode() != VertexFormat.Mode.QUADS
                        || state.vertexCount() != 4 || state.indexCount() != 6 || mesh.indexBuffer() != null) {
                    throw new IllegalArgumentException("Unsupported render-target blit geometry");
                }
                if (!state.format().equals(this.shader.getVertexFormat())) {
                    baseCopy("vertex_format");
                    return;
                }
                boolean programOwned = VeilShaderProgramAccess.isWrapper(this.shader);
                ShaderMixed.BlitContract contract = this.owner.blitContract();
                GraphicsPipeline pipeline;
                if (programOwned) {
                    if (GlProgram.currentProgramId() != VeilShaderProgramAccess.programId(this.shader)) {
                        baseCopy("program_owner_unbound");
                        return;
                    }

                    pipeline = VeilProgramDraws.prepare(this.shader, state.format(), false);
                } else {
                    if (contract == ShaderMixed.BlitContract.UNSUPPORTED) {
                        baseCopy("unavailable_shader");
                        return;
                    }
                    pipeline = this.owner.getPipeline();
                }
                if (pipeline == null) {
                    baseCopy("unavailable_pipeline");
                    return;
                }
                long units = 0L;
                VulkanImage diffuse = null;
                for (var descriptor : pipeline.getImageDescriptors()) {
                    int unit = descriptor.getTextureUnit();
                    if (descriptor.isStorageImage || unit < 0 || unit >= VTextureSelector.SIZE
                            || descriptor.usesVanillaTextureBinding() == programOwned) {
                        baseCopy("sampler_contract");
                        return;
                    }
                    units |= 1L << unit;
                    if ("DiffuseSampler".equals(descriptor.name)) {
                        if (programOwned) diffuse = descriptor.getImage();
                        else {
                            int id = this.owner.samplerTextureId(pipeline, descriptor.name);
                            diffuse = texture(id != -1 ? id : RenderSystem.getShaderTexture(unit));
                        }
                    }
                }
                if (!programOwned && contract == ShaderMixed.BlitContract.CANONICAL_COPY) {
                    if (diffuse == null) {
                        baseCopy("unavailable_diffuse");
                        return;
                    }
                    copy(diffuse);
                    return;
                }
                if (diffuse != null && !diffuse.isRowsBottomUp()) {

                    baseCopy("top_down_custom_shader");
                    return;
                }
                if (pipeline.hasTessellation() && GlPatchState.getPatchVertices() != 4) {
                    baseCopy("patch_vertices");
                    return;
                }
                try (NativeState ignored = new NativeState(units)) {
                    RenderSystem.disableCull();
                    VRenderSystem.setPrimitiveTopology(state.mode());
                    Renderer renderer = Renderer.getInstance();
                    renderer.bindGraphicsPipeline(pipeline);
                    if (!programOwned) VTextureSelector.bindShaderTextures(pipeline, this.owner);
                    renderer.uploadAndBindUBOs(pipeline);
                    if (pipeline.hasTessellation()) {
                        PreparedDraw prepared = new PreparedDraw();
                        try {
                            Renderer.getDrawer().prepare(prepared, mesh.vertexBuffer(), state, null);
                            Renderer.getDrawer().drawPrepared(prepared, true);
                        } finally { prepared.invalidate(); }
                    } else Renderer.getDrawer().draw(mesh.vertexBuffer(), state, null);
                }
            }
        }

        private void baseCopy(String reason) {
            GlEmulationLog.warnContractGap("draw_path", "blitBaseCopy." + reason,
                    "Render-target blit retained its base image because {} is unsupported; shader={}. Custom effect was not drawn",
                    reason, this.shader.getName());
            VulkanImage source = texture(this.sourceId);
            if (source == null) throw new IllegalStateException("Render-target base image has no live texture");
            copy(source);
        }

        private static void copy(VulkanImage image) {
            try (NativeState ignored = new NativeState(1L)) {
                VTextureSelector.bindTexture(0, image);
                DrawUtil.blitToScreen(image.isRowsBottomUp());
            }
        }

        private static VulkanImage texture(int id) {
            GlTexture texture = id > 0 ? GlTexture.getTexture(id) : null;
            return texture == null ? null : texture.getVulkanImage();
        }

        @Override
        public void close() {
            this.closed = true;
            this.owner = null;
            this.shader = null;
            this.sourceId = 0;
        }
    }


    private static final class NativeState implements AutoCloseable {
        private final Renderer renderer = Renderer.getInstance();
        private final Pipeline previousPipeline = this.renderer.getBoundPipeline();
        private final int previousTopology = VRenderSystem.topology;
        private final boolean previousCull = VRenderSystem.cull;
        private final VTextureSelector.BindingSnapshot textures;

        NativeState(long units) { this.textures = VTextureSelector.captureBindings(units); }

        @Override
        public void close() {
            Throwable failure = null;
            try { this.textures.restore(); }
            catch (RuntimeException | Error cleanup) { failure = cleanup; }
            VRenderSystem.topology = this.previousTopology;
            try {
                if (this.previousCull) RenderSystem.enableCull();
                else RenderSystem.disableCull();
            } catch (RuntimeException | Error cleanup) { failure = merge(failure, cleanup); }
            try { this.renderer.restoreGraphicsPipeline(this.previousPipeline); }
            catch (RuntimeException | Error cleanup) { failure = merge(failure, cleanup); }
            if (failure instanceof RuntimeException runtime) throw runtime;
            if (failure instanceof Error error) throw error;
        }

        private static Throwable merge(Throwable primary, Throwable cleanup) {
            if (primary == null) return cleanup;
            if (primary != cleanup) primary.addSuppressed(cleanup);
            return primary;
        }
    }
}
