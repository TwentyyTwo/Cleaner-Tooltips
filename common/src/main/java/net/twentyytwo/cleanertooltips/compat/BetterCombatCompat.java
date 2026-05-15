package net.twentyytwo.cleanertooltips.compat;

import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.twentyytwo.cleanertooltips.AttributeFormattingData;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.services.Services;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.formatting;

public class BetterCombatCompat {

    public static final boolean isModLoaded = Services.getInstance().isModLoaded("bettercombat");

    public static boolean hasAttributes(ItemStack stack) {
        return (WeaponRegistry.getAttributes(stack) != null);
    }

    public static AttributeFormattingData attackRangeEntry(ItemStack stack) {
        ResourceLocation INTERACTION_RANGE = CleanerTooltips.location("textures/gui/attribute/entity_interaction_range.png");

        double baseValue = CleanerTooltipsUtil.getBaseValue(Attributes.ENTITY_INTERACTION_RANGE);
        double value = getTotalRange(stack, baseValue);

        Comparison comparison = getComparison(stack, baseValue, value);

        MutableComponent text = formatting(value, baseValue, AttributeDisplayType.NUMBER);
        return new AttributeFormattingData(text, INTERACTION_RANGE, comparison);
    }

    private static double getTotalRange(ItemStack stack, double baseValue) {
        WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);

        final double[] totalAddValue = {baseValue};
        final double[] totalBaseMultiplier = {1};
        final double[] totalMultiplier = {1};


        stack.forEachModifier(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ENTITY_INTERACTION_RANGE)) {
                switch (modifier.operation()) {
                    case ADD_VALUE -> totalAddValue[0] += modifier.amount();
                    case ADD_MULTIPLIED_BASE -> totalBaseMultiplier[0] += modifier.amount();
                    case ADD_MULTIPLIED_TOTAL -> totalMultiplier[0] *= (1 + modifier.amount());
                }
            }
        });

        return ((totalAddValue[0] * totalBaseMultiplier[0]) * totalMultiplier[0]) - baseValue + attributes.rangeBonus();
    }

    private static Comparison getComparison(ItemStack stack, double baseValue, double value) {
        if (config.general.compareAttributes) {
            ItemStack comparedStack = MC.player.getItemBySlot(MC.player.getEquipmentSlotForItem(stack));

            if (!comparedStack.isEmpty() && !comparedStack.equals(stack) && CleanerTooltipsUtil.hasAttributes(comparedStack)) {
                double comparedValue = getTotalRange(comparedStack, baseValue);
                return Comparison.getComparison(value, comparedValue);
            }
        }
        return Comparison.NONE;
    }
}
