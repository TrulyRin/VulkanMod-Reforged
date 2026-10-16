package net.vulkanmod.config.option;

public final class OptionKeys {
    public static final OptionKey<Boolean> INDIRECT_DRAW =
            new OptionKey<>("vulkanmod:indirect_draw", Boolean.class);
    public static final OptionKey<Boolean> UNIQUE_OPAQUE_LAYER =
            new OptionKey<>("vulkanmod:unique_opaque_layer", Boolean.class);
    public static final OptionKey<Integer> RENDER_SCALE =
            new OptionKey<>("vulkanmod:render_scale", Integer.class);

    private OptionKeys() {
    }
}
