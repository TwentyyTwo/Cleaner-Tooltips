package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.AbstractTextFieldListListEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractListBuilder;
import net.minecraft.network.chat.Component;
import net.twentyytwo.cleanertooltips.config.FilterableListListEntry.FilterableListCell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class FilterableListListEntry extends AbstractTextFieldListListEntry<String, FilterableListCell, FilterableListListEntry> {

    public FilterableListListEntry(Component fieldName,
                                   List<String> value,
                                   boolean defaultExpanded,
                                   Supplier<Optional<Component[]>> tooltipSupplier,
                                   Consumer<List<String>> saveConsumer,
                                   Supplier<List<String>> defaultValue,
                                   Component resetButtonKey,
                                   boolean requiresRestart,
                                   boolean deleteButtonEnabled,
                                   boolean insertInFront,
                                   Predicate<String> filter) {
        super(fieldName,
              value,
              defaultExpanded,
              tooltipSupplier,
              saveConsumer,
              defaultValue,
              resetButtonKey,
              requiresRestart,
              deleteButtonEnabled,
              insertInFront,
              (string, listListEntry) -> new FilterableListCell(string, listListEntry, filter));
    }

    @Override
    public FilterableListListEntry self() {
        return this;
    }

    public static class FilterableListCell extends AbstractTextFieldListCell<String, FilterableListCell, FilterableListListEntry> {

        public FilterableListCell(@Nullable String value, FilterableListListEntry listListEntry, Predicate<String> filter) {
            super(value, listListEntry);
            this.widget.setFilter(filter);
        }

        @Override
        protected @Nullable String substituteDefault(@Nullable String value) {
            return value == null ? "" : value;
        }

        @Override
        protected boolean isValidText(@NotNull String s) {
            return true;
        }

        @Override
        public String getValue() {
            return this.widget.getValue();
        }

        @Override
        public Optional<Component> getError() {
            return Optional.empty();
        }
    }

    public static class Builder extends AbstractListBuilder<String, FilterableListListEntry, Builder> {
        private Function<FilterableListListEntry, FilterableListCell> createNewInstance;
        private Predicate<String> filter;

        protected Builder(ConfigEntryBuilder entryBuilder, Component fieldNameKey, List<String> value) {
            super(entryBuilder.getResetButtonKey() , fieldNameKey);
            this.value = value;
        }

        public Builder setCreateNewInstance(
                Function<FilterableListListEntry, FilterableListListEntry.FilterableListCell> createNewInstance) {
            this.createNewInstance = createNewInstance;
            return this;
        }

        public Builder setFilter(Predicate<String> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public @NotNull FilterableListListEntry build() {
            FilterableListListEntry entry = new FilterableListListEntry(
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
                    this.filter
            );

            if (this.createNewInstance != null) {
                entry.setCreateNewInstance(this.createNewInstance);
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
}
