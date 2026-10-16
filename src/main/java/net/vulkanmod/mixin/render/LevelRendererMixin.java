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

    @Shadow(remap = false) private @Nullable PostChain f_109412_;

    @Shadow(remap = false) @Final private Minecraft f_109461_;

    @Shadow(remap = false) @Nullable private RenderTarget f_109411_;

    @Shadow(remap = false) @Final private static Logger f_109453_;

         
                
         
                
                                 
                                          
                                        
           
  
                                                                                                         
    
                 
                                                                                                                                                                                   
                                                                                                                        
                                                                             
                                        
                                                                                 
                                         
                                         
                                                
                                                                                  
                                         
                                         
             
       

}
