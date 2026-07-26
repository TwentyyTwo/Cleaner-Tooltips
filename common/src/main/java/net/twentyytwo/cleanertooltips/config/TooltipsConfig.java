package net.twentyytwo.cleanertooltips.config;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TooltipsConfig {
    public static final Set<Holder<Attribute>> BLACKLISTED_HINTS = new HashSet<>();

    // -------------------- General --------------------- //
    public boolean iconsEnabled = true;
    public boolean comparisonEnabled = true;
    public boolean comparisonArrow = true;
    public boolean onlyCompareMutual = false;
    public boolean hintEnabled = true;
    public List<String> hintBlacklist = List.of("minecraft:player.mining_efficiency");

    public int attributeGap = 8;
    public int innerGap = 3;

    public GroupDisplay groupDisplay = GroupDisplay.ROWS;

    public boolean sharpnessFix = true;
    public boolean miningSpeed = true;

    // ------------------- Durability ------------------- //
    public boolean durabilityEnabled = false;
    public boolean durabilityMaximum = true;
    public boolean hideWhenRepaired = false;
    public boolean durabilityColor = true;

    public Style durabilityStyle = Style.DEFAULT;
    public Position durabilityPos = Position.INLINE;

    public enum Style {
        DEFAULT, PERCENTAGE
    }

    public enum GroupDisplay {
        ROWS, INLINE, PRIMARY
    }

    public enum Position {
        INLINE, BELOW, BOTTOM
    }
}
