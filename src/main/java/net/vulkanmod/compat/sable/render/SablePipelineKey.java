package net.vulkanmod.compat.sable.render;

import java.util.Objects;

public record SablePipelineKey(Object shaderIdentity,
                               Object vertexFormatIdentity,
                               String topology,
                               int renderStateKey,
                               int framebufferFormat,
                               int sampleCount,
                               String shaderVariant) {
    public SablePipelineKey {
        Objects.requireNonNull(shaderIdentity, "shaderIdentity");
        Objects.requireNonNull(vertexFormatIdentity, "vertexFormatIdentity");
        Objects.requireNonNull(topology, "topology");
        Objects.requireNonNull(shaderVariant, "shaderVariant");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SablePipelineKey key)) {
            return false;
        }
        return shaderIdentity == key.shaderIdentity
                && vertexFormatIdentity == key.vertexFormatIdentity
                && renderStateKey == key.renderStateKey
                && framebufferFormat == key.framebufferFormat
                && sampleCount == key.sampleCount
                && topology.equals(key.topology)
                && shaderVariant.equals(key.shaderVariant);
    }

    @Override
    public int hashCode() {
        int result = System.identityHashCode(shaderIdentity);
        result = 31 * result + System.identityHashCode(vertexFormatIdentity);
        result = 31 * result + topology.hashCode();
        result = 31 * result + renderStateKey;
        result = 31 * result + framebufferFormat;
        result = 31 * result + sampleCount;
        return 31 * result + shaderVariant.hashCode();
    }
}
