package net.vulkanmod.mixin.wayland;

import com.mojang.blaze3d.platform.IconSet;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.vulkanmod.config.Platform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow(remap = false) @Final private Window f_90990_;
    @Shadow(remap = false) @Final private VanillaPackResources f_243783_;

       
              
                                                                   
       
    @Redirect(method="<init>", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/platform/Window;setIcon(Lnet/minecraft/server/packs/PackResources;Lcom/mojang/blaze3d/platform/IconSet;)V"))
    private void bypassWaylandIcon(Window instance, PackResources packResources, IconSet iconSet) throws IOException {
        if(!Platform.isWayLand())
        {
            this.f_90990_.setIcon(this.f_243783_, SharedConstants.getCurrentVersion().isStable() ? IconSet.RELEASE : IconSet.SNAPSHOT);
        }
    }
}

