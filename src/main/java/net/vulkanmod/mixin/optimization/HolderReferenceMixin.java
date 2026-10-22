package net.vulkanmod.mixin.optimization;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Holder.Reference.class)
public abstract class HolderReferenceMixin<T> {

    private static final ConcurrentHashMap<Set<?>, Set<?>> TAG_SET_CACHE = new ConcurrentHashMap<>();

    @Inject(method = "bindTags", at = @At("TAIL"))
    private void onBindTags(Collection<TagKey<T>> collection, CallbackInfo ci) {
        try {
            for (java.lang.reflect.Field field : Holder.Reference.class.getDeclaredFields()) {
                if (Set.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Set<?> set = (Set<?>) field.get(this);
                    if (set != null) {
                        Set<?> interned = TAG_SET_CACHE.computeIfAbsent(set, s -> s);
                        field.set(this, interned);
                    }
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
