package net.vulkanmod.config.gui.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.VAbstractWidget;

public interface SettingsPageContent {
    String id();

    Component title();

    void initialize(int x, int y, int width, int height, int itemHeight);

    SettingsPageListener listener();

    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick);

    default void onSelected() {
    }

    default void updateAvailability() {
    }

    default boolean searchable() {
        return false;
    }

    default VAbstractWidget getHoveredWidget(double mouseX, double mouseY) {
        return null;
    }
}
