package net.twentyytwo.cleanertooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.twentyytwo.cleanertooltips.util.AttributeManager;
import net.twentyytwo.cleanertooltips.util.Comparison;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

/**
 * This object contains all necessary data to render a value-icon pair. The {@code textWidth} is automatically
 * calculated in the constructor based on the provided {@code text} mutableComponent. The {@code icon} can be
 * either manually provided, or dynamically gathered based on the attribute.
 */
public record AttributeFormattingData(MutableComponent text, int textWidth, ResourceLocation icon, Comparison comparison) {

    public AttributeFormattingData(MutableComponent text, ResourceLocation icon, Comparison comparison) {
        this(text, MC.font.width(text), icon, comparison);
    }

    public AttributeFormattingData(MutableComponent text, Holder<Attribute> attribute, Comparison comparison) {
        this(text, MC.font.width(text), AttributeManager.getTexture(attribute), comparison);
    }

    public ChatFormatting getFormatting() {
        return switch (this.comparison()) {
            case HIGHER -> ChatFormatting.GREEN;
            case LOWER -> this.text().getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.RED) ? ChatFormatting.DARK_RED : ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };
    }
}