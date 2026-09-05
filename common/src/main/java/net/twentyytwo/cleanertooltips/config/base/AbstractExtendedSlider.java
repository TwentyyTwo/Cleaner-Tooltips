package net.twentyytwo.cleanertooltips.config.base;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

/**
 * Slider widget implementation which allows inputting values in a certain range with optional step size.
 */
public abstract class AbstractExtendedSlider extends AbstractSliderButton {
    private final DecimalFormat format;

    protected Component prefix, suffix;
    protected double minValue, maxValue;

    protected double stepSize;

    protected boolean drawString;

    public AbstractExtendedSlider(int x, int y, int width, int height, Component prefix, Component suffix,
                                  double minValue, double maxValue, double value, boolean drawString) {
        this(x, y, width, height, prefix, suffix, minValue, maxValue, 1.0D, 0, value, drawString);
    }

    public AbstractExtendedSlider(int x, int y, int width, int height,
                                  Component prefix, Component suffix,
                                  double minValue, double maxValue,
                                  double stepSize, int precision,
                                  double currentValue, boolean drawString) {
        super(x, y, width, height, Component.empty(), 0.0D);

        this.prefix = prefix;
        this.suffix = suffix;

        this.minValue = minValue;
        this.maxValue = maxValue;

        this.stepSize = Math.abs(stepSize);
        this.value = this.snapToNearest((currentValue - minValue) / (maxValue - minValue));

        this.drawString = drawString;

        if (stepSize == 0D) {
            precision = Math.min(precision, 4);
            StringBuilder builder = new StringBuilder("0");

            if (precision > 0) builder.append('.');
            while (precision-- > 0) builder.append('0');

            this.format = new DecimalFormat(builder.toString());
        } else if (Mth.equal(this.stepSize, Mth.floor(this.stepSize))) {
            this.format = new DecimalFormat("0");
        } else {
            this.format = new DecimalFormat(Double.toString(this.stepSize).replaceAll("\\d", "0"));
        }

        this.updateMessage();
    }

    /**
     * @return Current slider value as a double
     */
    public double getValue() {
        return this.value * (maxValue - minValue) + minValue;
    }

    public double getValueLong() {
        return Math.round(this.getValue());
    }

    public int getValueInt() {
        return (int) getValueLong();
    }

    public String getValueString() {
        return this.format.format(this.getValue());
    }

    public void setValue(double value) {
        this.setFractionalValue((value - this.minValue) / (this.maxValue - this.minValue));
    }

    private void setValueFromMouse(double mouseX) {
        this.setFractionalValue((mouseX - (this.getX() + 4)) / (this.getWidth() - 8));
    }

    private void setFractionalValue(double fractionalValue) {
        double oldValue = this.value;
        this.value = snapToNearest(fractionalValue);

        if (!Mth.equal(oldValue, this.value)) this.applyValue();
        this.updateMessage();
    }

    /**
     * Snaps the value, so that the displayed value is the nearest multiple of {@code stepSize}.
     * If {@code stepSize} is 0, no snapping occurs.
     *
     * @param value fractional progress between 0 and 1
     * @return      fractional progress between 0 and 1, snapped to the nearest allowed value
     */
    private double snapToNearest(double value) {
        if (this.stepSize <= 0d) return Mth.clamp(value, 0d, 1d);

        value = Mth.lerp(Mth.clamp(value, 0d, 1d), this.minValue, this.maxValue);
        value = (this.stepSize * Math.round(value / this.stepSize));

        if (this.minValue > this.maxValue) {
            value = Mth.clamp(value, this.maxValue, this.minValue);
        } else {
            value = Mth.clamp(value, this.minValue, this.maxValue);
        }

        return Mth.map(value, this.minValue, this.maxValue, 0d, 1d);
    }

    @Override
    public void onClick(@NotNull MouseButtonEvent event, boolean doubleClick) {
        this.setValueFromMouse(event.x());
    }

    @Override
    protected void onDrag(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        super.onDrag(event, dragX, dragY);
        this.setValueFromMouse(event.x());
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent keyEvent) {
        boolean isLeft = keyEvent.isLeft();
        if (isLeft || keyEvent.isRight()) {
            if (this.minValue > this.maxValue) isLeft = !isLeft;

            float f = isLeft ? -1.0f : 1.0f;
            if (stepSize <= 0.0D) {
                this.setFractionalValue(this.value + (f / (this.getWidth() - 8)));
            } else {
                this.setValue(this.getValue() + f * this.stepSize);
            }
            return true;
        }

        return false;
    }

    @Override
    protected void updateMessage() {
        if (this.drawString) {
            this.setMessage(Component.empty().append(prefix).append(this.getValueString()).append(suffix));
        } else {
            this.setMessage(Component.empty());
        }
    }
}