package net.twentyytwo.cleanertooltips.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum AttributeDisplayType {
    /**
     * Displays as either Enabled or Disabled.<br>
     * Example:<br>
     * An item that enables the neoforge attribute {@code creative_flight} shows "Enabled".
     **/
    BOOLEAN,
    /**
     * Displays as a difference from the players default value.<br>
     * Examples:<br>
     * An item increasing your {@code max_health} by 4 shows "+4".
     * An item decreasing your {@code entity_interaction_range} by 1 shows "-1".
     */
    DIFFERENCE,
    /**
     * Displays as a flat number.<br>
     * Example:<br>
     * An item increasing your {@code attack_damage} by 6 shows "6".
     */
    NUMBER,
    /**
     * Displays as a multiplier from the players default value.<br>
     * Currently has no use cases.
     */
    MULTIPLIER,
    /**
     * Displays as a percentage value.<br>
     * Examples:<br>
     * An item increasing {@code knockback_resistance} by 0.1 shows "+10%"
     * An item decreasing {@code movement_speed} by 0.2 shows "-20%"
     */
    PERCENTAGE;

    public boolean hasBaseValue() {
        return this == NUMBER || this == MULTIPLIER;
    }

    /**
     * A map of attributes where each attribute is associated with a corresponding {@code AttributeDisplayType}.
     */
    public static final Map<ResourceLocation, AttributeDisplayType[]> MAP = new HashMap<>();
    static {
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor"), new AttributeDisplayType[]{NUMBER, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor_toughness"), new AttributeDisplayType[]{NUMBER, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_damage"), new AttributeDisplayType[]{NUMBER, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_knockback"), new AttributeDisplayType[]{DIFFERENCE, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_speed"), new AttributeDisplayType[]{NUMBER, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.block_interaction_range"), new AttributeDisplayType[]{DIFFERENCE, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.entity_interaction_range"), new AttributeDisplayType[]{DIFFERENCE, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.gravity"), new AttributeDisplayType[]{PERCENTAGE, PERCENTAGE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.knockback_resistance"), new AttributeDisplayType[]{PERCENTAGE, PERCENTAGE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.luck"), new AttributeDisplayType[]{PERCENTAGE, PERCENTAGE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"), new AttributeDisplayType[]{DIFFERENCE, DIFFERENCE});
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.movement_speed"), new AttributeDisplayType[]{PERCENTAGE, PERCENTAGE});
    }

    public static AttributeDisplayType get(ItemAttributeModifiers.Entry entry) {
        if (!entry.modifier().operation().equals(AttributeModifier.Operation.ADD_VALUE)) {
            return PERCENTAGE;
        }

        ResourceLocation key = BuiltInRegistries.ATTRIBUTE.getKey(entry.attribute().value());
        if (MAP.containsKey(key)) {
            return Set.of(1, 4, 5, 6, 7).contains(entry.slot().ordinal())
                    ? MAP.get(key)[0]
                    : MAP.get(key)[1];
        }
        return NUMBER;
    }
}
