package net.vulkanmod.compat.tdp;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.fml.ModList;
import net.vulkanmod.compat.sable.aeronautics.AeronauticsMaterialState;
import net.vulkanmod.interfaces.ShaderMixed;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import org.lwjgl.system.MemoryUtil;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;


public final class TdpFastDrawBridge {
    private TdpFastDrawBridge() {}
    private static Layout cached;

    public static boolean enabled() { return !ModList.get().isLoaded("iris"); }

    public record Layout(VertexFormat sourceFormat, VertexFormat drawFormat, int sourceStride, int drawStride) {}

    public static synchronized Layout layout(VertexFormat source) {
        Objects.requireNonNull(source);
        if (cached != null && cached.sourceFormat() == source) return cached;


        if (source == CustomVertexFormat.COMPRESSED_TERRAIN)
            return cached = new Layout(source, source, source.getVertexSize(), source.getVertexSize());
        int stride = source.getVertexSize(), alignment = 1, end = 0;
        if (stride <= 0) throw new IllegalArgumentException("Empty TDP vertex format");
        var builder = VertexFormat.builder();
        for (int i = 0; i < source.getElements().size(); i++) {
            var element = source.getElements().get(i);
            int component = element.type().size(), offset = source.getOffset(element);
            int size = Math.multiplyExact(component, element.count());
            if (component <= 0 || offset < end || offset % component != 0 || size > stride - offset)
                throw new IllegalArgumentException("Misaligned or overlapping TDP vertex element");
            alignment = Math.max(alignment, component);
            builder.padding(offset - end).add(source.getElementAttributeNames().get(i), element);
            end = offset + size;
        }
        int padded = Math.toIntExact(((long) stride + alignment - 1) / alignment * alignment);
        VertexFormat draw = source;
        if (padded != stride) draw = builder.padding(padded - end).build();
        return cached = new Layout(source, draw, stride, padded);
    }

    public static VertexFormat forShader(VertexFormat source) { return layout(source).drawFormat(); }

    private static int bytes(int vertices, int stride) {
        if (vertices < 0 || vertices % 4 != 0) throw new IllegalArgumentException("TDP batch must contain complete quads");
        long bytes = (long) vertices * stride;
        if (bytes > Integer.MAX_VALUE || (long) vertices / 4 * 6 > Integer.MAX_VALUE)
            throw new IllegalArgumentException("TDP batch exceeds supported capacity");
        return (int) bytes;
    }

    public static void copyVertices(Layout layout, ByteBuffer source, int vertices, ByteBuffer destination) {
        int sourceBytes = bytes(vertices, layout.sourceStride()), drawBytes = bytes(vertices, layout.drawStride());
        if (source.remaining() < sourceBytes || destination.remaining() < drawBytes)
            throw new IllegalArgumentException("Truncated TDP vertex batch");
        int src = source.position(), dst = destination.position();
        for (int vertex = 0; vertex < vertices; vertex++) {
            for (int b = 0; b < layout.sourceStride(); b++) destination.put(dst + b, source.get(src + b));
            for (int b = layout.sourceStride(); b < layout.drawStride(); b++) destination.put(dst + b, (byte) 0);
            src += layout.sourceStride(); dst += layout.drawStride();
        }
    }

    public interface Lease extends AutoCloseable { @Override void close(); }
    public interface Mesh extends Lease {
        ByteBuffer bytes();
        void finish(VertexFormat format, int vertices);
    }
    public interface Backend {
        Mesh allocate(int bytes);
        Lease captureState();
        void setup();
        void draw(Mesh mesh);
        void clear();
    }

    public static void route(VertexFormat format, ByteBuffer source, int vertices, Runnable release, Backend backend) {
        Mesh mesh = null; Lease state = null;
        boolean released = false, setup = false;
        Throwable failure = null;
        try {
            Layout layout = layout(format);
            int sourceBytes = bytes(vertices, layout.sourceStride()), drawBytes = bytes(vertices, layout.drawStride());
            if (source.remaining() < sourceBytes) throw new IllegalArgumentException("Truncated TDP vertex batch");
            if (vertices != 0) {
                mesh = backend.allocate(drawBytes);
                copyVertices(layout, source, vertices, mesh.bytes());
                mesh.finish(layout.drawFormat(), vertices);
            }
            released = true; release.run();
            if (vertices != 0) {
                state = backend.captureState();
                setup = true; backend.setup();
                backend.draw(mesh);
            }
        } catch (RuntimeException | Error error) { failure = error; }
        finally {
            if (!released) { try { release.run(); } catch (RuntimeException | Error error) { failure = append(failure, error); } }
            if (setup) { try { backend.clear(); } catch (RuntimeException | Error error) { failure = append(failure, error); } }
            if (state != null) { try { state.close(); } catch (RuntimeException | Error error) { failure = append(failure, error); } }
            if (mesh != null) { try { mesh.close(); } catch (RuntimeException | Error error) { failure = append(failure, error); } }
        }
        rethrow(failure);
    }

    public static void routeBorrowed(VertexFormat format, long address, int capacity, int vertices, Runnable release, Backend backend) {
        boolean[] released = {false};
        Runnable once = () -> { if (!released[0]) { released[0] = true; release.run(); } };
        Throwable failure = null;
        try {
            Layout layout = layout(format);
            int size = bytes(vertices, layout.sourceStride());
            bytes(vertices, layout.drawStride());
            if (capacity < 0 || size > capacity || (size != 0 && address == 0))
                throw new IllegalArgumentException("Invalid borrowed TDP vertex range");
            route(format, size == 0 ? ByteBuffer.allocate(0) : MemoryUtil.memByteBuffer(address, size), vertices, once, backend);
        } catch (RuntimeException | Error error) { failure = error; }
        finally { try { once.run(); } catch (RuntimeException | Error error) { failure = append(failure, error); } }
        rethrow(failure);
    }

    public static void draw(RenderType owner, RenderType.CompositeState state, long address, int capacity, int vertices, Runnable release) {
        routeBorrowed(owner.format(), address, capacity, vertices, release, new NativeBackend(owner, state));
    }

    private static final class NativeBackend implements Backend {
        private final RenderType owner;
        private final RenderType.CompositeState state;
        private ShaderInstance shader;
        private NativeBackend(RenderType owner, RenderType.CompositeState state) { this.owner = owner; this.state = state; }
        public Mesh allocate(int bytes) { return new NativeMesh(bytes); }
        public Lease captureState() {
            RenderSystem.assertOnRenderThread();
            shader = ShaderFields.resolve(state);
            int prefix = 0;
            for (var descriptor : ((ShaderMixed) shader).getPipeline().getImageDescriptors()) {
                if (!descriptor.usesVanillaTextureBinding()) continue;
                int unit = descriptor.getTextureUnit();
                if (unit < 0 || unit >= 48) throw new IllegalArgumentException("Invalid TDP shader texture unit");
                prefix = Math.max(prefix, unit + 1);
            }
            var borrowed = AeronauticsMaterialState.capture(prefix);
            return borrowed::close;
        }
        public void setup() { owner.setupRenderState(); }
        public void draw(Mesh mesh) { BufferUploader.drawWithShader(((NativeMesh) mesh).data); }
        public void clear() {
            Throwable failure = null;
            try { shader.clear(); } catch (RuntimeException | Error error) { failure = error; }
            try { owner.clearRenderState(); } catch (RuntimeException | Error error) { failure = append(failure, error); }
            rethrow(failure);
        }
    }


    private static final class ShaderFields {
        private static final Field SHARD = field(RenderType.CompositeState.class, "shaderState");
        private static final Field SUPPLIER = field(RenderStateShard.ShaderStateShard.class, "shader");
        private static Field field(Class<?> type, String name) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
            catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot resolve TDP render shader metadata", error); }
        }
        @SuppressWarnings("unchecked")
        private static ShaderInstance resolve(RenderType.CompositeState state) {
            try {
                var supplier = (Optional<Supplier<ShaderInstance>>) SUPPLIER.get(SHARD.get(state));
                return Objects.requireNonNull(supplier.orElseThrow().get(), "TDP shader is not ready");
            } catch (IllegalAccessException error) { throw new IllegalStateException("Cannot read TDP render shader", error); }
        }
    }

    private static final class NativeMesh implements Mesh {
        private final ByteBufferBuilder builder;
        private final ByteBuffer bytes;
        private MeshData data;
        private ByteBufferBuilder.Result result;
        private boolean closed;
        private NativeMesh(int capacity) {
            builder = new ByteBufferBuilder(capacity);
            try { bytes = MemoryUtil.memByteBuffer(builder.reserve(capacity), capacity); }
            catch (RuntimeException | Error error) { builder.close(); throw error; }
        }
        public ByteBuffer bytes() { return bytes; }
        public void finish(VertexFormat format, int vertices) {
            if (closed || data != null) throw new IllegalStateException("TDP mesh already sealed or closed");
            result = Objects.requireNonNull(builder.build());
            try {
                int indices = vertices / 4 * 6;
                data = new MeshData(result, new MeshData.DrawState(format, vertices, indices, VertexFormat.Mode.QUADS,
                        VertexFormat.IndexType.least(vertices))) {
                    @Override public void close() { NativeMesh.this.close(); }
                };
            } catch (RuntimeException | Error error) { result.close(); throw error; }
        }
        public void close() {
            if (closed) return;
            closed = true;
            Throwable failure = null;
            try { if (result != null) result.close(); } catch (RuntimeException | Error error) { failure = error; }
            try { builder.close(); } catch (RuntimeException | Error error) { failure = append(failure, error); }
            rethrow(failure);
        }
    }

    private static Throwable append(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }
    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) throw error;
        if (failure != null) throw (RuntimeException) failure;
    }
}
