package net.vulkanmod.mixin.compatibility.tdp;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.compat.tdp.TdpFastDrawBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Pseudo
@Mixin(targets="org.mesdag.thr_dim_particle.client.TDPClient", remap=false)
public abstract class TdpShaderFormatMixin {
    @ModifyArg(method="registerShaders", at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/ShaderInstance;<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V"), index=2, require=4)
    private static VertexFormat vulkanmod$alignedDrawFormat(VertexFormat original) {
        return TdpFastDrawBridge.enabled() ? TdpFastDrawBridge.forShader(original) : original;
    }
}
