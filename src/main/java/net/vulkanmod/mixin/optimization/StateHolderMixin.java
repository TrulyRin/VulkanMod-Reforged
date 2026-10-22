package net.vulkanmod.mixin.optimization;

import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Mixin(StateHolder.class)
public abstract class StateHolderMixin {

    private static final ConcurrentHashMap<Map<Property<?>, Comparable<?>>, Map<Property<?>, Comparable<?>>> VALUES_MAP_CACHE = new ConcurrentHashMap<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        try {
            for (java.lang.reflect.Field field : StateHolder.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Map map = (Map) field.get(this);
                    if (map != null) {
                        Map interned = VALUES_MAP_CACHE.computeIfAbsent(map, m -> m);
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
