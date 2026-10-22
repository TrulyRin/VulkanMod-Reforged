package net.vulkanmod.vulkan.shader;

import com.mojang.blaze3d.shaders.Uniform;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public final class RawShaderUniforms {
    private RawShaderUniforms() {}

    public static void bind(List<UBO> buffers, Map<String, Uniform> owners) {
        for (UBO buffer : buffers) for (var field : buffer.getUniforms()) {
            String name = field.getName();
            if (owners.containsKey(name)) {
                Supplier<MappedBuffer> source = source(field, owners, name);
                field.setSupplier(name.equals("ProjMat") && field.getType().equals("mat4")
                        ? LegacyProjectionSupplier.fromSource(source) : source);
            } else if (name.equals("MVP") && field.getType().equals("mat4")) {
                field.setSupplier(new OwnerMvp(owners));
            }
        }
    }

    private static Supplier<MappedBuffer> source(net.vulkanmod.vulkan.shader.layout.Uniform field,
                                                 Map<String, Uniform> owners, String name) {
        return new LegacyUniformSupplier(field, () -> owners.get(name), () -> ordinary(owners.get(name), name));
    }

    private static MappedBuffer ordinary(Uniform owner, String name) {
        if (owner == null) throw new IllegalArgumentException("Raw shader uniform owner was removed: " + name);
        if (owner.getType() <= 3) {
            IntBuffer values = owner.getIntBuffer();
            if (values == null || !values.isDirect()) throw new IllegalArgumentException("Raw uniform requires direct integer storage: " + name);
            return MappedBuffer.createFromBuffer(MemoryUtil.memByteBuffer(values.duplicate().clear()));
        }
        FloatBuffer values = owner.getFloatBuffer();
        if (values == null || !values.isDirect()) throw new IllegalArgumentException("Raw uniform requires direct float storage: " + name);
        return MappedBuffer.createFromBuffer(MemoryUtil.memByteBuffer(values.duplicate().clear()));
    }

    private static final class OwnerMvp implements Supplier<MappedBuffer> {
        private final Supplier<MappedBuffer> model, projection;
        private final Matrix4f modelMatrix = new Matrix4f(), product = new Matrix4f();
        private final MappedBuffer result = MappedBuffer.createFromBuffer(ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()));

        OwnerMvp(Map<String, Uniform> owners) {
            var modelField = net.vulkanmod.vulkan.shader.layout.Uniform.createField(
                    net.vulkanmod.vulkan.shader.layout.Uniform.createUniformInfo("mat4", "ModelViewMat"));
            var projectionField = net.vulkanmod.vulkan.shader.layout.Uniform.createField(
                    net.vulkanmod.vulkan.shader.layout.Uniform.createUniformInfo("mat4", "ProjMat"));
            this.model = source(modelField, owners, "ModelViewMat");
            this.projection = LegacyProjectionSupplier.fromSource(source(projectionField, owners, "ProjMat"));
        }

        @Override public MappedBuffer get() {
            read(model.get(), modelMatrix); read(projection.get(), product);
            product.mul(modelMatrix).get(result.buffer);
            return result;
        }

        private static void read(MappedBuffer source, Matrix4f destination) {
            destination.set(source.getFloat(0), source.getFloat(4), source.getFloat(8), source.getFloat(12),
                    source.getFloat(16), source.getFloat(20), source.getFloat(24), source.getFloat(28),
                    source.getFloat(32), source.getFloat(36), source.getFloat(40), source.getFloat(44),
                    source.getFloat(48), source.getFloat(52), source.getFloat(56), source.getFloat(60));
        }
    }
}
