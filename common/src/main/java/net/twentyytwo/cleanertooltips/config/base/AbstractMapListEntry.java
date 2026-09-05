package net.twentyytwo.cleanertooltips.config.base;

import me.shedaniel.math.Rectangle;
import net.minecraft.network.chat.Component;
import net.twentyytwo.cleanertooltips.config.base.AbstractMapListEntry.AbstractMapCell;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import org.apache.commons.lang3.function.TriFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @param <K>       the configuration object key type
 * @param <V>       the configuration object value type
 * @param <C>       the cell type
 * @param <SELF>    the "curiously recurring template pattern" type parameter
 */
@SuppressWarnings("unused")
public abstract class AbstractMapListEntry<K, V, C extends AbstractMapCell<K, V, C, SELF>, SELF extends AbstractMapListEntry<K, V, C, SELF>>
        extends BaseMapEntry<K, V, C, SELF> {
    protected final TriFunction<K, V, SELF, C> createNewCell;

    protected BiFunction<K, V, Optional<Component>> cellErrorSupplier;
    protected Map<K, V> original;

    public AbstractMapListEntry(@NotNull Component fieldName,
                                Map<K, V> value,
                                @Nullable Supplier<Optional<Component[]>> tooltipSupplier,
                                @Nullable Supplier<Map<K, V>> defaultValue,
                                @Nullable Consumer<Map<K, V>> saveConsumer,
                                Component resetButtonKey,
                                boolean defaultExpanded,
                                boolean requiresRestart,
                                boolean deleteButtonEnabled,
                                boolean insertInFront,
                                TriFunction<K, V, SELF, C> createNewCell) {
        super(fieldName,
              tooltipSupplier,
              defaultValue,
              self -> createNewCell.apply(null, null, self),
              saveConsumer,
              resetButtonKey,
              requiresRestart,
              deleteButtonEnabled,
              insertInFront);
        this.createNewCell = createNewCell;
        this.original = new LinkedHashMap<>(value);

        value.forEach((k, v) -> this.cells.add(createNewCell.apply(k, v, this.self())));

        this.widgets.addAll(this.cells);
        setExpanded(defaultExpanded);
    }

    public BiFunction<K, V, Optional<Component>> getCellErrorSupplier() {
        return cellErrorSupplier;
    }

    public void setCellErrorSupplier(BiFunction<K, V, Optional<Component>> cellErrorSupplier) {
        this.cellErrorSupplier = cellErrorSupplier;
    }

    @Override
    public Map<K, V> getValue() {
        return this.cells.stream().collect(Collectors.toMap(C::getKey, C::getValue, (v, v2) -> v, LinkedHashMap::new));
    }

    @Override
    protected C getFromValue(K key, V value) {
        return createNewCell.apply(key, value, this.self());
    }

    @Override
    public boolean isEdited() {
        if (super.isEdited()) {
            return true;
        } else {
            Map<K, V> value = this.getValue();
            return !TooltipsUtil.equalsOrdered(value, this.original);
        }
    }

    public abstract static class AbstractMapCell<K, V, SELF extends AbstractMapCell<K, V, SELF, OUTER_SELF>, OUTER_SELF extends AbstractMapListEntry<K, V, SELF, OUTER_SELF>>
            extends BaseMapCell {
        protected final OUTER_SELF mapListEntry;
        protected final Rectangle cellBounds = new Rectangle();

        public AbstractMapCell(@Nullable K key, @Nullable V value, OUTER_SELF mapListEntry) {
            this.mapListEntry = mapListEntry;
            this.setErrorSupplier(() -> Optional.ofNullable(mapListEntry.cellErrorSupplier)
                                                .flatMap(cellErrorFn -> cellErrorFn.apply(getKey(), getValue())));
        }

        public abstract K getKey();

        public abstract V getValue();

        @Override
        public void updateBounds(boolean expanded, int x, int y, int entryWidth, int entryHeight) {
            if (expanded) {
                this.cellBounds.setBounds(x, y, entryWidth, entryHeight);
            } else {
                this.cellBounds.setBounds(0, 0, 0, 0);
            }
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return cellBounds.contains(mouseX, mouseY);
        }
    }
}
