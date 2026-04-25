package net.twentyytwo.cleanertooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.twentyytwo.cleanertooltips.util.Comparison;
import org.jetbrains.annotations.Nullable;

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
        this(text, MC.font.width(text), getIcon(attribute), comparison);
    }

    @Nullable
    private static ResourceLocation getIcon(Holder<Attribute> attribute) {
        ResourceLocation attributeKey = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());

        if (attributeKey == null) return null;
        String texturePath = "textures/gui/attribute/" + attributeKey.getPath().replaceFirst("(generic|player)\\.", "") + ".png";
        ResourceLocation resourceLocation =  ResourceLocation.fromNamespaceAndPath(CleanerTooltips.MOD_ID, texturePath);
        return MC.getResourceManager().getResource(resourceLocation).isEmpty() ? null : resourceLocation;
    }

    public ChatFormatting getFormatting() {
        return switch (this.comparison()) {
            case HIGHER -> ChatFormatting.GREEN;
            case LOWER -> this.text().getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.RED) ? ChatFormatting.DARK_RED : ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };
    }
}