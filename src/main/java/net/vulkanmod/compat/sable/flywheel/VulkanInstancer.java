package net.vulkanmod.compat.sable.flywheel;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.InstanceWriter;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.model.Model;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

final class VulkanInstancer<I extends Instance> implements Instancer<I>, AutoCloseable {
    private final InstanceType<I> type;
    private final Model model;
    private final List<I> instances = new ArrayList<>();
    private final Map<I, Handle> handles = new IdentityHashMap<>();
    private final DirtyRangeTracker dirtyRanges = new DirtyRangeTracker();
    private ByteBuffer shadowStorage = MemoryUtil.memAlloc(1);

    VulkanInstancer(InstanceType<I> type, Model model) {
        this.type = type;
        this.model = model;
    }

    @Override
    public synchronized I createInstance() {
        Handle handle = new Handle(instances.size());
        I instance = type.create(handle);
        instances.add(instance);
        handles.put(instance, handle);
        resizeStorage();
        dirtyRanges.mark(handle.index * type.layout().byteSize(), (handle.index + 1) * type.layout().byteSize());
        return instance;
    }

    @Override
    public synchronized void stealInstance(I instance) {
        if (instance == null || instance.type() != type || handles.containsKey(instance)) {
            return;
        }
        Handle handle = new Handle(instances.size());
        instances.add(instance);
        handles.put(instance, handle);
        resizeStorage();
        dirtyRanges.mark(handle.index * type.layout().byteSize(), (handle.index + 1) * type.layout().byteSize());
    }

    synchronized DirtyRangeTracker.Range writeDirtyInstances() {
        InstanceWriter<I> writer = type.writer();
        int stride = type.layout().byteSize();
        for (int index = 0; index < instances.size(); index++) {
            Handle handle = handles.get(instances.get(index));
            if (handle != null && handle.changed && !handle.deleted) {
                writer.write(MemoryUtil.memAddress(shadowStorage) + (long) index * stride, instances.get(index));
                handle.changed = false;
                dirtyRanges.mark(index * stride, (index + 1) * stride);
            }
        }
        return dirtyRanges.take().orElse(null);
    }

    synchronized List<I> visibleInstances() {
        return instances.stream().filter(instance -> {
            Handle handle = handles.get(instance);
            return handle != null && handle.visible && !handle.deleted;
        }).toList();
    }

    Model model() {
        return model;
    }

    ByteBuffer shadowStorage() {
        return shadowStorage;
    }

    private void resizeStorage() {
        int required = Math.max(1, instances.size() * type.layout().byteSize());
        if (shadowStorage.capacity() >= required) {
            return;
        }
        ByteBuffer replacement = MemoryUtil.memAlloc(required);
        replacement.put(0, shadowStorage, 0, shadowStorage.capacity());
        MemoryUtil.memFree(shadowStorage);
        shadowStorage = replacement;
    }

    @Override
    public synchronized void close() {
        MemoryUtil.memFree(shadowStorage);
        instances.clear();
        handles.clear();
    }

    private final class Handle implements InstanceHandle {
        private final int index;
        private boolean changed = true;
        private boolean deleted;
        private boolean visible = true;

        private Handle(int index) {
            this.index = index;
        }

        @Override
        public void setChanged() {
            changed = true;
        }

        @Override
        public void setDeleted() {
            deleted = true;
            changed = true;
        }

        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
            changed = true;
        }

        @Override
        public boolean isVisible() {
            return visible && !deleted;
        }
    }
}
