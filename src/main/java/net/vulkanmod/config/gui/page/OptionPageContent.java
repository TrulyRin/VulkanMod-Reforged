package net.vulkanmod.config.gui.page;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.config.option.OptionPage;

import java.util.Objects;

public final class OptionPageContent implements SettingsPageContent {
    private final String id;
    private final OptionPage page;

    public OptionPageContent(String id, OptionPage page) {
        this.id = Objects.requireNonNull(id, "id");
        this.page = Objects.requireNonNull(page, "page");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Component title() {
        return Component.literal(page.name);
    }

    @Override
    public void initialize(int x, int y, int width, int height, int itemHeight) {
        page.createList(x, y, width, height, itemHeight);
        page.updateOptionStates();
    }

    @Override
    public SettingsPageListener listener() {
        return page.getOptionList();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        page.getOptionList().updateState(mouseX, mouseY);
        page.getOptionList().renderWidget(mouseX, mouseY);
    }

    @Override
    public boolean searchable() {
        return true;
    }

    @Override
    public VAbstractWidget getHoveredWidget(double mouseX, double mouseY) {
        return page.getOptionList().getHoveredWidget(mouseX, mouseY);
    }
}
