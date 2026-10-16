package net.vulkanmod.config.gui.widget;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class VTextInputWidget extends VAbstractWidget {
    public boolean selected = false;
    Consumer<VTextInputWidget> onSearch;                                                       
    private String text;
    private final Component placeholder;

    private int cursorPos = 0;
    private int selectionEnd = 0;
    private long lastBlinkTime = 0;
    private boolean showCursor = true;

    private static final int CURSOR_BLINK_INTERVAL = 500;      

    public VTextInputWidget(int x, int y, int width, int height, Component placeholder, Consumer<VTextInputWidget> onSearch) {
        this.setPosition(x, y, width, height);

        this.placeholder = placeholder;
        this.onSearch = onSearch;
        this.text = "";
    }

    @Override
    public void renderWidget(double mouseX, double mouseY) {
        if (!this.isVisible()) return;

        boolean hasText = !this.text.isEmpty();
        boolean isFocused = this.focused || this.selected;

        int backgroundColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.45f);

        int textColor = hasText ? VGuiConstants.COLOR_WHITE : VGuiConstants.COLOR_GRAY;

        GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);

        this.renderHovering(0, 0);

        if (isFocused && cursorPos != selectionEnd) {
            int start = Math.min(cursorPos, selectionEnd);
            int end = Math.max(cursorPos, selectionEnd);
            String before = text.substring(0, start);
            String selected = text.substring(start, end);

            int xBefore = this.x + 8 + Minecraft.getInstance().font.width(before);
            int xSelected = Minecraft.getInstance().font.width(selected);

            int selColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.55f);
            GuiRenderer.fill(xBefore, this.y + 4, xBefore + xSelected, this.y + this.height - 4, selColor);
        }

        Component displayText = hasText ? Component.literal(this.text) : this.placeholder;
        GuiRenderer.drawString(Minecraft.getInstance().font, displayText,
                this.x + 8, this.y + (this.height - 8) / 2, textColor | 0xFF000000);

        if (isFocused && showCursor) {
            String beforeCursor = text.substring(0, cursorPos);
            int cursorX = this.x + 8 + Minecraft.getInstance().font.width(beforeCursor);

            GuiRenderer.fill(cursorX, this.y + 6, cursorX + 1, this.y + this.height - 6,
                    VGuiConstants.COLOR_WHITE);
        }

        if (isFocused) {
            int borderColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.8f);
            GuiRenderer.renderBorder(this.x, this.y, this.x + this.width, this.y + this.height, 1, borderColor);
        }

        if (isFocused) {
            long time = Util.getMillis();
            if (time - lastBlinkTime > CURSOR_BLINK_INTERVAL) {
                showCursor = !showCursor;
                lastBlinkTime = time;
            }
        } else {
            showCursor = true;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.focused && !this.selected) return false;

        boolean shift = Screen.hasShiftDown();
        boolean ctrl = Screen.hasControlDown();

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.onSearch.accept(this);
            return true;
        }

        if (cursorPos != selectionEnd) {
            int start = Math.min(cursorPos, selectionEnd);
            int end = Math.max(cursorPos, selectionEnd);

            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                this.text = text.substring(0, start) + text.substring(end);
                cursorPos = start;
                selectionEnd = start;
                this.onSearch.accept(this);
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorPos > 0) {
                this.text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                cursorPos--;
                selectionEnd = cursorPos;
                this.onSearch.accept(this);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPos < text.length()) {
                this.text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                this.onSearch.accept(this);
            }
            return true;
        }

        if (ctrl && keyCode == GLFW.GLFW_KEY_A) {
            cursorPos = text.length();
            selectionEnd = 0;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (cursorPos > 0) cursorPos--;
            if (!shift) selectionEnd = cursorPos;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (cursorPos < text.length()) cursorPos++;
            if (!shift) selectionEnd = cursorPos;
            return true;
        }

        String keyName = GLFW.glfwGetKeyName(keyCode, scanCode);
        if (keyName != null && keyName.length() == 1) {
            char c = shift ? keyName.toUpperCase().charAt(0) : keyName.charAt(0);

            if (cursorPos != selectionEnd) {
                int start = Math.min(cursorPos, selectionEnd);
                int end = Math.max(cursorPos, selectionEnd);
                this.text = text.substring(0, start) + c + text.substring(end);
                cursorPos = start + 1;
            } else {
                this.text = text.substring(0, cursorPos) + c + text.substring(cursorPos);
                cursorPos++;
            }
            selectionEnd = cursorPos;
            this.onSearch.accept(this);
            return true;
        }

        return false;
    }

    public String getInput() {
        return this.text;
    }

    public void setInput(String input) {
        this.text = input != null ? input : "";
    }

    @SuppressWarnings("unused")
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
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.active || !this.visible)
            return null;
        return super.nextFocusPath(event);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;

        boolean clicked = this.clicked(mouseX, mouseY);
        if (clicked) {
            this.setFocused(true);
            this.selected = true;

            int relX = (int) mouseX - (this.x + 8);
            int pos = 0;
            for (int i = 0; i < text.length(); i++) {
                if (Minecraft.getInstance().font.width(text.substring(0, i + 1)) > relX) break;
                pos = i + 1;
            }
            cursorPos = pos;
            selectionEnd = pos;

            return true;
        } else {
            this.setFocused(false);
            this.selected = false;
            return false;
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.selected = false;
        }
    }
}
