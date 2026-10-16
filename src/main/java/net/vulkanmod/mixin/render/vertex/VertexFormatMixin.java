package net.vulkanmod.mixin.render.vertex;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.buffers.GpuBuffer;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.interfaces.VertexFormatMixed;
import net.vulkanmod.vulkan.Renderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.nio.ByteBuffer;

@Mixin(VertexFormat.class)
public class VertexFormatMixin implements VertexFormatMixed {

    private int[] offsets;

    private ObjectArrayList<VertexFormatElement> fastList;

    @Inject(method = "uploadImmediateVertexBuffer", at = @At("HEAD"), cancellable = true)
    private void uploadImmediateVertexBuffer(ByteBuffer data, CallbackInfoReturnable<GpuBuffer> cir) {
        var buffer = Renderer.getDrawer().getGpuBuffers().getVertexBuffer();
        buffer.getBuffer().copyBuffer(data, data.remaining());
        buffer.setOffset(Math.toIntExact(buffer.getBuffer().getOffset()));
        cir.setReturnValue(buffer);
    }

    @Inject(method = "uploadImmediateIndexBuffer", at = @At("HEAD"), cancellable = true)
    private void uploadImmediateIndexBuffer(ByteBuffer data, CallbackInfoReturnable<GpuBuffer> cir) {
        var buffer = Renderer.getDrawer().getGpuBuffers().getIndexBuffer();
        buffer.getBuffer().copyBuffer(data, data.remaining());
        buffer.setOffset(Math.toIntExact(buffer.getBuffer().getOffset()));
        cir.setReturnValue(buffer);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectList(List<VertexFormatElement> list, List<String> list2, IntList intList, int i, CallbackInfo ci) {
        ObjectArrayList<VertexFormatElement> fList = new ObjectArrayList<>();
        fList.addAll(list);

        this.fastList = fList;

        this.offsets = intList.toIntArray();
    }

    public int getOffset(int i) {
        return this.offsets[i];
    }

    public VertexFormatElement getElement(int i) {
        return this.fastList.get(i);
    }

    public List<VertexFormatElement> getFastList() {
        return this.fastList;
    }

}
