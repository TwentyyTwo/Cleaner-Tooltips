package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.clothconfig2.gui.entries.AbstractTextFieldListListEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.twentyytwo.cleanertooltips.config.AutoListListEntry.AutoListCell;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class AutoListListEntry extends AbstractTextFieldListListEntry<String, AutoListCell, AutoListListEntry> {
    private final List<String> suggestions;
    private final Predicate<String> filter;

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
        this.suggestions = suggestions;
        this.filter = filter;
    }

    public void reinitializeCells() {
        List<String> values = this.getValue();
        this.widgets.removeAll(this.cells);
        this.cells.clear();
        for (var s : values) {
            AutoListCell cell = new AutoListCell(s, this, suggestions, filter);
            this.cells.add(cell);
        }
        this.widgets.addAll(this.cells);
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
}
