package net.twentyytwo.cleanertooltips;

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
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.twentyytwo.cleanertooltips.compat.BetterCombatCompat;
import net.twentyytwo.cleanertooltips.config.CleanerTooltipsConfig;
import net.twentyytwo.cleanertooltips.services.Services;
import net.twentyytwo.cleanertooltips.util.AttributeDisplayType;
import net.twentyytwo.cleanertooltips.util.CleanerTooltipsUtil;
import net.twentyytwo.cleanertooltips.util.Comparison;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class CleanerTooltips {

    public static final String MOD_ID = "cleanertooltips";
    public static final Minecraft MC = Minecraft.getInstance();
    public static final KeyMapping hideTooltip = new KeyMapping(
            "key.cleanertooltips.hide_tooltip",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            KeyMapping.CATEGORY_INVENTORY
    );

    public static final int legendaryTooltipsIncreasedHeight = 1;
    public static final boolean isLegendaryTooltipsLoaded = Services.getInstance().isModLoaded("legendarytooltips");

    public static CleanerTooltipsConfig config;

    private static final int GAP = 3; // The gap between the icon and the value
    private static final int GROUP_GAP = 8; // The gap between attributes

    private static final ResourceLocation DURABILITY_ICON = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attribute/durability.png");
    private static final ResourceLocation DIGGING_SPEED = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/attribute/digging_speed.png");

    public static void init() {
        AutoConfig.register(CleanerTooltipsConfig.class, JanksonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(CleanerTooltipsConfig.class).getConfig();
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
                .append(config.durability.showMaximumDurability ? totalDurability : Component.empty());
    }

    private record FormattingSlotList(List<AttributeFormattingData> formattingDataList, ResourceLocation slotIcon) {

        public FormattingSlotList(List<AttributeFormattingData> formattingDataList, EquipmentSlotGroup slotGroup) {
            this(formattingDataList, getSlotIcon(slotGroup));
        }

        private static ResourceLocation getSlotIcon(EquipmentSlotGroup slotGroup) {
            String slotGroupKey = slotGroup.getSerializedName();

            String texturePath = "textures/gui/slot/" + slotGroupKey + ".png";
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, texturePath);
            if (MC.getResourceManager().getResource(resourceLocation).isEmpty()) {
                return ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/slot/any.png");
            }
            return resourceLocation;
        }
    }

    public record IconAttributeModifierTooltip(
            ItemStack stack,
            List<FormattingSlotList> formattingSlotLists,
            MutableComponent durabilityComponent,
            float miningSpeed) implements TooltipComponent, ClientTooltipComponent {

        public IconAttributeModifierTooltip(ItemStack stack, ItemAttributeModifiers modifiers) {
            this(stack, getFormattingSlotLists(stack, modifiers), durabilityFormatting(stack), getMiningSpeed(stack));

            if (BetterCombatCompat.isModLoaded && BetterCombatCompat.hasAttributes(stack)) {
                formattingSlotLists.getFirst().formattingDataList().add(BetterCombatCompat.attackRangeEntry(stack, modifiers));
            }
        }

        private static float getMiningSpeed(ItemStack stack) {
            if (config.general.miningSpeed && stack.getItem() instanceof DiggerItem item) {
                float speed = item.getTier().getSpeed();

                ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                for (var enchantment : enchantments.entrySet()) {
                    var enchantmentKey = enchantment.getKey().unwrapKey();
                    if (enchantmentKey.isPresent() && enchantmentKey.get() == Enchantments.EFFICIENCY  && enchantment.getIntValue() > 0) {
                        speed += (float) (enchantment.getIntValue() * enchantment.getIntValue()) + 1;
                        break;
                    }
                }
                return speed;
            }

            return 0;
        }

        private static List<FormattingSlotList> getFormattingSlotLists(ItemStack stack, ItemAttributeModifiers modifiers) {
            CombinedAttributeModifiers combinedModifiers = new CombinedAttributeModifiers(stack, modifiers);

            List<FormattingSlotList> comparisonFormattingSlotLists = getComparisonFormattingData(stack, combinedModifiers);
            if (comparisonFormattingSlotLists != null) {
                return comparisonFormattingSlotLists;
            }

            List<FormattingSlotList> formattingSlotLists = new ArrayList<>();
            combinedModifiers.modifiers().forEach((slotGroup, entries) -> {
                formattingSlotLists.add(new FormattingSlotList(new ArrayList<>(), slotGroup));
                for (CombinedAttributeModifiers.Entry entry : entries) {
                    MutableComponent text = formatting(entry.modifier().amount(), entry.baseValue(), entry.displayType());
                    formattingSlotLists.getLast().formattingDataList().add(new AttributeFormattingData(text, entry.attribute(), Comparison.NONE));
                }
            });

            return formattingSlotLists;
        }

        private static List<FormattingSlotList> getComparisonFormattingData(ItemStack stack, CombinedAttributeModifiers combinedModifiers) {
            if (!config.general.compareAttributes) {
                return null;
            }

            ItemStack comparedStack = Objects.requireNonNull(MC.player).getItemBySlot(MC.player.getEquipmentSlotForItem(stack));
            if (comparedStack.isEmpty() || comparedStack.equals(stack)) {
                return null;
            }

            ItemAttributeModifiers comparedModifiers = CleanerTooltipsUtil.getAttributeModifiers(comparedStack);
            if (comparedModifiers.modifiers().isEmpty()) {
                return null;
            }

            CombinedAttributeModifiers combinedComparedModifiers = new CombinedAttributeModifiers(comparedStack, comparedModifiers);
            Set<EquipmentSlotGroup> modifierGroups = combinedModifiers.modifiers().keySet();
            Set<EquipmentSlotGroup> comparedGroups = combinedComparedModifiers.modifiers().keySet();
            if (modifierGroups.stream().noneMatch(comparedGroups::contains)) {
                return null;
            }

            if (!config.advanced.onlyCompareRelevant) {
                combinedModifiers.merge(combinedComparedModifiers);
            }

            List<FormattingSlotList> slotLists = new ArrayList<>();
            combinedModifiers.modifiers().forEach((slotGroup, entries) -> {
                slotLists.add(new FormattingSlotList(new ArrayList<>(), slotGroup));
                for (CombinedAttributeModifiers.Entry entry : entries) {
                    Comparison comparison = Comparison.NONE;
                    if (comparedGroups.contains(slotGroup)) {
                        Optional<CombinedAttributeModifiers.Entry> foundEntry = combinedComparedModifiers.modifiers().get(slotGroup).stream()
                                .filter(comparedEntry -> comparedEntry.attribute().equals(entry.attribute()))
                                .filter(comparedEntry -> !(entry.modifier().operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                                        || comparedEntry.modifier().operation().equals(entry.modifier().operation()))
                                .findFirst();

                        comparison = foundEntry.isPresent()
                                ? entry.getComparison(foundEntry.get())
                                : entry.getComparison(0, 0);
                    }

                    MutableComponent text = formatting(entry.modifier().amount(), entry.baseValue(), entry.displayType());
                    slotLists.getLast().formattingDataList().add(new AttributeFormattingData(text, entry.attribute(), comparison));
                }
            });
            return slotLists;
        }

        @Override
        public int getHeight() {
            if (config.advanced.slotDisplay.ordinal() == 1) {
                int rowCounter = 1;

                boolean firstIteration = true;
                for (FormattingSlotList formattingSlotList : formattingSlotLists) {
                    if (formattingSlotList.formattingDataList().stream().map(AttributeFormattingData::icon).anyMatch(Objects::nonNull)) {
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

            width += (miningSpeed > 0)
                    ? font.width(DecimalFormat.getInstance().format(miningSpeed)) + GROUP_GAP + GAP + 9
                    : 0;

            width += (config.durability.durabilityEnabled && stack.getMaxDamage() > 0
                    && config.durability.durabilityPos == CleanerTooltipsConfig.posValues.INLINE)
                    ? MC.font.width(durabilityComponent) + 9 + GAP + GROUP_GAP
                    : 0;

            width = calculateAttributeWidth(formattingSlotLists, font, width);

            return width - GROUP_GAP;
        }

        private static int calculateAttributeWidth(List<FormattingSlotList> formattingSlotLists, Font font, int width) {
            int slotCounter = 0;
            int firstRowWidth = width;
            int biggestRowWidth = 0;

            boolean firstIteration = true;
            boolean anyIconMissing = false;

            for (FormattingSlotList formattingSlotList : formattingSlotLists) {
                if (formattingSlotList.formattingDataList().stream().map(AttributeFormattingData::icon).allMatch(Objects::isNull)) {
                    anyIconMissing = true;
                    continue;
                }

                for (AttributeFormattingData formattingData : formattingSlotList.formattingDataList()) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    width += formattingData.textWidth() + 9 + GAP + GROUP_GAP;
                }

                if (config.advanced.slotDisplay.ordinal() == 2) break;
                else if (config.advanced.slotDisplay.ordinal() == 1) {
                    if (firstIteration) {
                        firstIteration = false;
                        firstRowWidth = width;
                    } else {
                        if (width > biggestRowWidth) biggestRowWidth = width;
                    }
                    width = 0;
                }

                slotCounter++;
            }

            if (formattingSlotLists.size() > 1 && config.advanced.slotDisplay.ordinal() == 0) {
                width += slotCounter * (9 + GROUP_GAP);
            } else if (formattingSlotLists.size() > 1 && config.advanced.slotDisplay.ordinal() == 1) {
                firstRowWidth += (9 + GROUP_GAP);
                biggestRowWidth += (9 + GROUP_GAP);
            }

            if (anyIconMissing && config.general.hiddenAttributesHint) {
                if (config.advanced.slotDisplay.ordinal() == 1) firstRowWidth += font.width("[+]") + GROUP_GAP;
                else width += font.width("[+]") + GROUP_GAP;
            }

            if (config.advanced.slotDisplay.ordinal() == 1) width = Math.max(firstRowWidth, biggestRowWidth);

            return width;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            int groupX = renderAttributeModifiers(formattingSlotLists, font, guiGraphics, x, y);

            if (miningSpeed > 0) {
                Comparison comparison = Comparison.NONE;
                if (config.general.compareAttributes) {
                    ItemStack comparedStack = Objects.requireNonNull(MC.player).getItemBySlot(MC.player.getEquipmentSlotForItem(stack));

                    if (!comparedStack.isEmpty() && !comparedStack.equals(stack) && comparedStack.getItem() instanceof DiggerItem) {
                        float comparedMiningSpeed = getMiningSpeed(comparedStack);
                        if (miningSpeed > comparedMiningSpeed) {
                            comparison = Comparison.HIGHER;
                        } else if (miningSpeed < comparedMiningSpeed) {
                            comparison = Comparison.LOWER;
                        }
                    }
                }

                groupX = renderMiningTooltip(guiGraphics, groupX, y - 1, miningSpeed, comparison);
            }

            if (config.durability.durabilityEnabled && stack.getMaxDamage() > 0 && config.durability.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) {
                guiGraphics.blit(DURABILITY_ICON, groupX, y - 1, 0, 0, 9, 9, 9, 9);
                guiGraphics.drawString(MC.font, durabilityComponent, groupX + 9 + GAP, y, -1);
            }
        }

        private static int renderAttributeModifiers(List<FormattingSlotList> formattingSlotLists, Font font, GuiGraphics guiGraphics, int x, int y) {
            int groupX = x;
            int groupY = y - 1;
            int firstGroupX = x;

            boolean shouldRenderSlotGroup = formattingSlotLists.size() > 1;
            boolean firstIteration = true;
            boolean anyIconMissing = false;

            for (FormattingSlotList formattingSlotList : formattingSlotLists) {
                if (formattingSlotList.formattingDataList().stream().map(AttributeFormattingData::icon).allMatch(Objects::isNull)) {
                    anyIconMissing = true;
                    continue;
                }

                if (shouldRenderSlotGroup && config.advanced.slotDisplay.ordinal() != 2) groupX = renderSlotGroupIcon(guiGraphics, formattingSlotList.slotIcon(), groupX, groupY);
                for (AttributeFormattingData formattingData : formattingSlotList.formattingDataList()) {
                    if (formattingData.icon() == null) {
                        anyIconMissing = true;
                        continue;
                    }

                    groupX = renderAttributeIconPair(guiGraphics, formattingData, groupX, groupY);
                }

                if (firstIteration && config.advanced.slotDisplay.ordinal() == 2) {
                    break;
                } else if (firstIteration) {
                    firstIteration = false;
                    firstGroupX = groupX;
                }

                if (config.advanced.slotDisplay.ordinal() == 1) {
                    groupX = x;
                    groupY += 10;
                }
            }

            if (config.advanced.slotDisplay.ordinal() == 1) groupX = firstGroupX;

            if (anyIconMissing && config.general.hiddenAttributesHint) {
                guiGraphics.drawString(font, Component.literal("[+]").withStyle(ChatFormatting.YELLOW), groupX, y, -1);
                groupX += font.width("[+]") + GROUP_GAP;
            }

            return groupX;
        }

        private static int renderSlotGroupIcon(GuiGraphics guiGraphics, ResourceLocation icon, int x, int y) {
            guiGraphics.blit(icon, x, y, 0, 0, 9, 9, 9, 9);
            return x + 9 + GROUP_GAP;
        }

        // Renders the icon and value for the respective attribute, and returns the total width that is then used as the x position for the next attribute
        private static int renderAttributeIconPair(GuiGraphics guiGraphics, AttributeFormattingData entry, int x, int y) {
            switch (entry.comparison()) {
                case HIGHER -> entry.text().withStyle(ChatFormatting.GREEN);
                case LOWER -> {
                    if (entry.text().getStyle().getColor() == TextColor.fromLegacyFormat(ChatFormatting.RED)) {
                        entry.text().withStyle(ChatFormatting.DARK_RED);
                    } else {
                        entry.text().withStyle(ChatFormatting.RED);
                    }
                }
            }

            guiGraphics.blit(entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, entry.text(), x + 9 + GAP, y + 1, -1);

            return x + entry.textWidth() + 9 + GAP + GROUP_GAP;
        }

        private static int renderMiningTooltip(GuiGraphics guiGraphics, int x, int y, float miningSpeed, Comparison comparison) {
            MutableComponent miningSpeedComponent = Component.literal(DecimalFormat.getInstance().format(miningSpeed));
            switch (comparison) {
                case HIGHER -> miningSpeedComponent.withStyle(ChatFormatting.GREEN);
                case LOWER -> miningSpeedComponent.withStyle(ChatFormatting.RED);
            }

            guiGraphics.blit(DIGGING_SPEED, x, y, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, miningSpeedComponent, x + 9 + GAP, y + 1, -1);
            return x + MC.font.width(miningSpeedComponent) + GROUP_GAP + GAP + 9;
        }
    }

    public record IconDurabilityTooltip(ItemStack stack, MutableComponent text) implements TooltipComponent, ClientTooltipComponent {

        /**
         * A custom durability tooltip rendering the durability of an itemstack on the tooltip. <p>
         *
         * Only used when the config option {@code INLINE} isn't selected, otherwise the durability
         * tooltip is handled by the {@link IconAttributeModifierTooltip} object.
         * @param stack the item stack
         */
        public IconDurabilityTooltip(ItemStack stack) {
            this(stack, durabilityFormatting(stack));
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