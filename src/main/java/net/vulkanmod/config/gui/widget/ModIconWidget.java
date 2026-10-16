package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.vulkan.util.ColorUtil;

public class ModIconWidget extends VAbstractWidget {
    final FormattedText name;
    final Identifier icon;

    public ModIconWidget(FormattedText name, Identifier icon, int x0, int y0, int width, int height) {
        this.name = name;
        this.icon = icon;
        this.x = x0;
        this.y = y0;
        this.width = width;
        this.height = height;
    }

    public void render(double mX, double mY) {
        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.6f);
        int width = this.width;
        int height = this.height;
        GuiRenderer.fill(this.x, this.y, this.x + width, this.y + height, backgroundColor);


        int size = this.height - 4;
        int iconX = this.x + 4;
        int iconY = this.y + (height - size) / 2;
                                                                                           
        GuiRenderer.guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                                     icon, iconX, iconY, 0.0f, 0.0f, size, size, size, size);

        size = this.height;
                                                                                    
                                                     
        int textX = this.x + 6 + size;
        int availableWidth = this.x + this.width - textX - 4;
        GuiRenderer.drawScrollingString(Minecraft.getInstance().font, (Component) this.name,
                textX + availableWidth / 2, this.y + this.height / 2 - 4, availableWidth, 0xffffffff);
    }
}
