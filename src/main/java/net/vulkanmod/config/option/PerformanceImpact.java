package net.vulkanmod.config.option;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum PerformanceImpact {
    LOW(Component.translatable("vulkanmod.options.performanceImpact.low").withStyle(ChatFormatting.DARK_GREEN)),
    MEDIUM(Component.translatable("vulkanmod.options.performanceImpact.medium").withStyle(ChatFormatting.YELLOW)),
    HIGH(Component.translatable("vulkanmod.options.performanceImpact.high").withStyle(ChatFormatting.RED));

    private final Component component;

    PerformanceImpact(Component component) {
        this.component = component;
    }

    public Component component() {
        return this.component;
    }
}
