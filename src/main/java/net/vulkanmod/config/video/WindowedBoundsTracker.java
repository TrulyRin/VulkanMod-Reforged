package net.vulkanmod.config.video;

                                                                                       
public final class WindowedBoundsTracker {
    public record Bounds(int x, int y, int width, int height) { }

    private Bounds normalBounds;

    public void enter(int x, int y, int width, int height) {
        if (normalBounds == null) {
            normalBounds = new Bounds(x, y, Math.max(1, width), Math.max(1, height));
        }
    }

    public boolean isActive() {
        return normalBounds != null;
    }

    public Bounds restore() {
        Bounds bounds = normalBounds;
        normalBounds = null;
        return bounds;
    }
}
