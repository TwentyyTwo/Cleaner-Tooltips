package net.twentyytwo.cleanertooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.twentyytwo.cleanertooltips.util.AttributeManager;
import net.twentyytwo.cleanertooltips.util.Comparison;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

/**
 * A record holding data to help with rendering.
 *
 * @param text          a component of the numerical value of the attribute
 * @param textWidth     the {@code int} width of the text
 * @param icon          the resource location of the attribute
 * @param comparison    a comparison of this attribute to another
 */
public record AttributeFormattingData(
        MutableComponent text,
        int textWidth,
        Identifier icon,
        Comparison comparison
) {

    public AttributeFormattingData(MutableComponent text, Identifier icon,
                                   Comparison comparison) {
        this(text, MC.font.width(text), icon, comparison);
    }

    public AttributeFormattingData(MutableComponent text, Holder<Attribute> attribute,
                                   Comparison comparison) {
        this(text, MC.font.width(text), AttributeManager.getTexture(attribute), comparison);
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