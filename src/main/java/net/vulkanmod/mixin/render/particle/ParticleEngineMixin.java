package net.vulkanmod.mixin.render.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Shadow @Final private Map<ParticleRenderType, Queue<Particle>> particles;

    @Unique
    private final Reference2IntOpenHashMap<Class<?>> vulkanmod$sameTypeCounts = new Reference2IntOpenHashMap<>();
    @Unique
    private long vulkanmod$countsGameTime = Long.MIN_VALUE;

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAdd(Particle particle, CallbackInfo ci) {
        if (this.shouldCullAndPrioritize(particle)) {
            ci.cancel();
            return;
        }
        this.vulkanmod$sameTypeCounts.addTo(particle.getClass(), 1);
    }

    @Unique
    private int vulkanmod$sameTypeCount(Particle particle) {
        Minecraft mc = Minecraft.getInstance();
        long gameTime = mc.level == null ? Long.MIN_VALUE : mc.level.getGameTime();
        if (gameTime != this.vulkanmod$countsGameTime) {
            this.vulkanmod$countsGameTime = gameTime;
            this.vulkanmod$sameTypeCounts.clear();
            for (Queue<Particle> queue : this.particles.values()) {
                for (Particle p : queue) {
                    this.vulkanmod$sameTypeCounts.addTo(p.getClass(), 1);
                }
            }
        }
        return this.vulkanmod$sameTypeCounts.getInt(particle.getClass());
    }

    private boolean shouldCullAndPrioritize(Particle particle) {
        int cullingMode = Initializer.CONFIG.particleCulling;
        if (cullingMode == 0) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        double dx = ((ParticleAccessor) particle).getX() - cameraPos.x;
        double dy = ((ParticleAccessor) particle).getY() - cameraPos.y;
        double dz = ((ParticleAccessor) particle).getZ() - cameraPos.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        double maxDistSq;
        double protectedDistSq;
        double minDot;
        int maxCount;

        if (cullingMode == 1) {
            maxDistSq = 48.0 * 48.0;
            protectedDistSq = 10.0 * 10.0;
            minDot = 0.5;
            maxCount = 128;
        } else if (cullingMode == 2) {
            maxDistSq = 32.0 * 32.0;
            protectedDistSq = 8.0 * 8.0;
            minDot = 0.707;
            maxCount = 64;
        } else {
            maxDistSq = 16.0 * 16.0;
            protectedDistSq = 5.0 * 5.0;
            minDot = 0.866;
            maxCount = 24;
        }

        if (distSq <= protectedDistSq) {
            return false;
        }

        if (distSq > maxDistSq) {
            return true;
        }

        org.joml.Vector3f lookVec = camera.getLookVector();
        double dist = Math.sqrt(distSq);
        if (dist > 0.01) {
            double dot = (dx * lookVec.x + dy * lookVec.y + dz * lookVec.z) / dist;
            if (dot < minDot) {
                return true;
            }
        }

        return this.vulkanmod$sameTypeCount(particle) >= maxCount;
    }
}
