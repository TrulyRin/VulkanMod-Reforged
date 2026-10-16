package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VertexBuffer.class, priority = 1100)
public class VertexBufferM {

    private VBO vbo;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(VertexBuffer.Usage usage, CallbackInfo ci) {
        vbo = new VBO();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenBuffers()I"))
    private int doNothing() {
        return 0;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenVertexArrays()I"))
    private int doNothing2() {
        return 0;
    }

    @Inject(method = "bind", at = @At("HEAD"), cancellable = true)
    private void bind(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "unbind", at = @At("HEAD"), cancellable = true)
    private static void unbind(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void upload(MeshData meshData, CallbackInfo ci) {
        vbo.upload(meshData);
        ci.cancel();
    }

    @Inject(method = "uploadIndexBuffer(Lcom/mojang/blaze3d/vertex/ByteBufferBuilder$Result;)V", at = @At("HEAD"), cancellable = true)
    private void uploadIndexBuffer(ByteBufferBuilder.Result result, CallbackInfo ci) {
        try (result) {
            vbo.uploadIndexBuffer(result.byteBuffer());
        }
        ci.cancel();
    }

    @Inject(method = "drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V", at = @At("HEAD"), cancellable = true)
    private void drawWithShader(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance shader, CallbackInfo ci) {
        vbo.drawWithShader(viewMatrix, projectionMatrix, shader);
        ci.cancel();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void draw(CallbackInfo ci) {
        vbo.draw();
        ci.cancel();
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void close(CallbackInfo ci) {
        vbo.close();
        ci.cancel();
    }
}
