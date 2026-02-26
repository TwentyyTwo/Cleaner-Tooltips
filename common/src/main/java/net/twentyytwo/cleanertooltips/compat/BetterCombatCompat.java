package net.twentyytwo.cleanertooltips.compat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.TooltipEntry;
import net.twentyytwo.cleanertooltips.services.Services;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;

import java.util.List;
import java.util.Objects;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.*;

public class BetterCombatCompat {

    private static final boolean isModLoaded = Services.getInstance().isModLoaded("bettercombat");

    public static boolean shouldAdd(ItemStack stack) {
        return isModLoaded && (WeaponRegistry.getAttributes(stack) != null);
    }

    public static TooltipEntry attackRangeEntry(ItemStack stack, ItemAttributeModifiers modifiers) {
        Holder<Attribute> interactionRange = Attributes.ENTITY_INTERACTION_RANGE;
        AttributeMap playerAttributes = Objects.requireNonNull(MC.player).getAttributes();
        ResourceLocation INTERACTION_RANGE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attribute/entity_interaction_range.png");

        double baseValue = (playerAttributes.hasAttribute(interactionRange) ? playerAttributes.getBaseValue(interactionRange) : 0);
        double value = calculateDuplicates(modifiers.modifiers(), baseValue, getRangeBonus(stack));

        MutableComponent text = formatting(value, baseValue, AttributeDisplayType.NUMBER);
        return new TooltipEntry(text, MC.font.width(text), INTERACTION_RANGE);
    }

    private static double getRangeBonus(ItemStack stack) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        if (attributes != null) {
            return attributes.rangeBonus();
        }
        else return 0;
    }

    private static double calculateDuplicates(List<ItemAttributeModifiers.Entry> entries, double baseValue, double value) {
        double totalAddValue = value + baseValue;
        double totalMultiBase = 1;
        double totalMultiplier = 1;

        for (ItemAttributeModifiers.Entry entry : entries) {
            if (!entry.attribute().equals(Attributes.ENTITY_INTERACTION_RANGE)) {
                continue;
            }

            double modifierValue = entry.modifier().amount();

            switch (entry.modifier().operation()) {
                case ADD_VALUE -> totalAddValue += modifierValue;
                case ADD_MULTIPLIED_BASE -> totalMultiBase *= (1 + modifierValue);
                case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= (1 + modifierValue);
            }
        }

        return ((totalAddValue * totalMultiBase) * totalMultiplier) - baseValue;
    }
}
