package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.OptionBlock;

import java.util.ArrayList;
import java.util.List;

public class Page {
    private final String name;
    private final List<Block> blocks = new ArrayList<>();

    private Page(String name) {
        this.name = name;
    }

    public static Page of(String name) {
        return new Page(name);
    }

    public Block block(String title) {
        Block block = new Block(title, this);
        blocks.add(block);
        return block;
    }

    public static class Block {
        private final String title;
        private final List<Option<?>> options = new ArrayList<>();
        private final Page parent;

        private Block(String title, Page parent) {
            this.title = title;
            this.parent = parent;
        }

        public Block add(Option<?> option) {
            options.add(option);
            return this;
        }

        public Page done() {
            return parent;
        }

        private OptionBlock build() {
            return new OptionBlock(title, options.toArray(new Option[0]));
        }
    }
}