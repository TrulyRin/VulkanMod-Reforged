package net.vulkanmod.config.video;

                                                                                       
public final class WindowVideoModePolicy {
    private WindowVideoModePolicy() { }

    public static VideoModeSet.VideoMode resolveExclusiveMode(VideoModeSet.VideoMode requested,
                                                               VideoModeSet[] supported,
                                                               VideoModeSet.VideoMode desktop) {
        if (isSupported(requested, supported)) {
            return requested;
        }
        if (isSupported(desktop, supported)) {
            return desktop;
        }
        if (supported != null) {
            for (int i = supported.length - 1; i >= 0; i--) {
                VideoModeSet set = supported[i];
                if (set != null && set.width > 0 && set.height > 0 && !set.getRefreshRates().isEmpty()) {
                    VideoModeSet.VideoMode fallback = set.getVideoMode();
                    if (fallback.refreshRate > 0) {
                        return fallback;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSupported(VideoModeSet.VideoMode mode, VideoModeSet[] supported) {
        if (mode == null || mode.width <= 0 || mode.height <= 0 || mode.refreshRate <= 0 || supported == null) {
            return false;
        }
        for (VideoModeSet set : supported) {
            if (set != null && set.width == mode.width && set.height == mode.height
                    && set.bitDepth == mode.bitDepth && set.hasRefreshRate(mode.refreshRate)) {
                return true;
            }
        }
        return false;
    }
}
