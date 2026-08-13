package net.twentyytwo.cleanertooltips.config.base;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import me.shedaniel.clothconfig2.api.Expandable;
import me.shedaniel.clothconfig2.api.ReferenceProvider;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @param <K>    the configuration object key type
 * @param <V>    the configuration object value type
 * @param <C>    the cell type
 * @param <SELF> the "curiously recurring template pattern" type parameter
 * @see me.shedaniel.clothconfig2.gui.entries.BaseListEntry
 */
@SuppressWarnings({"deprecation", "UnstableApiUsage", "unused"})
public abstract class BaseMapEntry<K, V, C extends BaseMapCell, SELF extends BaseMapEntry<K, V, C, SELF>>
        extends TooltipListEntry<Map<K, V>> implements Expandable {
    protected static final ResourceLocation CONFIG_TEXTURE = ResourceLocation
            .fromNamespaceAndPath("cloth-config2", "textures/gui/cloth_config.png");

    protected final @NotNull List<C> cells;
    protected final @NotNull List<GuiEventListener> widgets;
    protected final @NotNull List<NarratableEntry> narratables;

    protected boolean expanded;
    protected boolean insertButtonEnabled = true;
    protected boolean deleteButtonEnabled;
    protected boolean insertInFront;

    protected MapLabelWidget labelWidget;
    protected AbstractWidget resetWidget;
    protected @NotNull Function<SELF, C> createNewInstance;
    protected @NotNull Supplier<Map<K, V>> defaultValue;

    protected @Nullable Component addTooltip = Component.translatable("text.cloth-config.list.add");
    protected @Nullable Component removeTooltip = Component.translatable("text.cloth-config.list.remove");

    public BaseMapEntry(@NotNull Component fieldName,
                        @Nullable Supplier<Optional<Component[]>> tooltipSupplier,
                        @Nullable Supplier<Map<K, V>> defaultValue,
                        @NotNull Function<SELF, C> createNewInstance,
                        @Nullable Consumer<Map<K, V>> saveConsumer,
                        Component resetButtonKey) {
        this(fieldName, tooltipSupplier, defaultValue, createNewInstance, saveConsumer, resetButtonKey, false);
    }

    public BaseMapEntry(@NotNull Component fieldName,
                        @Nullable Supplier<Optional<Component[]>> tooltipSupplier,
                        @Nullable Supplier<Map<K, V>> defaultValue,
                        @NotNull Function<SELF, C> createNewInstance,
                        @Nullable Consumer<Map<K, V>> saveConsumer,
                        Component resetButtonKey,
                        boolean requiresRestart) {
        this(fieldName, tooltipSupplier, defaultValue, createNewInstance, saveConsumer,
                resetButtonKey, requiresRestart, true, true);
    }

    public BaseMapEntry(@NotNull Component fieldName,
                        @Nullable Supplier<Optional<Component[]>> tooltipSupplier,
                        @Nullable Supplier<Map<K, V>> defaultValue,
                        @NotNull Function<SELF, C> createNewInstance,
                        @Nullable Consumer<Map<K, V>> saveConsumer,
                        Component resetButtonKey,
                        boolean requiresRestart,
                        boolean deleteButtonEnabled,
                        boolean insertInFront) {
        super(fieldName, tooltipSupplier, requiresRestart);
        this.deleteButtonEnabled = deleteButtonEnabled;
        this.insertInFront = insertInFront;

        this.cells = new ArrayList<>();
        this.labelWidget = new MapLabelWidget();
        this.widgets = Lists.newArrayList(new GuiEventListener[]{this.labelWidget});
        this.narratables = new ArrayList<>();

        this.resetWidget = Button.builder(resetButtonKey, widget -> {
            this.widgets.removeAll(this.cells);
            this.narratables.removeAll(this.cells);

            for (C cell : this.cells) cell.onDelete();

            this.cells.clear();
            defaultValue.get().forEach((k, v) -> cells.add(this.getFromValue(k, v)));
            this.cells.forEach(BaseMapCell::onAdd);

            this.widgets.addAll(this.cells);
            this.narratables.addAll(this.cells);

        }).bounds(0, 0, Minecraft.getInstance().font.width(resetButtonKey) + 6, 20).build();

        this.widgets.add(resetWidget);
        this.saveCallback = saveConsumer;
        this.createNewInstance = createNewInstance;
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isExpanded() {
        return this.expanded && this.isEnabled();
    }

    @Override
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public boolean isEdited() {
        if (super.isEdited()) return true;
        return cells.stream().anyMatch(BaseMapCell::isEdited);
    }

    public boolean isMatchDefault() {
        Optional<Map<K, V>> defaultValueOptional = getDefaultValue();
        if (defaultValueOptional.isPresent()) {
            Map<K, V> value = getValue();
            Map<K, V> defaultValue = defaultValueOptional.get();
            return TooltipsUtil.equalsOrdered(value, defaultValue);
        }
        return false;
    }

    @Override
    public boolean isRequiresRestart() {
        return this.cells.stream().anyMatch(BaseMapCell::isRequiresRestart);
    }

    @Override
    public void setRequiresRestart(boolean requiresRestart) {
    }

    public abstract SELF self();

    public boolean isDeleteButtonEnabled() {
        return this.deleteButtonEnabled && isEnabled();
    }

    public boolean isInsertButtonEnabled() {
        return this.insertButtonEnabled && isEnabled();
    }

    public void setDeleteButtonEnabled(boolean deleteButtonEnabled) {
        this.deleteButtonEnabled = deleteButtonEnabled;
    }

    public void setInsertButtonEnabled(boolean insertButtonEnabled) {
        this.insertButtonEnabled = insertButtonEnabled;
    }

    protected abstract C getFromValue(K key, V value);

    public @NotNull Function<SELF, C> getCreateNewInstance() {
        return this.createNewInstance;
    }

    public void setCreateNewInstance(@NotNull Function<SELF, C> createNewInstance) {
        this.createNewInstance = createNewInstance;
    }

    public @Nullable Component getAddTooltip() {
        return this.addTooltip;
    }

    public void setAddTooltip(@Nullable Component addTooltip) {
        this.addTooltip = addTooltip;
    }

    public @Nullable Component getRemoveTooltip() {
        return this.removeTooltip;
    }

    public void setRemoveTooltip(@Nullable Component removeTooltip) {
        this.removeTooltip = removeTooltip;
    }

    @Override
    public Optional<Map<K, V>> getDefaultValue() {
        if (this.defaultValue == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(this.defaultValue.get());
        }
    }

    @Override
    public int getItemHeight() {
        int i = 24;
        if (isExpanded()) {
            for (BaseMapCell cell : this.cells) i += cell.getCellHeight();
        }
        return i;
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        if (!isExpanded()) {
            List<GuiEventListener> elements = new ArrayList<>(this.widgets);
            elements.removeAll(cells);
            return elements;
        }
        return this.widgets;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return this.narratables;
    }

    @Override
    public Optional<Component> getError() {
        List<Component> errors = this.cells.stream()
                                           .map(C::getConfigError)
                                           .filter(Optional::isPresent)
                                           .map(Optional::get).toList();

        if (errors.size() > 1) {
            return Optional.of(Component.translatable("text.cloth-config.multi_error"));
        } else {
            return errors.stream().findFirst();
        }
    }

    @Override
    public void save() {
        for (C cell : this.cells) {
            if (cell instanceof ReferenceProvider<?> provider) {
                provider.provideReferenceEntry().save();
            }
        }

        super.save();
    }

    @Override
    public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
        labelWidget.rectangle.x = x - 15;
        labelWidget.rectangle.y = y;
        labelWidget.rectangle.width = entryWidth + 15;
        labelWidget.rectangle.height = 24;
        return new Rectangle(getParent().left, y, getParent().right - getParent().left, 20);
    }

    protected boolean isInsideCreateNew(double mouseX, double mouseY) {
        return isInsertButtonEnabled()
                && mouseX >= labelWidget.rectangle.x + 12 && mouseY >= labelWidget.rectangle.y + 3
                && mouseX <= labelWidget.rectangle.x + 12 + 11 && mouseY <= labelWidget.rectangle.y + 3 + 11;
    }

    protected boolean isInsideDelete(double mouseX, double mouseY) {
        int x = isInsertButtonEnabled() ? 25 : 12;
        return isDeleteButtonEnabled()
                && mouseX >= labelWidget.rectangle.x + x && mouseY >= labelWidget.rectangle.y + 3
                && mouseX <= labelWidget.rectangle.x + x + 11 && mouseY <= labelWidget.rectangle.y + 3 + 11;
    }

    @Override
    public Optional<Component[]> getTooltip(int mouseX, int mouseY) {
        if (this.addTooltip != null && isInsideCreateNew(mouseX, mouseY)) {
            return Optional.of(new Component[]{this.addTooltip});
        }
        if (this.removeTooltip != null && isInsideDelete(mouseX, mouseY)) {
            return Optional.of(new Component[]{this.removeTooltip});
        }
        return super.getTooltip(mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int index, int y, int x,
                       int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        RenderSystem.setShaderTexture(0, CONFIG_TEXTURE);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        BaseMapCell focused = !isExpanded() || getFocused() == null
                || !(getFocused() instanceof BaseMapCell) ? null : (BaseMapCell) getFocused();

        boolean insideLabel = labelWidget.rectangle.contains(mouseX, mouseY);
        boolean insideCreateNew = isInsideCreateNew(mouseX, mouseY);
        boolean insideDelete = isInsideDelete(mouseX, mouseY);

        graphics.blit(CONFIG_TEXTURE, x - 15, y + 5, 24 + 9,
                (isEnabled() ? (insideLabel && !insideCreateNew && !insideDelete ? 18 : 0) : 36)
                        + (this.isExpanded() ? 9 : 0), 9, 9);

        if (this.isInsertButtonEnabled()) {
            graphics.blit(CONFIG_TEXTURE, x - 15 + 13, y + 5, 24 + 18, insideCreateNew ? 9 : 0, 9, 9);
        }
        if (this.isDeleteButtonEnabled()) {
            graphics.blit(CONFIG_TEXTURE,
                    x - 15 + (isInsertButtonEnabled() ? 26 : 13), y + 5, 24 + 27,
                    focused == null ? 0 : insideDelete ? 18 : 9, 9, 9);
        }

        resetWidget.setX(x + entryWidth - resetWidget.getWidth());
        resetWidget.setY(y);
        resetWidget.active = isEditable() && getDefaultValue().isPresent() && !isMatchDefault();
        resetWidget.render(graphics, mouseX, mouseY, delta);

        int offset = (isInsertButtonEnabled() || isDeleteButtonEnabled() ? 6 : 0)
                + (isInsertButtonEnabled() ? 9 : 0) + (isDeleteButtonEnabled() ? 9 : 0);
        graphics.drawString(
                Minecraft.getInstance().font,
                getDisplayedFieldName().getVisualOrderText(),
                x + offset, y + 6,
                insideLabel && !resetWidget.isMouseOver(mouseX, mouseY)
                        && !insideDelete && !insideCreateNew ? 0xffe6fe16 : getPreferredTextColor()
        );

        if (isExpanded()) {
            int yy = y + 24;
            for (BaseMapCell cell : this.cells) {
                cell.render(
                        graphics, -1, yy, x + 14, entryWidth - 14,
                        cell.getCellHeight(), mouseX, mouseY, getParent().getFocused() != null &&
                        getParent().getFocused().equals(this) && getFocused() != null && getFocused().equals(cell),
                        delta);
                yy += cell.getCellHeight();
            }
        }
    }

    @Override
    public void updateSelected(boolean isSelected) {
        for (C cell : this.cells) cell.updateSelected(isSelected && getFocused() == cell && isExpanded());
    }

    @Override
    public int getInitialReferenceOffset() {
        return 24;
    }

    public boolean insertInFront() {
        return this.insertInFront;
    }

    public class MapLabelWidget implements GuiEventListener {
        protected Rectangle rectangle = new Rectangle();

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isEnabled()) {
                return false;
            } else if (resetWidget.isMouseOver(mouseX, mouseY)) {
                return false;
            } else if (isInsideCreateNew(mouseX, mouseY)) {
                setExpanded(true);

                C cell;
                if (insertInFront()) {
                    cells.addFirst(cell = createNewInstance.apply(BaseMapEntry.this.self()));
                    widgets.addFirst(cell);
                } else {
                    cells.add(cell = createNewInstance.apply(BaseMapEntry.this.self()));
                    widgets.add(cell);
                }

                cell.onAdd();
                playClickSound();

                return true;
            } else if (isDeleteButtonEnabled() && isInsideDelete(mouseX, mouseY)) {
                GuiEventListener focused = getFocused();
                if (isExpanded() && focused instanceof BaseMapCell) {
                    ((BaseMapCell) focused).onDelete();

                    //noinspection SuspiciousMethodCalls
                    cells.remove(focused);
                    widgets.remove(focused);

                    playClickSound();
                }
                return true;
            } else if (rectangle.contains(mouseX, mouseY)) {
                setExpanded(!expanded);
                playClickSound();
                return true;
            }
            return false;
        }

        private void playClickSound() {
            Minecraft.getInstance().getSoundManager()
                     .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }

        @Override
        public void setFocused(boolean b) {
        }

        @Override
        public boolean isFocused() {
            return false;
        }
    }
}