package net.vulkanmod.config.gui;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.option.Options;

import java.util.Set;

public class ModSettingsRegistry {

    public static final ModSettingsRegistry INSTANCE = new ModSettingsRegistry();

    private final Set<ModSettingsEntry> modEntries = new ObjectArraySet<>();

    ModSettingsRegistry() {
        ModSettingsEntry vulkanModSettings = new ModSettingsEntry(Component.literal("VulkanMod Reforged").withStyle(ChatFormatting.DARK_RED),
                                                                  () -> Identifier.fromNamespaceAndPath("vulkanmod", "vlogo_transparent.png"),
                                                                  Options::getOptionPages,
                                                                  () -> Initializer.CONFIG.write());
        this.addModEntry(vulkanModSettings);
    }

    public void addModEntry(ModSettingsEntry entry) {
        this.modEntries.add(entry);
    }

    public Set<ModSettingsEntry> getModEntries() {
        return modEntries;
    }
}
