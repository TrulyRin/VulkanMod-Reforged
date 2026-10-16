package net.vulkanmod.mixin.render;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow private @Nullable PostChain entityEffect;

    @Shadow @Final private Minecraft minecraft;

    @Shadow @Nullable private RenderTarget entityTarget;

    @Shadow @Final private static Logger LOGGER;

         
                
         
                
                                 
                                          
                                        
           
  
                                                                                                         
    
                 
                                                                                                                                                                                   
                                                                                                                        
                                                                             
                                        
                                                                                 
                                         
                                         
                                                
                                                                                  
                                         
                                         
             
       

}