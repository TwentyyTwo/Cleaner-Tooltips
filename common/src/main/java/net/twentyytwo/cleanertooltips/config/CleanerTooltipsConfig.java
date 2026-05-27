package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

@Config(name = CleanerTooltips.MOD_ID)
public class CleanerTooltipsConfig implements ConfigData {

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
