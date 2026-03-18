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
import net.minecraft.resources.ResourceLocation;
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
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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

        ChatFormatting durabilityColor = (!config.durabilityColor || curDurability == maxDurability) ? ChatFormatting.GRAY
                : diff >= 0.5f ? ChatFormatting.GREEN
                : diff >= 0.15f ? ChatFormatting.GOLD
                : ChatFormatting.RED;

        Component totalDurability = Component.literal(" / ")
                .append(Component.literal(String.valueOf(maxDurability)))
                .withStyle(ChatFormatting.DARK_GRAY);

        return Component.literal(String.valueOf(curDurability)).withStyle(durabilityColor)
                .append(config.showMaximumDurability ? totalDurability : Component.empty());
    }

    public record IconAttributeModifierTooltip(
            ItemStack stack,
            ItemAttributeModifiers modifiers,
            List<AttributeFormattingData> attributeFormattingDataList,
            MutableComponent durabilityComponent,
            float miningSpeed) implements TooltipComponent, ClientTooltipComponent {

        /**
         * A custom tooltip object rendering the attribute modifiers of an itemstack as icons.
         * @param stack     the item stack
         * @param modifiers the {@code ItemAttributeModifiers} of the itemstack
         */
        public IconAttributeModifierTooltip(ItemStack stack, ItemAttributeModifiers modifiers) {
            this(stack, modifiers, gatherFormattingData(stack, modifiers), durabilityFormatting(stack), getMiningSpeed(stack));

            if (BetterCombatCompat.isModLoaded && BetterCombatCompat.hasAttributes(stack)) {
                attributeFormattingDataList.add(BetterCombatCompat.attackRangeEntry(stack, modifiers));
            }
        }

        private static float getMiningSpeed(ItemStack stack) {
            if (config.miningSpeed && stack.getItem() instanceof DiggerItem item) {
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

        private static List<AttributeFormattingData> gatherFormattingData(ItemStack stack, ItemAttributeModifiers modifiers) {
            List<AttributeFormattingData> tooltipEntries = new ArrayList<>();

            CombinedAttributeModifiers combinedModifiers = new CombinedAttributeModifiers(stack, modifiers);
            combinedModifiers.modifiers().forEach((equipmentSlotGroup, entries) -> {
                for (int i = 0; i < entries.size(); i++) {
                    CombinedAttributeModifiers.Entry entry = entries.get(i);
                    MutableComponent text = formatting(entry.modifier().amount(), entry.baseValue(), entry.displayType());
                    tooltipEntries.add(new AttributeFormattingData(text, entry.attribute(), (i == entries.size() - 1)));
                }
            });

            return tooltipEntries;
        }

        @Override
        public int getHeight() {
            return 10;
        }

        @Override
        public int getWidth(@NotNull Font font) {
            int width = calculateAttributeWidth(attributeFormattingDataList, font);

            width += (miningSpeed > 0)
                    ? font.width(DecimalFormat.getInstance().format(miningSpeed)) + GROUP_GAP + GAP + 9
                    : 0;

            width += (config.durability && stack.getMaxDamage() > 0
                    && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE)
                    ? MC.font.width(durabilityComponent) + 9 + GAP + GROUP_GAP
                    : 0;

            return width - GROUP_GAP;
        }

        private static int calculateAttributeWidth(List<AttributeFormattingData> entries, Font font) {
            int width = 0;

            boolean anyIconMissing = false;
            for (int i = 0; i < entries.size(); i++) {
                AttributeFormattingData entry = entries.get(i);
                if (entry.icon() == null) {
                    anyIconMissing = true;
                    continue;
                }

                width += entry.textWidth() + 9 + GAP + GROUP_GAP;
                if (entry.isLastEntry() && i != entries.size() - 1) {
                    width += font.width("|") + GROUP_GAP;
                }
            }

            width += (anyIconMissing && config.hiddenAttributesHint)
                    ? font.width("[+]") + GROUP_GAP
                    : 0;

            return width;
        }

        @Override
        public void renderImage(@NotNull Font font, int x, int y, @NotNull GuiGraphics guiGraphics) {
            int groupX = renderAttributeModifiers(attributeFormattingDataList, font, guiGraphics, x, y);

            if (miningSpeed > 0) {
                groupX = renderMiningTooltip(guiGraphics, groupX, y - 1, miningSpeed);
            }

            if (config.durability && stack.getMaxDamage() > 0 && config.durabilityPos == CleanerTooltipsConfig.posValues.INLINE) {
                guiGraphics.blit(DURABILITY_ICON, groupX, y - 1, 0, 0, 9, 9, 9, 9);
                guiGraphics.drawString(MC.font, durabilityComponent, groupX + 9 + GAP, y, -1);
            }
        }

        private static int renderAttributeModifiers(List<AttributeFormattingData> entries, Font font, GuiGraphics guiGraphics, int x, int y) {
            int groupX = x;

            boolean anyIconMissing = false;
            for (int i = 0; i < entries.size(); i++) {
                AttributeFormattingData entry = entries.get(i);
                if (entry.icon() == null) {
                    anyIconMissing = true;
                    continue;
                }

                groupX = renderAttributeIconPair(guiGraphics, entry, groupX, y - 1);
                if (entry.isLastEntry() && i != entries.size() - 1) {
                    guiGraphics.drawString(font, Component.literal("|"), groupX, y, -1);
                    groupX += font.width("|") + GROUP_GAP;
                }
            }

            if (anyIconMissing && config.hiddenAttributesHint) {
                guiGraphics.drawString(font, Component.literal("[+]").withStyle(ChatFormatting.YELLOW), groupX, y, -1);
                groupX += font.width("[+]") + GROUP_GAP;
            }

            return groupX;
        }

        // Renders the icon and value for the respective attribute, and returns the total width that is then used as the x position for the next attribute
        private static int renderAttributeIconPair(GuiGraphics guiGraphics, AttributeFormattingData entry, int x, int y) {
            guiGraphics.blit(entry.icon(), x, y, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, entry.text(), x + 9 + GAP, y + 1, -1);

            x += entry.textWidth() + 9 + GAP + GROUP_GAP;
            return x;
        }

        private static int renderMiningTooltip(GuiGraphics guiGraphics, int x, int y, float miningSpeed) {
            String miningSpeedStr = DecimalFormat.getInstance().format(miningSpeed);
            guiGraphics.blit(DIGGING_SPEED, x, y, 0, 0, 9, 9, 9, 9);
            guiGraphics.drawString(MC.font, miningSpeedStr, x + 9 + GAP, y + 1, -1);
            return x + MC.font.width(miningSpeedStr) + GROUP_GAP + GAP + 9;
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