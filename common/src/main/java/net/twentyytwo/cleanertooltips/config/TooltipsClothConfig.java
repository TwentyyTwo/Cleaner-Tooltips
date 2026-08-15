package net.twentyytwo.cleanertooltips.config;

import com.google.common.collect.ImmutableMap;
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
import me.shedaniel.clothconfig2.gui.entries.EmptyEntry;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.twentyytwo.cleanertooltips.CleanerTooltips;
import net.twentyytwo.cleanertooltips.config.AutoListListEntry.AutoListCell;
import net.twentyytwo.cleanertooltips.config.ColorStopMapListEntry.ColorStopMapCell;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
@Config(name = CleanerTooltips.MOD_ID)
public class TooltipsClothConfig extends TooltipsConfig implements ConfigData {

    private static final Function<String, Optional<Component>> ATTRIBUTE_ID_VALIDATOR = str -> {
        if (str == null || str.trim().isEmpty()) {
            return Optional.of(translate("attribute_id.empty"));
        }
        ResourceLocation id = ResourceLocation.parse(str);
        if (!BuiltInRegistries.ATTRIBUTE.containsKey(id)) {
            return Optional.of(translate("attribute_id.not_found", str));
        }
        return Optional.empty();
    };

    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder configBuilder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(translate("title"))
                .setSavingRunnable(TooltipsClothConfig::saveConfig);
        ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder().setResetButtonKey(Component.literal("⇄"));

        var config = CleanerTooltips.config;

        EmptyEntry empty = new EmptyEntry(20);

        // --------------------------------------------------
        // General
        // --------------------------------------------------
        BooleanListEntry iconsEnabled = entryBuilder
                .startBooleanToggle(translate("option.iconsEnabled"), config.iconsEnabled)
                .setTooltip(translate("option.iconsEnabled.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(true)
                .setSaveConsumer(newVal -> config.iconsEnabled = newVal)
                .build();

        BooleanListEntry comparisonEnabled = entryBuilder
                .startBooleanToggle(translate("option.comparisonEnabled"), config.comparisonEnabled)
                .setTooltip(translate("option.comparisonEnabled.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(true)
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.comparisonEnabled = newVal)
                .build();

        BooleanListEntry comparisonArrow = entryBuilder
                .startBooleanToggle(translate("option.comparisonArrow"), config.comparisonArrow)
                .setTooltip(translate("option.comparisonArrow.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(true)
                .setRequirement(Requirement.all(Requirement.isTrue(iconsEnabled), Requirement.isTrue(comparisonEnabled)))
                .setSaveConsumer(newVal -> config.comparisonArrow = newVal)
                .build();

        BooleanListEntry onlyCompareShared = entryBuilder
                .startBooleanToggle(translate("option.onlyCompareShared"), config.onlyCompareMutual)
                .setTooltip(translate("option.onlyCompareShared.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(false)
                .setRequirement(Requirement.all(Requirement.isTrue(iconsEnabled), Requirement.isTrue(comparisonEnabled)))
                .setSaveConsumer(newVal -> config.onlyCompareMutual = newVal)
                .build();

        BooleanListEntry hiddenHint = entryBuilder
                .startBooleanToggle(translate("option.hintEnabled"), config.hintEnabled)
                .setTooltip(translate("option.hintEnabled.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(true)
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.hintEnabled = newVal)
                .build();

        List<String> attributeIds = new TreeSet<>(BuiltInRegistries.ATTRIBUTE.keySet()).stream()
                .map(ResourceLocation::toString).toList();

        AutoListListEntry hintBlacklist = new AutoListBuilder(
                entryBuilder, translate("option.hintBlacklist"), config.hintBlacklist)
                .setTooltip(translate("option.hintBlacklist.tooltip"))
                .setDefaultValue(List.of("minecraft:player.mining_efficiency"))
                .setRequirement(Requirement.all(Requirement.isTrue(iconsEnabled), Requirement.isTrue(hiddenHint)))
                .setSaveConsumer(newVal -> config.hintBlacklist = newVal)
                .setInsertInFront(true)
                .setCellErrorSupplier(ATTRIBUTE_ID_VALIDATOR)
                .setCreateNewInstance(entry -> new AutoListCell("", entry, attributeIds, TooltipsClothConfig::isValidLocation))
                .setSuggestions(attributeIds)
                .setFilter(TooltipsClothConfig::isValidLocation)
                .build();

        ExtendedSliderEntry attributeGap = new ExtendedSliderBuilder(entryBuilder, translate("option.attributeGap"),
                                                                     config.attributeGap, 0, 20)
                .setTooltip(translate("option.attributeGap.tooltip"))
                .setDefaultValue(8)
                .setSuffix(Component.literal("px"))
                .setRequirement(Requirement.isTrue(iconsEnabled))
                .setSaveConsumer(newVal -> config.attributeGap = newVal)
                .build();

        ExtendedSliderEntry innerGap = new ExtendedSliderBuilder(entryBuilder, translate("option.innerGap"),
                                                                config.innerGap, 0, 20)
                .setTooltip(translate("option.innerGap.tooltip"))
                .setDefaultValue(3)
                .setSuffix(Component.literal("px"))
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
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(true)
                .setSaveConsumer(newVal -> config.sharpnessFix = newVal)
                .build();

        BooleanListEntry miningSpeed = entryBuilder
                .startBooleanToggle(translate("option.miningSpeed"), config.miningSpeed)
                .setTooltip(translate("option.miningSpeed.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(true)
                .setSaveConsumer(newVal -> config.miningSpeed = newVal)
                .build();

        addEntries(
                configBuilder.getOrCreateCategory(translate("general")),
                iconsEnabled, comparisonEnabled, comparisonArrow, onlyCompareShared, hiddenHint,
                hintBlacklist, groupDisplay, attributeGap, innerGap, empty, sharpness, miningSpeed
        );

        // --------------------------------------------------
        // Durability
        // --------------------------------------------------
        BooleanListEntry durabilityEnabled = entryBuilder
                .startBooleanToggle(translate("option.durabilityEnabled"), config.durabilityEnabled)
                .setTooltip(translate("option.durabilityEnabled.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
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
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(true)
                .setRequirement(Requirement.all(Requirement.isTrue(durabilityEnabled), Requirement.isValue(style, Style.DEFAULT)))
                .setSaveConsumer(newVal -> config.durabilityMaximum = newVal)
                .build();

        BooleanListEntry hideWhenRepaired = entryBuilder
                .startBooleanToggle(translate("option.hideWhenRepaired"), config.hideWhenRepaired)
                .setTooltip(translate("option.hideWhenRepaired.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
                .setDefaultValue(false)
                .setRequirement(Requirement.isTrue(durabilityEnabled))
                .setSaveConsumer(newVal -> config.hideWhenRepaired = newVal)
                .build();

        BooleanListEntry durabilityColor = entryBuilder
                .startBooleanToggle(translate("option.durabilityColor"), config.durabilityColor)
                .setTooltip(translate("option.durabilityColor.tooltip"))
                .setYesNoTextSupplier(TooltipsClothConfig::toggleOnOff)
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

        EnumListEntry<ColorMode> colorMode = entryBuilder
                .startEnumSelector(translate("option.durabilityColorMode"), ColorMode.class, config.colorMode)
                .setTooltip(translate("option.durabilityColorMode.tooltip"))
                .setDefaultValue(ColorMode.DEFAULT)
                .setRequirement(Requirement.all(Requirement.isTrue(durabilityEnabled), Requirement.isTrue(durabilityColor)))
                .setSaveConsumer(newVal -> config.colorMode = newVal)
                .build();

        ColorStopMapListEntry colorStops =
                        new ColorStopMapBuilder(entryBuilder, translate("option.colorStops"), config.colorsStops)
                .setTooltip(translate("option.colorStops.tooltip"))
                .setDefaultValue(new LinkedHashMap<>(ImmutableMap.of(100, 0x55ff55, 50, 0xffaa00, 15, 0xff5555)))
                .setExpanded(true)
                .setRequirement(Requirement.all(Requirement.isTrue(durabilityEnabled),
                                                Requirement.isTrue(durabilityColor),
                                                Requirement.not(Requirement.isValue(colorMode, ColorMode.NATIVE))))
                .setSaveConsumer(newVal -> config.colorsStops = newVal)
                .setCreateNewInstance(entry -> new ColorStopMapCell(35, 0x000000, entry))
                .build();

        addEntries(
                configBuilder.getOrCreateCategory(translate("durability")),
                durabilityEnabled, durabilityMaximum, hideWhenRepaired, position, style, empty,
                durabilityColor, colorMode, colorStops
        );

        return configBuilder.build();
    }

    private static Component translate(String key, Object... args) {
        return Component.translatable("config.cleanertooltips." + key, args);
    }

    private static Component toggleOnOff(boolean state) {
        return state ? Component.translatable("text.cleanertooltips.toggle.enabled").withStyle(ChatFormatting.GREEN)
                     : Component.translatable("text.cleanertooltips.toggle.disabled").withStyle(ChatFormatting.RED);
    }

    private static void addEntries(ConfigCategory category, AbstractConfigListEntry<?>... entries) {
        Arrays.stream(entries).forEach(category::addEntry);
    }

    private static boolean isValidLocation(String text) {
        return Pattern.compile("^[a-z0-9._-]*(:[a-z0-9._/-]*)?$").matcher(text).matches();
    }

    public static void saveConfig() {
        BLACKLISTED_HINTS.clear();
        SORTED_STOPS.clear();
        var config = CleanerTooltips.config;

        config.hintBlacklist.forEach(s -> TooltipsUtil.getAttributeFromString(s).ifPresent(BLACKLISTED_HINTS::add));
        SORTED_STOPS.putAll(config.colorsStops);

        CleanerTooltips.GROUP_GAP = config.attributeGap;
        CleanerTooltips.GAP = config.innerGap;

        AutoConfig.getConfigHolder(TooltipsClothConfig.class).save();
    }

    public static TooltipsClothConfig init() {
        AutoConfig.register(TooltipsClothConfig.class, GsonConfigSerializer::new);
        return AutoConfig.getConfigHolder(TooltipsClothConfig.class).getConfig();
    }
}
