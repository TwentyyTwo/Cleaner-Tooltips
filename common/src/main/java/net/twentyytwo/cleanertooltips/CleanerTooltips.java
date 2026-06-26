package net.twentyytwo.cleanertooltips;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.twentyytwo.cleanertooltips.compat.BetterCombatHandler;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig.PosValues;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.AttributeManager;
import net.twentyytwo.cleanertooltips.util.TooltipsUtil;
import net.twentyytwo.cleanertooltips.util.ClientIconComponent;
import net.twentyytwo.cleanertooltips.util.Comparison;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;

import static net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig.blacklistedHints;
import static net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig.configHolder;

public class CleanerTooltips {

    public static Minecraft MC = Minecraft.getInstance();

    public static final String MOD_ID = "cleanertooltips";
    public static final KeyMapping hideTooltip = new KeyMapping(
            "key.cleanertooltips.hide_tooltip",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.INVENTORY
    );

    public static CleanerTooltipsConfig config;

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes

    private static final String PATH = "textures/gui/attribute/";
    private static final Identifier DURABILITY_ICON = location(PATH + "durability.png");
    private static final Identifier DIGGING_SPEED = location(PATH + "digging_speed.png");
    private static final Identifier HIGHER = location(PATH + "higher.png");
    private static final Identifier LOWER = location(PATH + "lower.png");

    public static void init() {
        configHolder = AutoConfig.register(CleanerTooltipsConfig.class, GsonConfigSerializer::new);
        configHolder.registerSaveListener((holder, configInstance) -> {
            config = configInstance;
            config.onConfigSave();
            return InteractionResult.SUCCESS;
        });
        config = AutoConfig.getConfigHolder(CleanerTooltipsConfig.class).getConfig();
        config.onConfigSave();
    }

    public static Identifier location(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
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

        var remain = Component.literal(String.valueOf(curDurability)).withStyle(durabilityColor);
        if (!config.durability.maximumDurability) {
            return remain;
        }

        return remain.append(Component.translatable("text.cleanertooltips.total_durability", maxDurability)
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    public record IconAttributeComponent(ItemStack stack) implements TooltipComponent {
    }

    public static class IconAttributeTooltip implements ClientIconComponent {
        private final ItemStack stack;
        private final ListMultimap<EquipmentSlotGroup, AttributeFormattingData> groupFormattingDataMap;
        private final MutableComponent durabilityComponent;
        private final AttributeFormattingData miningSpeedData;

        private final boolean anyTextureMissing;

        public ItemStack getStack() {
            return this.stack;
        }

        public IconAttributeTooltip(ItemStack stack,
                ListMultimap<EquipmentSlotGroup, AttributeFormattingData> groupFormattingDataMap,
                MutableComponent durabilityComponent, AttributeFormattingData miningSpeedData,
                boolean anyTextureMissing) {
            this.stack = stack;
            this.groupFormattingDataMap = groupFormattingDataMap;
            this.durabilityComponent = durabilityComponent;
            this.miningSpeedData = miningSpeedData;
            this.anyTextureMissing = anyTextureMissing;
        }

        public IconAttributeTooltip(IconAttributeComponent component) {
            this(component.stack());
        }

        public IconAttributeTooltip(ItemStack stack) {
            CombinedAttributeModifiers modifiers = CombinedAttributeModifiers.fromStack(stack);
            CombinedAttributeModifiers comparedModifiers = getComparedModifiers(stack);

            final boolean[] anyTextureMissing = {false};

            if (!config.advanced.onlyCompareShared) {
                boolean isArmor = TooltipsUtil.isArmor(stack);
                modifiers = modifiers.combine(comparedModifiers, isArmor, false);
            }

            ImmutableListMultimap.Builder<EquipmentSlotGroup, AttributeFormattingData> builder =
                    ImmutableListMultimap.builder();
            modifiers.modifiers().forEach((slot, entry) -> {
                Comparison comparison = Comparison.NONE;
                if (comparedModifiers.modifiers().containsKey(slot)) {
                    boolean keepOperationsSeparate = TooltipsUtil.separateOperations(slot);
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
                        TooltipsUtil.getBaseValue(attribute), entry.displayType());

                Identifier texture = AttributeManager.getTexture(attribute);
                if (texture != null) {
                    builder.put(slot, new AttributeFormattingData(text, attribute, comparison));
                } else if (!blacklistedHints.contains(attribute)) {
                    anyTextureMissing[0] = true;
                }
            });

            EquipmentSlotGroup mainhand = EquipmentSlotGroup.MAINHAND;
            if (BetterCombatHandler.isModLoaded && BetterCombatHandler.hasAttributes(stack)
                    && modifiers.modifiers().containsKey(mainhand)) {
                builder.put(mainhand, BetterCombatHandler.getRangeData(stack));
            }

            this.stack = stack;
            this.groupFormattingDataMap = builder.build();
            this.durabilityComponent = durabilityFormatting(stack);
            this.miningSpeedData = getMiningSpeedData(stack);
            this.anyTextureMissing = anyTextureMissing[0];
        }

        private static CombinedAttributeModifiers getComparedModifiers(ItemStack stack) {
            if (!config.general.compareAttributes) {
                return CombinedAttributeModifiers.EMPTY;
            }

            var comparedStack = TooltipsUtil.getEquippedStack(stack);
            if (comparedStack.isEmpty() || comparedStack.equals(stack)
                    || !TooltipsUtil.hasAttributes(comparedStack)) {
                return CombinedAttributeModifiers.EMPTY;
            }

            return CombinedAttributeModifiers.fromStack(comparedStack);
        }

        @Nullable
        private static AttributeFormattingData getMiningSpeedData(ItemStack stack) {
            if (config.general.miningSpeed) {
                float speed = TooltipsUtil.getDiggingSpeed(stack);
                if (speed <= 0.0f) return null;

                var component = Component.literal(DecimalFormat.getInstance().format(speed));
                Comparison comparison = getMiningSpeedComparison(stack, speed);

                return new AttributeFormattingData(component, DIGGING_SPEED, comparison);
            }
            return null;
        }

        private static Comparison getMiningSpeedComparison(ItemStack stack, float speed) {
            if (config.general.compareAttributes) {
                var comparedStack = TooltipsUtil.getEquippedStack(stack);

                if (!comparedStack.isEmpty() && !comparedStack.equals(stack)
                        && stack.getItem().getClass().equals(comparedStack.getItem().getClass())) {
                    float comparedSpeed = TooltipsUtil.getDiggingSpeed(comparedStack);
                    if (comparedSpeed <= 0.0f) return Comparison.NONE;
                    return Comparison.getComparison(speed, comparedSpeed);
                }
            }

            return Comparison.NONE;
        }

        @Override
        public int getHeight(@NotNull Font font) {
            return config.advanced.groupDisplay == CleanerTooltipsConfig.GroupDisplay.ROWS
                    ? Math.max(10, groupFormattingDataMap.asMap().size() * 10)
                    : 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = 0;

            width += miningSpeedData != null
                    ? miningSpeedData.textWidth() + GROUP_GAP + GAP + 9
                    : 0;

            width += (TooltipsUtil.canAddDurabilityTooltip(stack)
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

            var dataMap = groupFormattingDataMap.asMap();
            for (var collection : dataMap.values()) {
                int rowWidth = 0;
                for (var formattingData : collection) {
                    rowWidth += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
                }

                if (firstIteration) {
                    firstIteration = false;
                    firstRowWidth += rowWidth;
                }
                biggestRowWidth = Math.max(rowWidth, biggestRowWidth);
            }

            if (this.anyTextureMissing && config.general.hiddenAttributesHint) {
                firstRowWidth += font.width("[+]") + GROUP_GAP;
            }

            int slotSize = dataMap.size() > 1 ? (9 + GROUP_GAP) : 0;
            return Math.max(firstRowWidth, biggestRowWidth) + slotSize;
        }

        private int getWidthInline(Font font, int width) {
            for (var formattingData : groupFormattingDataMap.values()) {
                width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
            }

            if (this.anyTextureMissing && config.general.hiddenAttributesHint) {
                width += font.width("[+]") + GROUP_GAP;
            }

            int slotSize = groupFormattingDataMap.asMap().size();
            return width + (slotSize > 1 ? slotSize * (9 + GROUP_GAP) : 0);
        }

        private int getWidthPrimary(Font font, int width) {
            for (var formattingData : groupFormattingDataMap.asMap().values().iterator().next()) {
                width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
            }

            if (this.anyTextureMissing && config.general.hiddenAttributesHint) {
                width += font.width("[+]") + GROUP_GAP;
            }

            return width;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, int width, int height,
                                @NotNull GuiGraphics guiGraphics) {
            int groupX = renderAttributeModifiers(font, guiGraphics, x, y);

            if (miningSpeedData != null) {
                groupX = renderMiningTooltip(guiGraphics, groupX, y - 1);
            }

            if (TooltipsUtil.canAddDurabilityTooltip(stack)
                    && config.durability.durabilityPos == PosValues.INLINE) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DURABILITY_ICON, groupX, y - 1, 0, 0, 9, 9, 9, 9);
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

            var dataMap = groupFormattingDataMap.asMap();
            for (var entry : dataMap.entrySet()) {
                if (dataMap.size() > 1) {
                    var icon = getSlotIcon(entry.getKey());
                    groupX = renderSlotGroupIcon(guiGraphics, icon, groupX, groupY);
                }

                for (var formattingData : entry.getValue()) {
                    groupX = renderAttributeIconPair(guiGraphics, formattingData, groupX, groupY);
                }

                if (firstIteration) {
                    firstIteration = false;
                    firstGroupX = groupX;
                }
                groupX = x;
                groupY += 10;
            }

            return this.anyTextureMissing
                    ? renderHiddenHint(font, guiGraphics, firstGroupX, y)
                    : firstGroupX;
        }

        private int renderInline(Font font, GuiGraphics guiGraphics, int x, int y) {
            var dataMap = groupFormattingDataMap.asMap();
            for (var entry : dataMap.entrySet()) {
                if (dataMap.size() > 1) {
                    var icon = getSlotIcon(entry.getKey());
                    x = renderSlotGroupIcon(guiGraphics, icon, x, y - 1);
                }

                for (var formattingData : entry.getValue()) {
                    x = renderAttributeIconPair(guiGraphics, formattingData, x, y - 1);
                }
            }

            return this.anyTextureMissing ? renderHiddenHint(font, guiGraphics, x, y) : x;
        }

        private int renderPrimary(Font font, GuiGraphics guiGraphics, int x, int y) {
            for (var formattingData : groupFormattingDataMap.asMap().values().iterator().next()) {
                x = renderAttributeIconPair(guiGraphics, formattingData, x, y - 1);
            }

            return this.anyTextureMissing ? renderHiddenHint(font, guiGraphics, x, y) : x;
        }

        private int renderSlotGroupIcon(GuiGraphics guiGraphics,
                                        Identifier icon,
                                        int x, int y) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, x, y, 0, 0, 9, 9, 9, 9);
            return x + 9 + GROUP_GAP;
        }

        private int renderAttributeIconPair(GuiGraphics guiGraphics,
                                            AttributeFormattingData entry,
                                            int x, int y) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
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
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, miningSpeedData.icon(), x, y, 0, 0, 9, 9, 9, 9);
            renderComparisonArrow(guiGraphics, miningSpeedData.comparison(), x, y);
            var component = miningSpeedData.text().withStyle(miningSpeedData.getFormatting());
            guiGraphics.drawString(MC.font, component, x + 9 + GAP, y + 1, -1);

            return x + miningSpeedData.textWidth() + GROUP_GAP + GAP + 9;
        }

        private void renderComparisonArrow(GuiGraphics guiGraphics, Comparison comparison,
                                           int x, int y) {
            if (config.general.comparisonArrow && !comparison.equals(Comparison.NONE)) {
                Identifier arrow = comparison.equals(Comparison.HIGHER) ? HIGHER : LOWER;
                int height = TooltipsUtil.getTickToggle() ? y : y - 1;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, arrow, x + 7, height, 0, 0, 3, 3, 3, 3);
            }
        }

        private Identifier getSlotIcon(EquipmentSlotGroup slotGroup) {
            String texturePath = "textures/gui/slot/" + slotGroup.getSerializedName() + ".png";
            Identifier resourceLocation = location(texturePath);
            return MC.getResourceManager().getResource(resourceLocation).isEmpty()
                    ? location("textures/gui/slot/any.png")
                    : resourceLocation;
        }
    }

    public record IconDurabilityComponent(ItemStack stack) implements TooltipComponent {
    }

    /**
     * A custom durability tooltip rendering the durability of an itemstack on the tooltip. <p>
     *
     * Only used when the config option {@code INLINE} isn't selected, otherwise the durability
     * tooltip is handled by the {@link IconAttributeTooltip} object.
     */
    public record IconDurabilityTooltip(MutableComponent text) implements ClientIconComponent {

        public IconDurabilityTooltip(IconDurabilityComponent component) {
            this(component.stack());
        }

        public IconDurabilityTooltip(ItemStack stack) {
            this(durabilityFormatting(stack));
        }

        @Override
        public int getHeight(@NotNull Font font) {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            return MC.font.width(text) + 9 + GAP;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, int width, int height,
                                @NotNull GuiGraphics guiGraphics) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DURABILITY_ICON, x, y - 1, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, text, x + 9 + GAP, y, -1);
        }
    }

}