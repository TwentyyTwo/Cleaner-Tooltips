package net.twentyytwo.cleanertooltips.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.compat.LegendaryTooltipsCompat;
import net.twentyytwo.cleanertooltips.services.Services;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.List;

import static net.twentyytwo.cleanertooltips.CleanerTooltips.MC;
import static net.twentyytwo.cleanertooltips.CleanerTooltips.config;

/**
 * Collection of useful functions.
 */
public class CleanerTooltipsUtil {
    private static int tick = 0;
    private static boolean tickToggle = false;

    public static void onTick() {
        tick++;
        if (tick >= 20) {
            tickToggle = !tickToggle;
            tick = 0;
        }
    }

    public static boolean getTickToggle() {
        return tickToggle;
    }

    /**
     * Gets the index of the item name and returns the index below it.
     * @param stack             the item stack whose name is used
     * @param tooltipElements   the elements of the tooltip
     * @return                  the index at which the icon attributes should be added
     */
    public static int getIndexNeoforge(ItemStack stack, List<Either<FormattedText, TooltipComponent>> tooltipElements) {
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
     * Calculates the index of the first ClientTextTooltip and returns the index below it.
     * @param components the list of {@code ClientTooltipComponent}s
     * @return the index at which the icon attributes should be added
     */
    public static int getIndexFabric(List<ClientTooltipComponent> components) {
        int indexToReturn = 1;
        for (int i = 0; i < components.size(); i++) {
            var clientTooltipComponent = components.get(i);
            if (clientTooltipComponent instanceof ClientTextTooltip) {
                // Because this is called after LegendaryTooltips add their components, we'll have to check for them.
                indexToReturn = LegendaryTooltipsCompat.isModLoaded && LegendaryTooltipsCompat.hasTitleBreak(components)
                        ? i + 2
                        : i + 1;
                break;
            }
        }
        return Math.min(indexToReturn, components.size());
    }

    /**
     * Calculates the additional attack damage from the sharpness enchantment.
     * @param stack the item stack
     * @return      the additional attack damage
     */
    public static double getSharpnessBonus(ItemStack stack) {
        double sharpnessBonus = 0;
        if (config.general.sharpness) {
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

    public static double getBaseValue(Holder<Attribute> attribute) {
        return MC.player != null ? MC.player.getAttributeBaseValue(attribute) : 0;
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
        if (defaultModifiers.showInTooltip()) for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
            stack.forEachModifier(slot, (attribute, modifier) -> builder.add(attribute, modifier, slot));
        }
        return builder.build();
    }

    public static boolean shouldAddAttributes() {
        return MC.player != null && config.general.enabled && !Services.getInstance().isKeyDown();
    }

    public static boolean hasAttributes(ItemStack stack) {
        MutableBoolean hasAttributes = new MutableBoolean(false);
        if (!stack.isEmpty()) {
            for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
                stack.forEachModifier(slot, (attribute, modifier) -> {
                    if ((AttributeManager.getDisplayType(attribute, modifier, slot, shouldSeparateOperations(slot)).hasBaseValue()
                            && modifier.amount() + getBaseValue(attribute) != 0 || modifier.amount() != 0) && AttributeManager.getTexture(attribute) != null) {
                        hasAttributes.setTrue();
                    }
                });
            }
        }
        return hasAttributes.booleanValue();
    }

    public static boolean shouldSeparateOperations(EquipmentSlotGroup slotGroup) {
        return switch (slotGroup) {
            case MAINHAND, OFFHAND, BODY -> false;
            case null, default -> true;
        };
    }
}
