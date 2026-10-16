package net.vulkanmod.render.culling;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

                                                                                                
public final class ParticleAdmissionTracker<T> {
    private final List<WeakReference<T>> admitted = new ArrayList<>();
    private final Map<Class<?>, Integer> counts = new HashMap<>();
    private long countedTick = Long.MIN_VALUE;
    private boolean initialized;

    public void accepted(T particle) {
        admitted.add(new WeakReference<>(particle));
        counts.merge(particle.getClass(), 1, Integer::sum);
    }

    public int countAlive(Class<?> particleType, long tick, Predicate<T> alive) {
        if (!initialized || countedTick != tick) {
            initialized = true;
            countedTick = tick;
            counts.clear();
            Iterator<WeakReference<T>> iterator = admitted.iterator();
            while (iterator.hasNext()) {
                T particle = iterator.next().get();
                if (particle == null || !alive.test(particle)) {
                    iterator.remove();
                } else {
                    counts.merge(particle.getClass(), 1, Integer::sum);
                }
            }
        }
        return counts.getOrDefault(particleType, 0);
    }

    public void clear() {
        admitted.clear();
        counts.clear();
        countedTick = Long.MIN_VALUE;
        initialized = false;
    }
}
