package net.vulkanmod.config.option;

import java.util.Objects;

public record OptionKey<T>(String id, Class<T> type) {
    public OptionKey {
        id = Objects.requireNonNull(id, "id");
        type = Objects.requireNonNull(type, "type");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Option key id must not be blank");
        }
    }
}
