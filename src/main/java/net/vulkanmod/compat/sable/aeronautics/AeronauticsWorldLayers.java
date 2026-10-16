package net.vulkanmod.compat.sable.aeronautics;

import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.compat.sable.veil.VeilShaderProgramAccess;
import net.vulkanmod.compat.sable.veil.VulkanVeilCapabilities;
import net.vulkanmod.gl.GlProgram;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

                                                                                              
public final class AeronauticsWorldLayers {
    public static final ResourceLocation PROGRAM = ResourceLocation.fromNamespaceAndPath("aeronautics", "levitite/levitite");
    private static final Snapshot<RenderType> EMPTY = new Snapshot<>(null, null, false);
    private static volatile Resolved resolved;
    private static boolean initialized;

    private AeronauticsWorldLayers() {}

                                                                            
    public static void initialize() {
        if (initialized) return;
        try {
            ClassLoader loader = AeronauticsWorldLayers.class.getClassLoader();
            Class<?> types = Class.forName("dev.eriksonn.aeronautics.index.client.AeroRenderTypes", false, loader);
            Class<?> material = Class.forName("dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager", false, loader);
            Class<?> veil = Class.forName("foundry.veil.api.client.render.VeilRenderSystem", false, loader);
            Class<?> bridge = Class.forName("foundry.veil.api.client.render.VeilRenderBridge", false, loader);
            Class<?> programType = Class.forName("foundry.veil.api.client.render.shader.program.ShaderProgram", false, loader);
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            RenderType base = (RenderType) lookup.unreflect(types.getMethod("levitite")).invoke();
            RenderType ghost = (RenderType) lookup.unreflect(types.getMethod("levititeGhosts")).invoke();
            var rendererMethod = veil.getMethod("renderer");
            var managerMethod = rendererMethod.getReturnType().getMethod("getShaderManager");
            var shaderMethod = managerMethod.getReturnType().getMethod("getShader", ResourceLocation.class);
            MethodHandle manager = MethodHandles.filterReturnValue(lookup.unreflect(rendererMethod), lookup.unreflect(managerMethod));
            MethodHandle getter = MethodHandles.insertArguments(lookup.unreflect(shaderMethod), 1, PROGRAM);
            MethodHandle program = MethodHandles.filterReturnValue(manager, getter).asType(MethodType.methodType(Object.class));
            MethodHandle wrapper = lookup.unreflect(bridge.getMethod("toShaderInstance", programType))
                    .asType(MethodType.methodType(ShaderInstance.class, Object.class));
            MethodHandle prepare = lookup.unreflect(material.getMethod("prepareShaderForWorld", ShaderInstance.class,
                    double.class, double.class, double.class));
            resolved = new Resolved(new Snapshot<>(base, ghost, false), new Snapshot<>(base, ghost, true), program, wrapper, prepare);
        } catch (ClassNotFoundException absent) {
                                                                                 
        } catch (Throwable failure) {
            if (failure instanceof VirtualMachineError fatal) throw fatal;
            LogUtils.getLogger().warn("Aeronautics world-layer integration unavailable; retaining ordinary terrain", failure);
        }
        initialized = true;
    }

                                                                                          
    public static Snapshot<RenderType> captureBuildLayers() {
        Resolved current = resolved;
        return current == null ? EMPTY : isEnabled() ? current.enabled : current.disabled;
    }

    public static boolean isEnabled() {
        return resolved != null && VulkanVeilCapabilities.supported(VulkanVeilCapabilities.Feature.TESSELLATION);
    }

    public static boolean isBase(RenderType type) { return captureBuildLayers().isBase(type); }
    public static boolean isGhost(RenderType type) { return captureBuildLayers().isGhost(type); }

                                                                                                      
    public static ShaderInstance shader() {
        Resolved current = resolved;
        if (current == null) return null;
        try {
            Object program = current.program.invokeExact();
            if (program == null) return null;
            ShaderInstance shader = (ShaderInstance) current.wrapper.invokeExact(program);
            int owner = VeilShaderProgramAccess.programId(shader);
            return owner > 0 && GlProgram.linkedProgram(owner) != null && GlProgram.convertedProgram(owner) != null ? shader : null;
        } catch (Throwable failure) { throw invocationFailure(failure); }
    }

    public static void prepare(ShaderInstance shader, double cameraX, double cameraY, double cameraZ) {
        Resolved current = resolved;
        if (current == null) throw new IllegalStateException("Aeronautics world layers are unavailable");
        try { current.prepare.invokeExact(shader, cameraX, cameraY, cameraZ); }
        catch (Throwable failure) { throw invocationFailure(failure); }
    }

    private static IllegalStateException invocationFailure(Throwable failure) {
        if (failure instanceof VirtualMachineError fatal) throw fatal;
        return new IllegalStateException("Aeronautics world-layer invocation failed", failure);
    }

    public record Snapshot<T>(T base, T ghost, boolean buildBase) {
        public boolean isBase(T type) { return base != null && type == base; }
        public boolean isGhost(T type) { return ghost != null && type == ghost; }
    }

    private record Resolved(Snapshot<RenderType> disabled, Snapshot<RenderType> enabled,
                            MethodHandle program, MethodHandle wrapper, MethodHandle prepare) {}
}
