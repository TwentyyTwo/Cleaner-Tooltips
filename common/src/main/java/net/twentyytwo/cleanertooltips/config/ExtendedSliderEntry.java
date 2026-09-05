package net.twentyytwo.cleanertooltips.config;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import me.shedaniel.clothconfig2.impl.builders.AbstractSliderFieldBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.twentyytwo.cleanertooltips.config.base.AbstractExtendedSlider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings({"deprecation", "unused", "UnstableApiUsage"})
public class ExtendedSliderEntry extends TooltipListEntry<Integer> {
    protected Slider sliderWidget;
    protected Button resetButton;

    protected AtomicInteger value;
    protected final long original;

    private final Supplier<Integer> defaultValue;
    private final List<AbstractWidget> widgets;

    public ExtendedSliderEntry(Component fieldName,
                               int minimum, int maximum,
                               Component prefix, Component suffix,
                               int value,
                               Component resetButtonKey,
                               Supplier<Integer> defaultValue,
                               Consumer<Integer> saveConsumer,
                               @Nullable Supplier<Optional<Component[]>> tooltipSupplier,
                               boolean requiresRestart) {
        super(fieldName, tooltipSupplier, requiresRestart);

        this.value = new AtomicInteger(value);
        this.defaultValue = defaultValue;
        this.original = value;

        this.saveCallback = saveConsumer;

        this.sliderWidget = new Slider(0, 0, 152, 20, prefix, suffix, minimum, maximum, this.value.get(), true);
        this.resetButton = Button.builder(resetButtonKey, b -> {
            this.sliderWidget.setValue(defaultValue.get());
            this.value.set(defaultValue.get());
        }).bounds(0, 0, Minecraft.getInstance().font.width(resetButtonKey) + 6, 20).build();

        this.widgets = Lists.newArrayList(this.sliderWidget, this.resetButton);
    }

    @Override
    public Integer getValue() {
        return this.value.get();
    }

    @Override
    public Optional<Integer> getDefaultValue() {
        if (this.defaultValue == null) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(this.defaultValue.get());
        }
    }

    @Override
    public boolean isEdited() {
        return super.isEdited() || this.getValue() != this.original;
    }

    @Override
    public @NotNull List<? extends GuiEventListener> children() {
        return this.widgets;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return this.widgets;
    }

    @Override
    public void render(GuiGraphics graphics,
                       int index, int y, int x,
                       int entryWidth, int entryHeight,
                       int mouseX, int mouseY,
                       boolean isHovered,
                       float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        this.resetButton.active = this.isEditable() && this.getDefaultValue().isPresent()
                && this.defaultValue.get() != this.value.get();
        this.resetButton.setX(x + entryWidth - this.resetButton.getWidth());
        this.resetButton.setY(y);

        this.sliderWidget.active = this.isEditable();
        this.sliderWidget.setWidth(150 - this.resetButton.getWidth() - 2);
        this.sliderWidget.setX(x + entryWidth - 150);
        this.sliderWidget.setY(y);

        graphics.drawString(Minecraft.getInstance().font, this.getDisplayedFieldName().getVisualOrderText(),
                            x, y + 6, this.getPreferredTextColor());

        this.resetButton.render(graphics, mouseX, mouseY, delta);
        this.sliderWidget.render(graphics, mouseX, mouseY, delta);
    }

    protected class Slider extends AbstractExtendedSlider {

        public Slider(int x, int y, int width, int height,
                      Component prefix, Component suffix,
                      double minValue, double maxValue,
                      double value, boolean drawString) {
            super(x, y, width, height, prefix, suffix, minValue, maxValue, value, drawString);
        }

        @Override
        protected void applyValue() {
            ExtendedSliderEntry.this.value.set(this.getValueInt());
        }


    }

    public static class Builder extends AbstractSliderFieldBuilder<Integer, ExtendedSliderEntry, Builder> {
        protected Component prefix = Component.empty();
        protected Component suffix = Component.empty();

        protected Builder(ConfigEntryBuilder entryBuilder, Component fieldNameKey, int value, int min, int max) {
            super(entryBuilder.getResetButtonKey(), fieldNameKey);
            this.value = value;
            this.min = min;
            this.max = max;
        }

        public Builder setPrefix(Component prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder setSuffix(Component suffix) {
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
}
