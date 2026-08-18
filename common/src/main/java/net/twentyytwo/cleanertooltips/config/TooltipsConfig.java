package net.twentyytwo.cleanertooltips.config;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class TooltipsConfig {
    public static final Set<Holder<Attribute>> BLACKLISTED_HINTS = new HashSet<>();
    public static final NavigableMap<Integer, Integer> SORTED_STOPS = new TreeMap<>();

    // -------------------- General --------------------- //
    public boolean iconsEnabled = true;
    public boolean comparisonEnabled = true;
    public boolean comparisonArrow = true;
    public boolean onlyCompareMutual = false;
    public boolean hintEnabled = true;
    public List<String> hintBlacklist = List.of("minecraft:player.mining_efficiency");

    public boolean customOrder = false;
    public GroupDisplay groupDisplay = GroupDisplay.ROWS;

    public int attributeGap = 8;
    public int innerGap = 3;

    public boolean sharpnessFix = true;
    public boolean miningSpeed = true;

    // ------------------- Durability ------------------- //
    public boolean durabilityEnabled = false;
    public boolean durabilityMaximum = true;
    public boolean hideWhenRepaired = false;
    public Position durabilityPos = Position.INLINE;
    public Style durabilityStyle = Style.DEFAULT;

    public boolean durabilityColor = true;
    public ColorMode colorMode = ColorMode.DEFAULT;

    public Map<Integer, Integer> colorsStops = new LinkedHashMap<>(
            ImmutableMap.of(100, 0x55ff55, 50, 0xffaa00, 15, 0xff5555));

    public enum Style {
        DEFAULT, PERCENTAGE
    }

    public enum ColorMode {
        DEFAULT, LINEAR, NATIVE
    }

    public enum GroupDisplay {
        ROWS, INLINE
    }

    public enum Position {
        INLINE, BELOW, BOTTOM
    }
}
