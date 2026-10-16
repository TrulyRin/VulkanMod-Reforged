package net.vulkanmod.mixin.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.chunk.WorldRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow(remap = false)
    @Final
    private RenderBuffers f_109464_;

    @Shadow(remap = false)
    @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> f_109409_;
    @Shadow(remap = false)
    private @Nullable ClientLevel f_109465_;

    @Shadow(remap = false)
    public abstract void m_173014_();

    @Shadow(remap = false)
    private int f_109438_;
    @Shadow(remap = false)
    @Final
    private Minecraft f_109461_;
    @Shadow(remap = false)
    @Final
    private Set<BlockEntity> f_109468_;
    @Shadow(remap = false)
    private boolean f_109474_;
    @Shadow(remap = false)
    @Final
    private EntityRenderDispatcher f_109463_;

    @Shadow(remap = false)
    protected abstract boolean m_109817_();

    @Shadow(remap = false)
    public abstract void m_109826_();

    @Shadow(remap = false)
    public abstract void m_109599_(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f);

    private WorldRenderer worldRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.worldRenderer = WorldRenderer.init(this.f_109464_);
    }

    @Inject(method = "m_109701_", at = @At("RETURN"), remap = false)
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        this.worldRenderer.setLevel(clientLevel);
    }

    @Inject(method = "m_109599_", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;m_109588_(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE, remap = false), remap = false)
    private void renderBlockEntities(PoseStack poseStack, float f, long l, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        this.worldRenderer.renderBlockEntities(poseStack, pos.x(), pos.y(), pos.z(), this.f_109409_, f);
    }

                                                                                                 
                                                                                                   
                                                                                                  

                  
    @Inject(method = "m_194338_", at = @At("HEAD"), cancellable = true, remap = false)
    private void setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator, CallbackInfo ci) {
        this.worldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);
        ci.cancel();
    }

                                                                                               
    @Inject(method = "m_194370_", at = @At("HEAD"), cancellable = true, remap = false)
    private void skipCompileSections(Camera camera, CallbackInfo ci) {
        ci.cancel();
    }

                        
    @Inject(method = "m_202430_", at = @At("HEAD"), cancellable = true, remap = false)
    private void isSectionCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.worldRenderer.isSectionCompiled(blockPos));
    }

                       
    @Inject(method = "m_172993_", at = @At("HEAD"), cancellable = true, remap = false)
    private void renderSectionLayer(RenderType renderType, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.worldRenderer.renderSectionLayer(renderType, poseStack, camX, camY, camZ, projectionMatrix);
        ci.cancel();
    }

                                                                                                  
                                                                                              
                                        
    @Inject(method = "m_109818_", at = @At("HEAD"), cancellable = true, remap = false)
    private void allChanged(CallbackInfo ci) {
        if (this.f_109465_ != null) {
            this.m_173014_();
            this.f_109465_.clearTintCaches();

            this.f_109474_ = true;
            ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
            this.f_109438_ = this.f_109461_.options.getEffectiveRenderDistance();
            synchronized (this.f_109468_) {
                this.f_109468_.clear();
            }
        }

        this.worldRenderer.allChanged();
        ci.cancel();
    }

                      
    @Inject(method = "m_109501_", at = @At("HEAD"), cancellable = true, remap = false)
    private void setSectionDirty(int x, int y, int z, boolean flag, CallbackInfo ci) {
        this.worldRenderer.setSectionDirty(x, y, z, flag);
        ci.cancel();
    }

                           
    @Inject(method = "m_109820_", at = @At("HEAD"), cancellable = true, remap = false)
    private void getSectionStatistics(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(this.worldRenderer.getChunkStatistics());
    }

                             
    @Inject(method = "m_109825_", at = @At("HEAD"), cancellable = true, remap = false)
    private void hasRenderedAllSections(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.worldRenderer.graphNeedsUpdate());
    }

                            
    @Inject(method = "m_109821_", at = @At("HEAD"), cancellable = true, remap = false)
    private void countRenderedSections(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.worldRenderer.getVisibleSectionsCount());
    }

                   
    @Inject(method = "m_109517_", at = @At("HEAD"), cancellable = true, remap = false)
    private void renderEntity(Entity entity, double d, double e, double f, float g, PoseStack poseStack, MultiBufferSource multiBufferSource, CallbackInfo ci) {
        double h = Mth.lerp(g, entity.xOld, entity.getX());
        double i = Mth.lerp(g, entity.yOld, entity.getY());
        double j = Mth.lerp(g, entity.zOld, entity.getZ());
        float k = Mth.lerp(g, entity.yRotO, entity.getYRot());
        this.f_109463_.render(entity, h - d, i - e, j - f, k, g, poseStack, multiBufferSource, this.f_109463_.getPackedLightCoords(entity, g));
        ci.cancel();
    }

    @Redirect(method = "m_173012_", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;m_172790_()F", remap = false), remap = false)
    private float getRenderDistanceZFar(GameRenderer instance) {
        return instance.getRenderDistance() * 4F;
    }

                                                                                                                                                                        
                                                                      
  
                                               
                       
                                        
                               
               
  
                       
                                    
                              
               
            
       
  
                                                                
                                                                                                                  
                                      
                                                    
       
                                                                                                                                                                                              
                                                                  
                                                                                                                                                
                                                             
                                                                                                                                                                                                                                                                                                                                                            
                                              
                                                  
                                                      
                                                      
                                                      
                       
  
                                                          
                                                                                                                
                                       
                                                                                                             
                                                                  
                                                        
                                      
                                                
                                               
                                           
                                                                      
                              
                                                 
                       
  
                                                                                                     
                   
               
           
       

}

