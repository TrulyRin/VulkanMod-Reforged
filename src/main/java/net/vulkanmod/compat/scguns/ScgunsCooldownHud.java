package net.vulkanmod.compat.scguns;





public final class ScgunsCooldownHud {
    private static ScgunsCooldownRenderer renderer;
    private static boolean hudScope;

    private ScgunsCooldownHud() {
    }

    public static void register(ScgunsCooldownRenderer handler) {
        renderer = handler;
    }

    public static boolean isRegistered() {
        return renderer != null;
    }

    public static boolean isHudScope() {
        return hudScope;
    }

    public static void render(boolean safeFrame, boolean hiddenGui) {
        ScgunsCooldownRenderer current = renderer;
        if (current == null || !safeFrame || hiddenGui || hudScope) {
            return;
        }

        hudScope = true;
        try {

            current.vulkanMod$renderCooldownHud();
        } finally {
            hudScope = false;
        }
    }
}
