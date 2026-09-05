package net.twentyytwo.cleanertooltips.config.base;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractFieldBuilder;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

@SuppressWarnings({"unchecked", "unused"})
public abstract class AbstractMapBuilder<K, V, A extends AbstractConfigListEntry<?>, SELF extends AbstractMapBuilder<K, V, A, SELF>>
        extends AbstractFieldBuilder<Map<K, V>, A, SELF> {
    protected BiFunction<K, V, Optional<Component>> cellErrorSupplier;

    private boolean expanded = false;

    private Component addTooltip = Component.translatable("text.cloth-config.list.add");
    private Component removeTooltip = Component.translatable("text.cloth-config.list.remove");

    private boolean insertButtonEnabled = true, deleteButtonEnabled = true, insertInFront = false;

    protected AbstractMapBuilder(Component resetButtonKey, Component fieldNameKey) {
        super(resetButtonKey, fieldNameKey);
    }

    public BiFunction<K, V, Optional<Component>> getCellErrorSupplier() {
        return cellErrorSupplier;
    }

    public SELF setCellErrorSupplier(BiFunction<K, V, Optional<Component>> cellErrorSupplier) {
        this.cellErrorSupplier = cellErrorSupplier;
        return (SELF) this;
    }

    public SELF setInsertButtonEnabled(boolean insertButtonEnabled) {
        this.insertButtonEnabled = insertButtonEnabled;
        return (SELF) this;
    }

    public SELF setDeleteButtonEnabled(boolean deleteButtonEnabled) {
        this.deleteButtonEnabled = deleteButtonEnabled;
        return (SELF) this;
    }

    public SELF setInsertInFront(boolean insertInFront) {
        this.insertInFront = insertInFront;
        return (SELF) this;
    }

    public SELF setAddButtonTooltip(Component addTooltip) {
        this.addTooltip = addTooltip;
        return (SELF) this;
    }

    public SELF setRemoveButtonTooltip(Component removeTooltip) {
        this.removeTooltip = removeTooltip;
        return (SELF) this;
    }

    public SELF setExpanded(boolean expanded) {
        this.expanded = expanded;
        return (SELF) this;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public Component getAddTooltip() {
        return addTooltip;
    }

    public Component getRemoveTooltip() {
        return removeTooltip;
    }

    public boolean isInsertButtonEnabled() {
        return insertButtonEnabled;
    }

    public boolean isDeleteButtonEnabled() {
        return deleteButtonEnabled;
    }

    public boolean isInsertInFront() {
        return insertInFront;
    }
}
