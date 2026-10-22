package net.vulkanmod.render;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class CanonicalBlitResources {
    private static final String[] EXTENSIONS = {"json", "vsh", "fsh"};
    private static final String[] DIGESTS = {
            "f75212293232bffbe7a2d188bd49e1f4a7afe076e5a6985702f978e9ac61f253",
            "73d16123c82b9a51f419a9a5281c1feb896e107bb20128ed1d881cb1e746497a",
            "be951da6d3698c520a5716a0d1f1a66556c9c138960bf231a4459f580bf099b5"
    };

    private CanonicalBlitResources() {}

    public static boolean matches(ResourceProvider provider) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < EXTENSIONS.length; i++) {
                var location = ResourceLocation.fromNamespaceAndPath("minecraft",
                        "shaders/core/blit_screen." + EXTENSIONS[i]);
                try (var input = provider.getResourceOrThrow(location).open()) {
                    byte[] bytes = input.readNBytes(65537);
                    if (bytes.length > 65536 || !HexFormat.of().formatHex(digest.digest(bytes)).equals(DIGESTS[i])) return false;
                }
            }
            return true;
        } catch (IOException failure) {
            return false;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
