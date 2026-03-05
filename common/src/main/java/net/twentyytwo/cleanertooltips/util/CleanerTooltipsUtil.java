package net.twentyytwo.cleanertooltips.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.services.Services;

import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;

/**
 * Collection of useful functions.
 */
public class CleanerTooltipsUtil {

    /**
     * Gets the index of the item name and returns the index below it.
     * @param stack             the item stack whose name is used
     * @param tooltipElements   the elements of the tooltip
     * @return                  the index at which the icon attributes should be added
     */
    public static int getInsertIndex(ItemStack stack, List<Either<FormattedText, TooltipComponent>> tooltipElements) {
        Component itemName = stack.getHoverName();
        int nameIndex = 0;
        for (int i = 0; i < tooltipElements.size(); i++) {
            var component = tooltipElements.get(i);
            if (component.left().isPresent() && component.left().get().getString().equals(itemName.getString())) {
                nameIndex = i;
                break;
            }
        }
        return nameIndex + 1;
    }

    /**
     * Gets the index of the empty component which the icon attributes should replace.<br>
     * Only for fabric, and should only be called after the empty component has been added.
     * @param list  the {@code ClientTooltipComponent}s of the tooltip
     * @return      the index of the second empty component
     */
    public static int getReplaceIndex(List<ClientTooltipComponent> list) {
        int insertIndex = 1;
        int counter = 2;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof ClientTextTooltip) {
                counter--;
                if (counter <= 0) {
                    insertIndex = i;
                    break;
                }
            }
        }
        return insertIndex;
    }

    /**
     * Calculates the additional attack damage from the sharpness enchantment.
     * @param stack the item stack
     * @return      the additional attack damage
     */
    public static double getSharpnessBonus(ItemStack stack) {
        double sharpnessBonus = 0;
        if (CleanerTooltips.config.sharpness) {
            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var enchantment : enchantments.entrySet()) {
                var enchantmentKey = enchantment.getKey().unwrapKey();
                if (enchantmentKey.isPresent() && enchantmentKey.get().equals(Enchantments.SHARPNESS) && enchantment.getIntValue() > 0) {
                    sharpnessBonus = ((0.5 * enchantment.getIntValue()) + 0.5);
                    break;
                }
            }
        }
        return sharpnessBonus;
    }

    /**
     * Calculates the attribute modifiers of all equipment slots.<br>
     * Works for every type of equipment.
     * @param stack the item stack that is used for the attribute modifiers
     * @return      {@code ItemAttributeModifiers} of the item stack
     */
    public static ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        ItemAttributeModifiers defaultModifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        if (defaultModifiers.showInTooltip()) {
            for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
                stack.forEachModifier(slot, (attribute, modifier) -> builder.add(attribute, modifier, slot));
            }
        }
        return new ItemAttributeModifiers(builder.build().modifiers(), defaultModifiers.showInTooltip());
    }

    /**
     * Boolean to check whether the icon attributes should be added.
     * @param modifiers the {@code ItemAttributeModifiers} of the item stack
     * @return          whether the icon attributes should be added.
     */
    public static boolean shouldAddTooltip(ItemAttributeModifiers modifiers) {
        if (MC.player == null) {
            return false;
        } else if (!CleanerTooltips.config.enabled) {
            return false;
        } else if (Services.getInstance().isKeyDown()) {
            return false;
        } else if (modifiers.modifiers().isEmpty()) {
            return false;
        }

        AttributeMap playerAttributes = MC.player.getAttributes();

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (AttributeDisplayType.get(entry.attribute()).hasBaseValue()) {
                double baseValue = MC.player != null && playerAttributes.hasAttribute(entry.attribute())
                        ? playerAttributes.getBaseValue(entry.attribute())
                        : 0;

                if (entry.modifier().amount() + baseValue != 0) {
                    return true;
                }
            } else {
                if (entry.modifier().amount() != 0) {
                    return true;
                }
            }
        }

        return false;
    }
}
