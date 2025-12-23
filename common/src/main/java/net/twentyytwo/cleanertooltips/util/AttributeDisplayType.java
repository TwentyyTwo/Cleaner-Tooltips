package net.twentyytwo.cleanertooltips.util;

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
     * Example:<br>
     * An item increasing the {@code gravity} by twice the default value shows "2x".
     */
    MULTIPLIER,
    /**
     * Displays as a percentage value.<br>
     * Examples:<br>
     * An item increasing {@code knockback_resistance} by 0.1 shows "+10%"
     * An item decreasing {@code movement_speed} by 0.2 shows "-20%"
     */
    PERCENTAGE
}
