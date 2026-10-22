package net.vulkanmod.compat;

import net.vulkanmod.compat.path.RenderPath;
import net.vulkanmod.compat.path.RenderPathOwnership;

public final class RenderPathClaimAdapter implements CompatAdapter {
    private final String modId;
    private final String displayName;

    public RenderPathClaimAdapter(String modId, String displayName) {
        this.modId = modId.toLowerCase();
        this.displayName = displayName;
    }

    @Override
    public String getModId() {
        return modId;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isAvailable() {
        return CompatDetector.isModLoaded(modId);
    }

    @Override
    public void init() {
    }

    @Override
    public void onEnable() {
        RenderPathOwnership.assignOwnership(RenderPath.GUI, modId, CompatMode.SAFE);
        RenderPathOwnership.assignOwnership(RenderPath.TEXT, modId, CompatMode.SAFE);
        RenderPathOwnership.assignOwnership(RenderPath.ITEMS, modId, CompatMode.SAFE);
        RenderPathOwnership.assignOwnership(RenderPath.HUD_CACHE, modId, CompatMode.SAFE);
    }

    @Override
    public void onDisable() {
        RenderPathOwnership.releaseOwnership(RenderPath.GUI, modId);
        RenderPathOwnership.releaseOwnership(RenderPath.TEXT, modId);
        RenderPathOwnership.releaseOwnership(RenderPath.ITEMS, modId);
        RenderPathOwnership.releaseOwnership(RenderPath.HUD_CACHE, modId);
    }
}
