package net.vulkanmod.mixin.render;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow(remap = false, aliases = "f_172578_") @Final private Map<String, ShaderInstance> shaders;

    @Shadow(remap = false, aliases = "f_172579_") private @Nullable static ShaderInstance positionShader;
    @Shadow(remap = false, aliases = "f_172580_") private @Nullable static ShaderInstance positionColorShader;
    @Shadow(remap = false, aliases = "f_172581_") private @Nullable static ShaderInstance positionColorTexShader;
    @Shadow(remap = false, aliases = "f_172582_") private @Nullable static ShaderInstance positionTexShader;
    @Shadow(remap = false, aliases = "f_172583_") private @Nullable static ShaderInstance positionTexColorShader;
    @Shadow(remap = false, aliases = "f_172586_") private @Nullable static ShaderInstance particleShader;
    @Shadow(remap = false, aliases = "f_172589_") private @Nullable static ShaderInstance positionTexColorNormalShader;
    @Shadow(remap = false, aliases = "f_172591_") private @Nullable static ShaderInstance rendertypeSolidShader;
    @Shadow(remap = false, aliases = "f_172608_") private @Nullable static ShaderInstance rendertypeCutoutMippedShader;
    @Shadow(remap = false, aliases = "f_172609_") private @Nullable static ShaderInstance rendertypeCutoutShader;
    @Shadow(remap = false, aliases = "f_172610_") private @Nullable static ShaderInstance rendertypeTranslucentShader;
    @Shadow(remap = false, aliases = "f_172611_") private @Nullable static ShaderInstance rendertypeTranslucentMovingBlockShader;
    @Shadow(remap = false, aliases = "f_172613_") private @Nullable static ShaderInstance rendertypeArmorCutoutNoCullShader;
    @Shadow(remap = false, aliases = "f_172614_") private @Nullable static ShaderInstance rendertypeEntitySolidShader;
    @Shadow(remap = false, aliases = "f_172615_") private @Nullable static ShaderInstance rendertypeEntityCutoutShader;
    @Shadow(remap = false, aliases = "f_172616_") private @Nullable static ShaderInstance rendertypeEntityCutoutNoCullShader;
    @Shadow(remap = false, aliases = "f_172617_") private @Nullable static ShaderInstance rendertypeEntityCutoutNoCullZOffsetShader;
    @Shadow(remap = false, aliases = "f_172618_") private @Nullable static ShaderInstance rendertypeItemEntityTranslucentCullShader;
    @Shadow(remap = false, aliases = "f_172619_") private @Nullable static ShaderInstance rendertypeEntityTranslucentCullShader;
    @Shadow(remap = false, aliases = "f_172620_") private @Nullable static ShaderInstance rendertypeEntityTranslucentShader;
    @Shadow(remap = false, aliases = "f_234217_") private @Nullable static ShaderInstance rendertypeEntityTranslucentEmissiveShader;
    @Shadow(remap = false, aliases = "f_172621_") private @Nullable static ShaderInstance rendertypeEntitySmoothCutoutShader;
    @Shadow(remap = false, aliases = "f_172622_") private @Nullable static ShaderInstance rendertypeBeaconBeamShader;
    @Shadow(remap = false, aliases = "f_172623_") private @Nullable static ShaderInstance rendertypeEntityDecalShader;
    @Shadow(remap = false, aliases = "f_172624_") private @Nullable static ShaderInstance rendertypeEntityNoOutlineShader;
    @Shadow(remap = false, aliases = "f_172625_") private @Nullable static ShaderInstance rendertypeEntityShadowShader;
    @Shadow(remap = false, aliases = "f_172626_") private @Nullable static ShaderInstance rendertypeEntityAlphaShader;
    @Shadow(remap = false, aliases = "f_172627_") private @Nullable static ShaderInstance rendertypeEyesShader;
    @Shadow(remap = false, aliases = "f_172628_") private @Nullable static ShaderInstance rendertypeEnergySwirlShader;
    @Shadow(remap = false, aliases = "f_172629_") private @Nullable static ShaderInstance rendertypeLeashShader;
    @Shadow(remap = false, aliases = "f_172630_") private @Nullable static ShaderInstance rendertypeWaterMaskShader;
    @Shadow(remap = false, aliases = "f_172631_") private @Nullable static ShaderInstance rendertypeOutlineShader;
    @Shadow(remap = false, aliases = "f_172632_") private @Nullable static ShaderInstance rendertypeArmorGlintShader;
    @Shadow(remap = false, aliases = "f_172633_") private @Nullable static ShaderInstance rendertypeArmorEntityGlintShader;
    @Shadow(remap = false, aliases = "f_172593_") private @Nullable static ShaderInstance rendertypeGlintTranslucentShader;
    @Shadow(remap = false, aliases = "f_172594_") private @Nullable static ShaderInstance rendertypeGlintShader;
    @Shadow(remap = false, aliases = "f_172595_") private @Nullable static ShaderInstance rendertypeGlintDirectShader;
    @Shadow(remap = false, aliases = "f_172596_") private @Nullable static ShaderInstance rendertypeEntityGlintShader;
    @Shadow(remap = false, aliases = "f_172597_") private @Nullable static ShaderInstance rendertypeEntityGlintDirectShader;
    @Shadow(remap = false, aliases = "f_172598_") private @Nullable static ShaderInstance rendertypeTextShader;
    @Shadow(remap = false, aliases = "f_172599_") private @Nullable static ShaderInstance rendertypeTextIntensityShader;
    @Shadow(remap = false, aliases = "f_172600_") private @Nullable static ShaderInstance rendertypeTextSeeThroughShader;
    @Shadow(remap = false, aliases = "f_172601_") private @Nullable static ShaderInstance rendertypeTextIntensitySeeThroughShader;
    @Shadow(remap = false, aliases = "f_172602_") private @Nullable static ShaderInstance rendertypeLightningShader;
    @Shadow(remap = false, aliases = "f_172603_") private @Nullable static ShaderInstance rendertypeTripwireShader;
    @Shadow(remap = false, aliases = "f_172604_") private @Nullable static ShaderInstance rendertypeEndPortalShader;
    @Shadow(remap = false, aliases = "f_172605_") private @Nullable static ShaderInstance rendertypeEndGatewayShader;
    @Shadow(remap = false, aliases = "f_172606_") private @Nullable static ShaderInstance rendertypeLinesShader;
    @Shadow(remap = false, aliases = "f_172607_") private @Nullable static ShaderInstance rendertypeCrumblingShader;

    @Shadow(remap = false, aliases = "f_268423_") private static @Nullable ShaderInstance rendertypeTextBackgroundShader;
    @Shadow(remap = false, aliases = "f_268525_") private static @Nullable ShaderInstance rendertypeTextBackgroundSeeThroughShader;
    @Shadow(remap = false, aliases = "f_285653_") private static @Nullable ShaderInstance rendertypeGuiShader;
    @Shadow(remap = false, aliases = "f_285598_") private static @Nullable ShaderInstance rendertypeGuiOverlayShader;
    @Shadow(remap = false, aliases = "f_285623_") private static @Nullable ShaderInstance rendertypeGuiTextHighlightShader;
    @Shadow(remap = false, aliases = "f_285569_") private static @Nullable ShaderInstance rendertypeGuiGhostRecipeOverlayShader;

    @Shadow(remap = false, aliases = "f_172587_") private @Nullable static ShaderInstance positionColorLightmapShader;
    @Shadow(remap = false, aliases = "f_172588_") private @Nullable static ShaderInstance positionColorTexLightmapShader;
    @Shadow(remap = false, aliases = "f_172590_") private @Nullable static ShaderInstance positionTexLightmapColorShader;

    @Shadow(remap = false) public ShaderInstance f_172635_;

    @Shadow(remap = false)
    private ShaderInstance m_172724_(ResourceProvider resourceProvider, String string, VertexFormat vertexFormat) {
        throw new AssertionError();
    }

    @Inject(method = "m_172767_", at = @At("HEAD"), cancellable = true, remap = false)
    public void reloadShaders(ResourceProvider provider, CallbackInfo ci) throws IOException {
        RenderSystem.assertOnRenderThread();

        List<Pair<ShaderInstance, Consumer<ShaderInstance>>> shaders = Lists.newArrayListWithCapacity(this.shaders.size());

        try {
            shaders.add(Pair.of(new ShaderInstance(provider, "particle", DefaultVertexFormat.PARTICLE), (shaderInstance) -> {
                particleShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position", DefaultVertexFormat.POSITION), (shaderInstance) -> {
                positionShader = shaderInstance;
            }));

            ShaderInstance positionColor = new ShaderInstance(provider, "position_color", DefaultVertexFormat.POSITION_COLOR);
            shaders.add(Pair.of(positionColor, (shaderInstance) -> positionColorShader = shaderInstance));
                                                                                                                                                             
                                                              
                  
            shaders.add(Pair.of(new ShaderInstance(provider, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX), (shaderInstance) -> {
                positionColorTexShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_color_tex_lightmap", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
               positionColorTexLightmapShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_tex", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                positionTexShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR), (shaderInstance) -> {
                positionTexColorShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "position_tex_color_normal", DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), (shaderInstance) -> {
                positionTexColorNormalShader = shaderInstance;
            }));
                                                                                                                                                                     
                                                                 
                  
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_solid", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeSolidShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_cutout_mipped", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeCutoutMippedShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_cutout", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeCutoutShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_translucent", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeTranslucentShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_translucent_moving_block", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeTranslucentMovingBlockShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeArmorCutoutNoCullShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_solid", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntitySolidShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_cutout", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityCutoutShader = shaderInstance;
            }));

                                      
            ShaderInstance entity_no_cull = new ShaderInstance(provider, "rendertype_entity_cutout_no_cull", DefaultVertexFormat.NEW_ENTITY);
            shaders.add(Pair.of(entity_no_cull, (shaderInstance) -> {
                rendertypeEntityCutoutNoCullShader = shaderInstance;
            }));
                                                                                                                                                                                      
                                                                       
                  
            shaders.add(Pair.of(entity_no_cull, (shaderInstance) -> {
                rendertypeEntityCutoutNoCullZOffsetShader = shaderInstance;
            }));

            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_item_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeItemEntityTranslucentCullShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_cull", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityTranslucentCullShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityTranslucentShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_translucent_emissive", DefaultVertexFormat.NEW_ENTITY), shader -> {
                rendertypeEntityTranslucentEmissiveShader = shader;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_smooth_cutout", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntitySmoothCutoutShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_beacon_beam", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeBeaconBeamShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_decal", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityDecalShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_no_outline", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityNoOutlineShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_shadow", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityShadowShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_alpha", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEntityAlphaShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_eyes", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEyesShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_energy_swirl", DefaultVertexFormat.NEW_ENTITY), (shaderInstance) -> {
                rendertypeEnergySwirlShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_leash", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (shaderInstance) -> {
                rendertypeLeashShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_water_mask", DefaultVertexFormat.POSITION), (shaderInstance) -> {
                rendertypeWaterMaskShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_outline", DefaultVertexFormat.POSITION_COLOR_TEX), (shaderInstance) -> {
                rendertypeOutlineShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeArmorGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_armor_entity_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeArmorEntityGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_glint_translucent", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeGlintTranslucentShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_glint_direct", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeGlintDirectShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_glint", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeEntityGlintShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_entity_glint_direct", DefaultVertexFormat.POSITION_TEX), (shaderInstance) -> {
                rendertypeEntityGlintDirectShader = shaderInstance;
            }));

                  
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_background", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextBackgroundShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_intensity", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextIntensityShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextSeeThroughShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_background_see_through", DefaultVertexFormat.POSITION_COLOR_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextBackgroundSeeThroughShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_text_intensity_see_through", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP), (shaderInstance) -> {
                rendertypeTextIntensitySeeThroughShader = shaderInstance;
            }));

            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_lightning", DefaultVertexFormat.POSITION_COLOR), (shaderInstance) -> {
                rendertypeLightningShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_tripwire", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeTripwireShader = shaderInstance;
            }));
            ShaderInstance endPortalShader = new ShaderInstance(provider, "rendertype_end_portal", DefaultVertexFormat.POSITION);
            shaders.add(Pair.of(endPortalShader, (shaderInstance) -> {
                rendertypeEndPortalShader = shaderInstance;
            }));
            shaders.add(Pair.of(endPortalShader, (shaderInstance) -> {
                rendertypeEndGatewayShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_lines", DefaultVertexFormat.POSITION_COLOR_NORMAL), (shaderInstance) -> {
                rendertypeLinesShader = shaderInstance;
            }));
            shaders.add(Pair.of(new ShaderInstance(provider, "rendertype_crumbling", DefaultVertexFormat.BLOCK), (shaderInstance) -> {
                rendertypeCrumblingShader = shaderInstance;
            }));

            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiShader = shaderInstance;
            }));
            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiOverlayShader = shaderInstance;
            }));
            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiTextHighlightShader = shaderInstance;
            }));
            shaders.add(Pair.of(positionColor, (shaderInstance) -> {
                rendertypeGuiGhostRecipeOverlayShader = shaderInstance;
            }));

            net.minecraftforge.fml.ModLoader.get().postEvent(new net.minecraftforge.client.event.RegisterShadersEvent(provider, shaders));
        } catch (IOException ioexception) {
            shaders.forEach((pair) -> pair.getFirst().close());
            throw new RuntimeException("could not reload shaders", ioexception);
        }

        this.m_172759_();
        shaders.forEach((pair) -> {
            ShaderInstance shaderinstance = pair.getFirst();
            this.shaders.put(shaderinstance.getName(), shaderinstance);
            pair.getSecond().accept(shaderinstance);
        });

        ci.cancel();
    }

       
              
              
       
    @Overwrite(remap = false)
    private void m_172759_() {
        RenderSystem.assertOnRenderThread();

        final var clearList = ImmutableList.copyOf(this.shaders.values());
        MemoryManager.getInstance().addFrameOp(() -> clearList.forEach((ShaderInstance::close)));

        this.shaders.clear();
    }

       
              
              
       
    @Overwrite(remap = false)
    public void m_172722_(ResourceProvider resourceProvider) {
        if (this.f_172635_ != null) {
            throw new RuntimeException("Blit shader already preloaded");
        } else {
            try {
                this.f_172635_ = new ShaderInstance(resourceProvider, "blit_screen", DefaultVertexFormat.POSITION_TEX);
            } catch (IOException var3) {
                throw new RuntimeException("could not preload blit shader", var3);
            }

            positionShader = this.m_172724_(resourceProvider, "position", DefaultVertexFormat.POSITION);
            positionColorShader = this.m_172724_(resourceProvider, "position_color", DefaultVertexFormat.POSITION_COLOR);
            positionColorTexShader = this.m_172724_(resourceProvider, "position_color_tex", DefaultVertexFormat.POSITION_COLOR_TEX);
            positionTexShader = this.m_172724_(resourceProvider, "position_tex", DefaultVertexFormat.POSITION_TEX);
            positionTexColorShader = this.m_172724_(resourceProvider, "position_tex_color", DefaultVertexFormat.POSITION_TEX_COLOR);
            rendertypeTextShader = this.m_172724_(resourceProvider, "rendertype_text", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

            rendertypeGuiShader = positionColorShader;
            rendertypeGuiOverlayShader = positionColorShader;
        }
    }

       
              
              
       
    @Overwrite(remap = false)
    public float m_172790_() {
        return Float.POSITIVE_INFINITY;
    }

}

