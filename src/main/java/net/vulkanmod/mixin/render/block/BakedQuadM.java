package net.vulkanmod.mixin.render.block;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.vulkanmod.render.model.quad.QuadView;
import net.vulkanmod.render.model.quad.ModelQuadFlags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.vulkanmod.render.model.quad.BakedQuadDeduplicator;

import static net.vulkanmod.render.model.quad.ModelQuad.VERTEX_SIZE;

@Mixin(BakedQuad.class)
public class BakedQuadM implements QuadView {

    @Shadow(remap = false) @Final protected int[] f_111292_;
    @Shadow(remap = false) @Final protected Direction f_111294_;
    @Shadow(remap = false) @Final protected int f_111293_;
    private int flags;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int[] modifyVertices(int[] vertices) {
        return BakedQuadDeduplicator.deduplicateVertices(vertices);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite textureAtlasSprite, boolean shade, CallbackInfo ci) {
        this.flags = ModelQuadFlags.getQuadFlags(this.f_111292_, direction);
    }

    @Override
    public int getFlags() {
        return flags;
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.f_111292_[vertexOffset(idx) + 0]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.f_111292_[vertexOffset(idx) + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.f_111292_[vertexOffset(idx) + 2]);
    }

    @Override
    public int getColor(int idx) {
        return this.f_111292_[vertexOffset(idx) + 3];
    }

    @Override
    public float getU(int idx) {
        return Float.intBitsToFloat(this.f_111292_[vertexOffset(idx) + 4]);
    }

    @Override
    public float getV(int idx) {
        return Float.intBitsToFloat(this.f_111292_[vertexOffset(idx) + 5]);
    }

    @Override
    public int getColorIndex() {
        return this.f_111293_;
    }

    @Override
    public Direction getFacingDirection() {
        return this.f_111294_;
    }

    @Override
    public boolean isTinted() {
        return this.f_111293_ != -1;
    }

    private static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }
}

