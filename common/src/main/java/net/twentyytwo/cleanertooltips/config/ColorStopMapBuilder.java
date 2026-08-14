package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.twentyytwo.cleanertooltips.config.base.AbstractMapBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;

public class ColorStopMapBuilder extends AbstractMapBuilder<Integer, Integer, ColorStopMapListEntry, ColorStopMapBuilder> {
    private Function<ColorStopMapListEntry, ColorStopMapListEntry.ColorStopMapCell> createNewInstance;

    protected ColorStopMapBuilder(ConfigEntryBuilder entryBuilder, Component fieldNameKey, Map<Integer, Integer> value) {
        super(entryBuilder.getResetButtonKey(), fieldNameKey);
        this.value = value;
    }

    public ColorStopMapBuilder setCreateNewInstance(
            Function<ColorStopMapListEntry, ColorStopMapListEntry.ColorStopMapCell> createNewInstance) {
        this.createNewInstance = createNewInstance;
        return this;
    }

    @Override
    public @NotNull ColorStopMapListEntry build() {
        ColorStopMapListEntry entry = new ColorStopMapListEntry(
                this.getFieldNameKey(),
                this.value,
                null,
                this.defaultValue,
                this.getSaveConsumer(),
                this.getResetButtonKey(),
                this.isExpanded(),
                this.isRequireRestart(),
                this.isDeleteButtonEnabled(),
                this.isInsertInFront()
        );

        if (this.createNewInstance != null) {
            entry.setCreateNewInstance(this.createNewInstance);
        }

        entry.setInsertButtonEnabled(this.isInsertButtonEnabled());
        entry.setCellErrorSupplier(this.cellErrorSupplier);
        entry.setTooltipSupplier(() -> this.getTooltipSupplier().apply(entry.getValue()));
        entry.setRemoveTooltip(this.getRemoveTooltip());
        entry.setAddTooltip(this.getAddTooltip());
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
        }

        return this.finishBuilding(entry);
    }
}
