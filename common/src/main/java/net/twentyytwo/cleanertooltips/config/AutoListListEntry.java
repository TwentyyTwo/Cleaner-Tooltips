package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.AbstractTextFieldListListEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractListBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.twentyytwo.cleanertooltips.config.AutoListListEntry.AutoListCell;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class AutoListListEntry extends AbstractTextFieldListListEntry<String, AutoListCell, AutoListListEntry> {

    public AutoListListEntry(Component fieldName,
                             List<String> value,
                             boolean defaultExpanded,
                             Supplier<Optional<Component[]>> tooltipSupplier,
                             Consumer<List<String>> saveConsumer,
                             Supplier<List<String>> defaultValue,
                             Component resetButtonKey,
                             boolean requiresRestart,
                             boolean deleteButtonEnabled,
                             boolean insertInFront,
                             List<String> suggestions,
                             Predicate<String> filter) {
        super(
                fieldName,
                value,
                defaultExpanded,
                tooltipSupplier,
                saveConsumer,
                defaultValue,
                resetButtonKey,
                requiresRestart,
                deleteButtonEnabled,
                insertInFront,
                (value1, listListEntry) -> new AutoListCell(value1, listListEntry, suggestions, filter)
        );
    }

    public AutoListListEntry self() {
        return this;
    }

    public static class AutoListCell extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<String, AutoListCell, AutoListListEntry> {

        public AutoListCell(String value, AutoListListEntry listListEntry, List<String> suggestions, Predicate<String> filter) {
            super(value, listListEntry);

            // Replace the EditBox with our custom AutoEditBox class.
            this.widget = new AutoEditBox(suggestions);
            this.widget.setFilter(filter);
            this.widget.setMaxLength(Integer.MAX_VALUE);
            this.widget.setBordered(false);
            this.widget.setValue(Objects.toString(this.substituteDefault(value)));
            this.widget.moveCursorToStart(false);
            this.widget.setResponder(getWidget()::onValueChange);
        }

        public AutoEditBox getWidget() {
            return (AutoEditBox) this.widget;
        }

        protected @Nullable String substituteDefault(@Nullable String value) {
            return value == null ? "" : value;
        }

        protected boolean isValidText(@NotNull String s) {
            return true;
        }

        public String getValue() {
            return this.widget.getValue();
        }

        public Optional<Component> getError() {
            return Optional.empty();
        }
    }

    public static class Builder extends AbstractListBuilder<String, AutoListListEntry, Builder> {
        private Function<AutoListListEntry, AutoListCell> createNewInstance;
        private List<String> suggestions;
        private Predicate<String> filter;

        public Builder(ConfigEntryBuilder entryBuilder, Component fieldNameKey, List<String> value) {
            super(entryBuilder.getResetButtonKey(), fieldNameKey);
            this.value = value;
        }

        public Builder setCreateNewInstance(Function<AutoListListEntry, AutoListCell> createNewInstance) {
            this.createNewInstance = createNewInstance;
            return this;
        }

        public Builder setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
            return this;
        }

        public Builder setFilter(Predicate<String> filter) {
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
