package net.vulkanmod.compat;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class CompatAdapterRegistry {
    private static final ConcurrentHashMap<String, CompatAdapter> adapters = new ConcurrentHashMap<>();

    public static void register(CompatAdapter adapter) {
        if (adapter == null) return;
        String modId = adapter.getModId().toLowerCase();
        adapters.put(modId, adapter);
        adapter.init();

        CompatMode mode = CompatPolicyManager.getCompatMode(modId);
        if (mode.enablesAdapter()) {
            adapter.onEnable();
        }
    }

    public static CompatAdapter getAdapter(String modId) {
        return adapters.get(modId.toLowerCase());
    }

    public static Collection<CompatAdapter> getAllAdapters() {
        return adapters.values();
    }
}
