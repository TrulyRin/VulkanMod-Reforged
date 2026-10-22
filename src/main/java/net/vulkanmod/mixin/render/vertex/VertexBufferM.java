package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.gl.GlBuffer;
import net.vulkanmod.gl.GlVertexArray;
import net.vulkanmod.render.VBO;
import net.vulkanmod.interfaces.ImmediateBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VertexBuffer.class, priority = 1100)
public class VertexBufferM implements ImmediateBuffer {

    @Shadow private int vertexBufferId;
    @Shadow private int indexBufferId;
    @Shadow private int arrayObjectId;
    @Shadow private VertexFormat format;
    @Shadow private VertexFormat.Mode mode;
    @Shadow private VertexFormat.IndexType indexType;
    @Shadow private int indexCount;
    @Shadow private RenderSystem.AutoStorageIndexBuffer sequentialIndices;
    private VBO vbo;

    @Override
    public boolean setImmediateUpload(boolean enabled) {
        return this.vbo.setImmediateUpload(enabled);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(VertexBuffer.Usage usage, CallbackInfo ci) {
        vbo = new VBO();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenBuffers()I"))
    private int doNothing() {
        return GlBuffer.glGenBuffers();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenVertexArrays()I"))
    private int doNothing2() {
        return GlVertexArray.genVertexArray();
    }

    @Inject(method = "bind", at = @At("HEAD"), cancellable = true)
    private void bind(CallbackInfo ci) {
        BufferUploader.invalidate();
        GlVertexArray.bindVertexArray(this.arrayObjectId < 0 ? 0 : this.arrayObjectId);
        ci.cancel();
    }

    @Inject(method = "unbind", at = @At("HEAD"), cancellable = true)
    private static void unbind(CallbackInfo ci) {
        BufferUploader.invalidate();
        GlVertexArray.bindVertexArray(0);
        ci.cancel();
    }

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void upload(MeshData meshData, CallbackInfo ci) {
        MeshData.DrawState state = this.vbo.uploadAndGetState(meshData);
        this.format = state.format();
        this.mode = state.mode();
        this.indexType = state.indexType();
        this.indexCount = state.indexCount();
        this.sequentialIndices = this.vbo.usesCustomIndices() ? null : RenderSystem.getSequentialBuffer(this.mode);
        this.vbo.markManaged(this.vertexBufferId, this.indexBufferId);
        ci.cancel();
    }

    @Inject(method = "uploadIndexBuffer(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder$Result;)V", at = @At("HEAD"), cancellable = true)
    private void uploadIndexBuffer(ByteBufferBuilder.Result result, CallbackInfo ci) {
        try (result) {
            vbo.uploadIndexBuffer(result.byteBuffer());
        }
        this.sequentialIndices = null;
        this.vbo.markManaged(this.vertexBufferId, this.indexBufferId);
        ci.cancel();
    }

    @Inject(method = "drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", at = @At("HEAD"), cancellable = true)
    private void drawWithShader(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance shader, CallbackInfo ci) {
        if (this.arrayObjectId >= 0) {
            syncNativeState();
            vbo.drawWithShader(viewMatrix, projectionMatrix, shader);
        }
        ci.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void draw(CallbackInfo ci) {
        if (this.arrayObjectId >= 0) {
            syncNativeState();
            vbo.draw();
        }
        ci.cancel();
    }

    private void syncNativeState() {
        this.vbo.syncDirect(this.vertexBufferId, this.indexBufferId, this.format, this.mode,
                this.indexType, this.sequentialIndices != null, this.indexCount);
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void close(CallbackInfo ci) {
        int vertex = this.vertexBufferId, index = this.indexBufferId, array = this.arrayObjectId;

        this.vertexBufferId = this.indexBufferId = this.arrayObjectId = -1;
        this.indexCount = 0;
        this.format = null;
        this.mode = null;
        this.indexType = null;
        this.sequentialIndices = null;
        Throwable failure = null;
        try { if (this.vbo != null) this.vbo.close(); }
        catch (RuntimeException | Error cleanup) { failure = cleanup; }
        try { if (vertex > 0) GlBuffer.glDeleteBuffers(vertex); }
        catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        try { if (index > 0) GlBuffer.glDeleteBuffers(index); }
        catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        try { if (array > 0) GlVertexArray.deleteVertexArray(array); }
        catch (RuntimeException | Error cleanup) { failure = combine(failure, cleanup); }
        if (failure instanceof RuntimeException error) throw error;
        if (failure instanceof Error error) throw error;
        ci.cancel();
    }

    private static Throwable combine(Throwable primary, Throwable cleanup) {
        if (primary == null) return cleanup;
        if (primary != cleanup) primary.addSuppressed(cleanup);
        return primary;
    }
}
