package net.vulkanmod.compat.sable.flywheel;

import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class VulkanEmbeddingData {
    private final Matrix4f pose = new Matrix4f();
    private final Matrix3f normal = new Matrix3f();
    private final Matrix4f lightingTransform = new Matrix4f();
    private int lightingSceneId;
    private float skyLightScale = 1.0f;

    public synchronized void updateTransforms(Matrix4fc pose, Matrix3fc normal) {
        this.pose.set(pose);
        this.normal.set(normal);
    }

    public synchronized void updateLighting(Matrix4fc transform, int sceneId, float skyLightScale) {
        this.lightingTransform.set(transform);
        this.lightingSceneId = sceneId;
        this.skyLightScale = skyLightScale;
    }

    public synchronized Matrix4f pose() {
        return new Matrix4f(pose);
    }

    public synchronized Matrix3f normal() {
        return new Matrix3f(normal);
    }

    public synchronized Matrix4f lightingTransform() {
        return new Matrix4f(lightingTransform);
    }

    public synchronized int lightingSceneId() {
        return lightingSceneId;
    }

    public synchronized float skyLightScale() {
        return skyLightScale;
    }
}
