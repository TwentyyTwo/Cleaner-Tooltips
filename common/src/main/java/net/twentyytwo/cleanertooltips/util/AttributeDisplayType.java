package net.twentyytwo.cleanertooltips.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

import java.util.function.IntFunction;

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

    public static final IntFunction<AttributeDisplayType> BY_ID = ByIdMap
            .continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
    public static final Codec<AttributeDisplayType> CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(AttributeDisplayType.valueOf(s.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return DataResult.success(NUMBER);
        }
    }, AttributeDisplayType::toString);

    public static final StreamCodec<ByteBuf, AttributeDisplayType> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);
}
