package net.vulkanmod.render;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;


public final class CanonicalParticleResources {
    private static final Map<ResourceLocation, String> DIGESTS = Map.of(
            location("core/particle.json"), "1acb68ecbb60b06ed1acb7a3f0f0fdf64cc18b47f56850de450a33a22f9b5f59",
            location("core/particle.vsh"), "b1d9f46ed0d464bcee6ba31219489aed39cf06d48680f0f107e08ea19240eb21",
            location("core/particle.fsh"), "7d07dcf396ae055be1c70bc7fdbfd12d2ac22bf145ce9dca40cb3f1038258b59",
            location("include/fog.glsl"), "a3c0f5e59aea2eaae919792b3f5c7fba3c12fbd78317da479e0fb6a25d2d4fd1");

    private CanonicalParticleResources() {}

    public static boolean matches(ProviderShaderResources selected) {
        return selected.matches(DIGESTS);
    }

    private static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", "shaders/" + path);
    }
}
