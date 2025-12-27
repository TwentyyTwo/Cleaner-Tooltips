package net.twentyytwo.cleanertooltips.util;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
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
    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(ItemStack stack) {
        Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            multimap.putAll(stack.getAttributeModifiers(slot));
        }
        return multimap;
    }

    /**
     * Returns whether the tooltip should be added.<br>
     * Takes in the {@code ItemAttributeModifiers} rather than an {@code ItemStack} to
     * minimize the total amount of calculations.
     *
     * @param modifiers the {@code ItemAttributeModifiers} of the {@code ItemStack}*/
    public static boolean shouldAddTooltip(Multimap<Attribute, AttributeModifier> modifiers) {
        Minecraft mc = CleanerTooltips.MC;
        return !InputConstants.isKeyDown(mc.getWindow().getWindow(), ((KeyMappingAccessor) CleanerTooltips.hideTooltip).getKey().getValue()) &&
                !modifiers.isEmpty() && mc.player != null && CleanerTooltips.config.enabled;
    }

    /**
     * A list of most attributes and their associated {@code AttributeDisplayType}.
     */
    public static final Map<ResourceLocation, AttributeDisplayType> ATTRIBUTE_DISPLAY_MAP = new HashMap<>();
    static {
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.armor"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.armor_toughness"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.attack_damage"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.attack_knockback"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.attack_speed"), AttributeDisplayType.NUMBER);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "player.block_break_speed"), AttributeDisplayType.MULTIPLIER);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "player.block_interaction_range"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "player.entity_interaction_range"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.gravity"), AttributeDisplayType.MULTIPLIER);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.knockback_resistance"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.luck"), AttributeDisplayType.PERCENTAGE);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.max_health"), AttributeDisplayType.DIFFERENCE);
        ATTRIBUTE_DISPLAY_MAP.put(new ResourceLocation("minecraft", "generic.movement_speed"), AttributeDisplayType.MULTIPLIER);
    }
}
