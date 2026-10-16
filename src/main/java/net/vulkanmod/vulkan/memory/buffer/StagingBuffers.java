package net.vulkanmod.vulkan.memory.buffer;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class StagingBuffers {
    final Stack<StagingBuffer> availableBuffers = new ObjectArrayList<>();

    ObjectArrayList<StagingBuffer>[] usedBuffersByFrame;
    StagingBuffer currentBuffer;
    boolean inFrame = false;

    public void updateFrameCount(int frames) {
                                                      
                                            
        if (usedBuffersByFrame != null) {
            for (var bufferList : usedBuffersByFrame) {
                for (var buffer : bufferList) {
                    availableBuffers.push(buffer);
                }
            }
        }
        else {
            for (int i = 0; i < frames + 1; i++) {
                availableBuffers.push(new StagingBuffer());
            }
        }

        currentBuffer = null;

        usedBuffersByFrame = new ObjectArrayList[frames];

        for (int i = 0; i < frames; i++) {
            usedBuffersByFrame[i] = new ObjectArrayList<>();
        }
    }

    public StagingBuffer getStagingBuffer() {
        if (currentBuffer == null) {
            if (availableBuffers.isEmpty()) {
                availableBuffers.push(new StagingBuffer());
            }

            currentBuffer = availableBuffers.pop();
        }

        if (!inFrame)
            System.nanoTime();

        return currentBuffer;
    }

    public void beginFrame(int frame) {
        var usedBuffers = usedBuffersByFrame[frame];
        for (StagingBuffer buffer : usedBuffers) {
            buffer.reset();
            availableBuffers.push(buffer);
        }

        usedBuffers.clear();

                             
        if (currentBuffer != null) {
            usedBuffers.push(currentBuffer);
        }

        if (availableBuffers.isEmpty()) {
            availableBuffers.push(new StagingBuffer());
        }

        currentBuffer = availableBuffers.pop();
        inFrame = true;
    }

    public void endFrame(int frame) {
        usedBuffersByFrame[frame].push(this.currentBuffer);

        currentBuffer = null;
        inFrame = false;
    }

    public void free() {
        for (var bufferList : usedBuffersByFrame) {
            for (var buffer : bufferList) {
                buffer.scheduleFree();
            }
        }

        while (!availableBuffers.isEmpty()) {
            availableBuffers.pop().scheduleFree();
        }
    }


}
