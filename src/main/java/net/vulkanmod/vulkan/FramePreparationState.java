package net.vulkanmod.vulkan;

                                                                               
final class FramePreparationState {
    private int preparedFrame = -1;
    private int lastResetFrame = -1;

    boolean needsPreparation(int frame, boolean recording) {
        return !recording && preparedFrame != frame;
    }

    boolean needsIdleWait(int frame) {
        return lastResetFrame == frame;
    }

    void preparationCompleted(int frame) {
        preparedFrame = frame;
        lastResetFrame = frame;
    }

    void submitted() {
        preparedFrame = -1;
    }

    void recreated() {
        preparedFrame = -1;
        lastResetFrame = -1;
    }
}
