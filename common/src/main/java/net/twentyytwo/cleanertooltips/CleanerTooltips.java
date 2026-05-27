package net.twentyytwo.cleanertooltips;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.compat.BetterCombatHandler;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig.PosValues;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.Objects;

public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final Minecraft MC = Minecraft.getInstance();
    public static final KeyMapping hideTooltip = new KeyMapping(
            "key.cleanertooltips.hide_tooltip",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.CATEGORY_INVENTORY
    );

    public static CleanerTooltipsConfig config;

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes

    private static final String PATH = "textures/gui/attribute/";
    private static final ResourceLocation DURABILITY_ICON = location(PATH + "durability.png");
    private static final ResourceLocation DIGGING_SPEED = location(PATH + "digging_speed.png");
    private static final ResourceLocation HIGHER = location(PATH + "higher.png");
    private static final ResourceLocation LOWER = location(PATH + "lower.png");

    public static void init() {
        AutoConfig.register(CleanerTooltipsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(CleanerTooltipsConfig.class).getConfig();
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MutableComponent formatting(double value, double baseValue,
                                              AttributeDisplayType displayType) {
        return switch (displayType) {
            case BOOLEAN -> Component.literal(value > 0.0 ? "Enabled" : "Disabled")
                    .withStyle(ChatFormatting.WHITE);
            case DIFFERENCE -> Component.literal((value > 0 ? "+" : "") + format(value))
                    .withStyle(value < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
            case MULTIPLIER -> Component.literal(format((value + baseValue) / baseValue) + "x")
                    .withStyle(ChatFormatting.WHITE);
            case PERCENTAGE -> Component.literal((value > 0 ? "+" : "") + format(value * 100)
                    .formatted(value < 0 ? ChatFormatting.RED : ChatFormatting.WHITE) + "%");
            case null, default -> Component.literal(format(value + baseValue))
                    .withStyle((value + baseValue) < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
        };
    }

    private static String format(double value) {
        return ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value);
    }

    private static MutableComponent durabilityFormatting(ItemStack stack) {
        int maxDurability = stack.getMaxDamage();
        int curDurability = maxDurability - stack.getDamageValue();
        float diff = (float) curDurability / maxDurability;

        ChatFormatting durabilityColor = !config.durability.durabilityColor
                || curDurability == maxDurability ? ChatFormatting.GRAY
                : diff >= 0.5f ? ChatFormatting.GREEN
                : diff >= 0.15f ? ChatFormatting.GOLD
                : ChatFormatting.RED;

        Component totalDurability = Component.literal(" / ")
                .append(Component.literal(String.valueOf(maxDurability)))
                .withStyle(ChatFormatting.DARK_GRAY);

        return Component.literal(String.valueOf(curDurability)).withStyle(durabilityColor)
                .append(config.durability.maximumDurability ? totalDurability : Component.empty());
    }

    public record IconAttributeModifierTooltip(ItemStack stack,
            ListMultimap<EquipmentSlotGroup, AttributeFormattingData> groupFormattingDataMap,
            MutableComponent durabilityComponent, AttributeFormattingData miningSpeedData)
            implements TooltipComponent, ClientTooltipComponent {

        public static IconAttributeModifierTooltip get(ItemStack stack) {
            CombinedAttributeModifiers modifiers = CombinedAttributeModifiers.fromStack(stack);
            CombinedAttributeModifiers comparedModifiers = getComparedModifiers(stack);

            if (!config.advanced.onlyCompareShared) {
                boolean isArmor = stack.getItem() instanceof ArmorItem;
                modifiers = modifiers.combine(comparedModifiers, isArmor, false);
            }

            ImmutableListMultimap.Builder<EquipmentSlotGroup, AttributeFormattingData> builder =
                    ImmutableListMultimap.builder();
            modifiers.modifiers().forEach((slot, entry) -> {
                Comparison comparison = Comparison.NONE;
                if (comparedModifiers.modifiers().containsKey(slot)) {
                    boolean keepOperationsSeparate = CleanerTooltipsUtil.separateOperations(slot);
                    comparison = comparedModifiers.modifiers().get(slot).stream()
                            .filter(e -> {
                                boolean baseCheck = entry.matchesAttribute(e.attribute());
                                return keepOperationsSeparate
                                        ? baseCheck && entry.matchesOperation(e.modifier())
                                        : baseCheck;
                            })
                            .findFirst()
                            .map(entry::getComparison)
                            .orElseGet(() -> entry.getComparison(0, 0));
                }

                var attribute = entry.attribute();
                MutableComponent text = formatting(entry.modifier().amount(),
                        CleanerTooltipsUtil.getBaseValue(attribute), entry.displayType());
                builder.put(slot, new AttributeFormattingData(text, attribute, comparison));
            });

            EquipmentSlotGroup mainhand = EquipmentSlotGroup.MAINHAND;
            if (BetterCombatHandler.isModLoaded && BetterCombatHandler.hasAttributes(stack)
                    && modifiers.modifiers().containsKey(mainhand)) {
                builder.put(mainhand, BetterCombatHandler.getRangeData(stack));
            }

            return new IconAttributeModifierTooltip(stack,
                    builder.build(),
                    durabilityFormatting(stack),
                    getMiningSpeedData(stack));
        }

        private static CombinedAttributeModifiers getComparedModifiers(ItemStack stack) {
            if (!config.general.compareAttributes) {
                return CombinedAttributeModifiers.EMPTY;
            }

            var comparedStack = CleanerTooltipsUtil.getEquippedStack(stack);
            if (comparedStack.isEmpty() || comparedStack.equals(stack)
                    || !CleanerTooltipsUtil.hasAttributes(comparedStack)) {
                return CombinedAttributeModifiers.EMPTY;
            }

            return CombinedAttributeModifiers.fromStack(comparedStack);
        }

        @Nullable
        private static AttributeFormattingData getMiningSpeedData(ItemStack stack) {
            if (config.general.miningSpeed && stack.getItem() instanceof DiggerItem item) {
                float speed = getMiningSpeed(stack, item.getTier().getSpeed());

                var component = Component.literal(DecimalFormat.getInstance().format(speed));
                Comparison comparison = getMiningSpeedComparison(stack, speed);

                return new AttributeFormattingData(component, DIGGING_SPEED, comparison);
            }
            return null;
        }

        private static Comparison getMiningSpeedComparison(ItemStack stack, float speed) {
            if (config.general.compareAttributes) {
                var comparedStack = CleanerTooltipsUtil.getEquippedStack(stack);

                if (!comparedStack.isEmpty() && !comparedStack.equals(stack)
                        && comparedStack.getItem() instanceof DiggerItem item
                        && stack.getItem().getClass().equals(item.getClass())) {
                    float comparedSpeed = item.getTier().getSpeed();
                    comparedSpeed = getMiningSpeed(comparedStack, comparedSpeed);
                    return Comparison.getComparison(speed, comparedSpeed);
                }
            }

            return Comparison.NONE;
        }

        private static float getMiningSpeed(ItemStack stack, float speed) {
            ItemEnchantments enchantments =
                    stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var enchantment : enchantments.entrySet()) {
                var key = enchantment.getKey().unwrapKey();
                if (key.isPresent() && key.get() == Enchantments.EFFICIENCY
                        && enchantment.getIntValue() > 0) {
                    speed += (float) (enchantment.getIntValue() * enchantment.getIntValue()) + 1;
                    break;
                }
            }
            return speed;
        }

        @Override
        public int getHeight() {
            if (config.advanced.groupDisplay == CleanerTooltipsConfig.GroupDisplay.ROWS) {
                int rowCounter = 0;

                for (var formattingData : groupFormattingDataMap.asMap().values()) {
                    if (formattingData.stream()
                            .map(AttributeFormattingData::icon)
                            .anyMatch(Objects::nonNull)) {
                        rowCounter++;
                    }
                }
                return Math.max(10, rowCounter * 10);
            }
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = 0;

            width += miningSpeedData != null
                    ? miningSpeedData.textWidth() + GROUP_GAP + GAP + 9
                    : 0;

            width += (config.durability.durabilityEnabled && stack.getMaxDamage() > 0
                    && config.durability.durabilityPos == PosValues.INLINE)
                    ? MC.font.width(durabilityComponent) + 9 + GAP + GROUP_GAP
                    : 0;

            width = calculateAttributeWidth(font, width);

            return width - GROUP_GAP;
        }

        private int calculateAttributeWidth(Font font, int width) {
            return switch (config.advanced.groupDisplay) {
                case ROWS -> getWidthRows(font, width);
                case INLINE -> getWidthInline(font, width);
                case PRIMARY -> getWidthPrimary(font, width);
            };
        }

        private int getWidthRows(Font font, int width) {
            int firstRowWidth = width;
            int biggestRowWidth = 0;

            boolean firstIteration = true;
            boolean anyIconMissing = false;

            var dataMap = groupFormattingDataMap.asMap();
            for (var collection : dataMap.values()) {
                for (var formattingData : collection) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
                }

                if (firstIteration) {
                    firstIteration = false;
                    firstRowWidth = width;
                } else if (width > biggestRowWidth) {
                    biggestRowWidth = width;
                }
                width = 0;
            }

            if (dataMap.size() > 1) {
                firstRowWidth += (9 + GROUP_GAP);
                biggestRowWidth += (9 + GROUP_GAP);
            }

            if (anyIconMissing && config.general.hiddenAttributesHint) {
                firstRowWidth += font.width("[+]") + GROUP_GAP;
            }

            return Math.max(firstRowWidth, biggestRowWidth);
        }

        private int getWidthInline(Font font, int width) {
            int slotCounter = 0;
            boolean anyIconMissing = false;

            var dataMap = groupFormattingDataMap.asMap();
            for (var collection : dataMap.values()) {
                boolean groupHasIcons = false;
                for (var formattingData : collection) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    groupHasIcons = true;
                    width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
                }

                if (groupHasIcons) slotCounter++;
            }

            if (dataMap.size() > 1) {
                width += slotCounter * (9 + GROUP_GAP);
            }

            if (anyIconMissing && config.general.hiddenAttributesHint) {
                width += font.width("[+]") + GROUP_GAP;
            }

            return width;
        }

        private int getWidthPrimary(Font font, int width) {
            boolean anyIconMissing = false;

            for (var formattingData : groupFormattingDataMap.asMap().values().iterator().next()) {
                if (formattingData.icon() == null) {
                    anyIconMissing = true;
                    continue;
                }

                width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
            }

            if (anyIconMissing && config.general.hiddenAttributesHint) {
                width += font.width("[+]") + GROUP_GAP;
            }

            return width;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y,
                                @NotNull GuiGraphics guiGraphics) {
            int groupX = renderAttributeModifiers(font, guiGraphics, x, y);

            if (miningSpeedData != null) {
                groupX = renderMiningTooltip(guiGraphics, groupX, y - 1);
            }

            if (config.durability.durabilityEnabled && stack.getMaxDamage() > 0
                    && config.durability.durabilityPos == PosValues.INLINE) {
                guiGraphics.blit(DURABILITY_ICON, groupX, y - 1, 0, 0, 9, 9, 9, 9);
                guiGraphics.drawString(MC.font, durabilityComponent, groupX + 9 + GAP, y, -1);
            }
        }

        private int renderAttributeModifiers(Font font, GuiGraphics guiGraphics, int x, int y) {
            return switch (config.advanced.groupDisplay) {
                case ROWS -> renderRows(font, guiGraphics, x, y);
                case INLINE -> renderInline(font, guiGraphics, x, y);
                case PRIMARY -> renderPrimary(font, guiGraphics, x, y);
            };
        }

        private int renderRows(Font font, GuiGraphics guiGraphics, int x, int y) {
            int groupX = x;
            int groupY = y - 1;
            int firstGroupX = x;

            boolean firstIteration = true;
            boolean anyIconMissing = false;

            var dataMap = groupFormattingDataMap.asMap();
            for (var entry : dataMap.entrySet()) {
                var collection = entry.getValue();

                boolean groupHasIcons = false;
                for (var formattingData : collection) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    if (dataMap.size() > 1 && !groupHasIcons) {
                        var icon = getSlotIcon(entry.getKey());
                        groupX = renderSlotGroupIcon(guiGraphics, icon, groupX, groupY);
                        groupHasIcons = true;
                    }

                    groupX = renderAttributeIconPair(guiGraphics, formattingData, groupX, groupY);
                }

                if (firstIteration) {
                    firstIteration = false;
                    firstGroupX = groupX;
                }
                groupX = x;
                groupY += 10;
            }

            return anyIconMissing
                    ? renderHiddenHint(font, guiGraphics, firstGroupX, y)
                    : firstGroupX;
        }

        private int renderInline(Font font, GuiGraphics guiGraphics, int x, int y) {
            boolean anyIconMissing = false;

            var dataMap = groupFormattingDataMap.asMap();
            for (var entry : dataMap.entrySet()) {
                var collection = entry.getValue();

                boolean groupHasIcons = false;
                for (var formattingData : collection) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    if (dataMap.size() > 1 && !groupHasIcons) {
                        var icon = getSlotIcon(entry.getKey());
                        x = renderSlotGroupIcon(guiGraphics, icon, x, y - 1);
                        groupHasIcons = true;
                    }

                    x = renderAttributeIconPair(guiGraphics, formattingData, x, y - 1);
                }
            }

            return anyIconMissing ? renderHiddenHint(font, guiGraphics, x, y) : x;
        }

        private int renderPrimary(Font font, GuiGraphics guiGraphics, int x, int y) {
            boolean anyIconMissing = false;

            for (var formattingData : groupFormattingDataMap.asMap().values().iterator().next()) {
                if (formattingData.icon() == null) {
                    anyIconMissing = true;
                    continue;
                }

                x = renderAttributeIconPair(guiGraphics, formattingData, x, y - 1);
            }

            return anyIconMissing ? renderHiddenHint(font, guiGraphics, x, y) : x;
        }

        private int renderSlotGroupIcon(GuiGraphics guiGraphics,
                                        ResourceLocation icon,
                                        int x, int y) {
            guiGraphics.blit(icon, x, y, 0, 0, 9, 9, 9, 9);
            return x + 9 + GROUP_GAP;
        }

        private int renderAttributeIconPair(GuiGraphics guiGraphics,
                                            AttributeFormattingData entry,
                                            int x, int y) {
            guiGraphics.blit(entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
            renderComparisonArrow(guiGraphics, entry.comparison(), x, y);
            var component = entry.text().withStyle(entry.getFormatting());
            guiGraphics.drawString(MC.font, component, x + 9 + GAP, y + 1, -1);

            return x + entry.textWidth() + 9 + GAP + GROUP_GAP;
        }

        private int renderHiddenHint(Font font, GuiGraphics guiGraphics, int x, int y) {
            if (config.general.hiddenAttributesHint) {
                var component = Component.literal("[+]").withStyle(ChatFormatting.YELLOW);
                guiGraphics.drawString(font, component, x, y, -1);
                x += font.width("[+]") + GROUP_GAP;
            }
            return x;
        }

        private int renderMiningTooltip(GuiGraphics guiGraphics, int x, int y) {
            guiGraphics.blit(miningSpeedData.icon(), x, y, 0, 0, 9, 9, 9, 9);
            renderComparisonArrow(guiGraphics, miningSpeedData.comparison(), x, y);
            var component = miningSpeedData.text().withStyle(miningSpeedData.getFormatting());
            guiGraphics.drawString(MC.font, component, x + 9 + GAP, y + 1, -1);

            return x + miningSpeedData.textWidth() + GROUP_GAP + GAP + 9;
        }

        private void renderComparisonArrow(GuiGraphics guiGraphics, Comparison comparison,
                                           int x, int y) {
            if (config.general.comparisonArrow && !comparison.equals(Comparison.NONE)) {
                ResourceLocation arrow = comparison.equals(Comparison.HIGHER) ? HIGHER : LOWER;
                int height = CleanerTooltipsUtil.getTickToggle() ? y : y - 1;
                guiGraphics.blit(arrow, x + 7, height, 0, 0, 3, 3, 3, 3);
            }
        }

        private ResourceLocation getSlotIcon(EquipmentSlotGroup slotGroup) {
            String texturePath = "textures/gui/slot/" + slotGroup.getSerializedName() + ".png";
            ResourceLocation resourceLocation = location(texturePath);
            return MC.getResourceManager().getResource(resourceLocation).isEmpty()
                    ? location("textures/gui/slot/any.png")
                    : resourceLocation;
        }
    }

    /**
     * A custom durability tooltip rendering the durability of an itemstack on the tooltip. <p>
     *
     * Only used when the config option {@code INLINE} isn't selected, otherwise the durability
     * tooltip is handled by the {@link IconAttributeModifierTooltip} object.
     */
    public record IconDurabilityTooltip(MutableComponent text)
            implements TooltipComponent, ClientTooltipComponent {

        public IconDurabilityTooltip(ItemStack stack) {
            this(durabilityFormatting(stack));
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return MC.font.width(text) + 9 + GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y,
                                @NotNull GuiGraphics guiGraphics) {
            guiGraphics.blit(DURABILITY_ICON , x, y - 1, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, text, x + 9 + GAP, y, -1);
        }
    }

}