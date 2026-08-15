package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractSliderFieldBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class ExtendedSliderBuilder extends AbstractSliderFieldBuilder<Integer, ExtendedSliderEntry, ExtendedSliderBuilder> {
    protected Component prefix = Component.empty();
    protected Component suffix = Component.empty();

    protected ExtendedSliderBuilder(ConfigEntryBuilder entryBuilder, Component fieldNameKey, int value, int min, int max) {
        super(entryBuilder.getResetButtonKey(), fieldNameKey);
        this.value = value;
        this.min = min;
        this.max = max;
    }

    public ExtendedSliderBuilder setPrefix(Component prefix) {
        this.prefix = prefix;
        return this;
    }

    public ExtendedSliderBuilder setSuffix(Component suffix) {
        this.suffix = suffix;
        return this;
    }

    public Component getPrefix() {
        return prefix;
    }

    public Component getSuffix() {
        return suffix;
    }

    @Override
    public @NotNull ExtendedSliderEntry build() {
        ExtendedSliderEntry entry = new ExtendedSliderEntry(
                this.getFieldNameKey(),
                this.min, this.max,
                this.prefix, this.suffix,
                this.value,
                this.getResetButtonKey(),
                this.defaultValue,
                this.getSaveConsumer(),
                null,
                this.isRequireRestart()
        );

        entry.setTooltipSupplier(() -> this.getTooltipSupplier().apply(entry.getValue()));
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
        }

        return this.finishBuilding(entry);
    }
}
