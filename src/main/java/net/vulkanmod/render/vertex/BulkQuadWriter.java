package net.vulkanmod.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

                                                                                             
public final class BulkQuadWriter {
    private static final ThreadLocal<ScratchStack> SCRATCH = ThreadLocal.withInitial(ScratchStack::new);

                                                                                           
    private static final ClassValue<Boolean> HAS_ARRAY_OVERRIDE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> consumerClass) {
            try {
                return consumerClass.getMethod("putBulkData", PoseStack.Pose.class, BakedQuad.class,
                        float[].class, float.class, float.class, float.class, float.class,
                        int[].class, int.class).getDeclaringClass() != VertexConsumer.class;
            } catch (NoSuchMethodException impossible) {
                throw new IllegalStateException("Vertex consumer is missing the modern bulk-quad overload", impossible);
            }
        }
    };

    private BulkQuadWriter() {}

    public static void putBulkData(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                   float red, float green, float blue, float alpha, int light, int overlay) {
        if (HAS_ARRAY_OVERRIDE.get(consumer.getClass())) {
            consumer.putBulkData(pose, quad, new float[]{1, 1, 1, 1}, red, green, blue, alpha,
                    new int[]{light, light, light, light}, overlay);
            return;
        }
        putBulkData(consumer, pose, quad, null, red, green, blue, alpha, null, light, overlay, true);
    }

    public static void putBulkData(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                   float[] brightness, float red, float green, float blue, float alpha,
                                   int[] lights, int overlay) {
        putBulkData(consumer, pose, quad, brightness, red, green, blue, alpha, lights, 0, overlay, false);
    }

    private static void putBulkData(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                    float[] brightness, float red, float green, float blue, float alpha,
                                    int[] lights, int uniformLight, int overlay, boolean uniform) {
        ScratchStack stack = SCRATCH.get();
        Scratch scratch = stack.acquire();
        try {
            Vector3fc faceNormal = quad.direction().getUnitVec3f();
            Matrix4f transform = pose.pose();
            Vector3f normal = pose.transformNormal(faceNormal, scratch.normal);
            int emission = quad.lightEmission();

            for (int vertex = 0; vertex < 4; vertex++) {
                Vector3fc position = quad.position(vertex);
                long packedUv = quad.packedUV(vertex);
                float shade = uniform ? 1.0f : brightness[vertex];
                int color = ARGB.colorFromFloat(alpha, shade * red, shade * green, shade * blue);
                color = ARGB.multiply(color, quad.bakedColors().color(vertex));
                int light = LightTexture.lightCoordsWithEmission(uniform ? uniformLight : lights[vertex], emission);
                Vector3f transformedPosition = transform.transformPosition(position, scratch.position);
                float u = UVPair.unpackU(packedUv);
                float v = UVPair.unpackV(packedUv);

                                                                                                   
                                                                                             
                consumer.applyBakedNormals(normal, quad.bakedNormals(), vertex, pose.normal());
                consumer.addVertex(transformedPosition.x(), transformedPosition.y(), transformedPosition.z(),
                        color, u, v, overlay, light, normal.x(), normal.y(), normal.z());
            }
        } finally {
            stack.release(scratch);
        }
    }

                                                                                                             
    private static final class ScratchStack {
        private final Scratch root = new Scratch(null);
        private Scratch current;

        Scratch acquire() {
            if (current == null) {
                current = root;
            } else {
                if (current.next == null) current.next = new Scratch(current);
                current = current.next;
            }
            return current;
        }

        void release(Scratch scratch) {
            current = scratch.parent;
        }
    }

    private static final class Scratch {
        private final Vector3f normal = new Vector3f();
        private final Vector3f position = new Vector3f();
        private final Scratch parent;
        private Scratch next;

        Scratch(Scratch parent) {
            this.parent = parent;
        }
    }
}
