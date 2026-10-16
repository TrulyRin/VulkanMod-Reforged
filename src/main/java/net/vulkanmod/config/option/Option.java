package net.vulkanmod.config.option;

import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.widget.OptionWidget;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.Optional;

public abstract class Option<T> {
    protected final Component name;
    @SuppressWarnings("unused")
    protected Component tooltip;
    protected PerformanceImpact impact;

    protected Consumer<T> onApply;
    protected Supplier<T> valueSupplier;

    protected T value;
    protected T newValue;

    protected Function<T, Component> translator;
    protected Function<T, Component> tooltipTranslator;

    OptionWidget<?> widget;

    protected boolean active;
    protected Runnable onChange;
    protected Supplier<Boolean> activationFn;
    private OptionKey<T> key;

    @SuppressWarnings("unused")
    public Option(Component name, Consumer<T> setter, Supplier<T> getter, Function<T, Component> translator,  Function<T, Component> tooltip) {
        this.name = name;

        this.onApply = setter;
        this.valueSupplier = getter;

        this.translator = translator;
        this.tooltipTranslator = tooltip;

        this.newValue = this.value = this.valueSupplier.get();
    }

    public Option(Component name, Consumer<T> setter, Supplier<T> getter, Function<T, Component> translator) {
        this.name = name;

        this.onApply = setter;
        this.valueSupplier = getter;

        this.newValue = this.value = this.valueSupplier.get();

        this.translator = translator;
    }

    public Option(Component name, Consumer<T> setter, Supplier<T> getter) {
        this.name = name;

        this.onApply = setter;
        this.valueSupplier = getter;

        this.newValue = this.value = this.valueSupplier.get();
    }

    @SuppressWarnings("unused")
    public Option<T> setOnApply(Consumer<T> onApply) {
        this.onApply = onApply;
        return this;
    }

    @SuppressWarnings("unused")
    public Option<T> setValueSupplier(Supplier<T> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public Option<T> setTranslator(Function<T, Component> translator) {
        this.translator = translator;
        return this;
    }

    public Function<T, Component> getTranslator() {
        return translator;
    }

    public Option<T> setTooltip(Function<T, Component> tooltipTranslator) {
        this.tooltipTranslator = tooltipTranslator;
        return this;
    }

    public Option<T> setTooltip(Component tooltip) {
        this.tooltipTranslator = v -> tooltip;
        return this;
    }

    public Option<T> setKey(OptionKey<T> key) {
        this.key = java.util.Objects.requireNonNull(key, "key");
        if (!key.type().isInstance(this.newValue)) {
            throw new IllegalArgumentException("Option value does not match key type " + key.id());
        }
        return this;
    }

    public Optional<OptionKey<T>> key() {
        return Optional.ofNullable(key);
    }

    public String draftIdentity() {
        return key != null
                ? "key:" + key.id()
                : getClass().getName() + ":" + name.getString();
    }

    public boolean acceptsDraftValue(Object candidate) {
        if (candidate == null) {
            return false;
        }
        Class<?> expectedType = key != null
                ? key.type()
                : this.newValue == null ? null : this.newValue.getClass();
        return expectedType != null && expectedType.isInstance(candidate);
    }

    public PerformanceImpact getImpact() {
        return impact;
    }

    public Option<T> setImpact(PerformanceImpact impact) {
        this.impact = impact;
        return this;
    }

    public Option<T> setActive(boolean active) {
        this.active = active;
        this.widget.active = active;
        return this;
    }

    protected abstract OptionWidget<?> createWidget();

    public OptionWidget<?> getWidget() {
        if (this.widget == null) {
            this.widget = this.createWidget();
        }

        return this.widget;
    }

    public void setNewValue(T t) {
        this.newValue = t;

        if (onChange != null)
            onChange.run();
    }

    public void updateActiveState() {
        if (this.activationFn != null) {
            this.active = this.activationFn.get();
        }
        else {
            this.active = true;
        }

        this.widget.setActive(this.active);
    }

    public Component getName() {
        return this.name;
    }

    public Option<T> setOnChange(Runnable runnable) {
        this.onChange = runnable;
        return this;
    }

    public Option<T> setActivationFn(Supplier<Boolean> activationFn) {
        this.activationFn = activationFn;
        return this;
    }

    public boolean isChanged() {
        return !this.newValue.equals(this.value);
    }

    public void apply() {
        onApply.accept(this.newValue);
        this.value = this.newValue;
    }

    public void resetValue() {
        this.setNewValue(this.value);
    }

    public T getNewValue() {
        return this.newValue;
    }

    public T getAppliedValue() {
        return this.value;
    }

    public Component getDisplayedValue() {
        return this.translator.apply(this.newValue);
    }

    public Component getTooltip() {
        if (this.tooltipTranslator != null) {
            return this.tooltipTranslator.apply(this.newValue);
        } else {
            return null;
        }
    }
}
