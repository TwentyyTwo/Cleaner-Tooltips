package net.twentyytwo.cleanertooltips.config;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.twentyytwo.cleanertooltips.compat.BetterCombatHandler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class TooltipsConfig {
    public static final Set<Holder<Attribute>> BLACKLISTED_ATTRIBUTES = new HashSet<>();
    public static final Set<RegexLocation> BLACKLISTED_MODIFIERS = new HashSet<>();

    public static final NavigableMap<Integer, Integer> SORTED_STOPS = new TreeMap<>();

    // -------------------- General --------------------- //
    public boolean iconsEnabled = true;
    public boolean comparisonEnabled = true;
    public boolean comparisonArrow = true;
    public boolean onlyCompareMutual = false;
    public boolean hintEnabled = true;

    public boolean customOrder = false;
    public GroupDisplay groupDisplay = GroupDisplay.ROWS;

    public List<String> attributeIdBlacklist = BetterCombatHandler.isModLoaded
            ? List.of("minecraft:player.entity_interaction_range") : List.of();
    public List<String> modifierIdBlacklist = List.of("minecraft:enchantment.efficiency/mainhand",
            "apotheosis:overworld/royalty_modifier_apothic_attributes.head_#");

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

    public record RegexLocation(String namespace, String regexPath) {

        public static RegexLocation parse(String location) {
            return bySeparator(location, ':');
        }

        public static RegexLocation bySeparator(String location, char separator) {
            int index = location.indexOf(separator);

            if (index >= 0) {
                String path = location.substring(index + 1);
                if (index != 0) return new RegexLocation(location.substring(0, index), getRegexFromPath(path));
                else return new RegexLocation("minecraft", getRegexFromPath(path));
            } else return new RegexLocation("minecraft", getRegexFromPath(location));
        }

        private static String getRegexFromPath(String path) {
            if (!path.contains("#")) return Pattern.quote(path);

            String[] parts = path.split("#", -1);
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) regex.append("\\d+");
                if (!parts[i].isBlank()) {
                    regex.append(Pattern.quote(parts[i]));
                }
            }

            return regex.toString();
        }

        public boolean matches(ResourceLocation location) {
            return location.getNamespace().equals(this.namespace) && location.getPath().matches(this.regexPath);
        }
    }
}
