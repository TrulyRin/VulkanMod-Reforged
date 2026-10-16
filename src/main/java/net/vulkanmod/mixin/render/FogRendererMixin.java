package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Shadow private static boolean fogEnabled;
    @Unique private boolean vulkanmod$capturingWorldFog;
    @Unique private boolean vulkanmod$hasWorldFog;
    @Unique private final FogData vulkanmod$worldFog = new FogData();
    @Unique private final Vector4f vulkanmod$worldColor = new Vector4f();
    @Unique private final FogData vulkanmod$emptyFog = vulkanmod$noFog();

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
            at = @At("HEAD"))
    private void beginWorldFog(Camera camera, int distance, DeltaTracker delta, float darkness, ClientLevel level,
                               CallbackInfoReturnable<Vector4f> cir) {
        this.vulkanmod$capturingWorldFog = true;
    }

    @Inject(method = "updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", at = @At("HEAD"))
    private void captureWorldFog(ByteBuffer buffer, int offset, Vector4f color, float environmentalStart,
                                 float environmentalEnd, float distanceStart, float distanceEnd, float skyEnd,
                                 float cloudEnd, CallbackInfo ci) {
                                                                                      
                                                                                                
        if (!this.vulkanmod$capturingWorldFog) return;
        FogData fog = this.vulkanmod$worldFog;
        fog.environmentalStart = environmentalStart;
        fog.environmentalEnd = environmentalEnd;
        fog.renderDistanceStart = distanceStart;
        fog.renderDistanceEnd = distanceEnd;
        fog.skyEnd = skyEnd;
        fog.cloudEnd = cloudEnd;
        this.vulkanmod$worldColor.set(color);
        this.vulkanmod$hasWorldFog = true;
    }

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;",
            at = @At("RETURN"))
    private void endWorldFog(Camera camera, int distance, DeltaTracker delta, float darkness, ClientLevel level,
                             CallbackInfoReturnable<Vector4f> cir) {
        this.vulkanmod$capturingWorldFog = false;
    }

    @Inject(method = "getBuffer(Lnet/minecraft/client/renderer/fog/FogRenderer$FogMode;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;",
            at = @At("HEAD"))
    private void applySelectedFog(FogRenderer.FogMode mode, CallbackInfoReturnable<GpuBufferSlice> cir) {
        if (mode != FogRenderer.FogMode.WORLD) return;
        VRenderSystem.fogData = fogEnabled && this.vulkanmod$hasWorldFog
                ? this.vulkanmod$worldFog : this.vulkanmod$emptyFog;
        Vector4f color = this.vulkanmod$worldColor;
        VRenderSystem.setShaderFogColor(color.x, color.y, color.z, color.w);
    }

    @Unique
    private static FogData vulkanmod$noFog() {
        FogData fog = new FogData();
        fog.environmentalStart = Float.MAX_VALUE;
        fog.environmentalEnd = Float.MAX_VALUE;
        fog.renderDistanceStart = Float.MAX_VALUE;
        fog.renderDistanceEnd = Float.MAX_VALUE;
        fog.skyEnd = Float.MAX_VALUE;
        fog.cloudEnd = Float.MAX_VALUE;
        return fog;
    }
}
