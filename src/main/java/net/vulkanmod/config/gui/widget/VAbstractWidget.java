package net.vulkanmod.config.gui.widget;

import net.minecraft.client.input.CharacterEvent;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.vulkanmod.config.gui.GuiElement;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.option.PerformanceImpact;
import net.vulkanmod.vulkan.util.ColorUtil;

public abstract class VAbstractWidget extends GuiElement {
    public boolean active = true;
    public boolean visible = true;
    public boolean focused;

    protected Component message;
    protected boolean centeredText = true;
    protected int margin = 4;

    public void setDimensions(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setTextLayout(boolean centered, int margin) {
        this.centeredText = centered;
        this.margin = margin;
    }

    public void render(double mX, double mY) {
        this.updateState(mX, mY);
        this.renderWidget(mX, mY);
    }

    public void renderWidget(double mX, double mY) {
    }

    public void onClick(double mX, double mY) {
    }

    public void onRelease(double mX, double mY) {
    }

    protected void onDrag(double mX, double mY, double f, double g) {
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    protected void renderHovering(int xPadding, int yPadding) {
        if (this.isFocused() || !this.isActive() || !this.visible || this.focused)
            return;

        float hoverMultiplier = this.getHoverMultiplier(200);
        int borderColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, hoverMultiplier);
        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.3f * hoverMultiplier);

        if (hoverMultiplier > 0.0f) {
            GuiRenderer.fill(this.x - xPadding, this.y - yPadding,
                    this.x + this.width + xPadding, this.y + this.height + yPadding,
                    backgroundColor);

            int x0 = this.x - xPadding;
            int x1 = this.x + this.width + xPadding;
            int y0 = this.y - yPadding;
            int y1 = this.y + height + yPadding;
            int border = 1;

            GuiRenderer.renderBorder(x0, y0, x1, y1, border, borderColor);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (this.active && this.visible) {
            if (this.isValidClickButton(button)) {
                boolean clicked = this.clicked(mouseX, mouseY);
                if (clicked) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onClick(mouseX, mouseY);
                    return true;
                }
            }

        }
        return false;
    }

    protected boolean clicked(double mX, double mY) {
        return this.active
                && this.visible
                && mX >= (double)this.getX()
                && mY >= (double)this.getY()
                && mX < (double)(this.getX() + this.getWidth())
                && mY < (double)(this.getY() + this.getHeight());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (this.isValidClickButton(button)) {
            this.onRelease(mouseX, mouseY);
            return true;
        } else {
            return false;
        }
    }

    protected boolean isValidClickButton(int button) {
        return button == 0;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double d, double e) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        if (this.isValidClickButton(button)) {
            this.onDrag(mouseX, mouseY, d, e);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void updateState(double mX, double mY) {
        super.updateState(mX, mY);

    }

    public void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    public Component getTooltip() {
        return null;
    }

    public PerformanceImpact getImpact() {
        return null;
    }
}
