package net.vulkanmod.config.gui.widget;

import net.minecraft.client.input.CharacterEvent;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class VButtonWidget extends VAbstractWidget {
    boolean selected = false;
    Consumer<VButtonWidget> onPress;

    float alpha = 1.0f;

    public VButtonWidget(int x, int y, int width, int height, Component message, Consumer<VButtonWidget> onPress) {
        this.setPosition(x, y, width, height);

        this.message = message;
        this.onPress = onPress;
    }

    public void renderWidget(double mouseX, double mouseY) {
        if (!this.isVisible()) return;

        int backgroundColor = this.isActive()
                ? ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.45f)
                : ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.3f);
        int textColor = this.isActive()
                ? VGuiConstants.COLOR_WHITE
                : VGuiConstants.COLOR_GRAY;
                                     
        int selectionOutlineColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.8f);
        int selectionFillColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.2f);

        GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);

        if (this.selected) {
            GuiRenderer.fill(this.x, this.y, this.x + 2, this.y + this.height, selectionOutlineColor);
            GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, selectionFillColor);
        }

        if (this.focused) {
            GuiRenderer.renderBorder(this.x, this.y, this.x + this.width, this.y + this.height,
                    1, selectionOutlineColor);
        }

        this.renderHovering(0, 0);

                                                
        if (this.centeredText) {
            GuiRenderer.drawCenteredString(
                    Minecraft.getInstance().font,
                    this.message,
                    this.x + this.width / 2, (this.y + this.height / 2) - 4,
                    textColor | (Mth.ceil(this.alpha * 255.0f) << 24));
        }
        else {
            GuiRenderer.drawString(Minecraft.getInstance().font,
                                   this.message,
                                   this.x + this.margin, (this.y + this.height / 2) - 4,
                                   textColor | (Mth.ceil(this.alpha * 255.0f) << 24));
        }

    }

    public void onClick(double mX, double mY) {
        this.onPress.accept(this);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key(); int scanCode = event.scancode(); int modifiers = event.modifiers();
        if (!this.active || !this.visible) {
            return false;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER
                || keyCode == GLFW.GLFW_KEY_SPACE) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress.accept(this);
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (!this.visible) {
            return NarrationPriority.NONE;
        }
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        }
        if (this.hovered) {
            return NarrationPriority.HOVERED;
        }
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        if (!this.visible) {
            return;
        }
        output.add(NarratedElementType.TITLE, this.message);
        if (!this.active) {
            output.add(NarratedElementType.USAGE,
                    Component.translatable("vulkanmod.options.narration.disabled"));
        } else if (this.focused) {
            output.add(NarratedElementType.USAGE,
                    Component.translatable("narration.button.usage.focused"));
        } else if (this.hovered) {
            output.add(NarratedElementType.USAGE,
                    Component.translatable("narration.button.usage.hovered"));
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.active || !this.visible)
            return null;
        return ComponentPath.leaf(this);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.x, this.y, this.width, this.height);
    }

}
