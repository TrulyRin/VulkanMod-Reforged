package net.vulkanmod.compat;

public final class CompatBootstrap {
    private static boolean initialized;

    private CompatBootstrap() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;

        if (CompatState.soloMode()) {
            return;
        }

        registerRenderPathClaims("/assets/vulkanmod/compat/hud_path_claim_mod_ids.txt");
    }

    private static void registerRenderPathClaims(String resourcePath) {
        for (String modId : CompatMods.loadLines(resourcePath)) {
            CompatAdapterRegistry.register(new RenderPathClaimAdapter(modId, modId));
        }
    }
}

