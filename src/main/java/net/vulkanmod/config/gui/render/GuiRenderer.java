package net.vulkanmod.config.gui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

public abstract class GuiRenderer {

    public static Minecraft minecraft;
    public static GuiGraphics guiGraphics;
                                                                                      
    public static org.joml.Matrix3x2fStack pose;
    public static BufferBuilder bufferBuilder;

    public static void enableScissor(int i, int j, int k, int l) {
        guiGraphics.enableScissor(i, j, k, l);
    }

    public static void disableScissor() {
        guiGraphics.disableScissor();
    }

    public static void fillBox(int x0, int y0, int width, int height, int color) {
        fill(x0, y0, x0 + width, y0 + height, 0, color);
    }

    public static void fill(int x0, int y0, int x1, int y1, int color) {
        fill(x0, y0, x1, y1, 0, color);
    }

    public static void fill(int x0, int y0, int x1, int y1, @SuppressWarnings("unused") int z, int color) {
        guiGraphics.fill(x0, y0, x1, y1, color);
    }

    @SuppressWarnings("unused")
    public static void fillGradient(int x0, int y0, int x1, int y1, int color1, int color2) {
        fillGradient(x0, y0, x1, y1, 0, color1, color2);
    }

    public static void fillGradient(int x0, int y0, int x1, int y1, @SuppressWarnings("unused") int z, int color1, int color2) {
        guiGraphics.fillGradient(x0, y0, x1, y1, color1, color2);
    }

    public static void renderBoxBorder(int x0, int y0, int width, int height, int borderWidth, int color) {
        renderBorder(x0, y0, x0 + width, y0 + height, borderWidth, color);
    }

    public static void renderBorder(int x0, int y0, int x1, int y1, int width, int color) {
        GuiRenderer.fill(x0, y0, x1, y0 + width, color);
        GuiRenderer.fill(x0, y1 - width, x1, y1, color);

        GuiRenderer.fill(x0, y0 + width, x0 + width, y1 - width, color);
        GuiRenderer.fill(x1 - width, y0 + width, x1, y1 - width, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color) {
        drawString(font, component.getVisualOrderText(), x, y, color);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color, boolean shadow) {
        drawString(font, component.getVisualOrderText(), x, y, color, shadow);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color, boolean shadow) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color, shadow);
    }

    public static void drawCenteredString(Font font, Component component, int x, int y, int color) {
        FormattedCharSequence formattedCharSequence = component.getVisualOrderText();
        guiGraphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color);
    }

    public static void drawScrollingString(Font font, Component component, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(component);
        if (textWidth <= maxWidth) {
            drawCenteredString(font, component, x, y, color);
        } else {
            int x0 = x - maxWidth / 2, x1 = x + maxWidth / 2;
            int scrollAmount = textWidth - maxWidth;
            double currentTimeInSeconds = (double) Util.getMillis() / 1000.0;
            double scrollSpeed = Math.max(scrollAmount * 0.5, 3.0);
            double scrollingOffset = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * currentTimeInSeconds / scrollSpeed)) / 2.0 + 0.5;
            double horizontalScroll = Mth.lerp(scrollingOffset, 0.0, scrollAmount);

            enableScissor(x0 - 1, 0, x1, Minecraft.getInstance().getWindow().getScreenHeight());
            drawString(font, component, (int) (x0 - horizontalScroll), y, color);
            disableScissor();
        }
    }

    public static int getMaxTextWidth(Font font, List<FormattedCharSequence> list) {
        int maxWidth = 0;
        for (var text : list) {
            int width = font.width(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }

       
                                                                           
      
                                                                                                 
                                                                                                  
                                                                                                
                                                                                                 
                                                     
       
    public static void drawTriangles(float[][] vertices, int color) {
        for (int i = 0; i + 2 < vertices.length; i += 3) {
            fillTriangle(vertices[i], vertices[i + 1], vertices[i + 2], color);
        }
    }

    private static void fillTriangle(float[] a, float[] b, float[] c, int color) {
        int minY = (int) Math.floor(Math.min(a[1], Math.min(b[1], c[1])));
        int maxY = (int) Math.ceil(Math.max(a[1], Math.max(b[1], c[1])));

        for (int y = minY; y < maxY; ++y) {
            float sampleY = y + 0.5f;
            float left = Float.MAX_VALUE;
            float right = -Float.MAX_VALUE;

            for (int e = 0; e < 3; ++e) {
                float[] p = e == 0 ? a : e == 1 ? b : c;
                float[] q = e == 0 ? b : e == 1 ? c : a;

                                                        
                if ((p[1] <= sampleY && q[1] > sampleY) || (q[1] <= sampleY && p[1] > sampleY)) {
                    float t = (sampleY - p[1]) / (q[1] - p[1]);
                    float x = p[0] + t * (q[0] - p[0]);
                    left = Math.min(left, x);
                    right = Math.max(right, x);
                }
            }

            if (left <= right) {
                guiGraphics.fill((int) Math.floor(left), y, (int) Math.ceil(right), y + 1, color);
            }
        }
    }
}
