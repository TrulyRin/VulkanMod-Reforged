package net.vulkanmod.mixin.render.vertex;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.interfaces.VertexFormatMixed;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(VertexFormat.class)
public class VertexFormatMixin implements VertexFormatMixed {

    @Shadow(remap = false) @Final private ImmutableList<VertexFormatElement> f_86012_;

    @Shadow(remap = false) @Final private IntList f_86013_;

    private int[] offsets;

    private ObjectArrayList<VertexFormatElement> fastList;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void injectList(ImmutableMap<String, VertexFormatElement> elements, CallbackInfo ci) {
        ObjectArrayList<VertexFormatElement> fList = new ObjectArrayList<>();
        fList.addAll(this.f_86012_);

        this.fastList = fList;

        this.offsets = this.f_86013_.toIntArray();
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

