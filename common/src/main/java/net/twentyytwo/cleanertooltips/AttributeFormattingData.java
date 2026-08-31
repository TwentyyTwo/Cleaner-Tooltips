package net.twentyytwo.cleanertooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.twentyytwo.cleanertooltips.util.AttributeHelper;
import net.twentyytwo.cleanertooltips.util.Comparison;

/**
 * A record holding data to help with rendering.
 *
 * @param text          a component of the numerical value of the attribute
 * @param textWidth     the {@code int} width of the text
 * @param icon          the resource location of the attribute
 * @param comparison    a comparison of this attribute to another
 */
public record AttributeFormattingData(MutableComponent text,
                                      int textWidth,
                                      ResourceLocation icon,
                                      Comparison comparison) {

    public AttributeFormattingData(CombinedAttributeModifiers.Entry entry, ResourceLocation icon,
                                   Comparison comparison) {
        this(CleanerTooltips.formatting(entry.modifier().amount(),
                                        AttributeHelper.getBaseValue(entry.attribute()),
                                        entry.displayType()), icon, comparison);
    }

    public AttributeFormattingData(MutableComponent text, ResourceLocation icon,
                                   Comparison comparison) {
        this(text, Minecraft.getInstance().font.width(text), icon, comparison);
    }

    public ChatFormatting getFormatting() {
        return switch (this.comparison()) {
            case HIGHER -> ChatFormatting.GREEN;
            case LOWER -> isAlreadyRed() ? ChatFormatting.DARK_RED : ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };
    }

    private boolean isAlreadyRed() {
        return this.text.getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.RED);
    }
}