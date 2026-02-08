package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.twentyytwo.cleanertooltips.CleanerTooltips;

@Config(name = CleanerTooltips.MOD_ID)
public class CleanerTooltipsConfig implements ConfigData {
    @ConfigEntry.Gui.PrefixText

    // Whether Cleaner Tooltips should be enabled.
    @ConfigEntry.Gui.Tooltip
    public boolean enabled = true;

    // Whether sharpness should change the damage value.
    @ConfigEntry.Gui.Tooltip
    public boolean sharpness = true;

    @ConfigEntry.Gui.Tooltip
    public boolean hiddenAttributesHint = true;

    @ConfigEntry.Gui.PrefixText

    // Whether durability should be displayed in the tooltip.
    @ConfigEntry.Gui.Tooltip
    public boolean durability = false;

    // Set the position of the durability.
    @ConfigEntry.Gui.Tooltip(count = 2)
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public posValues durabilityPos = posValues.INLINE;

    @ConfigEntry.Gui.Tooltip
    public boolean showMaximumDurability = true;

    @ConfigEntry.Gui.Tooltip
    public boolean durabilityColor = true;

    public enum posValues {
        INLINE,
        BOTTOM,
        BELOW
    }
}
