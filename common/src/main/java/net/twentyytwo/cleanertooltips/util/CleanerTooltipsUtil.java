package net.twentyytwo.cleanertooltips.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.compat.LegendaryTooltipsHandler;
import net.twentyytwo.cleanertooltips.services.Services;

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
     * @param tooltipElements   the elements of the tooltip
     * @return                  the index at which the icon attributes should be added
     */
    public static int getIndexNeoforge(List<Either<FormattedText, TooltipComponent>> tooltipElements) {
        int indexToReturn = 1;
        for (int i = 0; i < tooltipElements.size(); i++) {
            if (tooltipElements.get(i).left().isPresent()) {
                indexToReturn = LegendaryTooltipsHandler.isModLoaded
                        && LegendaryTooltipsHandler.hasTitleBreakNeoforge(tooltipElements)
                        ? i + 2
                        : i + 1;
                break;
            }
        }
        return Math.min(indexToReturn, tooltipElements.size());
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
                indexToReturn = LegendaryTooltipsHandler.isModLoaded
                        && LegendaryTooltipsHandler.hasTitleBreakFabric(components)
                        ? i + 2
                        : i + 1;
                break;
            }
        }
        return Math.min(indexToReturn, components.size());
    }

    public static ItemStack getEquippedStack(ItemStack stack) {
        assert MC.player != null;
        return MC.player.getItemBySlot(MC.player.getEquipmentSlotForItem(stack));
    }

    /**
     * Calculates the additional attack damage from the sharpness enchantment.
     * @param stack the item stack
     * @return      the additional attack damage
     */
    public static double getSharpnessBonus(ItemStack stack) {
        double sharpnessBonus = 0;
        if (config.general.sharpness) {
            var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var enchantment : enchantments.entrySet()) {
                var key = enchantment.getKey().unwrapKey();
                if (key.isPresent() && key.get() == Enchantments.SHARPNESS) {
                    int level = enchantment.getIntValue();
                    if (level > 0) {
                        sharpnessBonus = ((0.5 * level) + 0.5);
                    }
                    break;
                }
            }
        }
        return sharpnessBonus;
    }

    public static double getBaseValue(Holder<Attribute> attribute) {
        return MC.player != null ? MC.player.getAttributeBaseValue(attribute) : 0;
    }

    public static boolean shouldAddAttributes() {
        return MC.player != null && config.general.enabled && !Services.getInstance().isKeyDown();
    }

    public static boolean hasAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        boolean[] found = new boolean[]{false};
        for (EquipmentSlotGroup slot : EquipmentSlotGroup.values()) {
            if (found[0]) break;
            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (found[0]) return;
                if (AttributeManager.getTexture(attribute) == null) return;
                if (modifier.amount() != 0) {
                    found[0] = true;
                } else if (AttributeManager.getDisplayType(attribute).hasBaseValue()
                        && modifier.amount() + getBaseValue(attribute) != 0) {
                    found[0] = true;
                }
            });
        }
        return found[0];
    }

    public static boolean separateOperations(EquipmentSlotGroup slotGroup) {
        return slotGroup != EquipmentSlotGroup.MAINHAND
                && slotGroup != EquipmentSlotGroup.OFFHAND
                && slotGroup != EquipmentSlotGroup.BODY;
    }
}
