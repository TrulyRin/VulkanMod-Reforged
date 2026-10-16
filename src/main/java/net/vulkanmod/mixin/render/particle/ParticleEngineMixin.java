package net.vulkanmod.mixin.render.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.culling.ParticleAdmissionTracker;
import net.vulkanmod.render.culling.ParticleCullingPolicy;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

                                                                                          
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Unique private final ParticleAdmissionTracker<Particle> vulkanmod$admissions = new ParticleAdmissionTracker<>();

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$filterParticle(Particle particle, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || Initializer.CONFIG == null) {
            return;
        }
        int count = this.vulkanmod$admissions.countAlive(particle.getClass(), minecraft.level.getGameTime(), Particle::isAlive);
        int mode = Initializer.CONFIG.particleCulling;
        if (mode <= 0) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return;
        }
        Vec3 position = camera.position();
        Vector3fc look = camera.forwardVector();
        ParticleAccessor coordinates = (ParticleAccessor) particle;
        if (ParticleCullingPolicy.shouldCull(mode, coordinates.getX() - position.x,
                coordinates.getY() - position.y, coordinates.getZ() - position.z,
                look.x(), look.y(), look.z(), count)) {
            ci.cancel();
        }
    }

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
    private boolean vulkanmod$trackAcceptedParticle(Queue<Particle> queue, Object value) {
        Particle particle = (Particle) value;
        boolean accepted = queue.add(particle);
        if (accepted) {
            this.vulkanmod$admissions.accepted(particle);
        }
        return accepted;
    }

    @Inject(method = "clearParticles", at = @At("RETURN"))
    private void vulkanmod$clearAdmissions(CallbackInfo ci) {
        this.vulkanmod$admissions.clear();
    }
}
