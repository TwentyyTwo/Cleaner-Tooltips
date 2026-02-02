package net.twentyytwo.cleanertooltips.util;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.mixin.KeyMappingAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CleanerTooltipsUtil {
    /**
     * Returns the index at which the tooltip should be added.
     *
     * @param stack The item whose name is used for comparison
     * @param tooltipElements The elements which are iterated over to find the name*/
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
     * Returns the index at which the empty component should be replaced.
     * Has to be called after the empty component has been added to work properly.
     *
     * @param list the list of {@code ClientTooltipComponent} */
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
     * Returns an {@code ItemAttributeModifiers} component of the given
     * {@code ItemStack}, which works for all {@code EquipmentSlotGroup}
     *
     * @param stack the item which modifiers are returned*/
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
     * Returns whether the tooltip should be added.<br>
     * Takes in the {@code ItemAttributeModifiers} rather than an {@code ItemStack} to
     * minimize the total amount of calculations.
     *
     * @param modifiers the {@code ItemAttributeModifiers} of the {@code ItemStack}*/
    public static boolean shouldAddTooltip(ItemAttributeModifiers modifiers) {
        Minecraft mc = CleanerTooltips.MC;

        if (mc.player == null)
            return false;
        else if (!CleanerTooltips.config.enabled)
            return false;
        else if (InputConstants.isKeyDown(mc.getWindow().getWindow(), ((KeyMappingAccessor) CleanerTooltips.hideTooltip).getKey().getValue()))
            return false;
        else if (modifiers.modifiers().isEmpty())
            return false;

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) { // Seems inefficient, might fix later
            double baseValue = mc.player != null && mc.player.getAttributes().hasAttribute(entry.attribute()) ? mc.player.getAttributeBaseValue(entry.attribute()) : 0;
            switch (ATTRIBUTE_DISPLAY_MAP.get(BuiltInRegistries.ATTRIBUTE.getKey(entry.attribute().value()))) {
                case DIFFERENCE -> {
                    if (entry.modifier().amount() != 0) return true;
                }
                case null, default -> {
                    if (entry.modifier().amount() + baseValue != 0) return true;
                }
            }
        }
        return false;
    }

    /**
     * A list of most attributes and their associated {@code AttributeDisplayType}.
     */
    public static final Map<ResourceLocation, AttributeDisplayType> ATTRIBUTE_DISPLAY_MAP = new HashMap<>();
    static {
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor_toughness"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_damage"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_knockback"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_speed"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.block_break_speed"), AttributeDisplayType.MULTIPLIER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.block_interaction_range"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.entity_interaction_range"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.gravity"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.knockback_resistance"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.luck"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.movement_speed"), AttributeDisplayType.PERCENTAGE);
    }
}
