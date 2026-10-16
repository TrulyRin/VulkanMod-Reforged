package net.vulkanmod.render.vertex;

                                                                                          
public final class VertexFormatAlignment {
    private VertexFormatAlignment() {
    }

    public static int padding(int offset, int componentSize) {
        return (componentSize - offset % componentSize) % componentSize;
    }
}
