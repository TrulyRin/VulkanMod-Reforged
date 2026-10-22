package net.vulkanmod.mixin.compatibility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.vulkanmod.vulkan.Renderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mixin(value = PostChain.class, priority = 900)
public abstract class PostChainM {

    @Shadow private int screenWidth;
    @Shadow private int screenHeight;

    @Shadow @Final private Map<String, RenderTarget> customRenderTargets;
    @Shadow @Final private RenderTarget screenTarget;
    @Shadow @Final private List<PostPass> passes;

    @Shadow private float lastStamp;
    @Shadow private float time;

    @Shadow public abstract void addTempTarget(String string, int i, int j);
    @Shadow protected abstract void parseTargetNode(JsonElement jsonElement) throws ChainedJsonException;
    @Shadow protected abstract void parseUniformNode(JsonElement jsonElement) throws ChainedJsonException;

    @Shadow protected abstract void setFilterMode(int i);

    @Inject(method = "process", at = @At("HEAD"), cancellable = true)
    private void process(float f, CallbackInfo ci) {
        ci.cancel();

        if (f < this.lastStamp) {
            this.time += 1.0F - this.lastStamp;
            this.time += f;
        } else {
            this.time += f - this.lastStamp;
        }

        this.lastStamp = f;

        while(this.time > 20.0F) {
            this.time -= 20.0F;
        }

        int filterMode = 9728;

        for(PostPass postPass : this.passes) {
            int passFilterMode = postPass.getFilterMode();
            if (filterMode != passFilterMode) {
                this.setFilterMode(passFilterMode);
                filterMode = passFilterMode;
            }

            postPass.process(this.time / 20.0F);
        }

        this.setFilterMode(9728);

        Renderer.resetViewport();
    }

}
