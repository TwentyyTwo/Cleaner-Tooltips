package net.twentyytwo.cleanertooltips.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public enum Comparison {
    NONE,
    HIGHER,
    LOWER;

    public static Comparison getComparison(double entryValue, double comparedValue) {
        if (entryValue > comparedValue) {
            return HIGHER;
        } else if (entryValue < comparedValue) {
            return LOWER;
        } else {
            return NONE;
        }
    }

    public ChatFormatting getFormatting(MutableComponent component) {
        switch (this) {
            case HIGHER -> {
                return ChatFormatting.GREEN;
            }
            case LOWER -> {
                if (component.getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.RED)) {
                    return ChatFormatting.DARK_RED;
                } else {
                    return ChatFormatting.RED;
                }
            }
        }
        return ChatFormatting.WHITE;
    }
}
