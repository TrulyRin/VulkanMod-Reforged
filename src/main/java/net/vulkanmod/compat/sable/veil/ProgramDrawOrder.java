package net.vulkanmod.compat.sable.veil;

import java.util.Objects;


final class ProgramDrawOrder<S, F, P, M> {
    interface Backend<S, F, P, M> {
        int owner(S shader);
        long generation(int owner);
        void apply(S shader);
        void uniforms(S shader, M modelView, M projection);
        P pipeline(S shader, F format);
        boolean resources(S shader, P pipeline);
    }
    private final Backend<S, F, P, M> backend;
    ProgramDrawOrder(Backend<S, F, P, M> backend) { this.backend = Objects.requireNonNull(backend); }
    P prepare(S shader, F format, boolean apply) {
        return prepare(shader, format, apply, null, null);
    }
    P prepare(S shader, F format, boolean apply, M modelView, M projection) {
        if (shader == null || format == null) return null;
        int owner = backend.owner(shader);
        if (owner <= 0) return null;
        long generation = backend.generation(owner);
        if (generation <= 0) return null;
        if (apply) backend.apply(shader);
        if (!current(shader, owner, generation)) return null;
        backend.uniforms(shader, modelView, projection);
        if (!current(shader, owner, generation)) return null;
        P pipeline = backend.pipeline(shader, format);
        if (pipeline == null || !current(shader, owner, generation)) return null;
        if (!backend.resources(shader, pipeline) || !current(shader, owner, generation)) return null;
        return pipeline;
    }
    private boolean current(S shader, int owner, long generation) {
        return backend.owner(shader) == owner && backend.generation(owner) == generation;
    }
}
