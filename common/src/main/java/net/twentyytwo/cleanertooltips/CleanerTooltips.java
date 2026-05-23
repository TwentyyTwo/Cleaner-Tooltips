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
import net.twentyytwo.cleanertooltips.compat.BetterCombatCompat;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.Collection;
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

    private static final ResourceLocation DURABILITY_ICON = location("textures/gui/attribute/durability.png");
    private static final ResourceLocation DIGGING_SPEED = location("textures/gui/attribute/digging_speed.png");
    private static final ResourceLocation HIGHER = location("textures/gui/attribute/higher.png");
    private static final ResourceLocation LOWER = location("textures/gui/attribute/lower.png");

    public static void init() {
        AutoConfig.register(CleanerTooltipsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(CleanerTooltipsConfig.class).getConfig();
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static MutableComponent formatting(double value, double baseValue, AttributeDisplayType displayType) {
        switch (displayType) {
            case BOOLEAN -> {
                return Component.literal(value > (double) 0.0F ? "Enabled" : "Disabled").withStyle(ChatFormatting.WHITE);
            }
            case DIFFERENCE -> {
                return Component.literal((value > 0 ? "+" : "") + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value))
                                .withStyle(value < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
            }
            case MULTIPLIER -> {
                return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format((value + baseValue) / baseValue) + "x")
                                .withStyle(ChatFormatting.WHITE);
            }
            case PERCENTAGE -> {
                return Component.literal((value > 0 ? "+" : "") + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value * 100)
                                .formatted(value < 0 ? ChatFormatting.RED : ChatFormatting.WHITE) + "%");
            }
            default -> {
                return Component.literal(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(value + baseValue))
                                .withStyle((value + baseValue) < 0 ? ChatFormatting.RED : ChatFormatting.WHITE);
            }
        }
    }

    private static MutableComponent durabilityFormatting(ItemStack stack) {
        int maxDurability = stack.getMaxDamage();
        int curDurability = maxDurability - stack.getDamageValue();
        float diff = (float) curDurability / maxDurability;

        ChatFormatting durabilityColor = (!config.durability.durabilityColor || curDurability == maxDurability) ? ChatFormatting.GRAY
                : diff >= 0.5f ? ChatFormatting.GREEN
                : diff >= 0.15f ? ChatFormatting.GOLD
                : ChatFormatting.RED;

        Component totalDurability = Component.literal(" / ")
                .append(Component.literal(String.valueOf(maxDurability)))
                .withStyle(ChatFormatting.DARK_GRAY);

        return Component.literal(String.valueOf(curDurability)).withStyle(durabilityColor)
                .append(config.durability.maximumDurability ? totalDurability : Component.empty());
    }

    public record IconAttributeModifierTooltip(
            ItemStack stack,
            ListMultimap<EquipmentSlotGroup, AttributeFormattingData> formattingDataMap,
            MutableComponent durabilityComponent,
            AttributeFormattingData miningSpeedData) implements TooltipComponent, ClientTooltipComponent {

        public static IconAttributeModifierTooltip get(ItemStack stack) {
            CombinedAttributeModifiers modifiers = CombinedAttributeModifiers.fromStack(stack);
            CombinedAttributeModifiers comparedModifiers = getComparedModifiers(stack);

            if (!config.advanced.onlyCompareShared) {
                modifiers = modifiers.combine(comparedModifiers, stack.getItem() instanceof ArmorItem, false);
            }

            ImmutableListMultimap.Builder<EquipmentSlotGroup, AttributeFormattingData> builder = ImmutableListMultimap.builder();
            modifiers.modifiers().forEach((slot, entry) -> {
                Comparison comparison = Comparison.NONE;
                if (comparedModifiers.modifiers().containsKey(slot)) {
                    boolean keepOperationsSeparate = CleanerTooltipsUtil.shouldSeparateOperations(slot);
                    comparison = comparedModifiers.modifiers().get(slot).stream()
                            .filter(e -> {
                                boolean baseCheck = entry.matchesAttribute(e.attribute());
                                return keepOperationsSeparate ? baseCheck && entry.matchesOperation(e.modifier()) : baseCheck;
                            })
                            .findFirst()
                            .map(entry::getComparison)
                            .orElseGet(() -> entry.getComparison(0, 0));
                }

                MutableComponent text = formatting(entry.modifier().amount(), CleanerTooltipsUtil.getBaseValue(entry.attribute()), entry.displayType());
                builder.put(slot, new AttributeFormattingData(text, entry.attribute(), comparison));
            });

            if (BetterCombatCompat.isModLoaded && BetterCombatCompat.hasAttributes(stack)) {
                if (modifiers.modifiers().containsKey(EquipmentSlotGroup.MAINHAND)) {
                    builder.put(EquipmentSlotGroup.MAINHAND, BetterCombatCompat.attackRangeEntry(stack));
                }
            }

            return new IconAttributeModifierTooltip(stack, builder.build(), durabilityFormatting(stack), getMiningSpeedData(stack));
        }

        private static CombinedAttributeModifiers getComparedModifiers(ItemStack stack) {
            if (!config.general.compareAttributes) {
                return CombinedAttributeModifiers.EMPTY;
            }

            ItemStack comparedStack = Objects.requireNonNull(MC.player).getItemBySlot(MC.player.getEquipmentSlotForItem(stack));
            if (comparedStack.isEmpty() || comparedStack.equals(stack) || !CleanerTooltipsUtil.hasAttributes(comparedStack)) {
                return CombinedAttributeModifiers.EMPTY;
            }

            return CombinedAttributeModifiers.fromStack(comparedStack);
        }

        @Nullable
        private static AttributeFormattingData getMiningSpeedData(ItemStack stack) {
            if (config.general.miningSpeed && stack.getItem() instanceof DiggerItem item) {
                float miningSpeed = getMiningSpeed(stack, item);

                MutableComponent miningSpeedComponent = Component.literal(DecimalFormat.getInstance().format(miningSpeed));
                Comparison comparison = getMiningSpeedComparison(stack, miningSpeed);

                return new AttributeFormattingData(miningSpeedComponent, DIGGING_SPEED, comparison);
            }
            return null;
        }

        private static Comparison getMiningSpeedComparison(ItemStack stack, float miningSpeed) {
            ItemStack comparedStack = Objects.requireNonNull(MC.player).getItemBySlot(MC.player.getEquipmentSlotForItem(stack));
            if (config.general.compareAttributes && !comparedStack.isEmpty() && !comparedStack.equals(stack) &&
                    comparedStack.getItem() instanceof DiggerItem comparedItem && stack.getItem().getClass().equals(comparedItem.getClass())) {
                float comparedMiningSpeed = getMiningSpeed(comparedStack, comparedItem);
                return Comparison.getComparison(miningSpeed, comparedMiningSpeed);
            }

            return Comparison.NONE;
        }

        private static float getMiningSpeed(ItemStack stack, DiggerItem item) {
            float miningSpeed = item.getTier().getSpeed();

            ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (var enchantment : enchantments.entrySet()) {
                var enchantmentKey = enchantment.getKey().unwrapKey();
                if (enchantmentKey.isPresent() && enchantmentKey.get() == Enchantments.EFFICIENCY  && enchantment.getIntValue() > 0) {
                    miningSpeed += (float) (enchantment.getIntValue() * enchantment.getIntValue()) + 1;
                    break;
                }
            }
            return miningSpeed;
        }

        @Override
        public int getHeight() {
            if (config.advanced.slotDisplay.ordinal() == 0) {
                int rowCounter = 1;

                boolean firstIteration = true;
                for (Collection<AttributeFormattingData> formattingDataList : formattingDataMap.asMap().values()) {
                    if (formattingDataList.stream().map(AttributeFormattingData::icon).anyMatch(Objects::nonNull)) {
                        if (firstIteration) {
                            firstIteration = false;
                            continue;
                        }
                        rowCounter++;
                    }
                }
                return rowCounter * 10;
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
                    && config.durability.durabilityPos == CleanerTooltipsConfig.posValues.INLINE)
                    ? MC.font.width(durabilityComponent) + 9 + GAP + GROUP_GAP
                    : 0;

            width = calculateAttributeWidth(font, width);

            return width - GROUP_GAP;
        }

        private int calculateAttributeWidth(Font font, int width) {
            int slotCounter = 0;
            int firstRowWidth = width;
            int biggestRowWidth = 0;

            boolean firstIteration = true;
            boolean anyIconMissing = false;

            for (Collection<AttributeFormattingData> formattingDataList : formattingDataMap.asMap().values()) {
                if (formattingDataList.stream().map(AttributeFormattingData::icon).allMatch(Objects::isNull)) {
                    anyIconMissing = true;
                    continue;
                }

                for (AttributeFormattingData formattingData : formattingDataList) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
                }

                if (config.advanced.slotDisplay.ordinal() == 2) {
                    break;
                } else if (config.advanced.slotDisplay.ordinal() == 0) {
                    if (firstIteration) { // the first width includes durability, mining speed etc
                        firstIteration = false;
                        firstRowWidth = width;
                    } else if (width > biggestRowWidth) biggestRowWidth = width;
                    width = 0;
                }

                slotCounter++;
            }

            if (formattingDataMap.keySet().size() > 1) {
                switch (config.advanced.slotDisplay) {
                    case INLINE -> width += slotCounter * (9 + GROUP_GAP);
                    case ROWS -> {
                        firstRowWidth += (9 + GROUP_GAP);
                        biggestRowWidth += (9 + GROUP_GAP);
                    }
                }
            }

            if (anyIconMissing && config.general.hiddenAttributesHint) {
                if (config.advanced.slotDisplay.ordinal() == 0) {
                    firstRowWidth += font.width("[+]") + GROUP_GAP;
                } else {
                    width += font.width("[+]") + GROUP_GAP;
                }
            }

            if (config.advanced.slotDisplay.ordinal() == 0) width = Math.max(firstRowWidth, biggestRowWidth);

            return width;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            int groupX = renderAttributeModifiers(font, guiGraphics, x, y);

            if (miningSpeedData != null) {
                groupX = renderMiningTooltip(guiGraphics, groupX, y - 1);
            }

            if (config.durability.durabilityEnabled && stack.getMaxDamage() > 0 && config.durability.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) {
                guiGraphics.blit(DURABILITY_ICON, groupX, y - 1, 0, 0, 9, 9, 9, 9);
                guiGraphics.drawString(MC.font, durabilityComponent, groupX + 9 + GAP, y, -1);
            }
        }

        private int renderAttributeModifiers(Font font, GuiGraphics guiGraphics, int x, int y) {
            int groupX = x;
            int groupY = y - 1;
            int firstGroupX = x;

            boolean firstIteration = true;
            boolean anyIconMissing = false;

            for (EquipmentSlotGroup slot : formattingDataMap.keySet()) {
                // Check if all icons in the current list are null, if so then continue
                if (formattingDataMap.get(slot).stream().map(AttributeFormattingData::icon).allMatch(Objects::isNull)) {
                    anyIconMissing = true;
                    continue;
                }

                if (formattingDataMap.keySet().size() > 1 && config.advanced.slotDisplay.ordinal() != 2) {
                    groupX = renderSlotGroupIcon(guiGraphics, getSlotIcon(slot), groupX, groupY);
                }
                for (AttributeFormattingData formattingData : formattingDataMap.get(slot)) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    groupX = renderAttributeIconPair(guiGraphics, formattingData, groupX, groupY);
                }

                if (config.advanced.slotDisplay.ordinal() == 2) {
                    break;
                } else if (config.advanced.slotDisplay.ordinal() == 0) {
                    if (firstIteration) {
                        firstIteration = false;
                        firstGroupX = groupX;
                    }
                    groupX = x;
                    groupY += 10;
                }
            }

            if (config.advanced.slotDisplay.ordinal() == 0) groupX = firstGroupX;

            if (anyIconMissing && config.general.hiddenAttributesHint) {
                guiGraphics.drawString(font, Component.literal("[+]").withStyle(ChatFormatting.YELLOW), groupX, y, -1);
                groupX += font.width("[+]") + GROUP_GAP;
            }

            return groupX;
        }

        private int renderSlotGroupIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y) {
            guiGraphics.blit(icon, x, y, 0, 0, 9, 9, 9, 9);
            return x + 9 + GROUP_GAP;
        }

        // Renders the icon and value for the respective attribute, and returns the total width that is then used as the x position for the next attribute
        private int renderAttributeIconPair(GuiGraphics guiGraphics, AttributeFormattingData entry, int x, int y) {
            guiGraphics.blit(entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
            renderComparisonArrow(guiGraphics, entry.comparison(), x, y);
            guiGraphics.drawString(MC.font, entry.text().withStyle(entry.getFormatting()), x + 9 + GAP, y + 1, -1);

            return x + entry.textWidth() + 9 + GAP + GROUP_GAP;
        }

        private int renderMiningTooltip(GuiGraphics guiGraphics, int x, int y) {
            guiGraphics.blit(miningSpeedData.icon(), x, y, 0, 0, 9, 9, 9, 9);
            renderComparisonArrow(guiGraphics, miningSpeedData.comparison(), x, y);
            guiGraphics.drawString(MC.font, miningSpeedData.text().withStyle(miningSpeedData.getFormatting()), x + 9 + GAP, y + 1, -1);

            return x + miningSpeedData.textWidth() + GROUP_GAP + GAP + 9;
        }

        private void renderComparisonArrow(GuiGraphics guiGraphics, Comparison comparison, int x, int y) {
            if (config.general.comparisonArrow && !comparison.equals(Comparison.NONE)) {
                ResourceLocation arrow = comparison.equals(Comparison.HIGHER) ? HIGHER : LOWER;
                guiGraphics.blit(arrow, x + 7, CleanerTooltipsUtil.getTickToggle() ? y : y - 1, 0, 0, 3, 3, 3, 3);
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
    public record IconDurabilityTooltip(MutableComponent text) implements TooltipComponent, ClientTooltipComponent {

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
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            guiGraphics.blit(DURABILITY_ICON , x, y - 1, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, text, x + 9 + GAP, y, -1);
        }
    }

}