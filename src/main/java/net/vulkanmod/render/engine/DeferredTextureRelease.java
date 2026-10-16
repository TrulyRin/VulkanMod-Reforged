package net.vulkanmod.render.engine;

   
                                                                                 
                                                                              
                                               
   
final class DeferredTextureRelease {
    private int views;
    private boolean closeRequested;
    private boolean released;

    synchronized void addView() {
        if (closeRequested) {
            throw new IllegalStateException("Cannot create a view of a closed texture");
        }
        views++;
    }

    synchronized boolean removeView() {
        if (views == 0) {
            throw new IllegalStateException("Texture view count is already zero");
        }
        views--;
        return markReleasedIfReady();
    }

    synchronized boolean requestClose() {
        if (closeRequested) {
            return false;
        }
        closeRequested = true;
        return markReleasedIfReady();
    }

    synchronized int viewCount() {
        return views;
    }

    private boolean markReleasedIfReady() {
        if (closeRequested && views == 0 && !released) {
            released = true;
            return true;
        }
        return false;
    }
}
