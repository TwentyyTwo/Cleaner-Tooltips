package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Config(name = CleanerTooltips.MOD_ID)
public class CleanerTooltipsConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static ConfigHolder<CleanerTooltipsConfig> configHolder;

    @ConfigEntry.Gui.Excluded
    public static Set<Holder<Attribute>> blacklistedHints = new HashSet<>();

    public void onConfigSave() {
        blacklistedHints.clear();

        int i = 0;
        for (String s : this.advanced.hintBlacklist) {
            var holder = CleanerTooltipsUtil.resolveAttribute(s);
            if (holder.isPresent()) {
                blacklistedHints.add(holder.get());
                String resolved = holder.get().key().location().toString();
                if (!s.equals(resolved)) {
                    this.advanced.hintBlacklist.set(i, resolved);
                }
            }
            i++;
        }
    }

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public General general = new General();

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public Durability durability = new Durability();

    @ConfigEntry.Gui.CollapsibleObject
    public Advanced advanced = new Advanced();

    public static class General {
        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        @ConfigEntry.Gui.Tooltip
        public boolean compareAttributes = true;

        @ConfigEntry.Gui.Tooltip
        public boolean comparisonArrow = true;

        @ConfigEntry.Gui.Tooltip
        public boolean sharpness = true;

        @ConfigEntry.Gui.Tooltip
        public boolean hiddenAttributesHint = true;

        @ConfigEntry.Gui.Tooltip
        public boolean miningSpeed = false;
    }

    public static class Durability {
        @ConfigEntry.Gui.Tooltip
        public boolean durabilityEnabled = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public PosValues durabilityPos = PosValues.INLINE;

        @ConfigEntry.Gui.Tooltip
        public boolean maximumDurability = true;

        @ConfigEntry.Gui.Tooltip
        public boolean durabilityColor = true;
    }

    public static class Advanced {
        @ConfigEntry.Gui.Tooltip
        public boolean onlyCompareShared = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public GroupDisplay groupDisplay = GroupDisplay.ROWS;

        @ConfigEntry.Gui.Tooltip
        public List<String> hintBlacklist = List.of("minecraft:mining_efficiency");
    }

    public enum GroupDisplay {
        ROWS,
        INLINE,
        PRIMARY
    }

    public enum PosValues {
        INLINE,
        BOTTOM,
        BELOW
    }
}
