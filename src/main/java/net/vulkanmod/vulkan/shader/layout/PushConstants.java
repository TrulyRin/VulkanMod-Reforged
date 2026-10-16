package net.vulkanmod.vulkan.shader.layout;

import java.util.List;

public class PushConstants extends AlignedStruct {
    public final int stages;

    protected PushConstants(int stages, List<Uniform.Info> infoList, int size) {
        super(infoList, size);
        this.stages = stages;
    }

}
