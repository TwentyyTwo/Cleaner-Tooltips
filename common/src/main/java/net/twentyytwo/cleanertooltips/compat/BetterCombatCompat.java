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
import net.twentyytwo.cleanertooltips.AttributeFormattingData;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.services.Services;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;

import java.util.List;
import java.util.Objects;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.formatting;

public class BetterCombatCompat {

    public static final boolean isModLoaded = Services.getInstance().isModLoaded("bettercombat");

    public static boolean hasAttributes(ItemStack stack) {
        return (WeaponRegistry.getAttributes(stack) != null);
    }

    public static AttributeFormattingData attackRangeEntry(ItemStack stack, ItemAttributeModifiers modifiers) {
        Holder<Attribute> interactionRange = Attributes.ENTITY_INTERACTION_RANGE;
        AttributeMap playerAttributes = Objects.requireNonNull(MC.player).getAttributes();
        ResourceLocation INTERACTION_RANGE = CleanerTooltips.location("textures/gui/attribute/entity_interaction_range.png");

        double baseValue = (playerAttributes.hasAttribute(interactionRange) ? playerAttributes.getBaseValue(interactionRange) : 0);
        double value = calculateDuplicates(modifiers.modifiers(), baseValue, getRangeBonus(stack));

        Comparison comparison = getComparison(stack, baseValue, value);

        MutableComponent text = formatting(value, baseValue, AttributeDisplayType.NUMBER);
        return new AttributeFormattingData(text, INTERACTION_RANGE, comparison);
    }

    private static double getRangeBonus(ItemStack stack) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        return attributes != null ? attributes.rangeBonus() : 0;
    }

    private static double calculateDuplicates(List<ItemAttributeModifiers.Entry> entries, double baseValue, double value) {
        double totalAddValue = value + baseValue;
        double totalBaseMultiplier = 1;
        double totalMultiplier = 1;

        for (ItemAttributeModifiers.Entry entry : entries) {
            if (!entry.attribute().equals(Attributes.ENTITY_INTERACTION_RANGE)) {
                continue;
            }

            double amount = entry.modifier().amount();

            switch (entry.modifier().operation()) {
                case ADD_VALUE -> totalAddValue += amount;
                case ADD_MULTIPLIED_BASE -> totalBaseMultiplier += amount;
                case ADD_MULTIPLIED_TOTAL -> totalMultiplier *= (1 + amount);
            }
        }

        return ((totalAddValue * totalBaseMultiplier) * totalMultiplier) - baseValue;
    }

    private static Comparison getComparison(ItemStack stack, double baseValue, double value) {
        if (config.general.compareAttributes) {
            ItemStack comparedStack = MC.player.getItemBySlot(MC.player.getEquipmentSlotForItem(stack));

            if (!comparedStack.isEmpty() && !comparedStack.equals(stack)) {
                ItemAttributeModifiers comparedModifiers = CleanerTooltipsUtil.getAttributeModifiers(comparedStack);

                if (!comparedModifiers.modifiers().isEmpty()) {
                    double comparedValue = calculateDuplicates(comparedModifiers.modifiers(), baseValue, getRangeBonus(comparedStack));
                    return Comparison.getComparison(value, comparedValue);
                }
            }
        }
        return Comparison.NONE;
    }
}
