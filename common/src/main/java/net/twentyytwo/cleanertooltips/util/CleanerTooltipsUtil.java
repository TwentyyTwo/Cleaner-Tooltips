package net.twentyytwo.cleanertooltips.util;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Boolean to check whether the icon attributes should be added.<br>
     * Note: the keybind is checked separately because of fabric and neoforge differences.
     * @param modifiers the {@code ItemAttributeModifiers} of the item stack
     * @return          whether the icon attributes should be added.
     */
    public static boolean shouldAddTooltip(ItemAttributeModifiers modifiers) {
        Minecraft mc = CleanerTooltips.MC;

        if (mc.player == null)
            return false;
        else if (!CleanerTooltips.config.enabled)
            return false;
        else if (modifiers.modifiers().isEmpty())
            return false;

        AttributeMap playerAttributes = mc.player.getAttributes();
        Registry<Attribute> attributeRegistry = BuiltInRegistries.ATTRIBUTE;

        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            double baseValue = mc.player != null && playerAttributes.hasAttribute(entry.attribute()) ? playerAttributes.getBaseValue(entry.attribute()) : 0;
            switch (ATTRIBUTE_DISPLAY_MAP.get(attributeRegistry.getKey(entry.attribute().value()))) {
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
     * A map of attributes where each attribute is associated with a {@code AttributeDisplayType}.
     */
    public static final Map<ResourceLocation, AttributeDisplayType> ATTRIBUTE_DISPLAY_MAP = new HashMap<>();
    static {
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor_toughness"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_damage"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_knockback"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_speed"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.block_break_speed"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.block_interaction_range"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.entity_interaction_range"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.gravity"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.knockback_resistance"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.luck"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.movement_speed"), AttributeDisplayType.PERCENTAGE);
    }
}
