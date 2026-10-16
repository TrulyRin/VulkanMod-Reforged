package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VertexBuffer.class, priority = 900)
public class VertexBufferM {

    private VBO vbo;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(CallbackInfo ci) {
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

                                                                                                 
                                                                                     

           
    @Inject(method = "m_85921_", at = @At("HEAD"), cancellable = true, remap = false)
    private void bind(CallbackInfo ci) {
        ci.cancel();
    }

             
    @Inject(method = "m_85931_", at = @At("HEAD"), cancellable = true, remap = false)
    private static void unbind(CallbackInfo ci) {
        ci.cancel();
    }

             
    @Inject(method = "m_231221_", at = @At("HEAD"), cancellable = true, remap = false)
    private void upload(BufferBuilder.RenderedBuffer buffer, CallbackInfo ci) {
        vbo.upload(buffer);
        ci.cancel();
    }

                     
    @Inject(method = "m_253207_", at = @At("HEAD"), cancellable = true, remap = false)
    private void drawWithShader(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance shader, CallbackInfo ci) {
        vbo.drawWithShader(viewMatrix, projectionMatrix, shader);
        ci.cancel();
    }

                         
    @Inject(method = "m_166882_", at = @At("HEAD"), cancellable = true, remap = false)
    private void draw(CallbackInfo ci) {
        vbo.drawChunkLayer();
        ci.cancel();
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true, remap = false)
    private void close(CallbackInfo ci) {
        vbo.close();
        ci.cancel();
    }
}

