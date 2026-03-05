package net.twentyytwo.cleanertooltips.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashMap;
import java.util.Map;

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
    public static final Map<ResourceLocation, AttributeDisplayType> MAP = new HashMap<>();
    static {
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor"), NUMBER);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.armor_toughness"), NUMBER);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_damage"), NUMBER);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_knockback"), NUMBER);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.attack_speed"), NUMBER);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.block_interaction_range"), DIFFERENCE);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "player.entity_interaction_range"), DIFFERENCE);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.gravity"), PERCENTAGE);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.knockback_resistance"), PERCENTAGE);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.luck"), PERCENTAGE);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.max_health"), DIFFERENCE);
        MAP.put(ResourceLocation.fromNamespaceAndPath("minecraft", "generic.movement_speed"), PERCENTAGE);
    }

    public static AttributeDisplayType get(Holder<Attribute> attribute) {
        return MAP.getOrDefault(BuiltInRegistries.ATTRIBUTE.getKey(attribute.value()), NUMBER);
    }
}
