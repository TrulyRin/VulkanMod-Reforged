package net.vulkanmod.compat.external;

import java.util.Locale;

public final class ExternalRenderPathSupport {
    public static final String MODE_PROPERTY = "vulkanmod.compat.externalLod";
    public static final String DRAW_PROPERTY = "vulkanmod.compat.externalLod.draw";

    private static final Mode MODE = Mode.fromProperty(System.getProperty(MODE_PROPERTY));

    private ExternalRenderPathSupport() {
    }

    public static Mode mode() {
        return MODE;
    }

    public static boolean isExternalLodBridgeEnabled() {
        return MODE == Mode.EXPERIMENTAL;
    }

    public static boolean shouldDrawExternalLodBridge() {
        return shouldDrawExternalLodBridge(MODE, System.getProperty(DRAW_PROPERTY));
    }

    static boolean shouldDrawExternalLodBridge(Mode mode, String drawProperty) {
        return mode == Mode.EXPERIMENTAL && Boolean.parseBoolean(drawProperty == null ? "true" : drawProperty);
    }

    public static boolean shouldDrawExternalLodBridgeDirectlyToMainFramebuffer() {
        return shouldDrawExternalLodBridgeDirectlyToMainFramebuffer(MODE, System.getProperty(DRAW_PROPERTY));
    }

    static boolean shouldDrawExternalLodBridgeDirectlyToMainFramebuffer(Mode mode, String drawProperty) {
        return shouldDrawExternalLodBridge(mode, drawProperty);
    }

    public static boolean shouldSkipExternalLodApplyPass() {
        return shouldSkipExternalLodApplyPass(MODE);
    }

    static boolean shouldSkipExternalLodApplyPass(Mode mode) {
        return mode == Mode.SAFE || mode == Mode.EXPERIMENTAL;
    }

    public static boolean shouldBypassExternalRenderer() {
        return shouldBypassExternalRenderer(MODE);
    }

    static boolean shouldBypassExternalRenderer(Mode mode) {
        return mode == Mode.SAFE;
    }

    public static boolean shouldApplyMixin() {
        return shouldApplyMixin(MODE);
    }

    static boolean shouldApplyMixin(Mode mode) {
        return mode != Mode.OFF;
    }

    public static boolean shouldCreateLodPipeline() {
        return MODE == Mode.EXPERIMENTAL;
    }

    public static boolean shouldCreateExternalLodPipeline() {
        return shouldCreateLodPipeline();
    }

    public enum Mode {
        SAFE,
        EXPERIMENTAL,
        OFF;

        public static Mode fromProperty(String value) {
            if (value == null || value.isBlank()) {
                return OFF;
            }

            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "experimental", "bridge", "render", "on", "true" -> EXPERIMENTAL;
                case "off", "disabled", "disable", "false", "none" -> OFF;
                case "safe", "bypass", "compat", "compatibility" -> SAFE;
                default -> OFF;
            };
        }
    }
}
