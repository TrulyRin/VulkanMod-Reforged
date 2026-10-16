package net.vulkanmod.compat.sable.aeronautics;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.compat.gl.GlPatchState;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.gl.GlProgram;

import java.util.Objects;

                                                                                                         
public final class AeronauticsAssemblyRenderer {
    private AeronauticsAssemblyRenderer() {}

       
                                                                                        
                                                                                            
                                                                                              
                                                                                           
       
    public static void renderBaseLayer(ShaderInstance shader, Runnable original, Runnable layerCleanup) {
                                                                                                     
        render(shader, 0.0, 0.0, 0.0, 0.0f, false, original, layerCleanup);
    }

    public static void renderGhostLayer(ShaderInstance shader,
            double cameraX, double cameraY, double cameraZ, float time, Runnable original, Runnable layerCleanup) {
                                                                                                    
        render(shader, cameraX, cameraY, cameraZ, time, true, original, layerCleanup);
    }

    private static void render(ShaderInstance shader, double cameraX, double cameraY, double cameraZ,
            float time, boolean prepareGhost, Runnable original, Runnable layerCleanup) {
        Objects.requireNonNull(layerCleanup, "layerCleanup");
        Objects.requireNonNull(shader, "shader");
        Objects.requireNonNull(original, "original");
        RenderSystem.assertOnRenderThread();
        if (!Double.isFinite(cameraX) || !Double.isFinite(cameraY) || !Double.isFinite(cameraZ)
                || !Float.isFinite(time)) {
            throw new IllegalArgumentException("Assembly material requires a finite camera and time");
        }
        int owner = VeilShaderProgramAccess.programId(shader);
        var conversion = owner > 0 ? GlProgram.convertedProgram(owner) : null;
        if (conversion == null) throw new IllegalStateException("The exact converted assembly material is unavailable");

        AeronauticsMaterialState before = AeronauticsMaterialState.capture(conversion.samplers().size());
        boolean shaderTouched = false;
        boolean originalEntered = false;
        boolean originalReturned = false;
        Throwable failure = null;
        try {
            if (prepareGhost) {
                shaderTouched = true;
                shader.apply();
                if (GlProgram.currentProgramId() != owner) {
                    throw new IllegalStateException("Assembly material selected a different program owner");
                }
                AeronauticsWorldLayers.prepare(shader, cameraX, cameraY, cameraZ);
            }
            shader.safeGetUniform("layerIndex").set(0);
            if (prepareGhost) {
                shader.safeGetUniform("time").set(time);
                shader.safeGetUniform("SableEnableNormalLighting").set(0.0f);
                shader.safeGetUniform("SableSkyLightScale").set(1.0f);
            }
            GlPatchState.patchParameteri(0x8E72, 4);
            originalEntered = true;
            original.run();
            originalReturned = true;
        } catch (RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            Throwable cleanup = failure;
            if (!originalReturned) {
                if (shaderTouched || originalEntered) {
                    try { shader.clear(); }
                    catch (RuntimeException | Error error) { cleanup = append(cleanup, error); }
                }
                if (originalEntered) {
                    try { layerCleanup.run(); }
                    catch (RuntimeException | Error error) { cleanup = append(cleanup, error); }
                }
            }
            cleanup = before.restore(cleanup);
            if (failure == null && cleanup != null) rethrow(cleanup);
        }
    }

    private static Throwable append(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) throw error;
        throw (RuntimeException) failure;
    }
}
