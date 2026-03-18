package net.twentyytwo.cleanertooltips;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.twentyytwo.cleanertooltips.util.Comparison;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

/**
 * This object contains all necessary data to render a value-icon pair. The {@code textWidth} is automatically
 * calculated in the constructor based on the provided {@code text} mutableComponent. The {@code icon} can be
 * either manually provided, or dynamically gathered based on the attribute.<p>
 *
 * The boolean {@code isLastEntry} represents if the current value-icon pair is the last entry in their
 * respective list.
 */
public record AttributeFormattingData(MutableComponent text, int textWidth, ResourceLocation icon, boolean isLastEntry) {

    public AttributeFormattingData(MutableComponent text, ResourceLocation icon) {
        this(text, MC.font.width(text), icon, false);
    }

    public AttributeFormattingData(MutableComponent text, Holder<Attribute> attribute, boolean isLastEntry) {
        this(text, MC.font.width(text), getIcon(attribute), isLastEntry);
    }

    @Nullable
    private static ResourceLocation getIcon(Holder<Attribute> attribute) {
        ResourceLocation attributeKey = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());

        if (attributeKey == null) return null;
        String texturePath = "textures/gui/attribute/" + attributeKey.getPath().replaceFirst("(generic|player)\\.", "") + ".png";
        ResourceLocation resourceLocation =  ResourceLocation.fromNamespaceAndPath(CleanerTooltips.MOD_ID, texturePath);
        if (MC.getResourceManager().getResource(resourceLocation).isEmpty())
            return null;
        return resourceLocation;
    }
}