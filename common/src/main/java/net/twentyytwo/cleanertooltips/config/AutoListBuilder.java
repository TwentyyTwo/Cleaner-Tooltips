package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.AbstractListBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AutoListBuilder extends AbstractListBuilder<String, AutoListListEntry, AutoListBuilder> {
    private Function<AutoListListEntry, AutoListListEntry.AutoListCell> createNewInstance;
    private List<String> suggestions;
    private Predicate<String> filter;

    public AutoListBuilder(ConfigEntryBuilder entryBuilder, Component fieldNameKey, List<String> value) {
        super(entryBuilder.getResetButtonKey(), fieldNameKey);
        this.value = value;
    }

    public Function<String, Optional<Component>> getCellErrorSupplier() {
        return super.getCellErrorSupplier();
    }

    public AutoListBuilder setCellErrorSupplier(Function<String, Optional<Component>> cellErrorSupplier) {
        return super.setCellErrorSupplier(cellErrorSupplier);
    }

    public AutoListBuilder setErrorSupplier(Function<List<String>, Optional<Component>> errorSupplier) {
        return super.setErrorSupplier(errorSupplier);
    }

    public AutoListBuilder setInsertInFront(boolean insertInFront) {
        return super.setInsertInFront(insertInFront);
    }

    public AutoListBuilder setAddButtonTooltip(Component addTooltip) {
        return super.setAddButtonTooltip(addTooltip);
    }

    public AutoListBuilder setRemoveButtonTooltip(Component removeTooltip) {
        return super.setRemoveButtonTooltip(removeTooltip);
    }

    public AutoListBuilder requireRestart() {
        return super.requireRestart();
    }

    public AutoListBuilder setCreateNewInstance(
            Function<AutoListListEntry, AutoListListEntry.AutoListCell> createNewInstance) {
        this.createNewInstance = createNewInstance;
        return this;
    }

    public AutoListBuilder setExpanded(boolean expanded) {
        return super.setExpanded(expanded);
    }

    public AutoListBuilder setSaveConsumer(Consumer<List<String>> saveConsumer) {
        return super.setSaveConsumer(saveConsumer);
    }

    public AutoListBuilder setDefaultValue(Supplier<List<String>> defaultValue) {
        return super.setDefaultValue(defaultValue);
    }

    public AutoListBuilder setDefaultValue(List<String> defaultValue) {
        return super.setDefaultValue(defaultValue);
    }

    public AutoListBuilder setTooltipSupplier(Function<List<String>, Optional<Component[]>> tooltipSupplier) {
        return super.setTooltipSupplier(tooltipSupplier);
    }

    public AutoListBuilder setTooltipSupplier(Supplier<Optional<Component[]>> tooltipSupplier) {
        return super.setTooltipSupplier(tooltipSupplier);
    }

    public AutoListBuilder setTooltip(Optional<Component[]> tooltip) {
        return super.setTooltip(tooltip);
    }

    public AutoListBuilder setTooltip(Component... tooltip) {
        return super.setTooltip(tooltip);
    }

    public AutoListBuilder setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
        return this;
    }

    public AutoListBuilder setFilter(Predicate<String> filter) {
        this.filter = filter;
        return this;
    }

    public @NotNull AutoListListEntry build() {
        AutoListListEntry entry = new AutoListListEntry(
                this.getFieldNameKey(),
                this.value,
                this.isExpanded(),
                null,
                this.getSaveConsumer(),
                this.defaultValue,
                this.getResetButtonKey(),
                this.isRequireRestart(),
                this.isDeleteButtonEnabled(),
                this.isInsertInFront(),
                this.suggestions,
                this.filter
        );

        if (this.createNewInstance != null) {
            entry.setCreateNewInstance(this.createNewInstance);
            entry.reinitializeCells();
        }

        entry.setInsertButtonEnabled(this.isInsertButtonEnabled());
        entry.setCellErrorSupplier(this.cellErrorSupplier);
        entry.setTooltipSupplier(() -> this.getTooltipSupplier().apply(entry.getValue()));
        entry.setAddTooltip(this.getAddTooltip());
        entry.setRemoveTooltip(this.getRemoveTooltip());
        if (this.errorSupplier != null) {
            entry.setErrorSupplier(() -> this.errorSupplier.apply(entry.getValue()));
        }

        return this.finishBuilding(entry);
    }
}
