package net.vulkanmod.config.gui;

import net.minecraft.client.input.CharacterEvent;

import net.minecraft.client.input.KeyEvent;

import net.minecraft.client.input.MouseButtonEvent;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.vulkanmod.config.UpdateChecker;
import net.vulkanmod.compat.doctor.DoctorAction;
import net.vulkanmod.compat.doctor.DoctorActionPolicy;
import net.vulkanmod.compat.doctor.DoctorSetting;
import net.vulkanmod.compat.doctor.DoctorSettingChange;
import net.vulkanmod.compat.doctor.DoctorSettingsSnapshot;
import net.vulkanmod.compat.doctor.DoctorValue;
import net.vulkanmod.config.gui.page.DiagnosticsPageContent;
import net.vulkanmod.config.gui.page.DoctorSettingsController;
import net.vulkanmod.config.gui.page.DoctorStageResult;
import net.vulkanmod.config.gui.page.OptionPageContent;
import net.vulkanmod.config.gui.page.SettingsPageContent;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.SearchHelper;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.config.gui.widget.*;
import net.vulkanmod.config.option.*;
import net.vulkanmod.diagnostics.benchmark.MinecraftBenchmarkController;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VOptionScreen extends Screen {
    public final static int MARGIN = 10;

    private final Screen parent;

    private final List<ModSettingsEntry> modSettingsEntries;

    private final List<OptionPage> optionPages;
    private final List<SettingsPageContent> pageContents;
    private final DiagnosticsPageContent diagnosticsPage;
    private final Map<OptionKey<?>, Option<?>> keyedOptions = new LinkedHashMap<>();
    private DoctorAction.StageSettings pendingDoctorDraft;
    private List<OptionDraft> pendingDraftsToRestore = List.of();
    private boolean applyingSettings;
    private OptionPage searchResultsPage;

    private int currentListIdx = 0;
    private String selectedPageId;
    private boolean isSearchActive = false;

    private int tooltipX;
    private int tooltipY;
    private int tooltipWidth;

    private VButtonWidget applyButton;
    private VButtonWidget undoButton;

    private VTextInputWidget searchField;

    private final List<VAbstractWidget> iconWidgets = Lists.newArrayList();
    private final List<VButtonWidget> pageButtons = Lists.newArrayList();
    private final List<VButtonWidget> buttons = Lists.newArrayList();


    public VOptionScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;

        this.optionPages = new ArrayList<>();
        this.pageContents = new ArrayList<>();
        this.diagnosticsPage = new DiagnosticsPageContent(new DoctorSettingsController() {
            @Override
            public DoctorSettingsSnapshot settingsSnapshot() {
                return VOptionScreen.this.doctorSettingsSnapshot();
            }

            @Override
            public DoctorStageResult stage(DoctorAction.StageSettings action) {
                return VOptionScreen.this.stageDoctorSettings(action);
            }

            @Override
            public boolean hasPendingSettings() {
                return VOptionScreen.this.hasPendingSettings();
            }

            @Override
            public int pendingSettingsCount() {
                return VOptionScreen.this.pendingSettingsCount();
            }

            @Override
            public long contextToken() {
                return MinecraftBenchmarkController.worldSessionId(VOptionScreen.this.minecraft);
            }

            @Override
            public void focusApplyControl() {
                if (VOptionScreen.this.applyButton != null) {
                    VOptionScreen.this.setFocused(VOptionScreen.this.applyButton);
                    VOptionScreen.this.applyButton.setFocused(true);
                }
            }
        });
        this.modSettingsEntries = new ArrayList<>(ModSettingsRegistry.INSTANCE.getModEntries());
    }

    @Override
    protected void init() {
                                                                                  
                                                                                  
        if (this.isSearchActive) {
            this.isSearchActive = false;
            this.searchResultsPage = null;
        }
        this.initOptionsPages();

        if (this.pageContents.isEmpty()) {
            throw new IllegalStateException("Default Options weren't added!");
        }

        this.buildLayout();

        this.updateState();
    }

    private void buildLayout() {
        int top = 32;
        int bottom = 60;
        int itemHeight = 20;

        int leftMargin = MARGIN + VGuiConstants.PAGE_BUTTON_WIDTH + 6;
        int listWidth = Math.min(this.width - leftMargin - MARGIN, 420);
        int listHeight = this.height - top - bottom;

        this.buildLists(leftMargin, top, listWidth, listHeight, itemHeight);

        this.searchField = createSearchField();

        int x = leftMargin + listWidth + 6;
        int tooltipWidth = Math.min(this.width - x - 10, 420);
        int y = top + itemHeight + 6;

        if (tooltipWidth < 200) {
            x = leftMargin + 3;
            tooltipWidth = listWidth;
            y = this.height - bottom + 10;
        }

        this.tooltipX = x;
        this.tooltipY = y;
        this.tooltipWidth = tooltipWidth;

        this.buildPage();
    }

    private void initOptionsPages() {
        List<OptionDraft> preservedDrafts = applyingSettings
                ? List.of()
                : capturePendingOptionDrafts();
        if (!applyingSettings && pendingDoctorDraft != null && !doctorDraftMatchesCurrentOptions()) {
            pendingDoctorDraft = null;
            diagnosticsPage.onDoctorDraftInvalidated();
        }
        this.optionPages.clear();
        this.pageContents.clear();

        int groupIndex = 0;
        for (var modPageSet : this.modSettingsEntries) {
            modPageSet.initPages();
            int pageIndex = 0;
            for (OptionPage page : modPageSet.getPages()) {
                this.optionPages.add(page);
                this.pageContents.add(new OptionPageContent(
                        "options:" + groupIndex + ":" + pageIndex, page));
                pageIndex++;
            }
            groupIndex++;
        }

        this.indexKeyedOptions();
        this.pendingDraftsToRestore = preservedDrafts;

        if (selectedPageId == null && !pageContents.isEmpty()) {
            selectedPageId = pageContents.getFirst().id();
        }
        currentListIdx = indexOfPage(selectedPageId);
        if (currentListIdx < 0) {
            currentListIdx = 0;
            selectedPageId = pageContents.getFirst().id();
        }
    }

    private VTextInputWidget createSearchField() {
        int rightMargin = 10;
        int padding = 10;
        int kofiWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + padding;
        int topBarRight = this.width - kofiWidth - rightMargin;

        if (UpdateChecker.isUpdateAvailable()) {
            int updateWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.update_available")) + padding;
            topBarRight -= updateWidth + VGuiConstants.WIDGET_MARGIN;
        }


        int leftMargin = VGuiConstants.PAGE_BUTTON_WIDTH + MARGIN + 6;
        int width = Math.min(topBarRight - leftMargin - 4, 413);

        return new VTextInputWidget(
                leftMargin, 4,
                width, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.searchFieldPlaceholder"),
                widget -> performSearch(widget.getInput())
        );
    }

    private void buildLists(int left, int top, int listWidth, int listHeight, int itemHeight) {
        int diagnosticsWidth = Math.min(this.width - left - MARGIN, 720);
        for (SettingsPageContent page : this.pageContents) {
            page.initialize(left, top,
                    page == diagnosticsPage ? diagnosticsWidth : listWidth,
                    listHeight, itemHeight);
        }
        restoreOptionDrafts(pendingDraftsToRestore);
        pendingDraftsToRestore = List.of();
        for (OptionPage page : optionPages) {
            page.updateOptionStates();
        }
                                                                                 
                                                                                
                                                                      
        if (!applyingSettings && (pendingDoctorDraft != null || hasPendingSettings())) {
            diagnosticsPage.onOptionDraftsRestored();
        }
    }

    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            isSearchActive = false;
            this.currentListIdx = 0;
            this.selectedPageId = this.pageContents.getFirst().id();
            buildPage();
            return;
        }

        String searchTerm = query.toLowerCase().trim();
        List<OptionBlock> searchResults = new ArrayList<>();

        for (OptionPage page : this.optionPages) {
            List<Option<?>> matchingOptions = new ArrayList<>();

            for (OptionBlock block : page.optionBlocks) {
                for (Option<?> option : block.options()) {
                    boolean matches = false;

                    String optionName = option.getName().getString().toLowerCase();
                    String optionTooltip = option.getTooltip() != null ?
                            option.getTooltip().getString().toLowerCase() : "";
                    String displayedValue = option.getDisplayedValue().getString().toLowerCase();

                    if (optionName.contains(searchTerm) ||
                            optionTooltip.contains(searchTerm) ||
                            displayedValue.contains(searchTerm)) {
                        matches = true;
                    }

                    else if (option instanceof CyclingOption<?> cycling) {
                        if (SearchHelper.matchesAnyValue(cycling, searchTerm)) {
                            matches = true;
                        }
                    }

                    if (matches) {
                        matchingOptions.add(option);
                    }
                }
            }

            if (!matchingOptions.isEmpty()) {
                searchResults.add(new OptionBlock("§l" + page.name,
                        matchingOptions.toArray(new Option<?>[0])));
                searchResults.add(new OptionBlock("", new Option<?>[0]));
            }
        }

        searchResultsPage = new OptionPage(
                "Search Results",
                searchResults.toArray(new OptionBlock[0])
        );

        int top = 32;
        int bottom = 60;
        int itemHeight = 20;
        int leftMargin = MARGIN + VGuiConstants.PAGE_BUTTON_WIDTH + 6;
        int listWidth = Math.min(this.width - leftMargin - MARGIN, 420);
        int listHeight = this.height - top - bottom;

        searchResultsPage.createList(leftMargin, top, listWidth, listHeight, itemHeight);

        isSearchActive = true;
        buildPage();
    }

    private void buildPage() {
        this.buttons.clear();
        this.pageButtons.clear();
        this.iconWidgets.clear();

        String savedInput = this.searchField != null ? this.searchField.getInput() : "";
        boolean savedFocused = this.searchField != null && this.searchField.focused;
        boolean savedSelected = this.searchField != null && this.searchField.selected;

        this.setFocused(null);
        this.clearWidgets();

        int x = MARGIN;
        int y = 4;

        int width = VGuiConstants.PAGE_BUTTON_WIDTH;
        int j = 0;
        for (var modEntry : this.modSettingsEntries) {
            ModIconWidget iconWidget = new ModIconWidget(modEntry.modName, modEntry.getIcon(), x, y, width, 28);
            this.iconWidgets.add(iconWidget);
            this.addWidget(iconWidget);
            y += 28;

            var pages = modEntry.getPages();
            for (OptionPage ignored : pages) {
                SettingsPageContent page = this.pageContents.get(j);
                final int finalIdx = j;
                VButtonWidget widget = new VButtonWidget(x, y, width, VGuiConstants.WIDGET_HEIGHT,
                        page.title(), button -> this.setOptionList(finalIdx));
                widget.setTextLayout(false, 12);
                this.buttons.add(widget);
                this.pageButtons.add(widget);
                this.addWidget(widget);

                y += VGuiConstants.WIDGET_HEIGHT;
                j++;
            }
        }

        if (!isSearchActive) {
            this.pageButtons.get(this.currentListIdx).setSelected(true);
            this.addWidget(this.pageContents.get(this.currentListIdx).listener());
        } else {
            if (searchResultsPage != null) {
                VOptionList searchList = searchResultsPage.getOptionList();
                this.addWidget(searchList);
                searchResultsPage.updateOptionStates();
            }
        }

        this.addButtonsWithSearchBar();

        this.searchField.setInput(isDiagnosticsSelected() ? "" : savedInput);
        if (savedFocused && !isDiagnosticsSelected()) {
            this.searchField.setFocused(true);
            this.searchField.setSelected(savedSelected);
        }
        this.updateState();
    }

    @SuppressWarnings("DuplicatedCode")
    private void addButtonsWithSearchBar() {
        int rightMargin = 10;
        int padding = 10;
        int buttonWidth = Minecraft.getInstance().font.width(CommonComponents.GUI_DONE) + 2 * padding;
        int x0 = (this.width - buttonWidth - rightMargin);
        int y0 = this.height - VGuiConstants.WIDGET_HEIGHT - 7;

        VButtonWidget doneButton = new VButtonWidget(x0, y0, buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                CommonComponents.GUI_DONE, button -> Minecraft.getInstance().setScreen(this.parent));

        buttonWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.apply")) + 2 * padding;
        x0 -= (buttonWidth + VGuiConstants.WIDGET_MARGIN);
        this.applyButton = new VButtonWidget(x0, y0, buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.apply"), button -> this.applyOptions());

        buttonWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.undo")) + 2 * padding;
        x0 -= (buttonWidth + VGuiConstants.WIDGET_MARGIN);
        this.undoButton = new VButtonWidget(x0, y0, buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.undo"), button -> undo());

        int kofiWidth = Minecraft.getInstance().font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + padding;

        int kofiX = this.width - kofiWidth - rightMargin;
        VButtonWidget supportButton = new VButtonWidget(kofiX, 4, kofiWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.kofi"),
                button -> Util.getPlatform().openUri("https://ko-fi.com/rindw"));

        this.buttons.add(this.applyButton);
        this.buttons.add(doneButton);
        this.buttons.add(supportButton);
        this.buttons.add(this.undoButton);

        this.addWidget(this.applyButton);
        this.addWidget(doneButton);
        this.addWidget(supportButton);
        this.addWidget(this.undoButton);
        if (!isDiagnosticsSelected()) {
            this.addWidget(this.searchField);
        }

        if (UpdateChecker.isUpdateAvailable()) {
            assert minecraft != null;
            int updateWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.update_available")) + padding;
            var updateButton = new VButtonWidget(
                    kofiX - updateWidth - VGuiConstants.WIDGET_MARGIN, 4,
                    updateWidth, VGuiConstants.WIDGET_HEIGHT,
                    Component.translatable("vulkanmod.options.buttons.update_available").withStyle(ChatFormatting.UNDERLINE),
                    button -> Util.getPlatform().openUri(UpdateChecker.RELEASES_URL)
            );
            this.buttons.add(updateButton);
            this.addWidget(updateButton);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        GuiEventListener focusedBeforeClick = this.getFocused();
        for (GuiEventListener element : this.children()) {
            if (element.mouseClicked(event, doubleClick)) {
                                                                                 
                                                                                  
                                                                            
                if (this.getFocused() == focusedBeforeClick && this.children().contains(element)) {
                    this.setFocused(element);
                }
                if (button == 0) {
                    this.setDragging(true);
                }

                this.updateState();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        double mouseX = event.x(); double mouseY = event.y(); int button = event.button();
        this.setDragging(false);
        this.updateState();
        return this.getChildAt(mouseX, mouseY)
                   .filter(guiEventListener -> guiEventListener.mouseReleased(event))
                   .isPresent();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiRenderer.guiGraphics = guiGraphics;
        GuiRenderer.pose = guiGraphics.pose();

        VOptionList searchList = null;
        SettingsPageContent currentContent = null;
        if (isSearchActive && searchResultsPage != null) {
            searchList = searchResultsPage.getOptionList();
            searchList.updateState(mouseX, mouseY);
            searchList.renderWidget(mouseX, mouseY);
        } else {
            currentContent = this.pageContents.get(this.currentListIdx);
            currentContent.updateAvailability();
            currentContent.render(guiGraphics, mouseX, mouseY, delta);
        }

        for (var widget : iconWidgets) {
            widget.render(mouseX, mouseY);
        }

        for (VButtonWidget button : buttons) {
            button.updateState(mouseX, mouseY);
            button.render(mouseX, mouseY);
        }
        if (!isDiagnosticsSelected()) {
            searchField.updateState(mouseX, mouseY);
            searchField.render(mouseX, mouseY);
        } else {
            int headingX = MARGIN + VGuiConstants.PAGE_BUTTON_WIDTH + 6;
            guiGraphics.drawString(this.font, this.diagnosticsPage.title(), headingX, 10, 0xFFFFFFFF, false);
        }

        VAbstractWidget hoveredWidget = null;

        for (var b : buttons) {
            if (b.isMouseOver(mouseX, mouseY)) {
                hoveredWidget = b;
                break;
            }
        }

        if (hoveredWidget == null) {
            if (searchList != null) {
                hoveredWidget = searchList.getHoveredWidget(mouseX, mouseY);
            } else if (currentContent != null) {
                hoveredWidget = currentContent.getHoveredWidget(mouseX, mouseY);
            }
        }

        if (hoveredWidget != null) {
            this.renderTooltip(hoveredWidget, this.tooltipX, this.tooltipY);
        }
    }

    private void renderTooltip(VAbstractWidget widget, int x, int y) {
        var list = this.getWidgetTooltip(widget);

        if (list.isEmpty()) {
            return;
        }

        int lines = list.size();

        int padding = 3;
        int width = GuiRenderer.getMaxTextWidth(this.font, list);
        int height = lines * 10;
        float intensity = 0.05f;
        int color = ColorUtil.ARGB.pack(intensity, intensity, intensity, 0.6f);
        GuiRenderer.fill(x - padding, y - padding, x + width + padding, y + height + padding, color);

        color = VGuiConstants.COLOR_RED;
        GuiRenderer.renderBorder(x - padding, y - padding, x + width + padding, y + height + padding, 1, color);

        int yOffset = 0;
        for (var text : list) {
            GuiRenderer.drawString(this.font, text, x, y + yOffset, 0xffffffff);
            yOffset += 10;
        }
    }

    private List<FormattedCharSequence> getWidgetTooltip(VAbstractWidget widget) {
        var tooltip = widget.getTooltip();
        var impact = widget.getImpact();

        List<FormattedCharSequence> textList = new ArrayList<>();
        if (tooltip != null) {
            textList.addAll(this.font.split(tooltip, this.tooltipWidth));
        }

        if (impact != null) {
            textList.addAll(this.font.split(Component.translatable("vulkanmod.options.performanceImpact", impact.component()), this.tooltipWidth));
        }

        return textList;
    }

    private void updateState() {
        if (this.applyButton == null || this.undoButton == null) return;
        boolean modified = hasPendingSettings();

        this.applyButton.active = modified;
        this.undoButton.visible = modified;
        this.diagnosticsPage.updateAvailability();
    }

    private void setOptionList(int i) {
        if (i < 0 || i >= this.pageContents.size()) {
            return;
        }
        this.currentListIdx = i;
        this.selectedPageId = this.pageContents.get(i).id();
        this.isSearchActive = false;

        this.searchField.setInput("");
        this.searchField.setFocused(false);

        this.pageContents.get(i).onSelected();

        this.buildPage();

        this.pageButtons.get(i).setSelected(true);
    }

    private void undo() {
        for (OptionPage page : this.optionPages) {
            page.resetToOriginalState();
            page.updateOptionStates();
        }

        pendingDoctorDraft = null;
        diagnosticsPage.onSettingsReset();
        buildPage();
        updateState();
    }

    private void applyOptions() {
        this.diagnosticsPage.beforeSettingsApplied();
        List<OptionPage> pages = List.copyOf(this.optionPages);
        this.applyingSettings = true;
        try {
            for (var page : pages) {
                page.applyOptionChanges();
                page.updateOptionStates();
            }

            for (var modEntry : this.modSettingsEntries) {
                modEntry.runOnApply();
            }
        } finally {
            this.applyingSettings = false;
        }

                                                                              
                                                                                    
        this.pendingDoctorDraft = null;
        this.rebuildPagesFromCurrentOptions();
        this.diagnosticsPage.onSettingsApplied();
    }

    private void rebuildPagesFromCurrentOptions() {
        this.isSearchActive = false;
        this.searchResultsPage = null;

        this.initOptionsPages();
        this.buildLayout();

        this.applyButton.active = false;
        this.undoButton.visible = false;
    }

    private boolean hasPendingSettings() {
        return pendingSettingsCount() > 0;
    }

    private int pendingSettingsCount() {
        int changed = 0;
        for (OptionPage page : this.optionPages) {
            for (OptionBlock block : page.optionBlocks) {
                for (Option<?> option : block.options()) {
                    if (option.isChanged()) {
                        changed++;
                    }
                }
            }
        }
        return changed;
    }

    private void indexKeyedOptions() {
        keyedOptions.clear();
        for (OptionPage page : optionPages) {
            for (OptionBlock block : page.optionBlocks) {
                for (Option<?> option : block.options()) {
                    option.key().ifPresent(key -> {
                        Option<?> previous = keyedOptions.putIfAbsent(key, option);
                        if (previous != null) {
                            throw new IllegalStateException("Duplicate stable option key: " + key.id());
                        }
                    });
                }
            }
        }
    }

    private DoctorSettingsSnapshot doctorSettingsSnapshot() {
        EnumMap<DoctorSetting, DoctorValue> values = new EnumMap<>(DoctorSetting.class);
        EnumSet<DoctorSetting> pending = EnumSet.noneOf(DoctorSetting.class);
        captureDoctorSetting(values, pending, DoctorSetting.INDIRECT_DRAW, OptionKeys.INDIRECT_DRAW);
        captureDoctorSetting(values, pending, DoctorSetting.UNIQUE_OPAQUE_LAYER, OptionKeys.UNIQUE_OPAQUE_LAYER);
        captureDoctorSetting(values, pending, DoctorSetting.RENDER_SCALE, OptionKeys.RENDER_SCALE);
        return new DoctorSettingsSnapshot(values, pending);
    }

    private <T> void captureDoctorSetting(Map<DoctorSetting, DoctorValue> values,
                                          Set<DoctorSetting> pending,
                                          DoctorSetting setting,
                                          OptionKey<T> key) {
        Option<T> option = keyedOption(key);
        if (option == null) {
            return;
        }
        DoctorValue value = doctorValue(option.getNewValue());
        if (value == null) {
            return;
        }
        values.put(setting, value);
        if (option.isChanged()) {
            pending.add(setting);
        }
    }

    private DoctorStageResult stageDoctorSettings(DoctorAction.StageSettings action) {
        if (action == null || action.changes().isEmpty()) {
            return DoctorStageResult.NO_CHANGES;
        }
        if (!DoctorActionPolicy.isAllowed(action)) {
            return DoctorStageResult.INVALID_ACTION;
        }
        if (hasPendingSettings()) {
            return DoctorStageResult.PENDING_SETTINGS;
        }

        EnumSet<DoctorSetting> seen = EnumSet.noneOf(DoctorSetting.class);
        List<StagedOption> validated = new ArrayList<>(action.changes().size());
        for (DoctorSettingChange change : action.changes()) {
            if (!seen.add(change.setting())) {
                return DoctorStageResult.INVALID_ACTION;
            }
            Option<?> option = keyedOptions.get(optionKey(change.setting()));
            if (option == null) {
                return DoctorStageResult.OPTION_UNAVAILABLE;
            }
            DoctorValue current = doctorValue(option.getNewValue());
            if (!change.expectedValue().equals(current)) {
                return DoctorStageResult.STALE_ASSESSMENT;
            }
            validated.add(new StagedOption(option, change.targetValue().rawValue()));
        }

        for (StagedOption staged : validated) {
            setOptionNewValue(staged.option(), staged.value());
        }
        pendingDoctorDraft = action;
        for (OptionPage page : optionPages) {
            page.updateOptionStates();
        }
        updateState();
        return DoctorStageResult.STAGED;
    }

    private boolean doctorDraftMatchesCurrentOptions() {
        DoctorAction.StageSettings action = pendingDoctorDraft;
        if (action == null || !DoctorActionPolicy.isAllowed(action)
                || pendingSettingsCount() != action.changes().size()) {
            return false;
        }
        for (DoctorSettingChange change : action.changes()) {
            Option<?> option = keyedOptions.get(optionKey(change.setting()));
            if (option == null || !change.targetValue().equals(doctorValue(option.getNewValue()))) {
                return false;
            }
        }
        return true;
    }

    private List<OptionDraft> capturePendingOptionDrafts() {
        List<OptionDraft> drafts = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < optionPages.size(); pageIndex++) {
            OptionPage page = optionPages.get(pageIndex);
            for (int blockIndex = 0; blockIndex < page.optionBlocks.length; blockIndex++) {
                Option<?>[] options = page.optionBlocks[blockIndex].options();
                for (int optionIndex = 0; optionIndex < options.length; optionIndex++) {
                    Option<?> option = options[optionIndex];
                    if (option.isChanged()) {
                        drafts.add(new OptionDraft(
                                pageIndex, page.name,
                                blockIndex, page.optionBlocks[blockIndex].title(),
                                optionIndex, option.draftIdentity(), option.getNewValue()));
                    }
                }
            }
        }
        return List.copyOf(drafts);
    }

    private void restoreOptionDrafts(List<OptionDraft> drafts) {
        for (OptionDraft draft : drafts) {
            if (draft.pageIndex() < 0 || draft.pageIndex() >= optionPages.size()) {
                continue;
            }
            OptionPage page = optionPages.get(draft.pageIndex());
            if (!java.util.Objects.equals(draft.pageName(), page.name)) {
                continue;
            }
            if (draft.blockIndex() < 0 || draft.blockIndex() >= page.optionBlocks.length) {
                continue;
            }
            OptionBlock block = page.optionBlocks[draft.blockIndex()];
            if (!java.util.Objects.equals(draft.blockTitle(), block.title())) {
                continue;
            }
            Option<?>[] options = block.options();
            if (draft.optionIndex() < 0 || draft.optionIndex() >= options.length) {
                continue;
            }
            Option<?> option = options[draft.optionIndex()];
            Object value = draft.value();
            if (!draft.optionIdentity().equals(option.draftIdentity())
                    || !option.acceptsDraftValue(value)) {
                continue;
            }
            setOptionNewValue(option, value);
        }
    }

    private static OptionKey<?> optionKey(DoctorSetting setting) {
        return switch (setting) {
            case INDIRECT_DRAW -> OptionKeys.INDIRECT_DRAW;
            case UNIQUE_OPAQUE_LAYER -> OptionKeys.UNIQUE_OPAQUE_LAYER;
            case RENDER_SCALE -> OptionKeys.RENDER_SCALE;
        };
    }

    @SuppressWarnings("unchecked")
    private <T> Option<T> keyedOption(OptionKey<T> key) {
        Option<?> option = keyedOptions.get(key);
        if (option == null || !key.type().isInstance(option.getNewValue())) {
            return null;
        }
        return (Option<T>) option;
    }

    private static DoctorValue doctorValue(Object value) {
        if (value instanceof Boolean bool) {
            return DoctorValue.of(bool);
        }
        if (value instanceof Integer integer) {
            return DoctorValue.of(integer);
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setOptionNewValue(Option option, Object value) {
        option.setNewValue(value);
    }

    private record StagedOption(Option<?> option, Object value) {
    }

    private record OptionDraft(
            int pageIndex,
            String pageName,
            int blockIndex,
            String blockTitle,
            int optionIndex,
            String optionIdentity,
            Object value) {
    }

    private boolean isDiagnosticsSelected() {
        return !isSearchActive
                && currentListIdx >= 0
                && currentListIdx < pageContents.size()
                && DiagnosticsPageContent.PAGE_ID.equals(pageContents.get(currentListIdx).id());
    }

    private int indexOfPage(String pageId) {
        if (pageId == null) {
            return -1;
        }
        for (int i = 0; i < pageContents.size(); i++) {
            if (pageId.equals(pageContents.get(i).id())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key(); int scanCode = event.scancode(); int modifiers = event.modifiers();
        if (!isDiagnosticsSelected() && event.hasControlDown() && keyCode == GLFW.GLFW_KEY_L) {
            this.setFocused(searchField);
            searchField.setFocused(true);
            searchField.setSelected(true);

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.isSearchActive) {
            this.isSearchActive = false;
            this.searchField.setInput("");
            this.searchField.setFocused(false);
            this.buildPage();
            this.pageButtons.get(this.currentListIdx).setSelected(true);
            return true;
        }


        if (!this.searchField.focused
                && keyCode == GLFW.GLFW_KEY_P
                && event.hasShiftDown()) {
            Minecraft.getInstance().setScreen(new VideoSettingsScreen(this, Minecraft.getInstance(), Minecraft.getInstance().options));

            return false;
        }

        return super.keyPressed(event);
    }
}
