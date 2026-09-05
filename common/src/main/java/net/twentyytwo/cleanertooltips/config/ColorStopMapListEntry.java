package net.twentyytwo.cleanertooltips.config;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.widget.ColorDisplayWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.twentyytwo.cleanertooltips.config.ColorStopMapListEntry.ColorStopMapCell;
import net.twentyytwo.cleanertooltips.config.base.AbstractExtendedSlider;
import net.twentyytwo.cleanertooltips.config.base.AbstractMapBuilder;
import net.twentyytwo.cleanertooltips.config.base.AbstractMapListEntry;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ColorStopMapListEntry extends AbstractMapListEntry<Integer, Integer, ColorStopMapCell, ColorStopMapListEntry> {

    public ColorStopMapListEntry(@NotNull Component fieldName,
                                 Map<Integer, Integer> value,
                                 @Nullable Supplier<Optional<Component[]>> tooltipSupplier,
                                 @Nullable Supplier<Map<Integer, Integer>> defaultValue,
                                 @Nullable Consumer<Map<Integer, Integer>> saveConsumer,
                                 Component resetButtonKey,
                                 boolean defaultExpanded,
                                 boolean requiresRestart,
                                 boolean deleteButtonEnabled,
                                 boolean insertInFront) {
        super(fieldName,
              value,
              tooltipSupplier,
              defaultValue,
              saveConsumer,
              resetButtonKey,
              defaultExpanded,
              requiresRestart,
              deleteButtonEnabled,
              insertInFront,
              ColorStopMapCell::new);
    }

    @Override
    public ColorStopMapListEntry self() {
        return this;
    }

    private void moveUp(ColorStopMapCell cell) {
        int index = this.cells.indexOf(cell);
        if (index > 0) {
            Collections.swap(this.cells, index, index - 1);
            Collections.swap(this.widgets, index, index - 1);
        }
    }

    private void moveDown(ColorStopMapCell cell) {
        int index = this.cells.indexOf(cell);
        if (index >= 0 && index < this.cells.size() - 1) {
            Collections.swap(this.cells, index, index + 1);
            Collections.swap(this.widgets, index, index + 1);
        }
    }

    public static class ColorStopMapCell extends AbstractMapCell<Integer, Integer, ColorStopMapCell, ColorStopMapListEntry> {
        private boolean isSelected = false;

        private final Slider sliderWidget;
        private final ColorDisplayWidget colorDisplay;
        private final EditBox colorField;
        private final Button upButton;
        private final Button downButton;

        protected final List<AbstractWidget> widgets;

        public ColorStopMapCell(Integer key, Integer value, ColorStopMapListEntry mapListEntry) {
            super(key, value, mapListEntry);

            ColorValue colorValue = getColorValue(String.valueOf(value));
            if (colorValue.getError() != null) {
                throw new IllegalArgumentException("Invalid Color: " + colorValue.getError().name());
            }

            this.sliderWidget = new Slider(0, 0, 152, 20, 0, 100, key);
            this.colorField = new EditBox(Minecraft.getInstance().font, 0, 0, 148, 18, Component.empty()) {
                @Override
                public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                    setFocused(isSelected && ColorStopMapCell.this.getFocused() == this);
                    textFieldPreRender(this);
                    super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
                }
            };
            this.colorField.setValue(getHexColorString(value));
            this.colorField.setResponder(s -> this.colorField.setTextColor(this.getPreferredTextColor()));
            this.colorDisplay = new ColorDisplayWidget(this.colorField, 0, 0, 18,
                                                       getColorValueColor(this.colorField.getValue()));

            this.upButton = Button.builder(Component.literal("↑"), b -> mapListEntry.moveUp(this))
                                  .bounds(0, 0, 14, 20).build();
            this.downButton = Button.builder(Component.literal("↓"), b -> mapListEntry.moveDown(this))
                                    .bounds(0, 0, 14, 20).build();

            this.widgets = Lists.newArrayList(this.sliderWidget, this.colorField, this.upButton, this.downButton);
        }

        @Override
        public void updateSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }

        @Override
        public Integer getKey() {
            return this.sliderWidget.getValueInt();
        }

        @Override
        public Integer getValue() {
            return this.getColorValueColor(this.colorField.getValue());
        }

        @Override
        public Optional<Component> getError() {
            ColorValue colorValue = this.getColorValue(this.colorField.getValue());
            if (colorValue.getError() != null) {
                String errorName = colorValue.getError().name();
                return Optional.of(Component.translatable("text.cloth-config.error.color." + errorName.toLowerCase(Locale.ROOT)));
            }
            return Optional.empty();
        }

        @Override
        public int getCellHeight() {
            return 20;
        }

        protected void textFieldPreRender(EditBox widget) {
            widget.setTextColor(getConfigError().isPresent() ? 0xffff5555 : 0xffe0e0e0);
        }

        @Override
        public void render(GuiGraphics graphics,
                           int index, int y, int x,
                           int entryWidth, int entryHeight,
                           int mouseX, int mouseY,
                           boolean isSelected,
                           float delta) {
            int cellIndex = mapListEntry.cells.indexOf(this);

            this.downButton.active = mapListEntry.isEditable() && cellIndex < mapListEntry.cells.size() - 1;
            this.downButton.setX(x + entryWidth - this.downButton.getWidth());
            this.downButton.setY(y);

            this.upButton.active = mapListEntry.isEditable() && cellIndex > 0;
            this.upButton.setX(this.downButton.getX() - this.upButton.getWidth() - 2);
            this.upButton.setY(y);

            this.colorField.setEditable(this.mapListEntry.isEditable());
            this.colorField.setY(y + 1);
            this.colorField.setX(x + entryWidth - 148);
            this.colorField.setWidth(148 - (this.downButton.getRight() - this.upButton.getX()) - 3);

            ColorValue colorValue = this.getColorValue(this.colorField.getValue());
            if (!colorValue.hasError()) {
                this.colorDisplay.setColor(0xff000000 | colorValue.getColor());
            }
            this.colorDisplay.setY(y + 1);
            this.colorDisplay.setX(this.colorField.getX() - 21);

            this.sliderWidget.setWidth(entryWidth - (this.downButton.getRight() - this.colorDisplay.getX()) - 2);
            this.sliderWidget.setX(x);
            this.sliderWidget.setY(y);

            this.downButton.render(graphics, mouseX, mouseY, delta);
            this.upButton.render(graphics, mouseX, mouseY, delta);
            this.colorField.render(graphics, mouseX, mouseY, delta);
            this.colorDisplay.render(graphics, mouseX, mouseY, delta);
            this.sliderWidget.render(graphics, mouseX, mouseY, delta);
        }

        @Override
        public @NotNull List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        public @NotNull NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NotNull NarrationElementOutput narrationElementOutput) {
            this.sliderWidget.updateNarration(narrationElementOutput);
            this.colorField.updateNarration(narrationElementOutput);
        }

        protected int getColorValueColor(String str) {
            return getColorValue(str).getColor();
        }

        protected ColorValue getColorValue(String str) {
            try {
                int color;

                if (str.startsWith("#")) {
                    String strWithoutHex = removeHex(str);
                    if (strWithoutHex.length() > 8) return ColorError.INVALID_COLOR.toValue();
                    if (strWithoutHex.length() > 6) return ColorError.NO_ALPHA_ALLOWED.toValue();
                    color = (int) Long.parseLong(strWithoutHex, 16);
                } else {
                    color = (int) Long.parseLong(str);
                }

                if ((color >> 24 & 255) > 0) return ColorError.NO_ALPHA_ALLOWED.toValue();

                return new ColorValue(color);
            } catch (NumberFormatException e) {
                return ColorError.INVALID_COLOR.toValue();
            }
        }

        protected String getHexColorString(int color) {
            return "#" + StringUtils.leftPad(Integer.toHexString(color), 6, '0');
        }

        protected String removeHex(String hex) {
            return hex.startsWith("#") ? hex.substring(1) : hex;
        }

        private static class Slider extends AbstractExtendedSlider {

            public Slider(int x, int y, int width, int height, double minValue, double maxValue, double value) {
                super(x, y, width, height, Component.empty(), Component.literal("%"), minValue, maxValue, value, true);
            }

            @Override
            protected void applyValue() {
            }
        }

        protected enum ColorError {
            NO_ALPHA_ALLOWED,
            INVALID_RED,
            INVALID_GREEN,
            INVALID_BLUE,
            INVALID_COLOR;

            private final ColorValue value;

            ColorError() {
                this.value = new ColorValue(this);
            }

            public ColorValue toValue() {
                return this.value;
            }
        }

        protected static class ColorValue {
            private int color = -1;
            private @Nullable ColorError error = null;

            public ColorValue(int color) {
                this.color = color;
            }

            public ColorValue(@Nullable ColorError error) {
                this.error = error;
            }

            public int getColor() {
                return color;
            }

            public @Nullable ColorError getError() {
                return error;
            }

            public boolean hasError() {
                return this.getError() != null;
            }
        }
    }

    public static class Builder extends AbstractMapBuilder<Integer, Integer, ColorStopMapListEntry, Builder> {
        private Function<ColorStopMapListEntry, ColorStopMapCell> createNewInstance;

        protected Builder(ConfigEntryBuilder entryBuilder, Component fieldNameKey, Map<Integer, Integer> value) {
            super(entryBuilder.getResetButtonKey(), fieldNameKey);
            this.value = value;
        }

        public Builder setCreateNewInstance(Function<ColorStopMapListEntry, ColorStopMapCell> createNewInstance) {
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
}