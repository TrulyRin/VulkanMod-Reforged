package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraftforge.client.extensions.IForgeVertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(VertexConsumer.class)
public interface VertexConsumerM {

    @Shadow(remap = false)
    void m_5954_(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ);

       
              
       
    @Overwrite(remap = false)
    default public void m_85995_(PoseStack.Pose matrixEntry, BakedQuad quad, float[] brightness, float red, float green, float blue, int[] lights, int overlay, boolean useQuadColorData) {
        int[] vertices = quad.getVertices();
        Vec3i vec3i = quad.getDirection().getNormal();
        Matrix4f matrix4f = matrixEntry.pose();
        Vector3f normal = matrixEntry.normal().transform(new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ()));

        int vertexCount = vertices.length / 8;

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            for (int k = 0; k < vertexCount; ++k) {
                intBuffer.clear();
                intBuffer.put(vertices, k * 8, 8);

                float x = byteBuffer.getFloat(0);
                float y = byteBuffer.getFloat(4);
                float z = byteBuffer.getFloat(8);

                float r;
                float g;
                float b;
                if (useQuadColorData) {
                    r = (byteBuffer.get(12) & 255) / 255.0F * brightness[k] * red;
                    g = (byteBuffer.get(13) & 255) / 255.0F * brightness[k] * green;
                    b = (byteBuffer.get(14) & 255) / 255.0F * brightness[k] * blue;
                } else {
                    r = brightness[k] * red;
                    g = brightness[k] * green;
                    b = brightness[k] * blue;
                }

                int light = ((IForgeVertexConsumer) this).applyBakedLighting(lights[k], byteBuffer);
                float u = byteBuffer.getFloat(16);
                float v = byteBuffer.getFloat(20);

                Vector4f vector4f = matrix4f.transform(new Vector4f(x, y, z, 1.0F));
                ((IForgeVertexConsumer) this).applyBakedNormals(normal, byteBuffer, matrixEntry.normal());

                this.m_5954_(vector4f.x(), vector4f.y(), vector4f.z(), r, g, b, 1.0F, u, v, overlay, light, normal.x(), normal.y(), normal.z());
            }
        }
    }
}

