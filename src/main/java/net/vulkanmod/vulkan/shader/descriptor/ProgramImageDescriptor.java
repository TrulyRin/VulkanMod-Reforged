package net.vulkanmod.vulkan.shader.descriptor;

import net.vulkanmod.compat.gl.TextureUnitLimits;
import net.vulkanmod.gl.GlProgram;
import net.vulkanmod.gl.GlSampler;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.function.IntFunction;
import java.util.function.IntToLongFunction;


public final class ProgramImageDescriptor extends ImageDescriptor {
    @FunctionalInterface
    public interface SamplerLookup {
        long resolve(int textureUnit, VulkanImage image);
    }

    private final int program;
    private final long generation;
    private final MappedBuffer samplerValue;
    private final IntToLongFunction currentGeneration;
    private final IntFunction<VulkanImage> images;
    private final SamplerLookup samplers;

    private boolean prepared;
    private int resolvedUnit = -1;
    private VulkanImage resolvedImage;
    private long resolvedSampler;

    public ProgramImageDescriptor(int binding, String type, String name, int program, long generation,
                                  MappedBuffer samplerValue) {
        this(binding, type, name, program, generation, samplerValue, id -> {
            GlProgram.LinkedProgram linked = GlProgram.linkedProgram(id);
            return linked == null ? 0 : linked.generation();
        }, VTextureSelector::getImage, GlSampler::resolveSampler);
    }


    public ProgramImageDescriptor(int binding, String type, String name, int program, long generation,
                                  MappedBuffer samplerValue, IntToLongFunction currentGeneration,
                                  IntFunction<VulkanImage> images, SamplerLookup samplers) {
        super(binding, type, name, -1);
        if (binding < 0 || !"sampler2D".equals(type) || name == null || name.isBlank()
                || program <= 0 || generation <= 0) {
            throw new IllegalArgumentException("A program sampler requires a valid binding, name, owner and generation");
        }
        if (samplerValue == null || samplerValue.buffer == null || !samplerValue.buffer.isDirect()
                || samplerValue.buffer.capacity() < Integer.BYTES || samplerValue.ptr == 0) {
            throw new IllegalArgumentException("Program sampler " + name + " requires a packed direct integer view");
        }
        if (currentGeneration == null || images == null || samplers == null) {
            throw new IllegalArgumentException("Program sampler resource and generation lookups are required");
        }
        this.program = program;
        this.generation = generation;
        this.samplerValue = samplerValue;
        this.currentGeneration = currentGeneration;
        this.images = images;
        this.samplers = samplers;
    }


    public void refreshForDraw() {

        this.prepared = false;
        this.resolvedUnit = -1;
        this.resolvedImage = null;
        this.resolvedSampler = 0;

        if (this.currentGeneration.applyAsLong(this.program) != this.generation) {
            throw new IllegalStateException("Program sampler " + this.name + " belongs to a retired generation");
        }

        int unit = MemoryUtil.memGetInt(this.samplerValue.ptr);
        if (!TextureUnitLimits.isValid(unit)) {
            throw new IllegalStateException("Program sampler " + this.name + " has an unavailable texture unit " + unit);
        }
        VulkanImage image = this.images.apply(unit);
        if (image == null || getImageView(image) == 0) {
            throw new IllegalStateException("Program sampler " + this.name + " has no available image on unit " + unit);
        }
        long sampler = this.useSampler ? this.samplers.resolve(unit, image) : 0;
        if (this.useSampler && sampler == 0) {
            throw new IllegalStateException("Program sampler " + this.name + " has no available sampler on unit " + unit);
        }
        this.resolvedUnit = unit;
        this.resolvedImage = image;
        this.resolvedSampler = sampler;
        this.prepared = true;
    }

    @Override
    public boolean usesVanillaTextureBinding() { return false; }

    @Override
    public int getTextureUnit() {
        requirePrepared();
        return this.resolvedUnit;
    }

    @Override
    public VulkanImage getImage() {
        requirePrepared();
        return this.resolvedImage;
    }

    @Override
    public long getSampler(VulkanImage image) {
        requirePrepared();
        if (image != this.resolvedImage) {
            throw new IllegalArgumentException("Program sampler image must match this draw's cached image");
        }
        return this.resolvedSampler;
    }

    private void requirePrepared() {
        if (!this.prepared) throw new IllegalStateException("Program sampler " + this.name + " is not prepared for a draw");
    }
}
