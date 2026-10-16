package net.vulkanmod.vulkan;

import com.mojang.blaze3d.buffers.GpuFence;
import net.vulkanmod.vulkan.util.VkResult;
import org.lwjgl.vulkan.VkDevice;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

                                                                                                 
public final class SubmissionFenceTracker {
    private final Map<Long, Generation> generations = new HashMap<>();
    private Generation lastSubmitted;
    private PendingMarker pending;

    public GpuFence createFence(boolean recordingOrPendingUploads, Runnable blockingCompletion) {
        if (!recordingOrPendingUploads && lastSubmitted != null) {
            return new FenceView(lastSubmitted);
        }
        if (pending == null) {
            pending = new PendingMarker(blockingCompletion);
        }
        return new FenceView(pending);
    }

                                                               
    public void requireRetiredGeneration(long fence) {
        Generation previous = generations.get(fence);
        if (previous != null && !previous.isCompleted()) {
            throw new IllegalStateException("Cannot reset an incomplete GPU fence generation");
        }
    }

                                                           
    public void submitted(VkDevice device, long fence) {
        Generation generation = new Generation(device, fence);
        generations.put(fence, generation);
        lastSubmitted = generation;
        if (pending != null) {
            pending.generation = generation;
            pending = null;
        }
    }

                                                                                              
    public void completed(long fence) {
        Generation generation = generations.get(fence);
        if (generation != null) {
            generation.complete();
        }
    }

                                                                                               
    public void completeAll() {
        generations.values().forEach(Generation::complete);
        if (pending != null) {
            pending.completed = true;
            pending = null;
        }
    }

                                                                                
    public void retireNativeFences() {
        completeAll();
        generations.clear();
    }

    private interface Completion {
        boolean await(long timeout);
    }

    private static final class Generation implements Completion {
        private final VkDevice device;
        private final long fence;
        private boolean completed;

        private Generation(VkDevice device, long fence) {
            this.device = device;
            this.fence = fence;
        }

        synchronized boolean isCompleted() { return completed; }

        synchronized void complete() { completed = true; }

        @Override
        public synchronized boolean await(long timeout) {
            if (completed) {
                return true;
            }
            int result = timeout == 0
                    ? vkGetFenceStatus(device, fence)
                    : vkWaitForFences(device, fence, true, timeout);
            if (result == VK_SUCCESS) {
                completed = true;
                return true;
            }
            if (result == VK_NOT_READY || result == VK_TIMEOUT) {
                return false;
            }
            throw new IllegalStateException("Failed to complete GPU fence: " + VkResult.decode(result));
        }
    }

    private final class PendingMarker implements Completion {
        private final Runnable blockingCompletion;
        private volatile Generation generation;
        private volatile boolean completed;

        private PendingMarker(Runnable blockingCompletion) {
            this.blockingCompletion = blockingCompletion;
        }

        @Override
        public boolean await(long timeout) {
            if (completed) {
                return true;
            }
            Generation submitted = generation;
            if (submitted != null) {
                return submitted.await(timeout);
            }
                                                                                    
                                                                                 
                                                                                         
            if (timeout != Long.MAX_VALUE && timeout != -1L) {
                return false;
            }
            blockingCompletion.run();
            submitted = generation;
            if (submitted != null) {
                return submitted.await(timeout);
            }
                                                                                     
            completed = true;
            if (pending == this) {
                pending = null;
            }
            return true;
        }
    }

    private static final class FenceView implements GpuFence {
        private final Completion completion;
        private volatile boolean closed;

        private FenceView(Completion completion) { this.completion = completion; }

        @Override
        public void close() {
                                                                          
            closed = true;
        }

        @Override
        public boolean awaitCompletion(long timeout) {
            return closed || completion.await(timeout);
        }
    }
}
