package net.twentyytwo.cleanertooltips.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.api.Requirement;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Config(name = CleanerTooltips.MOD_ID)
public class TooltipsClothConfig extends TooltipsConfig implements ConfigData {

    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder configBuilder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(translate("title"))
                .setSavingRunnable(TooltipsClothConfig::saveConfig);
        configBuilder.setGlobalized(true);
        configBuilder.setGlobalizedExpanded(false);
        ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder().setResetButtonKey(Component.literal("⇄"));

        var config = CleanerTooltips.config;

        // --------------------------------------------------
        // General
        // --------------------------------------------------
        BooleanListEntry iconsEnabled = entryBuilder
                .startBooleanToggle(translate("option.iconsEnabled"), config.iconsEnabled)
                .setTooltip(translate("option.iconsEnabled.tooltip"))
                .setDefaultValue(true)
                .setSaveConsumer(newVal -> config.iconsEnabled = newVal)
                .build();

        BooleanListEntry comparisonEnabled = entryBuilder
                .startBooleanToggle(translate("option.comparisonEnabled"), config.comparisonEnabled)
                .setTooltip(translate("option.comparisonEnabled.tooltip"))
                .setDefaultValue(true)
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.comparisonEnabled = newVal)
                .build();

        BooleanListEntry comparisonArrow = entryBuilder
                .startBooleanToggle(translate("option.comparisonArrow"), config.comparisonArrow)
                .setTooltip(translate("option.comparisonArrow.tooltip"))
                .setDefaultValue(true)
                .setRequirement(Requirement.all(Requirement.isTrue(iconsEnabled), Requirement.isTrue(comparisonEnabled)))
                .setSaveConsumer(newVal -> config.comparisonArrow = newVal)
                .build();

        BooleanListEntry onlyCompareShared = entryBuilder
                .startBooleanToggle(translate("option.onlyCompareShared"), config.onlyCompareMutual)
                .setTooltip(translate("option.onlyCompareShared.tooltip"))
                .setDefaultValue(false)
                .setRequirement(Requirement.all(Requirement.isTrue(iconsEnabled), Requirement.isTrue(comparisonEnabled)))
                .setSaveConsumer(newVal -> config.onlyCompareMutual = newVal)
                .build();

        BooleanListEntry hiddenHint = entryBuilder
                .startBooleanToggle(translate("option.hintEnabled"), config.hintEnabled)
                .setTooltip(translate("option.hintEnabled.tooltip"))
                .setDefaultValue(true)
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.hintEnabled = newVal)
                .build();

        StringListListEntry hintBlacklist = entryBuilder
                .startStrList(translate("option.hintBlacklist"), config.hintBlacklist)
                .setTooltip(translate("option.hintBlacklist.tooltip"))
                .setDefaultValue(List.of("minecraft:player.mining_efficiency"))
                .setRequirement(Requirement.all(Requirement.isTrue(iconsEnabled), Requirement.isTrue(hiddenHint)))
                .setSaveConsumer(newVal -> config.hintBlacklist = newVal)
                .build();

        IntegerSliderEntry attributeGap = entryBuilder
                .startIntSlider(translate("option.attributeGap"), config.attributeGap, 0, 20)
                .setTooltip(translate("option.attributeGap.tooltip"))
                .setDefaultValue(8)
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.attributeGap = newVal)
                .build();

        IntegerSliderEntry innerGap = entryBuilder
                .startIntSlider(translate("option.innerGap"), config.innerGap, 0, 20)
                .setTooltip(translate("option.innerGap.tooltip"))
                .setDefaultValue(3)
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.innerGap = newVal)
                .build();

        EnumListEntry<GroupDisplay> groupDisplay = entryBuilder
                .startEnumSelector(translate("option.groupDisplay"), GroupDisplay.class, config.groupDisplay)
                .setTooltip(translate("option.groupDisplay.tooltip"))
                .setDefaultValue(GroupDisplay.ROWS)
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.groupDisplay = newVal)
                .build();

        BooleanListEntry sharpness = entryBuilder
                .startBooleanToggle(translate("option.sharpnessFix"), config.sharpnessFix)
                .setTooltip(translate("option.sharpnessFix.tooltip"))
                .setDefaultValue(true)
                .setSaveConsumer(newVal -> config.sharpnessFix = newVal)
                .build();

        BooleanListEntry miningSpeed = entryBuilder
                .startBooleanToggle(translate("option.miningSpeed"), config.miningSpeed)
                .setTooltip(translate("option.miningSpeed.tooltip"))
                .setDefaultValue(true)
                .setSaveConsumer(newVal -> config.miningSpeed = newVal)
                .build();

        addEntries(
                configBuilder.getOrCreateCategory(translate("general")),
                iconsEnabled, comparisonEnabled, comparisonArrow, onlyCompareShared, hiddenHint,
                hintBlacklist, attributeGap, innerGap, groupDisplay, sharpness, miningSpeed
        );

        // --------------------------------------------------
        // Durability
        // --------------------------------------------------
        BooleanListEntry durabilityEnabled = entryBuilder
                .startBooleanToggle(translate("option.durabilityEnabled"), config.durabilityEnabled)
                .setTooltip(translate("option.durabilityEnabled.tooltip"))
                .setDefaultValue(false)
                .setSaveConsumer(newVal -> config.durabilityEnabled = newVal)
                .build();

        EnumListEntry<Style> style = entryBuilder
                .startEnumSelector(translate("option.durabilityStyle"), Style.class, config.durabilityStyle)
                .setTooltip(translate("option.durabilityStyle.tooltip"))
                .setDefaultValue(Style.DEFAULT)
                .setRequirement(Requirement.isTrue(durabilityEnabled))
                .setSaveConsumer(newVal -> config.durabilityStyle = newVal)
                .build();

        BooleanListEntry durabilityMaximum = entryBuilder
                .startBooleanToggle(translate("option.durabilityMaximum"), config.durabilityMaximum)
                .setTooltip(translate("option.durabilityMaximum.tooltip"))
                .setDefaultValue(true)
                .setRequirement(Requirement.all(Requirement.isTrue(durabilityEnabled), Requirement.isValue(style, Style.DEFAULT)))
                .setSaveConsumer(newVal -> config.durabilityMaximum = newVal)
                .build();

        BooleanListEntry durabilityColor = entryBuilder
                .startBooleanToggle(translate("option.durabilityColor"), config.durabilityColor)
                .setTooltip(translate("option.durabilityColor.tooltip"))
                .setDefaultValue(true)
                .setRequirement(Requirement.isTrue(durabilityEnabled))
                .setSaveConsumer(newVal -> config.durabilityColor = newVal)
                .build();

        EnumListEntry<Position> position = entryBuilder
                .startEnumSelector(translate("option.durabilityPos"), Position.class, config.durabilityPos)
                .setTooltip(translate("option.durabilityPos.tooltip"))
                .setDefaultValue(Position.INLINE)
                .setRequirement(Requirement.isTrue(durabilityEnabled))
                .setSaveConsumer(newVal -> config.durabilityPos = newVal)
                .build();

        addEntries(
                configBuilder.getOrCreateCategory(translate("durability")),
                durabilityEnabled, durabilityMaximum, durabilityColor, style, position
        );

        return configBuilder.build();
    }

    private static Component translate(String key, Object... args) {
        return Component.translatable("config.cleanertooltips." + key, args);
    }

    private static void addEntries(ConfigCategory category, AbstractConfigListEntry<?>... entries) {
        Arrays.stream(entries).forEach(category::addEntry);
    }

    public static void saveConfig() {
        BLACKLISTED_HINTS.clear();
        var config = CleanerTooltips.config;

        int[] i = {0};
        for (var s : config.hintBlacklist) {
            TooltipsUtil.resolveAttribute(s).ifPresent(a -> {
                BLACKLISTED_HINTS.add(a);
                String resolved = a.key().location().toString();
                if (!s.equals(resolved)) config.hintBlacklist.set(i[0], resolved);
            });
            i[0]++;
        }

        CleanerTooltips.GROUP_GAP = config.attributeGap;
        CleanerTooltips.GAP = config.innerGap;

        AutoConfig.getConfigHolder(TooltipsClothConfig.class).save();
    }

    public static TooltipsClothConfig init() {
        AutoConfig.register(TooltipsClothConfig.class, GsonConfigSerializer::new);
        return AutoConfig.getConfigHolder(TooltipsClothConfig.class).getConfig();
    }
}
