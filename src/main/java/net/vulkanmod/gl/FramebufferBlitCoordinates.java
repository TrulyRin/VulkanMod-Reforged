package net.vulkanmod.gl;


public final class FramebufferBlitCoordinates {
    private FramebufferBlitCoordinates() {}

    public static int physicalY(int glY, int height, boolean rowsBottomUp) {
        return rowsBottomUp ? glY : height - glY;
    }
}
